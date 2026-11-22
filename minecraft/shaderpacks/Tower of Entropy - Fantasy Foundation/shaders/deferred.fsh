#version 120
#include "/lib/options.glsl"
uniform sampler2D colortex0;
varying vec2 uv;
#if SCREEN_AO > 0 || CONTACT_SHADOWS > 0 || SOLID_REFLECTIONS > 0
uniform sampler2D depthtex0;
uniform int isEyeInWater;
#include "/lib/screen_projection.glsl"
#endif
#if SCREEN_AO > 0 || CONTACT_SHADOWS > 0
uniform mat4 gbufferModelViewInverse;
#include "/lib/screen_occlusion.glsl"
#endif
#if SOLID_REFLECTIONS > 0
#include "/lib/solid_reflections.glsl"
#endif
#if SCREEN_AO > 0 || CONTACT_SHADOWS > 0 || SOLID_REFLECTIONS > 0
/* DRAWBUFFERS:40 */
#else
/* DRAWBUFFERS:4 */
#endif
void main() {
    vec4 scene = texture2D(colortex0, uv);
#if SCREEN_AO > 0 || CONTACT_SHADOWS > 0 || SOLID_REFLECTIONS > 0
    float depth = texture2D(depthtex0, uv).r;
    if (depth < 0.999999 && isEyeInWater == 0) {
        vec3 position = screenViewPosition(uv, depth);
#if SCREEN_AO > 0 || CONTACT_SHADOWS > 0
        scene.rgb *= screenVisibility(uv, position);
#endif
#if SOLID_REFLECTIONS > 0
        scene.rgb += solidReflection(uv, position);
#endif
    }
#endif
    gl_FragData[0] = scene;
#if SCREEN_AO > 0 || CONTACT_SHADOWS > 0 || SOLID_REFLECTIONS > 0
    gl_FragData[1] = scene;
#endif
}
