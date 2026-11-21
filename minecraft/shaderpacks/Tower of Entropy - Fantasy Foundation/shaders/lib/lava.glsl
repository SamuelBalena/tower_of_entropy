uniform vec3 cameraPosition;
uniform float frameTimeCounter;

float lavaHash(vec2 cell) {
    cell = mod(cell, 720.0);
    vec3 p = fract(cell.xyx * vec3(0.1031, 0.11369, 0.13787));
    p += dot(p, p.yzx + 19.19);
    return fract((p.x + p.y) * p.z);
}

float lavaNoise(vec2 p) {
    vec2 cell = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(lavaHash(cell), lavaHash(cell + vec2(1, 0)), f.x),
               mix(lavaHash(cell + vec2(0, 1)), lavaHash(cell + vec2(1, 1)), f.x), f.y);
}

vec3 lavaColor(vec3 view, vec3 face, bool flowing, vec2 atlas) {
    vec3 world = (gbufferModelViewInverse * vec4(view, 1.0)).xyz + cameraPosition;
    vec3 axis = abs(mat3(gbufferModelViewInverse) * face);
    bool vertical = max(axis.x, axis.z) > 1.5 * axis.y;
    const mat2 rotation = mat2(0.8, 0.6, -0.6, 0.8);
    vec2 p = vertical ? vec2(axis.x > axis.z ? world.z : world.x, world.y) * vec2(6.0, 3.4)
                      : rotation * world.xz * 5.8;
    float phase = frameTimeCounter * (6.2831853 / 3600.0);
    // Integer harmonics and a 720-cell period meet at the Oculus timer rollover.
    vec2 drift = vec2(sin(phase * 17.0 + p.y * 0.075), cos(phase * 13.0 + p.x * 0.086));
    p += drift * (flowing ? 0.95 : 0.54);
    if (vertical) p.y += frameTimeCounter * 0.2;

    float distanceToEye = length(view);
    float textureWeight = 1.0 - smoothstep(21.0, 72.0, distanceToEye);
    float body = lavaNoise(p + vec2(8.4, 13.1));
    float sparks = lavaNoise(p * 2.0 + vec2(19.2, 11.7));
#if LAVA_DETAIL > 0
    float eddies = lavaNoise(p * 3.0 + vec2(27.1, 5.2));
    sparks = mix(sparks, eddies, 0.24 * (1.0 - smoothstep(12.0, 36.0, distanceToEye)));
#endif
#if LAVA_DETAIL > 1
    float speck = lavaNoise(p * 5.0 + vec2(8.2, 37.4));
    body += (speck - 0.5) * 0.20 * (1.0 - smoothstep(6.0, 21.0, distanceToEye));
#endif

    vec3 cooler = vec3(0.75, 0.225, 0.041);
    vec3 orange = vec3(0.93, 0.395, 0.070);
    vec3 gold = vec3(0.98, 0.69, 0.23);
    float heat = mix(0.53, smoothstep(0.12, 0.86, body), textureWeight);
    float flecks = smoothstep(0.63, 0.93, sparks) * mix(0.31, 0.94, heat) * textureWeight;
    vec3 color = mix(mix(cooler, orange, heat), gold, flecks);
    return color * (0.98 + dot(atlas, vec2(0.012, 0.008)));
}
