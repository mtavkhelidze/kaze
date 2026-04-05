package kaze

import cats.effect.*
import cats.syntax.all.*

import java.lang.foreign.*
import java.lang.invoke.MethodHandle
import java.nio.file.*

type Signature = (name: String, desc: FunctionDescriptor)

val utilsSignatures: List[Signature] =
  List(
    (
      "to_unix_ts",
      FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
    ),
  )

trait UtilsModule[F[_]] {
  def toUnixTimestamp(dateStr: String): F[Long]
}

trait TheTailor[F[_]: Sync] { // of Panama
  def utils: Resource[F, UtilsModule[F]]
  // Later, mate...
  /// object compute extends SumComputation[F]
}
object TheTailor {
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

    utilsHandlers.map { case uh =>
      new TheTailor[F] {
        def utils: Resource[F, UtilsModule[F]] = {
          mkArena[F].map { case arena =>
            val buf = arena.allocate(10L).fill(0.toByte)
            new UtilsModule[F] {
              override def toUnixTimestamp(dateStr: String): F[Long] = {
                Sync[F]
                  .blocking {
                    val bytes = dateStr
                      .getBytes(java.nio.charset.StandardCharsets.UTF_8)
                    MemorySegment.copy(
                      MemorySegment.ofArray(bytes),
                      0L,
                      buf,
                      0L,
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
