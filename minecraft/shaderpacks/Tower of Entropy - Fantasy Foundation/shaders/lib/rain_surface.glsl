uniform float wetness;
#if FOG_MODE == 0
uniform float toeCold, toeOverworld;
#ifndef LAVA
uniform vec3 cameraPosition;
uniform float frameTimeCounter;
#endif
#endif

float rainSurface(inout SurfaceMaterial m, inout vec3 albedo, inout vec3 normal, vec3 geometry) {
    float wet = wetness * toeOverworld * (1.0 - toeCold) * smoothstep(0.80, 0.965, lightUV.y);
    if (wet < 0.005) return 0.0;
    vec3 up = vec3(gbufferModelViewInverse[0].y, gbufferModelViewInverse[1].y, gbufferModelViewInverse[2].y);
    float id = floor(solidMaterial + 0.5);
    float flatness = max(dot(up, geometry), 0.0);
    float porous = id == 11108.0 ? 1.0 : 0.0;
    float stone = float(id == 11101.0 || id == 11102.0 || id == 11115.0);
    float strength = wet * (0.32 + 0.68 * flatness) * (0.65 + 0.12 * float(WET_SURFACES));
    albedo *= 1.0 - strength * mix(0.14 + 0.09 * stone, 0.28, porous) * (1.0 - m.metal * 0.74);
    m.roughness = mix(m.roughness, mix(0.26, 0.63, porous), strength * 0.62);
    m.f0 = max(m.f0, vec3(0.041 * strength));
    float puddle = 0.0;
#if WET_SURFACES >= 2
    if (flatness > 0.88 && stone > 0.0 && wet > 0.34) {
        vec2 p = (mat3(gbufferModelViewInverse) * viewPos + cameraPosition).xz;
        float basin = sin(p.x * 1.71 + sin(p.y * 0.83)) * cos(p.y * 1.38 - sin(p.x * 0.53));
        puddle = smoothstep(0.50, 0.86, basin) * smoothstep(0.34, 0.95, wet);
        m.roughness = mix(m.roughness, 0.17, puddle);
        vec2 cell = floor(p * 2.1);
        vec2 seed = fract(sin(vec2(dot(cell, vec2(42.7, 17.3)), dot(cell, vec2(23.1, 83.9)))) * 4375.23);
        vec2 delta = fract(p * 2.1) - mix(vec2(0.34), vec2(0.66), seed);
        float radius = length(delta);
        float phase = fract(frameTimeCounter * (0.82 + seed.x * 0.3) + seed.y);
        float wave = sin((radius - phase * 0.32) * 99.0) * (1.0 - smoothstep(0.07, 0.28, radius));
        wave *= smoothstep(0.0, 0.2, phase) * (1.0 - phase) * rainStrength * puddle;
        vec3 perturbation = vec3(delta.x, 0.0, delta.y) * wave * 0.24;
        normal = normalize(normal + vec3(dot(gbufferModelViewInverse[0].xyz, perturbation),
            dot(gbufferModelViewInverse[1].xyz, perturbation), dot(gbufferModelViewInverse[2].xyz, perturbation)));
    }
#endif
    return puddle;
}
