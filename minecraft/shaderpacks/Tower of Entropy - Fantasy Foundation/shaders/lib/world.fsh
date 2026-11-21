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
#if defined(SOLID_MATERIALS) && MATERIAL_QUALITY > 0
varying float solidMaterial;
#include "/lib/materials.glsl"
#endif
/* DRAWBUFFERS:0 */
void main() {
    vec4 base = texture2D(gtexture, uv) * tint;
    if (base.a < alphaTestRef) discard;
#ifdef LAVA
    // Derivatives of the actual mesh also identify fluid sides with an up-facing vertex normal.
    vec3 face = cross(dFdx(viewPos), dFdy(viewPos));
    if (lavaMaterial > 0.5) {
        float fog = smoothstep(fogStart, max(fogStart + 0.1, fogEnd), length(viewPos));
        vec3 molten = lavaColor(viewPos, face, lavaMaterial > 1.5, base.rg);
        gl_FragData[0] = vec4(mix(molten, lightingFog(), fog), 1.0);
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
#ifdef WATER
    if (waterMaterial > 0.5) {
        gl_FragData[0] = vec4(waterColor(viewPos, normalView, lightUV), 1.0);
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
#ifdef WORLD_LIGHTING
    fog = max(fog, smoothstep(24.0, 112.0, length(viewPos)) * lightUV.y * (0.14 + rainStrength * 0.14));
#endif
    gl_FragData[0] = vec4(mix(rgb, haze, fog), base.a);
}
