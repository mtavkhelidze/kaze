package kaze
package panama

import cats.effect.*

trait TheTailor[F[_]: Sync] { // of Panama
//  def utils: Resource[F, ModuleUtils[F]]
  def query: Resource[F, ModuleQuery[F]]
}

object TheTailor {
  def apply[F[_]: Sync]: TheTailor[F] = {
    new TheTailor[F] {
      def query = ModuleQuery[F]
    }
  }
}
