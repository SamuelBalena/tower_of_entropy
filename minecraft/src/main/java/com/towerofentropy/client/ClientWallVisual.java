package com.towerofentropy.client;

import com.towerofentropy.animation.ActionNetwork;
import com.towerofentropy.movement.ParkourPose;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT)
public final class ClientWallVisual {
    public enum Profile {
        ORIGINAL("wall_slide", "ledge_climb"),
        EXPERIMENTAL("wall_slide_push", "ledge_climb_ual2"),
        MISTO("wall_slide", "ledge_climb_ual2");

        private final String slide, climb;

        Profile(String slide, String climb) {
            this.slide = slide;
            this.climb = climb;
        }
    }

    private static Profile profile = Profile.ORIGINAL;

    static void profile(Profile selected) {
        profile = selected;
    }
    private static ParkourPose pose = ParkourPose.NONE;
    private static float facing;
    private static int started;
    private static boolean held;

    private ClientWallVisual() {}

    public static void accept(ParkourPose next, float yaw) {
        var player = Minecraft.getInstance().player;
        if (next != pose) {
            if (next == ParkourPose.JUMP) VisualContext.wallJumpStarted();
            else if (next != ParkourPose.NONE) VisualContext.wallContactResumed();
        }
        pose = next;
        facing = yaw;
        started = player == null ? 0 : player.tickCount;
    }

    public static boolean active() { return pose == ParkourPose.SLIDE; }
    public static boolean jumping() { return pose == ParkourPose.JUMP; }
    static float yaw() { return facing; }
    static boolean owns(String clip) { return clip.startsWith("wall_") || clip.startsWith("ledge_"); }

    public static String clip() {
        return switch (pose) {
            case SLIDE -> profile.slide;
            case HANG -> "ledge_hang";
            case CLIMB -> profile.climb;
            default -> null;
        };
    }

    static float time(String clip, float duration, float partial) {
        float elapsed = Math.max(0, Minecraft.getInstance().player.tickCount - started + partial);
        if (clip.startsWith("ledge_climb")) return duration * Mth.clamp(elapsed / 18, 0, 1);
        return elapsed / 20 % duration;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void input(MovementInputUpdateEvent event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.player != event.getEntity()) return;
        boolean jump = mc.options.keyJump.isDown();
        var input = event.getInput();
        if (jump && !held && (pose == ParkourPose.HANG || pose == ParkourPose.SLIDE)
                && !ClientRollMotion.busy() && !ClientAttackMotion.active()
                && !ClientAttackJumpGate.waitingRelease() && mc.screen == null && mc.getConnection() != null
                && ActionNetwork.ready(mc.getConnection().getConnection())) {
            ActionNetwork.requestParkour(input.leftImpulse, input.forwardImpulse);
        }
        held = jump;
        if (pose == ParkourPose.HANG || pose == ParkourPose.CLIMB) {
            input.jumping = false;
            input.forwardImpulse = 0;
            input.leftImpulse = 0;
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        pose = ParkourPose.NONE;
        profile = Profile.ORIGINAL;
        held = false;
    }
}
