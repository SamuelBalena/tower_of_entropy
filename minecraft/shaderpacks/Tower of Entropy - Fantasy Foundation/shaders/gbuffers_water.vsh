#version 120
attribute vec4 mc_Entity;
varying float waterMaterial;
#define main terrainVertex
#include "/lib/world.vsh"
#undef main
void main() {
    terrainVertex();
    waterMaterial = float(abs(mc_Entity.x - 11001.0) < 0.5);
}
