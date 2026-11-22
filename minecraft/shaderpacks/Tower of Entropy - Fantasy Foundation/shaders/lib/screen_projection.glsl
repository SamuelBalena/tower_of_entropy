uniform mat4 gbufferProjection, gbufferProjectionInverse;

vec3 screenViewPosition(vec2 uv, float depth) {
    vec4 point = gbufferProjectionInverse * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return point.xyz / point.w;
}

bool screenProjection(vec3 point, out vec2 uv) {
    vec4 projected = gbufferProjection * vec4(point, 1.0);
    if (projected.w <= 0.0) return false;
    uv = projected.xy / projected.w * 0.5 + 0.5;
    return all(greaterThan(uv, vec2(0.0))) && all(lessThan(uv, vec2(1.0)));
}
