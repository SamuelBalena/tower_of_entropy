#ifdef SOLID_MATERIALS
#include "/lib/options.glsl"
#endif
varying vec2 uv, lightUV;
#if defined(SOLID_MATERIALS) && MATERIAL_QUALITY > 0
varying float solidMaterial;
#if MATERIAL_LABPBR == 1 && defined(MC_TEXTURE_FORMAT_LAB_PBR_1_3) && MATERIAL_QUALITY >= 2
attribute vec4 at_tangent;
varying vec4 materialTangent;
#endif
#endif
varying vec4 tint;
varying vec3 viewPos, normalView;
#ifdef WORLD_LIGHTING
uniform mat4 gbufferModelViewInverse, shadowModelView;
varying vec3 shadowViewPos, shadowViewNormal;
#endif
#ifdef LAVA
attribute vec4 mc_Entity;
varying float lavaMaterial;
#endif
void main() {
#ifdef LAVA
    lavaMaterial = abs(mc_Entity.x - 11002.0) < 0.5 ? 1.0
                 : float(abs(mc_Entity.x - 11003.0) < 0.5) * 2.0;
#endif
    gl_Position = ftransform();
    uv = (gl_TextureMatrix[0] * gl_MultiTexCoord0).st;
    lightUV = (gl_TextureMatrix[1] * gl_MultiTexCoord1).st;
    tint = gl_Color;
    vec4 view = gl_ModelViewMatrix * gl_Vertex;
    viewPos = view.xyz;
    normalView = normalize(gl_NormalMatrix * gl_Normal);
#if defined(SOLID_MATERIALS) && MATERIAL_QUALITY > 0
    solidMaterial = mc_Entity.x;
#if MATERIAL_LABPBR == 1 && defined(MC_TEXTURE_FORMAT_LAB_PBR_1_3) && MATERIAL_QUALITY >= 2
    materialTangent = vec4(gl_NormalMatrix * at_tangent.xyz, at_tangent.w);
#endif
#endif
#ifdef WORLD_LIGHTING
    mat4 viewToShadow = shadowModelView * gbufferModelViewInverse;
    shadowViewPos = (viewToShadow * view).xyz;
    shadowViewNormal = mat3(viewToShadow) * normalView;
#endif
}
