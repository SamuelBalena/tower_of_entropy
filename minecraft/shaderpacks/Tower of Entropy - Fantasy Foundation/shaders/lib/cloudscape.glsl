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

const float cloudBase = 174.0;
const float cloudTop = 224.0;

float cloudNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = smoothstep(vec2(0.0), vec2(1.0), fract(p));
    return mix(mix(skyHash(vec3(i, 12.0)), skyHash(vec3(i + vec2(1.0, 0.0), 12.0)), f.x),
               mix(skyHash(vec3(i + vec2(0.0, 1.0), 12.0)), skyHash(vec3(i + 1.0, 12.0)), f.x), f.y);
}

float cloudDensity(vec2 p, float h, float cover) {
    vec2 q = p + vec2(0.23, -0.17) * h;
    float shape = cloudNoise(q) * 0.71 + cloudNoise(q * 2.27 + vec2(7.2, 1.6) + vec2(-0.52, 0.28) * h) * 0.29;
    // Tall lobes grow above the broad base while the sides taper to clear sky.
    float envelope = 0.29 * pow(abs(h - 0.40) * 1.67, 1.55);
    float rim = smoothstep(0.0, 0.075, h) * (1.0 - smoothstep(0.84, 1.0, h));
    return smoothstep(cover + envelope, cover + envelope + 0.17, shape) * rim;
}

float cloudSelfShadow(vec2 p, float h, vec3 light, float density, float cover) {
#if SHADOW_RESOLUTION == 512
    return 1.0 / (1.0 + 1.25 * density * (1.0 - h));
#else
    vec2 stepXZ = light.xz * 0.17;
    float stepY = light.y * 0.30 + 0.047;
    float blocker = cloudDensity(p + stepXZ, h + stepY, cover) * 0.93;
#if SHADOW_RESOLUTION > 1024
    blocker += 0.63 * cloudDensity(p + 2.35 * stepXZ, h + 2.35 * stepY, cover);
#endif
    return exp(-1.65 * blocker);
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
    vec3 base = mix(vec3(0.067, 0.083, 0.17), vec3(0.43, 0.53, 0.65), day);
    base = mix(base, vec3(0.25, 0.19, 0.34), twilight * 0.57);
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
            float lit = cloudSelfShadow(p, h, light, density, cover);
            float exposure = clamp(0.10 + 0.31 * h + lit * (0.37 + 0.19 * forwardLight), 0.0, 1.0);
            vec3 color = mix(base, bright, exposure);
            float fade = 1.0 - smoothstep(390.0, 830.0, t);
            float extinction = mix(0.052, 0.083, day) + 0.028 * rain;
            float opacity = 1.0 - exp(-density * interval * extinction * fade);
            scattering += color * opacity * transmission;
            transmission *= 1.0 - opacity;
            if (transmission < 0.025) break;
        }
    }
    return sky * transmission + scattering;
}
