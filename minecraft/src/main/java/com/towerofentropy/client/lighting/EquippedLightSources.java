package com.towerofentropy.client.lighting;

import com.mojang.logging.LogUtils;
import com.towerofentropy.client.lighting.DynamicLightAttachments.Attachment;
import com.towerofentropy.client.lighting.DynamicLightAttachments.Emission;
import com.towerofentropy.client.lighting.DynamicLightAttachments.Socket;
import com.towerofentropy.combat.EffectiveEquipment;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class EquippedLightSources {
    record Source(ResourceLocation id, UUID owner, Socket socket, Vec3 position, Emission emission) {}
    private record Anchor(long frame, Vec3 localOffset, HumanoidArm arm) {}
    private record Transition(boolean posed, long lastSample, long start, Vec3 from, Vec3 output) {}

    private static final Emission TORCH = new Emission(new Vec3(1, .73, .44), 14f / 15, 6.7f, .027f);
    private static final Emission LANTERN = new Emission(new Vec3(1, .82, .58), 1, 7.5f, 0);
    private static final Emission SOUL_TORCH = new Emission(new Vec3(.34, .82, 1), 10f / 15, 5.3f, .022f);
    private static final Emission SOUL_LANTERN = new Emission(new Vec3(.40, .84, 1), 10f / 15, 5.8f, 0);
    private static final Emission REDSTONE = new Emission(new Vec3(1, .25, .16), 7f / 15, 3.7f, .012f);
    private static final EnumMap<Socket, Anchor> ANCHORS = new EnumMap<>(Socket.class);
    private static final EnumMap<Socket, Transition> TRANSITIONS = new EnumMap<>(Socket.class);
    private static UUID anchorOwner;
    private static long frame;
    private static boolean reportedProviderError;

    private EquippedLightSources() {}

    static void beginFrame() { frame++; }

    static long frame() { return frame; }

    static void reset() {
        anchorOwner = null;
        ANCHORS.clear();
        TRANSITIONS.clear();
    }

    private static Emission vanilla(ItemStack item) {
        if (item.is(Items.TORCH)) return TORCH;
        if (item.is(Items.LANTERN)) return LANTERN;
        if (item.is(Items.SOUL_TORCH)) return SOUL_TORCH;
        if (item.is(Items.SOUL_LANTERN)) return SOUL_LANTERN;
        if (item.is(Items.REDSTONE_TORCH)) return REDSTONE;
        return null;
    }

    public static boolean needsAttachments(AbstractClientPlayer player) {
        return vanilla(EffectiveEquipment.selected(player)) != null || vanilla(player.getOffhandItem()) != null
                || !DynamicLightAttachments.providers().isEmpty();
    }

    public static void captureHand(AbstractClientPlayer player, InteractionHand hand, HumanoidArm arm,
                                   Matrix4f inverseRoot, Matrix4f itemPose) {
        if (inverseRoot == null) return;
        var point = new Vector3f(0, .15f, 0);
        itemPose.transformPosition(point);
        inverseRoot.transformPosition(point);
        capture(player, hand == InteractionHand.MAIN_HAND ? Socket.MAIN_HAND : Socket.OFF_HAND, point, arm);
    }

    public static void captureBody(AbstractClientPlayer player, Matrix4f inverseRoot, Matrix4f bodyPose) {
        if (inverseRoot == null) return;
        for (var socket : Socket.values()) {
            if (socket == Socket.MAIN_HAND || socket == Socket.OFF_HAND) continue;
            var point = bodyAnchor(socket);
            bodyPose.transformPosition(point);
            inverseRoot.transformPosition(point);
            capture(player, socket, point, null);
        }
    }

    private static void capture(AbstractClientPlayer player, Socket socket, Vector3f point, HumanoidArm arm) {
        if (!player.getUUID().equals(anchorOwner)) {
            ANCHORS.clear();
            TRANSITIONS.clear();
            anchorOwner = player.getUUID();
        }
        ANCHORS.put(socket, new Anchor(frame, new Vec3(point.x, point.y, point.z), arm));
    }

    private static Vector3f bodyAnchor(Socket socket) {
        return switch (socket) {
            case BACK_LEFT -> new Vector3f(.25f, .26f, -.29f);
            case BACK_RIGHT -> new Vector3f(-.25f, .26f, -.29f);
            case BACK_CENTER -> new Vector3f(0, .29f, -.32f);
            case BELT_LEFT -> new Vector3f(.31f, .02f, -.04f);
            case BELT_RIGHT -> new Vector3f(-.31f, .02f, -.04f);
            default -> throw new IllegalArgumentException("Expected torso socket");
        };
    }

    /** Current world-space point for an attachment model or emitter. Call on the render thread. */
    public static Vec3 socketPosition(AbstractClientPlayer player, Socket socket, float partial) {
        return player.getPosition(partial).add(anchor(player, socket, partial));
    }

    static List<Source> collect(AbstractClientPlayer player, float partial) {
        if (!player.isAlive() || player.isSpectator() || !needsAttachments(player)) return List.of();
        var position = player.getPosition(partial);
        var results = new ArrayList<Source>(4);
        var sockets = EnumSet.noneOf(Socket.class);
        var ids = new HashSet<ResourceLocation>();
        add(results, sockets, ids, player, position, partial, new Attachment(
                new ResourceLocation("towerofentropy", "main_hand"), Socket.MAIN_HAND,
                EffectiveEquipment.selected(player), null, null, true));
        add(results, sockets, ids, player, position, partial, new Attachment(
                new ResourceLocation("towerofentropy", "off_hand"), Socket.OFF_HAND,
                player.getOffhandItem(), null, null, true));
        for (var provider : DynamicLightAttachments.providers()) {
            try {
                for (var light : provider.equippedLights(player, partial))
                    add(results, sockets, ids, player, position, partial, light);
            } catch (RuntimeException error) {
                if (!reportedProviderError) LogUtils.getLogger().warn("Dynamic light attachment provider failed", error);
                reportedProviderError = true;
            }
        }
        results.sort(Comparator.<Source>comparingDouble(light ->
                -light.emission.strength() / (1 + light.position.distanceToSqr(position) * .10))
                .thenComparing(light -> light.id.toString()));
        return results;
    }

    private static void add(List<Source> into, EnumSet<Socket> sockets, HashSet<ResourceLocation> ids,
                            AbstractClientPlayer player, Vec3 ownerPosition, float partial, Attachment attachment) {
        if (attachment == null || !attachment.active() || attachment.item().isEmpty()) return;
        var emission = attachment.emission() == null ? vanilla(attachment.item()) : attachment.emission();
        if (emission == null || emission.strength() == 0 || sockets.contains(attachment.socket()) || ids.contains(attachment.id())) return;
        var position = attachment.worldPosition();
        if (position == null) position = ownerPosition.add(blendedAnchor(player, attachment.socket(), partial));
        double distance = position.distanceToSqr(ownerPosition);
        if (!Double.isFinite(distance) || distance > 9) return;
        sockets.add(attachment.socket());
        ids.add(attachment.id());
        into.add(new Source(attachment.id(), player.getUUID(), attachment.socket(), position, emission));
    }

    private static Vec3 blendedAnchor(AbstractClientPlayer player, Socket socket, float partial) {
        var pose = trackedAnchor(player, socket);
        var target = pose == null ? fallbackAnchor(player, socket, partial) : pose.localOffset;
        var before = TRANSITIONS.get(socket);
        long now = System.nanoTime();
        long start = now;
        var from = target;
        if (before != null && now - before.lastSample < 150_000_000L) {
            if (before.posed == (pose != null)) {
                start = before.start;
                from = before.from;
            } else {
                from = before.output;
            }
        }
        var output = from.lerp(target, Math.min(1, (now - start) / 90_000_000.0));
        TRANSITIONS.put(socket, new Transition(pose != null, now, start, from, output));
        return output;
    }

    private static Vec3 anchor(AbstractClientPlayer player, Socket socket, float partial) {
        var pose = trackedAnchor(player, socket);
        return pose == null ? fallbackAnchor(player, socket, partial) : pose.localOffset;
    }

    private static Anchor trackedAnchor(AbstractClientPlayer player, Socket socket) {
        boolean hand = socket == Socket.MAIN_HAND || socket == Socket.OFF_HAND;
        var arm = socket == Socket.OFF_HAND ? player.getMainArm().getOpposite() : player.getMainArm();
        var captured = player.getUUID().equals(anchorOwner) ? ANCHORS.get(socket) : null;
        if (captured != null && frame - captured.frame <= 1 && (!hand || captured.arm == arm)
                && !Minecraft.getInstance().options.getCameraType().isFirstPerson()) return captured;
        return null;
    }

    private static Vec3 fallbackAnchor(AbstractClientPlayer player, Socket socket, float partial) {
        boolean hand = socket == Socket.MAIN_HAND || socket == Socket.OFF_HAND;
        var arm = socket == Socket.OFF_HAND ? player.getMainArm().getOpposite() : player.getMainArm();
        float yaw = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
        var local = hand ? fallbackHand(player, arm, partial) : bodyAnchor(socket);
        if (!hand) {
            boolean belt = socket == Socket.BELT_LEFT || socket == Socket.BELT_RIGHT;
            local.set(local.x, player.getBbHeight() * (belt ? .56f : .78f), -local.z);
        }
        var world = new Matrix4f().rotateY((180 - yaw) * Mth.DEG_TO_RAD).scale(-1, 1, 1).transformPosition(local);
        return new Vec3(world.x, world.y, world.z);
    }

    private static Vector3f fallbackHand(AbstractClientPlayer player, HumanoidArm arm, float partial) {
        float side = arm == HumanoidArm.RIGHT ? -1 : 1;
        float width = player.getModelName().equals("slim") ? .28f : .31f;
        float stride = Mth.cos(player.walkAnimation.position(partial) * .6662f) * player.walkAnimation.speed(partial) * side;
        float attack = player.swingingArm != null &&
                ((player.swingingArm == InteractionHand.MAIN_HAND) == (arm == player.getMainArm()))
                ? Mth.sin(player.getAttackAnim(partial) * Mth.PI) : 0;
        return new Vector3f(width * side, player.getBbHeight() * .70f + .12f * Math.abs(stride) + attack * .19f,
                -.23f + .23f * stride - attack * .28f);
    }
}
