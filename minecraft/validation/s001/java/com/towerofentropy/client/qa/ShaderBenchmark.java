package com.towerofentropy.client.qa;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

@Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT)
public final class ShaderBenchmark {
    private static final boolean ENABLED = Boolean.getBoolean("toe.s001.bench");
    private static final String[] SCENES = {"forest", "plains", "cave", "water", "underwater", "night_torches",
            "crowd", "armor", "camera_tunnel", "camera_restore", "golden_hour", "rain", "combat", "camera_tunnel_opaque", "camera_tunnel_clear", "village_still", "village_walk", "village_sprint", "village_turn", "village_shoulder", "dawn", "sunset", "village_night", "village_rain",
            "sky_sunrise", "sky_morning", "sky_noon", "sky_afternoon", "sky_sunset", "sky_dusk", "sky_night",
            "lake_shallow", "river_trees", "lake_deep", "water_sunset", "water_moon", "water_rain",
            "water_swim", "water_submerged", "waterside_village", "water_cave", "water_turn", "water_edge",
            "lava_pool", "lava_nether", "lava_river", "lava_fall", "lava_cave", "lava_water", "lava_obsidian",
            "lava_day", "lava_night", "lava_player", "lava_turn", "lava_close", "lava_mobs"};
    private static final boolean S0013 = Boolean.getBoolean("toe.s0013.qa");
    private static final int ORIGINAL_SCENES = 15;
    private static final int[] SHARPNESS_SCENES = {15, 16, 17, 18, 19, 12, 3, 8};
    private static final boolean S0014 = Boolean.getBoolean("toe.s0014.qa");
    private static final boolean S0015 = Boolean.getBoolean("toe.s0015.qa");
    private static final boolean S0016 = Boolean.getBoolean("toe.s0016.qa");
    private static final boolean LAVA = Boolean.getBoolean("toe.s00161.qa");
    private static final int[] LAVA_SCENES = Boolean.getBoolean("toe.s00161.quick")
            ? new int[]{44, 43, 46, 47}
            : new int[]{43, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 44};
    private static final int[] WATER_SCENES = Boolean.getBoolean("toe.s0016.quick")
            ? new int[]{31, 32, 34, 35, 39, 41}
            : new int[]{31, 32, 33, 34, 35, 36, 7, 37, 38, 39, 40, 41, 42, 8, 9, 12};
    private static final int[] SKY_SCENES = {24, 25, 26, 27, 28, 29, 30, 0, 3, 7, 8, 9, 12, 4};
    private static final int[] LIGHTING_SCENES = {15, 0, 1, 20, 21, 22, 23, 7, 8, 9, 12, 4, 3};
    private static int selectedScene = -1;
    private static final int Y = 108;
    private static final int WARMUP = 48;
    private static final int END = 108;
    private static final ArrayList<Double> samples = new ArrayList<>();
    private static final ArrayList<Zombie> mobs = new ArrayList<>();
    private static int titleTicks, scene = -1, tick;
    private static long lastFrame, initialArmor, initialComposites;
    private static long initialDraws, initialPipeline, initialCapture, initialComposite, initialOculus, initialOculusBinds;
    private static volatile boolean ready;
    private static float lavaOceanYaw;
    private static boolean opening;

    @SubscribeEvent
    public static void client(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (!opening && mc.screen instanceof TitleScreen && mc.getOverlay() == null && ++titleTicks > 40) {
            opening = true;
            mc.options.pauseOnLostFocus = false;
            if ((S0016 || LAVA) && System.getProperty("toe.s0016.width") != null) {
                mc.getWindow().setWindowed(Integer.getInteger("toe.s0016.width"), Integer.getInteger("toe.s0016.height"));
            }
            mc.createWorldOpenFlows().loadLevel(mc.screen, "Global Survival one");
        }
        if (mc.player == null || mc.screen != null) return;
        if (scene < 0) {
            mc.options.enableVsync().set(false);
            mc.options.framerateLimit().set(260);
            mc.options.hideGui = true;
            LogUtils.getLogger().info("TOE_S001_SETUP shader={} gpu={} gl={} size={}x{} distance={} vsync={}",
                    shaderActive(), GL11.glGetString(GL11.GL_RENDERER), GL11.glGetString(GL11.GL_VERSION),
                    mc.getWindow().getWidth(), mc.getWindow().getHeight(), mc.options.renderDistance().get(),
                    mc.options.enableVsync().get());
            scene = Integer.getInteger("toe.s001.scene", 0) - 1;
            advance(mc);
        }
        if (!ready || scene >= SCENES.length) return;
        ++tick;
        if (S0013) sharpnessInput(mc);
        if (S0016 && scene == 37) {
            mc.options.keyUp.setDown(tick >= 24);
            mc.options.keySprint.setDown(tick >= 24);
        }
        if (S0016 && scene == 41) mc.player.setYRot(39.0f * (float)Math.sin(tick * 0.23f));
        if (LAVA && scene == 44) {
            mc.player.setYRot(lavaOceanYaw);
            mc.player.setXRot(14.0f);
        }
        if (LAVA && scene == 53) mc.player.setYRot(34.0f * (float)Math.sin(tick * .27f));
        if (scene == 12 && (tick == 43 || tick == 65 || tick == 86)) attack();
        if (tick == WARMUP) {
            samples.clear();
            lastFrame = 0;
            initialArmor = counter("PlayerRendererR3", "armorDraws");
            initialComposites = counter("ContextualPlayerLayer", "composites");
            initialDraws = counter("PlayerRendererR3", "drawCalls");
            initialPipeline = counter("PlayerRendererR3", "pipelineNanos");
            initialCapture = counter("ContextualPlayerLayer", "captureNanos");
            initialComposite = counter("ContextualPlayerLayer", "compositeNanos");
            initialOculus = counter("ContextualPlayerLayer", "oculusComposites");
            initialOculusBinds = counter("ContextualPlayerLayer", "oculusCaptureBinds");
        }
        if (tick == END) {
            report(mc);
            Screenshot.grab(new File("."), "s001-" + SCENES[scene] + ".png", mc.getMainRenderTarget(), ignored -> {});
            if (Boolean.getBoolean("toe.s001.oneScene")) {
                mc.stop();
                return;
            }
            advance(mc);
        }
    }

    @SubscribeEvent
    public static void render(TickEvent.RenderTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END || !ready || tick < WARMUP || tick >= END) return;
        long now = System.nanoTime();
        if (lastFrame != 0) samples.add((now - lastFrame) / 1e6);
        lastFrame = now;
    }

    private static void advance(Minecraft mc) {
        ready = false;
        tick = 0;
        if (S0013) {
            mc.options.keyUp.setDown(false);
            mc.options.keySprint.setDown(false);
            scene = ++selectedScene < SHARPNESS_SCENES.length ? SHARPNESS_SCENES[selectedScene] : SCENES.length;
        } else if (S0014) {
            scene = ++selectedScene < LIGHTING_SCENES.length ? LIGHTING_SCENES[selectedScene] : SCENES.length;
        } else if (S0015) {
            scene = ++selectedScene < SKY_SCENES.length ? SKY_SCENES[selectedScene] : SCENES.length;
        } else if (LAVA) {
            scene = ++selectedScene < LAVA_SCENES.length ? LAVA_SCENES[selectedScene] : SCENES.length;
        } else if (S0016) {
            mc.options.keyUp.setDown(false);
            mc.options.keySprint.setDown(false);
            scene = ++selectedScene < WATER_SCENES.length ? WATER_SCENES[selectedScene] : SCENES.length;
        } else {
            ++scene;
        }
        if (scene >= (S0013 || S0014 || S0015 || S0016 || LAVA ? SCENES.length : ORIGINAL_SCENES)) {
            mc.stop();
            return;
        }
        mc.options.setCameraType((scene >= 7 && scene <= 9) || (scene >= 12 && scene < 24) || scene == 37 || scene == 52
                ? CameraType.THIRD_PERSON_BACK : CameraType.FIRST_PERSON);
        setMinimumOpacity(scene == 13 ? 1.0 : scene == 14 ? 0.0 : .12);
        if (S0013 || S0014) setCameraOption("RIGHT_SHOULDER", true);
        var server = mc.getSingleplayerServer();
        var id = mc.player.getUUID();
        int stage = scene;
        server.execute(() -> {
            var player = server.getPlayerList().getPlayer(id);
            if (player == null) throw new IllegalStateException("Missing benchmark player");
            prepare(player, stage);
            ready = true;
        });
    }

    private static void report(Minecraft mc) {
        if (samples.isEmpty()) throw new IllegalStateException("Benchmark recorded no frames");
        samples.sort(Comparator.naturalOrder());
        int n = samples.size();
        double average = samples.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        int tail = Math.max(1, (int)Math.ceil(n * .01));
        double worst = samples.subList(n - tail, n).stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        double p95 = samples.get((int)Math.ceil(n * .95) - 1);
        double p99 = samples.get((int)Math.ceil(n * .99) - 1);
        LogUtils.getLogger().info("TOE_S001_RESULT {} n={} fps={} low={} p95={} opacity={} armor={} composites={}",
                SCENES[scene], n, 1000 / average, 1000 / worst, p95, opacity(mc),
                delta("PlayerRendererR3", "armorDraws", initialArmor), delta("ContextualPlayerLayer", "composites", initialComposites));
        long draws = delta("PlayerRendererR3", "drawCalls", initialDraws);
        long blends = delta("ContextualPlayerLayer", "composites", initialComposites);
        LogUtils.getLogger().info("TOE_S0011_TIMING {} p99={} draws={} r3Ms={} captureMs={} compositeMs={}",
                SCENES[scene], p99, draws, meanMillis(delta("PlayerRendererR3", "pipelineNanos", initialPipeline), draws),
                meanMillis(delta("ContextualPlayerLayer", "captureNanos", initialCapture), blends),
                meanMillis(delta("ContextualPlayerLayer", "compositeNanos", initialComposite), blends));
        LogUtils.getLogger().info("TOE_S0011_OCULUS {} composites={} binds={}", SCENES[scene],
                delta("ContextualPlayerLayer", "oculusComposites", initialOculus),
                delta("ContextualPlayerLayer", "oculusCaptureBinds", initialOculusBinds));
        if (S0014 || S0015 || S0016) {
            LogUtils.getLogger().info("TOE_S0014_WEATHER {} time={} rain={}", SCENES[scene],
                    mc.level.getDayTime(), mc.level.getRainLevel(1.0f));
        }
        if (S0016 && (scene == 37 || scene == 38 || scene == 41)) {
            LogUtils.getLogger().info("TOE_S0016_MOTION {} swimming={} submerged={} pos={} yaw={}",
                    SCENES[scene], mc.player.isSwimming(), mc.player.isUnderWater(),
                    mc.player.position(), mc.player.getYRot());
        }
    }

    private static double meanMillis(long nanos, long count) {
        return count <= 0 ? 0 : nanos / (count * 1e6);
    }

    private static void sharpnessInput(Minecraft mc) {
        boolean moving = tick >= 35 && tick <= END && (scene == 16 || scene == 17);
        mc.options.keyUp.setDown(moving);
        mc.options.keySprint.setDown(moving && scene == 17);
        if (scene == 18 && tick >= 36) mc.player.setYRot(54f * (float)Math.sin((tick - 36) * .27));
        if (scene == 19 && tick == 78) setCameraOption("RIGHT_SHOULDER", false);
        if (tick == END) {
            String clip = activeClip();
            LogUtils.getLogger().info("TOE_S0013_VISUAL {} clip={} sprint={} pos={} yaw={}",
                    SCENES[scene], clip,
                    mc.player.isSprinting(), mc.player.position(), mc.player.getYRot());
        }
    }

    private static String activeClip() {
        try {
            var type = Class.forName("com.towerofentropy.client.PlayerRendererR3");
            var controller = type.getMethod("controller").invoke(null);
            return controller == null ? "" : (String)controller.getClass().getMethod("getAnimationName").invoke(controller);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Could not query the R3 clip", error);
        }
    }

    private static void setMinimumOpacity(double opacity) {
        setCameraOption("FADE_MINIMUM", opacity);
    }

    private static void setCameraOption(String field, Object option) {
        try {
            var settings = Class.forName("com.towerofentropy.client.camera.CameraSettings");
            var minimum = settings.getDeclaredField(field);
            minimum.setAccessible(true);
            var value = minimum.get(null);
            if (!option.equals(value.getClass().getMethod("get").invoke(value))) {
                value.getClass().getMethod("set", Object.class).invoke(value, option);
            }
        } catch (ClassNotFoundException standalone) {
            // The standalone fixture uses the vanilla camera.
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not configure the camera benchmark", failure);
        }
    }

    private static void attack() {
        try {
            Class.forName("com.towerofentropy.animation.ActionNetwork")
                    .getMethod("requestAttack", boolean.class).invoke(null, false);
        } catch (ClassNotFoundException standalone) {
            // Combat is measured with the complete mod only.
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not trigger a benchmark attack", failure);
        }
    }

    private static long counter(String type, String field) {
        try {
            return Class.forName("com.towerofentropy.client." + type).getField(field).getLong(null);
        } catch (ClassNotFoundException missing) {
            return -1;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Could not read renderer counter", error);
        }
    }

    private static long delta(String type, String field, long start) {
        return start < 0 ? -1 : counter(type, field) - start;
    }

    private static float opacity(Minecraft mc) {
        try {
            var camera = Class.forName("com.towerofentropy.client.camera.ShoulderCamera");
            return (float)camera.getMethod("characterOpacity", Minecraft.class).invoke(null, mc);
        } catch (ClassNotFoundException missing) {
            return Float.NaN;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Could not read camera opacity", error);
        }
    }

    private static String shaderActive() {
        try {
            var api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            var active = (boolean)api.getMethod("isShaderPackInUse").invoke(api.getMethod("getInstance").invoke(null));
            if (!active) return "off";
            var iris = Class.forName("net.irisshaders.iris.Iris");
            var pack = ((java.util.Optional<?>)iris.getMethod("getCurrentPack").invoke(null)).orElseThrow();
            var dimension = iris.getMethod("getCurrentDimension");
            var program = pack.getClass().getMethod("getProgramSet", dimension.getReturnType())
                    .invoke(pack, dimension.invoke(null));
            var directives = program.getClass().getMethod("getPackDirectives").invoke(program);
            var shadow = directives.getClass().getMethod("getShadowDirectives").invoke(directives);
            var size = shadow.getClass().getMethod("getResolution").invoke(shadow);
            if (S0014) {
                LogUtils.getLogger().info("TOE_S0014_SHADOW range={} renderMultiplier={}",
                        shadow.getClass().getMethod("getDistance").invoke(shadow),
                        shadow.getClass().getMethod("getDistanceRenderMul").invoke(shadow));
            }
            return iris.getMethod("getCurrentPackName").invoke(null) + "/"
                    + pack.getClass().getMethod("getProfileInfo").invoke(pack) + " shadow=" + size;
        } catch (ClassNotFoundException missing) {
            return "no-loader";
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Could not query Oculus", error);
        }
    }

    private static void prepare(ServerPlayer player, int stage) {
        if (stage >= 43) {
            prepareLava(player, stage);
            return;
        }
        ServerLevel level = player.serverLevel();
        if (stage == 0 || (S0013 || S0014) && stage == 15 || S0015 && stage == 24 || S0016 && stage == 31 || Boolean.getBoolean("toe.s001.oneScene")) {
            level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS).set(false, level.getServer());
            player.setGameMode(GameType.SURVIVAL);
            player.setInvulnerable(true);
            player.getInventory().clearContent();
        }
        for (var mob : mobs) mob.discard();
        mobs.clear();
        for (var item : level.getEntitiesOfClass(ItemEntity.class, new AABB(-20, Y - 5, -16, 20, Y + 15, 30))) item.discard();
        var random = new Random(30791);
        boolean rain = stage == 11 || stage == 23 || stage == 36;
        level.setWeatherParameters(rain ? 0 : 10000, rain ? 10000 : 0, rain, false);
        if (S0016) {
            level.setRainLevel(rain ? 1.0f : 0.0f);
            level.setThunderLevel(0.0f);
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(
                    net.minecraft.network.protocol.game.ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,
                    rain ? 1.0f : 0.0f));
            level.setDayTime(switch (stage) {
                case 34 -> 12400;
                case 35 -> 14800;
                default -> 3900;
            });
        } else if (S0015) {
            level.setDayTime(switch (stage) {
                case 24 -> 23600;
                case 25 -> 2300;
                case 26 -> 6000;
                case 27 -> 8800;
                case 28 -> 12400;
                case 29 -> 13000;
                case 30 -> 14800;
                default -> stage == 3 ? 8500 : 3800;
            });
        } else if (S0014) {
            level.setRainLevel(rain ? 1.0f : 0.0f);
            level.setThunderLevel(0.0f);
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(
                    net.minecraft.network.protocol.game.ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,
                    rain ? 1.0f : 0.0f));
            level.setDayTime(switch (stage) {
                case 0, 1, 15 -> 3500;
                case 20 -> 1200;
                case 21 -> 11200;
                case 22 -> 17000;
                default -> 6000;
            });
        } else {
            level.setDayTime(stage == 5 ? 17700 : (stage == 8 || stage == 13 || stage == 14) ? 3000 : stage == 0 ? 1500 : stage == 10 ? 11400 : 5500);
        }
        for (int x = -16; x <= 16; x++) for (int z = -12; z <= 25; z++) {
            put(level, x, Y - 2, z, Blocks.STONE);
            put(level, x, Y - 1, z, stage == 2 || stage == 8 || stage == 13 || stage == 14 ? Blocks.STONE
                    : stage == 3 || stage == 4 ? Blocks.SAND : Blocks.GRASS_BLOCK);
            for (int y = 0; y <= 7; y++) put(level, x, Y + y, z, Blocks.AIR);
            if (S0016 && stage < 31) {
                for (int y = -8; y < -2; y++) put(level, x, Y + y, z, Blocks.STONE);
            }
            if (stage == 0 || stage == 1 || stage == 5 || stage == 7 || stage == 10 || stage == 11 || stage >= 24) {
                int r = random.nextInt(24);
                if (Math.abs(x) > 2 || Math.abs(z) > 3) {
                    if (r < 3) put(level, x, Y, z, r == 0 ? Blocks.ALLIUM : r == 1 ? Blocks.POPPY : Blocks.DANDELION);
                    if (r >= 3 && r < 8) put(level, x, Y, z, Blocks.GRASS);
                }
            }
        }
        if (stage >= 31) {
            prepareWater(player, stage);
            return;
        }
        switch (stage) {
            case 0 -> {
                for (int x = -12; x <= 12; x += 6) for (int z = 3; z <= 23; z += 6) {
                    int px = x + random.nextInt(3) - 1, pz = z + random.nextInt(3) - 1;
                    for (int y = 0; y < 5; y++) put(level, px, Y + y, pz, Blocks.OAK_LOG);
                    for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++)
                        for (int dy = 3; dy <= 6; dy++)
                            if (Math.abs(dx) + Math.abs(dz) + Math.abs(dy - 4) <= 4 && !(dx == 0 && dz == 0 && dy < 5))
                                level.setBlock(new BlockPos(px + dx, Y + dy, pz + dz),
                                        Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true), Block.UPDATE_CLIENTS);
                }
            }
            case 1 -> {
                if (S0014) {
                    for (int x = 5; x <= 13; ++x) for (int z = 7; z <= 16; ++z)
                        for (int y = 0; y < Math.min(5, x - 4); ++y)
                            put(level, x, Y + y, z, y < 2 ? Blocks.STONE : Blocks.GRASS_BLOCK);
                }
            }
            case 2 -> {
                for (int x = -12; x <= 12; x++) for (int z = -8; z <= 19; z++)
                    for (int y = 0; y <= 4; y++)
                        if (y == 4 || Math.abs(x) == 12 || z == -8 || z == 19)
                            put(level, x, Y + y, z, random.nextInt(4) == 0 ? Blocks.TUFF : Blocks.DEEPSLATE);
                for (int z = 3; z < 18; z += 5) put(level, z % 2 == 0 ? 4 : -4, Y, z, Blocks.TORCH);
            }
            case 3, 4 -> {
                for (int x = -12; x <= 12; x++) for (int z = 2; z <= 22; z++)
                    for (int y = 0; y <= 2; y++)
                        put(level, x, Y + y, z, Math.abs(x) == 12 || z == 22 || z == 2 ? Blocks.SANDSTONE : Blocks.WATER);
                for (int x = -8; x <= 8; x += 4) for (int z = 6; z <= 18; z += 6)
                    put(level, x, Y - 1, z, Blocks.PRISMARINE);
            }
            case 5 -> {
                for (int x = -12; x <= 12; x += 4) for (int z = 2; z <= 22; z += 5) {
                    put(level, x, Y, z, Blocks.COBBLESTONE);
                    put(level, x, Y + 1, z, Blocks.TORCH);
                }
            }
            case 6 -> {
                for (int x = -9; x <= 9; x += 2) for (int z = 4; z < 16; z += 2) {
                    var mob = EntityType.ZOMBIE.create(level);
                    if (mob == null) continue;
                    mob.setNoAi(true);
                    mob.setInvulnerable(true);
                    mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                    mob.setPos(x + .5, Y, z + .5);
                    level.addFreshEntity(mob);
                    mobs.add(mob);
                }
            }
            case 8, 13, 14 -> {
                for (int x = -3; x <= 3; x++) for (int z = -6; z <= 4; z++) put(level, x, Y + 2, z, Blocks.STONE);
                for (int x = -1; x <= 1; x++) for (int y = 0; y < 2; y++) put(level, x, Y + y, 3, Blocks.DIAMOND_ORE);
            }
            case 15, 16, 17, 18, 19, 20, 21, 22, 23 -> {
                village(level);
                if (S0014) {
                    for (int z = 3; z <= 23; z += 5) {
                        put(level, -2, Y, z, Blocks.COBBLESTONE_WALL);
                        put(level, -2, Y + 1, z, Blocks.COBBLESTONE_WALL);
                        put(level, -2, Y + 2, z, Blocks.LANTERN);
                    }
                }
            }
            case 28, 29 -> {
                for (int z : new int[]{-11, -6, 3, 8}) {
                    for (int x = -15; x <= -10; ++x) for (int dz = 0; dz < 4; ++dz) {
                        boolean wall = x == -15 || x == -10 || dz == 0 || dz == 3;
                        if (wall) for (int y = 0; y < 3; ++y) put(level, x, Y + y, z + dz, Blocks.OAK_PLANKS);
                        put(level, x, Y + 3, z + dz, Blocks.SPRUCE_PLANKS);
                        if (dz == 1 || dz == 2) put(level, x, Y + 4, z + dz, Blocks.SPRUCE_PLANKS);
                    }
                    put(level, -10, Y + 1, z + 1, Blocks.GLASS);
                    put(level, -9, Y, z + 2, Blocks.LANTERN);
                }
            }
            default -> {}
        }
        if (stage == 7 || S0013 && stage == 15 || stage == 8 && Boolean.getBoolean("toe.s001.oneScene")) {
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
            player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
            var helmet = new ItemStack(Items.DIAMOND_HELMET);
            helmet.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 1);
            var registries = level.registryAccess();
            var material = registries.registryOrThrow(net.minecraft.core.registries.Registries.TRIM_MATERIAL)
                    .getHolderOrThrow(net.minecraft.world.item.armortrim.TrimMaterials.GOLD);
            var pattern = registries.registryOrThrow(net.minecraft.core.registries.Registries.TRIM_PATTERN)
                    .getHolderOrThrow(net.minecraft.world.item.armortrim.TrimPatterns.COAST);
            net.minecraft.world.item.armortrim.ArmorTrim.setTrim(registries, helmet,
                    new net.minecraft.world.item.armortrim.ArmorTrim(material, pattern));
            player.setItemSlot(EquipmentSlot.HEAD, helmet);
        }
        if (stage == 12) {
            player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            try {
                Class.forName("com.towerofentropy.combat.WeaponReadiness")
                        .getMethod("toggle", ServerPlayer.class).invoke(null, player);
            } catch (ClassNotFoundException standalone) {
                // Vanilla rendering still exercises the sword and target model.
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Could not equip the benchmark sword", failure);
            }
            var target = EntityType.ZOMBIE.create(level);
            if (target != null) {
                target.setNoAi(true);
                target.setInvulnerable(true);
                target.setPos(.5, Y, 2.4);
                level.addFreshEntity(target);
                mobs.add(target);
            }
        }
        if (stage >= 13) player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        double height = stage == 3 ? 4.0 : 0.0;
        float yaw = 0;
        float pitch = stage == 3 ? 23 : (stage == 8 || stage == 13 || stage == 14) ? 18 : 4;
        if (S0015) {
            yaw = switch (stage) {
                case 24, 25, 30 -> -90;
                case 27, 28, 29 -> 90;
                default -> 0;
            };
            pitch = switch (stage) {
                case 24, 28, 29 -> -7;
                case 25, 26, 27 -> -21;
                case 30 -> -34;
                case 3 -> 3;
                default -> pitch;
            };
            if (stage >= 24) height = 2.0;
        }
        player.teleportTo(level, stage == 0 ? -2.5 : .5, Y + height, stage == 4 ? 6.5 : .5, yaw, pitch);
        if (stage == 3) {
            put(level, 0, Y + 2, 0, Blocks.SANDSTONE);
            put(level, 0, Y + 3, 0, Blocks.SANDSTONE);
        }
    }

    private static void prepareLava(ServerPlayer player, int stage) {
        var server = player.serverLevel().getServer();
        var level = server.overworld();
        for (var mob : mobs) mob.discard();
        mobs.clear();
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, server);
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOFIRETICK).set(false, server);
        level.setWeatherParameters(20000, 0, false, false);
        player.setGameMode(GameType.SURVIVAL);
        player.setInvulnerable(true);
        player.getInventory().clearContent();
        if (stage == 44) {
            prepareNetherOcean(player, server.getLevel(net.minecraft.world.level.Level.NETHER));
            return;
        }
        level.setDayTime(stage == 51 ? 17000 : 5700);
        int anchorZ = 80;
        boolean river = stage == 45;
        boolean fall = stage == 46;
        boolean cave = stage == 47;
        var random = new Random(3193);
        for (int x = -16; x <= 16; ++x) for (int z = -12; z <= 24; ++z) {
            boolean lava = z >= 0 && z <= 17 && Math.abs(x) <= (river ? 3 : 8);
            if (stage == 48 && x >= 2) lava = false;
            boolean water = stage == 48 && x >= 3 && x <= 10 && z >= 0 && z <= 17;
            for (int y = river ? -7 : -2; y <= -2; ++y)
                put(level, x, Y + y, anchorZ + z, Blocks.STONE);
            put(level, x, Y - 1, anchorZ + z, lava || water ? Blocks.SMOOTH_BASALT : Blocks.STONE);
            Block shore = stage == 49 ? Blocks.OBSIDIAN : random.nextInt(5) == 0 ? Blocks.SMOOTH_BASALT
                    : cave || random.nextInt(4) == 0 ? Blocks.BLACKSTONE : Blocks.STONE;
            if (stage == 43 && !lava && random.nextInt(4) == 0) shore = Blocks.DIRT;
            put(level, x, Y, anchorZ + z, water ? Blocks.WATER : lava && !river ? Blocks.LAVA : lava ? Blocks.AIR : shore);
            for (int y = 1; y <= 8; ++y) {
                boolean caveWall = cave && (y == 5 || Math.abs(x) == 16 || z == -12 || z == 24);
                boolean cliff = fall && z >= 13 && z <= 17 && Math.abs(x) <= 5 && y <= 6 + (x % 3 == 0 ? 1 : 0);
                put(level, x, Y + y, anchorZ + z, caveWall ? Blocks.DEEPSLATE : cliff ? Blocks.BASALT : Blocks.AIR);
            }
        }
        if (river) {
            for (int z = 0; z < 18; ++z) for (int x = -3; x <= 3; ++x) {
                int step = z / 3;
                put(level, x, Y - 1 - step, anchorZ + z, Blocks.BLACKSTONE);
                for (int y = Y - step; y <= Y; ++y) put(level, x, y, anchorZ + z, Blocks.AIR);
                if (Math.abs(x) <= 2) {
                    int fluidLevel = z % 3 == 0 ? 0 : z % 3 * 2;
                    level.setBlock(new BlockPos(x, Y - step, anchorZ + z),
                            Blocks.LAVA.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, fluidLevel),
                            Block.UPDATE_CLIENTS);
                }
            }
            for (int x = -2; x <= 2; ++x)
                level.setBlock(new BlockPos(x, Y, anchorZ), Blocks.LAVA.defaultBlockState(), Block.UPDATE_ALL);
        }
        if (fall) for (int x : new int[]{-2, -1, 0, 2}) {
            for (int y = 1; y <= 6; ++y) {
                level.setBlock(new BlockPos(x, Y + y, anchorZ + 12),
                        Blocks.LAVA.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, 8),
                        Block.UPDATE_CLIENTS);
            }
            level.setBlock(new BlockPos(x, Y + 7, anchorZ + 12), Blocks.LAVA.defaultBlockState(), Block.UPDATE_ALL);
        }
        if (stage == 52) {
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        }
        if (stage == 55) {
            for (int x = -10; x <= 10; x += 2) for (int z : new int[]{-1, 19, 21}) {
                var mob = EntityType.ZOMBIE.create(level);
                if (mob == null) continue;
                mob.setNoAi(true);
                mob.setInvulnerable(true);
                mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                mob.setPos(x + 0.5, Y + 1, anchorZ + z + 0.5);
                level.addFreshEntity(mob);
                mobs.add(mob);
            }
        }
        double x = river ? 4.5 : 0.5;
        double z = stage == 54 || stage == 43 ? -0.5 : fall ? -5.5 : -2.5;
        player.teleportTo(level, x, Y + 1, anchorZ + z, river ? 26 : 0, fall ? -2 : stage == 54 ? 56 : 21);
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    }

    private static void prepareNetherOcean(ServerPlayer player, ServerLevel level) {
        // Fixed sampling order gives each preset the same natural sea and camera.
        BlockPos best = null;
        int score = -1;
        for (int x = -56; x <= 56; x += 16) for (int z = -56; z <= 56; z += 16) {
            if (!level.getBlockState(new BlockPos(x, 31, z)).is(Blocks.LAVA)) continue;
            int open = 0;
            for (int dx = -6; dx <= 6; dx += 3) for (int dz = -6; dz <= 6; dz += 3) {
                if (level.getBlockState(new BlockPos(x + dx, 31, z + dz)).is(Blocks.LAVA)
                        && level.isEmptyBlock(new BlockPos(x + dx, 37, z + dz))) ++open;
            }
            if (open > score && level.isEmptyBlock(new BlockPos(x, 39, z))) {
                score = open;
                best = new BlockPos(x, 37, z);
            }
            if (score == 25) break;
        }
        if (best == null) throw new IllegalStateException("QA seed did not expose a Nether lava sea near the origin");
        float yaw = 0;
        int bestView = -1;
        for (int angle = 0; angle < 360; angle += 45) {
            int visible = 0;
            for (int distance = 4; distance <= 40; distance += 4) {
                double radians = Math.toRadians(angle);
                int x = best.getX() - (int)Math.round(Math.sin(radians) * distance);
                int z = best.getZ() + (int)Math.round(Math.cos(radians) * distance);
                if (level.getBlockState(new BlockPos(x, 31, z)).is(Blocks.LAVA)) ++visible;
            }
            if (visible > bestView) {
                bestView = visible;
                yaw = angle;
            }
        }
        lavaOceanYaw = yaw;
        level.setBlock(best, Blocks.BLACKSTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        player.teleportTo(level, best.getX() + .5, best.getY() + 1, best.getZ() + .5, yaw, 14);
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        LogUtils.getLogger().info("TOE_S00161_OCEAN {} yaw={} seaSamples={}", best, yaw, bestView);
    }

    private static void prepareWater(ServerPlayer player, int stage) {
        ServerLevel level = player.serverLevel();
        int halfWidth = stage == 32 ? 7 : 15;
        int deep = stage == 33 ? 8 : stage == 31 ? 2 : 5;
        for (int x = -16; x <= 16; ++x) for (int z = -12; z <= 25; ++z) {
            boolean wet = Math.abs(x) <= halfWidth && z >= -10 && z <= 23;
            int depth = wet ? Math.min(deep, Math.min(halfWidth + 1 - Math.abs(x), z + 11)) : 0;
            for (int y = -8; y <= 0; ++y) {
                var block = y > -depth ? Blocks.WATER : y == -depth && wet
                        ? ((x + z * 3) % 11 == 0 ? Blocks.PRISMARINE : Blocks.SAND)
                        : y == 0 ? Blocks.GRASS_BLOCK : Blocks.STONE;
                put(level, x, Y + y, z, block);
            }
        }
        if (stage == 32 || stage == 39 || stage >= 41) {
            for (int z = 1; z <= 22; z += 6) {
                for (int x : new int[]{-halfWidth - 1, halfWidth + 1}) {
                    for (int y = 1; y <= 4; ++y) put(level, x, Y + y, z, Blocks.OAK_LOG);
                    for (int dx = -2; dx <= 2; ++dx) for (int dz = -2; dz <= 2; ++dz) {
                        if (Math.abs(dx) + Math.abs(dz) <= 3) {
                            var leaves = Blocks.OAK_LEAVES.defaultBlockState()
                                    .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
                            level.setBlock(new BlockPos(x + dx, Y + 5, z + dz), leaves, Block.UPDATE_CLIENTS);
                            if (Math.abs(dx) + Math.abs(dz) <= 1)
                                level.setBlock(new BlockPos(x + dx, Y + 6, z + dz), leaves, Block.UPDATE_CLIENTS);
                        }
                    }
                }
            }
        }
        if (stage == 39 || stage == 42) {
            for (int x = -6; x <= 6; ++x) for (int z = 19; z <= 24; ++z) {
                put(level, x, Y, z, Blocks.COBBLESTONE);
                for (int y = 1; y <= 4; ++y) {
                    var block = y == 4 ? Blocks.SPRUCE_PLANKS : y == 2 && x % 3 == 0 ? Blocks.GLASS : Blocks.OAK_PLANKS;
                    put(level, x, Y + y, z, block);
                }
            }
            for (int x : new int[]{-5, 5}) put(level, x, Y + 1, 18, Blocks.LANTERN);
        }
        if (stage == 40) {
            for (int x = -15; x <= 15; ++x) for (int z = -12; z <= 24; ++z) {
                put(level, x, Y + 5, z, Blocks.DEEPSLATE);
                if (Math.abs(x) == 15 || z == 24 || z == -12)
                    for (int y = 1; y < 5; ++y) put(level, x, Y + y, z, Blocks.DEEPSLATE);
            }
            for (int z = -7; z <= 20; z += 6) for (int x : new int[]{-6, 6}) {
                put(level, x, Y, z, Blocks.TUFF);
                put(level, x, Y + 1, z, Blocks.TORCH);
            }
        }
        boolean swimming = stage == 37 || stage == 38;
        double x = stage == 34 ? 13.5 : stage == 35 ? -12.5 : stage == 42 ? 6.5 : 0.5;
        double z = stage == 34 || stage == 35 ? 1.5 : swimming ? -5.5 : -9.5;
        int base = swimming ? Y - 3 : Y + 1;
        if (!swimming) put(level, (int)Math.floor(x), Y, (int)Math.floor(z), Blocks.SMOOTH_STONE);
        float yaw = stage == 34 ? 90 : stage == 35 ? -90 : stage == 42 ? -23 : 0;
        float pitch = stage == 34 ? 8 : stage == 35 ? 32 : swimming ? -4 : 16;
        player.teleportTo(level, x, base, z, yaw, pitch);
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    }

    private static void village(ServerLevel level) {
        for (int z = -6; z <= 25; z++) {
            for (int x = -2; x <= 2; x++) put(level, x, Y - 1, z, Blocks.DIRT_PATH);
        }
        for (int side : new int[]{-1, 1}) {
            for (int origin : new int[]{3, 12, 21}) {
                for (int z = origin; z <= origin + 4; z++) for (int x = 4; x <= 9; x++) {
                    for (int h = 0; h <= 3; h++) {
                        boolean edge = x == 4 || x == 9 || z == origin || z == origin + 4;
                        if (edge) put(level, side * x, Y + h, z, h == 2 && z == origin + 2 ? Blocks.GLASS
                                : h == 0 ? Blocks.COBBLESTONE : z == origin || z == origin + 4 ? Blocks.OAK_PLANKS : Blocks.WHITE_TERRACOTTA);
                    }
                    int ridge = 4 + Math.min(x - 4, 9 - x);
                    put(level, side * x, Y + ridge, z, Blocks.DARK_OAK_PLANKS);
                }
                put(level, side * 3, Y, origin + 1, Blocks.ALLIUM);
                put(level, side * 3, Y, origin + 3, Blocks.GRASS);
            }
        }
    }

    private static void put(ServerLevel level, int x, int y, int z, Block block) {
        level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), Block.UPDATE_CLIENTS);
    }
}
