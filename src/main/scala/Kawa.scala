package kaze

import cats.effect.*
import fs2.*
import fs2.io.file.{Files, Path as FS2Path}

object Kawa {
  def apply[F[_]: Async](fname: String): Stream[F, String] = {
    Files[F]
      .readAll(FS2Path(fname))
      .through(text.utf8.decode)
      .through(text.lines)
      .filter(_.nonEmpty)
  }
}
