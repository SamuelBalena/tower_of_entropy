#version 330 compatibility
#include "/lib/options.glsl"
#include "/lib/smaa/integration.glsl"

uniform sampler2D colortex1, smaaArea, smaaSearch;
varying vec2 uv;
const int RGBA8 = 32856;
const int colortex2Format = RGBA8;
/* DRAWBUFFERS:2 */
void main() {
    vec2 pixel;
    vec4 offsets[3];
    SMAABlendingWeightCalculationVS(uv, pixel, offsets);
    gl_FragData[0] = SMAABlendingWeightCalculationPS(
        uv, pixel, offsets, colortex1, smaaArea, smaaSearch, vec4(0.0));
}
