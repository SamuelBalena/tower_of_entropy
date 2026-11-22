uniform sampler2D colortex3;

vec3 selectiveBloom(vec2 uv) {
    vec2 pixel = 1.0 / vec2(textureSize(colortex3, 0));
    vec2 stepSize = pixel * (1.25 + float(SELECTIVE_BLOOM));
    vec3 glow = vec3(0.0);
    for (int y = -1; y <= 1; ++y) {
        for (int x = -1; x <= 1; ++x) {
            vec2 p = clamp(uv + vec2(x, y) * stepSize, pixel * 0.5, 1.0 - pixel * 0.5);
            float weight = (x == 0 ? 2.0 : 1.0) * (y == 0 ? 2.0 : 1.0);
            glow += texture2D(colortex3, p).rgb * weight;
        }
    }
    return glow * ((0.12 + 0.045 * float(SELECTIVE_BLOOM)) / 16.0);
}
