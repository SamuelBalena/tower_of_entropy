#version 150
uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;
uniform sampler2D TranslucentSampler;
uniform sampler2D TranslucentDepthSampler;
uniform sampler2D ItemEntitySampler;
uniform sampler2D ItemEntityDepthSampler;
uniform sampler2D ParticlesSampler;
uniform sampler2D ParticlesDepthSampler;
uniform sampler2D WeatherSampler;
uniform sampler2D WeatherDepthSampler;
uniform sampler2D CloudsSampler;
uniform sampler2D CloudsDepthSampler;
uniform sampler2D PlayerColor;
uniform sampler2D PlayerDepth;
uniform float PlayerOpacity;

in vec2 texCoord;
out vec4 fragColor;

struct Layer {
    vec4 color;
    float depth;
};
Layer layers[7];
int count = 0;

void insertLayer(vec4 color, float depth) {
    if (color.a <= 0.0) return;
    int index = count++;
    while (index > 0 && depth > layers[index - 1].depth) {
        layers[index] = layers[index - 1];
        --index;
    }
    layers[index] = Layer(color, depth);
}

void main() {
    float sceneDepth = texture(DiffuseDepthSampler, texCoord).r;
    insertLayer(vec4(texture(DiffuseSampler, texCoord).rgb, 1.0), sceneDepth);
    insertLayer(texture(TranslucentSampler, texCoord), texture(TranslucentDepthSampler, texCoord).r);
    insertLayer(texture(ItemEntitySampler, texCoord), texture(ItemEntityDepthSampler, texCoord).r);
    insertLayer(texture(ParticlesSampler, texCoord), texture(ParticlesDepthSampler, texCoord).r);
    insertLayer(texture(WeatherSampler, texCoord), texture(WeatherDepthSampler, texCoord).r);
    insertLayer(texture(CloudsSampler, texCoord), texture(CloudsDepthSampler, texCoord).r);
    if (PlayerOpacity > 0.0) {
        vec4 player = texture(PlayerColor, texCoord);
        if (player.a >= 0.001) {
            float depth = texture(PlayerDepth, texCoord).r;
            if (depth <= sceneDepth) insertLayer(player * PlayerOpacity, depth);
        }
    }

    vec3 color = layers[0].color.rgb;
    for (int i = 1; i < count; ++i)
        color = layers[i].color.rgb + color * (1.0 - layers[i].color.a);
    fragColor = vec4(color, 1.0);
}
