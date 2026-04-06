## Kaze

#### __Compiled query engine__

This is a fast end-to-end compiled query engine prototype that turns a custom
DSL or SQL-like query into native C++ execution with zero-copy streaming, built
from parser to runtime.

> In Japanese kaze (風邪 / かぜ) means wind and also influenza, essentially the
> pain in the neck which building integrated systems like this indeed is.

#### Components
- **BunKaze** (文風) — DSL: Expr, Rel, Func
- **KamiKaze** (神風) — Rel tree → C++ → .so
- **KisokuKaze** (規則) - config
- **YomiKaze** (読み風) — JSON → Rel tree
- **TheTailor (of Panama)** — FFI bridge, bespoke suit per query

#### The pipeline:

```
    JSON DSL → 
    YomiKaze (parser) → 
    AST →
    KamiKaze (code generator) →
    C++ codegen →
    cmake →
    libkaze.so →
    TheTailor (native loader) →
    FFI →
    fs2 stream
```
