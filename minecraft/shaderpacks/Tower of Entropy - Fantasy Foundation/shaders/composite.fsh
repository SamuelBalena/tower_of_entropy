#version 330 compatibility
#include "/lib/options.glsl"
#include "/lib/smaa/integration.glsl"

uniform sampler2D colortex0;
varying vec2 uv;
const int RG8 = 33323;
const int colortex1Format = RG8;
const bool colortex1Clear = true;
const vec4 colortex1ClearColor = vec4(0.0, 0.0, 0.0, 0.0);
/* DRAWBUFFERS:1 */
void main() {
    vec4 offsets[3];
    SMAAEdgeDetectionVS(uv, offsets);
    gl_FragData[0] = vec4(SMAALumaEdgeDetectionPS(uv, offsets, colortex0), 0.0, 0.0);
}
