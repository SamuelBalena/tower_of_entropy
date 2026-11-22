struct SurfaceMaterial {
    float roughness;
    vec3 f0;
    float metal;
    float ao;
    float emission;
};

SurfaceMaterial fallbackMaterial(float encodedId) {
    // GLSL 1.20 interpolates varyings. Even identical vertex IDs can drift by a fraction of a unit.
    float id = floor(encodedId + 0.5);
    SurfaceMaterial m = SurfaceMaterial(0.84, vec3(0.025), 0.0, 1.0, 0.0);
    if (id == 11101.0) m.roughness = 0.86;
    if (id == 11102.0) m.roughness = 0.73;
    if (id == 11103.0) { m.roughness = 0.69; m.f0 = vec3(0.026); }
    if (id == 11104.0) { m.roughness = 0.38; m.f0 = vec3(0.55, 0.56, 0.58); m.metal = 0.72; }
    if (id == 11105.0) { m.roughness = 0.30; m.f0 = vec3(0.91, 0.66, 0.27); m.metal = 0.78; }
    if (id == 11106.0) { m.roughness = 0.46; m.f0 = vec3(0.81, 0.49, 0.29); m.metal = 0.65; }
    if (id == 11107.0) m.roughness = 0.82;
    if (id == 11108.0 || id == 11110.0) { m.roughness = 0.92; m.f0 = vec3(0.020); }
    if (id == 11109.0) { m.roughness = 0.44; m.f0 = vec3(0.047); }
    if (id == 11111.0 || (id >= 11201.0 && id <= 11207.0)) { m.roughness = 0.95; m.f0 = vec3(0.018); }
    if (id == 11112.0) { m.roughness = 0.28; m.f0 = vec3(0.042); }
    if (id == 11113.0) { m.roughness = 0.74; m.emission = 0.44; }
    if (id == 11114.0) { m.roughness = 0.51; m.f0 = vec3(0.20, 0.33, 0.28); m.metal = 0.21; }
    if (id == 11115.0) { m.roughness = 0.66; m.f0 = vec3(0.030); }
    if (id == 11116.0) { m.roughness = 0.62; m.f0 = vec3(0.035); }
    return m;
}

#if MATERIAL_LABPBR == 1 && defined(MC_TEXTURE_FORMAT_LAB_PBR_1_3)
uniform sampler2D specular;
#if MATERIAL_QUALITY >= 2
uniform sampler2D normals;
varying vec4 materialTangent;
#endif

vec3 conductorF0(vec3 n, vec3 k) {
    vec3 minusOne = n - 1.0;
    vec3 plusOne = n + 1.0;
    return (minusOne * minusOne + k * k) / (plusOne * plusOne + k * k);
}

vec3 labMetalF0(float code, vec3 albedo) {
    if (code == 230.0) return conductorF0(vec3(2.9114, 2.9497, 2.5845), vec3(3.0893, 2.9318, 2.7670));
    if (code == 231.0) return conductorF0(vec3(0.18299, 0.42108, 1.3734), vec3(3.4242, 2.3459, 1.7704));
    if (code == 232.0) return conductorF0(vec3(1.3456, 0.96521, 0.61722), vec3(7.4746, 6.3995, 5.3031));
    if (code == 233.0) return conductorF0(vec3(3.1071, 3.1812, 2.3230), vec3(3.3314, 3.3291, 3.1350));
    if (code == 234.0) return conductorF0(vec3(0.27105, 0.67693, 1.3164), vec3(3.6092, 2.6248, 2.2921));
    if (code == 235.0) return conductorF0(vec3(1.9100, 1.8300, 1.4400), vec3(3.5100, 3.4000, 3.1800));
    if (code == 236.0) return conductorF0(vec3(2.3757, 2.0847, 1.8453), vec3(4.2655, 3.7153, 2.3808));
    if (code == 237.0) return conductorF0(vec3(0.15943, 0.14512, 0.13547), vec3(3.9291, 3.1900, 2.3808));
    return albedo;
}

bool readLabPbr(inout SurfaceMaterial m, inout vec3 normal, vec3 albedo) {
    vec4 packedSpec = texture2D(specular, uv);
    // Oculus uses transparent black for atlas entries missing a specular map.

    bool hasSpec = any(greaterThan(packedSpec, vec4(0.5 / 255.0)));
    if (hasSpec) {
        float perceptualRoughness = 1.0 - packedSpec.r;
        m.roughness = clamp(perceptualRoughness * perceptualRoughness, 0.13, 1.0);
        float code = floor(packedSpec.g * 255.0 + 0.5);
        m.metal = step(230.0, code);
        m.f0 = code < 230.0 ? vec3(packedSpec.g) : labMetalF0(code, albedo);
        m.emission = packedSpec.a >= 254.5 / 255.0 ? 0.0 : packedSpec.a * (255.0 / 254.0);
    }
#if MATERIAL_QUALITY >= 2
    vec3 packedNormal = texture2D(normals, uv).rgb;

    m.ao = packedNormal.b;
    vec3 tangent = materialTangent.xyz - normal * dot(normal, materialTangent.xyz);
    float tangentLength = dot(tangent, tangent);
    if (tangentLength < 0.00001 || abs(materialTangent.w) < 0.5) return hasSpec;

    tangent *= inversesqrt(tangentLength);
    vec3 bitangent = cross(tangent, normal) * sign(materialTangent.w);
    // Iris tangents follow atlas U and V. LabPBR green follows increasing V.
    vec2 xy = packedNormal.rg * 2.0 - 1.0;
    xy *= step(2.0 / 255.0, length(xy));
    xy *= MATERIAL_NORMAL;
    float z = sqrt(max(1.0 - dot(xy, xy), 0.0001));
    normal = normalize(tangent * xy.x + bitangent * xy.y + normal * z);
#endif
    return hasSpec;
}
#endif

float fresnelWeight(float cosine) {
    float complement = 1.0 - cosine;
    float squared = complement * complement;
    return squared * squared * complement;
}

vec3 materialSpecular(SurfaceMaterial m, WorldLight light, vec3 normal, vec3 eye) {
    float nl = max(dot(normal, light.sun), 0.0);
    float nv = max(dot(normal, eye), 0.001);
    vec3 halfSum = light.sun + eye;
    float lengthSquared = dot(halfSum, halfSum);
    if (nl <= 0.0 || lengthSquared < 0.00001 || dot(light.irradiance, light.irradiance) == 0.0) return vec3(0.0);
    vec3 halfVector = halfSum * inversesqrt(lengthSquared);
    float nh = max(dot(normal, halfVector), 0.0);
    vec3 fresnel = mix(m.f0, vec3(1.0), fresnelWeight(clamp(dot(halfVector, eye), 0.0, 1.0)));
    float roughnessSquared = m.roughness * m.roughness;
#if MATERIAL_QUALITY == 1
    float lobe = pow(nh, mix(72.0, 5.0, m.roughness)) * mix(1.1, 0.22, m.roughness);
#else
    float denom = nh * nh * (roughnessSquared - 1.0) + 1.0;
    float distribution = roughnessSquared / (3.14159265 * denom * denom);
#if MATERIAL_QUALITY == 3
    float viewMask = nl * sqrt(nv * nv * (1.0 - roughnessSquared) + roughnessSquared);
    float lightMask = nv * sqrt(nl * nl * (1.0 - roughnessSquared) + roughnessSquared);
    float visibility = 0.5 / max(viewMask + lightMask, 0.01);
#else
    float visibility = 0.25 / max(mix(1.0, nl * nv, 0.45), 0.1);
#endif
    float lobe = min(distribution * visibility, 1.8);
#endif
    return fresnel * light.irradiance * nl * lobe * mix(0.32, 1.0, clamp(max(m.f0.r, max(m.f0.g, m.f0.b)) * 3.0, 0.0, 1.0));
}

#if WET_SURFACES > 0
#include "/lib/rain_surface.glsl"
#endif

vec3 shadeSolidMaterial(vec3 albedo, vec3 geometryNormal) {
    SurfaceMaterial m = fallbackMaterial(solidMaterial);
    vec3 normal = geometryNormal;
    bool mappedEmission = false;
#if MATERIAL_LABPBR == 1 && defined(MC_TEXTURE_FORMAT_LAB_PBR_1_3)
    mappedEmission = readLabPbr(m, normal, albedo);
#endif

    float puddle = 0.0;
#if WET_SURFACES > 0
    puddle = rainSurface(m, albedo, normal, geometryNormal);
#endif
    WorldLight light = sampleWorldLight(viewPos, normal, lightUV, geometryNormal);
    vec3 eye = normalize(-viewPos);
    vec3 energy = light.ambient * mix(1.0, m.ao, 0.65) + light.direct + light.blockLight;
    vec3 body = albedo * compressLighting(energy);
    vec3 tintF0 = mix(m.f0, m.f0 * mix(vec3(0.55), vec3(1.0), albedo), m.metal * 0.45);
    m.f0 = tintF0;
    vec3 highlight = materialSpecular(m, light, normal, eye);
    vec3 skySheen = light.skyColor * light.sky * m.f0 * m.metal *
        (0.32 + 0.38 * fresnelWeight(max(dot(normal, eye), 0.0))) * (1.0 - 0.5 * m.roughness);
    body *= 1.0 - MATERIAL_SPECULAR * (0.32 * m.metal + 0.10 * max(m.f0.r, max(m.f0.g, m.f0.b)));
    vec3 wetSky = vec3(0.0);
#if PUDDLE_REFLECTIONS == 1 && WET_SURFACES >= 2
    wetSky = light.skyColor * light.sky * puddle * (0.025 + 0.17 * fresnelWeight(max(dot(normal, eye), 0.0)));
#endif
    vec3 specularLight = MATERIAL_SPECULAR * (highlight + skySheen + wetSky);
#if SELECTIVE_BLOOM > 0
    gl_FragData[1] = vec4(albedo * m.emission * smoothstep(0.10, 0.68, max(albedo.r, max(albedo.g, albedo.b))), 1.0);
#endif
#if SOLID_REFLECTIONS > 0
    float nv = max(dot(normal, eye), 0.0);
    float metal = m.metal * (1.0 - m.roughness) * 0.16;
    float polished = float(abs(solidMaterial - 11109.0) < 0.5) * 0.05;
#if PUDDLE_REFLECTIONS == 1 && WET_SURFACES >= 2
    polished += puddle * 0.31;
#endif
    float coverage = (metal + polished * (0.10 + 0.90 * fresnelWeight(nv))) * MATERIAL_SPECULAR;
    if (coverage > 0.005 && tint.a > 0.99) {
        vec3 oct = normal / (abs(normal.x) + abs(normal.y) + abs(normal.z));
        if (oct.z < 0.0)
            oct.xy = (1.0 - abs(oct.yx)) * vec2(oct.x < 0.0 ? -1.0 : 1.0, oct.y < 0.0 ? -1.0 : 1.0);
        gl_FragData[TOE_REFLECTION_TARGET] = vec4(oct.xy * 0.5 + 0.5, coverage, -viewPos.z);
    }
#endif
    return mappedEmission ? body + specularLight + albedo * m.emission * 0.85
                          : max(body, albedo * m.emission) + specularLight;
}
