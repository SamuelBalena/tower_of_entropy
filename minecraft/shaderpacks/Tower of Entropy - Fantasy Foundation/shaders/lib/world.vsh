varying vec2 uv, lightUV;
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
#ifdef WORLD_LIGHTING
    mat4 viewToShadow = shadowModelView * gbufferModelViewInverse;
    shadowViewPos = (viewToShadow * view).xyz;
    shadowViewNormal = mat3(viewToShadow) * normalView;
#endif
}
