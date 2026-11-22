uniform mat4 gbufferModelViewInverse;
uniform vec3 cameraPosition;
uniform float toeCold, rainStrength, thunderStrength, frameTimeCounter;

vec3 snowDrift(vec3 view) {
    float winter = toeCold * rainStrength;
    if (winter < 0.015) return vec3(0.0);
    vec3 origin = (gbufferModelViewInverse * vec4(view, 1.0)).xyz + cameraPosition;
    float gust = sin(dot(origin.xz, vec2(0.28, -0.17)) + origin.y * 0.34 - frameTimeCounter * 1.25);
    float strength = winter * (0.020 + 0.021 * float(WINTER_WEATHER)) * (1.0 + thunderStrength * 0.7);
    strength *= 1.0 - smoothstep(12.0, 38.0, length(view));
    vec3 offset = vec3(0.52 + gust * 0.48, 0.0, 0.38 + gust * 0.28) * strength;
    return vec3(dot(gbufferModelViewInverse[0].xyz, offset),
                dot(gbufferModelViewInverse[1].xyz, offset),
                dot(gbufferModelViewInverse[2].xyz, offset));
}
