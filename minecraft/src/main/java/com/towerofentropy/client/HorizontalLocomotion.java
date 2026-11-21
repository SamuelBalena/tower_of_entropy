package com.towerofentropy.client;

import com.towerofentropy.animation.HorizontalMotion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT)
public final class HorizontalLocomotion {
    public static final HorizontalMotion MOTION = new HorizontalMotion();
    private static LocalPlayer owner;
    private static String gait = "idle";
    private static float previousYaw, yaw;
    private static boolean hasHeading, wasAirborne, swimMoving;
    private static int keepHeadingUntil;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var player = Minecraft.getInstance().player;
        if (player != owner) {
            owner = player;
            MOTION.reset();
            gait = "idle";
            hasHeading = false;
            wasAirborne = false;
            swimMoving = false;
            keepHeadingUntil = 0;
        }
        if (player == null) return;
        double dx = player.getX() - player.xo;
        double dz = player.getZ() - player.zo;
        MOTION.update(dx, dz, player.getYRot(), player.isSprinting());
        // The vanilla camera-relative controls provide the displacement in all eight directions.
        // Normal ground movement is around 0.216 blocks/tick on a standard surface.
        double speed = MOTION.speed();
        boolean idle = MOTION.state() == HorizontalMotion.State.IDLE;
        gait = idle ? "idle" : player.isSprinting() && speed > .14 ? "sprint"
                : speed < (gait.equals("walk") ? .235 : .217) ? "walk" : "jog";
        boolean aquatic = player.isInWater();
        double dy = player.getY() - player.yo;
        double threshold = swimMoving ? .010 : .018;
        boolean verticalStroke = player.input != null && (player.input.jumping || player.input.shiftKeyDown)
                && Math.abs(dy) > threshold;
        boolean swimInput = player.input != null
                && (Math.abs(player.input.leftImpulse) > .001f || Math.abs(player.input.forwardImpulse) > .001f);
        // Gravity and buoyancy alone do not trigger strokes on a resting swimmer.
        swimMoving = aquatic && (swimInput || verticalStroke || dx * dx + dz * dz > threshold * threshold);
        boolean airborne = !player.onGround();
        if (wasAirborne && !airborne) keepHeadingUntil = player.tickCount + 4;
        wasAirborne = airborne;
        boolean jumpHeading = airborne || player.tickCount < keepHeadingUntil;
        previousYaw = hasHeading ? yaw : player.yBodyRot;
        double displacement = Math.hypot(dx, dz);
        boolean moving = aquatic ? displacement >= .008 && displacement <= 1 : jumpHeading ? displacement >= .025 && displacement <= 1 : !idle;
        float target = moving ? (float)Math.toDegrees(Math.atan2(-dx, dz))
                : aquatic || jumpHeading ? previousYaw : player.yBodyRot;
        float change = .42f * Mth.wrapDegrees(target - previousYaw);
        yaw = previousYaw + (jumpHeading ? Mth.clamp(change, -35, 35) : change);
        hasHeading = true;
    }

    static boolean swimMoving() { return swimMoving; }

    static String gait() {
        return gait;
    }

    static float travelYaw(float partial, float fallback) {
        return hasHeading ? Mth.rotLerp(partial, previousYaw, yaw) : fallback;
    }

    public static boolean loop(String clip) {
        return clip.equals("idle") || clip.equals("walk") || clip.equals("jog")
                || clip.equals("sprint") || VisualContext.loop(clip);
    }

    public static double referenceSpeed(String clip) {
        return switch (clip) { case "walk" -> .105; case "sprint" -> .281; default -> .216; };
    }
}
