#version 120
#include "/lib/options.glsl"
#include "/lib/atmosphere.glsl"
uniform int renderStage;
uniform mat4 gbufferModelViewInverse;
varying vec3 ray;
varying vec4 tint;
/* DRAWBUFFERS:0 */
void main() {
    vec3 direction = normalize(ray);
    vec3 sky = skyGradient(direction);
    vec3 sunDirection = normalize(mat3(gbufferModelViewInverse) * sunPosition);
    float halo = pow(max(dot(direction, sunDirection), 0.0), 12.0);
    sky += vec3(0.26, 0.13, 0.044) * halo * goldenHour() * smoothstep(-0.12, 0.07, sunDirection.y);
#ifdef MC_RENDER_STAGE_STARS
    if (renderStage == MC_RENDER_STAGE_STARS) sky = tint.rgb;
#endif
#ifdef MC_RENDER_STAGE_CUSTOM_SKY
    if (renderStage == MC_RENDER_STAGE_CUSTOM_SKY) sky = tint.rgb;
#endif
    gl_FragData[0] = vec4(sky, tint.a);
}
