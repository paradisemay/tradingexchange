// read_itmoex.c
#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>

#define DEVICE_PATH "/dev/itmoex"
#define STOCKS_COUNT 242

struct mock {
    char ticker[8];
    int price;   // x100
};

int main(void)
{
    int fd = open(DEVICE_PATH, O_RDONLY);
    if (fd < 0) {
        perror("open");
        return 1;
    }

    size_t batch_size = STOCKS_COUNT * sizeof(struct mock);

    struct mock *records = malloc(batch_size);
    if (!records) {
        perror("malloc");
        close(fd);
        return 1;
    }

    ssize_t n = read(fd, records, batch_size);
    if (n < 0) {
        perror("read");
        free(records);
        close(fd);
        return 1;
    }

    close(fd);

    size_t count = n / sizeof(struct mock);

    for (size_t i = 0; i < count; i++) {
        printf("%.*s %d.%02d\n",
               8,
               records[i].ticker,
               records[i].price / 100,
               abs(records[i].price % 100));
    }

    free(records);
    return 0;
}