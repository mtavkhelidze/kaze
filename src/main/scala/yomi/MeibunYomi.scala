package kaze
package yomi
import yomi.Ku.*
import yomi.Op.*

import cats.parse.Parser
import cats.parse.Parser.*
import cats.parse.Rfc5234.digit

private[yomi] object Helpers {
  def escaped(c: Char): Parser[Char] =
    string(s"$c$c").as(c)

  def quotedString(c: Char): Parser[String] =
    char(c)
      *> (escaped(c) | Parser.charWhere(_ != c)).rep.string
      <* char(c)

  def litString = quotedString('"') | quotedString('\'')

  def number: Parser[String] = digit.rep(1).string

  def identBacktick: Parser[String] =
    quotedString('`')

  def identBare: Parser[String] =
    (Parser.charIn(('a' to 'z') ++ ('A' to 'Z')) ~
      Parser
        .charIn(('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') :+ '_')
        .rep0).string
}

object MeibunYomi {
  import Helpers.*

  def eq: Parser[Op] = string("=").as(Eq)
  def neq: Parser[Op] = string("!=").as(Neq)

  def lit: Parser[Ku] = (litString | number).map(Lit.apply)

  def ident: Parser[Ku] =
    (identBacktick | identBare).map(Ident.apply)

  def bin: Parser[Ku] =
    ((lit | ident) ~ (eq | neq) ~ (lit | ident)).map { case ((ka, op), kb) =>
      Bin(op, ka, kb)
    }
}
