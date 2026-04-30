# Запуск
```
cd ./itmoex
make
```
# Чтение 

Чтение просиходит из файла драйвера `/dev/itmoex` структуры 
```
struct mock { 
	char ticker[8];
	int price;
};
```
**ВНИМАНИЕ!** цену после получение нужно поделить на 100.
Пример в `reader.c`



![alt text](https://sun9-1.vkuserphoto.ru/s/v1/ig2/pexXOPVNff25bbxgqJMGrJ7bxhGpupNIOxHSJeLGpb6eUg5FnTJNGYxngHdc2Xvms_y4li6veN8KlhLPJy0w-avB.jpg?quality=95&as=32x32,48x48,72x72,108x108,160x160,240x240,360x360,480x480,540x540,640x640,720x720,1080x1080,1254x1254&from=bu&u=hVi3iFa72XSLnS3Z6u1FORByrxaa3VUAaly_Ds6_KRs&cs=1254x0 "Logo Title Text 1")
