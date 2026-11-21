package com.towerofentropy.client.camera;

import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT)
public final class ShoulderCamera {
    private static final double AIM_RANGE = 96;
    private static final double RESPONSE_SECONDS = 0.085;
    static final double COMBAT_APPROACH = .40;
    private static final double COLLISION_RADIUS = 0.10;
    private static final double COLLISION_SKIN = 0.02;
    private static final CameraSweep SWEEP = new CameraSweep();
    private static final Shapes.DoubleLineConsumer INCLUDE_BOX = SWEEP::include;
    private static long previousFrame;
    private static ClientLevel level;
    private static Vec3 followedEye;
    private static double aimParallax, usableFraction;
    private static double opacity = 1;
    private static double distance, shoulder, height;

    private ShoulderCamera() {}

    public static boolean active(Minecraft mc) {
        return mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK
                && mc.player != null && mc.level != null && mc.getCameraEntity() == mc.player
                && !mc.player.isSpectator() && !mc.player.isSleeping();
    }

    @SubscribeEvent
    public static void stableFov(ViewportEvent.ComputeFov event) {
        var mc = Minecraft.getInstance();
        if (event.usedConfiguredFov() && active(mc) && mc.player.isAlive() && !mc.player.isScoping()
                && !mc.player.isInWaterOrBubble() && !mc.player.isInLava())
            event.setFOV(mc.options.fov().get());
    }

    public static float characterOpacity(Minecraft mc) {
        return level == mc.level && active(mc) ? (float) opacity : 1;
    }

    static void reset() {
        opacity = 1;
        previousFrame = 0;
        level = null;
        followedEye = null;
    }

    public static boolean apply(Camera camera, float partialTick) {
        var mc = Minecraft.getInstance();
        if (!active(mc) || camera.getEntity() != mc.player || !(camera instanceof CameraPoseAccess access)) return false;
        var profiler = mc.getProfiler();
        profiler.push("toeShoulderCamera");
        try {
            applyPose(mc, camera, access, partialTick);
        } finally {
            profiler.pop();
        }
        return true;
    }

    private static void applyPose(Minecraft mc, Camera camera, CameraPoseAccess access, float partialTick) {
        long now = System.nanoTime();
        double elapsed = (now - previousFrame) * 1e-9;
        Vec3 eye = camera.getPosition();
        boolean fresh = previousFrame == 0 || level != mc.level || elapsed > .5
                || followedEye.distanceToSqr(eye) > 4;
        previousFrame = now;
        level = mc.level;
        updateOffsets(mc, elapsed, fresh);
        followedEye = fresh ? eye : new Vec3(
                CameraDamping.follow(followedEye.x, eye.x, elapsed, .052, .13),
                CameraDamping.follow(followedEye.y, eye.y, elapsed, .075, .23),
                CameraDamping.follow(followedEye.z, eye.z, elapsed, .052, .13));

        Vec3 forward = vector(camera.getLookVector());
        Vec3 left = vector(camera.getLeftVector());
        Vec3 up = vector(camera.getUpVector());
        Vec3 lag = followedEye.subtract(eye);
        Vec3 offset = lag.subtract(forward.scale(lag.dot(forward))).add(forward.scale(-distance))
                .add(left.scale(-shoulder)).add(up.scale(height));
        double unobstructed = collisionFraction(mc, eye, offset);
        usableFraction = fresh || unobstructed <= usableFraction ? unobstructed
                : CameraDamping.approach(usableFraction, unobstructed, elapsed, .09);
        Vec3 position = eye.add(offset.scale(usableFraction));
        // Projected body size depends on proximity and FOV. No fade during manual zoom by itself.
        double clearance = position.distanceTo(eye) * Math.tan(Math.toRadians(mc.options.fov().get() * .5))
                / Math.tan(Math.toRadians(35));
        double target = usableFraction < .995 ? CharacterOpacity.target(clearance, CameraSettings.FADE_COMFORTABLE.get(),
                CameraSettings.FADE_CLOSE.get(), CameraSettings.FADE_MINIMUM.get()) : 1;
        opacity = CharacterOpacity.update(fresh ? 1 : opacity, target, fresh ? 1.0 / 60 : elapsed);

        // Smooth only the reciprocal focus distance. The mouse direction is sampled raw every frame.
        Vec3 aimEye = mc.player.getEyePosition(partialTick);
        double depth = Mth.clamp(aimPoint(mc, partialTick).subtract(aimEye).dot(forward), .35, AIM_RANGE);
        aimParallax = fresh ? 1 / depth : CameraDamping.approach(aimParallax, 1 / depth, elapsed, .10);
        Vec3 aim = aimEye.add(forward.scale(1 / aimParallax)).subtract(position);
        float yaw = (float) Math.toDegrees(Math.atan2(-aim.x, aim.z));
        float pitch = (float) Math.toDegrees(-Math.atan2(aim.y, aim.horizontalDistance()));
        access.toeSetPose(position, yaw, pitch);
    }

    private static void updateOffsets(Minecraft mc, double elapsed, boolean fresh) {
        double manualDistance = CameraSettings.DISTANCE.get();
        double targetDistance = Math.max(CameraSettings.MIN_DISTANCE,
                manualDistance - (com.towerofentropy.client.ClientWeaponState.inCombat() ? COMBAT_APPROACH : 0));
        double aspect = mc.getWindow().getWidth() / (double) Math.max(1, mc.getWindow().getHeight());
        double visibleHalfWidth = manualDistance * Math.tan(Math.toRadians(mc.options.fov().get() * .5)) * aspect;
        double targetShoulder = Math.min(CameraSettings.SHOULDER_OFFSET.get(), visibleHalfWidth * .65)
                * (CameraSettings.RIGHT_SHOULDER.get() ? 1 : -1);
        double targetHeight = CameraSettings.HEIGHT_OFFSET.get();
        distance = fresh ? targetDistance : CameraDamping.approach(distance, targetDistance, elapsed, RESPONSE_SECONDS);
        shoulder = fresh ? targetShoulder : CameraDamping.approach(shoulder, targetShoulder, elapsed, RESPONSE_SECONDS);
        height = fresh ? targetHeight : CameraDamping.approach(height, targetHeight, elapsed, RESPONSE_SECONDS);
    }

    static double collisionFraction(Minecraft mc, Vec3 pivot, Vec3 offset) {
        double length = offset.length();
        if (length == 0) return 1;
        Vec3 endpoint = pivot.add(offset);
        var bounds = new AABB(pivot, endpoint).inflate(COLLISION_RADIUS);
        SWEEP.reset(pivot.x, pivot.y, pivot.z, offset.x, offset.y, offset.z,
                COLLISION_RADIUS, COLLISION_SKIN / length);
        for (var shape : mc.level.getBlockCollisions(mc.player, bounds)) {
            shape.forAllBoxes(INCLUDE_BOX);
            if (SWEEP.fraction() == 0) break;
        }
        return SWEEP.fraction();
    }

    private static Vec3 aimPoint(Minecraft mc, float partialTick) {
        if (mc.hitResult != null && mc.hitResult.getType() != HitResult.Type.MISS)
            return mc.hitResult.getLocation();

        var player = mc.player;
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 block = player.pick(AIM_RANGE, partialTick, false).getLocation();
        var entity = ProjectileUtil.getEntityHitResult(player, eye, block,
                player.getBoundingBox().expandTowards(block.subtract(eye)).inflate(1),
                candidate -> candidate.isPickable() && !candidate.isSpectator(), eye.distanceToSqr(block));
        return entity == null ? block : entity.getLocation();
    }

    private static Vec3 vector(org.joml.Vector3f vector) {
        return new Vec3(vector.x(), vector.y(), vector.z());
    }
}
