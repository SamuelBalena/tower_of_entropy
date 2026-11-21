package com.towerofentropy.client.camera;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.mojang.authlib.GameProfile;
import com.towerofentropy.animation.ActionNetwork;
import com.towerofentropy.client.ContextualPlayerLayer;
import com.towerofentropy.client.PlayerRendererR3;
import com.towerofentropy.combat.WeaponReadiness;
import java.io.File;
import java.util.UUID;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT)
public final class C0021RuntimeTest {
    private static final boolean ENABLED = Boolean.getBoolean("toe.c0021.qa");
    private static final boolean OCULUS_QA = Boolean.getBoolean("toe.s0011.qa");
    private static final boolean ARMOR = Boolean.getBoolean("toe.a001.qa");
    private static final int Y = 120, WATER = 145, FADE_STEP = 54, FADE_SCENES = ARMOR ? 21 : 11, SWIM_STEP = 31;
    private static final int WATER_WARMUP = FADE_STEP * FADE_SCENES;
    private static final int SWIM_START = WATER_WARMUP + 28;
    private static final int[] DX = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] DZ = {1, 1, 0, -1, -1, -1, 0, 1};
    private static final String[] LABELS = {"open", "corridor", "ore_opaque", "ore_fade", "restore", "manual",
            "combat", "armor", "fast", "bow", "translucent_item", "diamond_open", "dyed_leather",
            "chainmail", "iron", "gold", "netherite", "mixed_turtle", "trim_glint", "unequipped", "active_roll"};
    private static final int POSE_START = SWIM_START + 13 * SWIM_STEP, POSE_STEP = 8;
    private static final String[] POSES = {
            "idle", "walk", "jog", "sprint", "backward", "strafe_left", "strafe_right", "crouch_idle", "crouch_move",
            "jump_start", "jump_loop", "jump_land", "wall_jump", "wall_slide", "wall_slide_push", "ledge_hang",
            "ledge_climb", "ledge_climb_ual2", "sword", "sword_b", "sword_c", "heavy", "punch_jab", "punch_cross",
            "bow_load", "bow_hold", "bow_release", "roll", "swim", "swim_idle"
    };
    private static volatile int age = -80;
    private static int title, lastAge, observedScene = -1, poseTick;
    private static long composites, captureNanos, compositeNanos, pipelineNanos, draws, other, remoteCanceled, attachments;
    private static long previousFrame, armorFrames, armorNanos, oculusFrames;
    private static int frameCount;
    private static long frameNanos;
    private static boolean opening, initialized;
    private static double startX, startY, startZ, speedSum;
    private static float restoreEarly;
    private static final double[] swimSpeeds = new double[8];
    private static boolean remoteCaptured, remoteSeen, armoredRoll;
    private static int speeds;
    private static final java.util.Set<String> bowMotion = new java.util.TreeSet<>();
    private static RemotePlayer remote;
    private static Zombie zombie;

    private static void check(String label, boolean pass, Object detail) {
        LogUtils.getLogger().info("TOE_C0021 {} {} {}", label, pass ? "PASS" : "FAIL", detail);
    }

    @SubscribeEvent
    public static void frames(TickEvent.RenderTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END || observedScene < 0) return;
        long now = System.nanoTime();
        if (previousFrame > 0) { frameCount++; frameNanos += now - previousFrame; }
        previousFrame = now;
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
        if (mc.player == null || mc.screen != null || age == lastAge) return;
        lastAge = age;
        if (!initialized) {
            initialized = true;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            CameraSettings.DISTANCE.set(4.0);
            CameraSettings.RIGHT_SHOULDER.set(true);
            installSkin(mc, Boolean.getBoolean("toe.qa.slim"));
            int actualMode = mc.options.graphicsMode().get().getId();
            int expectedMode = Integer.getInteger("toe.qa.graphicsMode", actualMode);
            check("graphics_mode", actualMode == expectedMode
                    && Minecraft.useShaderTransparency() == (expectedMode == 2),
                    mc.options.graphicsMode().get() + " transparency=" + Minecraft.useShaderTransparency());
        }
        if (age < 0) return;
        if (age < WATER_WARMUP) camera(mc, age / FADE_STEP, age % FADE_STEP);
        else if (age < SWIM_START) control(mc, 0, 0, false, false, false);
        else if (ARMOR && age >= POSE_START) poses(mc);
        else swim(mc, (age - SWIM_START) / SWIM_STEP, (age - SWIM_START) % SWIM_STEP);
    }

    private static void camera(Minecraft mc, int scene, int step) {
        var p = mc.player;
        if (observedScene != scene) {
            observedScene = scene;
            CameraSettings.FADE_MINIMUM.set(scene == 2 ? 1.0 : .12);
            CameraSettings.DISTANCE.set(scene == 5 ? 2.0 : 4.0);
            CameraSettings.RIGHT_SHOULDER.set(scene != 1);
        }
        p.setYRot(0);
        p.setXRot(scene == 2 || scene == 3 || scene == 6 || scene == 7 || scene == 9 || scene == 10 ? 18 : 0);
        if (scene == 4 && step == 2) restoreEarly = ShoulderCamera.characterOpacity(mc);
        if (scene == 3 && step >= 15 && step < 32 && remote == null) {
            remote = new RemotePlayer(mc.level, new GameProfile(UUID.fromString("86ad99b1-1840-4ffc-bc13-b4707ccd80c1"), "C0021QA"));
            remote.setPos(.45, Y, 2);
            remote.setId(30841);
            mc.level.addPlayer(remote.getId(), remote);
            remoteCanceled = PlayerRendererR3.otherCanceled;
        }
        if (scene == 3 && step >= 29 && step < 32 && remote != null && !remoteCaptured) {
            remoteCaptured = true;
            remoteSeen = PlayerRendererR3.otherPre > other && PlayerRendererR3.otherCanceled == remoteCanceled;
            capture(mc, "remote_opaque");
        }
        if ((scene != 3 || step >= 32) && remote != null) {
            mc.level.removeEntity(remote.getId(), net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            remote = null;
        }
        if (OCULUS_QA && scene == 3) {
            if (step >= 22 && step < 42 && step % 3 == 1) {
                var near = new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1, .2f, .8f), 1.6f);
                var far = new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(.2f, .9f, 1), 1.6f);
                mc.level.addParticle(near, true, .62, Y + 1.45, .37, 0, 0, 0);
                mc.level.addParticle(far, true, .26, Y + 1.2, 2.25, 0, 0, 0);
            }
            if (step == 39) capture(mc, "oculus_particles");
        }
        if (scene == 6 && step == 16) ActionNetwork.requestAttack(false);
        if (scene == 6 && step == 34) ActionNetwork.requestCombatRoll(0, -1);
        if (ARMOR && scene == 9) {
            mc.options.keyUse.setDown(step >= 6 && step < 37);
            var controller = PlayerRendererR3.controller();
            if (controller != null) {
                String clip = controller.getAnimationName();
                if (clip != null && clip.startsWith("UAL_bow_") && !clip.equals("UAL_bow_idle") && bowMotion.add(clip))
                    capture(mc, clip);
            }
            if (step == 48) check("active_bow", bowMotion.containsAll(java.util.Set.of("UAL_bow_load", "UAL_bow_hold", "UAL_bow_release")), bowMotion);
        }
        if (scene == 20) {
            if (step == 18) ActionNetwork.requestRoll(0, 1);
            var controller = PlayerRendererR3.controller();
            if (!armoredRoll && controller != null && controller.getAnimationName().equals("UAL_roll")
                    && com.towerofentropy.client.ClientRollMotion.visual() && PlayerRendererR3.armorDraws > armorFrames) {
                armoredRoll = true;
                capture(mc, "active_roll_in_motion");
            }
        }
        if (scene == 8) control(mc, 0, step >= 10 && step < 48 ? 1 : 0, true, false, false);
        if (step == 12) {
            composites = ContextualPlayerLayer.composites;
            oculusFrames = ContextualPlayerLayer.oculusComposites;
            captureNanos = ContextualPlayerLayer.captureNanos;
            compositeNanos = ContextualPlayerLayer.compositeNanos;
            pipelineNanos = PlayerRendererR3.pipelineNanos;
            draws = PlayerRendererR3.drawCalls;
            attachments = PlayerRendererR3.attachmentDraws;
            armorFrames = PlayerRendererR3.armorDraws;
            armorNanos = PlayerRendererR3.armorNanos;
            other = PlayerRendererR3.otherPre;
            frameCount = 0;
            frameNanos = previousFrame = 0;
        }
        if (step != 49) return;
        float opacity = ShoulderCamera.characterOpacity(mc);
        long composed = ContextualPlayerLayer.composites - composites;
        long r3 = PlayerRendererR3.drawCalls - draws;
        boolean pass = switch (scene) {
            case 0, 2, 4, 5 -> opacity >= .99 && (scene != 4 || restoreEarly < .99);
            case 1, 8 -> opacity < .99 && opacity > .15 && composed > 0;
            case 3 -> opacity < .16 && composed > 0 && r3 > 0 && remoteSeen && mc.level.getEntity(30841) == null;
            case 6 -> opacity < .99 && composed > 0 && com.towerofentropy.client.ClientWeaponState.inCombat();
            case 7 -> opacity < .16 && composed > 0 && r3 > 0 && PlayerRendererR3.armorDraws > armorFrames;
            case 9 -> opacity < .16 && composed > 0 && r3 > 0 && PlayerRendererR3.attachmentDraws > attachments
                    && PlayerRendererR3.controller().getAnimationName().equals("UAL_bow_idle");
            case 10 -> opacity < .16 && composed > 0 && r3 > 0 && PlayerRendererR3.attachmentDraws > attachments;
            case 11, 12, 13, 14, 15, 16, 17, 18, 19 -> opacity >= .99 && r3 > 0;
            case 20 -> opacity >= .99 && r3 > 0 && armoredRoll;
            default -> false;
        };
        long armored = PlayerRendererR3.armorDraws - armorFrames;
        if (ARMOR) pass &= scene == 0 || scene == 19 ? armored == 0 : armored > 0;
        if (OCULUS_QA && scene == 3) check("oculus_composition",
                composed > 0 && ContextualPlayerLayer.oculusComposites - oculusFrames == composed, composed);
        check(LABELS[scene], pass, "opacity=" + opacity + " early=" + restoreEarly + " composites=" + composed
                + " r3=" + r3 + " other=" + (PlayerRendererR3.otherPre - other));
        LogUtils.getLogger().info("TOE_C0021_FRAME {} n={} frameMs={} drawMs={} isolatedMs={} compositeMs={} armorFrames={} armorMs={}", LABELS[scene],
                frameCount, mean(frameNanos, frameCount), mean(PlayerRendererR3.pipelineNanos - pipelineNanos, r3),
                mean(ContextualPlayerLayer.captureNanos - captureNanos, composed),
                mean(ContextualPlayerLayer.compositeNanos - compositeNanos, composed), armored, mean(PlayerRendererR3.armorNanos - armorNanos, armored));
        capture(mc, LABELS[scene]);
    }

    private static void swim(Minecraft mc, int scene, int step) {
        if (scene > 12) { control(mc, 0, 0, false, false, false); mc.stop(); return; }
        var p = mc.player;
        if (step < 4) {
            control(mc, 0, 0, false, false, false);
            CameraSettings.RIGHT_SHOULDER.set((scene < 4) != Boolean.getBoolean("toe.qa.slim"));
        }
        p.setYRot(0);
        p.setXRot(0);
        if (step == 4) {
            armorFrames = PlayerRendererR3.armorDraws;
            armorNanos = PlayerRendererR3.armorNanos;
            frameCount = 0;
            frameNanos = previousFrame = 0;
            startX = p.getX();
            startY = p.getY();
            startZ = p.getZ();
            speedSum = 0;
            speeds = 0;
        }
        if (step >= 4 && step < 29) {
            int direction = scene < 8 ? scene : scene == 8 && step >= 15 ? 4 : 0;
            control(mc, scene == 10 || scene == 11 ? 0 : DX[direction], scene == 10 || scene == 11 ? 0 : DZ[direction],
                    scene <= 8, scene == 10 && step < 15, scene == 10 && step >= 19);
            if (step >= 18) {
                speedSum += Math.hypot(p.getX() - p.xo, p.getZ() - p.zo);
                speeds++;
            }
        }
        if (scene == 10 && step == 15) {
            check("vertical_up", p.getY() > startY + .1, p.getY() - startY);
            startY = p.getY();
        }
        if (step != 28) return;
        if (ARMOR) {
            long armored = PlayerRendererR3.armorDraws - armorFrames;
            check("swim_armor_" + scene, armored > 0, armored);
            LogUtils.getLogger().info("TOE_C0021_FRAME swim_{} n={} frameMs={} armorFrames={} armorMs={}", scene,
                    frameCount, mean(frameNanos, frameCount), armored, mean(PlayerRendererR3.armorNanos - armorNanos, armored));
        }
        double x = p.getX() - startX, z = p.getZ() - startZ;
        String clip = PlayerRendererR3.controller() == null ? "" : PlayerRendererR3.controller().getAnimationName();
        if (scene < 8) {
            double direction = Math.toDegrees(Math.atan2(-DX[scene], DZ[scene]));
            double actual = Math.toDegrees(Math.atan2(-x, z));
            check("direction_" + scene, p.isSwimming() && clip.equals("UAL_swim")
                    && Math.abs(Mth.wrapDegrees(actual - direction)) < 20
                    && Math.abs(Mth.wrapDegrees(PlayerRendererR3.lastYaw - direction)) < 30,
                    "vector=" + x + "," + z + " yaw=" + PlayerRendererR3.lastYaw + " speed=" + speedSum / speeds + " clip=" + clip);
            swimSpeeds[scene] = speedSum / speeds;
            if (scene == 7) {
                double cardinal = (swimSpeeds[0] + swimSpeeds[2] + swimSpeeds[4] + swimSpeeds[6]) / 4;
                double diagonal = (swimSpeeds[1] + swimSpeeds[3] + swimSpeeds[5] + swimSpeeds[7]) / 4;
                check("diagonal_speed", Math.abs(diagonal / cardinal - 1) < .10, diagonal / cardinal);
            }
            capture(mc, "swim-" + scene);
        } else if (scene == 8) {
            check("rapid_reversal", p.isSwimming() && p.getZ() - p.zo < -.02
                    && Math.abs(Mth.wrapDegrees(PlayerRendererR3.lastYaw - 180)) < 40, PlayerRendererR3.lastYaw);
        } else if (scene == 9) {
            check("surface", p.isInWater() && clip.equals("UAL_swim") && z > .2, z);
        } else if (scene == 10) {
            check("vertical_down", p.getY() < startY - .04, p.getY() - startY);
        } else if (scene == 11) {
            check("aquatic_idle", p.isInWater() && clip.equals("UAL_swim_idle"), clip);
        } else {
            check("return_to_land", !p.isInWater() && !clip.startsWith("UAL_swim") && z > .2, clip);
        }
    }

    private static void poses(Minecraft mc) {
        int frame = poseTick++;
        int scene = frame / POSE_STEP, step = frame % POSE_STEP;
        if (scene == POSES.length) {
            if (step == 0) {
                armorFrames = PlayerRendererR3.armorDraws;
                PlayerRendererR3.pose("idle", .25f);
                mc.options.setCameraType(CameraType.FIRST_PERSON);
            }
            if (step == 3) {
                check("first_person", PlayerRendererR3.armorDraws == armorFrames, PlayerRendererR3.armorDraws - armorFrames);
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            }
            if (step == 7) check("third_person_restored", PlayerRendererR3.armorDraws > armorFrames, PlayerRendererR3.armorDraws - armorFrames);
            return;
        }
        if (scene > POSES.length && scene <= POSES.length + 4) {
            int phase = scene - POSES.length;
            boolean slim = Boolean.getBoolean("toe.qa.slim");
            if (phase <= 2) slim = !slim;
            if ((phase == 1 || phase == 3) && step == 0) {
                armorFrames = PlayerRendererR3.armorDraws;
                installSkin(mc, slim);
            }
            if ((phase == 2 || phase == 4) && step == 7) {
                String model = mc.player.getModelName();
                check(phase == 2 ? "armor_after_skin_swap" : "armor_after_skin_restore",
                        model.equals(slim ? "slim" : "default") && PlayerRendererR3.armorDraws > armorFrames, model);
                capture(mc, phase == 2 ? "skin-swap" : "skin-restored");
            }
            return;
        }
        if (scene > POSES.length + 4 && scene <= POSES.length + 7) {
            int phase = scene - POSES.length;
            if (step == 0) {
                var server = mc.getSingleplayerServer();
                var id = mc.player.getUUID();
                server.execute(() -> {
                    var player = server.getPlayerList().getPlayer(id);
                    if (player == null) return;
                    equipArmor(player, phase == 5 ? 17 : phase == 6 ? 19 : 11);
                    player.inventoryMenu.broadcastFullState();
                });
            }
            if (step == 4) armorFrames = PlayerRendererR3.armorDraws;
            if (step == 7) {
                var items = new java.util.ArrayList<Item>();
                for (var stack : mc.player.getArmorSlots()) items.add(stack.getItem());
                boolean drawn = PlayerRendererR3.armorDraws > armorFrames;
                boolean expected = switch (phase) {
                    case 5 -> drawn && items.equals(java.util.List.of(Items.GOLDEN_BOOTS, Items.AIR, Items.IRON_CHESTPLATE, Items.TURTLE_HELMET));
                    case 6 -> !drawn && items.stream().allMatch(item -> item == Items.AIR);
                    case 7 -> drawn && items.equals(java.util.List.of(Items.DIAMOND_BOOTS, Items.DIAMOND_LEGGINGS, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_HELMET));
                    default -> false;
                };
                check("rapid_armor_" + phase, expected, items);
            }
            return;
        }
        if (scene > POSES.length + 7) {
            mc.stop();
            return;
        }
        String clip = POSES[scene];
        if (step == 0) {
            control(mc, 0, 0, false, false, false);
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            mc.options.hideGui = true;
            CameraSettings.RIGHT_SHOULDER.set(true);
            armorFrames = PlayerRendererR3.armorDraws;
            PlayerRendererR3.pose(clip, .21f);
        }
        var controller = PlayerRendererR3.controller();
        boolean active = controller != null && controller.getAnimationName().equals("UAL_" + clip);
        if (step == 3 && active) {
            capture(mc, "pose-" + clip + "-early");
            PlayerRendererR3.pose(clip, controller.getCurrentAnimation().getDuration() * .65f);
        }
        if (step == 7) {
            check("pose_" + clip, active && PlayerRendererR3.armorDraws > armorFrames,
                    "active=" + active + " armorFrames=" + (PlayerRendererR3.armorDraws - armorFrames));
            capture(mc, "pose-" + clip + "-late");
        }
    }

    private static void control(Minecraft mc, int x, int z, boolean sprint, boolean up, boolean down) {
        mc.options.keyUp.setDown(z > 0);
        mc.options.keyDown.setDown(z < 0);
        mc.options.keyLeft.setDown(x > 0);
        mc.options.keyRight.setDown(x < 0);
        mc.options.keySprint.setDown(sprint);
        mc.options.keyJump.setDown(up);
        mc.options.keyShift.setDown(down);
    }

    private static double mean(long ns, long n) { return n == 0 ? 0 : ns / (n * 1e6); }

    private static void capture(Minecraft mc, String name) {
        Screenshot.grab(new File("."), (ARMOR ? "a001-" : "c0021-") + name + ".png", mc.getMainRenderTarget(), ignored -> {});
    }

    @SuppressWarnings("unchecked")
    private static void installSkin(Minecraft mc, boolean slim) {
        try {
            ResourceLocation original = new ResourceLocation("minecraft", slim
                    ? "textures/entity/player/slim/steve.png" : "textures/entity/player/wide/steve.png");
            try (var stream = mc.getResourceManager().open(original)) {
                var image = NativeImage.read(stream);
                for (int x = 8; x < 16; x++) for (int y = 8; y < 16; y++) image.setPixelRGBA(x, y, 0xFF7064D3);
                // Deliberate holes in the rear hat overlay exercise the skin's cutout alpha.
                for (int x = 56; x < 64; x++) for (int y = 8; y < 16; y++)
                    image.setPixelRGBA(x, y, (x + y) % 2 == 0 ? 0 : 0xFF2465E7);
                var id = mc.getTextureManager().register("toe_c0021_custom", new DynamicTexture(image));
                var info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
                var cls = info.getClass();
                var textures = cls.getDeclaredField("textureLocations");
                textures.setAccessible(true);
                var map = (java.util.Map<com.mojang.authlib.minecraft.MinecraftProfileTexture.Type, ResourceLocation>) textures.get(info);
                map.put(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN, id);
                map.remove(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.CAPE);
                var model = cls.getDeclaredField("skinModel");
                model.setAccessible(true);
                model.set(info, slim ? "slim" : "default");
                var pending = cls.getDeclaredField("pendingTextures");
                pending.setAccessible(true);
                pending.setBoolean(info, true);
                check("custom_skin", mc.player.getSkinTextureLocation().equals(id), id);
                check("skin_model", mc.player.getModelName().equals(slim ? "slim" : "default"), mc.player.getModelName());
            }
        } catch (Exception error) {
            throw new IllegalStateException("Could not install fixture skin", error);
        }
    }

    @SubscribeEvent
    public static void server(TickEvent.ServerTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.START || event.getServer().getPlayerList().getPlayers().isEmpty()) return;
        ServerPlayer p = event.getServer().getPlayerList().getPlayers().get(0);
        int next = age + 1;
        if (next == -79) {
            p.setGameMode(GameType.SURVIVAL);
            p.setInvulnerable(true);
            p.getInventory().clearContent();
            p.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
            p.getInventory().selected = 0;
            p.inventoryMenu.broadcastFullState();
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
            p.teleportTo(p.serverLevel(), .5, Y, .5, 0, 0);
            pool(p);
        }
        if (next >= 0 && next < WATER_WARMUP && next % FADE_STEP == 0) environment(p, next / FADE_STEP);
        if (next == WATER_WARMUP || next >= SWIM_START && (!ARMOR || next < POSE_START) && (next - SWIM_START) % SWIM_STEP == 0) {
            int index = next == WATER_WARMUP ? 0 : (next - SWIM_START) / SWIM_STEP;
            if (ARMOR && next == WATER_WARMUP) {
                equipArmor(p, 11);
                p.inventoryMenu.broadcastFullState();
            }
            p.setSprinting(false);
            p.teleportTo(p.serverLevel(), .5, index == 12 ? Y : index == 9 ? WATER + 3.6 : WATER + 1, .5, 0, 0);
            p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        }
        if (ARMOR && next == POSE_START) {
            p.teleportTo(p.serverLevel(), .5, Y, .5, 0, 0);
            p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        }
        age = next;
    }

    private static void environment(ServerPlayer p, int scene) {
        if (scene == 3) return; // Keep the identical geometry and camera for the opacity A/B pair.
        if (zombie != null) { zombie.discard(); zombie = null; }
        var level = p.serverLevel();
        p.teleportTo(level, .5, Y, .5, 0, 0);
        for (int x = -3; x <= 3; x++) for (int z = -6; z <= 18; z++) {
            level.setBlock(new BlockPos(x, Y - 1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            for (int y = 0; y <= 3; y++)
                level.setBlock(new BlockPos(x, Y + y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        if (scene == 1 || scene == 8) for (int z = -6; z <= 18; z++) for (int y = 0; y < 3; y++)
            for (int x : new int[]{-1, 1})
                level.setBlock(new BlockPos(x, Y + y, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        if (scene == 2 || scene == 6 || scene == 7 || scene == 9 || scene == 10) {
            for (int x = -3; x <= 3; x++) for (int z = -6; z <= 4; z++)
                level.setBlock(new BlockPos(x, Y + 2, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            for (int x = -1; x <= 1; x++) for (int y = 0; y < 2; y++)
                level.setBlock(new BlockPos(x, Y + y, 3), Blocks.DIAMOND_ORE.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        if (scene == 6) {
            WeaponReadiness.toggle(p);
            zombie = EntityType.ZOMBIE.create(level);
            zombie.setNoAi(true);
            zombie.setPos(.5, Y, 2.5);
            level.addFreshEntity(zombie);
            zombie.setTarget(p);
        }
        if (scene == 20) {
            p.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
            try {
                com.towerofentropy.combat.CombatController.enable(p);
            } catch (Exception failure) {
                throw new IllegalStateException("Could not start the A001 Roll fixture", failure);
            }
        }
        if (scene == 9) {
            p.getInventory().setItem(0, new ItemStack(Items.BOW));
            if (ARMOR) p.getInventory().setItem(1, new ItemStack(Items.ARROW, 16));
        }
        if (scene == 10) p.getInventory().setItem(0, new ItemStack(Items.CYAN_STAINED_GLASS));
        var helmet = scene == 7 ? new ItemStack(Items.IRON_HELMET) : ItemStack.EMPTY;
        if (!helmet.isEmpty()) helmet.enchant(Enchantments.UNBREAKING, 1);
        if (ARMOR) equipArmor(p, scene);
        else p.getInventory().armor.set(3, helmet);
        p.inventoryMenu.broadcastFullState();
    }

    private static void equipArmor(ServerPlayer player, int scene) {
        var items = switch (scene) {
            case 0, 19 -> new Item[4];
            case 12 -> new Item[]{Items.LEATHER_BOOTS, Items.LEATHER_LEGGINGS, Items.LEATHER_CHESTPLATE, Items.LEATHER_HELMET};
            case 13 -> new Item[]{Items.CHAINMAIL_BOOTS, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_HELMET};
            case 14 -> new Item[]{Items.IRON_BOOTS, Items.IRON_LEGGINGS, Items.IRON_CHESTPLATE, Items.IRON_HELMET};
            case 15 -> new Item[]{Items.GOLDEN_BOOTS, Items.GOLDEN_LEGGINGS, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_HELMET};
            case 16 -> new Item[]{Items.NETHERITE_BOOTS, Items.NETHERITE_LEGGINGS, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_HELMET};
            case 17 -> new Item[]{Items.GOLDEN_BOOTS, null, Items.IRON_CHESTPLATE, Items.TURTLE_HELMET};
            default -> new Item[]{Items.DIAMOND_BOOTS, Items.DIAMOND_LEGGINGS, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_HELMET};
        };
        for (int slot = 0; slot < items.length; slot++) {
            var item = items[slot];
            var stack = item == null ? ItemStack.EMPTY : new ItemStack(item);
            if (item instanceof DyeableLeatherItem dyeable) dyeable.setColor(stack, 0xBE376D);
            if (scene == 7 || scene == 18) stack.enchant(Enchantments.UNBREAKING, 1);
            if (scene == 18) {
                var registries = player.serverLevel().registryAccess();
                var material = registries.registryOrThrow(Registries.TRIM_MATERIAL)
                        .getHolderOrThrow(TrimMaterials.GOLD);
                var pattern = registries.registryOrThrow(Registries.TRIM_PATTERN)
                        .getHolderOrThrow(TrimPatterns.COAST);
                ArmorTrim.setTrim(registries, stack,
                        new ArmorTrim(material, pattern));
            }
            player.getInventory().armor.set(slot, stack);
        }
    }

    private static void pool(ServerPlayer p) {
        var level = p.serverLevel();
        for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
            for (int y = -1; y <= 5; y++) {
                boolean border = x == -8 || x == 8 || z == -8 || z == 8 || y == -1;
                var state = border ? Blocks.STONE.defaultBlockState() : y <= 3 ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
                level.setBlock(new BlockPos(x, WATER + y, z), state, Block.UPDATE_CLIENTS);
            }
        }
    }
}
