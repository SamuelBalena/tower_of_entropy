uniform sampler2D toeEquippedLightData;

vec4 equippedLightTexel(int x, int y) {
    return texelFetch(toeEquippedLightData, ivec2(x, y), 0);
}

vec4 equippedCell(int index) {
    return equippedLightTexel(index & 255, (index >> 8) + 1);
}

int equippedIndex(ivec3 cell) {
    return cell.x + 23 * (cell.y + 23 * cell.z);
}

// One texel per cell, with the minimum and maximum packed into each axis.
bool equippedLightVisible(vec3 start, vec3 end) {
    vec3 ray = end - start;
    ivec3 origin = ivec3(floor(start));
    if (any(lessThan(origin, ivec3(0))) || any(greaterThanEqual(origin, ivec3(23)))) return false;
    if (any(lessThan(end, vec3(0.0))) || any(greaterThanEqual(end, vec3(23.0)))) return false;
    vec3 direction = mix(vec3(-1.0), vec3(1.0), step(vec3(0.0), ray));
    vec3 inverse = direction / max(abs(ray), vec3(0.00001));
    vec3 next = (vec3(origin) + step(vec3(0.0), ray) - start) * inverse;
    float travel = 0.0;
    for (int i = 0; i < 27; i++) {
        vec4 cell = equippedCell(equippedIndex(origin));
        if (cell.a > 0.5) {
            ivec3 packedBounds = ivec3(cell.rgb);
            vec3 lower = vec3(packedBounds & 255) / 255.0;
            vec3 upper = vec3(packedBounds >> 8) / 255.0;
            vec3 p = (vec3(origin) + lower - start) * inverse;
            vec3 q = (vec3(origin) + upper - start) * inverse;
            vec3 nearSide = min(p, q), farSide = max(p, q);
            float enter = max(max(nearSide.x, nearSide.y), nearSide.z);
            float leave = min(min(farSide.x, farSide.y), farSide.z);
            if (max(enter, travel + 0.0001) < min(leave, 0.9999)) return false;
        }
        travel = min(next.x, min(next.y, next.z));
        if (travel >= 1.0) return true;
        // Move across all tied faces at a voxel edge or corner.
        vec3 advance = step(next, vec3(travel + 0.000001));
        origin += ivec3(direction * advance);
        next += abs(inverse) * advance;
    }
    return false;
}

vec3 equippedLighting(vec3 view, vec3 normal, vec3 geometryNormal, vec2 levels) {
#if DYNAMIC_LIGHT == 1
    const int sourceBudget = 1;
    const float reachScale = 0.76;
#elif DYNAMIC_LIGHT == 2
    const int sourceBudget = 2;
    const float reachScale = 0.91;
#else
    const int sourceBudget = 4;
    const float reachScale = 1.0;
#endif
    vec4 grid = equippedLightTexel(0, 0);
    if (grid.w != -281.0) return vec3(0.0);

    vec4 first = equippedLightTexel(1, 0);
    if (first.w <= 0.0) return vec3(0.0);
    vec3 receiver = (gbufferModelViewInverse * vec4(view, 1.0)).xyz;
    vec3 gridReceiver = receiver - grid.xyz + mat3(gbufferModelViewInverse) * geometryNormal * 0.018;
    vec3 worldNormal = mat3(gbufferModelViewInverse) * normal;
    vec3 result = vec3(0.0);
    for (int i = 0; i < sourceBudget; i++) {
        vec4 source = i == 0 ? first : equippedLightTexel(1 + 2 * i, 0);
        if (source.w <= 0.0) break;
        vec3 offset = source.xyz - receiver;
        float reach = source.w * reachScale;
        float distanceSquared = dot(offset, offset);
        if (distanceSquared >= reach * reach) continue;
        float facing = max(dot(worldNormal, offset * inversesqrt(max(distanceSquared, 0.01))), 0.0);
        if (facing < 0.002 || !equippedLightVisible(gridReceiver, source.xyz - grid.xyz)) continue;
        vec4 emission = equippedLightTexel(2 + 2 * i, 0);
        float falloff = 1.0 - distanceSquared / (reach * reach);
        float attenuation = falloff * falloff / (1.0 + distanceSquared * 0.11);
        result += emission.rgb * (emission.a * attenuation * facing);
    }
    return min(result, vec3(1.0)) * (0.56 * (1.0 - 0.68 * levels.x * levels.x));
}
