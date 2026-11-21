package com.towerofentropy.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.towerofentropy.client.camera.ShoulderCamera;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.AbstractUniform;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** Captures the existing local entity render once, then fades the combined visible silhouette. */
@Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT)
public final class ContextualPlayerLayer {
    private static ShaderInstance composite;
    private static TextureTarget layer;
    private static MultiBufferSource.BufferSource isolatedBuffers;
    private static LevelRenderer worldPass;
    private static float pendingOpacity;
    private static boolean capturing;
    private static boolean oculusWorld, oculusCapture;
    private static PostChain fabulousChain;
    private static AbstractUniform fabulousOpacity;
    private static float postedOpacity;
    private static final String VANILLA_CHAIN = "minecraft:shaders/post/transparency.json";
    public static long captureNanos, compositeNanos, composites;
    public static long oculusComposites, oculusCaptureBinds;

    private ContextualPlayerLayer() {}

    @Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Shaders {
        @SubscribeEvent
        public static void register(RegisterShadersEvent event) throws IOException {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    new ResourceLocation("towerofentropy", "player_fade"), DefaultVertexFormat.POSITION_TEX),
                    shader -> composite = shader);
        }
    }

    /** Only RenderStateShard output routing uses this predicate. */
    public static boolean useWorldTransparencyTargets() {
        return !capturing && !oculusWorld && Minecraft.useShaderTransparency();
    }

    /** Called only by the active Oculus shader-world pipeline. */
    public static void beginOculusWorld() {
        oculusWorld = true;
        pendingOpacity = 0;
    }

    public static boolean hasOculusComposite() {
        return oculusWorld && pendingOpacity > 0;
    }

    public static void endOculusWorld() {
        oculusWorld = false;
        worldPass = null;
        pendingOpacity = 0;
    }

    public static boolean bindOculusCapture() {
        if (!oculusCapture) return false;
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, layer.frameBufferId);
        oculusCaptureBinds++;
        return true;
    }

    public static void restoreOculusShaderTarget(RenderTarget target, boolean viewport) {
        if (!oculusCapture) target.bindWrite(viewport);
    }

    public static void composeOculus(int framebuffer) {
        int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, framebuffer);
        try {
            drawComposite(pendingOpacity);
            oculusComposites++;
        } finally {
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
        }
    }

    public static PostPass addPostPass(PostChain chain, String name, RenderTarget input, RenderTarget output)
            throws IOException {
        if (!VANILLA_CHAIN.equals(chain.getName()) || !name.equals("transparency"))
            return chain.addPass(name, input, output);
        var pass = chain.addPass("towerofentropy:fabulous_player", input, output);
        pass.addAuxAsset("PlayerColor", () -> layer == null ? -1 : layer.getColorTextureId(), 0, 0);
        pass.addAuxAsset("PlayerDepth", () -> layer == null ? -1 : layer.getDepthTextureId(), 0, 0);
        fabulousChain = chain;
        fabulousOpacity = pass.getEffect().safeGetUniform("PlayerOpacity");
        postedOpacity = 0;
        return pass;
    }

    public static void closePostChain(PostChain chain) {
        if (chain == fabulousChain) {
            fabulousChain = null;
            fabulousOpacity = null;
        }
    }

    public static <T extends Entity> void render(EntityRenderer<T> renderer, T entity, float yaw, float partial,
            PoseStack pose, MultiBufferSource buffers, int light) {
        var mc = Minecraft.getInstance();
        float opacity = entity == mc.player && worldPass == mc.levelRenderer ? ShoulderCamera.characterOpacity(mc) : 1;
        boolean iris = oculusWorld;
        if (opacity >= 1 || (!iris && Minecraft.useShaderTransparency() ? fabulousOpacity == null : composite == null)) {
            renderer.render(entity, yaw, partial, pose, buffers, light);
            return;
        }

        var scene = mc.getMainRenderTarget();
        int draw = iris ? GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING) : 0;
        int read = iris ? GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING) : 0;
        if (layer == null) {
            layer = new TextureTarget(scene.width, scene.height, true, Minecraft.ON_OSX);
            layer.setClearColor(0, 0, 0, 0);
            if (isolatedBuffers == null) isolatedBuffers = new RenderBuffers().bufferSource();
        } else if (layer.width != scene.width || layer.height != scene.height) {
            layer.resize(scene.width, scene.height, Minecraft.ON_OSX);
        }
        long started = System.nanoTime();
        RenderSystem.depthMask(true);
        if (iris) {
            // Keep Iris in its main-world shader context while redirecting only the GL draw target.
            scene.bindWrite(false);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, layer.frameBufferId);
            RenderSystem.viewport(0, 0, layer.viewWidth, layer.viewHeight);
            RenderSystem.clearColor(0, 0, 0, 0);
            RenderSystem.clearDepth(1);
            RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        } else {
            layer.clear(Minecraft.ON_OSX);
            layer.bindWrite(true);
        }
        capturing = true;
        oculusCapture = iris;
        try {
            renderer.render(entity, yaw, partial, pose, isolatedBuffers, light);
            pendingOpacity = opacity;
        } finally {
            try {
                isolatedBuffers.endBatch();
            } finally {
                capturing = false;
                oculusCapture = false;
                if (iris) {
                    GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
                    GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
                } else {
                    scene.bindWrite(true);
                }
            }
        }
        captureNanos += System.nanoTime() - started;
    }

    @SubscribeEvent
    public static void stage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            worldPass = event.getLevelRenderer();
            pendingOpacity = 0;
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            worldPass = null;
            if (oculusWorld) return;
            if (Minecraft.useShaderTransparency()) {
                if (fabulousOpacity != null && pendingOpacity != postedOpacity) {
                    fabulousOpacity.set(pendingOpacity);
                    postedOpacity = pendingOpacity;
                }
                if (pendingOpacity > 0) composites++;
            } else if (pendingOpacity > 0) drawComposite(pendingOpacity);
            pendingOpacity = 0;
        }
    }

    private static void drawComposite(float opacity) {
        long started = System.nanoTime();
        var previousShader = RenderSystem.getShader();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        try {
            composite.setSampler("PlayerColor", layer.getColorTextureId());
            composite.setSampler("PlayerDepth", layer.getDepthTextureId());
            composite.safeGetUniform("Opacity").set(opacity);
            RenderSystem.setShader(() -> composite);
            var quad = Tesselator.getInstance().getBuilder();
            quad.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
            quad.vertex(-1, -1, 0).uv(0, 0).endVertex();
            quad.vertex(3, -1, 0).uv(2, 0).endVertex();
            quad.vertex(-1, 3, 0).uv(0, 2).endVertex();
            BufferUploader.drawWithShader(quad.end());
        } finally {
            RenderSystem.setShader(() -> previousShader);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
        }
        composites++;
        compositeNanos += System.nanoTime() - started;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        endOculusWorld();
        worldPass = null;
        pendingOpacity = 0;
        if (fabulousOpacity != null) fabulousOpacity.set(0f);
        postedOpacity = 0;
        if (layer != null) {
            layer.destroyBuffers();
            layer = null;
        }
    }
}
