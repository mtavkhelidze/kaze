package kaze
package panama

import java.lang.foreign.*

trait ModuleUtils[F[_]] {
  def toUnixTimestamp(dateStr: String): F[Long]
}

object ModuleUtils {
  val decl: List[Signature] = List(
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
}
