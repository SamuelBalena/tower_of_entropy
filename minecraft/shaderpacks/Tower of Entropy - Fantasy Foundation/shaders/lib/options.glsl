#define SHADOW_RESOLUTION 1024 // [512 1024 2048]
#define SHADOW_FILTER 1 // [0 1 2]
#define LAVA_DETAIL 1 // [0 1 2]
#define ANTIALIASING 2 // [0 1 2]
#define AA_QUALITY 2 // [1 2 3]
#define AA_STRENGTH 0.85 // [0.60 0.75 0.85 1.00]
#define MATERIAL_QUALITY 2 // [0 1 2 3]
#define MATERIAL_SPECULAR 0.80 // [0.00 0.40 0.60 0.80 1.00]
#define MATERIAL_NORMAL 0.75 // [0.00 0.35 0.55 0.75 1.00]
#define MATERIAL_LABPBR 1 // [0 1]
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

// Regional sky
#define AURORA 1 // [0 1]
#define AURORA_FREQUENCY 0.65 // [0.25 0.40 0.50 0.65 0.80 1.00]
#define AURORA_INTENSITY 0.85 // [0.45 0.60 0.75 0.85 1.00 1.20]
#define AURORA_SPEED 1.0 // [0.5 0.75 1.0 1.25 1.5]
#define AURORA_QUALITY 2 // [1 2 3]

// Atmospheric extinction
#define FOG_MODE 4 // [0 1 2 3 4]
#define FOG_QUALITY 2 // [1 2 3]
#define FOG_INTENSITY 1.0 // [0.50 0.65 0.80 1.0 1.20 1.40]
