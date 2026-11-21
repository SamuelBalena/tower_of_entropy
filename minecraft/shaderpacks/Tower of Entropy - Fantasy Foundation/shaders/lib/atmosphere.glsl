uniform int worldTime;
uniform float rainStrength;
uniform vec3 sunPosition;
uniform vec3 fogColor;

float daylight() {
    float t = float(worldTime) * (6.2831853 / 24000.0);
    return smoothstep(-0.19, 0.17, sin(t));
}
float goldenHour() {
    float t = float(worldTime) * (6.2831853 / 24000.0);
    return (1.0 - smoothstep(0.03, 0.45, abs(sin(t)))) * (1.0 - rainStrength);
}
vec3 sunTint() {
    return mix(vec3(1.04, 1.01, 0.94), vec3(1.20, 0.93, 0.70), goldenHour());
}
vec3 horizonColor() {
    vec3 night = mix(vec3(0.18, 0.24, 0.36), vec3(0.23, 0.25, 0.49), MAGIC);
    vec3 day = mix(vec3(0.64, 0.81, 0.93), vec3(1.0, 0.62, 0.43), goldenHour() * 0.75);
    return mix(mix(night, day, daylight()), fogColor, rainStrength * 0.82);
}
vec3 skyGradient(vec3 ray) {
    float up = smoothstep(-0.07, 0.82, ray.y);
    vec3 zenith = mix(vec3(0.052, 0.083, 0.19), vec3(0.31, 0.58, 0.87), daylight());
    zenith = mix(zenith, fogColor, rainStrength * 0.56);
    return mix(horizonColor(), zenith, up);
}
