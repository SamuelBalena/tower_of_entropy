uniform vec3 cameraPosition;
uniform float frameTimeCounter;

float lavaHash(vec2 cell) {
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
    vec2 p = vertical ? vec2(axis.x > axis.z ? world.z : world.x, world.y) : world.xz;
    float time = frameTimeCounter;
    // Vertical runoff follows gravity. Horizontal motion models local circulation.
    if (vertical) p = p * vec2(1.35, 0.64) + vec2(0.0, time * 0.31);
    vec2 curl = vec2(lavaNoise(p * 0.24 + vec2(time * 0.025, 17.0)),
                     lavaNoise(p * 0.27 + vec2(31.0, -time * 0.032))) - 0.5;
    p += curl * (flowing ? 2.3 : 1.7);
    float field = lavaNoise(p * 0.79 + vec2(8.4, 13.0));
    float grain = 0.5 + 0.7 * curl.y;
#if LAVA_DETAIL > 0
    grain = lavaNoise(p * 1.9 + vec2(-time * 0.052, 29.0));
    field += (grain - 0.5) * 0.19;
#endif
#if LAVA_DETAIL > 1
    float pores = lavaNoise(p * 4.25 + vec2(51.0, time * 0.044));
    float detail = 1.0 - smoothstep(9.0, 25.0, length(view));
    field += (pores - 0.5) * 0.09 * detail;
    grain = mix(grain, pores, 0.36 * detail);
#endif
    float distant = smoothstep(18.0, 52.0, length(view));
    float seam = 1.0 - smoothstep(0.019, mix(0.072, 0.12, distant), abs(field - 0.44));
    seam *= 1.0 - distant * 0.55;
    float heat = smoothstep(0.33, 0.79, field);
    vec3 crust = mix(vec3(0.108, 0.042, 0.023), vec3(0.42, 0.090, 0.031), heat);
    crust *= mix(0.77, 1.18, grain);
    vec3 liquid = mix(vec3(0.79, 0.14, 0.031), vec3(0.96, 0.63, 0.15), smoothstep(0.30, 0.91, heat));
    float whiteHot = smoothstep(0.87, 0.98, field) * smoothstep(0.45, 0.82, grain);
    liquid = mix(liquid, vec3(0.97, 0.86, 0.67), whiteHot * 0.70);
    vec3 color = mix(crust, liquid, max(smoothstep(0.43, 0.60, field), seam * 0.82));
    // Keep only a small contribution from the animated vanilla sprites.
    return color * (0.93 + dot(atlas, vec2(0.045, 0.035)));
}