varying vec2 uv, lightUV;
varying vec4 tint;
varying vec3 viewPos, normalView;
#ifdef TERRAIN
uniform mat4 gbufferModelViewInverse, shadowModelView, shadowProjection;
varying vec3 shadowPos;
#endif
void main() {
    gl_Position = ftransform();
    uv = (gl_TextureMatrix[0] * gl_MultiTexCoord0).st;
    lightUV = (gl_TextureMatrix[1] * gl_MultiTexCoord1).st;
    tint = gl_Color;
    vec4 view = gl_ModelViewMatrix * gl_Vertex;
    viewPos = view.xyz;
    normalView = normalize(gl_NormalMatrix * gl_Normal);
#ifdef TERRAIN
    vec4 clip = shadowProjection * shadowModelView * gbufferModelViewInverse * view;
    shadowPos = clip.xyz / clip.w * 0.5 + 0.5;
#endif
}
