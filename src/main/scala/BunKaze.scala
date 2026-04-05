package kaze

enum Expr {
  case Col(id: Int, alias: String)
  case Apply(alias: String, fn: Expr)
  case Func(name: String, expr: Expr)
}

enum Rel {
  case Scan(table: String)
  case Select(source: Rel, cols: List[Expr])
}
