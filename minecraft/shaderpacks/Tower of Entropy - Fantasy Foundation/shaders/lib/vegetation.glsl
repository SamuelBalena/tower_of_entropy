attribute vec4 mc_Entity;
attribute vec3 at_midBlock;
uniform vec3 cameraPosition;
uniform float frameTimeCounter, rainStrength, thunderStrength;

vec3 vegetationOffset(vec3 player) {
    float id = floor(mc_Entity.x + 0.5);
    if (id < 11201.0 || id > 11207.0) return vec3(0.0);
    vec3 world = player + cameraPosition;
    float height = clamp(0.5 - at_midBlock.y / 64.0, 0.0, 1.0);
    float anchor = height * height;
    float amplitude = 0.078;
    if (id == 11202.0) { amplitude = 0.033; anchor = 1.0; } // Canopy
    if (id == 11203.0) amplitude = 0.044; // Flowers and crops
    if (id == 11204.0) { amplitude = 0.032; anchor = 1.0 - height; } // Hanging plants
    if (id == 11205.0) { // Submerged plants
        float phase = dot(world, vec3(0.42, -0.52, 0.34)) + frameTimeCounter * 1.52;
        return vec3(sin(phase), 0.0, cos(phase)) * 0.039 * (0.4 + 0.3 * float(VEGETATION_WIND));
    }
    if (id == 11206.0) { amplitude = 0.036; anchor = 1.0; } // Tall stalks keep shared seams
    if (id == 11207.0) anchor = 1.0 + 0.12 * height; // Upper halves of double plants
    float speed = 1.0 + rainStrength * 0.28 + thunderStrength * 0.36;
    float time = frameTimeCounter;
    float phase = dot(world.xz, vec2(0.59, 0.41)) + world.y * 0.31;
    float gust = sin(dot(world.xz, vec2(0.088, -0.056)) - time * 0.93);
    float sway = sin(phase - time * 1.63) * 0.60 + sin(phase * 1.67 - time * 2.46) * 0.24;
    vec3 field = vec3(sway + 0.47 * gust, -0.13 * sway * sway, 0.37 * sway + 0.19 * gust);
    return field * (amplitude * anchor * speed * (0.35 + 0.30 * float(VEGETATION_WIND)));
}
