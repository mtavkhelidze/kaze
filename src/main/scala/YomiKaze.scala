package kaze

import cats.effect.*
import ujson.Value

import scala.io.Source

object YomiKaze {
  type Schema = List[String]

  def apply[F[_]: Async](
      path: String,
  ): Resource[F, (tree: Rel, schema: Schema)] =
    Resource
      .make(Sync[F].delay(Source.fromFile(path)))(s => Sync[F].delay(s.close()))
      .map(s => s.mkString)
      .map(str => ujson.read(str))
      .map(json => (parse(json), json("schema").arr.map(_.str).toList))

  private def parse(json: Value): Rel = decodeRel(json)

  private def decodeRel(json: Value): Rel =
    json("type").str match
      case "scan" =>
        Rel.Scan(json("table").str)
      case "select" =>
        val source = decodeRel(json("source"))
        val columns = json("columns").arr.map(decodeColumn)
        Rel.Select(source, columns.toList)
      case unknown =>
        throw IllegalArgumentException(s"Unknown query type: $unknown")

  private def decodeFunc(
      co: Expr,
      vals: List[Value],
  ): Expr =
    vals.foldRight(co: Expr)((v, exp) => Expr.Func(v("name").str, exp))

  private def decodeColumn(json: Value): Expr = {
    json("type").str match
      case "col" => Expr.Col(json("id").num.toInt, json("name").str)
      case "apply" =>
        val alias = json("alias").str
        val to: Expr.Col =
          Expr.Col(json("to_id").num.toInt, json("to_name").str)
        val fn = decodeFunc(to, json("func").arr.toList)
        Expr.Apply(alias, fn)
      case unknown => throw new Exception(s"Unknown Expr type: $unknown")
  }
}
