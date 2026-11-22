uniform float viewWidth, viewHeight;
#define SMAA_GLSL_3 1
#define SMAA_RT_METRICS vec4(1.0 / viewWidth, 1.0 / viewHeight, viewWidth, viewHeight)
#if AA_QUALITY == 3
#define SMAA_PRESET_ULTRA
#else
#define SMAA_PRESET_HIGH
#endif
#include "/lib/smaa/reference.glsl"
