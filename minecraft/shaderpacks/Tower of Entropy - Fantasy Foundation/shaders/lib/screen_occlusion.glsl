uniform vec3 sunPosition;
uniform float viewWidth, viewHeight, rainStrength, toeOverworld;

vec3 positionAt(vec2 p) {
    return screenViewPosition(p, texture2D(depthtex0, clamp(p, vec2(0.0), vec2(1.0))).r);
}

vec3 depthNormal(vec2 p, vec3 center) {
    vec2 pixel = 1.0 / vec2(viewWidth, viewHeight);
    vec3 left = center - positionAt(p - vec2(pixel.x, 0.0));
    vec3 right = positionAt(p + vec2(pixel.x, 0.0)) - center;
    vec3 bottom = center - positionAt(p - vec2(0.0, pixel.y));
    vec3 top = positionAt(p + vec2(0.0, pixel.y)) - center;
    vec3 dx = abs(left.z) < abs(right.z) ? left : right;
    vec3 dy = abs(bottom.z) < abs(top.z) ? bottom : top;
    vec3 n = cross(dx, dy);
    return n * inversesqrt(max(dot(n, n), 1e-16));
}

#if SCREEN_AO > 0
float ambientOcclusion(vec2 uv, vec3 center, vec3 normal) {
    const int taps = 4 * SCREEN_AO;
    float radius = 0.45 + 0.15 * float(SCREEN_AO);
    vec2 scale = vec2(gbufferProjection[0][0], gbufferProjection[1][1]) * (0.5 * radius / -center.z);
    scale = clamp(scale, vec2(0.0), vec2(0.095));
    float sum = 0.0;
    for (int i = 0; i < taps; ++i) {
        float angle = float(i) * 2.39996323;
        float ring = sqrt((float(i) + 0.75) / float(taps));
        vec2 p = uv + vec2(cos(angle), sin(angle)) * scale * ring;
        if (min(p.x, p.y) <= 0.0 || max(p.x, p.y) >= 1.0) continue;
        vec3 delta = positionAt(p) - center;
        float distanceSquared = dot(delta, delta);
        if (distanceSquared >= radius * radius) continue;
        float h = dot(normal, delta) * inversesqrt(max(distanceSquared, 0.001));
        sum += smoothstep(0.10, 0.67, h) * (1.0 - distanceSquared / (radius * radius));
    }
    return 0.43 * sum / float(taps);
}
#endif

#if CONTACT_SHADOWS > 0
float contactVisibility(vec3 center, vec3 n) {
    vec3 sun = normalize(sunPosition);
    vec3 up = vec3(gbufferModelViewInverse[0].y, gbufferModelViewInverse[1].y, gbufferModelViewInverse[2].y);
    float intensity = toeOverworld * (1.0 - rainStrength)
                    * smoothstep(0.02, 0.25, dot(sun, up)) * smoothstep(0.05, 0.45, dot(n, sun));
    if (intensity < 0.015) return 1.0;
    const int taps = 2 + 2 * CONTACT_SHADOWS;
    for (int i = 1; i <= taps; ++i) {
        float travel = 0.045 + float(i) * (0.66 / float(taps));
        vec3 ray = center + n * 0.035 + sun * travel;
        vec2 p;
        if (!screenProjection(ray, p)) break;
        float gap = positionAt(p).z - ray.z;
        if (gap > 0.018 && gap < 0.095)
            return 1.0 - intensity * 0.17 * (1.0 - travel);
    }
    return 1.0;
}
#endif

float screenVisibility(vec2 uv, vec3 center) {
    float fade = smoothstep(0.35, 1.5, -center.z) * (1.0 - smoothstep(18.0, 34.0, -center.z));
    if (fade <= 0.0) return 1.0;
    vec3 normal = depthNormal(uv, center);
    float visibility = 1.0;
#if SCREEN_AO > 0
    visibility -= ambientOcclusion(uv, center, normal);
#endif
#if CONTACT_SHADOWS > 0
    visibility *= contactVisibility(center, normal);
#endif
    return mix(1.0, visibility, fade);
}
