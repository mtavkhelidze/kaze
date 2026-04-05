package kaze

import cats.effect.*
import cats.syntax.all.*

import java.lang.foreign.*
import java.lang.invoke.MethodHandle
import java.nio.file.*

type Signature = (name: String, desc: FunctionDescriptor)

val querySignatures: List[Signature] = List(
  (
    "execute",
    FunctionDescriptor.of(
      // return code
      ValueLayout.JAVA_INT,
      // in: char* csv line
      ValueLayout.ADDRESS,
      // out: Row* result
      ValueLayout.ADDRESS,
    ),
  ),
)
val utilsSignatures: List[Signature] = List(
  (
    "to_unix_ts",
    FunctionDescriptor.of(
      // return value
      ValueLayout.JAVA_LONG,
      // in: char* value
      ValueLayout.ADDRESS,
    ),
  ),
)

trait UtilsModule[F[_]] {
  def toUnixTimestamp(dateStr: String): F[Long]
}

trait QueryModule[F[_]] {
  def execute(line: String): F[List[String]]
}

trait TheTailor[F[_]: Sync] { // of Panama
  def utils: Resource[F, UtilsModule[F]]
  def query: Resource[F, QueryModule[F]]
}
object TheTailor {
  private def memcpy(src: MemorySegment, dst: MemorySegment, n: Long) =
    MemorySegment.copy(src, 0L, dst, 0L, n)

  private def memzero(dst: MemorySegment) = dst.fill(0.toByte)

  private def readField(rowBuf: MemorySegment, i: Int): String =
    rowBuf
      .get(ValueLayout.ADDRESS, (i + 1) * 8L) // read the pointer
      .reinterpret(Long.MaxValue) // give it a size so we can read from it
      .getString(0L)

  private def mkArena[F[_]: Sync]: Resource[F, Arena] = {
    Resource.make(Sync[F].delay(Arena.ofShared()))(x =>
      Sync[F].delay(x.close()),
    )
  }

  private def findHandles[F[_]: Sync](
      lookup: SymbolLookup,
      sigs: List[Signature],
  ): F[Map[String, MethodHandle]] = {
    Sync[F]
      .delay(Linker.nativeLinker())
      .map(linker =>
        sigs
          .map(s =>
            (
              s.name,
              linker
                .downcallHandle(lookup.find(s.name).get, s.desc),
            ),
          )
          .toMap,
      )
  }

  def apply[F[_]: Sync](libraryPath: String): F[TheTailor[F]] = {
    val lookup = Sync[F]
      .delay(SymbolLookup.libraryLookup(Paths.get(libraryPath), Arena.global()))
    val utilsHandlers =
      lookup.flatMap(lookup => findHandles[F](lookup, utilsSignatures))
    val queryHandlers =
      lookup.flatMap(lookup => findHandles[F](lookup, querySignatures))

    (utilsHandlers, queryHandlers).mapN { case (uh, qh) =>
      new TheTailor[F] {
        def query: Resource[F, QueryModule[F]] = {
          mkArena[F].map { case arena =>
            new QueryModule[F] {
              val inBuf = arena.allocate(1024L).fill(0.toByte)
              val outBuf = arena.allocate(1024L).fill(0.toByte)
              override def execute(row: String): F[List[String]] = {
                Sync[F]
                  .blocking {
                    val bytes = row
                      .getBytes(java.nio.charset.StandardCharsets.UTF_8)
                    memcpy(
                      MemorySegment.ofArray(bytes),
                      inBuf,
                      bytes.length.toLong,
                    )
                    qh("execute").invokeExact(inBuf, outBuf).asInstanceOf[Int]
                  }
                  .flatMap {
                    case 0L =>
                      val nfields = outBuf.get(ValueLayout.JAVA_LONG, 0L).toInt
                      Sync[F].pure(
                        (0 until nfields).map(i => readField(outBuf, i)).toList,
                      )
                    case _ =>
                      Sync[F].raiseError(
                        IllegalArgumentException("Invalid date format"),
                      )
                  }
              }
            }
          }
        }

        def utils: Resource[F, UtilsModule[F]] = {
          mkArena[F].map { case arena =>
            val buf = arena.allocate(10L).fill(0.toByte)
            new UtilsModule[F] {
              override def toUnixTimestamp(dateStr: String): F[Long] = {
                Sync[F]
                  .blocking {
                    val bytes = dateStr
                      .getBytes(java.nio.charset.StandardCharsets.UTF_8)
                    memcpy(
                      MemorySegment.ofArray(bytes),
                      buf,
                      bytes.length.toLong,
                    )
                    uh("to_unix_ts").invokeExact(buf).asInstanceOf[Long]
                  }
                  .flatMap {
                    case 0L =>
                      Sync[F].raiseError(
                        IllegalArgumentException("Invalid date format"),
                      )
                    case r => Sync[F].pure(r)
                  }
              }
            }
          }
        }
      }
    }
  }
}
