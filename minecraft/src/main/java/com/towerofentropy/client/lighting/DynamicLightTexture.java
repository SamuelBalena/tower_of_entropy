package com.towerofentropy.client.lighting;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import java.nio.FloatBuffer;
import java.util.IdentityHashMap;
import java.util.function.IntSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

/** Oculus bridge and a camera-relative metadata row followed by block collision bounds. */
public final class DynamicLightTexture {
    private static final String SAMPLER = "toeEquippedLightData";
    private static final int WIDTH = 256, SIDE = 23, CELLS = SIDE * SIDE * SIDE;
    private static final int MAX_LIGHTS = 4, HEIGHT = 1 + (CELLS + WIDTH - 1) / WIDTH;
    private static final float VERSION = -281;
    private static final long FULL_BLOCK = 0xffffff000000L;
    private static final IdentityHashMap<BlockState, Long> SHAPES = new IdentityHashMap<>();
    private static final long[] cells = new long[CELLS];
    private static final BlockPos.MutableBlockPos query = new BlockPos.MutableBlockPos();
    private static FloatBuffer metadata, bounds;
    private static int texture;
    private static int previousCount = -1;
    private static long writtenFrame = -1, worldTick = Long.MIN_VALUE;
    private static ClientLevel world;
    private static BlockPos origin;
    private static Method registerSampler;
    private static boolean failed;

    private DynamicLightTexture() {}

    public static void beginFrame() {
        EquippedLightSources.beginFrame();
    }

    public static void resetWorld() {
        world = null;
        origin = null;
        SHAPES.clear();
        EquippedLightSources.reset();
    }

    /** Invoked from IrisSamplers.addLevelSamplers; uses reflection to keep Oculus optional. */
    public static void addSampler(Object builder) {
        if (failed) return;
        try {
            if (registerSampler == null) {
                var holder = Class.forName("net.irisshaders.iris.gl.sampler.SamplerHolder");
                registerSampler = holder.getMethod("addDynamicSampler", IntSupplier.class, String[].class);
            }
            registerSampler.invoke(builder, (IntSupplier) DynamicLightTexture::texture, new String[]{SAMPLER});
        } catch (ReflectiveOperationException | LinkageError error) {
            failed = true;
            LogUtils.getLogger().warn("Oculus equipped-light sampler unavailable", error);
        }
    }

    private static int texture() {
        long frame = EquippedLightSources.frame();
        if (writtenFrame == frame && texture != 0) return texture;
        writtenFrame = frame;
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        var player = minecraft.player;
        var camera = minecraft.gameRenderer.getMainCamera().getPosition();
        float partial = minecraft.getFrameTime();
        var sources = player == null || level == null
                ? java.util.List.<EquippedLightSources.Source>of() : EquippedLightSources.collect(player, partial);
        int count = Math.min(MAX_LIGHTS, sources.size());
        if (count == 0 && previousCount == 0) return texture;
        previousCount = count;
        int oldBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            if (texture == 0) allocate();
            else GlStateManager._bindTexture(texture);
            if (world != level) {
                world = level;
                origin = null;
                SHAPES.clear();
            }
            metadata.clear();
            if (count > 0) {
                var base = player.blockPosition().offset(-SIDE / 2, -SIDE / 2, -SIDE / 2);
                if (!base.equals(origin) || worldTick != level.getGameTime()) {
                    if (collectBounds(level, base) || origin == null) uploadBounds();
                    origin = base;
                    worldTick = level.getGameTime();
                }
                metadata.put((float) (base.getX() - camera.x)).put((float) (base.getY() - camera.y))
                        .put((float) (base.getZ() - camera.z)).put(VERSION);
                for (int i = 0; i < count; i++) {
                    var light = sources.get(i);
                    var p = light.position().subtract(camera);
                    var emission = light.emission();
                    float time = (level.getGameTime() % 24000 + partial) * .05f;
                    double phase = time * 11 + i * 2.3;
                    float flicker = 1 + emission.flicker() * (float) (.66 * Math.sin(phase) + .34 * Math.sin(phase * 1.61));
                    metadata.put((float) p.x).put((float) p.y).put((float) p.z).put(emission.radius());
                    metadata.put((float) emission.color().x).put((float) emission.color().y)
                            .put((float) emission.color().z).put(emission.strength() * flicker);
                }
            } else {
                metadata.put(0).put(0).put(0).put(VERSION);
            }
            while (metadata.hasRemaining()) metadata.put(0);
            metadata.flip();
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 1 + 2 * MAX_LIGHTS, 1,
                    GL11.GL_RGBA, GL11.GL_FLOAT, metadata);
        } finally {
            GlStateManager._bindTexture(oldBinding);
        }
        return texture;
    }

    private static void allocate() {
        metadata = BufferUtils.createFloatBuffer((1 + 2 * MAX_LIGHTS) * 4);
        bounds = BufferUtils.createFloatBuffer(WIDTH * (HEIGHT - 1) * 4);
        texture = GlStateManager._genTexture();
        GlStateManager._bindTexture(texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F, WIDTH, HEIGHT, 0,
                GL11.GL_RGBA, GL11.GL_FLOAT, (FloatBuffer) null);
    }

    private static boolean collectBounds(ClientLevel level, BlockPos base) {
        boolean changed = false;
        for (int z = 0; z < SIDE; z++) {
            for (int x = 0; x < SIDE; x++) {
                int worldX = base.getX() + x, worldZ = base.getZ() + z;
                boolean loaded = level.hasChunk(worldX >> 4, worldZ >> 4);
                for (int y = 0; y < SIDE; y++) {
                    query.set(worldX, base.getY() + y, worldZ);
                    long shape = loaded ? shape(level, level.getBlockState(query)) : FULL_BLOCK;
                    int index = (z * SIDE + y) * SIDE + x;
                    changed |= cells[index] != shape;
                    cells[index] = shape;
                }
            }
        }
        return changed;
    }

    private static long shape(ClientLevel level, BlockState state) {
        if (state.isAir()) return 0;
        boolean dynamic = state.getBlock().hasDynamicShape();
        if (!dynamic) {
            Long cached = SHAPES.get(state);
            if (cached != null) return cached;
        }
        long result = 0;
        if (!state.is(Tags.Blocks.GLASS) && !state.is(Tags.Blocks.GLASS_PANES) && !state.is(BlockTags.LEAVES)) {
            var collision = state.getCollisionShape(level, query);
            if (!collision.isEmpty()) {
                var box = collision.bounds();
                result = quantize(box.minX, false) | quantize(box.minY, false) << 8 | quantize(box.minZ, false) << 16
                       | quantize(box.maxX, true) << 24 | quantize(box.maxY, true) << 32 | quantize(box.maxZ, true) << 40;
            }
        }
        if (!dynamic) SHAPES.put(state, result);
        return result;
    }

    private static long quantize(double coordinate, boolean upper) {
        // Clip bounds to their voxel. Collision above fences is approximated.
        return (long) Math.max(0, Math.min(255, upper ? Math.ceil(coordinate * 255) : Math.floor(coordinate * 255)));
    }

    private static float packedAxis(long shape, int shift) {
        return (shape >>> shift & 255) | (shape >>> (shift + 24) & 255) << 8;
    }

    private static void uploadBounds() {
        bounds.clear();
        for (long shape : cells) {
            bounds.put(packedAxis(shape, 0)).put(packedAxis(shape, 8)).put(packedAxis(shape, 16))
                    .put(shape == 0 ? 0 : 1);
        }
        while (bounds.hasRemaining()) bounds.put(0);
        bounds.flip();
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 1, WIDTH, HEIGHT - 1,
                GL11.GL_RGBA, GL11.GL_FLOAT, bounds);
    }
}
