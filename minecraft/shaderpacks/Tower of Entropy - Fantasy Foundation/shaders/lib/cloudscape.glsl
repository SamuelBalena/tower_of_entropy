uniform vec3 cameraPosition;
uniform float frameTimeCounter;

#if SHADOW_RESOLUTION == 512
const int cloudSteps = 8;
const int maxCloudSteps = 20;
#elif SHADOW_RESOLUTION == 1024
const int cloudSteps = 14;
const int maxCloudSteps = 28;
#else
const int cloudSteps = 22;
const int maxCloudSteps = 38;
#endif

const float cloudBase = 181.0;
const float cloudTop = 222.0;

float cloudNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = smoothstep(vec2(0.0), vec2(1.0), fract(p));
    return mix(mix(skyHash(vec3(i, 12.0)), skyHash(vec3(i + vec2(1.0, 0.0), 12.0)), f.x),
               mix(skyHash(vec3(i + vec2(0.0, 1.0), 12.0)), skyHash(vec3(i + 1.0, 12.0)), f.x), f.y);
}

float cloudDensity(vec2 p, float h, float cover) {
    if (h <= 0.0 || h >= 1.0) return 0.0;
    vec2 q = p + vec2(0.32, -0.21) * h;
    float body = cloudNoise(q);
    if (body + 0.45 * 0.27 <= cover - 0.105) return 0.0;
    // Shear the finer field with altitude so the fringe breaks up independently of the core.
    vec2 fine = q * vec2(3.1, 2.5) + vec2(7.2, 1.6) + vec2(-0.76, 0.37) * h;
    float erosion = cloudNoise(fine);
    float localHeight = h + (body - 0.55) * 0.24 + (erosion - 0.5) * 0.19;
    float bodyProfile = smoothstep(0.02, 0.35, localHeight)
                      * (1.0 - smoothstep(0.53, 0.98, localHeight));
    float shell = smoothstep(0.0, 0.19, h) * (1.0 - smoothstep(0.74, 1.0, h));
    float mass = body + (erosion - 0.55) * 0.27 - cover;
    float core = smoothstep(0.01, 0.25, mass);
    float fringe = smoothstep(-0.105, 0.095, mass) * smoothstep(0.28, 0.78, erosion);
    return (core * bodyProfile * 0.74 + fringe * (1.0 - core) * 0.24) * shell;
}

float cloudSelfShadow(vec2 p, float h, vec3 light, float density, float cover) {
#if SHADOW_RESOLUTION == 512
    return 1.0 / (1.0 + 0.84 * density * (1.0 - h));
#else
    vec2 stepXZ = light.xz * 0.17;
    float stepY = light.y * 0.30 + 0.047;
    float blocker = cloudDensity(p + stepXZ, h + stepY, cover) * 0.93;
#if SHADOW_RESOLUTION > 1024
    blocker += 0.63 * cloudDensity(p + 2.35 * stepXZ, h + 2.35 * stepY, cover);
#endif
    return exp(-1.26 * blocker);
#endif
}

vec3 shadeClouds(vec3 sky, vec3 ray, vec3 sun, vec3 moon, float day, float twilight, float rain) {
    // Slab bounds are fixed in world space. Every integration sample has its own altitude.
    float vertical = abs(ray.y) < 0.004 ? (ray.y < 0.0 ? -0.004 : 0.004) : ray.y;
    vec2 hit = (vec2(cloudBase, cloudTop) - cameraPosition.y) / vertical;
    float start = max(min(hit.x, hit.y), 0.0);
    float end = min(max(hit.x, hit.y), 830.0);
    if (end <= start) return sky;

    vec2 origin = cameraPosition.xz / 120.0 + vec2(4.7, 13.2) + frameTimeCounter * vec2(0.008, 0.004);
    float cover = mix(0.535, 0.41, rain);
    float sunWeight = smoothstep(-0.19, 0.10, sun.y);
    vec3 light = normalize(mix(moon, sun, sunWeight) + vec3(0.0, 0.22, 0.0));
    vec3 base = mix(vec3(0.079, 0.104, 0.20), vec3(0.53, 0.62, 0.73), day);
    base = mix(base, vec3(0.34, 0.26, 0.42), twilight * 0.55);
    vec3 sunlight = mix(vec3(1.0, 0.975, 0.91), vec3(1.0, 0.53, 0.29), twilight * 0.85);
    vec3 bright = mix(vec3(0.27, 0.35, 0.54), sunlight, sunWeight);
    base = mix(base, vec3(0.22, 0.25, 0.33), rain * 0.78);
    bright = mix(bright, vec3(0.48, 0.53, 0.61), rain * 0.8);
    float forwardLight = pow(max(dot(ray, light), 0.0), 7.0);
    // A shorter interval near the horizon keeps shallow rays from skipping cloud lobes.
    float samples = clamp((end - start) / 17.0, float(cloudSteps), float(maxCloudSteps));
    float span = (end - start) / samples;
    float transmission = 1.0;
    vec3 scattering = vec3(0.0);

    for (int i = 0; i < maxCloudSteps; ++i) {
        float remaining = samples - float(i);
        if (remaining <= 0.0) break;
        float interval = span * min(1.0, remaining);
        float t = start + float(i) * span + interval * 0.5;
        vec2 p = origin + ray.xz * t / 120.0;
        float h = (cameraPosition.y + ray.y * t - cloudBase) / (cloudTop - cloudBase);
        float density = cloudDensity(p, h, cover);
        if (density > 0.002) {
            float shadowWeight = smoothstep(0.05, 0.27, density);
            float lit = 1.0;
            if (shadowWeight > 0.0) {
                lit = mix(1.0, cloudSelfShadow(p, h, light, density, cover), shadowWeight);
            }
            float exposure = clamp(0.16 + 0.23 * h + lit * (0.36 + 0.21 * forwardLight)
                                 + 0.13 * (1.0 - density), 0.0, 1.0);
            vec3 color = mix(base, bright, exposure);
            float fade = 1.0 - smoothstep(390.0, 830.0, t);
            float extinction = mix(0.042, 0.067, day) + 0.021 * rain;
            float opacity = 1.0 - exp(-density * interval * extinction * fade);
            scattering += color * opacity * transmission;
            transmission *= 1.0 - opacity;
            if (transmission < 0.025) break;
        }
    }
    return sky * transmission + scattering;
}
