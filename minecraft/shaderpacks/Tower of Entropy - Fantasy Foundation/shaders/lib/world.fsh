#include "/lib/options.glsl"
#include "/lib/atmosphere.glsl"
uniform sampler2D gtexture, lightmap;
uniform float alphaTestRef;
uniform float fogStart, fogEnd;
varying vec2 uv, lightUV;
varying vec4 tint;
varying vec3 viewPos, normalView;
#ifdef TERRAIN
uniform sampler2D shadowtex0;
varying vec3 shadowPos;
float directionalShadow(float ndl) {
    if (any(lessThan(shadowPos, vec3(0.005))) || any(greaterThan(shadowPos, vec3(0.995)))) return 1.0;
    float depth = shadowPos.z - mix(0.0013, 0.0005, ndl);
    vec2 texel = vec2(1.0 / float(SHADOW_RESOLUTION));
#if SHADOW_FILTER == 0
    return step(depth, texture2D(shadowtex0, shadowPos.xy).r);
#else
    float lit = 0.0;
    for (int y = -SHADOW_FILTER; y <= SHADOW_FILTER; ++y)
        for (int x = -SHADOW_FILTER; x <= SHADOW_FILTER; ++x)
            lit += step(depth, texture2D(shadowtex0, shadowPos.xy + vec2(x, y) * texel).r);
    return lit / float((2 * SHADOW_FILTER + 1) * (2 * SHADOW_FILTER + 1));
#endif
}
#endif
#ifdef WATER
uniform float frameTimeCounter;
uniform vec3 cameraPosition;
uniform mat4 gbufferModelViewInverse;
varying float waterMaterial;
#endif
/* DRAWBUFFERS:0 */
void main() {
    vec4 base = texture2D(gtexture, uv) * tint;
    if (base.a < alphaTestRef) discard;
    vec3 lm = texture2D(lightmap, lightUV).rgb;
    vec3 rgb = base.rgb * lm;
#ifdef TERRAIN
    float sky = smoothstep(0.45, 0.98, lightUV.y);
    float direct = max(dot(normalize(normalView), normalize(sunPosition)), 0.0);
    float solar = daylight() * (1.0 - rainStrength * 0.82) * sky;
    float silhouette = mix(0.78, 1.0, directionalShadow(direct));
    rgb *= mix(vec3(1.0), mix(vec3(silhouette), sunTint(), direct * silhouette * 0.80), solar);
    rgb += base.rgb * vec3(0.024, 0.034, 0.060) * sky * (1.0 - daylight()) * MAGIC;
    rgb *= mix(vec3(1.0), vec3(1.12, 1.04, 0.92), smoothstep(0.18, 0.92, lightUV.x) * (1.0 - sky * daylight()));
#endif
#ifdef WATER
    if (waterMaterial > 0.5) {
        vec3 ray = normalize(mat3(gbufferModelViewInverse) * viewPos);
        vec3 normal = normalize(mat3(gbufferModelViewInverse) * normalView);
#if WATER_DETAIL > 0
        vec2 p = ((gbufferModelViewInverse * vec4(viewPos, 1.0)).xz + cameraPosition.xz);
        normal.xz += 0.065 * vec2(sin(p.x * 1.9 + frameTimeCounter * 1.30 + p.y * 0.7),
                                  cos(p.y * 1.5 - frameTimeCounter * 0.87 + p.x));
        normal = normalize(normal);
#endif
        float fresnel = pow(1.0 - clamp(dot(-ray, normal), 0.0, 1.0), 3.0);
        rgb = mix(rgb, vec3(0.14, 0.46, 0.52) * lm, 0.30);
#if WATER_DETAIL == 2
        vec3 reflection = skyGradient(reflect(ray, normal));
#else
        vec3 reflection = horizonColor();
#endif
        rgb = mix(rgb, reflection * mix(0.32, 1.0, daylight()), fresnel * (0.19 + 0.10 * float(WATER_DETAIL)));
        base.a = clamp(base.a * 0.80 + fresnel * 0.16, 0.22, 0.76);
    }
#endif
    float fog = smoothstep(fogStart, max(fogStart + 0.1, fogEnd), length(viewPos));
    gl_FragData[0] = vec4(mix(rgb, fogColor, fog), base.a);
}
