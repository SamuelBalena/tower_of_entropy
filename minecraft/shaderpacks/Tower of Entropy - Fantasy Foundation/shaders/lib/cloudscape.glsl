uniform vec3 cameraPosition;
uniform float frameTimeCounter;

float cloudNoise(vec2 p) {
    vec2 cell = floor(p);
    vec2 s = fract(p);
    s = s * s * (3.0 - 2.0 * s);
    return mix(mix(skyHash(vec3(cell, 12.0)), skyHash(vec3(cell + vec2(1.0, 0.0), 12.0)), s.x),
               mix(skyHash(vec3(cell + vec2(0.0, 1.0), 12.0)), skyHash(vec3(cell + 1.0, 12.0)), s.x), s.y);
}

float cloudMass(vec2 p) {
    float mass = cloudNoise(p) * 0.61 + cloudNoise(p * 2.08 + 7.2) * 0.26;
    return mass + cloudNoise(p * 4.22 + vec2(18.4, -4.6)) * 0.13;
}

vec3 shadeClouds(vec3 sky, vec3 ray, vec3 sun, float day, float twilight, float rain) {
    if (ray.y < 0.014) return sky;
    // A tilted sampling slice through a noise field gives soft relief at voxel scale.
    float layerHeight = clamp(195.0 - cameraPosition.y, 24.0, 320.0);
    vec2 p = (cameraPosition.xz + ray.xz * (layerHeight / max(0.035, ray.y))) / 112.0;
    p += vec2(4.7, 13.2) + frameTimeCounter * vec2(0.008, 0.004);
    float mass = cloudMass(p);
#if SHADOW_RESOLUTION > 512
    mass = mass * 0.94 + cloudNoise(p * 8.37 + vec2(-5.6, 26.3)) * 0.06;
#endif
    float threshold = 0.53 - rain * 0.15;
    float coverage = smoothstep(threshold, threshold + 0.050, mass);
#if SHADOW_RESOLUTION > 1024
    float high = cloudNoise(p * 0.53 + 31.4) * cloudNoise(p * 1.7 + 14.2);
    float veil = smoothstep(0.31, 0.62, high) * smoothstep(0.06, 0.32, ray.y);
    sky = mix(sky, mix(vec3(0.18, 0.23, 0.38), vec3(0.73, 0.83, 0.93), day), veil * (0.22 - rain * 0.12));
#endif
    if (coverage < 0.002) return sky;
    float thickness = smoothstep(threshold + 0.01, threshold + 0.23, mass);
    float facing = 0.80 - thickness * 0.49;
#if SHADOW_RESOLUTION > 512
    float sunward = cloudMass(p + sun.xz * 0.17);
    facing = clamp(facing + (mass - sunward) * 2.55, 0.04, 0.99);
#endif
    float towardSun = max(dot(ray, sun), 0.0);
    vec3 underside = mix(vec3(0.084, 0.102, 0.22), vec3(0.49, 0.63, 0.79), day);
    vec3 rim = mix(vec3(0.24, 0.29, 0.48), vec3(1.0, 0.99, 0.94), day);
    underside = mix(underside, vec3(0.37, 0.25, 0.44), twilight * 0.58);
    rim = mix(rim, vec3(1.0, 0.61, 0.40), twilight * (0.47 + towardSun * 0.45));
    vec3 cloud = mix(underside, rim, facing);
    cloud = mix(cloud, vec3(0.38, 0.43, 0.50) * (0.56 + 0.42 * day), rain * 0.78);
    return mix(sky, cloud, coverage * smoothstep(0.015, 0.15, ray.y) * 0.91);
}
