package com.towerofentropy.client.camera;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CameraInput {
    private static KeyMapping shoulder, leftShoulder, rightShoulder, closer, farther;

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        shoulder = key("shoulder", GLFW.GLFW_KEY_UNKNOWN);
        leftShoulder = key("shoulder_left", GLFW.GLFW_KEY_LEFT);
        rightShoulder = key("shoulder_right", GLFW.GLFW_KEY_RIGHT);
        closer = key("closer", GLFW.GLFW_KEY_UP);
        farther = key("farther", GLFW.GLFW_KEY_DOWN);
        event.register(shoulder);
        event.register(leftShoulder);
        event.register(rightShoulder);
        event.register(closer);
        event.register(farther);
    }

    private static KeyMapping key(String action, int code) {
        return new KeyMapping("key.towerofentropy.camera." + action, KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM, code, "key.categories.towerofentropy");
    }

    private static boolean migrateBinding(KeyMapping mapping, int legacy) {
        if (mapping.getKeyModifier() != KeyModifier.NONE || mapping.getKey().getValue() != legacy
                || mapping.getKey().getType() != InputConstants.Type.KEYSYM) return false;
        mapping.setKey(mapping.getDefaultKey());
        return true;
    }

    private static void migrateBindings(Minecraft mc) {
        if (!CameraSettings.SPEC.isLoaded()) return;
        boolean zoom = CameraSettings.ZOOM_BINDING_VERSION.get() == 0;
        boolean side = CameraSettings.SHOULDER_BINDING_VERSION.get() == 0;
        boolean modified = false;
        if (zoom) {
            modified = migrateBinding(closer, GLFW.GLFW_KEY_LEFT_BRACKET);
            modified |= migrateBinding(farther, GLFW.GLFW_KEY_RIGHT_BRACKET);
        }
        if (side) modified |= migrateBinding(shoulder, GLFW.GLFW_KEY_B);
        if (modified) {
            KeyMapping.resetMapping();
            mc.options.save();
        }
        if (zoom) {
            CameraSettings.ZOOM_BINDING_VERSION.set(1);
            CameraSettings.ZOOM_BINDING_VERSION.save();
        }
        if (side) {
            CameraSettings.SHOULDER_BINDING_VERSION.set(1);
            CameraSettings.SHOULDER_BINDING_VERSION.save();
        }
    }

    private static final class ZoomKey {
        private int heldTicks;
        int steps(KeyMapping mapping, boolean active) {
            int taps = 0;
            while (mapping.consumeClick()) taps++;
            if (!active) {
                heldTicks = 0;
                return 0;
            }
            if (!mapping.isDown()) {
                heldTicks = 0;
                return taps;
            }
            // Ignore operating-system key repeat and use a fixed delay and cadence.
            int held = ++heldTicks;
            return held == 1 || held >= 9 && (held - 9) % 4 == 0 ? 1 : 0;
        }
    }

    @Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT)
    public static final class InGame {
        private static final ZoomKey IN = new ZoomKey(), OUT = new ZoomKey();
        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END || shoulder == null) return;
            var mc = Minecraft.getInstance();
            migrateBindings(mc);
            boolean active = ShoulderCamera.active(mc);
            if (!active) ShoulderCamera.reset();
            boolean controls = active && mc.screen == null && mc.getOverlay() == null;
            boolean currentSide = CameraSettings.RIGHT_SHOULDER.get();
            boolean nextSide = currentSide;
            while (shoulder.consumeClick()) nextSide = !nextSide;
            boolean selectLeft = false, selectRight = false;
            while (leftShoulder.consumeClick()) selectLeft = true;
            while (rightShoulder.consumeClick()) selectRight = true;
            if (selectLeft != selectRight) nextSide = selectRight;
            if (controls && nextSide != currentSide) {
                CameraSettings.RIGHT_SHOULDER.set(nextSide);
                CameraSettings.RIGHT_SHOULDER.save();
            }
            int steps = OUT.steps(farther, controls) - IN.steps(closer, controls);
            if (steps != 0) zoom(steps * .5);
        }

        @SubscribeEvent
        public static void scroll(InputEvent.MouseScrollingEvent event) {
            if (handleScroll(Minecraft.getInstance(), Screen.hasAltDown(), event.getScrollDelta()))
                event.setCanceled(true);
        }

        private static boolean handleScroll(Minecraft mc, boolean alt, double delta) {
            if (!alt || delta == 0 || !ShoulderCamera.active(mc) || mc.screen != null || mc.getOverlay() != null)
                return false;
            zoom(-Math.signum(delta) * .5);
            return true;
        }

        private static void zoom(double delta) {
            double old = CameraSettings.DISTANCE.get();
            double next = Mth.clamp(old + delta, CameraSettings.MIN_DISTANCE, CameraSettings.MAX_DISTANCE);
            if (next != old) {
                CameraSettings.DISTANCE.set(next);
                CameraSettings.DISTANCE.save();
            }
        }
    }
}
