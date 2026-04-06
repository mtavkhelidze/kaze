### Native Function Conventions (ABI)

1. We only recognize two types: `Long / int64_t` and `char*`. We also recognize
   their vectors: `int64_t*` and `char**`.
2. Return type is always `Long / int64_t`. It can be either:
    - 0: success
    - < 0: error with different negative values meaning different errors
3. Arguments are always `char**` and number of them, number >= 0. Exactly like
   in textbook declaration of `main`.
    - **IMPORTANT**: arguments might be modified in place, so after the call
      they are in undefined state
    - All necessary convertions are performed by the native side
4. Return value is place in the caller's preallocated buffer. It's a caller's
   The return value is always a struct where the first member (8L or 64 bits)
   are reserved for number of fields in the given struct. It is the caller's
   responsibility to:
    - Allocate enough memory
    - Correctly interpret the return values

For example:

```c
typedef struct {
    size_t nfields;
    const char* name;
    const char* value;
} Row;

int foo(const char** argv, size_t argc, Row* out);
```
