#version 120
uniform sampler2D colortex0;
varying vec2 uv;
/* DRAWBUFFERS:4 */
void main() {
    gl_FragData[0] = texture2D(colortex0, uv);
}
