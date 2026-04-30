// hello_arm_driver.c
#include <linux/module.h>
#include <linux/kernel.h>
#include <linux/init.h>
#include <linux/uaccess.h>
#include <linux/device.h>
#include "perlin.h"

#define DEVICE_NAME "itmoex"
#define BUFFER_SIZE 10000
#define SUCCESS 0
#define STOCKS_COUNT (sizeof(stocks) / sizeof(stocks[0]))
#define MARKET_AMPLITUDE 10

/*
  _____ _______ __  __  ____  ________   __
 |_   _|__   __|  \/  |/ __ \|  ____\ \ / /
   | |    | |  | \  / | |  | | |__   \ V / 
   | |    | |  | |\/| | |  | |  __|   > <  
  _| |_   | |  | |  | | |__| | |____ / . \ 
 |_____|  |_|  |_|  |_|\____/|______/_/ \_\
                                           
*/



struct mock {
	char ticker[8];
	int price; //x100
}; 

// Initial stocks
struct mock stocks[] = {
    {"FEES", 7}, {"SNGS", 1970}, {"SNGSP", 4163}, {"VTBR", 9286},
    {"RNFT", 11140}, {"HYDR", 39}, {"ELFV", 48}, {"AFLT", 4751},
    {"GAZP", 11904}, {"EUTR", 8290}, {"URKZ", 26340}, {"UPRO", 139},
    {"MSRS", 200}, {"KRKOP", 1462}, {"SFIN", 99760}, {"MRKZ", 13},
    {"IRAO", 314}, {"TGKB", 1}, {"TGKBP", 1}, {"TNSE", 388500},
    {"VGSB", 1485}, {"VGSBP", 1245}, {"GAZA", 48100}, {"GAZAP", 37050},
    {"IGSTP", 318000}, {"IGST", 416000}, {"MRKP", 64}, {"KBSB", 90900},
    {"WTCM", 1398}, {"WTCMP", 1248}, {"MRKV", 19}, {"MRKC", 90},
    {"ZAYM", 13890}, {"UWGN", 2412}, {"MBNK", 129350}, {"MGTS", 105400},
    {"MGTSP", 74000}, {"SVET", 1200}, {"SVETP", 2330}, {"DOMRF", 223160},
    {"MRKU", 60}, {"NMTP", 832}, {"BSPB", 33632}, {"BSPBP", 4960},
    {"TRNFP", 136100}, {"SBER", 32009}, {"SBERP", 32000}, {"T", 30650},
    {"MGKL", 243}, {"DVEC", 164}, {"RENI", 8596}, {"ZILL", 434500},
    {"RDRB", 11740}, {"STSB", 458}, {"STSBP", 564}, {"MRSB", 86},
    {"BISVP", 1029}, {"LSNG", 1475}, {"LSNGP", 35490}, {"SARE", 74},
    {"SAREP", 40}, {"NKNC", 7125}, {"NKNCP", 5024}, {"ASSB", 310},
    {"GCHE", 336000}, {"RAGR", 10006}, {"CHKZ", 1705000}, {"LIFE", 291},
    {"SVCB", 1199}, {"BANE", 144550}, {"BANEP", 99200}, {"GLRX", 5798},
    {"OKEY", 3998}, {"EELT", 850}, {"SAGO", 267}, {"SAGOP", 254},
    {"SLEN", 335}, {"ALRS", 2775}, {"PRFN", 397}, {"KMEZ", 112400},
    {"OGKB", 31}, {"LSRG", 62380}, {"FIXR", 67}, {"SVAV", 45500},
    {"BTBR", 13000}, {"JNOS", 4688}, {"JNOSP", 1932}, {"LPSB", 6060},
    {"MOEX", 16911}, {"APRI", 1528}, {"TTLK", 69}, {"RTSB", 310},
    {"RTSBP", 144}, {"RTGZ", 4080000}, {"MISB", 5140}, {"MISBP", 5520},
    {"TGKA", 1}, {"LENT", 208000}, {"HNFG", 40040}, {"DATA", 9854},
    {"KUZB", 3}, {"NAUK", 50750}, {"TASB", 206}, {"TASBP", 99},
    {"PHOR", 673300}, {"KRSB", 2258}, {"KRSBP", 2406}, {"HEAD", 292500},
    {"RZSB", 3860}, {"NNSB", 382000}, {"NNSBP", 128000}, {"OMZZP", 1082000},
    {"NLMK", 8696}, {"DIAS", 141150}, {"ABRD", 14680}, {"POSI", 101140},
    {"ROSN", 43070}, {"TORS", 69}, {"TORSP", 50}, {"X5", 235400},
    {"BAZA", 11476}, {"BELU", 35900}, {"KRKN", 1105000}, {"KRKNP", 686000},
    {"OZPH", 4711}, {"IVAT", 12750}, {"TATN", 58350}, {"TATNP", 54760},
    {"AVAN", 64600}, {"PLZL", 206920}, {"MDMG", 132150}, {"MRKY", 007},
    {"BRZL", 158200}, {"PMSB", 57300}, {"PMSBP", 58780}, {"NKHP", 48200},
    {"SIBN", 50970}, {"USBN", 12}, {"GMKN", 12862}, {"MGNT", 253300},
    {"CBOM", 646}, {"ASTR", 28090}, {"RTKM", 5412}, {"RTKMP", 5675},
    {"VSEH", 7115}, {"RGSS", 19}, {"VRSB", 35550}, {"VRSBP", 13300},
    {"MTSS", 22030}, {"GEMC", 80630}, {"DZRD", 179500}, {"DZRDP", 192000},
    {"MSNG", 195}, {"GEMA", 12430}, {"SOFL", 6730}, {"KAZT", 38220},
    {"KAZTP", 39400}, {"PRMD", 40565}, {"YDEX", 401350}, {"KLSB", 1973},
    {"LEAS", 60330}, {"KZOS", 5720}, {"KZOSP", 1421}, {"ENPG", 40960},
    {"UGLD", 74}, {"NSVZ", 24550}, {"YRSB", 78200}, {"YRSBP", 19550},
    {"CNRU", 60600}, {"LVHK", 2315}, {"PIKK", 53500}, {"AKRN", 1940200},
    {"NVTK", 113550}, {"VLHZ", 16320}, {"MSTT", 9955}, {"ABIO", 5840},
    {"KFBA", 174500}, {"TGKN", 1}, {"LMBZ", 40850}, {"LNZL", 126000},
    {"LNZLP", 18400}, {"RKKE", 1390000}, {"KLVZ", 286}, {"MRKS", 48},
    {"SPBE", 20290}, {"YAKG", 4295}, {"VSMO", 2464000}, {"GAZC", 65900},
    {"IRKT", 2126}, {"GAZS", 83900}, {"CHMF", 75960}, {"OZON", 417000},
    {"HIMCP", 1794}, {"RBCM", 920}, {"VSYD", 995000}, {"VSYDP", 730000},
    {"AMEZ", 6805}, {"GAZT", 173900}, {"APTK", 718}, {"CARM", 116},
    {"ARSA", 791}, {"KCHE", 37}, {"KCHEP", 69}, {"PAZA", 916000},
    {"NKSH", 3800}, {"FESH", 7177}, {"KGKC", 4320}, {"KGKCP", 5120},
    {"UNKL", 421000}, {"CHGZ", 12460}, {"VJGZ", 432500}, {"VJGZP", 176800},
    {"UNAC", 39}, {"ELMT", 14}, {"RUSI", 6750}, {"GECO", 2008},
    {"UTAR", 1074}, {"RUAL", 3922}, {"SMLT", 59120}, {"AQUA", 37140},
    {"MAGN", 2522}, {"DIOD", 941}, {"ZVEZ", 651}, {"BLNG", 1067},
    {"KROT", 113200}, {"KROTP", 47500}, {"VKCO", 24650}, {"SELG", 4574},
    {"PRMB", 2540000}, {"MFGS", 43700}, {"MFGSP", 30550}, {"CNTL", 906},
    {"CNTLP", 552}, {"KOGK", 29680}, {"NFAZ", 25800}, {"TRMK", 8466},
    {"DELI", 7800}, {"LKOH", 547800}, {"FLOT", 7975}, {"MRKK", 1916},
    {"ROST", 8520}, {"WUSH", 7831}
};


static int Major;
static size_t buffer_pos = 0;
static struct mock Buffer[BUFFER_SIZE];
static int Device_Open;


static ssize_t itmoex_read(struct file *, char *, size_t, loff_t *);

static struct class *itmoex_class;
static struct device *itmoex_device;


static struct file_operations fops = {
	.read = itmoex_read,
};


void generate_initial_stock_map(void){
    for (int i = 0; i < STOCKS_COUNT; i++){
        Buffer[i] = stocks[i];
    }

    for (int i = STOCKS_COUNT; i < BUFFER_SIZE; i++) {
        int ticker_id = i % STOCKS_COUNT;

        Buffer[i] = Buffer[i - STOCKS_COUNT];

        Buffer[i].price = apply_price_noise(
            Buffer[i - STOCKS_COUNT].price,
            stocks[ticker_id].price,
            i / STOCKS_COUNT,
            ticker_id
        );
    }
}

void renew_stock_map(void){
    for (int i = BUFFER_SIZE - STOCKS_COUNT; i < BUFFER_SIZE; i++){
        stocks[i - (BUFFER_SIZE - STOCKS_COUNT)] = Buffer[i];
    }
}


static int __init itmoex_init(void)
{   
    generate_initial_stock_map();

    Major = register_chrdev(0, DEVICE_NAME, &fops);
    if (Major < 0) {
	  printk(KERN_ALERT "Registering char device failed with %d\n", Major);
	  return Major;
	}

    itmoex_class = class_create("itmoex_class");
    if (IS_ERR(itmoex_class)) {
        unregister_chrdev(Major, DEVICE_NAME);
        return PTR_ERR(itmoex_class);
    }

    itmoex_device = device_create(itmoex_class,
                                  NULL,
                                  MKDEV(Major, 0),
                                  NULL,
                                  "itmoex");

    if (IS_ERR(itmoex_device)) {
        class_destroy(itmoex_class);
        unregister_chrdev(Major, DEVICE_NAME);
        return PTR_ERR(itmoex_device);
    }
    

    pr_info("ITMOEX Driver Welcomes you!\n");
    return 0;
}

static void __exit itmoex_exit(void)
{
    device_destroy(itmoex_class, MKDEV(Major, 0));
    class_destroy(itmoex_class);
    unregister_chrdev(Major, DEVICE_NAME);

    pr_info("Goodbye from ITMOEX driver!\n");
}


static ssize_t itmoex_read(struct file *filp,
			   char *user_buf,	/* buffer to fill with data */
			   size_t length,	/* length of the buffer     */
			   loff_t * offset)
{
    if (length <  STOCKS_COUNT * sizeof(struct mock))
        return -EINVAL;

    if (buffer_pos + STOCKS_COUNT > BUFFER_SIZE){
        buffer_pos = 0;
        
        renew_stock_map();
        generate_initial_stock_map();
    }

    if (copy_to_user(user_buf,
                     &Buffer[buffer_pos],
                     STOCKS_COUNT * sizeof(struct mock)))
        return -EFAULT;

    *offset += STOCKS_COUNT * sizeof(struct mock);
    buffer_pos += STOCKS_COUNT;

    return STOCKS_COUNT * sizeof(struct mock);
}


module_init(itmoex_init);
module_exit(itmoex_exit);


MODULE_LICENSE("GPL");
MODULE_AUTHOR("Kholin Nikita");
MODULE_DESCRIPTION("ITMOEX Driver");
MODULE_VERSION("1.0");