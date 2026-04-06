package kaze
package panama
import java.lang.foreign.*

trait ModuleQuery[F[_]] {
  def execute(line: String): F[List[String]]
}

object ModuleQuery {
  val decl: List[Signature] = List("execute").map(ABI.singnature)
}
