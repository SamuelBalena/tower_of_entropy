#version 120
uniform sampler2D gtexture;
varying vec2 uv;
varying float opacity;
/* DRAWBUFFERS: */
void main() {
    if (texture2D(gtexture, uv).a * opacity < 0.45) discard;
}
