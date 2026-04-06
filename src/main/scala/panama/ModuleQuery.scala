package kaze
package panama

import cats.effect.*
import cats.implicits.*
import cats.syntax.all.*

trait ModuleQuery[F[_]] {
  def execute(line: String): F[List[String]]
}

object ModuleQuery {
  private val decl: List[Signature] = List("execute").map(ABI.signature)

  def apply[F[_]: Sync](using cfg: KisokuKaze): Resource[F, ModuleQuery[F]] = {
    ABI.allocate[F].map { case (in, out) =>
      (row: String) => {
        ABI.memcpy(List(row), in)
        ABI.findHandles[F](decl).flatMap { handlers =>
          Sync[F].blocking {
            val ret: Long = handlers("execute").invokeExact(in, 2L, out)
            ret match {
              case 0L => {
                val nfields = ABI.fieldCount(out)
                (0 until nfields.toInt)
                  .map(i => ABI.pick[String](out, i))
                  .toList
              }
              case _ => {
                throw IllegalArgumentException("Invalid date format")
              }
            }
          }
        }
      }
    }
  }
}
