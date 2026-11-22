#version 120
#include "/lib/options.glsl"
#include "/lib/atmosphere.glsl"
#define TOE_MAIN_SKY
#include "/lib/skydome.glsl"
uniform int renderStage;
uniform mat4 gbufferModelViewInverse;
varying vec3 ray;
varying vec4 tint;
/* DRAWBUFFERS:0 */
void main() {
#ifdef MC_RENDER_STAGE_SUNSET
    if (renderStage == MC_RENDER_STAGE_SUNSET) discard;
#endif
#ifdef MC_RENDER_STAGE_STARS
    if (renderStage == MC_RENDER_STAGE_STARS) discard;
#endif
#ifdef MC_RENDER_STAGE_CUSTOM_SKY
    if (renderStage == MC_RENDER_STAGE_CUSTOM_SKY
#if DIMENSION_FOG > 0
        && toeEnd < 0.5
#endif
    ) {
        gl_FragData[0] = tint;
        return;
    }
#endif
    vec3 direction = normalize(ray);
    vec3 sun = normalize(mat3(gbufferModelViewInverse) * sunPosition);
    vec3 moon = normalize(mat3(gbufferModelViewInverse) * moonPosition);
    gl_FragData[0] = vec4(renderSkydome(direction, sun, moon), tint.a);
}
