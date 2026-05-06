package kaze
package yomi

enum Op {
  case Eq
  case Neq
}

enum Ku {
  case Lit(v: String)
  case Ident(v: String)
  case Bin(o: Op, ka: Ku, kb: Ku)
  case Fn(n: Ident, ks: List[Ku])
}

enum Meibun {
  case Select(source: Meibun, columns: List[Ku])
  case From(t: Option[String])
}

object Meibun {
  object From {
    final val thinAir = From(None)
  }
}
