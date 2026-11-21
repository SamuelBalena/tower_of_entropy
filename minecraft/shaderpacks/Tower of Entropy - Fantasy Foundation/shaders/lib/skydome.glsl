#include "/lib/celestial.glsl"
#include "/lib/cloudscape.glsl"

vec3 renderSkydome(vec3 ray, vec3 sun, vec3 moon) {
    float rain = clamp(rainStrength, 0.0, 1.0);
    float day = smoothstep(-0.17, 0.24, sun.y);
    float twilight = (1.0 - smoothstep(0.07, 0.56, abs(sun.y))) * smoothstep(-0.32, -0.07, sun.y);
    float up = smoothstep(-0.03, 0.88, ray.y);
    vec3 horizon = mix(vec3(0.19, 0.21, 0.34), vec3(0.65, 0.86, 0.94), day);
    vec3 dome = mix(vec3(0.083, 0.10, 0.24), vec3(0.25, 0.65, 0.88), day);
    vec3 zenith = mix(vec3(0.034, 0.059, 0.16), vec3(0.11, 0.36, 0.69), day);
    float warmSide = 0.5 + 0.5 * dot(normalize(vec3(ray.x, 0.001, ray.z)), normalize(vec3(sun.x, 0.001, sun.z)));
    float dawn = smoothstep(-0.4, 0.4, sun.x);
    vec3 warm = mix(vec3(0.98, 0.48, 0.31), vec3(0.87, 0.56, 0.58), dawn);
    horizon = mix(horizon, mix(vec3(0.46, 0.37, 0.56), warm, warmSide), twilight * 0.91);
    dome = mix(dome, mix(vec3(0.25, 0.25, 0.49), vec3(0.63, 0.38, 0.51), warmSide), twilight * 0.60);
    vec3 sky = mix(mix(horizon, dome, smoothstep(0.0, 0.42, up)), zenith, smoothstep(0.19, 1.0, up));
    sky = mix(sky, mix(vec3(0.12, 0.15, 0.23), vec3(0.43, 0.51, 0.62), day), rain * 0.82);
    float night = (1.0 - smoothstep(-0.29, -0.08, sun.y)) * (1.0 - rain);
    if (night > 0.001) sky += starLight(ray, night);
    sky = celestialLight(sky, ray, sun, moon, 1.0 - rain);
    return shadeClouds(sky, ray, sun, moon, day, twilight, rain);
}
