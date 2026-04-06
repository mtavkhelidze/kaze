package kaze
package panama

import cats.effect.*
import cats.syntax.all.*

import java.lang.foreign.*
import java.lang.invoke.MethodHandle
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import scala.compiletime.*

type Signature = (name: String, desc: FunctionDescriptor)

object ABI {
  type InOut = (in: MemorySegment, out: MemorySegment)

  def findHandles[F[_]: Sync](
      sigs: List[Signature],
  ): F[Map[String, MethodHandle]] = {
    lookup.map { lookup =>
      val linker = Linker.nativeLinker()
      sigs
        .map(s =>
          (s.name, linker.downcallHandle(lookup.find(s.name).get, s.desc)),
        )
        .toMap
    }
  }

  def lookup[F[_]: Sync](using cfg: KisokuKaze) = Sync[F].unit.map(_ =>
    SymbolLookup.libraryLookup(Paths.get(cfg.libraryPath), Arena.global()),
  )

  def memcpy(src: List[String], dst: MemorySegment): Unit =
    var offset = 0L
    src.foreach { line =>
      val bytes = line.getBytes(StandardCharsets.UTF_8) ++ Array(0.toByte)
      val slice = dst.asSlice(offset)
      MemorySegment
        .copy(MemorySegment.ofArray(bytes), 0L, slice, 0L, bytes.length)
      offset += bytes.length
    }

  def memzero(dst: MemorySegment): Unit = dst.fill(0.toByte)

  def allocate[F[_]: Sync](using cfg: KisokuKaze): Resource[F, InOut] = {
    sharedArena[F].map { case arena =>
      (
        arena.allocate(cfg.bufferLength).fill(0.toByte),
        arena.allocate(cfg.bufferLength).fill(0.toByte),
      )
    }
  }
  def sharedArena[F[_]: Sync]: Resource[F, Arena] = {
    Resource.make(Sync[F].delay(Arena.ofShared()))(x =>
      Sync[F].delay(x.close()),
    )
  }

  def signature(of: String): Signature =
    (
      of,
      FunctionDescriptor.of(
        // return value
        ValueLayout.JAVA_LONG,
        // argv
        ValueLayout.ADDRESS,
        // argc
        ValueLayout.JAVA_LONG,
        // out: result
        ValueLayout.ADDRESS,
      ),
    )

  /** As per the ABI, first field in thre result is always the number of fields.
    */
  inline def fieldCount(buf: MemorySegment): Long =
    buf.get(ValueLayout.JAVA_LONG, 0L)

  /** Pick a value with a given type from a raw memory buffer.
    *
    * There is no type-safety in this part of the world.
    */
  inline def pick[A](buf: MemorySegment, i: Long): A = {
    val mem = buf
      .get(ValueLayout.ADDRESS, (i + 1) * 8L) // read the pointer
      .reinterpret(Long.MaxValue) // give it a size so we can read from it
    inline erasedValue[A] match {
      case _: Long => mem.get(ValueLayout.JAVA_LONG, 0L).asInstanceOf[A]
      case _: String => mem.getString(0L).asInstanceOf[A].asInstanceOf[A]
      case _ => error("Unsupported type: " + erasedValue[A])
    }
  }
}
