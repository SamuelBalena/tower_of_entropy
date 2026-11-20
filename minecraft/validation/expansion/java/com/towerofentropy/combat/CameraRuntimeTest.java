package com.towerofentropy.combat;

import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

final class CameraRuntimeTest {
    static final boolean ENABLED = Boolean.getBoolean("toe.c001.qa");
    static final int END = 2895;
    private static final BlockPos AIM_WALL = new BlockPos(0, 101, 4);
    private static int firstWidth, firstHeight;
    private static boolean intermediate;
    private static double rightSide;

    static void tick(Minecraft mc, int age) {
        var player = mc.player;
        var camera = mc.gameRenderer.getMainCamera();
        if (age == 2662) {
            mc.options.hideGui = false;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            A3RuntimeTest.server(server -> {
                var level = server.serverLevel();
                for (int x = -2; x <= 2; x++)
                    for (int z = -10; z <= 10; z++)
                        for (int y = 100; y <= 105; y++)
                            level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                server.teleportTo(level, .5, 100, .5, 0, 0);
            });
        }
        if (age > 2662) {
            player.setYRot(0);
            player.setXRot(0);
        }
        double lateral = camera.getPosition().x - player.getX();
        double distance = player.getZ() - camera.getPosition().z;
        if (age == 2690) {
            rightSide = lateral;
            check("rear_default", Math.abs(distance - 4) < .03 && rightSide < -.70
                    && camera.getPosition().y > player.getEyeY(), camera.getPosition());
            firstWidth = mc.getWindow().getWidth();
            firstHeight = mc.getWindow().getHeight();
            crosshair(mc, "right-default", true);
            KeyMapping.click(mc.options.keyTogglePerspective.getKey());
        }
        if (age == 2695) {
            check("front_vanilla", mc.options.getCameraType() == CameraType.THIRD_PERSON_FRONT
                    && distance < -3.9 && Math.abs(lateral) < .03, camera.getPosition());
            crosshair(mc, "front-vanilla", false);
            KeyMapping.click(mc.options.keyTogglePerspective.getKey());
        }
        if (age == 2700) {
            check("first_person_vanilla", mc.options.getCameraType() == CameraType.FIRST_PERSON
                    && Math.abs(distance) < .03 && Math.abs(lateral) < .03, camera.getPosition());
            crosshair(mc, "first-person-vanilla", false);
            KeyMapping.click(mc.options.keyTogglePerspective.getKey());
        }
        if (age == 2708) {
            check("right_shoulder_offset", Math.abs(lateral + com.towerofentropy.client.camera.CameraSettings.DEFAULT_SHOULDER) < .03, lateral);
            check("f5_returns_to_shoulder", mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK
                    && lateral < -.70, camera.getPosition());
            press(mc, "shoulder_left", 1);
        }
        if (age > 2708 && age < 2718) intermediate |= Math.abs(lateral) < Math.abs(rightSide) - .05;
        if (age == 2730) {
            check("left_shoulder_offset", Math.abs(lateral - com.towerofentropy.client.camera.CameraSettings.DEFAULT_SHOULDER) < .03, lateral);
            check("smooth_shoulder_swap", intermediate && lateral > .70, camera.getPosition());
            crosshair(mc, "left-default", true);
            press(mc, "closer", 30);
        }
        if (age == 2750) {
            check("min_distance", Math.abs(distance - 2) < .03, distance);
            press(mc, "farther", 30);
        }
        if (age == 2770) {
            check("max_distance", Math.abs(distance - 8) < .03, distance);
            try {
                var path = java.nio.file.Path.of("config", "towerofentropy-camera-client.toml");
                var content = Files.readString(path);
                check("client_preferences_on_disk", content.contains("distance = 8.0")
                        && content.contains("rightShoulder = false"), path);
            } catch (java.io.IOException error) {
                check("client_preferences_on_disk", false, error);
            }
            mc.options.guiScale().set(2);
            GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1031, 641);
            mc.resizeDisplay();
            press(mc, "closer", 8);
            A3RuntimeTest.server(server -> server.serverLevel().setBlockAndUpdate(AIM_WALL, Blocks.STONE.defaultBlockState()));
        }
        if (age == 2810) {
            check("resolution_changed", mc.getWindow().getWidth() != firstWidth
                    && mc.getWindow().getHeight() != firstHeight, mc.getWindow().getWidth() + "x" + mc.getWindow().getHeight());
            crosshair(mc, "left-resized", true);
            var pick = mc.hitResult;
            var forward = camera.getLookVector();
            var toPick = pick.getLocation().subtract(camera.getPosition()).normalize();
            double alignment = toPick.dot(new Vec3(forward.x(), forward.y(), forward.z()));
            check("eye_pick_and_mira_agree", pick instanceof BlockHitResult hit
                    && hit.getBlockPos().equals(AIM_WALL) && alignment > .999998, alignment);
            A3RuntimeTest.server(server -> {
                for (int x = -2; x <= 2; x++)
                    for (int y = 100; y <= 104; y++)
                        server.serverLevel().setBlockAndUpdate(new BlockPos(x, y, -2), Blocks.STONE.defaultBlockState());
            });
        }
        if (age == 2840) {
            check("baseline_camera_clipping", camera.getPosition().z > -1 && distance < 1.5, camera.getPosition());
            capture(mc, "wall-clipping");
            press(mc, "shoulder_right", 1);
        }
    }

    private static void crosshair(Minecraft mc, String name, boolean expected) {
        boolean ivory = false;
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            for (int y = image.getHeight() / 2 - 2; y <= image.getHeight() / 2 + 2; y++)
                for (int x = image.getWidth() / 2 - 2; x <= image.getWidth() / 2 + 2; x++) {
                    int pixel = image.getPixelRGBA(x, y);
                    ivory |= (pixel & 0x00ffffff) == 0x00cddfe7;
                }
            check("crosshair_" + name, ivory == expected, image.getWidth() + "x" + image.getHeight());
        }
        capture(mc, name);
    }

    private static void capture(Minecraft mc, String name) {
        var directory = A3RuntimeTest.DIR.toFile();
        directory.mkdirs();
        Screenshot.grab(directory, name + ".png", mc.getMainRenderTarget(), message -> {});
    }

    private static void press(Minecraft mc, String control, int repetitions) {
        for (var mapping : mc.options.keyMappings)
            if (mapping.getName().equals("key.towerofentropy.camera." + control))
                for (int i = 0; i < repetitions; i++) KeyMapping.click(mapping.getKey());
    }

    private static void check(String name, boolean pass, Object value) {
        LogUtils.getLogger().info("TOE_C001 {} {} {}", name, pass, value);
    }
}
