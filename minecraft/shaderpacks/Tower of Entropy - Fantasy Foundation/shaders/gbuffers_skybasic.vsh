#version 120
uniform mat4 gbufferModelViewInverse;
varying vec3 ray;
varying vec4 tint;
void main() {
    gl_Position = ftransform();
    ray = mat3(gbufferModelViewInverse) * (gl_ModelViewMatrix * gl_Vertex).xyz;
    tint = gl_Color;
}
