uniform float toeNether, toeEnd;

vec3 endSky(vec3 ray) {
    float horizon = exp(-abs(ray.y) * 4.3);
    float bands = sin(dot(ray, vec3(6.5, 5.0, 4.4))) * sin(dot(ray, vec3(-11.0, 8.0, 7.0)));
    return mix(vec3(0.045, 0.042, 0.071), vec3(0.15, 0.12, 0.20), horizon)
         + vec3(0.019, 0.014, 0.032) * (0.5 + 0.5 * bands) * float(DIMENSION_FOG) / 3.0;
}

vec3 dimensionFog(vec3 color, vec3 legacyTint, float legacyFog, vec3 view) {
    float travel = length(view);
    float density = (toeNether * 0.007 + toeEnd * 0.003) * (0.60 + 0.20 * float(DIMENSION_FOG));
    float extinction = min(1.0 - exp(-max(0.0, travel - 9.0) * density), 0.48);
    // The engine fog color already follows the active Nether biome.
    vec3 palette = mix(legacyTint * vec3(1.08, 0.96, 0.91),
                       vec3(0.13, 0.12, 0.21), toeEnd);
    return mix(mix(color, palette, extinction), legacyTint, legacyFog);
}
