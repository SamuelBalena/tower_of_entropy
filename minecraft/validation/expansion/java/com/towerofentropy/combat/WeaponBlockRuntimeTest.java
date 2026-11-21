package com.towerofentropy.combat;

import com.mojang.logging.LogUtils;
import com.towerofentropy.client.PlayerRendererR3;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

final class WeaponBlockRuntimeTest {
    static final boolean ENABLED = Boolean.getBoolean("toe.w001.qa");
    private static final int START = 2320, STEP = 52;
    private static final BlockPos POS = new BlockPos(0, 101, 2);
    private record Case(String name, Item item, Block block, boolean weapon, boolean creative, Item offhand) {
        private Case(String name, Item item, Block block, boolean weapon, boolean creative) {
            this(name, item, block, weapon, creative, Items.AIR);
        }
    }

    private static final Case[] CASES = {
        new Case("wood_grass", Items.WOODEN_SWORD, Blocks.GRASS, true, false),
        new Case("iron_flower", Items.IRON_SWORD, Blocks.POPPY, true, false),
        new Case("diamond_leaves", Items.DIAMOND_SWORD, Blocks.OAK_LEAVES, true, false),
        new Case("netherite_obsidian", Items.NETHERITE_SWORD, Blocks.OBSIDIAN, true, false),
        new Case("iron_mushroom", Items.IRON_SWORD, Blocks.BROWN_MUSHROOM, true, false),
        new Case("creative_sword", Items.IRON_SWORD, Blocks.STONE, true, true),
        new Case("datapack_survival", Items.BLAZE_ROD, Blocks.GRASS, true, false),
        new Case("datapack_creative", Items.BLAZE_ROD, Blocks.STONE, true, true),
        new Case("pickaxe", Items.IRON_PICKAXE, Blocks.STONE, false, false),
        new Case("axe", Items.IRON_AXE, Blocks.OAK_LOG, false, false),
        new Case("shovel", Items.IRON_SHOVEL, Blocks.DIRT, false, false),
        new Case("hoe", Items.IRON_HOE, Blocks.HAY_BLOCK, false, false),
        new Case("shears", Items.SHEARS, Blocks.OAK_LEAVES, false, false),
        new Case("empty", Items.AIR, Blocks.DIRT, false, false),
        new Case("torch", Items.TORCH, Blocks.GRASS, false, false),
        new Case("common_block", Items.COBBLESTONE, Blocks.GRASS, false, false),
        new Case("creative_pickaxe", Items.IRON_PICKAXE, Blocks.STONE, false, true),
        new Case("offhand_weapon_pickaxe", Items.IRON_PICKAXE, Blocks.STONE, false, false, Items.IRON_SWORD)
    };
    private static final EntityType<?>[] MOBS = {
        EntityType.COW, EntityType.CHICKEN, EntityType.ZOMBIE, EntityType.CREEPER, EntityType.SKELETON
    };
    private static volatile int target;
    private static volatile float initialHealth;
    private static boolean swing, mining, special, punch;
    private static long posts;

    static boolean tick(Minecraft mc, int age, String clip) {
        if (age < START) return false;
        int index = (age - START) / STEP;
        int phase = (age - START) % STEP;
        if (index < CASES.length) {
            blockCase(mc, CASES[index], phase, clip);
        } else if (index < CASES.length + MOBS.length) {
            entityCase(mc, MOBS[index - CASES.length], phase, clip);
        } else if (phase == 0) {
            A3RuntimeTest.server(WeaponBlockRuntimeTest::serverBoundaries);
        } else if (phase == 18) {
            A3RuntimeTest.server(p -> {
                check("abort_delayed_after_swap", !flag(p.gameMode, "f_9253_") && !flag(p.gameMode, "f_9249_")
                        && p.serverLevel().getBlockState(POS).is(Blocks.OBSIDIAN), POS);
                boolean command = p.server.getCommands().performPrefixedCommand(
                        p.createCommandSourceStack().withPermission(4).withSuppressedOutput(), "setblock 0 101 2 air") > 0;
                check("admin_setblock", command && p.serverLevel().getBlockState(POS).isAir(), POS);
            });
        } else if (phase == 40) return true;
        return false;
    }

    private static void blockCase(Minecraft mc, Case test, int phase, String clip) {
        var p = mc.player;
        if (phase == 0) {
            swing = mining = special = punch = false;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.options.hideGui = true;
            mc.mouseHandler.grabMouse();
            A3RuntimeTest.server(s -> {
                prepare(s, test.item, test.creative);
                s.getInventory().offhand.set(0, new ItemStack(test.offhand));
                s.inventoryMenu.broadcastFullState();
                var level = s.serverLevel();
                level.setBlockAndUpdate(POS.below(), Blocks.DIRT.defaultBlockState());
                var state = test.block.defaultBlockState();
                if (test.block instanceof LeavesBlock) state = state.setValue(LeavesBlock.PERSISTENT, true);
                level.setBlockAndUpdate(POS, state);
            });
        }
        if (phase >= 5 && phase < 43) {
            p.setYRot(0);
            p.setXRot(13);
        }
        if (phase == 14) {
            posts = PlayerRendererR3.localPosts;
            KeyMapping.click(mc.options.keyAttack.getKey());
        }
        if (phase >= 14 && phase < 42) mc.options.keyAttack.setDown(true);
        if (phase > 14 && phase < 43) {
            swing |= p.swinging;
            mining |= mc.gameMode.isDestroying();
            special |= clip.startsWith("UAL_sword");
            punch |= clip.startsWith("UAL_punch_");
            if (phase == 20) capture(mc, test.name);
        }
        if (phase == 42) {
            mc.options.keyAttack.setDown(false);
            boolean tagged = WeaponTags.isWeapon(p.getMainHandItem());
            boolean blockedClient = tagged && !swing && !mining && !punch
                    && special == (test.item instanceof net.minecraft.world.item.SwordItem)
                    && PlayerRendererR3.localPosts == posts && mc.level.getBlockState(POS).is(test.block);
            check("client_" + test.name, test.weapon ? blockedClient : !tagged && (swing || mining),
                    "tag=" + tagged + " swing=" + swing + " mining=" + mining + " attackClip=" + special);
        }
        if (phase == 45) A3RuntimeTest.server(s -> {
            var level = s.serverLevel();
            boolean played = !test.weapon && level.getBlockState(POS).isAir();
            if (test.weapon) {
                boolean stayed = level.getBlockState(POS).is(test.block);
                for (Action action : new Action[]{Action.START_DESTROY_BLOCK, Action.STOP_DESTROY_BLOCK})
                    s.gameMode.handleBlockBreakAction(POS, action, Direction.NORTH, level.getMaxBuildHeight(), 0);
                played = stayed && level.getBlockState(POS).is(test.block) && !flag(s.gameMode, "f_9249_")
                        && !flag(s.gameMode, "f_9253_") && !s.gameMode.destroyBlock(POS);
            }
            check("server_" + test.name, played, level.getBlockState(POS));
        });
    }

    private static void entityCase(Minecraft mc, EntityType<?> type, int phase, String clip) {
        var p = mc.player;
        String label = type.getDescriptionId().replace("entity.minecraft.", "");
        if (phase == 0) {
            special = swing = false;
            A3RuntimeTest.server(s -> {
                prepare(s, Items.IRON_SWORD, false);
                CombatController.disable(s);
                try { CombatController.automatic(s); }
                catch (Exception failure) { throw new IllegalStateException(failure); }
                s.serverLevel().setBlockAndUpdate(POS.below(), Blocks.AIR.defaultBlockState());
                var mob = (Mob) type.create(s.serverLevel());
                mob.setNoAi(true);
                mob.setPos(.5, 100, 2.65);
                target = mob.getId();
                initialHealth = mob.getHealth();
                s.serverLevel().addFreshEntity(mob);
            });
        }
        if (phase >= 5 && phase <= 30 && mc.level.getEntity(target) instanceof Mob mob) {
            var aim = mob.getBoundingBox().getCenter().subtract(p.getEyePosition());
            p.setYRot((float) Math.toDegrees(Math.atan2(-aim.x, aim.z)));
            p.setXRot((float) Math.toDegrees(-Math.atan2(aim.y, aim.horizontalDistance())));
        }
        if (phase == 14) {
            posts = PlayerRendererR3.localPosts;
            KeyMapping.click(mc.options.keyAttack.getKey());
        }
        if (phase > 14 && phase < 30) {
            if (!special && clip.startsWith("UAL_sword")) {
                special = true;
                capture(mc, "sword-" + label);
            }
            swing |= PlayerRendererR3.localPosts > posts || clip.startsWith("UAL_punch_");
        }
        if (phase == 35) {
            check("sword_clip_" + label, special && !swing, clip);
            A3RuntimeTest.server(s -> {
                var status = CombatController.status(s.getUUID());
                boolean damaged = s.serverLevel().getEntity(target) instanceof Mob mob && mob.getHealth() < initialHealth;
                check("sword_damage_" + label, damaged && status != null && status.hits() == 1, status);
            });
        }
    }

    private static void prepare(ServerPlayer p, Item item, boolean creative) {
        var level = p.serverLevel();
        p.setGameMode(creative ? GameType.CREATIVE : GameType.SURVIVAL);
        p.teleportTo(level, .5, 100, .5, 0, 0);
        p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        level.setDayTime(18000);
        for (int y=100; y<=105; y++)
            level.setBlockAndUpdate(new BlockPos(0,y,1), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState());
        for (Mob mob : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(12))) mob.discard();
        p.getInventory().selected = 8;
        p.getInventory().setItem(8, new ItemStack(item));
        p.getInventory().offhand.set(0, ItemStack.EMPTY);
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(8));
        p.inventoryMenu.broadcastFullState();
        if (WeaponTags.isWeapon(EffectiveEquipment.selected(p))) WeaponReadiness.toggle(p);
    }

    private static void serverBoundaries(ServerPlayer p) {
        prepare(p, Items.DIAMOND_PICKAXE, false);
        var level = p.serverLevel();
        level.setBlockAndUpdate(POS, Blocks.OBSIDIAN.defaultBlockState());
        p.gameMode.handleBlockBreakAction(POS, Action.START_DESTROY_BLOCK, Direction.NORTH, level.getMaxBuildHeight(), 0);
        check("active_tool_progress", flag(p.gameMode, "f_9249_"), POS);
        p.gameMode.handleBlockBreakAction(POS, Action.STOP_DESTROY_BLOCK, Direction.NORTH, level.getMaxBuildHeight(), 0);
        check("delayed_tool_progress", flag(p.gameMode, "f_9253_"), POS);
        p.getInventory().setItem(8, new ItemStack(Items.IRON_SWORD));
        p.inventoryMenu.broadcastFullState();
        WeaponReadiness.toggle(p);
    }

    private static boolean flag(ServerPlayerGameMode gameMode, String srgName) {
        try { return ObfuscationReflectionHelper.findField(ServerPlayerGameMode.class, srgName).getBoolean(gameMode); }
        catch (IllegalAccessException failure) { throw new IllegalStateException(failure); }
    }

    private static void capture(Minecraft mc, String name) {
        var dir = A3RuntimeTest.DIR.toFile();
        dir.mkdirs();
        Screenshot.grab(dir, name + ".png", mc.getMainRenderTarget(), ignored -> {});
    }

    private static void check(String name, boolean pass, Object detail) {
        LogUtils.getLogger().info("TOE_W001 {} {} {}", name, pass, detail);
    }
}
