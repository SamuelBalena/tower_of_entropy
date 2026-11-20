package com.towerofentropy.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/** Bounds voluntary travel during an airborne heavy without sending a position correction. */
public final class HeavyAirLock {
    public static final double AIR_DRAG = .46;
    private static final double MAX_RADIUS = .85;
    private record Origin(double x, double z, long tick) {}
    private static final Map<UUID, Origin> ACTIVE = new HashMap<>();

    private HeavyAirLock() {}

    static void begin(ServerPlayer p) {
        ACTIVE.put(p.getUUID(), new Origin(p.getX(), p.getZ(), p.level().getGameTime()));
    }

    static void end(ServerPlayer p) { ACTIVE.remove(p.getUUID()); }

    public static boolean active(ServerPlayer p) {
        return ACTIVE.containsKey(p.getUUID());
    }

    public static double x(ServerboundMovePlayerPacket packet, double current, ServerGamePacketListenerImpl listener) {
        return coordinate(packet, current, listener.player, true);
    }

    public static double z(ServerboundMovePlayerPacket packet, double current, ServerGamePacketListenerImpl listener) {
        return coordinate(packet, current, listener.player, false);
    }

    private static double coordinate(ServerboundMovePlayerPacket packet, double current, ServerPlayer player, boolean x) {
        double requested = x ? packet.getX(current) : packet.getZ(current);
        var origin = ACTIVE.get(player.getUUID());
        if (origin == null || player.hurtTime > 0) return requested;
        long age = player.level().getGameTime() - origin.tick;
        if (age >= CombatMove.HEAVY_ATTACK.durationTicks) return requested;

        double desiredX = packet.getX(player.getX()) - origin.x;
        double desiredZ = packet.getZ(player.getZ()) - origin.z;
        double desiredRadius = Math.hypot(desiredX, desiredZ);
        double currentRadius = Math.hypot(player.getX() - origin.x, player.getZ() - origin.z);
        double radius = Math.max(currentRadius, Math.min(MAX_RADIUS, .40 + .25 * age));
        if (desiredRadius <= radius) return requested;
        return (x ? origin.x : origin.z) + (x ? desiredX : desiredZ) * radius / desiredRadius;
    }
}
