uniform float viewWidth, viewHeight;

float edgeLuma(vec3 rgb) {
    return dot(rgb, vec3(0.2126, 0.7152, 0.0722));
}

float edgeSample(vec2 sampleUv, vec2 halfPixel) {
    return edgeLuma(texture2D(colortex0, clamp(sampleUv, halfPixel, 1.0 - halfPixel)).rgb);
}

vec3 resolveFxaa(vec2 center, vec3 source) {
    vec2 pixel = 1.0 / vec2(viewWidth, viewHeight);
    vec2 limits = 0.5 * pixel;
    vec2 dx = vec2(pixel.x, 0.0);
    vec2 dy = vec2(0.0, pixel.y);
    float m = edgeLuma(source);
    float left = edgeSample(center - dx, limits);
    float right = edgeSample(center + dx, limits);
    float up = edgeSample(center + dy, limits);
    float down = edgeSample(center - dy, limits);
    float low = min(m, min(min(left, right), min(up, down)));
    float high = max(m, max(max(left, right), max(up, down)));
    float range = high - low;
    if (range < max(0.030, high * 0.14)) return source;

    float ul = edgeSample(center - dx + dy, limits);
    float ur = edgeSample(center + dx + dy, limits);
    float dl = edgeSample(center - dx - dy, limits);
    float dr = edgeSample(center + dx - dy, limits);
    float edgeX = abs(ul + dl - 2.0 * left) + 2.0 * abs(up + down - 2.0 * m)
                + abs(ur + dr - 2.0 * right);
    float edgeY = abs(ul + ur - 2.0 * up) + 2.0 * abs(left + right - 2.0 * m)
                + abs(dl + dr - 2.0 * down);
    bool horizontal = edgeX >= edgeY;
    vec2 axis = horizontal ? dx : dy;
    vec2 perpendicular = horizontal ? dy : dx;
    float negative = horizontal ? down : left;
    float positive = horizontal ? up : right;
    bool useNegative = abs(negative - m) >= abs(positive - m);
    float adjacent = useNegative ? negative : positive;
    if (useNegative) perpendicular = -perpendicular;

    float across = abs(adjacent - m);
    if (across < range * 0.18) return source;
    float edgeCenter = 0.5 * (m + adjacent);
    vec2 start = center + 0.5 * perpendicular;
    float threshold = across * 0.25;
    float nearDistance = 0.0, farDistance = 0.0;
    float nearDelta = 0.0, farDelta = 0.0;
    bool nearFound = false, farFound = false;
    const int searchSteps = AA_QUALITY + 2;
    for (int i = 0; i < searchSteps; ++i) {
        float stepDistance = 1.0 + float(i * (i + 1)) * 0.5;
        if (!nearFound) {
            nearDistance = stepDistance;
            nearDelta = edgeSample(start - axis * stepDistance, limits) - edgeCenter;
            nearFound = abs(nearDelta) >= threshold;
        }
        if (!farFound) {
            farDistance = stepDistance;
            farDelta = edgeSample(start + axis * stepDistance, limits) - edgeCenter;
            farFound = abs(farDelta) >= threshold;
        }
        if (nearFound && farFound) break;
    }

    bool nearerIsNegative = nearDistance < farDistance;
    float closestDelta = nearerIsNegative ? nearDelta : farDelta;
    float closestDistance = min(nearDistance, farDistance);
    float edgeOffset = 0.5 - closestDistance / (nearDistance + farDistance);
    if (!(nearerIsNegative ? nearFound : farFound) || (closestDelta < 0.0) == (m < edgeCenter)) edgeOffset = 0.0;

    // Prefer silhouettes. Small offsets on coplanar texture edges keep the source texels legible.
    float depth = 1.0 - texture2D(depthtex0, center).r;
    float posDepth = 1.0 - texture2D(depthtex0, clamp(center + perpendicular, limits, 1.0 - limits)).r;
    float negDepth = 1.0 - texture2D(depthtex0, clamp(center - perpendicular, limits, 1.0 - limits)).r;
    float depthCurve = abs(posDepth + negDepth - 2.0 * depth);
    float silhouette = smoothstep(0.012, 0.065, depthCurve / max(max(depth, max(posDepth, negDepth)), 0.000005));
    float coverage = clamp(abs((left + right + up + down) * 0.25 - m) / range, 0.0, 1.0);
    // The coverage term keeps isolated thin foliage and fence edges from being skipped.
    float offset = max(edgeOffset, coverage * coverage * 0.29 * silhouette);
    offset *= AA_STRENGTH * mix(0.30, 1.0, silhouette);
    vec2 resolved = clamp(center + perpendicular * min(offset, 0.46), limits, 1.0 - limits);
    return texture2D(colortex0, resolved).rgb;
}
