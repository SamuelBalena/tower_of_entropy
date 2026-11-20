package com.towerofentropy.combat;

import java.util.Comparator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;

public final class SwordTargeting {
    public record Hit(int targetId, boolean damaged) { }
    public static boolean eligible(net.minecraft.world.entity.Entity entity) {
        return entity instanceof Mob mob && mob.isAlive() && mob.isAttackable() && !mob.isInvulnerable();
    }
    public static Hit impact(ServerPlayer player, CombatConfig.Settings settings) {
        var level = player.serverLevel();
        var origin = player.position();
        var eye = player.getEyePosition();
        var ray = player.getLookAngle().scale(settings.reach());
        var stop = level.clip(new ClipContext(eye, eye.add(ray), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player)).getLocation();
        var aimed = ProjectileUtil.getEntityHitResult(player, eye, stop,
                player.getBoundingBox().expandTowards(ray).inflate(1),
                entity -> entity.isPickable() && entity instanceof Mob mob && inRegion(player, settings, origin, mob),
                eye.distanceToSqr(stop));
        Mob target = aimed == null ? null : (Mob) aimed.getEntity();
        if (target == null) {
            target = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(settings.reach()), mob ->
                    inRegion(player, settings, origin, mob) && player.hasLineOfSight(mob)
                    && level.clip(new ClipContext(eye, mob.getBoundingBox().getCenter(),
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
                        .getType() == HitResult.Type.MISS)
                .stream().min(Comparator.comparingDouble((Mob m) -> m.getBoundingBox().getCenter().distanceToSqr(origin))
                        .thenComparingInt(Mob::getId)).orElse(null);
        }
        if (target == null) return new Hit(-1, false);
        if (!VanillaAttackGuard.allowsCustomImpact(player, target)) return new Hit(target.getId(), false);
        // One vanilla hurt on the server. Player.attack would also apply weapon attributes and cooldown.
        boolean damaged = target.hurt(player.damageSources().playerAttack(player), settings.damage());
        if (damaged) level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1, 1);
        return new Hit(target.getId(), damaged);
    }
    private static boolean inRegion(ServerPlayer player, CombatConfig.Settings settings, Vec3 origin, Mob mob) {
        if (!eligible(mob) || mob.isAlliedTo(player) || player.isAlliedTo(mob)
                || mob instanceof TamableAnimal pet && pet.isOwnedBy(player)) return false;
        var d = mob.getBoundingBox().getCenter().subtract(origin);
        return AttackGeometry.inRegion(d.x, d.y, d.z, player.getYRot(), settings.reach(), settings.arc());
    }
}
