#version 120
#include "/lib/options.glsl"
#include "/lib/atmosphere.glsl"
#if WATER_DETAIL == 0
#define REFLECTION_SKY_WITHOUT_CLOUDS
#endif
#include "/lib/skydome.glsl"
uniform mat4 gbufferModelViewInverse;
varying vec2 uv;
/* DRAWBUFFERS:5 */
void main() {
    float azimuth = (uv.x - 0.5) * 6.2831853;
    float altitude = uv.y * 1.5707963;
    vec3 ray = vec3(cos(azimuth) * cos(altitude), sin(altitude), sin(azimuth) * cos(altitude));
    vec3 sun = normalize(mat3(gbufferModelViewInverse) * sunPosition);
    vec3 moon = normalize(mat3(gbufferModelViewInverse) * moonPosition);
    vec3 sky = renderSkydome(ray, sun, moon);
    gl_FragData[0] = vec4(sky, 1.0);
}
