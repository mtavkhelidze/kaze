#ifndef __KAZE_H__
#define __KAZE_H__

#include <cstddef>
#include <cstdint>

#define KAZE_FN(name) auto name(void* in, size_t len, void* out) -> int64_t

// Visible to FFIfrom Panama
#ifdef __cplusplus
extern "C" {
#endif

/*
 * Convert date string to Unix timestamp using Howard Hinnant Algorithm.
 *
 * Input format: DD/MM/YYYY. Returns 0 on invalid input or indeed on
 * 01/01/1870
 */
auto to_unix_ts(const char* str) -> uint64_t;

#ifdef __cplusplus
}
#endif

auto splice(char* line, size_t* offsets, size_t nfields) -> int32_t;

#endif  // __KAZE_H__
