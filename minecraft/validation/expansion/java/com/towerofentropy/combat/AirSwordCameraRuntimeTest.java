package com.towerofentropy.combat;

import com.mojang.logging.LogUtils;
import com.towerofentropy.client.PlayerRendererR3;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT)
public final class AirSwordCameraRuntimeTest {
    static final boolean ENABLED = Boolean.getBoolean("toe.w0011.qa");
    static final int START = 3600, STEP = 90;
    private static final BlockPos FRONT = new BlockPos(0, 101, 2);
    private record Scenario(String name, Block block, boolean target, boolean air, boolean sprint, boolean damage) {}
    private static final Scenario[] CASES = {
        new Scenario("dirt", Blocks.DIRT, false, false, false, false),
        new Scenario("air_targetless", Blocks.AIR, false, false, false, false),
        new Scenario("entity_target", Blocks.AIR, true, false, false, true),
        new Scenario("grass_target", Blocks.GRASS, true, false, false, true),
        new Scenario("flower_target", Blocks.POPPY, true, false, false, true),
        new Scenario("solid_target", Blocks.STONE, true, false, false, false),
        new Scenario("jump_target", Blocks.AIR, true, true, false, true),
        new Scenario("sprint_jump_target", Blocks.AIR, true, true, true, true),
        new Scenario("combo", Blocks.AIR, false, false, false, false)
    };
    private static volatile int target = -1;
    private static volatile boolean airborneOnServer, motionDuringAir;
    private static double apex, minDistance, maxDistance, renderedDistance, renderedFov;
    private static boolean changedFov;
    private static boolean overlap, mining, sawAirClip, sawHorizontalSpeed;
    private static long posts, draws;
    private static final Set<String> clips = new HashSet<>();

    static void restart(Minecraft mc) {
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        boolean slim = Boolean.getBoolean("toe.qa.slim");
        check("restart_bindings", binding(mc,"shoulder_left").getKey().getValue()==GLFW.GLFW_KEY_LEFT
                && binding(mc,"shoulder_right").getKey().getValue()==GLFW.GLFW_KEY_RIGHT
                && (slim ? binding(mc,"shoulder").getKey().getValue()==GLFW.GLFW_KEY_K : binding(mc,"shoulder").isUnbound()),
                binding(mc,"shoulder").getKey());
        try {
            var toml = Files.readString(Path.of("config","towerofentropy-camera-client.toml"));
            check("restart_shoulder", toml.contains("rightShoulder = false"), toml.contains("rightShoulder = false"));
        } catch (java.io.IOException failure) { check("restart_shoulder", false, failure); }
        mc.stop();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderedCamera(ViewportEvent.ComputeCameraAngles event) {
        var p = Minecraft.getInstance().player;
        if (!ENABLED || p == null) return;
        float partial = (float) event.getPartialTick();
        renderedDistance = p.getEyePosition(partial).subtract(event.getCamera().getPosition())
                .dot(p.getViewVector(partial));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderedFov(ViewportEvent.ComputeFov event) {
        if (ENABLED && event.usedConfiguredFov()) renderedFov = event.getFOV();
    }

    static boolean tick(Minecraft mc, int age, String clip) {
        if (age < START) return false;
        int step = age - START;
        int index = step / STEP;
        if (index >= CASES.length) return camera(mc, step - CASES.length * STEP);
        combat(mc, CASES[index], step % STEP, clip);
        return false;
    }

    private static void combat(Minecraft mc, Scenario test, int phase, String clip) {
        var p = mc.player;
        if (phase == 0) {
            sawAirClip = overlap = mining = sawHorizontalSpeed = changedFov = false;
            airborneOnServer = motionDuringAir = false;
            apex = 100;
            minDistance = 100;
            maxDistance = 0;
            clips.clear();
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.mouseHandler.grabMouse();
            A3RuntimeTest.server(s -> {
                var level = s.serverLevel();
                s.setGameMode(GameType.SURVIVAL);
                s.teleportTo(level, .5, 100, .5, 0, 0);
                s.setDeltaMovement(Vec3.ZERO);
                s.setSprinting(false);
                s.getFoodData().setFoodLevel(20);
                for (int x = -2; x <= 2; x++)
                    for (int z = -10; z <= 15; z++) {
                        level.setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState());
                        for (int y = 100; y <= 106; y++)
                            level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                    }
                for (Mob mob : level.getEntitiesOfClass(Mob.class, s.getBoundingBox().inflate(16))) mob.discard();
                s.getInventory().selected = 8;
                s.getInventory().setItem(8, new ItemStack(Items.IRON_SWORD));
                s.getInventory().offhand.set(0, ItemStack.EMPTY);
                s.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(8));
                s.inventoryMenu.broadcastFullState();
                CombatController.disable(s);
                try { CombatController.automatic(s); }
                catch (Exception failure) { throw new IllegalStateException(failure); }
                WeaponReadiness.toggle(s);
                if (test.block != Blocks.AIR) {
                    var plant = test.block == Blocks.POPPY || test.block == Blocks.GRASS;
                    level.setBlockAndUpdate(FRONT.below(), (plant ? Blocks.DIRT : test.block).defaultBlockState());
                    level.setBlockAndUpdate(FRONT, test.block.defaultBlockState());
                    if (test.block == Blocks.STONE) level.setBlockAndUpdate(FRONT.above(), Blocks.STONE.defaultBlockState());
                }
                if (test.target) {
                    var mob = EntityType.ZOMBIE.create(level);
                    mob.setNoAi(true);
                    mob.setPos(.5, 100, test.sprint ? 6.5 : test.air ? 2.4 : 3.18);
                    level.addFreshEntity(mob);
                    target = mob.getId();
                } else target = -1;
            });
        }
        if (phase >= 7 && phase <= 50) {
            p.setYRot(0);
            if (test.air && mc.level.getEntity(target) instanceof Mob mob) {
                Vec3 toward = mob.getBoundingBox().getCenter().subtract(p.getEyePosition());
                p.setYRot((float)Math.toDegrees(Math.atan2(-toward.x, toward.z)));
                p.setXRot((float)Math.toDegrees(-Math.atan2(toward.y, toward.horizontalDistance())));
            } else p.setXRot(test.block == Blocks.AIR ? 0 : 13);
        }
        if (test.sprint) {
            mc.options.keySprint.setDown(phase >= 9 && phase < 29);
            mc.options.keyUp.setDown(phase >= 9 && phase < 29);
        }
        if (test.air) mc.options.keyJump.setDown(phase >= 19 && phase < 21);
        int attack = test.air ? 21 : 19;
        if (phase == attack) {
            posts = PlayerRendererR3.localPosts;
            draws = PlayerRendererR3.drawCalls;
            KeyMapping.click(mc.options.keyAttack.getKey());
        }
        if (test.name.equals("combo") && (phase == 23 || phase == 27)) KeyMapping.click(mc.options.keyAttack.getKey());
        if (test.block != Blocks.AIR && !test.target) mc.options.keyAttack.setDown(phase >= attack && phase < 40);
        if (phase > attack && phase < 55) {
            boolean drew = PlayerRendererR3.drawCalls > draws;
            boolean landed = p.onGround();
            overlap |= PlayerRendererR3.localPosts > posts || drew && clip.startsWith("UAL_punch_") || p.swinging;
            mining |= mc.gameMode.isDestroying();
            if (drew && clip.startsWith("UAL_sword") && clips.add(clip) && clips.size() == 1)
                capture(mc, test.name);
            sawAirClip |= !landed && drew && clip.startsWith("UAL_sword");
            sawHorizontalSpeed |= !landed && clip.startsWith("UAL_sword")
                    && p.position().subtract(p.xo, p.yo, p.zo).horizontalDistance() > .18;
            apex = Math.max(apex, p.getY());
            if (test.air && phase <= attack + 7) A3RuntimeTest.server(s -> {
                var status = CombatController.status(s.getUUID());
                if (status != null && status.attacking() && !s.onGround()) {
                    airborneOnServer = true;
                    motionDuringAir |= AttackMovement.committed(s.getUUID());
                }
            });
        }
        if (!test.air && phase >= attack - 5 && phase < 55) {
            double radius = renderedDistance;
            changedFov |= Math.abs(renderedFov - mc.options.fov().get()) > .1;
            minDistance = Math.min(minDistance, radius);
            maxDistance = Math.max(maxDistance, radius);
        }
        if (phase == 65) {
            check("visual_" + test.name, !clips.isEmpty() && !overlap && !mining
                    && (!test.air || sawAirClip && apex > 102.3 && apex < 102.7 && (!test.sprint || sawHorizontalSpeed)),
                    clips + " overlap=" + overlap + " mining=" + mining + " apex=" + apex);
            if (!test.air) check("camera_" + test.name, minDistance > 3.90 && maxDistance < 4.10 && !changedFov,
                    minDistance + " .. " + maxDistance + " fovChanged=" + changedFov);
            if (test.name.equals("combo")) check("combo_abc", clips.containsAll(Set.of("UAL_sword","UAL_sword_b","UAL_sword_c")), clips);
            A3RuntimeTest.server(s -> {
                var status = CombatController.status(s.getUUID());
                boolean blockIntact = test.block == Blocks.AIR || s.serverLevel().getBlockState(FRONT).is(test.block);
                boolean expectedHits = status != null && status.hits() == (test.damage ? 1 : 0) && status.impacts() > 0;
                check("server_" + test.name, blockIntact && expectedHits && (!test.air || airborneOnServer && !motionDuringAir),
                        status + " intact=" + blockIntact + " airborne=" + airborneOnServer + " lunge=" + motionDuringAir);
            });
        }
    }

    private static boolean camera(Minecraft mc, int t) {
        var p = mc.player;
        if (t == 0) {
            A3RuntimeTest.server(s -> s.teleportTo(s.serverLevel(), .5, 100, .5, 0, 0));
            p.setXRot(0);
            p.setYRot(0);
        }
        if (t == 14) {
            boolean slim = Boolean.getBoolean("toe.qa.slim");
            check("migrated_b", slim ? binding(mc,"shoulder").getKey().getValue()==GLFW.GLFW_KEY_K : binding(mc,"shoulder").isUnbound(), binding(mc,"shoulder").getKey());
            check("directional_defaults", binding(mc,"shoulder_left").getDefaultKey().getValue()==GLFW.GLFW_KEY_LEFT
                    && binding(mc,"shoulder_right").getDefaultKey().getValue()==GLFW.GLFW_KEY_RIGHT, "left/right");
            press(mc,"shoulder_left");
        }
        if (t == 35) press(mc,"shoulder_left");
        if (t == 54) {
            check("left_is_direct", lateral(mc) > .9, lateral(mc));
            press(mc,"shoulder_right");
            press(mc,"closer");
        }
        if (t == 76) {
            check("right_and_zoom_in", lateral(mc) < -.9 && Math.abs(distance(mc)-3.5)<.035, distance(mc));
            press(mc,"shoulder_right");
            press(mc,"farther");
        }
        if (t == 98) {
            check("right_is_direct_and_zoom_out", lateral(mc) < -.9 && Math.abs(distance(mc)-4)<.035, distance(mc));
            KeyMapping.click(mc.options.keyTogglePerspective.getKey());
        }
        if (t == 108) {
            check("front", mc.options.getCameraType()==CameraType.THIRD_PERSON_FRONT && distance(mc)<-3.9, distance(mc));
            KeyMapping.click(mc.options.keyTogglePerspective.getKey());
        }
        if (t == 118) {
            check("first_person", mc.options.getCameraType()==CameraType.FIRST_PERSON && Math.abs(distance(mc))<.04, distance(mc));
            KeyMapping.click(mc.options.keyTogglePerspective.getKey());
            A3RuntimeTest.server(s -> {
                for (int x=-2; x<=2; x++) for (int y=100; y<=104; y++)
                    s.serverLevel().setBlockAndUpdate(new BlockPos(x,y,-2),Blocks.STONE.defaultBlockState());
            });
        }
        if (t == 145) {
            check("real_wall_clips", distance(mc) < 1.6 && distance(mc) > .6, distance(mc));
            capture(mc,"camera-wall");
            A3RuntimeTest.server(s -> {
                for (int x=-2; x<=2; x++) for (int y=100; y<=104; y++)
                    s.serverLevel().setBlockAndUpdate(new BlockPos(x,y,-2),Blocks.AIR.defaultBlockState());
            });
        }
        if (t == 175) {
            check("wall_recovery", Math.abs(distance(mc)-4)<.035, distance(mc));
            press(mc,"shoulder_left");
        }
        if (t == 199) {
            try {
                var toml = Files.readString(Path.of("config","towerofentropy-camera-client.toml"));
                check("shoulder_on_disk", toml.contains("rightShoulder = false") && toml.contains("shoulderBindingVersion = 1"), toml.contains("rightShoulder = false"));
            } catch (java.io.IOException failure) { check("shoulder_on_disk", false, failure); }
            capture(mc,"camera-left");
            return true;
        }
        return false;
    }

    private static double distance(Minecraft mc) {
        var player = mc.player;
        return player.getEyePosition().subtract(mc.gameRenderer.getMainCamera().getPosition())
                .dot(player.getLookAngle());
    }

    private static double lateral(Minecraft mc) {
        return mc.gameRenderer.getMainCamera().getPosition().x - mc.player.getX();
    }

    private static KeyMapping binding(Minecraft mc, String name) {
        return java.util.Arrays.stream(mc.options.keyMappings).filter(key ->
                key.getName().equals("key.towerofentropy.camera."+name)).findFirst().orElseThrow();
    }

    private static void press(Minecraft mc, String name) { KeyMapping.click(binding(mc,name).getKey()); }

    private static void capture(Minecraft mc,String name) {
        var dir=A3RuntimeTest.DIR.toFile();
        dir.mkdirs();
        Screenshot.grab(dir,name+".png",mc.getMainRenderTarget(),ignored -> {});
    }

    private static void check(String name,boolean pass,Object detail) {
        LogUtils.getLogger().info("TOE_W0011 {} {} {}",name,pass,detail);
    }
}
