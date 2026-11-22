uniform int currentRenderedItemId;

vec3 equipmentResponse(vec3 albedo, vec3 rgb, vec3 normal) {
    int id = currentRenderedItemId;
    if (id < 12001 || id > 12006) return rgb;
    float roughness = 0.86;
    vec3 f0 = vec3(0.024);
    if (id == 12002) { roughness = 0.63; f0 = vec3(0.24, 0.25, 0.27); }
    if (id == 12003) { roughness = 0.41; f0 = vec3(0.53, 0.55, 0.59); }
    if (id == 12004) { roughness = 0.34; f0 = vec3(0.81, 0.53, 0.18); }
    if (id == 12005) { roughness = 0.32; f0 = vec3(0.15, 0.48, 0.44); }
    if (id == 12006) { roughness = 0.49; f0 = vec3(0.27, 0.23, 0.25); }
    vec3 eye = normalize(-viewPos);
    float sky = clamp((lightUV.y * 16.0 - 0.5) / 15.0, 0.0, 1.0);
    float local = clamp((lightUV.x * 16.0 - 0.5) / 15.0, 0.0, 1.0);
    float facing = max(dot(normal, eye), 0.0);
    float edge = (1.0 - facing) * (1.0 - facing);
    vec3 halfSum = normalize(sunPosition) + eye;
    vec3 halfVector = halfSum * inversesqrt(max(dot(halfSum, halfSum), 0.0001));
    float sheen = pow(max(dot(normal, halfVector), 0.0), mix(66.0, 8.0, roughness));
    float exposure = 0.05 + local * local * 0.25 + sky * sky * 0.70;
    vec3 lobe = mix(vec3(0.55, 0.70, 0.91), sunTint(), sky * daylight());
    return rgb + f0 * lobe * mix(vec3(0.25), vec3(1.0), albedo)
        * exposure * (0.047 + edge * 0.16 + sheen * 0.24 * daylight() * (1.0 - rainStrength));
}
