uniform sampler2D shadowtex0;
uniform mat4 shadowProjection;
varying vec3 shadowViewPos, shadowViewNormal;

#if ADVANCED_SHADOWS >= 2 && SHADOW_FILTER > 0
float penumbraScale(vec3 coord, vec2 texel, vec2 gradient, float bias) {
    float blockers = 0.0;
    float separation = 0.0;
    for (int i = 0; i < 4; ++i) {
        vec2 delta = vec2(mod(float(i), 2.0) * 2.0 - 1.0, floor(float(i) * 0.5) * 2.0 - 1.0)
                   * texel * 1.5;
        float gap = coord.z + dot(delta, gradient) - bias - texture2D(shadowtex0, coord.xy + delta).r;
        float blocker = step(0.00005, gap);
        separation += max(gap, 0.0);
        blockers += blocker;
    }
    float distance = separation / max(blockers, 1.0) * (2.0 / abs(shadowProjection[2][2]));
    return mix(1.0, 1.3 + 0.45 * float(ADVANCED_SHADOWS), smoothstep(0.12, 4.5, distance));
}
#endif

float compareShadow(vec2 uv, float depth) {
    return step(depth, texture2D(shadowtex0, uv).r);
}

float sunVisibility(vec3 viewPos, float incidence) {
    vec3 normal = normalize(shadowViewNormal);
    float slope = sqrt(max(1.0 - incidence * incidence, 0.0));
    float normalBias = mix(0.012, 0.045, slope);
#if ADVANCED_SHADOWS > 0 && SHADOW_FILTER > 0
    normalBias *= mix(0.95, 1.10, slope);
#endif
    vec3 receiver = shadowViewPos + normal * normalBias;
    vec4 projected = shadowProjection * vec4(receiver, 1.0);
    vec3 coord = projected.xyz / projected.w * 0.5 + 0.5;
    vec2 texel = vec2(1.0 / float(SHADOW_RESOLUTION));
    vec2 border = min(coord.xy, 1.0 - coord.xy);
    float edge = min(border.x, border.y);
    float range = 1.0 - smoothstep(shadowDistance * 0.78, shadowDistance, length(viewPos));
    float guard = 2.0 * texel.x;
#if ADVANCED_SHADOWS >= 2 && SHADOW_FILTER > 0
    guard = 4.0 * texel.x;
#endif
    if (edge < guard || coord.z <= 0.0 || coord.z >= 1.0 || range <= 0.0) return 1.0;

    // Compare with the plane's depth at the center of each sampled texel.
    // With an orthographic shadow projection, this is the projected plane's slope.
    float normalZ = max(abs(normal.z), 0.10) * (normal.z < 0.0 ? -1.0 : 1.0);
    vec2 depthGradient = -normal.xy / normalZ * shadowProjection[2][2]
                         / vec2(shadowProjection[0][0], shadowProjection[1][1]);
    vec2 center = (floor(coord.xy / texel) + 0.5) * texel;
    vec2 depthStep = depthGradient * texel;
    float depth = coord.z + dot(center - coord.xy, depthGradient)
                  - 0.022 * abs(shadowProjection[2][2]) * 0.5;
    vec2 stride = texel;
#if ADVANCED_SHADOWS >= 2 && SHADOW_FILTER > 0
    float bias = 0.022 * abs(shadowProjection[2][2]) * 0.5;
    stride *= penumbraScale(coord, texel, depthGradient, bias);
    depthStep = depthGradient * stride;
#endif
    float visibility = compareShadow(center, depth);
#if SHADOW_FILTER == 1
    visibility += compareShadow(center - vec2(stride.x, 0.0), depth - depthStep.x);
    visibility += compareShadow(center + vec2(stride.x, 0.0), depth + depthStep.x);
    visibility += compareShadow(center - vec2(0.0, stride.y), depth - depthStep.y);
    visibility += compareShadow(center + vec2(0.0, stride.y), depth + depthStep.y);
    visibility *= 0.2;
#elif SHADOW_FILTER == 2
    for (int y = -1; y <= 1; ++y)
        for (int x = -1; x <= 1; ++x)
            if (x != 0 || y != 0) visibility += compareShadow(center + vec2(x, y) * stride,
                                                             depth + dot(vec2(x, y), depthStep));
    visibility *= 1.0 / 9.0;
#endif
    return mix(1.0, visibility, range * smoothstep(guard, 0.07, edge));
}
