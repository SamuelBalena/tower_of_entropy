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
#include "/lib/water_optics.glsl"
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
        gl_FragData[0] = vec4(waterColor(viewPos, normalView, lightUV), 1.0);
        return;
    }
#endif
    float fog = smoothstep(fogStart, max(fogStart + 0.1, fogEnd), length(viewPos));
#ifdef WORLD_LIGHTING
    fog = max(fog, smoothstep(24.0, 112.0, length(viewPos)) * lightUV.y * (0.14 + rainStrength * 0.14));
#endif
    gl_FragData[0] = vec4(mix(rgb, haze, fog), base.a);
}
