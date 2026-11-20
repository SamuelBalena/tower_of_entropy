package com.towerofentropy.combat;

import com.mojang.logging.LogUtils;
import com.towerofentropy.client.camera.CameraDamping;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

final class CameraRefinementRuntimeTest {
    static final boolean ENABLED = Boolean.getBoolean("toe.c001.refinement.qa");
    private static final int STEP = 38;
    private static final EntityType<?>[] TYPES = {EntityType.COW, EntityType.CHICKEN, EntityType.PIG,
            EntityType.SHEEP, EntityType.ZOMBIE, EntityType.CREEPER, EntityType.SKELETON};
    private static final String[] WEAPONS = {"empty", "sword", "stick"};
    private static final int MATRIX_END = 145 + 63 * STEP;
    static final int STABILITY_LENGTH = 145 + 2 * STEP + 8;
    static final int LENGTH = MATRIX_END + 2 * STEP + 8;
    private static volatile int targetId;
    private static volatile float initialHealth;
    private static float changeYaw, previousYaw;
    private static double maxYawStep, lastLag, highestLag;
    private static boolean airborne, landed;
    private static final BlockPos NEAR = new BlockPos(0, 101, 2);
    private static final BlockPos COVER = NEAR.north();

    static void tick(Minecraft mc, int tick) {
        var p = mc.player;
        var camera = mc.gameRenderer.getMainCamera();
        if (tick == 0) {
            double result = filter(30);
            check("smoothing_fps", Math.abs(result - filter(60)) < 1e-12
                    && Math.abs(result - filter(144)) < 1e-12, result);
            A3RuntimeTest.server(server -> {
                var level = server.serverLevel();
                for (int y = 100; y <= 105; y++) {
                    for (int x = -2; x <= 2; x++)
                        level.setBlockAndUpdate(new BlockPos(x, y, -2), Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(new BlockPos(0, y, 4), Blocks.AIR.defaultBlockState());
                }
                server.teleportTo(level, .5, 100, .5, 0, 0);
            });
        }
        if (tick == 30) previousYaw = changeYaw = camera.getYRot();
        if (tick == 31) A3RuntimeTest.server(s -> s.serverLevel().setBlockAndUpdate(NEAR, Blocks.STONE.defaultBlockState()));
        if (tick >= 31 && tick <= 43) {
            maxYawStep = Math.max(maxYawStep, Math.abs(Mth.wrapDegrees(camera.getYRot() - previousYaw)));
            previousYaw = camera.getYRot();
        }
        if (tick == 43) {
            double total = Math.abs(Mth.wrapDegrees(camera.getYRot() - changeYaw));
            check("focus_transition", total > 1 && maxYawStep < total * .72,
                    "total=" + total + " maxTick=" + maxYawStep);
            A3RuntimeTest.server(s -> s.serverLevel().setBlockAndUpdate(NEAR, Blocks.AIR.defaultBlockState()));
        }
        if (tick == 62) mc.options.keyJump.setDown(true);
        if (tick == 65) mc.options.keyJump.setDown(false);
        if (tick >= 62 && tick < 125) {
            airborne |= !p.onGround();
            landed |= airborne && p.onGround();
            double lag = Math.abs(camera.getPosition().y - p.getEyeY() - .25);
            highestLag = Math.max(highestLag, lag);
            lastLag = lag;
        }
        if (tick == 125) {
            check("jump_follow_and_settle", airborne && landed && highestLag < 1.0 && lastLag < .035,
                    "air=" + airborne + " landed=" + landed + " max=" + highestLag + " settled=" + lastLag);
            A3RuntimeTest.server(CameraRefinementRuntimeTest::serverBoundaries);
        }
        if (Boolean.getBoolean("toe.c001.stability.qa") && tick >= 145) {
            vanillaBoundaries(mc, tick - 145);
            return;
        }
        if (tick >= MATRIX_END) {
            vanillaBoundaries(mc, tick - MATRIX_END);
            return;
        }
        if (tick < 145) return;
        int index = (tick - 145) / STEP;
        int phase = (tick - 145) % STEP;
        int weapon = index / TYPES.length % WEAPONS.length;
        int view = index / (TYPES.length * WEAPONS.length);
        int species = index % TYPES.length;
        if (phase == 0) {
            mc.options.setCameraType(view == 2 ? CameraType.FIRST_PERSON : CameraType.THIRD_PERSON_BACK);
            if (view == 0 && weapon == 0 && species == 0) press(mc, "closer", 4);
            if (view == 1 && weapon == 0 && species == 0) {
                press(mc, "shoulder_left", 1);
                press(mc, "farther", 12);
            }
            A3RuntimeTest.server(server -> {
                var level = server.serverLevel();
                if (level.getEntity(targetId) instanceof Mob old) old.discard();
                var stack = weapon == 0 ? ItemStack.EMPTY : new ItemStack(weapon == 1 ? Items.IRON_SWORD : Items.STICK);
                server.getInventory().setItem(8, stack);
                server.getInventory().selected = 8;
                server.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(8));
                server.inventoryMenu.broadcastFullState();
                server.teleportTo(level, .5, 100, .5, 0, 0);
                var mob = (Mob) TYPES[species].create(level);
                mob.setNoAi(true);
                mob.setPersistenceRequired();
                mob.setInvulnerable(false);
                mob.setPos(.5, 100, 2.9);
                targetId = mob.getId();
                initialHealth = mob.getHealth();
                level.addFreshEntity(mob);
            });
        }
        if (phase >= 7 && phase <= 16 && mc.level.getEntity(targetId) instanceof Mob mob) {
            Vec3 direction = mob.getBoundingBox().getCenter().subtract(p.getEyePosition());
            p.setYRot((float) Math.toDegrees(Math.atan2(-direction.x, direction.z)));
            p.setXRot((float) Math.toDegrees(-Math.atan2(direction.y, direction.horizontalDistance())));
        }
        if (phase == 16) {
            boolean aimed = mc.hitResult instanceof EntityHitResult hit && hit.getEntity().getId() == targetId;
            check("pick_" + species + "_" + WEAPONS[weapon] + "_view" + view, aimed, mc.hitResult);
            KeyMapping.click(mc.options.keyAttack.getKey());
        }
        if (phase == 34) A3RuntimeTest.server(server -> {
            if (server.serverLevel().getEntity(targetId) instanceof Mob mob)
                check("damage_" + species + "_" + WEAPONS[weapon] + "_view" + view, mob.getHealth() < initialHealth,
                        TYPES[species] + " health=" + initialHealth + "->" + mob.getHealth());
            else check("damage_" + species + "_" + WEAPONS[weapon] + "_view" + view, false, "missing target");
        });
    }

    private static void vanillaBoundaries(Minecraft mc, int tick) {
        if (tick >= 2 * STEP) {
            if (tick == 2 * STEP + 1 && !Boolean.getBoolean("toe.c001.stability.qa")) {
                press(mc, "closer", 8);
                press(mc, "shoulder_right", 1);
            }
            return;
        }
        boolean cover = tick >= STEP;
        int phase = tick % STEP;
        if (phase == 0) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            A3RuntimeTest.server(p -> {
                var level = p.serverLevel();
                if (level.getEntity(targetId) instanceof Mob old) old.discard();
                p.getInventory().setItem(8, cover ? ItemStack.EMPTY : new ItemStack(Items.STICK));
                p.inventoryMenu.broadcastFullState();
                p.teleportTo(level, .5, 100, .5, 0, 20);
                var state = (cover ? Blocks.STONE : Blocks.AIR).defaultBlockState();
                level.setBlockAndUpdate(COVER, state);
                level.setBlockAndUpdate(COVER.below(), state);
                var mob = EntityType.COW.create(level);
                mob.setNoAi(true);
                mob.setPos(.5, 100, cover ? 2.85 : 5.5);
                targetId = mob.getId();
                initialHealth = mob.getHealth();
                level.addFreshEntity(mob);
            });
        }
        if (phase == 16) {
            check(cover ? "vanilla_cover_not_picked" : "vanilla_far_not_picked",
                    !(mc.hitResult instanceof EntityHitResult hit && hit.getEntity().getId() == targetId), mc.hitResult);
            KeyMapping.click(mc.options.keyAttack.getKey());
        }
        if (phase == 30) A3RuntimeTest.server(p -> {
            if (p.serverLevel().getEntity(targetId) instanceof Mob mob)
                check(cover ? "vanilla_wall_blocks_damage" : "vanilla_range_blocks_damage",
                        mob.getHealth() == initialHealth && !mob.isInWall(), mob.getHealth());
        });
    }

    private static void serverBoundaries(net.minecraft.server.level.ServerPlayer p) {
        var level = p.serverLevel();
        var mob = EntityType.COW.create(level);
        mob.setNoAi(true);
        level.addFreshEntity(mob);
        var settings = CombatConfig.snapshot();
        p.setYRot(0);
        p.setXRot(20);
        mob.setPos(.5, 100, 5.5);
        check("sword_max_reach", !SwordTargeting.impact(p, settings).damaged(), settings.reach());
        mob.setPos(.5, 100, 2.85);
        level.setBlockAndUpdate(COVER, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(COVER.below(), Blocks.STONE.defaultBlockState());
        check("sword_blocked_by_wall", !SwordTargeting.impact(p, settings).damaged(), mob.getHealth());
        mob.setPos(1.1, 100, 2.85);
        p.setPos(.92, 100, .5);
        p.setYRot(-12);
        check("sword_exposed_hitbox", SwordTargeting.impact(p, settings).damaged(), mob.getHealth());
        var veto = new AttackVeto(mob);
        MinecraftForge.EVENT_BUS.register(veto);
        try {
            mob.invulnerableTime = 0;
            check("forge_attack_veto", !SwordTargeting.impact(p, settings).damaged() && veto.observed, mob.getHealth());
        } finally {
            MinecraftForge.EVENT_BUS.unregister(veto);
        }
        mob.discard();
        p.setPos(.5, 100, .5);
        level.setBlockAndUpdate(COVER, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(COVER.below(), Blocks.AIR.defaultBlockState());
    }

    public static final class AttackVeto {
        private final Mob target;
        private boolean observed;

        private AttackVeto(Mob target) { this.target = target; }

        @SubscribeEvent
        public void deny(AttackEntityEvent event) {
            if (event.getTarget() == target) {
                observed = true;
                event.setCanceled(true);
            }
        }
    }

    private static double filter(int frames) {
        double value = 0;
        for (int i = 0; i < frames; i++) value = CameraDamping.approach(value, 1, 1.0 / frames, .10);
        return value;
    }

    private static void press(Minecraft mc, String control, int count) {
        for (var mapping : mc.options.keyMappings)
            if (mapping.getName().equals("key.towerofentropy.camera." + control))
                for (int i = 0; i < count; i++) KeyMapping.click(mapping.getKey());
    }

    private static void check(String name, boolean pass, Object detail) {
        LogUtils.getLogger().info("TOE_C0011 {} {} {}", name, pass, detail);
    }
}
