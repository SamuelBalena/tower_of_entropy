struct FogProfile {
    vec3 tint;
    float distanceDensity;
    float heightDensity;
    float ceiling;
};

FogProfile fogProfile() {
    float forest = toeForest * (1.0 - toeDry);
    float marsh = toeMarsh * (1.0 - toeDry);
    float mountain = toeMountain;
    vec3 tint = vec3(0.84, 0.91, 0.97);
    tint = mix(tint, vec3(0.77, 0.89, 0.83), forest * 0.58 + marsh * 0.40);
    tint = mix(tint, vec3(0.68, 0.82, 1.00), toeCold * 0.73);
    tint = mix(tint, vec3(1.03, 0.88, 0.69), toeDry * 0.65);
    return FogProfile(tint, 0.0025 + forest * 0.00035 + mountain * 0.0011
        + toeCold * 0.0008 + marsh * 0.0016, (1.0 - toeDry * 0.91)
        * (0.004 + forest * 0.003 + marsh * 0.009 + toeCold * 0.0015),
        70.0 + 8.0 * mountain + 2.0 * toeCold);
}

float fogDawn() {
    float time = float(worldTime);
    return max(smoothstep(21500.0, 24000.0, time), 1.0 - smoothstep(0.0, 4200.0, time));
}

float fogAmount() {
#if FOG_MODE == 1
    return FOG_INTENSITY * 0.56;
#elif FOG_MODE == 2
    return FOG_INTENSITY * 0.9;
#elif FOG_MODE == 3
    return FOG_INTENSITY * 1.52;
#else
    return FOG_INTENSITY;
#endif
}

vec3 fogScattering(FogProfile profile, vec3 sun, vec3 moon) {
    float morning = fogDawn();
    float day = smoothstep(-0.27, 0.28, sun.y);
    float twilight = (1.0 - smoothstep(0.015, 0.32, abs(sun.y))) * (1.0 - rainStrength);
    float lunar = smoothstep(0.0, 0.65, moon.y) * (0.5 + 0.5 * cos(float(moonPhase) * 0.7853982));
    vec3 night = vec3(0.13, 0.19, 0.31) + vec3(0.035, 0.050, 0.064) * lunar * (1.0 - rainStrength);
    vec3 color = mix(night, vec3(0.57, 0.72, 0.83), day);
    vec3 warm = mix(vec3(0.84, 0.53, 0.34), vec3(0.93, 0.68, 0.56), morning);
    color = mix(color, warm, twilight * 0.61);
    vec3 overcast = mix(vec3(0.15, 0.20, 0.27), vec3(0.46, 0.55, 0.64), day);
    color = mix(color, overcast, rainStrength * 0.86);
    return mix(color, color * profile.tint, 0.56);
}

float lowFogDensity(float height, float ceiling) {
    float x = clamp((ceiling - height) / 31.0, 0.0, 1.0);
    return x * x * (3.0 - 2.0 * x);
}

vec3 applyCinematicFog(vec3 color, vec3 legacyTint, float legacyFog, vec3 view, float skyLight) {
    if (toeOverworld < 0.5 || isEyeInWater != 0 || fogEnd < 28.0) {
        return mix(color, legacyTint, legacyFog);
    }

    float distanceToEye = length(view);
    float exposed = smoothstep(0.27, 0.88, skyLight);
    if (distanceToEye < 5.0 || exposed < 0.002) return mix(color, legacyTint, legacyFog);

    FogProfile profile = fogProfile();
    vec3 displacement = mat3(gbufferModelViewInverse) * view;
    float nearHeight = cameraPosition.y;
    float farHeight = nearHeight + displacement.y;
    float ceiling = profile.ceiling;
#if FOG_QUALITY >= 2
    vec2 field = (cameraPosition.xz + displacement.xz * 0.52) * 0.023;
    ceiling += sin(field.x + sin(field.y * 0.72)) * 4.2;
#endif
    float midpoint = lowFogDensity(mix(nearHeight, farHeight, 0.50), ceiling);
#if FOG_QUALITY >= 3
    float nearSample = lowFogDensity(mix(nearHeight, farHeight, 0.16), ceiling);
    float farSample = lowFogDensity(mix(nearHeight, farHeight, 0.84), ceiling);
    float layer = (nearSample + midpoint + farSample) / 3.0;
#elif FOG_QUALITY >= 2
    float layer = (lowFogDensity(nearHeight, ceiling) + 4.0 * midpoint + lowFogDensity(farHeight, ceiling)) / 6.0;
#else
    float layer = midpoint;
#endif
    // The pre-dawn buildup and the following morning share the same daily phase.
    float morningDay = float(worldDay) + step(21500.0, float(worldTime));
    float morning = fogDawn() * (0.63 + 0.37 * cos(morningDay * 1.71));
    float weather = 1.0 + 0.63 * rainStrength + 0.61 * thunderStrength;
    float distanceOptics = max(distanceToEye - 17.0, 0.0) * profile.distanceDensity;
    float heightOptics = min(max(distanceToEye - 5.0, 0.0), 148.0) * profile.heightDensity * layer
        * (0.40 + 0.92 * morning + 0.46 * rainStrength);
    float depth = (distanceOptics + heightOptics) * exposed * weather * fogAmount();
    float extinction = min(1.0 - exp(-depth), 0.76);
    vec3 sun = normalize(mat3(gbufferModelViewInverse) * sunPosition);
    vec3 moon = normalize(mat3(gbufferModelViewInverse) * moonPosition);
    vec3 scattering = fogScattering(profile, sun, moon);
    // Blend once. Retain only the engine's far-plane fade on top of the new optical depth.
    float combined = extinction + legacyFog * (1.0 - extinction);
    vec3 inscatter = scattering * extinction + legacyTint * (combined - extinction);
    return color * (1.0 - combined) + inscatter;
}
