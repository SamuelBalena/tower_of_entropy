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
varying float waterMaterial;
varying float translucentMaterial;
#include "/lib/water_optics.glsl"
#endif
#ifdef LAVA
varying float lavaMaterial;
#include "/lib/lava.glsl"
#endif
#if DIMENSION_FOG > 0
#include "/lib/dimension_atmosphere.glsl"
#endif
#if FOG_MODE > 0
#include "/lib/region.glsl"
#if !defined(WATER) && !defined(LAVA)
uniform vec3 cameraPosition;
#endif
#if !defined(WORLD_LIGHTING) && !defined(WATER)
uniform mat4 gbufferModelViewInverse;
#endif
#ifndef WATER
uniform int isEyeInWater, moonPhase;
uniform vec3 moonPosition;
#endif
uniform int worldDay;
#if (WINTER_WEATHER > 0 || (defined(SOLID_MATERIALS) && WET_SURFACES > 0)) && !defined(WATER) && !defined(LAVA)
uniform float frameTimeCounter;
#endif
#include "/lib/cinematic_fog.glsl"
#endif
#if SELECTIVE_BLOOM > 0
#define TOE_REFLECTION_TARGET 2
#else
#define TOE_REFLECTION_TARGET 1
#endif
#if defined(SOLID_MATERIALS) && MATERIAL_QUALITY > 0
varying float solidMaterial;
#include "/lib/materials.glsl"
#endif
#if defined(EQUIPMENT) && EQUIPMENT_PBR == 1
#include "/lib/equipment.glsl"
#endif
#if SOLID_REFLECTIONS > 0
const int RGBA16F = 34842;
const int colortex6Format = RGBA16F;
const bool colortex6Clear = true;
const vec4 colortex6ClearColor = vec4(0.0, 0.0, 0.0, 0.0);
#endif
#if SELECTIVE_BLOOM > 0
const bool colortex3Clear = true;
const vec4 colortex3ClearColor = vec4(0.0, 0.0, 0.0, 0.0);
#endif
#if defined(WEATHER) && WINTER_WEATHER > 0 && FOG_MODE == 0
uniform float toeCold, thunderStrength;
#endif
#if (SELECTIVE_BLOOM == 0 && SOLID_REFLECTIONS == 0) || defined(WEATHER)
/* DRAWBUFFERS:0 */
#elif SELECTIVE_BLOOM == 0
/* DRAWBUFFERS:06 */
#elif SOLID_REFLECTIONS == 0
/* DRAWBUFFERS:03 */
#else
/* DRAWBUFFERS:036 */
#endif
void main() {
    vec4 base = texture2D(gtexture, uv) * tint;
    if (base.a < alphaTestRef) discard;
#if SELECTIVE_BLOOM > 0 && !defined(WEATHER)
    gl_FragData[1] = vec4(0.0, 0.0, 0.0, base.a);
#endif
#if SOLID_REFLECTIONS > 0 && !defined(WEATHER)
    gl_FragData[TOE_REFLECTION_TARGET] = vec4(0.0);
#endif
#ifdef LAVA
    // Derivatives of the actual mesh also identify fluid sides with an up-facing vertex normal.
    vec3 face = cross(dFdx(viewPos), dFdy(viewPos));
    if (lavaMaterial > 0.5) {
        float fog = smoothstep(fogStart, max(fogStart + 0.1, fogEnd), length(viewPos));
        vec3 molten = lavaColor(viewPos, face, lavaMaterial > 1.5, base.rg);
#if FOG_MODE > 0
        molten = applyCinematicFog(molten, lightingFog(), fog, viewPos, lightUV.y);
#else
        molten = mix(molten, lightingFog(), fog);
#endif
#if SELECTIVE_BLOOM > 0
        gl_FragData[1] = vec4(clamp(molten - 0.28, 0.0, 1.0) * 0.60, 1.0);
#endif
        gl_FragData[0] = vec4(molten, 1.0);
        return;
    }
#endif
#ifdef WORLD_LIGHTING
    vec3 normal = normalize(normalView);
#if defined(SOLID_MATERIALS) && MATERIAL_QUALITY > 0
    vec3 rgb = shadeSolidMaterial(base.rgb, normal);
#else
    vec3 rgb = base.rgb * worldLighting(viewPos, normal, lightUV);
#if defined(BLOCK_ENTITY_MATERIAL) && MATERIAL_QUALITY > 0
    // Block-entity textures are not the terrain atlas. Keep their tint and geometric normal.
    float edge = 1.0 - max(dot(normal, normalize(-viewPos)), 0.0);
    rgb += base.rgb * lightUV.y * MATERIAL_SPECULAR * 0.022 * edge * edge;
#endif
#endif
    vec3 haze = lightingFog();
#else
    vec3 lm = texture2D(lightmap, lightUV).rgb;
    vec3 rgb = base.rgb * lm;
    vec3 haze = fogColor;
#endif
#if defined(WEATHER) && WINTER_WEATHER > 0
    rgb = mix(rgb, rgb * vec3(1.01, 1.06, 1.13), toeCold * rainStrength * (0.34 + 0.30 * thunderStrength));
#endif
#if defined(EQUIPMENT) && EQUIPMENT_PBR == 1
    rgb = equipmentResponse(base.rgb, rgb, normalize(normalView));
#endif
#ifdef WATER
    if (waterMaterial > 0.5) {
        vec3 water = waterColor(viewPos, normalView, lightUV);
#if FOG_MODE > 0
        if (toeOverworld > 0.5 && isEyeInWater == 0 && fogEnd >= 28.0) {
            float planeFade = smoothstep(fogStart, max(fogStart + 0.1, fogEnd), length(viewPos));
            water = applyCinematicFog(water, haze, planeFade, viewPos, lightUV.y);
        }
#endif
        gl_FragData[0] = vec4(water, 1.0);
        return;
    }
#if MATERIAL_QUALITY > 0
    // Keep the original alpha and write order for every non-fluid translucent model.
    if (abs(translucentMaterial - 11112.0) < 0.5) {
        float edge = 1.0 - abs(dot(normalize(normalView), normalize(-viewPos)));
        float sheen = MATERIAL_SPECULAR * lightUV.y * (0.014 + 0.045 * edge * edge);
        rgb += rgb * sheen;
    }
#endif
#endif
    float fog = smoothstep(fogStart, max(fogStart + 0.1, fogEnd), length(viewPos));
#if defined(WORLD_LIGHTING)
#if FOG_MODE > 0
    if (toeOverworld < 0.5 || isEyeInWater != 0 || fogEnd < 28.0)
#endif
        fog = max(fog, smoothstep(24.0, 112.0, length(viewPos)) * lightUV.y * (0.14 + rainStrength * 0.14));
#endif
#if FOG_MODE > 0
    rgb = applyCinematicFog(rgb, haze, fog, viewPos, lightUV.y);
#else
#if DIMENSION_FOG > 0
    if (toeNether + toeEnd > 0.5) rgb = dimensionFog(rgb, haze, fog, viewPos);
    else
#endif
        rgb = mix(rgb, haze, fog);
#endif
    float opacity = base.a;
#if defined(WEATHER) && WINTER_WEATHER > 0
    opacity *= 1.0 - 0.65 * toeCold * (1.0 - smoothstep(0.65, 2.4, length(viewPos)));
#endif
    gl_FragData[0] = vec4(rgb, opacity);
}
