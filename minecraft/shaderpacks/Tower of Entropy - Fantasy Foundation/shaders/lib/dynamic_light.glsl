uniform int heldBlockLightValue, heldBlockLightValue2;
uniform vec3 heldBlockLightColor, heldBlockLightColor2;

vec3 heldLighting(vec3 view, vec3 normal, vec2 levels) {
    float power = max(float(heldBlockLightValue), float(heldBlockLightValue2)) / 15.0;
    if (power <= 0.0) return vec3(0.0);
    vec3 offset = vec3(0.0, -0.12, -0.20) - view;
    float distanceSquared = dot(offset, offset);
    const float radius = 3.5 + float(DYNAMIC_LIGHT) * 1.4;
    float reach = radius * max(0.4, power);
    if (distanceSquared >= reach * reach) return vec3(0.0);
    float fade = 1.0 - smoothstep(0.0, reach * reach, distanceSquared);
    float facing = 0.23 + 0.77 * max(dot(normal, offset * inversesqrt(max(distanceSquared, 0.01))), 0.0);
    vec3 source = heldBlockLightValue >= heldBlockLightValue2 ? heldBlockLightColor : heldBlockLightColor2;
    vec3 color = vec3(1.0, 0.72, 0.42);
    if (length(source) > 0.01 && max(abs(source.r - source.g), abs(source.g - source.b)) > 0.02) {
        color = clamp(source, 0.0, 1.0);
    }
    return color * (fade * fade * facing * power * (0.13 + 0.08 * float(DYNAMIC_LIGHT))
        * (1.0 - levels.x * levels.x));
}
