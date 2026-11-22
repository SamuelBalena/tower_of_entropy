uniform int worldDay;

float auroraHash(float n) {
    return fract(sin(n * 19.191 + 2.71) * 4375.11);
}

float auroraCurtain(vec3 ray, float phase, float offset, float detail) {
    // Three-dimensional direction remains continuous through the azimuth seam.
    float a = dot(ray.xz, vec2(0.58, 0.81));
    float s = dot(ray.xz, vec2(-0.81, 0.58));
    float fold = sin(s * 6.0 + phase * 0.42 + offset)
               + 0.38 * sin(s * 13.0 - phase * 0.28 + offset * 2.0);
    float base = 0.35 + 0.066 * fold + offset * 0.059 + a * 0.062;
    float height = (ray.y - base) / (0.25 + 0.052 * fold);
    float vertical = smoothstep(-0.06, 0.07, height) * (1.0 - smoothstep(0.12, 1.0, height));
    float silk = 0.56 + 0.30 * sin(s * 27.0 + fold * 3.0 + phase * 0.50 + height * 2.0);
    silk += detail * 0.14 * sin(s * 59.0 + fold * 7.0 - phase + height * 3.6);
    float arc = smoothstep(-0.65, 0.08, a) * (0.60 + 0.40 * cos(s * 3.7 + offset));
    float hem = exp(-pow((height - 0.05) * 9.0, 2.0));
    return vertical * silk * (0.71 + 0.29 * hem) * arc;
}

vec3 auroraLight(vec3 ray, vec3 sun, float rain) {
    float night = smoothstep(0.16, 0.37, -sun.y);
    float weather = 1.0 - smoothstep(0.12, 0.75, max(rain, thunderStrength));
    float active = night * weather * toeCold * toeOverworld;
    if (active < 0.002 || ray.y < 0.045 || ray.y > 0.85) return vec3(0.0);

    float chance = auroraHash(mod(float(worldDay), 8192.0));
    float occurrence = AURORA_FREQUENCY >= 0.999 ? 1.0 : 1.0 - smoothstep(AURORA_FREQUENCY * 0.88, AURORA_FREQUENCY, chance);
    if (occurrence < 0.002) return vec3(0.0);

    float phase = frameTimeCounter * 0.17 * AURORA_SPEED + chance * 6.2831853;
    float curtain = auroraCurtain(ray, phase, 0.0, 0.0);
    vec3 spectrum = mix(vec3(0.062, 0.83, 0.52), vec3(0.33, 0.40, 0.79), smoothstep(0.42, 0.70, ray.y));
    vec3 emission = spectrum * curtain * 0.89;
#if AURORA_QUALITY >= 2
    emission += vec3(0.071, 0.54, 0.73) * auroraCurtain(ray, phase * 0.83 + 1.7, 1.05, 0.7) * 0.42;
#endif
#if AURORA_QUALITY >= 3
    emission += vec3(0.31, 0.55, 0.59) * auroraCurtain(ray, phase * 0.76 + 3.8, -0.55, 1.0) * 0.24;
#endif
    float horizon = smoothstep(0.05, 0.23, ray.y);
    return emission * (active * occurrence * horizon * AURORA_INTENSITY);
}
