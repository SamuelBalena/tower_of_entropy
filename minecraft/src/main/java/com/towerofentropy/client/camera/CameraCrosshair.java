package com.towerofentropy.client.camera;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT)
public final class CameraCrosshair {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("towerofentropy", "textures/gui/shoulder_crosshair.png");
    private static final int SIZE = 13;
    private static net.minecraft.client.multiplayer.ClientLevel level;
    private static long previousFrame;
    private static float tint;

    @SubscribeEvent
    public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        level = null;
        previousFrame = 0;
        tint = 0;
    }

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Pre event) {
        var mc = Minecraft.getInstance();
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id()) || !ShoulderCamera.active(mc)) return;
        event.setCanceled(true);
        if (mc.options.hideGui) return;
        long now = System.nanoTime();
        double elapsed = (now - previousFrame) * 1e-9;
        float target = com.towerofentropy.client.ClientWeaponState.inCombat() ? 1 : 0;
        tint = mc.level != level || previousFrame == 0 ? target
                : (float) CameraDamping.approach(tint, target, elapsed, .07);
        level = mc.level;
        previousFrame = now;
        var gui = event.getGuiGraphics();
        var pose = gui.pose();
        gui.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        pose.pushPose();
        try {
            pose.translate(gui.guiWidth() * .5f - SIZE * .5f, gui.guiHeight() * .5f - SIZE * .5f, 0);
            RenderSystem.setShaderColor(1 - .35f * tint, 1 - .85f * tint, 1 - .81f * tint, 1);
            gui.blit(TEXTURE, 0, 0, 0, 0, SIZE, SIZE, SIZE, SIZE);
            gui.flush();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            if (tint > .01f) gui.fill(SIZE / 2, SIZE / 2, SIZE / 2 + 1, SIZE / 2 + 1,
                    ((int)(tint * 220) << 24) | 0xB14949);
        } finally {
            RenderSystem.setShaderColor(1, 1, 1, 1);
            pose.popPose();
            RenderSystem.disableBlend();
        }
    }
}
