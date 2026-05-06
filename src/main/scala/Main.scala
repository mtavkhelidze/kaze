package kaze

import yomi.YomiKaze

import cats.effect.*

object Main extends IOApp.Simple {

  private def program =
    YomiKaze[IO].parse

  def run: IO[Unit] = {
    program
      .run("SELECT * FROM users")
      .as(ExitCode.Success)
  }
}
