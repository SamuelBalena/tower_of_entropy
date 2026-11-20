package com.towerofentropy.client;

import com.towerofentropy.combat.WeaponReadiness.ThreatMode;
import com.towerofentropy.combat.WeaponReadiness.WeaponState;
import com.towerofentropy.combat.WeaponTags;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT)
public final class ClientWeaponState {
    private static WeaponState weapon = WeaponState.SHEATHED;
    private static ThreatMode threat = ThreatMode.EXPLORATION;
    private static int itemId = -1;
    private static int slot = -1;

    private ClientWeaponState() {}

    public static void accept(WeaponState next, ThreatMode mode, int selectedSlot, int selectedItemId) {
        var p = Minecraft.getInstance().player;
        if (p == null) return;
        weapon = next;
        threat = mode;
        itemId = selectedItemId;
        slot = selectedSlot;
    }

    public static boolean canUse(InteractionHand hand) {
        var p = Minecraft.getInstance().player;
        if (p == null || !WeaponTags.isWeapon(p.getItemInHand(hand))) return true;
        return hand == InteractionHand.MAIN_HAND && weapon == WeaponState.DRAWN && selectedWeapon();
    }

    public static boolean inCombat() {
        return threat == ThreatMode.COMBAT && selectedWeapon();
    }

    private static boolean selectedWeapon() {
        var p = Minecraft.getInstance().player;
        return p != null && slot == p.getInventory().selected && itemId == Item.getId(p.getMainHandItem().getItem())
                && WeaponTags.isWeapon(p.getMainHandItem());
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut ignored) {
        weapon = WeaponState.SHEATHED;
        threat = ThreatMode.EXPLORATION;
        itemId = -1;
        slot = -1;
    }
}
