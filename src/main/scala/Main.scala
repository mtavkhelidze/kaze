package kaze

import cats.effect.*
import fs2.*

object Main extends IOApp.Simple {

  private val libPath = "cpp/build/libkaze.dylib"
  private val csvFile = "./scripts/E22-24-25.csv"

  def stream = Stream
    .eval(TheTailor[IO](libPath))
    .flatMap { tailor =>
      Stream
        .resource(tailor.utils)
        .flatMap { utils =>
          Kawa[IO](csvFile)
            .drop(1) // Skip CSV Header if present
            .map(_.split(","))
            .evalMap { cols =>
              val dateStr = cols(1) // Assuming Date is Col 2
              val team = cols(2) // Assuming Team is Col 3

              utils.toUnixTimestamp(dateStr).map { ts =>
                s"TS: $ts | Team: $team"
              }
            }
        }
    }
    .evalTap(IO.println(_))
    .compile
    .drain

  def emit(q: String): IO[String] = YomiKaze[IO](q)
    .use(yk => IO(KamiKaze.emit(yk.tree, yk.schema)))

  def run: IO[Unit] = {
    emit("./query/league_team.json")
      .flatMap(KamiKaze.write[IO]("cpp/src/kaze.cpp"))
      .as(ExitCode.Success)
  }
}
