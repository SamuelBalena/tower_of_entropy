uniform sampler2D colortex6;

vec3 solidReflection(vec2 uv, vec3 position) {
    vec4 surface = texture2D(colortex6, uv);
    float distanceToEye = -position.z;
    if (surface.b < 0.005 || abs(surface.a - distanceToEye) > max(0.006, distanceToEye * 0.001) || distanceToEye > 42.0)
        return vec3(0.0);

    vec2 xy = surface.rg * 2.0 - 1.0;
    vec3 n = vec3(xy, 1.0 - abs(xy.x) - abs(xy.y));
    n.xy += vec2(n.x < 0.0 ? 1.0 : -1.0, n.y < 0.0 ? 1.0 : -1.0) * max(-n.z, 0.0);
    n = normalize(n);
    vec3 ray = reflect(normalize(position), n);
    vec3 origin = position + n * 0.055;
    const int taps = 4 * SOLID_REFLECTIONS;
    float previous = 0.035;
    for (int i = 1; i <= taps; ++i) {
        float fi = float(i);
        float travel = 0.12 + fi * (0.20 + fi * 0.058);
        vec3 point = origin + ray * travel;
        vec2 hit;
        if (!screenProjection(point, hit)) break;
        float depth = texture2D(depthtex0, hit).r;
        if (depth < 0.999999 && screenViewPosition(hit, depth).z > point.z) {
            float hi = travel;
            float lo = previous;
            for (int j = 0; j < 4; ++j) {
                float mid = (hi + lo) * 0.5;
                vec3 candidate = origin + ray * mid;
                if (!screenProjection(candidate, hit)) return vec3(0.0);
                if (screenViewPosition(hit, texture2D(depthtex0, hit).r).z > candidate.z) hi = mid;
                else lo = mid;
            }
            point = origin + ray * hi;
            if (!screenProjection(point, hit)) break;
            vec3 back = screenViewPosition(hit, texture2D(depthtex0, hit).r);
            float error = back.z - point.z;
            vec2 border = min(hit, 1.0 - hit);
            float confidence = smoothstep(0.015, 0.12, min(border.x, border.y))
                             * (1.0 - smoothstep(0.035, 0.23, error))
                             * smoothstep(0.11, 0.45, dot(back - position, n))
                             * (1.0 - smoothstep(20.0, 42.0, distanceToEye));
            return texture2D(colortex0, hit).rgb * (surface.b * confidence);
        }
        previous = travel;
    }
    return vec3(0.0);
}
