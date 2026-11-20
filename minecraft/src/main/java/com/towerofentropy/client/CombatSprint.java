package com.towerofentropy.client;

import com.mojang.logging.LogUtils;
import com.towerofentropy.animation.ActionNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Routes the configured physical Sprint press before the vanilla movement tick. */
@Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT)
public final class CombatSprint {
    private static boolean physicalDown, held, rawEdge, pressedInCombat;
    private static int awaitingReply;

    private CombatSprint() {}

    @SubscribeEvent
    public static void keyboard(InputEvent.Key event) {
        var binding = Minecraft.getInstance().options.keySprint;
        if (binding.matches(event.getKey(), event.getScanCode())) edge(binding, event.getAction());
    }

    @SubscribeEvent
    public static void mouse(InputEvent.MouseButton.Post event) {
        var binding = Minecraft.getInstance().options.keySprint;
        if (binding.matchesMouse(event.getButton())) edge(binding, event.getAction());
    }

    private static void edge(KeyMapping binding, int action) {
        if (action == GLFW.GLFW_RELEASE) {
            physicalDown = false;
        } else if (action == GLFW.GLFW_PRESS && binding.isConflictContextAndModifierActive()) {
            rawEdge = true;
            physicalDown = true;
            var mc = Minecraft.getInstance();
            pressedInCombat |= mc.screen == null && mc.getOverlay() == null && ClientWeaponState.inCombat();
        }
    }

    public static boolean down(KeyMapping vanilla) {
        if (!ClientWeaponState.inCombat()) return vanilla.isDown();
        return held && awaitingReply == 0 && !ClientRollMotion.busy();
    }

    static void answered() { awaitingReply = 0; }

    @SubscribeEvent
    public static void awaitRoll(MovementInputUpdateEvent event) {
        var p = Minecraft.getInstance().player;
        if (awaitingReply == 0 || p == null || event.getEntity() != p || p.hurtTime > 0) return;
        // The server anchors the Roll when it receives the request. Coasting before its
        // reply would spend part of the Roll envelope on ordinary walking.
        var input = event.getInput();
        input.forwardImpulse = input.leftImpulse = 0;
        input.up = input.down = input.left = input.right = false;
        p.setSprinting(false);
        var velocity = p.getDeltaMovement();
        p.setDeltaMovement(0, velocity.y, 0);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (awaitingReply > 0) awaitingReply--;
        var mc = Minecraft.getInstance();
        var sprint = mc.options.keySprint;
        boolean down = sprint.isConflictContextAndModifierActive()
                && (mc.options.toggleSprint().get() ? physicalDown : physicalDown || sprint.isDown());
        boolean combat = ClientWeaponState.inCombat();
        boolean pressed = pressedInCombat || (!rawEdge && down && !held && combat);
        if (combat && !down && held && mc.player != null) mc.player.setSprinting(false);
        held = down;
        rawEdge = false;
        pressedInCombat = false;
        if (!pressed || !combat || mc.screen != null || mc.getOverlay() != null || mc.getConnection() == null
                || !ActionNetwork.ready(mc.getConnection().getConnection())) return;
        if (Boolean.getBoolean("toe.combat.roll.trace"))
            LogUtils.getLogger().info("TOE_W0021_SPRINT key={} ground={} rollBusy={} attack={}",
                    sprint.getKey(), mc.player.onGround(), ClientRollMotion.busy(), ClientCombatState.clip());
        if (!ClientRollMotion.busy() && mc.player.onGround() && ClientCombatState.clip().isEmpty()) {
            awaitingReply = 4;
            var options = mc.options;
            ActionNetwork.requestCombatRoll((options.keyLeft.isDown() ? 1 : 0) - (options.keyRight.isDown() ? 1 : 0),
                    (options.keyUp.isDown() ? 1 : 0) - (options.keyDown.isDown() ? 1 : 0));
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        physicalDown = held = rawEdge = pressedInCombat = false;
        answered();
    }
}
