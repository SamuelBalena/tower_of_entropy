#include "/lib/options.glsl"
#include "/lib/atmosphere.glsl"
uniform sampler2D gtexture, lightmap;
uniform float alphaTestRef;
uniform float fogStart, fogEnd;
varying vec2 uv, lightUV;
varying vec4 tint;
varying vec3 viewPos, normalView;
#ifdef WORLD_LIGHTING
#include "/lib/lighting.glsl"
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
#ifdef WORLD_LIGHTING
    vec3 rgb = base.rgb * worldLighting(viewPos, normalize(normalView), lightUV);
    vec3 haze = lightingFog();
#else
    vec3 lm = texture2D(lightmap, lightUV).rgb;
    vec3 rgb = base.rgb * lm;
    vec3 haze = fogColor;
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
#ifdef WORLD_LIGHTING
    fog = max(fog, smoothstep(24.0, 112.0, length(viewPos)) * lightUV.y * (0.14 + rainStrength * 0.14));
#endif
    gl_FragData[0] = vec4(mix(rgb, haze, fog), base.a);
}
