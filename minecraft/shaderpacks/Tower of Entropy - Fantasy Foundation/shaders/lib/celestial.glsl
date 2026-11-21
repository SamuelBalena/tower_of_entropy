uniform vec3 moonPosition;
uniform int moonPhase;

float skyHash(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.x + p.y) * p.z);
}

float starLayer(vec3 ray, float scale, float cutoff) {
    vec3 p = ray * scale;
    vec3 cell = floor(p + 0.5);
    float seed = skyHash(cell);
    vec3 jitter = vec3(skyHash(cell + 8.2), skyHash(cell - 16.6), skyHash(cell + 31.7));
    float size = mix(0.075, 0.17, jitter.y);
    float radius = length(p - cell - (jitter - 0.5) * 0.42);
    float aa = clamp(fwidth(radius) * 0.52, 0.025, 0.12);
    return (1.0 - smoothstep(size, size + aa, radius)) * step(cutoff, seed) * seed;
}

vec3 starLight(vec3 ray, float night) {
    float stars = starLayer(ray, 45.0, 0.85);
#if SHADOW_RESOLUTION > 512
    stars += starLayer(ray.yzx + vec3(0.17, 0.31, 0.23), 94.0, 0.95) * 0.31;
#endif
    float band = exp(-9.0 * abs(dot(ray, normalize(vec3(0.42, 0.76, 0.49)))));
    vec3 dust = vec3(0.043, 0.039, 0.092) * band * MAGIC;
    return (dust + stars * mix(vec3(0.51, 0.68, 0.91), vec3(0.96, 0.82, 0.74), band * 0.7))
         * night * smoothstep(0.02, 0.29, ray.y);
}

vec3 celestialLight(vec3 sky, vec3 ray, vec3 sun, vec3 moon, float rain) {
    float sunAngle = max(1.0 - dot(ray, sun), 0.0);
    float sunAA = max(fwidth(sunAngle), 0.000018);
    float disc = 1.0 - smoothstep(0.00039 - sunAA, 0.00039 + sunAA, sunAngle);
    float golden = 1.0 - smoothstep(0.04, 0.42, sun.y);
    vec3 sunColor = mix(vec3(1.0, 0.95, 0.73), vec3(1.0, 0.71, 0.35), golden);
    float sunHalo = 0.27 * exp(-sunAngle * 162.0) + 0.095 * exp(-sunAngle * 17.0);
    float sunVisible = smoothstep(-0.045, 0.015, sun.y) * rain;
    sky += sunColor * sunHalo * sunVisible;
    sky = mix(sky, sunColor, disc * sunVisible);

    float moonAngle = max(1.0 - dot(ray, moon), 0.0);
    float moonAA = max(fwidth(moonAngle), 0.000018);
    float moonDisc = 1.0 - smoothstep(0.00054 - moonAA, 0.00054 + moonAA, moonAngle);
    float moonVisible = smoothstep(-0.035, 0.06, moon.y) * rain;
    float phase = float(moonPhase) * 0.78539816;
    sky += vec3(0.25, 0.44, 0.74) * exp(-moonAngle * 100.0)
         * (0.12 + 0.15 * (0.5 + 0.5 * cos(phase))) * moonVisible;
    if (moonDisc > 0.0) {
        vec3 axis = normalize(cross(vec3(0.0, 0.0, 1.0), moon));
        vec2 p = vec2(dot(ray, axis), dot(ray, cross(moon, axis))) / 0.03286;
        vec3 normal = vec3(p, sqrt(max(1.0 - dot(p, p), 0.0)));
        float lit = smoothstep(-0.022, 0.055, dot(normal, vec3(sin(phase), 0.0, cos(phase))));
        float basalt = 0.22 * exp(-8.0 * dot(p - vec2(-0.31, 0.22), p - vec2(-0.31, 0.22)));
        basalt += 0.13 * exp(-19.0 * dot(p - vec2(0.33, -0.18), p - vec2(0.33, -0.18)));
        vec3 silver = mix(vec3(0.60, 0.72, 0.87), vec3(0.94, 0.97, 1.0), normal.z) - basalt;
        sky = mix(sky, mix(sky * 0.65, silver, lit), moonDisc * moonVisible);
    }
    return sky;
}
