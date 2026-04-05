#include <iostream>

#include "kaze.h"
int main() {
    char line[] = "B1,27/07/2024,Anderlecht,1";
    size_t offsets[4];

    int32_t n = splice(line, offsets, 4);

    printf("fields: %d\n", n);
    printf("div:   %s\n", line + offsets[0]);
    printf("date:  %s\n", line + offsets[1]);
    printf("team:  %s\n", line + offsets[2]);
    printf("goals: %s\n", line + offsets[3]);

    return 0;
}
