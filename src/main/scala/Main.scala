package kaze

import cats.effect.*
import fs2.Stream
import panama.TheTailor

import java.io.File
import scala.sys.process.*

object Main extends IOApp.Simple {

  private val libPath = "./cpp/build/libkaze.dylib"
  private val csvFile = "./data/E22-24-25.csv"
  private val query = "./query/league_team.json"
  private val cppSrc = "./cpp/src/kaze.cpp"
  private val cppDir = "./cpp"

  private def cmake[F[_]: Sync](dir: String): F[Unit] =
    Sync[F].blocking {
      val cwd = new File(dir)
      val rc =
        Process(Seq("cmake", "--build", "build", "--target", "kaze"), cwd).!
      if rc != 0 then throw RuntimeException(s"cmake failed: $rc")
    }

  private def emit(q: String): IO[String] = YomiKaze[IO](q)
    .use(yk => IO(KamiKaze.emit(yk.tree, yk.schema)))

  private def program =
    emit(query)
      .flatMap(KamiKaze.write[IO](cppSrc))
      .flatTap(_ => cmake[IO](cppDir))
      .flatMap(_ => TheTailor[IO](libPath))
      .flatMap { tailor =>
        Stream
          .resource(tailor.query)
          .flatMap { query =>
            Kawa[IO](csvFile)
              .drop(1)
              .evalMap { row =>
                query
                  .execute(row)
                  .map(_.mkString(","))
              }
          }
          .evalTap(IO.println(_))
          .compile
          .drain
      }

  def run: IO[Unit] = {
    program.as(ExitCode.Success)
  }
}
