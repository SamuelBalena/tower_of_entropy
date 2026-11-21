uniform sampler2D colortex4, colortex5, depthtex1;
uniform mat4 gbufferModelViewInverse, gbufferProjection, gbufferProjectionInverse;
uniform vec3 cameraPosition, moonPosition;
uniform float viewWidth, viewHeight, frameTimeCounter;
uniform int isEyeInWater, moonPhase;

vec3 waterViewPosition(vec2 uv, float depth) {
    vec4 point = gbufferProjectionInverse * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return point.xyz / point.w;
}

vec2 waveSlope(vec2 p, vec2 direction, float frequency, float speed, float amplitude) {
    float phase = dot(p, direction) * frequency - frameTimeCounter * speed;
    return direction * (cos(phase) * amplitude);
}

vec3 waterNormal(vec3 normal, vec3 world, float distanceToEye) {
    vec2 p = world.xz;
    vec2 slope = waveSlope(p, vec2(0.94, 0.342), 0.71, 0.86, 0.060)
               + waveSlope(p, vec2(-0.44, 0.898), 1.43, 1.13, 0.049);
#if WATER_DETAIL > 0
    slope += waveSlope(p, vec2(0.225, -0.974), 2.85, 1.43, 0.029)
           + waveSlope(p, vec2(-0.806, -0.592), 4.67, 1.74, 0.020);
#endif
#if WATER_DETAIL > 1
    slope += waveSlope(p, vec2(0.658, 0.753), 7.3, 2.04, 0.012);
#endif
    float distant = 1.0 - smoothstep(18.0, 88.0, distanceToEye) * 0.68;
    return normalize(normal - vec3(slope.x, 0.0, slope.y) * abs(normal.y) * distant);
}

vec3 waterSky(vec3 ray) {
    float u = atan(ray.z, ray.x) * 0.15915494 + 0.5;
    float v = asin(clamp(ray.y, 0.0, 1.0)) * 0.63661977;
    // Sample both sides of the azimuth seam with the same interpolation as the interior.
    float x = u * 256.0 - 0.5;
    float a = (mod(floor(x), 256.0) + 0.5) / 256.0;
    float b = (mod(floor(x) + 1.0, 256.0) + 0.5) / 256.0;
    return mix(texture2D(colortex5, vec2(a, v)).rgb, texture2D(colortex5, vec2(b, v)).rgb, fract(x));
}

#if WATER_SSR == 1 && WATER_DETAIL > 0
bool waterProjection(vec3 point, out vec2 uv) {
    vec4 projected = gbufferProjection * vec4(point, 1.0);
    if (projected.w <= 0.0) return false;
    uv = projected.xy / projected.w * 0.5 + 0.5;
    return all(greaterThan(uv, vec2(0.0))) && all(lessThan(uv, vec2(1.0)));
}

vec4 waterSceneReflection(vec3 origin, vec3 direction) {
#if WATER_DETAIL == 1
    const int steps = 12;
#else
    const int steps = 20;
#endif
    float previous = 0.20;
    float lastGap = -1.0;
    for (int i = 0; i < steps; ++i) {
        float stride = 0.20 + float(i + 1) * (0.24 + float(i + 1) * 0.075);
        vec3 point = origin + direction * stride;
        vec2 uv;
        if (!waterProjection(point, uv)) break;
        float raw = texture2D(depthtex1, uv).r;
        float gap = raw < 0.999999 ? waterViewPosition(uv, raw).z - point.z : -1.0;
        if (lastGap < 0.0 && gap >= 0.0) {
            float lo = previous;
            float hi = stride;
            for (int j = 0; j < 4; ++j) {
                float mid = (lo + hi) * 0.5;
                point = origin + direction * mid;
                waterProjection(point, uv);
                raw = texture2D(depthtex1, uv).r;
                if (raw < 0.999999 && waterViewPosition(uv, raw).z >= point.z) hi = mid;
                else lo = mid;
            }
            point = origin + direction * hi;
            if (!waterProjection(point, uv)) break;
            raw = texture2D(depthtex1, uv).r;
            if (raw >= 0.999999) break;
            vec3 hit = waterViewPosition(uv, raw);
            vec3 worldOffset = mat3(gbufferModelViewInverse) * (hit - origin);
            float error = hit.z - point.z;
            if (error < 0.0 || error > 0.26 + hi * 0.025 || worldOffset.y < 0.09) break;
            vec2 border = min(uv, 1.0 - uv);
            float edge = smoothstep(0.02, 0.12, min(border.x, border.y));
            float confidence = edge * (1.0 - smoothstep(8.0, 26.0, hi));
            confidence *= 1.0 - smoothstep(0.12, 0.26 + hi * 0.025, error);
            return vec4(texture2D(colortex4, uv).rgb, confidence * 0.62);
        }
        previous = stride;
        lastGap = gap;
    }
    return vec4(0.0);
}
#endif

vec3 waterColor(vec3 position, vec3 normalView, vec2 lightUV) {
    mat3 toWorld = mat3(gbufferModelViewInverse);
    mat3 toView = mat3(vec3(toWorld[0].x, toWorld[1].x, toWorld[2].x),
                       vec3(toWorld[0].y, toWorld[1].y, toWorld[2].y),
                       vec3(toWorld[0].z, toWorld[1].z, toWorld[2].z));
    vec2 uv = gl_FragCoord.xy / vec2(viewWidth, viewHeight);
    float distanceToEye = length(position);
    vec3 normal = waterNormal(normalize(toWorld * normalView), toWorld * position + cameraPosition, distanceToEye);
    vec3 incident = normalize(toWorld * position);
    vec3 reflected = reflect(incident, normal);
    float cosine = clamp(abs(dot(incident, normal)), 0.0, 1.0);
    float fresnel = 0.0204 + 0.9796 * pow(1.0 - cosine, 5.0);

    float raw = texture2D(depthtex1, uv).r;
    vec3 back = waterViewPosition(uv, min(raw, 0.999998));
    float thickness = raw < 0.999999 && back.z < position.z ? min(length(back - position), 36.0) : 36.0;
    vec2 bottomUV = uv;
#if WATER_DETAIL > 0
    if (thickness < 35.0 && raw > gl_FragCoord.z) {
        vec2 delta = (toView * (normal - normalize(toWorld * normalView))).xy;
        vec2 candidate = uv + delta * (0.028 * smoothstep(0.02, 0.55, thickness));
        if (all(greaterThan(candidate, vec2(0.004))) && all(lessThan(candidate, vec2(0.996)))) {
            float shifted = texture2D(depthtex1, candidate).r;
            vec3 shiftedView = waterViewPosition(candidate, min(shifted, 0.999998));
            if (shifted < 0.999999 && shiftedView.z < position.z - 0.035
                    && abs(shiftedView.z - back.z) < max(0.20, thickness * 0.44)) bottomUV = candidate;
        }
    }
#endif
    vec3 background = texture2D(colortex4, bottomUV).rgb;
    vec3 sun = normalize(toWorld * sunPosition);
    vec3 moon = normalize(toWorld * moonPosition);
    float light = clamp((lightUV.y * 16.0 - 0.5) / 15.0, 0.0, 1.0);
    float lamp = pow(clamp((lightUV.x * 16.0 - 0.5) / 15.0, 0.0, 1.0), 2.0);
    float day = smoothstep(-0.16, 0.27, sun.y);
    vec3 scattering = mix(vec3(0.018, 0.073, 0.105), vec3(0.036, 0.31, 0.37), day);
    scattering *= 0.36 + light * 0.64;
    scattering += lamp * vec3(0.12, 0.095, 0.041);
    vec3 transmittance = exp(-min(thickness, 30.0) * vec3(0.32, 0.126, 0.070));
    vec3 color = background * transmittance + scattering * (1.0 - transmittance);
    vec3 skyReflection = waterSky(reflected) * (0.26 + light * 0.74);
#if WATER_SSR == 1 && WATER_DETAIL > 0
    if (reflected.y > 0.017 && fresnel > 0.071 && isEyeInWater == 0) {
        vec4 sceneReflection = waterSceneReflection(position, toView * reflected);
        skyReflection = mix(skyReflection, sceneReflection.rgb, sceneReflection.a);
    }
#endif
    vec3 result = mix(color, skyReflection, fresnel * 0.86);
    float sunDot = max(dot(reflected, sun), 0.0);
    float moonDot = max(dot(reflected, moon), 0.0);
    float sparkle = pow(sunDot, 105.0) * 0.46 + pow(sunDot, 26.0) * 0.095;
    float moonLight = (0.5 + 0.5 * cos(float(moonPhase) * 0.78539816))
                    * smoothstep(-0.02, 0.12, moon.y);
    float lunar = (pow(moonDot, 82.0) * 0.35 + pow(moonDot, 28.0) * 0.085) * moonLight;
    vec3 solarColor = mix(vec3(1.0, 0.67, 0.34), vec3(1.0, 0.96, 0.80), day);
    result += (solarColor * sparkle * smoothstep(-0.02, 0.16, sun.y)
             + vec3(0.42, 0.63, 0.96) * lunar * (1.0 - day))
             * light * light * (1.0 - rainStrength * 0.84) * (0.52 + fresnel * 0.48);
    return result;
}
