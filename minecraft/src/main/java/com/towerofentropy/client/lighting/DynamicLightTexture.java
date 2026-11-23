package com.towerofentropy.client.lighting;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import java.nio.FloatBuffer;
import java.util.function.IntSupplier;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL45;

/** Oculus bridge and a camera-relative metadata row followed by block collision bounds. */
public final class DynamicLightTexture {
    private static final String SAMPLER = "toeEquippedLightData";
    private static final int WIDTH = 256, CELLS = EquippedLightCollisionGrid.CELLS;
    private static final int MAX_LIGHTS = 4, HEIGHT = 1 + (CELLS + WIDTH - 1) / WIDTH;
    private static final float VERSION = -281;
    private static final boolean PROFILE = Boolean.getBoolean("toe.equippedLight.profile");
    private static final EquippedLightCollisionGrid grid = EquippedLightCollisionGrid.instance();
    private static FloatBuffer metadata, bounds;
    private static int texture;
    private static boolean directUpload;
    private static int previousCount = -1;
    private static long writtenFrame = -1;
    private static long sourceNanos, collisionNanos, uploadNanos, boundRows, activeFrames;
    private static Method registerSampler;
    private static boolean failed;

    private DynamicLightTexture() {}

    public static void beginFrame() {
        EquippedLightSources.beginFrame();
        grid.releaseOtherLevel(Minecraft.getInstance().level);
    }

    public static void resetWorld() {
        grid.reset();
        EquippedLightSources.reset();
    }

    public static long[] profileCounters() {
        return new long[]{sourceNanos, collisionNanos, uploadNanos, boundRows, activeFrames, grid.queries()};
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
        long start = PROFILE ? System.nanoTime() : 0;
        var sources = player == null || level == null
                ? java.util.List.<EquippedLightSources.Source>of() : EquippedLightSources.collect(player, partial);
        if (PROFILE) sourceNanos += System.nanoTime() - start;
        int count = Math.min(MAX_LIGHTS, sources.size());
        if (count == 0 && previousCount == 0) return texture;
        previousCount = count;
        int oldBinding = directUpload ? -1 : GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            if (texture == 0) allocate();
            else if (oldBinding >= 0) GlStateManager._bindTexture(texture);
            metadata.clear();
            if (count > 0) {
                start = PROFILE ? System.nanoTime() : 0;
                grid.update(level, player.blockPosition());
                var base = grid.origin();
                if (PROFILE) {
                    collisionNanos += System.nanoTime() - start;
                    activeFrames++;
                }
                if (grid.lastChanged() >= 0) uploadBounds();
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
            start = PROFILE ? System.nanoTime() : 0;
            upload(0, 1 + 2 * MAX_LIGHTS, 1, metadata);
            if (PROFILE) uploadNanos += System.nanoTime() - start;
        } finally {
            if (oldBinding >= 0) GlStateManager._bindTexture(oldBinding);
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
        directUpload = GL.getCapabilities().OpenGL45;
    }

    private static float packedAxis(long shape, int shift) {
        return (shape >>> shift & 255) | (shape >>> (shift + 24) & 255) << 8;
    }

    private static void uploadBounds() {
        long start = PROFILE ? System.nanoTime() : 0;
        int firstRow = grid.firstChanged() / WIDTH;
        int lastRow = grid.lastChanged() / WIDTH;
        bounds.clear();
        for (int i = firstRow * WIDTH; i < (lastRow + 1) * WIDTH; i++) {
            long shape = i < CELLS ? grid.cell(i) : 0;
            bounds.put(packedAxis(shape, 0)).put(packedAxis(shape, 8)).put(packedAxis(shape, 16))
                    .put(shape == 0 ? 0 : 1);
        }
        bounds.flip();
        int rows = lastRow - firstRow + 1;
        upload(firstRow + 1, WIDTH, rows, bounds);
        if (PROFILE) {
            uploadNanos += System.nanoTime() - start;
            boundRows += rows;
        }
    }

    private static void upload(int y, int width, int height, FloatBuffer data) {
        if (directUpload) {
            GL45.glTextureSubImage2D(texture, 0, 0, y, width, height, GL11.GL_RGBA, GL11.GL_FLOAT, data);
        } else {
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, y, width, height, GL11.GL_RGBA, GL11.GL_FLOAT, data);
        }
    }
}
