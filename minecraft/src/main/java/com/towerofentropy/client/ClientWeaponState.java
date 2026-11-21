package com.towerofentropy.client;

import com.towerofentropy.combat.WeaponReadiness.ThreatMode;
import com.towerofentropy.combat.WeaponReadiness.WeaponState;
import com.towerofentropy.combat.WeaponTags;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import com.towerofentropy.combat.EffectiveEquipment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT)
public final class ClientWeaponState {
    private static WeaponState weapon = WeaponState.SHEATHED;
    private static ThreatMode threat = ThreatMode.EXPLORATION;
    private static int itemId = -1;
    private static int slot = -1;

    private ClientWeaponState() {}

    public static void registerEquipment() {
        EffectiveEquipment.registerClient(
                player -> !player.isLocalPlayer() || posture(player) == WeaponState.DRAWN,
                player -> player.isLocalPlayer() && posture(player) != WeaponState.SHEATHED);
    }

    private static WeaponState posture(Player player) {
        return selectedWeapon(player) ? weapon : WeaponState.SHEATHED;
    }

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
        if (p == null || !WeaponTags.isWeapon((hand == InteractionHand.MAIN_HAND ? EffectiveEquipment.selected(p) : p.getOffhandItem()))) return true;
        return hand == InteractionHand.MAIN_HAND && weapon == WeaponState.DRAWN && selectedWeapon(p);
    }

    public static boolean inCombat() {
        return threat == ThreatMode.COMBAT && selectedWeapon(Minecraft.getInstance().player);
    }

    private static boolean selectedWeapon(Player p) {
        return p != null && slot == p.getInventory().selected && itemId == Item.getId(EffectiveEquipment.selected(p).getItem())
                && WeaponTags.isWeapon(EffectiveEquipment.selected(p));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && weapon != WeaponState.SHEATHED
                && !selectedWeapon(Minecraft.getInstance().player)) clear();
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut ignored) {
        clear();
    }

    private static void clear() {
        weapon = WeaponState.SHEATHED;
        threat = ThreatMode.EXPLORATION;
        itemId = -1;
        slot = -1;
    }
}
