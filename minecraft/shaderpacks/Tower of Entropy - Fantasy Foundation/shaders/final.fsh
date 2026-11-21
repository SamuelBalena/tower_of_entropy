#version 120
#include "/lib/options.glsl"
uniform sampler2D colortex0, depthtex0;
uniform mat4 gbufferProjectionInverse;
uniform int isEyeInWater;
varying vec2 uv;

void main() {
    // Grade the resolved pixel in place. Sampling displaced scene colors creates duplicate edges.
    vec3 color = texture2D(colortex0, uv).rgb * EXPOSURE;
    if (isEyeInWater == 1) {
        float depth = min(texture2D(depthtex0, uv).r, 0.999998);
        vec4 point = gbufferProjectionInverse * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
        float travel = min(length(point.xyz / point.w), 52.0);
        vec3 transmission = exp(-travel * vec3(0.073, 0.036, 0.021));
        color = color * transmission + vec3(0.028, 0.18, 0.22) * (1.0 - transmission) * EXPOSURE;
    }
    float y = dot(color, vec3(0.2126, 0.7152, 0.0722));
    color = mix(vec3(y), color, SATURATION);
    color = (color - 0.45) * CONTRAST + 0.45;
    color += vec3(0.016, 0.021, 0.034) * (1.0 - clamp(y, 0.0, 1.0)) * MAGIC;
    gl_FragData[0] = vec4(clamp(color, 0.0, 1.0), 1.0);
}
