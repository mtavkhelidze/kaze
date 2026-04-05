package kaze

import cats.effect.*

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

object KamiKaze {
  def write[F[_]: Sync](path: String): String => F[Unit] =
    (src: String) =>
      Sync[F].blocking {
        Files.writeString(Paths.get(path), src, StandardCharsets.UTF_8)
      }

  def emit(rel: Rel, schema: YomiKaze.Schema): String = {
    rel match {
      case Rel.Select(source, cols) => {
        s"""
           |#include "kaze.h"
           |
           |${emitStruct(cols)}
           |
           |#ifdef __cplusplus
           |extern "C" {
           |#endif
           |
           |auto execute(char* line, Row* out) -> int32_t {
           |    size_t offsets[${schema.length}];
           |    splice(line, offsets, ${schema.length});
           |${emitAssignments(cols)}
           |    return 0;
           |}
           |
           |#ifdef __cplusplus
           |}
           |#endif
          """.stripMargin
      }
      case other =>
        throw NotImplementedError(s"KamiKaze: $other")
    }
  }

  private def emitStruct(cols: List[Expr]): String =
    s"""
       |struct Row {
       |size_t n_fields;
       |${cols.map(emitStructField).mkString("\n")}
       |};
       |""".stripMargin

  private def emitStructField(expr: Expr): String =
    expr match
      case Expr.Col(_, alias) => s"const char* $alias;"
      case Expr.Apply(alias, _) => s"uint64_t $alias;"
      case _ => throw NotImplementedError(s"KamiKaze: $expr")

  private def emitAssignments(cols: List[Expr]): String =
    s"    out->n_fields = ${cols.length};\n" +
      cols.map(emitAssignment).mkString("\n")

  private def emitAssignment(expr: Expr): String =
    expr match
      case Expr.Col(id, alias) =>
        s"    out->$alias = line + offsets[$id];"
      case Expr.Apply(alias, fn) =>
        s"    out->$alias = ${emitExpr(fn)};"
      case _ => throw NotImplementedError(s"KamiKaze: $expr")

  private def emitExpr(expr: Expr): String =
    expr match
      case Expr.Col(id, _) => s"line + offsets[$id]"
      case Expr.Func(name, inner) => s"${cppName(name)}(${emitExpr(inner)})"
      case _ => throw NotImplementedError(s"KamiKaze: $expr")

  private val cppName: Map[String, String] = Map(
    "to_unix" -> "to_unix_ts",
    "identity" -> "identity",
  )
}
