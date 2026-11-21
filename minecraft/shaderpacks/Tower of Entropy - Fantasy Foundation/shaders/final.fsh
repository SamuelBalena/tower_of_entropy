#version 120
#include "/lib/options.glsl"
uniform sampler2D colortex0;
varying vec2 uv;

void main() {
    // Grade the resolved pixel in place. Sampling displaced scene colors creates duplicate edges.
    vec3 color = texture2D(colortex0, uv).rgb * EXPOSURE;
    float y = dot(color, vec3(0.2126, 0.7152, 0.0722));
    color = mix(vec3(y), color, SATURATION);
    color = (color - 0.45) * CONTRAST + 0.45;
    color += vec3(0.016, 0.021, 0.034) * (1.0 - clamp(y, 0.0, 1.0)) * MAGIC;
    gl_FragData[0] = vec4(clamp(color, 0.0, 1.0), 1.0);
}
