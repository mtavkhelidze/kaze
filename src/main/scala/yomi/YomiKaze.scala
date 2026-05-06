package kaze
package yomi

import cats.*
import cats.data.Kleisli

trait YomiKaze[F[_]] {
  def parse: Kleisli[F, String, Meibun]
}

object YomiKaze {
  def apply[F[_]: MonadThrow] = new YomiKaze[F] {
    def parse: Kleisli[F, String, Meibun] =
      ???
  }
}
