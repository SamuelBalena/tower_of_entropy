#version 150
uniform sampler2D PlayerColor;
uniform sampler2D PlayerDepth;
uniform float Opacity;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec4 color = texture(PlayerColor, texCoord);
    if (color.a < 0.001) discard;
    gl_FragDepth = texture(PlayerDepth, texCoord).r;
    fragColor = color * Opacity;
}
