#include "perlin.h"

#define FP_SHIFT 8
#define FP_ONE   (1 << FP_SHIFT)

#define MIN_PRICE_X100 1
#define MARKET_AMPLITUDE 20

static int SEED = 0;

static int hash[] = {
    208,34,231,213,32,248,233,56,161,78,24,140,71,48,140,254,
    245,255,247,247,40,185,248,251,245,28,124,204,204,76,36,1,
    107,28,234,163,202,224,245,128,167,204,9,92,217,54,239,174,
    173,102,193,189,190,121,100,108,167,44,43,77,180,204,8,81,
    70,223,11,38,24,254,210,210,177,32,81,195,243,125,8,169,
    112,32,97,53,195,13,203,9,47,104,125,117,114,124,165,203,
    181,235,193,206,70,180,174,0,167,181,41,164,30,116,127,198,
    245,146,87,224,149,206,57,4,192,210,65,210,129,240,178,105,
    228,108,245,148,140,40,35,195,38,58,65,207,215,253,65,85,
    208,76,62,3,237,55,89,232,50,217,64,244,157,199,121,252,
    90,17,212,203,149,152,140,187,234,177,73,174,193,100,192,143,
    97,53,145,135,19,103,13,90,135,151,199,91,239,247,33,39,
    145,101,120,99,3,186,86,99,41,237,203,111,79,220,135,158,
    42,30,154,120,67,87,167,135,176,183,191,253,115,184,21,233,
    58,129,233,142,39,128,211,118,137,139,255,114,20,218,113,154,
    27,127,246,250,1,8,198,250,209,92,222,173,21,88,102,219
};

static int noise2(int x, int y)
{
    int tmp = hash[(y + SEED) & 255];
    return hash[(tmp + x) & 255];
}

static int lerp_fp(int a, int b, int t)
{
    return a + (((b - a) * t) >> FP_SHIFT);
}

static int fade_fp(int t)
{
    return (t * t * (3 * FP_ONE - 2 * t)) >> (FP_SHIFT * 2);
}

static int noise2d_fp(int x_fp, int y_fp)
{
    int x0 = x_fp >> FP_SHIFT;
    int y0 = y_fp >> FP_SHIFT;

    int xf = x_fp & (FP_ONE - 1);
    int yf = y_fp & (FP_ONE - 1);

    int s = noise2(x0,     y0);
    int t = noise2(x0 + 1, y0);
    int u = noise2(x0,     y0 + 1);
    int v = noise2(x0 + 1, y0 + 1);

    int sx = fade_fp(xf);
    int sy = fade_fp(yf);

    int low  = lerp_fp(s, t, sx);
    int high = lerp_fp(u, v, sx);

    return lerp_fp(low, high, sy);
}

/*
 * Return range: approximately [-256; 256]
 */
int perlin2d(int x, int y, int freq_fp, int depth)
{
    int xa = x * freq_fp;
    int ya = y * freq_fp;

    int amp = FP_ONE;
    int fin = 0;
    int div = 0;

    int i;

    for (i = 0; i < depth; i++) {
        int n = noise2d_fp(xa, ya);

        fin += n * amp;
        div += 255 * amp;

        amp >>= 1;
        xa <<= 1;
        ya <<= 1;
    }

    if (div == 0)
        return 0;

    return ((fin * 512) / div) - 256;
}

int apply_price_noise(int prev_price, int base_price, int step, int ticker_id)
{
    int slow;
    int fast;
    int wave;
    int trend;
    int price;

    slow = perlin2d(step, ticker_id, FP_ONE / 32, 4);
    fast = perlin2d(step, ticker_id + 97, FP_ONE / 4, 2);

    wave = (slow * MARKET_AMPLITUDE * base_price) / (256 * 100);
    trend = (fast * MARKET_AMPLITUDE * base_price) / (1024 * 100);

    price = base_price + wave + trend;

    if (price < MIN_PRICE_X100)
        price = MIN_PRICE_X100;

    return price;
}