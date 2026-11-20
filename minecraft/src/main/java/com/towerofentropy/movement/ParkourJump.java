package com.towerofentropy.movement;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "towerofentropy")
public final class ParkourJump {
    public static final double VELOCITY = .618;

    private ParkourJump() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void groundJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player p) || p.isInWaterOrBubble() || p.isInLava()
                || p.isInFluidType() || p.onClimbable() || p.isPassenger() || p.isSpectator()
                || p.getAbilities().flying || p.isFallFlying() || p.isSwimming()) return;
        var boost = p.getEffect(MobEffects.JUMP);
        double bonus = boost == null ? 0 : .1 * (boost.getAmplifier() + 1);
        var motion = p.getDeltaMovement();
        // Retain the vanilla speed factor of the block under the feet, as well as Jump Boost.
        double factor = (motion.y - bonus) / .42;
        if (factor <= 0 || !Double.isFinite(factor)) return;
        var launch = SprintJump.takeoff(p, motion);
        p.setDeltaMovement(launch.x, VELOCITY * factor + bonus, launch.z);
    }
}
