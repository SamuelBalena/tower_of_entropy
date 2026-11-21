package com.towerofentropy.combat;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import com.towerofentropy.animation.ActionKeys;
import com.towerofentropy.client.ClientWeaponState;
import com.towerofentropy.client.PlayerRendererR3;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT)
public final class WeaponDynamicRuntimeTest {
    private static int title, stage;
    private static volatile int age;
    private static boolean opening, landedLocked, iframe, rollUnavailable;
    
    private static volatile long rollAt;
    private static long lastThreat, heavyStart;
    private static double baseSpeed, startX, startY, startZ, peakY, maxDrift, manualDistance;
    private static Mob mob;
    private static final List<EntityType<? extends Mob>> HOSTILES = List.of(EntityType.CREEPER, EntityType.SKELETON, EntityType.WOLF, EntityType.ENDERMAN);
    private static int hostileIndex;
    private static final List<Mob> mobs = new ArrayList<>();
    private static final int SPRINT_KEY = Boolean.getBoolean("toe.qa.slim") ? GLFW.GLFW_KEY_K : GLFW.GLFW_KEY_LEFT_CONTROL;
    private static final InputConstants.Key SPRINT = InputConstants.Type.KEYSYM.getOrCreate(SPRINT_KEY);
    private static boolean sprintHeld, rollClip;
    private static final BlockPos SHEATHED_BLOCK = new BlockPos(0, 101, 2);
    private static boolean punchMining;
    private static final boolean COUNTERS = Boolean.getBoolean("toe.w002.counters");
    private static final int[] COUNTER_THREAT_AGES = {842, 850, 932, 940};

    static int profileAge() { return age; }

    private static void check(String name, boolean ok, Object detail) {
        LogUtils.getLogger().info("TOE_W002 {} {} {}", name, ok ? "PASS" : "FAIL", detail);
    }

    @SubscribeEvent
    public static void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !Boolean.getBoolean("toe.w002.qa")) return;
        var mc = Minecraft.getInstance();
        if (!opening && mc.screen instanceof TitleScreen && mc.getOverlay() == null && ++title > 45) {
            opening = true;
            mc.options.pauseOnLostFocus = false;
            mc.createWorldOpenFlows().loadLevel(mc.screen, "Global Survival one");
        }
        if (mc.player == null || mc.getSingleplayerServer() == null || mc.getOverlay() != null) return;
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (mc.screen != null) return;
        age++;
        if (age == 5) {
            useTestSkin(mc);
            if (mc.getMainRenderTarget().width <= 1 || mc.getMainRenderTarget().height <= 1) {
                GLFW.glfwRestoreWindow(mc.getWindow().getWindow());
                mc.getWindow().setWindowed(854, 480);
            }
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.options.keySprint.setKey(SPRINT);
            mc.options.toggleSprint().set(Boolean.getBoolean("toe.qa.slim"));
            KeyMapping.resetMapping();
        }
        if (age == 8) check("tab_default", ActionKeys.WEAPON.getDefaultKey().getValue() == GLFW.GLFW_KEY_TAB
                && ActionKeys.WEAPON.getKey().equals(ActionKeys.WEAPON.getDefaultKey()), ActionKeys.WEAPON.getKey());
        if (age == 20) check("client_sheathed_empty_hand", mc.player.getMainHandItem().isEmpty()
                && EffectiveEquipment.selected(mc.player).is(Items.IRON_SWORD), EffectiveEquipment.selected(mc.player));
        if (age == 24) {
            mc.options.keyPlayerList.setDown(true);
            check("shared_tab_hides_player_list", !ActionKeys.playerListDown(mc.options.keyPlayerList), "");
            mc.options.keyPlayerList.setDown(false);
            weaponKey(GLFW.GLFW_KEY_TAB, GLFW.GLFW_PRESS);
        }
        if (age > 24 && age < 46) weaponKey(GLFW.GLFW_KEY_TAB, GLFW.GLFW_REPEAT);
        if (age == 46) weaponKey(GLFW.GLFW_KEY_TAB, GLFW.GLFW_RELEASE);
        if (age == 60) weaponKey(GLFW.GLFW_KEY_TAB, GLFW.GLFW_PRESS);
        if (age == 61) weaponKey(GLFW.GLFW_KEY_TAB, GLFW.GLFW_RELEASE);
        if (age == 85) {
            ActionKeys.WEAPON.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_K));
            KeyMapping.resetMapping();
            mc.options.keyPlayerList.setDown(true);
            check("player_list_after_remap", ActionKeys.playerListDown(mc.options.keyPlayerList), "");
            mc.options.keyPlayerList.setDown(false);
        }
        if (age == 88) weaponKey(GLFW.GLFW_KEY_K, GLFW.GLFW_PRESS);
        if (age == 89) weaponKey(GLFW.GLFW_KEY_K, GLFW.GLFW_RELEASE);
        if (age == 115) {
            ActionKeys.WEAPON.setKey(ActionKeys.WEAPON.getDefaultKey());
            KeyMapping.resetMapping();
        }
        if (age == 22 || age == 50 || age == 75 || age == 124 || age == 155 || age == 234 || age == 268 || age == 341 || age == 358) {
            var directory = new File(System.getProperty("toe.a3.output"));
            new File(directory, "screenshots").mkdirs();
            Screenshot.grab(directory, "w002-" + age + ".png", mc.getMainRenderTarget(), ignored -> {});
        }
        if (age == 124) manualDistance = cameraDistance(mc);
        if (age == 155) check("confirmed_combat_zoom", ClientWeaponState.inCombat()
                && Math.abs(cameraDistance(mc)-manualDistance+.40) < .08, manualDistance + " -> " + cameraDistance(mc));
        if (age == 156) cameraKey(GLFW.GLFW_KEY_DOWN, 1);
        if (age == 167) check("manual_zoom_during_combat", Math.abs(cameraDistance(mc)-(manualDistance+.10)) < .08, cameraDistance(mc));
        if (age == 168) {
            cameraKey(GLFW.GLFW_KEY_UP, 5);
            cameraKey(GLFW.GLFW_KEY_LEFT, 1);
        }
        if (age == 187) {
            check("combat_camera_minimum", Math.abs(cameraDistance(mc)-2) < .08, cameraDistance(mc));
            check("combat_left_shoulder", mc.gameRenderer.getMainCamera().getPosition().x - mc.player.getX() > .8, "");
            cameraKey(GLFW.GLFW_KEY_RIGHT, 1);
        }
        if (age == 202) {
            check("combat_right_shoulder", mc.gameRenderer.getMainCamera().getPosition().x - mc.player.getX() < -.8, "");
            cameraKey(GLFW.GLFW_KEY_DOWN, 4);
        }
        if (age == 237) check("camera_restores_manual_distance", !ClientWeaponState.inCombat()
                && Math.abs(cameraDistance(mc)-manualDistance) < .12, cameraDistance(mc));
        if (age >= 261 && age <= 295)
            rollClip |= com.towerofentropy.client.ClientRollMotion.visual() && PlayerRendererR3.controller() != null
                    && "UAL_roll".equals(PlayerRendererR3.controller().getAnimationName());
        if (age == 18) mc.mouseHandler.grabMouse();
        if (age >= 238 && age <= 302) {
            mc.options.keyUp.setDown(true);
            sprintInput(age <= 254 || age >= 261);
        }
        if (age == 241) check("normal_sprint_outside_combat", mc.player.isSprinting() && !ClientWeaponState.inCombat(), mc.player.position());
        if (age == 303) {
            check("roll_visual_received", rollClip, "");
            check("held_sprint_after_roll", mc.player.isSprinting(), mc.player.position());
            check("skin_variant", mc.player.getModelName().equals(Boolean.getBoolean("toe.qa.slim") ? "slim" : "default"), mc.player.getModelName());
            check("polymesh_r3", PlayerRendererR3.drawCalls > 10 && PlayerRendererR3.errors == 0, PlayerRendererR3.drawCalls);
            sprintInput(false);
        }
        if (age == 307) {
            check("released_sprint_in_combat", !mc.player.isSprinting() && ClientWeaponState.inCombat(), mc.player.position());
            mc.options.keyUp.setDown(false);
        }
        if (age >= 324 && age <= 330) mc.options.keyJump.setDown(age == 324);
        if (age >= 339 && age <= 383) mc.options.keyUp.setDown(age < 383);
        if (age >= 700 && age <= 750) {
            mc.player.setXRot(13);
            mc.player.setYRot(0);
        }
        if (age == 713) KeyMapping.click(mc.options.keyAttack.getKey());
        if (age >= 713 && age < 749) {
            mc.options.keyAttack.setDown(true);
            var controller = PlayerRendererR3.controller();
            punchMining |= controller != null && controller.getAnimationName().startsWith("UAL_punch_");
        }
        if (age == 749) {
            mc.options.keyAttack.setDown(false);
            check("sheathed_client_mining_and_punch", punchMining && mc.level.getBlockState(SHEATHED_BLOCK).isAir(), punchMining);
        }
        if (age == (COUNTERS ? 1060 : 825)) mc.stop();
    }

    private static void weaponKey(int key, int action) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new net.minecraftforge.client.event.InputEvent.Key(key, 0, action, 0));
    }

    private static void cameraKey(int code, int times) {
        for (int i=0; i<times; i++) KeyMapping.click(InputConstants.Type.KEYSYM.getOrCreate(code));
    }

    private static void sprintInput(boolean down) {
        if (sprintHeld == down) return;
        sprintHeld = down;
        KeyMapping.set(SPRINT, down);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.InputEvent.Key(
                SPRINT_KEY, 0, down ? GLFW.GLFW_PRESS : GLFW.GLFW_RELEASE, 0));
    }

    private static double cameraDistance(Minecraft mc) {
        return mc.player.getEyePosition().subtract(mc.gameRenderer.getMainCamera().getPosition()).dot(mc.player.getViewVector(1));
    }

    @SubscribeEvent
    public static void server(TickEvent.PlayerTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p)
                || !Boolean.getBoolean("toe.w002.qa") || age < 6) return;
        long now = p.level().getGameTime();
        if (stage == 0) {
            var level = p.serverLevel();
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, p.server);
            level.setDayTime(6000);
            for (int x = -5; x <= 5; x++) for (int z = -7; z <= 44; z++) {
                level.setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState());
                for (int y = 100; y < 104; y++) level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
            }
            p.setGameMode(GameType.SURVIVAL);
            p.setHealth(20);
            p.setInvulnerable(true);
            p.getFoodData().setFoodLevel(20);
            p.getInventory().clearContent();
            p.getInventory().selected = 0;
            p.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
            p.getInventory().setItem(1, new ItemStack(Items.IRON_SWORD));
            p.inventoryMenu.broadcastFullState();
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
            p.teleportTo(level, .5, 100, .5, 0, 0);
            CombatController.automatic(p);
            baseSpeed = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
            boolean initialAttackRejected = !CombatController.requestAttack(p, false).equals("accepted");
            check("sheathed_attack", initialAttackRejected && !WeaponReadiness.canUse(p, InteractionHand.MAIN_HAND), "");
            check("light_c_timeline", CombatMove.LIGHT_ATTACK_C.durationTicks == 26 && CombatMove.LIGHT_ATTACK_C.impactTicks == 14
                    && CombatMove.visualTime("sword_c", .7f) == .7f && CombatMove.visualTime("sword_c", 1.3f) > 1.999f, "");
            check("heavy_timeline", CombatMove.HEAVY_ATTACK.durationTicks == 25 && CombatMove.HEAVY_ATTACK.impactTicks == 12
                    && CombatMove.visualTime("heavy", .8f) == .8f && Math.abs(CombatMove.visualTime("heavy", 1.25f) - 70f/30) < .001, "");
            stage++;
        }
        if (stage == 1 && age >= 40) {
            check("draw_tab_without_repeat", WeaponReadiness.threat(p) == WeaponReadiness.ThreatMode.ARMED && WeaponReadiness.canUse(p, InteractionHand.MAIN_HAND), "");
            mob = spawn(p, EntityType.ZOMBIE, 14);
            stage++;
        }
        if (stage == 2 && age >= 54) {
            check("zombie_near_without_target", WeaponReadiness.threat(p) == WeaponReadiness.ThreatMode.ARMED, "");
            stage++;
        }
        if (stage == 3 && age >= 74) {
            check("sheath_tab", WeaponReadiness.threat(p) == WeaponReadiness.ThreatMode.EXPLORATION && !WeaponReadiness.canUse(p, InteractionHand.MAIN_HAND), "");
            stage++;
        }
        if (stage == 4 && age >= 107) {
            spawn(p, EntityType.WOLF, 12);
            spawn(p, EntityType.ENDERMAN, 15);
            spawn(p, EntityType.COW, 13).setTarget(p);
            spawn(p, EntityType.CHICKEN, 16).setTarget(p);
            check("drawn_attack_after_remap", CombatController.requestAttack(p, false).equals("accepted"), "");

            stage++;
        }
        if (stage == 5 && age >= 130) {
            check("passives_neutral", WeaponReadiness.threat(p) == WeaponReadiness.ThreatMode.ARMED, "");
            mob.setTarget(p);
            stage++;
        }
        if (stage == 6 && age >= 148) {
            check("zombie_target", WeaponReadiness.threat(p) == WeaponReadiness.ThreatMode.COMBAT, "");
            check("combat_speed", Math.abs(p.getAttributeValue(Attributes.MOVEMENT_SPEED)/baseSpeed - .95) < .0001, p.getAttributeValue(Attributes.MOVEMENT_SPEED));
            for (EntityType<? extends Mob> type : List.of(EntityType.CREEPER, EntityType.SKELETON, EntityType.WOLF, EntityType.ENDERMAN)) {
                spawn(p, type, 18).setTarget(p);
            }
            stage++;
        }
        if (stage == 7 && age >= 170) {
            check("multiple_threats_no_stacking", Math.abs(p.getAttributeValue(Attributes.MOVEMENT_SPEED)/baseSpeed - .95) < .0001, "");
            for (var entity : mobs) entity.discard();
            lastThreat = now;
            stage++;
        }
        if (stage == 8 && age >= 207) {
            check("aggro_grace", WeaponReadiness.threat(p) == WeaponReadiness.ThreatMode.COMBAT, now-lastThreat);
            stage++;
        }
        if (stage == 9 && age >= 242) {
            check("armed_speed_restored", WeaponReadiness.threat(p) == WeaponReadiness.ThreatMode.ARMED
                    && Math.abs(p.getAttributeValue(Attributes.MOVEMENT_SPEED) / (p.isSprinting() ? 1.3 : 1) - baseSpeed) < .001, now-lastThreat);
            mob = spawn(p, EntityType.ZOMBIE, 15);
            mob.setTarget(p);
            stage++;
        }
        if (stage == 10 && age >= 252) {
            check("sprint_held_at_combat_entry", RollController.status(p.getUUID()) == null, "");
            stage++;
        }
        if (age >= 261 && age <= 301) {
            var roll = RollController.status(p.getUUID());
            iframe |= RollController.iframe(p);
            if (roll != null && roll.startTick() != rollAt) {
                check(rollAt == 0 ? "remapped_sprint_roll" : "held_sprint_no_repeat", rollAt == 0, roll);
                rollAt = roll.startTick();
                if (roll.busy()) rollUnavailable = !RollController.requestContextual(p,0,1).equals("accepted");
            }
        }
        if (stage == 11 && age >= 310) {
            check("roll_busy_rejects_second_request", rollUnavailable, "");
            var completed = RollController.status(p.getUUID());
            check("roll_and_iframes", rollAt > 0 && iframe && !RollController.busy(p.getUUID()), completed);
            check("roll_authentic_distance_and_completion", completed != null && completed.reason().equals("complete")
                    && Math.abs(completed.distance()-2.5) < .09, completed);
            stage++;
        }
        if (stage == 12 && age >= 324) peakY = Math.max(peakY, p.getY());
        if (stage == 12 && age >= 337 && !p.onGround()) {
            startX=p.getX(); startY=p.getY(); startZ=p.getZ();
            peakY=Math.max(peakY,startY);
            heavyStart=now;
            check("airborne_heavy_requested", CombatController.requestAttack(p,true).equals("accepted") && HeavyAirLock.active(p), p.position());
            check("roll_during_heavy_rejected", !RollController.requestContextual(p,0,1).equals("accepted"), "");
            var packet = new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(startX + 4, startY, startZ + 4, false);
            double boundedX = HeavyAirLock.x(packet, startX, p.connection);
            double boundedZ = HeavyAirLock.z(packet, startZ, p.connection);
            check("server_packet_horizontal_bound", Math.abs(Math.hypot(boundedX-startX,boundedZ-startZ) - .40) < .001
                    && packet.getY(0) == startY, boundedX + " " + boundedZ);
            int hurt = p.hurtTime;
            p.hurtTime = 5;
            check("external_displacement_exemption", HeavyAirLock.x(packet,startX,p.connection) == startX+4, "");
            p.hurtTime = hurt;
            stage++;
        }
        if (stage == 13) {
            peakY = Math.max(peakY, p.getY());
            maxDrift = Math.max(maxDrift, Math.hypot(p.getX()-startX,p.getZ()-startZ));
            landedLocked |= p.onGround() && HeavyAirLock.active(p);
            if (now-heavyStart >= CombatMove.HEAVY_ATTACK.durationTicks-1) {
                check("heavy_horizontal_and_landing", maxDrift <= .86 && landedLocked, maxDrift + " landed=" + landedLocked);
                check("preserved_vertical_arc", peakY >= 102.45, peakY);
                stage++;
            }
        }
        if (stage == 14 && now-heavyStart >= CombatMove.HEAVY_ATTACK.durationTicks+7) {
            double resumed = Math.hypot(p.getX()-startX,p.getZ()-startZ);
            check("released_after_recovery", !HeavyAirLock.active(p) && resumed > maxDrift+.24, resumed);
            p.getInventory().selected = 1;
            check("same_item_different_slot_sheathed", !WeaponReadiness.canUse(p, InteractionHand.MAIN_HAND), "");
            for (var entity : mobs) entity.discard();
            p.getInventory().selected = 0;
            p.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
            p.inventoryMenu.broadcastFullState();
            WeaponReadiness.toggle(p);
            lastThreat = now;
            stage++;
        }
        if (stage == 15 && now-lastThreat >= (hostileIndex == 0 ? 12 : 64)) {
            if (hostileIndex == HOSTILES.size()) {
                check("completed", true, now);
                stage = 17;
                return;
            }
            var type = HOSTILES.get(hostileIndex);
            check("armed_before_" + EntityType.getKey(type).getPath(), WeaponReadiness.threat(p) == WeaponReadiness.ThreatMode.ARMED, "");
            mob = spawn(p, type, 8);
            mob.setTarget(p);
            lastThreat = now;
            stage = 16;
        }
        if (stage == 17 && age >= 690) {
            WeaponReadiness.toggle(p);
            stage++;
        }
        if (stage == 18 && age >= 704) {
            testSheathedGameplay(p);
            stage++;
        }
        if (stage == 19 && age >= 752) {
            check("sheathed_server_mining", p.level().getBlockState(SHEATHED_BLOCK).isAir()
                    && EffectiveEquipment.selected(p).getDamageValue() == 0, p.level().getBlockState(SHEATHED_BLOCK));
            var sword = EffectiveEquipment.selected(p);
            var swap = new net.minecraft.network.protocol.game.ServerboundPlayerActionPacket(
                    net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                    BlockPos.ZERO, net.minecraft.core.Direction.DOWN);
            p.connection.handlePlayerAction(swap);
            boolean swapped = EffectiveEquipment.selected(p).isEmpty() && p.getOffhandItem() == sword;
            p.connection.handlePlayerAction(swap);
            check("sheathed_hotbar_offhand_swap_preserves_stack",
                    swapped && EffectiveEquipment.selected(p) == sword && p.getOffhandItem().isEmpty(), sword);
            stage++;
        }
        if (stage == 20 && age >= 768) {
            p.getInventory().setItem(0, ItemStack.EMPTY);
            p.inventoryMenu.broadcastFullState();
            stage++;
        }
        if (COUNTERS && stage == 21 && age >= 815) {
            p.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
            p.inventoryMenu.broadcastFullState();
            WeaponReadiness.toggle(p);
            stage++;
        }
        int counterPhase = stage - 22;
        if (counterPhase >= 0 && counterPhase < COUNTER_THREAT_AGES.length && age >= COUNTER_THREAT_AGES[counterPhase]) {
            if (counterPhase % 2 == 0) {
                mob = spawn(p, EntityType.ZOMBIE, 7);
                mob.setTarget(p);
            } else {
                mob.discard();
            }
            stage++;
        }
        if (stage == 16 && now-lastThreat >= 8) {
            check(EntityType.getKey(mob.getType()).getPath() + "_actual_target",
                    WeaponReadiness.threat(p) == WeaponReadiness.ThreatMode.COMBAT, mob.getTarget());
            mob.discard();
            hostileIndex++;
            lastThreat = now;
            stage = 15;
        }
    }

    private static void testSheathedGameplay(ServerPlayer p) {
        var level = p.serverLevel();
        p.teleportTo(level, .5, 100, .5, 0, 13);
        var sword = EffectiveEquipment.selected(p);
        p.getInventory().setItem(0, ItemStack.EMPTY);
        var web = Blocks.COBWEB.defaultBlockState();
        float bareSpeed = p.getDestroySpeed(web);
        boolean bareHarvest = p.hasCorrectToolForDrops(web);
        p.getInventory().setItem(0, sword);
        sword.enchant(net.minecraft.world.item.enchantment.Enchantments.BLOCK_EFFICIENCY, 5);
        sword.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, 5);
        check("sheathed_speed_and_harvest_match_empty", sword.getDestroySpeed(web) > bareSpeed
                && p.getDestroySpeed(web) == bareSpeed && p.hasCorrectToolForDrops(web) == bareHarvest, bareSpeed);
        check("server_sheathed_empty_hand", p.getMainHandItem().isEmpty() && !EffectiveEquipment.blocksMining(p), sword);
        var cow = spawn(p, EntityType.COW, 1);
        cow.setInvulnerable(false);
        float health = cow.getHealth();
        p.attack(cow);
        float damage = health - cow.getHealth();
        check("unarmed_damage_and_no_durability", damage > 0 && damage <= 1.01 && sword.getDamageValue() == 0, damage);
        cow.discard();
        check("enchanted_sword_still_selected", EffectiveEquipment.selected(p) == sword && sword.isEnchanted(), sword);
        var lever = SHEATHED_BLOCK.east();
        level.setBlockAndUpdate(lever, Blocks.LEVER.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LeverBlock.FACE,
                        net.minecraft.world.level.block.state.properties.AttachFace.FLOOR));
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(lever),
                net.minecraft.core.Direction.UP, lever, false);
        var result = p.gameMode.useItemOn(p, level, p.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        check("sheathed_use_block", result.consumesAction()
                && level.getBlockState(lever).getValue(net.minecraft.world.level.block.LeverBlock.POWERED), result);
        level.setBlockAndUpdate(lever, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(SHEATHED_BLOCK, Blocks.DIRT.defaultBlockState());
        p.inventoryMenu.broadcastFullState();
    }

    @SuppressWarnings("unchecked")
    private static void useTestSkin(Minecraft mc) {
        try {
            boolean slim = Boolean.getBoolean("toe.qa.slim");
            var info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            var textures = info.getClass().getDeclaredField("textureLocations");
            textures.setAccessible(true);
            var map = (java.util.Map<com.mojang.authlib.minecraft.MinecraftProfileTexture.Type, net.minecraft.resources.ResourceLocation>) textures.get(info);
            map.put(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN, new net.minecraft.resources.ResourceLocation(
                    "minecraft", slim ? "textures/entity/player/slim/steve.png" : "textures/entity/player/wide/steve.png"));
            map.remove(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.CAPE);
            var model = info.getClass().getDeclaredField("skinModel");
            model.setAccessible(true);
            model.set(info, slim ? "slim" : "default");
            var pending = info.getClass().getDeclaredField("pendingTextures");
            pending.setAccessible(true);
            pending.setBoolean(info, true);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not prepare QA skin", ex);
        }
    }

    private static Mob spawn(ServerPlayer p, EntityType<? extends Mob> type, int distance) {
        var entity = type.create(p.serverLevel());
        entity.setNoAi(true);
        entity.setInvulnerable(true);
        entity.setPos(p.getX()+3, p.getY(), p.getZ()+distance);
        p.serverLevel().addFreshEntity(entity);
        mobs.add(entity);
        return entity;
    }
}
