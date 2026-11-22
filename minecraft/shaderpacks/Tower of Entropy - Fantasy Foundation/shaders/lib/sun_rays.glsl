uniform mat4 shadowModelView;
uniform ivec2 eyeBrightnessSmooth;

vec3 solarScattering(vec3 view, vec3 displacement, vec3 sun, float ceiling) {
    float distanceToEye = length(view);
    float strength = smoothstep(0.006, 0.14, sun.y) * (1.0 - 0.94 * rainStrength);
    float travel = min(distanceToEye, shadowDistance * 0.80);
    if (strength < 0.01 || travel < 2.0) return vec3(0.0);
#if SUN_RAYS == 1
    const int samples = 5;
#elif SUN_RAYS == 2
    const int samples = 10;
#else
    const int samples = 16;
#endif
    vec4 start = shadowProjection * shadowModelView * gbufferModelViewInverse * vec4(0.0, 0.0, 0.0, 1.0);
    vec4 delta = shadowProjection * shadowModelView * gbufferModelViewInverse * vec4(view * (travel / distanceToEye), 0.0);
    float sum = 0.0;
    for (int i = 0; i < samples; ++i) {
        float t = (float(i) + 0.5) / float(samples);
        vec4 clip = start + delta * t;
        vec3 p = clip.xyz / clip.w * 0.5 + 0.5;
        vec3 border = min(p, 1.0 - p);
        float edge = min(border.x, min(border.y, border.z));
        if (edge <= 0.0) continue;
        float height = cameraPosition.y + displacement.y * (t * travel / distanceToEye);
        sum += compareShadow(p.xy, p.z) * smoothstep(0.0, 0.045, edge)
             * mix(0.24, 1.0, lowFogDensity(height, ceiling));
    }
    float forward = max(dot(normalize(displacement), sun), 0.0);
    float phase = 0.20 + 0.80 * pow(forward, 5.0);
    return sunTint() * (strength * phase * min(travel * 0.0027, 0.13) * sum / float(samples));
}
