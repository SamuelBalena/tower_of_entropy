package com.towerofentropy.combat;

import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingSwapItemsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Resolves main-hand equipment independently of the physical hotbar stack. */
@Mod.EventBusSubscriber(modid = "towerofentropy")
public final class EffectiveEquipment {
    private static Predicate<Player> clientDrawn = player -> true;
    private static Predicate<Player> clientBlocks = player -> false;

    private EffectiveEquipment() {}

    public static void registerClient(Predicate<Player> drawn, Predicate<Player> blocks) {
        clientDrawn = drawn;
        clientBlocks = blocks;
    }

    public static ItemStack selected(Player player) {
        return player.getInventory().getSelected();
    }

    // Called from the vanilla equipment and correct-tool queries.
    public static ItemStack mainHand(Inventory inventory) {
        var stack = inventory.getSelected();
        if (!WeaponTags.isWeapon(stack)) return stack;
        return drawn(inventory.player) ? stack : ItemStack.EMPTY;
    }

    // Inventory.getDestroySpeed reads the slot array directly.
    public static float destroySpeed(ItemStack stack, BlockState state, Inventory inventory) {
        if (WeaponTags.isWeapon(stack) && !drawn(inventory.player)) return 1;
        return stack.getDestroySpeed(state);
    }

    public static boolean blocksMining(Player player) {
        if (!WeaponTags.isWeapon(selected(player))) return false;
        return player instanceof ServerPlayer server
                ? WeaponReadiness.posture(server) != WeaponReadiness.WeaponState.SHEATHED
                : clientBlocks.test(player);
    }

    private static boolean drawn(Player player) {
        return player instanceof ServerPlayer server
                ? WeaponReadiness.posture(server) == WeaponReadiness.WeaponState.DRAWN
                : clientDrawn.test(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void refreshAttackAttributes(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // A hotbar change and attack can arrive before vanilla's next equipment tick.
        try {
            EquipmentUpdates.DETECT.invoke(player);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot refresh effective attack attributes", failure);
        }
    }

    private static final class EquipmentUpdates {
        private static final java.lang.reflect.Method DETECT =
                ObfuscationReflectionHelper.findMethod(LivingEntity.class, "m_21315_");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void swapHands(LivingSwapItemsEvent.Hands event) {
        if (event.getEntity() instanceof ServerPlayer player && !drawn(player)
                && WeaponTags.isWeapon(selected(player))) event.setItemSwappedToOffHand(selected(player));
    }
}
