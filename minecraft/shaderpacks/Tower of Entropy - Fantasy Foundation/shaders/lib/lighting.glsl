#include "/lib/shadow.glsl"

uniform mat4 gbufferModelViewInverse;

vec3 worldLighting(vec3 viewPos, vec3 normal, vec2 lightUV) {
    vec3 sun = normalize(sunPosition);
    vec3 up = vec3(gbufferModelViewInverse[0].y, gbufferModelViewInverse[1].y, gbufferModelViewInverse[2].y);
    float elevation = dot(sun, up);
    float day = smoothstep(-0.14, 0.16, elevation);
    float twilight = (1.0 - smoothstep(0.02, 0.44, abs(elevation))) * (1.0 - rainStrength);
    float rain = clamp(rainStrength, 0.0, 1.0);

    // Lightmap coordinates carry the two voxel light levels independently.
    vec2 levels = clamp((lightUV * 16.0 - 0.5) / 15.0, 0.0, 1.0);
    float sky = levels.y * levels.y;
    float facingSky = dot(normal, up) * 0.5 + 0.5;
    vec3 skyColor = mix(vec3(0.23, 0.34, 0.58), vec3(0.62, 0.74, 0.91), day);
    skyColor = mix(skyColor, vec3(0.56, 0.47, 0.65), twilight * (0.32 + MAGIC * 0.25));
    skyColor = mix(skyColor, vec3(0.52, 0.61, 0.68), rain * day);
    vec3 ambient = vec3(0.14, 0.15, 0.18) + skyColor * sky * mix(0.46, 0.72, facingSky);

    vec3 direct = vec3(0.0);
    float incidence = max(dot(normal, sun), 0.0);
    float sunlight = incidence * smoothstep(0.015, 0.26, elevation) * (1.0 - rain * 0.88);
    sunlight *= smoothstep(0.42, 0.92, levels.y);
    if (sunlight > 0.001) {
        vec3 solarColor = mix(vec3(1.12, 1.04, 0.89), vec3(1.32, 0.79, 0.44), twilight);
        direct = solarColor * sunlight * sunVisibility(viewPos, incidence) * 0.70;
    }

    vec3 emitted = vec3(1.18, 0.83, 0.49) * pow(levels.x, 2.0);
    // Combine illumination before multiplying albedo. Keep hot texels from washing out.
    vec3 energy = ambient + direct + emitted * (1.0 - 0.36 * day * sky);
    return energy / (1.0 + 0.20 * max(energy - 1.0, vec3(0.0)));
}

vec3 lightingFog() {
    float elevation = dot(normalize(sunPosition),
        vec3(gbufferModelViewInverse[0].y, gbufferModelViewInverse[1].y, gbufferModelViewInverse[2].y));
    float twilight = (1.0 - smoothstep(-0.09, 0.30, elevation))
                   * smoothstep(-0.24, -0.06, elevation);
    return mix(fogColor, mix(fogColor, vec3(0.86, 0.49, 0.34), 0.36), twilight * (1.0 - rainStrength));
}
