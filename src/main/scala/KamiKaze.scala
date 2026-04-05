package kaze

import cats.effect.*

trait KamiKaze[F[_]: Async] {
  def emit(rel: Rel): F[Unit]
}

object KamiKaze {
  def apply[F[_]: Async]: KamiKaze[F] = new KamiKaze[F] {
    def emit(rel: Rel): F[Unit] = Async[F].unit
  }
}
