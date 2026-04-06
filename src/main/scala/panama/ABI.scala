package kaze
package panama

import java.lang.foreign.*
import scala.compiletime.*

type Signature = (name: String, desc: FunctionDescriptor)

object ABI {

  def singnature(of: String): Signature =
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
