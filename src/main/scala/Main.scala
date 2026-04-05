package kaze

import cats.effect.*
import cats.syntax.all.*
import fs2.*

import scala.io.Source

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

  def run: IO[Unit] = {
    YomiKaze[IO]("query/to_unix.json")
      .use { (tree, schema) =>
        IO.println(tree) *> IO.println(schema).as(ExitCode.Success)
      }
    stream.as(ExitCode.Success)
  }
}
