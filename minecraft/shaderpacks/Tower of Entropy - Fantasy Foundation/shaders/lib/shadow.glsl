uniform sampler2D shadowtex0;
uniform mat4 shadowProjection;
varying vec3 shadowViewPos, shadowViewNormal;

float compareShadow(vec2 uv, float depth) {
    return step(depth, texture2D(shadowtex0, uv).r);
}

float sunVisibility(vec3 viewPos, float incidence) {
    vec3 normal = normalize(shadowViewNormal);
    float slope = sqrt(max(1.0 - incidence * incidence, 0.0));
    vec3 receiver = shadowViewPos + normal * mix(0.012, 0.045, slope);
    vec4 projected = shadowProjection * vec4(receiver, 1.0);
    vec3 coord = projected.xyz / projected.w * 0.5 + 0.5;
    vec2 texel = vec2(1.0 / float(SHADOW_RESOLUTION));
    vec2 border = min(coord.xy, 1.0 - coord.xy);
    float edge = min(border.x, border.y);
    float range = 1.0 - smoothstep(shadowDistance * 0.78, shadowDistance, length(viewPos));
    if (edge < 2.0 * texel.x || coord.z <= 0.0 || coord.z >= 1.0 || range <= 0.0) return 1.0;

    // Compare with the plane's depth at the center of each sampled texel.
    // With an orthographic shadow projection, this is the projected plane's slope.
    float normalZ = max(abs(normal.z), 0.10) * (normal.z < 0.0 ? -1.0 : 1.0);
    vec2 depthGradient = -normal.xy / normalZ * shadowProjection[2][2]
                         / vec2(shadowProjection[0][0], shadowProjection[1][1]);
    vec2 center = (floor(coord.xy / texel) + 0.5) * texel;
    vec2 depthStep = depthGradient * texel;
    float depth = coord.z + dot(center - coord.xy, depthGradient)
                  - 0.022 * abs(shadowProjection[2][2]) * 0.5;
    float visibility = compareShadow(center, depth);
#if SHADOW_FILTER == 1
    visibility += compareShadow(center - vec2(texel.x, 0.0), depth - depthStep.x);
    visibility += compareShadow(center + vec2(texel.x, 0.0), depth + depthStep.x);
    visibility += compareShadow(center - vec2(0.0, texel.y), depth - depthStep.y);
    visibility += compareShadow(center + vec2(0.0, texel.y), depth + depthStep.y);
    visibility *= 0.2;
#elif SHADOW_FILTER == 2
    for (int y = -1; y <= 1; ++y)
        for (int x = -1; x <= 1; ++x)
            if (x != 0 || y != 0) visibility += compareShadow(center + vec2(x, y) * texel,
                                                             depth + dot(vec2(x, y), depthStep));
    visibility *= 1.0 / 9.0;
#endif
    return mix(1.0, visibility, range * smoothstep(2.0 * texel.x, 0.07, edge));
}
