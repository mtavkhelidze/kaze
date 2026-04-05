
#include <cstdint>

#include "kaze.h"

// NOLINTBEGIN(cppcoreguidelines-avoid-magic-numbers,readability-magic-numbers,cppcoreguidelines-pro-bounds-pointer-arithmetic)

static inline const auto num = [](char c) noexcept -> int { return c - '0'; };

/* Convert civil date to days since Unix epoch (1970-01-01)
 * Algorithm by Howard Hinnant:
 * https://howardhinnant.github.io/date_algorithms.html#days_from_civil
 */
static inline auto days_from_civil(int y, unsigned m, unsigned d) noexcept
  -> int {
    y -= static_cast<int>(m <= 2);
    const int era = (y >= 0 ? y : y - 399) / 400;
    // [0, 399]
    const auto yoe = static_cast<unsigned>(y - (era * 400));
    // [0, 365]
    const unsigned doy = ((153 * (m + (m > 2 ? -3 : 9)) + 2) / 5) + d - 1;
    // [0, 146096]
    const unsigned doe = (yoe * 365) + (yoe / 4) - (yoe / 100) + doy;
    // days since epoch
    return (era * 146097) + static_cast<int>(doe) - 719468;
}

auto to_unix_ts(const char* str) -> uint64_t {
    if (str == nullptr) {
        return 0;
    }

    if (str[2] != '/' || str[5] != '/') {
        return 0;
    }

    int d = (num(str[0]) * 10) + num(str[1]);
    int m = (num(str[3]) * 10) + num(str[4]);
    int y = (num(str[6]) * 1000)
          + (num(str[7]) * 100)
          + (num(str[8]) * 10)
          + num(str[9]);

    if (m < 1 || m > 12) {
        return 0;
    }
    if (d < 1 || d > 31) {
        return 0;
    }

    // Convert to epoch seconds
    int64_t days =
      days_from_civil(y, static_cast<unsigned>(m), static_cast<unsigned>(d));
    int64_t secs = days * 86400;

    return secs < 0 ? 0 : static_cast<uint64_t>(secs);
}

// NOLINTEND(cppcoreguidelines-avoid-magic-numbers,readability-magic-numbers,cppcoreguidelines-pro-bounds-pointer-arithmetic)
