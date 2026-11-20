package com.towerofentropy.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Suppress duplicate vanilla sword attacks while preserving Forge protection hooks. */
@Mod.EventBusSubscriber(modid = "towerofentropy")
public final class VanillaAttackGuard {
    private static final ThreadLocal<AttackEntityEvent> CUSTOM_IMPACT = new ThreadLocal<>();

    @SubscribeEvent
    public static void attack(AttackEntityEvent event) {
        if (event != CUSTOM_IMPACT.get() && event.getEntity() instanceof ServerPlayer player
                && CombatController.enabled(player) && player.getMainHandItem().getItem() instanceof SwordItem)
            event.setCanceled(true);
    }

    static boolean allowsCustomImpact(ServerPlayer player, Entity target) {
        var event = new AttackEntityEvent(player, target);
        var previous = CUSTOM_IMPACT.get();
        CUSTOM_IMPACT.set(event);
        try {
            if (MinecraftForge.EVENT_BUS.post(event)) return false;
            var stack = player.getMainHandItem();
            return stack.isEmpty() || !stack.getItem().onLeftClickEntity(stack, player, target);
        } finally {
            if (previous == null) CUSTOM_IMPACT.remove();
            else CUSTOM_IMPACT.set(previous);
        }
    }
}
