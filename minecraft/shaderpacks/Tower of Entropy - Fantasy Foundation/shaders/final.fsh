#version 330 compatibility
#include "/lib/options.glsl"
uniform sampler2D colortex0, depthtex0;
uniform mat4 gbufferProjectionInverse;
uniform int isEyeInWater;
varying vec2 uv;
#if ANTIALIASING == 1
#include "/lib/fxaa.glsl"
#elif ANTIALIASING == 2
#include "/lib/smaa/integration.glsl"
uniform sampler2D colortex2;
#endif

void main() {
    // Resolve only spatially detected edges before grading the current frame.
    vec3 source = texture2D(colortex0, uv).rgb;
#if ANTIALIASING == 1
    source = resolveFxaa(uv, source);
#elif ANTIALIASING == 2
    vec4 offset;
    SMAANeighborhoodBlendingVS(uv, offset);
    source = SMAANeighborhoodBlendingPS(uv, offset, colortex0, colortex2).rgb;
#endif
    vec3 color = source * EXPOSURE;
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
