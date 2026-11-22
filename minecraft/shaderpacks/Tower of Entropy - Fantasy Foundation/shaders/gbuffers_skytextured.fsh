#version 120
#include "/lib/options.glsl"
uniform sampler2D gtexture;
varying vec3 ray;
varying vec2 uv;
varying vec4 tint;
#if DIMENSION_FOG > 0
#include "/lib/dimension_atmosphere.glsl"
#endif
/* DRAWBUFFERS:0 */
void main() {
    vec4 texel = texture2D(gtexture, uv) * tint;
#if DIMENSION_FOG > 0
    if (toeEnd > 0.5) {
        vec3 sky = endSky(normalize(ray));
        texel = vec4(sky + (texel.rgb - 0.16) * 0.055, tint.a);
    }
#endif
    gl_FragData[0] = texel;
}
