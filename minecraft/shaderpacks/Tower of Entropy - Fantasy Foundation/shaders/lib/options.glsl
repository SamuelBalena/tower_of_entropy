#define SHADOW_RESOLUTION 1024 // [512 1024 2048]
#define SHADOW_FILTER 1 // [0 1 2]
#define LAVA_DETAIL 1 // [0 1 2]
#define WATER_DETAIL 1 // [0 1 2]
#define WATER_SSR 1 // [0 1]
#define EXPOSURE 1.00 // [0.85 0.95 1.00 1.05 1.15]
#define SATURATION 1.12 // [1.00 1.06 1.12 1.18 1.24]
#define CONTRAST 1.05 // [0.95 1.00 1.05 1.10 1.15]
#define MAGIC 0.60 // [0.00 0.30 0.60 1.00]
const int shadowMapResolution = SHADOW_RESOLUTION;
#if SHADOW_RESOLUTION == 512
const float shadowDistance = 44.0;
#elif SHADOW_RESOLUTION == 2048
const float shadowDistance = 100.0;
#else
const float shadowDistance = 68.0;
#endif
const float shadowDistanceRenderMul = 1.0;
const bool shadowtex0Nearest = true;
