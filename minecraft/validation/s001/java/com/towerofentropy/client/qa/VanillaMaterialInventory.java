package com.towerofentropy.client.qa;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

final class VanillaMaterialInventory {
    private VanillaMaterialInventory() {}

    static void write(Minecraft client) {
        var rows = new ArrayList<String>();
        rows.add("block,model,solid,cutout,translucent,special_model,block_entity,fluid,waterloggable,animated_sprite,emission_max");
        for (var block : BuiltInRegistries.BLOCK) {
            var key = BuiltInRegistries.BLOCK.getKey(block);
            if (!key.getNamespace().equals("minecraft")) continue;
            List<BlockState> states = block.getStateDefinition().getPossibleStates();
            boolean model = false, solid = false, cutout = false, translucent = false, special = false;
            boolean blockEntity = false, waterloggable = false, animated = false;
            int emission = 0;
            for (var state : states) {
                blockEntity |= state.hasBlockEntity();
                special |= state.getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED;
                waterloggable |= !state.getFluidState().isEmpty() && !(block instanceof LiquidBlock);
                emission = Math.max(emission, state.getLightEmission());
                if (state.getRenderShape() != RenderShape.MODEL) continue;
                model = true;
                var baked = client.getBlockRenderer().getBlockModel(state);
                var random = RandomSource.create(0);
                var layers = baked.getRenderTypes(state, random, ModelData.EMPTY);
                solid |= layers.contains(RenderType.solid());
                cutout |= layers.contains(RenderType.cutout()) || layers.contains(RenderType.cutoutMipped());
                translucent |= layers.contains(RenderType.translucent());
                if (animated) continue;
                animated = isAnimated(baked.getParticleIcon());
                for (var side : Direction.values()) {
                    for (var quad : baked.getQuads(state, side, RandomSource.create(0))) {
                        animated |= isAnimated(quad.getSprite());
                    }
                }
                for (var quad : baked.getQuads(state, null, RandomSource.create(0))) {
                    animated |= isAnimated(quad.getSprite());
                }
            }
            rows.add(String.join(",", key.toString(), Boolean.toString(model), Boolean.toString(solid),
                    Boolean.toString(cutout), Boolean.toString(translucent), Boolean.toString(special),
                    Boolean.toString(blockEntity), Boolean.toString(block instanceof LiquidBlock),
                    Boolean.toString(waterloggable), Boolean.toString(animated), Integer.toString(emission)));
        }
        try {
            Files.write(Path.of("vanilla-blocks-runtime.csv"), rows);
        } catch (IOException e) {
            throw new IllegalStateException("Could not write vanilla material inventory", e);
        }
    }

    private static boolean isAnimated(TextureAtlasSprite sprite) {
        return sprite.contents().getUniqueFrames().limit(2).count() > 1;
    }
}
