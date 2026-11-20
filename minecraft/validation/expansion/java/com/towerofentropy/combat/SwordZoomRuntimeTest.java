package com.towerofentropy.combat;

import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.platform.InputConstants;
import com.towerofentropy.client.PlayerRendererR3;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

final class SwordZoomRuntimeTest {
    static final int START = 3880, PHASE = 40;
    private static final BlockPos TARGET = new BlockPos(0, 101, 2);
    private static final Block[] BLOCKS = {Blocks.TALL_GRASS, Blocks.GRASS, Blocks.POPPY, Blocks.BROWN_MUSHROOM,
            Blocks.FERN, Blocks.DEAD_BUSH, Blocks.OAK_LEAVES, Blocks.OBSIDIAN};
    private static long draws, posts;
    private static int entityId = -1, beforeAccepted;
    private static float initialHealth;
    private static boolean sawSword, overlapped, digging, wheelSent;
    private static int hotbar;
    private static double position;

    static boolean tick(Minecraft mc, int age, String clip) {
        if (age < START) return false;
        int step = age - START;
        if (step < (BLOCKS.length + 2) * PHASE) {
            sword(mc, step / PHASE, step % PHASE, clip);
            return false;
        }
        return zoom(mc, step - (BLOCKS.length + 2) * PHASE);
    }

    private static void sword(Minecraft mc, int index, int phase, String clip) {
        boolean block = index < BLOCKS.length;
        String label = block ? BLOCKS[index].getDescriptionId().replace("block.minecraft.", "") : index == 8 ? "cow" : "zombie";
        if (phase == 0) {
            sawSword = overlapped = digging = false;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.options.hideGui = true;
            A3RuntimeTest.server(server -> {
                var level = server.serverLevel();
                for (var mob : level.getEntitiesOfClass(Mob.class, server.getBoundingBox().inflate(12))) mob.discard();
                var inventory = server.getInventory();
                inventory.selected = 8;
                inventory.setItem(8, new ItemStack(Items.IRON_SWORD));
                inventory.offhand.set(0, ItemStack.EMPTY);
                server.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(8));
                server.inventoryMenu.broadcastFullState();
                server.teleportTo(level, .5, 100, .5, 0, 0);
                server.setDeltaMovement(Vec3.ZERO);
                level.setBlockAndUpdate(TARGET.north(), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(TARGET.above(), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(TARGET, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(TARGET.below(),
                        (block ? index == 5 ? Blocks.SAND : Blocks.DIRT : Blocks.AIR).defaultBlockState());
                if (block) {
                    var type = BLOCKS[index];
                    var state = type.defaultBlockState();
                    if (type instanceof LeavesBlock) state = state.setValue(LeavesBlock.PERSISTENT, true);
                    level.setBlockAndUpdate(TARGET, state);
                    if (type instanceof DoublePlantBlock)
                        level.setBlockAndUpdate(TARGET.above(), state.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
                } else {
                    CombatController.disable(server);
                    try { CombatController.automatic(server); }
                    catch (Exception failure) { throw new IllegalStateException(failure); }
                    level.setDayTime(18000);
                    var mob = (index == 8 ? EntityType.COW : EntityType.ZOMBIE).create(level);
                    mob.setNoAi(true);
                    mob.setPos(.5, 100, 2.65);
                    entityId = mob.getId();
                    initialHealth = mob.getHealth();
                    level.addFreshEntity(mob);
                }
            });
        }
        var p = mc.player;
        if (phase >= 5 && phase <= 28) {
            p.setYRot(0);
            if (block) p.setXRot(13);
            else if (mc.level.getEntity(entityId) instanceof Mob mob) {
                Vec3 aim = mob.getBoundingBox().getCenter().subtract(p.getEyePosition());
                p.setYRot((float) Math.toDegrees(Math.atan2(-aim.x, aim.z)));
                p.setXRot((float) Math.toDegrees(-Math.atan2(aim.y, aim.horizontalDistance())));
            }
        }
        if (phase == 12) {
            A3RuntimeTest.server(server -> {
                var status = CombatController.status(server.getUUID());
                beforeAccepted = status == null ? 0 : status.accepted();
            });
            KeyMapping.click(mc.options.keyAttack.getKey());
        }
        if (block && index >= 6 && phase >= 12 && phase < 29) mc.options.keyAttack.setDown(true);
        if (phase == 29) mc.options.keyAttack.setDown(false);
        if (phase > 12 && phase <= 28) {
            boolean r3 = PlayerRendererR3.drawCalls > draws;
            overlapped |= PlayerRendererR3.localPosts > posts || r3 && clip.startsWith("UAL_punch_");
            digging |= mc.gameMode.isDestroying();
            if (r3 && clip.startsWith("UAL_sword") && !sawSword) {
                sawSword = true;
                capture(mc, "sword-" + label);
            }
        }
        draws = PlayerRendererR3.drawCalls;
        posts = PlayerRendererR3.localPosts;
        if (phase == 34) {
            check("sword_" + label, sawSword && !overlapped && (index != 7 || digging),
                    "sword=" + sawSword + " overlapping=" + overlapped + " digging=" + digging);
            A3RuntimeTest.server(server -> {
                var status = CombatController.status(server.getUUID());
                if (block) {
                    var state = server.serverLevel().getBlockState(TARGET);
                    check("block_rules_" + label, status != null && status.accepted() == beforeAccepted
                            && (index == 7 ? state.is(Blocks.OBSIDIAN) : state.isAir()), state + " " + status);
                } else if (server.serverLevel().getEntity(entityId) instanceof Mob mob) {
                    check("entity_rules_" + label, mob.getHealth() < initialHealth
                            && status != null && status.accepted() > beforeAccepted, "health=" + mob.getHealth() + " " + status);
                } else check("entity_rules_" + label, false, "missing target");
            });
        }
    }

    private static boolean zoom(Minecraft mc, int t) {
        if (t == 0) {
            mc.options.hideGui = false;
            A3RuntimeTest.server(server -> {
                if (server.serverLevel().getEntity(entityId) instanceof Mob old) old.discard();
                for (int x = -2; x <= 2; x++)
                    for (int z = -9; z <= 3; z++)
                        for (int y = 100; y <= 105; y++)
                            server.serverLevel().setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                server.teleportTo(server.serverLevel(), .5, 100, .5, 0, 0);
            });
        }
        if (t > 0) {
            mc.player.setXRot(0);
            mc.player.setYRot(0);
        }
        var camera = mc.gameRenderer.getMainCamera();
        double distance = mc.player.getZ() - camera.getPosition().z;
        if (t == 28) {
            int expectedKey = Boolean.getBoolean("toe.qa.slim") ? GLFW.GLFW_KEY_K : GLFW.GLFW_KEY_DOWN;
            check("binding_migration", binding(mc, "closer").getKey().getValue() == GLFW.GLFW_KEY_UP
                    && binding(mc, "farther").getKey().getValue() == expectedKey
                    && binding(mc, "farther").getDefaultKey().getValue() == GLFW.GLFW_KEY_DOWN,
                    binding(mc, "closer").getKey() + " " + binding(mc, "farther").getKey());
            check("no_default_conflicts", java.util.Arrays.stream(mc.options.keyMappings)
                    .filter(k -> !k.getName().startsWith("key.towerofentropy.camera."))
                    .noneMatch(k -> k.getKey().getType() == InputConstants.Type.KEYSYM
                            && (k.getKey().getValue() == GLFW.GLFW_KEY_UP || k.getKey().getValue() == GLFW.GLFW_KEY_DOWN)), "up/down");
            check("old_preferences_preserved", Math.abs(distance - 5) < .035, distance);
            position = distance;
            tap(mc, "closer", 1);
        }
        if (t == 31) check("approach_interpolates", distance < position && distance > 4.5, distance);
        if (t == 49) {
            check("up_reduces_on_right", Math.abs(distance - 4.5) < .035 && camera.getPosition().x < mc.player.getX(), distance);
            position = distance;
            tap(mc, "farther", 1);
        }
        if (t == 52) check("retreat_interpolates", distance > position && distance < 5, distance);
        if (t == 70) {
            check("down_increases_on_right", Math.abs(distance - 5) < .035, distance);
            tap(mc, "shoulder_left", 1);
        }
        if (t == 95) hold(mc, "closer", true);
        if (t >= 96 && t <= 122) tap(mc, "closer", 4); // OS repeat noise must not inflate the repeat rate.
        if (t == 100) check("repeat_has_initial_delay", savedDistance() == 4.5, savedDistance());
        if (t == 124) hold(mc, "closer", false);
        if (t == 132) {
            check("held_up_reaches_min_on_left", Math.abs(distance - 2) < .035
                    && camera.getPosition().x > mc.player.getX(), distance);
            hold(mc, "farther", true);
        }
        if (t == 186) hold(mc, "farther", false);
        if (t == 205) {
            check("held_down_reaches_max", Math.abs(distance - 8) < .035, distance);
            tap(mc, "closer", 8);
        }
        if (t == 232) {
            check("center_zoom_at_four", Math.abs(distance - 4) < .035, distance);
            hotbar = mc.player.getInventory().selected;
            wheelSent = wheel(mc, true, -1);
        }
        if (t == 245) {
            check("alt_wheel_up", wheelSent && savedDistance() == 3.5 && mc.player.getInventory().selected == hotbar, savedDistance());
            wheelSent = wheel(mc, true, 1);
        }
        if (t == 258) {
            check("alt_wheel_down", wheelSent && savedDistance() == 4 && mc.player.getInventory().selected == hotbar, savedDistance());
            wheelSent = wheel(mc, false, -1);
        }
        if (t == 271) {
            check("plain_wheel_hotbar", wheelSent && savedDistance() == 4 && mc.player.getInventory().selected != hotbar,
                    mc.player.getInventory().selected);
            tap(mc, "farther", 3);
        }
        if (t == 300) {
            check("save_zoom_5_5", Math.abs(distance - 5.5) < .035 && savedDistance() == 5.5, distance);
            capture(mc, "zoom-5-before-restart");
            return true;
        }
        return false;
    }

    static void restart(Minecraft mc) {
        double actual = mc.player.getZ() - mc.gameRenderer.getMainCamera().getPosition().z;
        check("restart_persists_zoom", Math.abs(actual - 5.5) < .04 && savedDistance() == 5.5, actual);
        check("restart_persists_rebinding", binding(mc, "closer").getKey().getValue() == GLFW.GLFW_KEY_UP
                && binding(mc, "farther").getKey().getValue() == (Boolean.getBoolean("toe.qa.slim") ? GLFW.GLFW_KEY_K : GLFW.GLFW_KEY_DOWN),
                binding(mc, "farther").getKey());
        mc.stop();
    }

    private static boolean wheel(Minecraft mc, boolean alt, int direction) {
        try {
            if (alt) {
                var method = com.towerofentropy.client.camera.CameraInput.InGame.class.getDeclaredMethod(
                        "handleScroll", Minecraft.class, boolean.class, double.class);
                method.setAccessible(true);
                return (boolean) method.invoke(null, mc, true, (double) -direction);
            }
            var method = net.minecraft.client.MouseHandler.class.getDeclaredMethod("onScroll",
                    long.class, double.class, double.class);
            method.setAccessible(true);
            method.invoke(mc.mouseHandler, mc.getWindow().getWindow(), 0.0, (double) -direction);
            return true;
        } catch (ReflectiveOperationException failure) {
            LogUtils.getLogger().warn("C001.2 wheel probe", failure);
            return false;
        }
    }

    private static double savedDistance() {
        try {
            return Files.readAllLines(Path.of("config", "towerofentropy-camera-client.toml")).stream()
                    .filter(line -> line.trim().startsWith("distance ="))
                    .mapToDouble(line -> Double.parseDouble(line.split("=")[1].trim())).findFirst().orElse(Double.NaN);
        } catch (java.io.IOException failure) {
            return Double.NaN;
        }
    }

    private static KeyMapping binding(Minecraft mc, String suffix) {
        return java.util.Arrays.stream(mc.options.keyMappings)
                .filter(mapping -> mapping.getName().equals("key.towerofentropy.camera." + suffix)).findFirst().orElseThrow();
    }

    private static void tap(Minecraft mc, String suffix, int count) {
        var key = binding(mc, suffix).getKey();
        for (int i = 0; i < count; i++) KeyMapping.click(key);
    }

    private static void hold(Minecraft mc, String suffix, boolean down) {
        KeyMapping.set(binding(mc, suffix).getKey(), down);
    }

    private static void capture(Minecraft mc, String name) {
        var directory = A3RuntimeTest.DIR.toFile();
        directory.mkdirs();
        Screenshot.grab(directory, name + ".png", mc.getMainRenderTarget(), message -> {});
    }

    private static void check(String test, boolean pass, Object detail) {
        LogUtils.getLogger().info("TOE_C0012 {} {} {}", test, pass, detail);
    }
}
