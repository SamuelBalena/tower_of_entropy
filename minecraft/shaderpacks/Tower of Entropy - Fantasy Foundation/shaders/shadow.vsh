#version 120
#include "/lib/options.glsl"
#if VEGETATION_WIND > 0
uniform mat4 shadowModelView, shadowModelViewInverse;
#include "/lib/vegetation.glsl"
#endif
varying vec2 uv;
varying float opacity;
void main() {
    gl_Position = ftransform();
#if VEGETATION_WIND > 0
    vec4 view = gl_ModelViewMatrix * gl_Vertex;
    vec3 player = (shadowModelViewInverse * view).xyz;
    view.xyz += mat3(shadowModelView) * vegetationOffset(player);
    gl_Position = gl_ProjectionMatrix * view;
#endif
    uv = (gl_TextureMatrix[0] * gl_MultiTexCoord0).st;
    opacity = gl_Color.a;
}
