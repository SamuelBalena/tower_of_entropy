package com.towerofentropy.client.camera;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import com.towerofentropy.animation.ActionNetwork;
import com.towerofentropy.client.ClientWeaponState;
import com.towerofentropy.combat.CombatDiagnostics;
import com.towerofentropy.combat.WeaponReadiness;
import java.io.File;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT)
public final class C002RuntimeTest {
    private static final boolean ENABLED = Boolean.getBoolean("toe.c002.qa");
    private static final int STEP = 55, Y = 120;
    private static final String[] SCENES = {"open", "wall", "corridor", "corner", "door", "ceiling",
            "stairs", "room", "shoulder", "zoom_clipped", "restore", "combat_corridor", "combat_roll", "air_attack",
            "front", "first_person", "vegetation"};
    private static volatile int age = -1;
    private static boolean opening, overlap;
    private static int title, lastAge = -1;
    private static double earlyRestore, midRestore, rollMin, peak;
    private static Zombie threat;
    private static long legacyNanos, sweepNanos;
    private static int probePairs;

    private static void check(String label, boolean pass, Object detail) {
        LogUtils.getLogger().info("TOE_C002 {} {} {}", label, pass ? "PASS" : "FAIL", detail);
    }

    @SubscribeEvent
    public static void client(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (!opening && mc.screen instanceof TitleScreen && mc.getOverlay() == null && ++title > 45) {
            opening = true;
            mc.options.pauseOnLostFocus = false;
            mc.createWorldOpenFlows().loadLevel(mc.screen, "Global Survival one");
        }
        if (mc.player == null || mc.screen != null || age < 0 || age == lastAge) return;
        lastAge = age;
        int scene = age / STEP, step = age % STEP;
        if (scene >= SCENES.length) { mc.stop(); return; }
        if (scene == 0 && step <= 2) {
            CameraSettings.DISTANCE.set(4.0);
            CameraSettings.RIGHT_SHOULDER.set(true);
        }
        mc.options.setCameraType(scene == 14 ? CameraType.THIRD_PERSON_FRONT
                : scene == 15 ? CameraType.FIRST_PERSON : CameraType.THIRD_PERSON_BACK);
        mc.player.setYRot(scene == 3 ? 18 : 0);
        mc.player.setXRot(scene == 5 || scene == 13 ? 18 : scene == 6 ? 4 : 0);
        if (step == 2) {
            overlap = false;
            rollMin = Double.POSITIVE_INFINITY;
            peak = Y;
            legacyNanos = sweepNanos = 0;
            probePairs = 0;
        }
        if (scene < 14 && step >= 15 && step <= 46) probe(mc, step);
        double distance = distance(mc);
        if (step >= 8 && (scene < 14 || scene == 16)) {
            var center = mc.gameRenderer.getMainCamera().getPosition();
            overlap |= mc.level.getBlockCollisions(mc.player, new AABB(center, center).inflate(.099)).iterator().hasNext();
            rollMin = Math.min(rollMin, distance);
            peak = Math.max(peak, mc.player.getY());
        }
        if (scene == 8 && step == 18) press(mc, "shoulder_left");
        if (scene == 8 && step == 38) press(mc, "shoulder_right");
        if (scene == 8 && step == 36) check("selected_left", CameraSettings.RIGHT_SHOULDER.get() == false
                && mc.gameRenderer.getMainCamera().getPosition().x > mc.player.getX() + .25, distance);
        if (scene == 9 && step == 12) {
            var farther = binding(mc, "farther");
            farther.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_O));
            KeyMapping.resetMapping();
            press(mc, "farther");
        }
        if (scene == 10 && step == 3) earlyRestore = distance;
        if (scene == 10 && step == 6) midRestore = distance;
        if (scene == 12 && step == 16) ActionNetwork.requestCombatRoll(0, 1);
        if (scene == 13 && step == 15) mc.options.keyJump.setDown(true);
        if (scene == 13 && step == 16) ActionNetwork.requestAttack(true);
        if (scene == 13 && step == 17) mc.options.keyJump.setDown(false);
        if (step != 50) return;
        boolean good = switch (scene) {
            case 0 -> Math.abs(distance - 4) < .06;
            case 1, 2, 3, 4, 5, 7 -> !overlap && distance > .08 && distance < 3.9;
            case 6 -> !overlap && distance > .08 && distance < 3.9;
            case 8 -> !overlap && CameraSettings.RIGHT_SHOULDER.get();
            case 9 -> !overlap && distance < 2 && CameraSettings.DISTANCE.get() == 4.5;
            case 10 -> !overlap && earlyRestore > 1.4 && midRestore > earlyRestore + .03 && distance > midRestore && Math.abs(distance - 4.5) < .06;
            case 11 -> !overlap && ClientWeaponState.inCombat() && distance < 3.9;
            case 12 -> !overlap && ClientWeaponState.inCombat() && rollMin < 4.1;
            case 13 -> !overlap && peak > Y + .08;
            case 14 -> distance < -3.9;
            case 15 -> Math.abs(distance) < .04;
            case 16 -> !overlap && Math.abs(distance - 4.5) < .06
                    && mc.level.getBlockState(new BlockPos(0, Y + 1, -2)).is(Blocks.VINE);
            default -> false;
        };
        check(SCENES[scene], good, "distance=" + distance + " overlap=" + overlap + " early=" + earlyRestore + " mid=" + midRestore + " peak=" + peak);
        if (probePairs > 0) LogUtils.getLogger().info("TOE_C002_PROBE {} n={} legacyUs={} sweepUs={}", SCENES[scene],
                probePairs, legacyNanos / (1000.0 * probePairs), sweepNanos / (1000.0 * probePairs));
        Screenshot.grab(new File("."), "c002-" + SCENES[scene] + ".png", mc.getMainRenderTarget(), ignored -> {});
    }

    private static void checkSweep() {
        var sweep = new CameraSweep();
        sweep.reset(0, 0, 0, 0, 0, 4, .1, .005);
        sweep.include(-.02, -.02, 1, .02, .02, 2);
        check("shape_between_corner_rays", Math.abs(sweep.fraction() - .22) < 1e-8, sweep.fraction());
        sweep.reset(.04, 0, 0, 1, 0, 1, .1, .01);
        sweep.include(-1, -1, -1, 0, 1, 1);
        check("can_leave_existing_margin", sweep.fraction() == 1, sweep.fraction());
        double whole = CameraDamping.approach(.2, 1, .20, .09);
        double part = CameraDamping.approach(.2, 1, .05, .09);
        check("frame_rate_independent_return", Math.abs(whole - CameraDamping.approach(part, 1, .15, .09)) < 1e-12, whole);
    }

    private static void probe(Minecraft mc, int step) {
        Vec3 eye = mc.player.getEyePosition();
        Vec3 forward = mc.player.getLookAngle();
        Vec3 offset = forward.scale(-CameraSettings.DISTANCE.get()).add(-.9775, .25, 0);
        // Alternate order so the second algorithm does not always benefit from the same warm caches.
        if (step % 2 == 0) {
            timeLegacy(mc, eye, offset);
            timeSweep(mc, eye, offset);
        } else {
            timeSweep(mc, eye, offset);
            timeLegacy(mc, eye, offset);
        }
        probePairs++;
    }

    private static void timeSweep(Minecraft mc, Vec3 eye, Vec3 offset) {
        long start = System.nanoTime();
        ShoulderCamera.collisionFraction(mc, eye, offset);
        sweepNanos += System.nanoTime() - start;
    }

    private static void timeLegacy(Minecraft mc, Vec3 eye, Vec3 offset) {
        long start = System.nanoTime();
        double limit = 1;
        double length = offset.length();
        for (int i = 0; i < 8; i++) {
            Vec3 from = eye.add(corner(i, 1), corner(i, 2), corner(i, 4));
            var hit = mc.level.clip(new ClipContext(from, from.add(offset), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, mc.player));
            if (hit.getType() != HitResult.Type.MISS)
                limit = Math.min(limit, Math.max(0, (from.distanceTo(hit.getLocation()) - .02) / length));
        }
        if (!Double.isFinite(limit)) throw new IllegalStateException("Invalid legacy camera fraction");
        legacyNanos += System.nanoTime() - start;
    }

    private static double corner(int i, int axis) { return (i & axis) == 0 ? -.1 : .1; }

    private static double distance(Minecraft mc) {
        return mc.player.getEyePosition().subtract(mc.gameRenderer.getMainCamera().getPosition()).dot(mc.player.getLookAngle());
    }

    private static KeyMapping binding(Minecraft mc, String action) {
        return java.util.Arrays.stream(mc.options.keyMappings)
                .filter(key -> key.getName().equals("key.towerofentropy.camera." + action)).findFirst().orElseThrow();
    }

    private static void press(Minecraft mc, String action) { KeyMapping.click(binding(mc, action).getKey()); }

    @SubscribeEvent
    public static void server(TickEvent.ServerTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.START || event.getServer().getPlayerList().getPlayers().isEmpty()) return;
        ServerPlayer p = event.getServer().getPlayerList().getPlayers().get(0);
        int time = age + 1, scene = time / STEP, step = time % STEP;
        if (scene < SCENES.length && step == 0) setup(p, scene);
        if (time == 1) {
            checkSweep();
            check("diagnostic_dev_default", CombatDiagnostics.of(p) != null, "");
        }
        if (time == 5) command(p, "off");
        if (time == 8) check("diagnostic_stays_off", CombatDiagnostics.of(p) == null, "");
        if (time == 10) command(p, "on");
        if (time == 14) check("diagnostic_reenabled", CombatDiagnostics.of(p) != null, "");
        if (scene == 12 && step == 45) {
            var roll = com.towerofentropy.combat.RollController.status(p.getUUID());
            check("roll_authorized", roll != null && roll.started() && roll.reason().equals("complete"), roll);
        }
        if (scene == 13 && step == 45) {
            var attack = com.towerofentropy.combat.CombatController.status(p.getUUID());
            check("heavy_requested", attack != null && attack.impacts() > 0, attack);
        }
        age = time;
    }

    private static void command(ServerPlayer p, String state) {
        p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(),
                "toe_combat diagnostics " + state);
    }

    private static void setup(ServerPlayer p, int scene) {
        var level = p.serverLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.setInvulnerable(true);
        if (scene != 12) p.teleportTo(level, .5, Y, .5, scene == 3 ? 18 : 0, scene == 5 || scene == 13 ? 18 : 0);
        if (scene == 12) return;
        if (scene != 11 && threat != null) { threat.discard(); threat = null; }
        for (int x = -3; x <= 3; x++) for (int z = -6; z <= 4; z++) {
            level.setBlockAndUpdate(new BlockPos(x, Y - 1, z), Blocks.STONE.defaultBlockState());
            for (int y = Y; y <= Y + 4; y++) level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
        }
        boolean corridor = scene == 2 || scene == 11;
        if (corridor || scene == 8) for (int z = -5; z <= 4; z++) for (int y = 0; y < 3; y++) {
            stone(p, -1, y, z);
            if (corridor) stone(p, 1, y, z);
        }
        if (scene == 1 || scene == 7 || scene == 9) {
            for (int x = -3; x <= 3; x++) for (int y = 0; y < 3; y++) stone(p, x, y, -2);
        }
        if (scene == 3) for (int y = 0; y < 3; y++) {
            stone(p, 0, y, -2);
            stone(p, -1, y, -3);
        }
        if (scene == 4) for (var half : DoubleBlockHalf.values()) {
            level.setBlockAndUpdate(new BlockPos(0, Y + (half == DoubleBlockHalf.UPPER ? 1 : 0), -2),
                    Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, half).setValue(DoorBlock.FACING, Direction.SOUTH));
        }
        if (scene == 5 || scene == 7 || scene == 13) for (int x = -3; x <= 3; x++) for (int z = -5; z <= 2; z++)
            stone(p, x, scene == 13 ? 3 : 2, z);
        if (scene == 6) for (int x = -2; x <= 2; x++)
            level.setBlockAndUpdate(new BlockPos(x, Y + 1, -2),
                    Blocks.STONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
        if (scene == 16) for (int x = -2; x <= 2; x++) {
            stone(p, x, 2, -2);
            level.setBlockAndUpdate(new BlockPos(x, Y + 1, -2),
                    Blocks.VINE.defaultBlockState().setValue(net.minecraft.world.level.block.VineBlock.UP, true));
        }
        if (scene == 11) {
            p.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
            p.getInventory().selected = 0;
            p.inventoryMenu.broadcastFullState();
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
            WeaponReadiness.toggle(p);
            threat = EntityType.ZOMBIE.create(level);
            threat.setNoAi(true);
            threat.setPos(.5, Y, 3.5);
            level.addFreshEntity(threat);
            threat.setTarget(p);
        }
    }

    private static void stone(ServerPlayer p, int x, int y, int z) {
        p.level().setBlockAndUpdate(new BlockPos(x, Y + y, z), Blocks.STONE.defaultBlockState());
    }
}
