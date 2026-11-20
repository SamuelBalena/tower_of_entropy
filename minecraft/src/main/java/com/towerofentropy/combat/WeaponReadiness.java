package com.towerofentropy.combat;

import com.towerofentropy.animation.ActionNetwork;
import com.towerofentropy.movement.WallSlide;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The main hand is the active weapon slot. Weapon posture and threat mode are independent. */
@Mod.EventBusSubscriber(modid = "towerofentropy")
public final class WeaponReadiness {
    public enum WeaponState { SHEATHED, DRAWING, DRAWN, SHEATHING }
    public enum ThreatMode { EXPLORATION, ARMED, COMBAT }
    private static final int TRANSITION_TICKS = 6, SCAN_TICKS = 5;
    private static final double THREAT_RADIUS = 24;
    private static final UUID SPEED_ID = UUID.fromString("8debd63c-858f-4b46-bcb4-5b5eb5488712");
    private static final AttributeModifier SPEED = new AttributeModifier(SPEED_ID, "TOE combat travel", -.05,
            AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final Map<UUID, Session> PLAYERS = new HashMap<>();

    private static final class Session {
        ItemStack equipped = ItemStack.EMPTY;
        int slot = -1;
        WeaponState weapon = WeaponState.SHEATHED, sentWeapon;
        ThreatMode threat = ThreatMode.EXPLORATION, sentThreat;
        long transitionEnd, nextScan, threatUntil;
    }

    private WeaponReadiness() {}

    public static boolean canUse(ServerPlayer player, InteractionHand hand) {
        if (!WeaponTags.isWeapon(player.getItemInHand(hand))) return true;
        var s = PLAYERS.get(player.getUUID());
        return hand == InteractionHand.MAIN_HAND && s != null && s.weapon == WeaponState.DRAWN
                && s.equipped == player.getMainHandItem() && s.slot == player.getInventory().selected;
    }

    public static ThreatMode threat(ServerPlayer player) {
        var s = PLAYERS.get(player.getUUID());
        return s != null && s.equipped == player.getMainHandItem() && s.slot == player.getInventory().selected
                ? s.threat : ThreatMode.EXPLORATION;
    }

    private static Session equipment(ServerPlayer p) {
        var s = PLAYERS.computeIfAbsent(p.getUUID(), ignored -> new Session());
        ItemStack equipped = p.getMainHandItem();
        int slot = p.getInventory().selected;
        if (s.equipped != equipped || s.slot != slot || !WeaponTags.isWeapon(equipped)) {
            if (WeaponTags.isWeapon(s.equipped)) CombatController.cancel(p);
            s.equipped = equipped;
            s.slot = slot;
            s.weapon = WeaponState.SHEATHED;
            s.transitionEnd = s.nextScan = s.threatUntil = 0;
        }
        return s;
    }

    public static void toggle(ServerPlayer p) {
        if (!p.isAlive() || p.isSpectator() || p.containerMenu != p.inventoryMenu || p.isUsingItem()
                || RollController.busy(p.getUUID()) || WallSlide.holdsPlayer(p)) return;
        var attack = CombatController.status(p.getUUID());
        if (attack != null && attack.attacking()) return;
        var s = equipment(p);
        if (!WeaponTags.isWeapon(s.equipped) || s.weapon == WeaponState.DRAWING || s.weapon == WeaponState.SHEATHING) return;
        s.weapon = s.weapon == WeaponState.DRAWN ? WeaponState.SHEATHING : WeaponState.DRAWING;
        s.transitionEnd = p.level().getGameTime() + TRANSITION_TICKS;
        if (s.weapon == WeaponState.SHEATHING) s.threatUntil = 0;
        publish(p, s, ThreatMode.EXPLORATION);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !(event.player instanceof ServerPlayer p)) return;
        if (!p.isAlive() || p.isSpectator()) {
            if (PLAYERS.containsKey(p.getUUID())) reset(p, true);
            return;
        }
        var s = equipment(p);
        long now = p.level().getGameTime();
        if (now >= s.transitionEnd) {
            if (s.weapon == WeaponState.DRAWING) s.weapon = WeaponState.DRAWN;
            if (s.weapon == WeaponState.SHEATHING) s.weapon = WeaponState.SHEATHED;
        }
        if (s.weapon != WeaponState.DRAWN) {
            publish(p, s, ThreatMode.EXPLORATION);
            return;
        }
        if (now >= s.nextScan) {
            s.nextScan = now + SCAN_TICKS;
            var threats = p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(THREAT_RADIUS),
                    mob -> isThreat(mob, p));
            if (!threats.isEmpty()) s.threatUntil = now + CombatConfig.THREAT_GRACE.get();
        }
        publish(p, s, now < s.threatUntil ? ThreatMode.COMBAT : ThreatMode.ARMED);
    }

    private static boolean isThreat(Mob mob, ServerPlayer p) {
        return mob.isAlive() && mob.getTarget() == p && !mob.isAlliedTo(p)
                && mob.distanceToSqr(p) <= THREAT_RADIUS * THREAT_RADIUS
                && (mob instanceof Enemy || mob instanceof NeutralMob
                    || mob.getType().getCategory() == MobCategory.MONSTER);
    }

    private static void publish(ServerPlayer p, Session s, ThreatMode next) {
        s.threat = next;
        if (s.sentWeapon == s.weapon && s.sentThreat == next) return;
        var speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            if (next == ThreatMode.COMBAT) {
                if (speed.getModifier(SPEED_ID) == null) speed.addTransientModifier(SPEED);
            } else speed.removeModifier(SPEED_ID);
        }
        s.sentWeapon = s.weapon;
        s.sentThreat = next;
        ActionNetwork.weaponState(p, s.weapon, next);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && !canUse(p, InteractionHand.MAIN_HAND))
            event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void use(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer p && !canUse(p, event.getHand())) {
            event.setCancellationResult(net.minecraft.world.InteractionResult.PASS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer p && !canUse(p, event.getHand())) event.setUseItem(Event.Result.DENY);
    }

    private static void reset(ServerPlayer p, boolean notify) {
        PLAYERS.remove(p.getUUID());
        var speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(SPEED_ID);
        HeavyAirLock.end(p);
        if (notify) ActionNetwork.weaponState(p, WeaponState.SHEATHED, ThreatMode.EXPLORATION);
    }

    @SubscribeEvent public static void death(LivingDeathEvent e) { if (e.getEntity() instanceof ServerPlayer p) reset(p, true); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer p) reset(p, true); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) { reset(p, true); CombatController.cancel(p); }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) { if (e.getEntity() instanceof ServerPlayer p) reset(p, false); }
    @SubscribeEvent public static void stop(ServerStoppedEvent ignored) { PLAYERS.clear(); }

}
