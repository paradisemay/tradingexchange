obj-m += itmoex.o
itmoex-objs := main.o perlin.o

KDIR ?= /lib/modules/$(shell uname -r)/build
PWD := $(shell pwd)
BUILD_DIR := $(PWD)/build

RELOAD ?= 1

all:
	$(MAKE) -C $(KDIR) M=$(PWD) modules
	@if [ "$(RELOAD)" = "1" ]; then \
		echo "Reloading module..."; \
		sudo rmmod itmoex; \
	fi
	sudo insmod itmoex.ko
	mkdir -p $(BUILD_DIR)
	mv -f *.o *.ko *.mod.c *.symvers *.order *.*.cmd .*.*.cmd .*.o.cmd ..*.o.cmd .*.ko.cmd .*-*.o $(BUILD_DIR) 2>/dev/null || true

clean:
	$(MAKE) -C $(KDIR) M=$(PWD) clean

rmmod:
	sudo rmmod itmoex
