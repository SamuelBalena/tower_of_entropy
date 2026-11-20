package com.towerofentropy.movement;

import com.towerofentropy.animation.ActionNetwork;
import com.towerofentropy.combat.AttackMovement;
import com.towerofentropy.combat.HeavyAirLock;
import com.towerofentropy.combat.RollController;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "towerofentropy")
public final class WallSlide {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue ENABLED;
    private static final ForgeConfigSpec.IntValue VIGOR_TICKS;
    private static final ForgeConfigSpec.DoubleValue FALL_SPEED;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    static {
        var builder = new ForgeConfigSpec.Builder();
        ENABLED = builder.define("enabled", true);
        VIGOR_TICKS = builder.comment("Ticks of adhesion per airborne period").defineInRange("vigorTicks", 35, 1, 200);
        FALL_SPEED = builder.comment("Maximum falling speed in blocks per tick during adhesion")
                .defineInRange("fallSpeed", .065, .025, .25);
        SPEC = builder.build();
    }

    static boolean engaged(ServerPlayer player) {
        var session = SESSIONS.get(player.getUUID());
        return session != null && session.pose != ParkourPose.NONE;
    }

    public static boolean holdsPlayer(ServerPlayer player) {
        var session = SESSIONS.get(player.getUUID());
        return session != null && (session.pose == ParkourPose.SLIDE || session.pose == ParkourPose.HANG
                || session.pose == ParkourPose.CLIMB);
    }

    private static final int WALL_JUMP_COST = 8;
    private static final int LEAVE_COOLDOWN = 12;
    record Contact(Direction side, BlockPos block) {}

    private static final class Session {
        int remaining = VIGOR_TICKS.get();
        long started, resumeAt;
        double lastY = Double.NaN, jumpY;
        boolean jumped;
        ParkourPose pose = ParkourPose.NONE;
        Ledge ledge;
        Direction side = Direction.NORTH;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (!player.isAlive() || !ENABLED.get()) {
            clear(player);
            return;
        }
        var session = SESSIONS.computeIfAbsent(player.getUUID(), id -> new Session());
        long now = player.level().getGameTime();
        if (player.onGround() && session.ledge == null) {
            pose(player, session, ParkourPose.NONE, now);
            session.lastY = player.getY();
            session.remaining = VIGOR_TICKS.get();
            session.jumped = false;
            session.resumeAt = 0;
            return;
        }
        boolean rising = player.getY() > session.lastY + .001;
        session.lastY = player.getY();
        if (!eligible(player)) {
            release(player, session, now);
            return;
        }
        if (session.ledge != null) {
            tickLedge(player, session, now);
            return;
        }
        Contact contact = session.remaining > 0 ? contact(player) : null;
        boolean jumping = session.pose == ParkourPose.JUMP && now < session.resumeAt;
        boolean canCatch = jumping ? player.getY() > session.jumpY + .48 : now >= session.resumeAt;
        if (contact != null && canCatch && !player.isShiftKeyDown()) {
            var ledge = Ledge.find(player, contact);
            if (ledge != null) {
                session.side = contact.side;
                session.ledge = ledge;
                pose(player, session, ParkourPose.HANG, now);
                tickLedge(player, session, now);
                return;
            }
        }
        if (jumping) return;
        if (rising && session.pose != ParkourPose.SLIDE) {
            pose(player, session, ParkourPose.NONE, now);
            return;
        }
        if (contact != null && now >= session.resumeAt) {
            session.remaining--;
            if (session.pose == ParkourPose.SLIDE && session.side != contact.side)
                ActionNetwork.parkourPose(player, ParkourPose.SLIDE, contact.side.toYRot());
            session.side = contact.side;
            var velocity = player.getDeltaMovement();
            push(player, new Vec3(velocity.x, -FALL_SPEED.get(), velocity.z));
            pose(player, session, ParkourPose.SLIDE, now);
        } else {
            pose(player, session, ParkourPose.NONE, now);
        }
    }

    public static void jump(ServerPlayer player, float strafe, float forward) {
        var session = SESSIONS.get(player.getUUID());
        if (session == null || player.onGround() || !eligible(player) || !ENABLED.get()
                || !Float.isFinite(strafe) || !Float.isFinite(forward)
                || Math.abs(strafe) > 1 || Math.abs(forward) > 1) return;
        long now = player.level().getGameTime();
        if (session.pose == ParkourPose.HANG && session.remaining > 0 && !player.isShiftKeyDown() && session.ledge.clearRoute(player)) {
            pose(player, session, ParkourPose.CLIMB, now);
            return;
        }
        if (session.pose != ParkourPose.SLIDE || session.jumped || session.remaining < WALL_JUMP_COST) return;
        var contact = contact(player);
        if (contact == null) return;
        session.remaining -= WALL_JUMP_COST;
        session.jumped = true;
        session.jumpY = player.getY();
        session.resumeAt = now + LEAVE_COOLDOWN;
        session.side = contact.side;
        Vec3 away = new Vec3(-contact.side.getStepX(), 0, -contact.side.getStepZ());
        double yaw = Math.toRadians(player.getYRot());
        Vec3 intent = new Vec3(strafe * Math.cos(yaw) - forward * Math.sin(yaw), 0,
                strafe * Math.sin(yaw) + forward * Math.cos(yaw));
        double horizontal = intent.normalize().dot(away) >= .40 ? .36 : 0;
        push(player, new Vec3(away.x * horizontal, ParkourJump.VELOCITY, away.z * horizontal));
        pose(player, session, ParkourPose.JUMP, now);
    }

    private static void tickLedge(ServerPlayer player, Session s, long now) {
        var ledge = s.ledge;
        boolean hanging = s.pose == ParkourPose.HANG;
        if (!ledge.clearRoute(player) || player.isShiftKeyDown()
                || (hanging && s.remaining <= 0)) {
            release(player, s, now);
            return;
        }
        if (hanging) s.remaining--;
        int elapsed = (int) Math.min(Ledge.CLIMB_TICKS, now - s.started);
        Vec3 next = hanging ? ledge.hang() : ledge.step(elapsed);
        if (!ledge.clearStep(player, player.position(), next) || player.position().distanceTo(next) > .38) {
            release(player, s, now);
            return;
        }
        push(player, Vec3.ZERO);
        if (player.position().distanceToSqr(next) > .000004) {
            player.connection.teleport(next.x, next.y, next.z, player.getYRot(), player.getXRot());
        }
        if (!hanging && elapsed == Ledge.CLIMB_TICKS) release(player, s, now);
    }

    private static void push(ServerPlayer player, Vec3 velocity) {
        player.setDeltaMovement(velocity);
        player.hurtMarked = true;
    }

    private static void pose(ServerPlayer p, Session s, ParkourPose next, long now) {
        if (s.pose == next) return;
        s.pose = next;
        s.started = now;
        ActionNetwork.parkourPose(p, next, s.side.toYRot());
    }

    private static void release(ServerPlayer p, Session s, long now) {
        if (s.ledge != null) {
            s.ledge = null;
            s.resumeAt = now + LEAVE_COOLDOWN;
        }
        pose(p, s, ParkourPose.NONE, now);
    }

    private static boolean eligible(ServerPlayer p) {
        if (p.isSpectator() || p.getAbilities().flying || p.isFallFlying()
                || p.isPassenger() || p.isInWaterOrBubble() || p.isInLava() || p.onClimbable()
                || p.isUsingItem() || p.hurtTime > 0 || RollController.busy(p.getUUID())) return false;
        return !AttackMovement.committed(p.getUUID()) && !HeavyAirLock.active(p);
    }

    private static Contact contact(ServerPlayer player) {
        AABB body = player.getBoundingBox();
        double gap = .035;
        // Check only the vertical sides of the torso. Keep the probe clear of floor and ceiling.
        double bottom = body.minY + .25;
        double top = body.maxY - .10;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            AABB probe = switch (side) {
                case NORTH -> new AABB(body.minX + .01, bottom, body.minZ - gap, body.maxX - .01, top, body.minZ + .001);
                case SOUTH -> new AABB(body.minX + .01, bottom, body.maxZ - .001, body.maxX - .01, top, body.maxZ + gap);
                case WEST -> new AABB(body.minX - gap, bottom, body.minZ + .01, body.minX + .001, top, body.maxZ - .01);
                case EAST -> new AABB(body.maxX - .001, bottom, body.minZ + .01, body.maxX + gap, top, body.maxZ - .01);
                default -> throw new IllegalStateException("Expected horizontal side");
            };
            var level = player.level();
            BlockPos topBlock = null;
            for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(probe.minX), Mth.floor(probe.minY),
                    Mth.floor(probe.minZ), Mth.floor(probe.maxX - 1e-7), Mth.floor(probe.maxY - 1e-7),
                    Mth.floor(probe.maxZ - 1e-7))) {
                if ((topBlock == null || pos.getY() > topBlock.getY())
                        && level.getBlockState(pos).isCollisionShapeFullBlock(level, pos)) topBlock = pos.immutable();
            }
            if (topBlock != null) return new Contact(side, topBlock);
        }
        return null;
    }

    private static void clear(ServerPlayer player) {
        var session = SESSIONS.remove(player.getUUID());
        if (session != null) pose(player, session, ParkourPose.NONE, player.level().getGameTime());
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void changeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) clear(player);
    }
}
