package com.towerofentropy.client.qa;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

final class AtmosphereGallery {
    private static final int ORIGIN = 992;
    private static final int BASE = 74;

    record Scene(String name, String biome, int time, int weather) {}

    static final Scene[] SCENES = {
        new Scene("aurora_snowy_plains", "snowy_plains", 18000, 0),
        new Scene("aurora_ice_spikes", "ice_spikes", 18000, 0),
        new Scene("aurora_frozen_ocean", "frozen_ocean", 18000, 0),
        new Scene("aurora_snowy_taiga", "snowy_taiga", 18000, 0),
        new Scene("aurora_grove", "grove", 18000, 0),
        new Scene("aurora_frozen_peaks", "frozen_peaks", 18000, 0),
        new Scene("aurora_dusk", "snowy_plains", 13700, 0),
        new Scene("aurora_desert", "desert", 18000, 0),
        new Scene("fog_forest_dawn", "forest", 700, 0),
        new Scene("fog_forest_noon", "forest", 6000, 0),
        new Scene("fog_swamp", "swamp", 1100, 0),
        new Scene("fog_mountain", "meadow", 1600, 0),
        new Scene("fog_plains_dawn", "plains", 700, 0),
        new Scene("fog_rain", "forest", 3500, 1),
        new Scene("fog_storm", "forest", 3500, 2),
        new Scene("aurora_boundary", "snowy_plains", 18000, 0),
        new Scene("aurora_clear_air", "frozen_peaks", 16300, 0),
        new Scene("fog_before_sunrise", "forest", 23980, 0),
        new Scene("fog_after_sunrise", "forest", 24020, 0),
        new Scene("expansion_window", "forest", 1300, 0),
        new Scene("expansion_window_rain", "forest", 1300, 1),
        new Scene("expansion_dry", "plains", 3700, 0),
        new Scene("expansion_rain", "plains", 3700, 1),
        new Scene("expansion_wind", "plains", 3700, 2),
        new Scene("expansion_snow", "snowy_plains", 18000, 2),
        new Scene("expansion_held_light", "forest", 18000, 0),
        new Scene("expansion_snow_clear", "snowy_plains", 18000, 0),
        new Scene("expansion_nether_wastes", "nether_wastes", 6000, 0),
        new Scene("expansion_nether_crimson", "crimson_forest", 6000, 0),
        new Scene("expansion_nether_warped", "warped_forest", 6000, 0),
        new Scene("expansion_nether_soul", "soul_sand_valley", 6000, 0),
        new Scene("expansion_nether_basalt", "basalt_deltas", 6000, 0)
    };
    private static boolean prepared;

    private AtmosphereGallery() {}

    static void prepare(ServerPlayer player, Scene scene) {
        var server = player.getServer();
        ServerLevel level = server.overworld();
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        level.setDayTime(24000L * 6 + scene.time());
        boolean wet = scene.weather() > 0;
        boolean storm = scene.weather() == 2;
        level.setWeatherParameters(wet ? 0 : 20000, wet ? 20000 : 0, wet, storm);
        level.setRainLevel(wet ? 1.0F : 0.0F);
        level.setThunderLevel(storm ? 1.0F : 0.0F);
        player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, level.getRainLevel(1)));
        player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, level.getThunderLevel(1)));
        player.setGameMode(GameType.CREATIVE);
        player.getInventory().clearContent();
        if (!prepared) {
            build(level);
            prepared = true;
        }
        var dispatcher = server.getCommands();
        var source = server.createCommandSourceStack().withSuppressedOutput().withLevel(level);
        dispatcher.performPrefixedCommand(source, "gamerule commandModificationBlockLimit 1000000");
        for (int x = (ORIGIN - 20) >> 4; x <= (ORIGIN + 20) >> 4; ++x) {
            for (int z = -20 >> 4; z <= 20 >> 4; ++z) level.getChunk(x, z);
        }
        String limits = (ORIGIN - 20) + " -64 -20 " + (ORIGIN + 20) + " 319 20";
        int replaced = dispatcher.performPrefixedCommand(source, "fillbiome " + limits + " minecraft:" + scene.biome());
        if (scene.name().equals("aurora_boundary")) {
            String warmerSide = (ORIGIN + 8) + " -64 -20 " + (ORIGIN + 20) + " 319 20";
            dispatcher.performPrefixedCommand(source, "fillbiome " + warmerSide + " minecraft:plains");
        }
        if (scene.name().startsWith("expansion_")) {
            buildExpansion(level, scene);
            if (scene.name().equals("expansion_held_light")) {
                player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TORCH));
            }
        }
        boolean elevated = scene.name().equals("aurora_clear_air");
        int cameraHeight = elevated ? 244 : BASE + 2;
        if (elevated) {
            for (int x = -4; x <= 4; ++x) {
                for (int z = -4; z <= 4; ++z) place(level, x, cameraHeight - 1, z, Blocks.SNOW_BLOCK);
            }
        }
        boolean gallery = scene.name().startsWith("expansion_");
        float pitch = elevated ? -27.0F : scene.name().startsWith("aurora") ? -15.0F : -6.0F;
        if (scene.name().equals("expansion_held_light")) pitch = 32.0F;
        player.teleportTo(level, ORIGIN + (gallery ? -3.5 : 0.5), cameraHeight, 0.5, -91.0F, pitch);
        var biome = level.getBiome(BlockPos.containing(ORIGIN + 0.5, cameraHeight, 0.5)).unwrapKey().orElseThrow();
        if (!biome.location().getPath().equals(scene.biome())) {
            throw new IllegalStateException("Expected biome " + scene.biome() + ", received " + biome.location());
        }
        LogUtils.getLogger().info("TOE_ATMOS_SCENE {} biome={} time={} weather={} cells={}",
                scene.name(), biome.location(), scene.time(), scene.weather(), replaced);
    }

    static void prepareNether(ServerPlayer player, Scene scene) {
        ServerLevel level = player.serverLevel();
        level.setDayTime(scene.time());
        var server = level.getServer();
        var source = server.createCommandSourceStack().withSuppressedOutput().withLevel(level);
        server.getCommands().performPrefixedCommand(source, "gamerule commandModificationBlockLimit 1000000");
        int floorY = 107, zCenter = 248;
        for (int x = -14; x <= 46; ++x) for (int z = zCenter - 5; z <= zCenter + 5; ++z) {
            level.setBlock(new BlockPos(x, floorY, z), Blocks.BASALT.defaultBlockState(), Block.UPDATE_CLIENTS);
            for (int y = 1; y <= 6; ++y) {
                boolean edge = x == -14 || x == 46 || Math.abs(z - zCenter) == 5;
                Block block = edge || y == 6 ? Blocks.BLACKSTONE : Blocks.AIR;
                if (y == 1 && z == zCenter - 4 && x % 9 == 0) block = Blocks.SHROOMLIGHT;
                if (y == 1 && z == zCenter + 4 && x % 9 == 0) block = Blocks.SOUL_LANTERN;
                level.setBlock(new BlockPos(x, floorY + y, z), block.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        for (int x = -1; x <= 2; ++x) level.getChunk(x, 15);
        server.getCommands().performPrefixedCommand(source,
            "fillbiome -16 96 240 47 125 255 minecraft:" + scene.biome());
        player.teleportTo(level, -11.5, floorY + 1, zCenter + 0.5, -90.0F, -3.0F);
        String actual = level.getBiome(player.blockPosition()).unwrapKey().orElseThrow().location().getPath();
        if (!actual.equals(scene.biome()))
            throw new IllegalStateException("Expected Nether biome " + scene.biome() + ", received " + actual);
        LogUtils.getLogger().info("TOE_ATMOS_SCENE {} biome={} time={} weather=0", scene.name(), actual, scene.time());
    }

    static void crossBoundary(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        player.teleportTo(level, ORIGIN + 15.5, BASE + 2.0, 0.5, -91.0F, -15.0F);
        var biome = level.getBiome(player.blockPosition()).unwrapKey().orElseThrow();
        if (!biome.location().getPath().equals("plains")) {
            throw new IllegalStateException("Expected plains beyond the boundary, received " + biome.location());
        }
        LogUtils.getLogger().info("TOE_ATMOS_BOUNDARY biome={}", biome.location());
    }

    private static void build(ServerLevel level) {
        for (int x = -8; x <= 115; ++x) {
            for (int z = -50; z <= 50; ++z) {
                double ridge = Math.exp(-Math.pow((x - 89) / 19.0, 2));
                double hills = 0.55 + 0.22 * Math.sin(z * 0.10) + 0.20 * Math.cos(z * 0.22 + x * 0.13);
                int ground = BASE - 8 + (int)(ridge * hills * 43 + 3.4 * Math.cos(z * 0.09 + x * 0.055));
                if (x <= 4 && Math.abs(z) < 8) ground = BASE;
                Block surface = ground > BASE + 11 ? Blocks.SNOW_BLOCK : ground < BASE - 6 ? Blocks.GRAVEL : Blocks.GRASS_BLOCK;
                for (int y = ground - 4; y < ground; ++y) place(level, x, y, z, Blocks.STONE);
                place(level, x, ground, z, surface);
                for (int y = ground + 1; y <= BASE - 6; ++y) place(level, x, y, z, Blocks.WATER);
                if (surface == Blocks.GRASS_BLOCK && x > 10 && Math.floorMod(x * 11 + z * 17, 37) == 0) {
                    for (int y = 1; y <= 6; ++y) place(level, x, ground + y, z, Blocks.SPRUCE_LOG);
                    for (int y = 3; y <= 7; ++y) {
                        int radius = y <= 4 ? 2 : y <= 6 ? 1 : 0;
                        for (int dx = -radius; dx <= radius; ++dx) {
                            for (int dz = -radius; dz <= radius; ++dz) {
                                if (dx == 0 && dz == 0 && y < 7 || Math.abs(dx) + Math.abs(dz) > radius + 1) continue;
                                place(level, x + dx, ground + y, z + dz, Blocks.SPRUCE_LEAVES);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void buildExpansion(ServerLevel level, Scene scene) {
        boolean room = scene.name().startsWith("expansion_window") || scene.name().equals("expansion_held_light");
        boolean winter = scene.name().startsWith("expansion_snow");
        var floor = new Block[]{Blocks.STONE_BRICKS, Blocks.SPRUCE_PLANKS, Blocks.SMOOTH_STONE, Blocks.GRASS_BLOCK};
        for (int x = -6; x <= 18; x++) for (int z = -9; z <= 9; z++) {
            place(level, x, BASE, z, winter ? Blocks.SNOW_BLOCK : floor[(z + 9) / 5]);
            for (int y = 1; y <= 6; y++) {
                boolean roof = room && x < 9 && y == 5;
                boolean wall = room && (z == -9 || z == 9 || x == -6 || x == 8) && y < 5;
                boolean window = x == 8 && Math.abs(z) <= 3 && y >= 2 && y <= 4;
                place(level, x, BASE + y, z, (roof || wall && !window) ? Blocks.STONE_BRICKS : Blocks.AIR);
            }
        }
        if (!room && !winter) {
            for (int x = 0; x <= 16; x += 2) {
                place(level, x, BASE + 1, 6, Blocks.FERN);
                place(level, x, BASE + 1, 8, Blocks.POPPY);
                place(level, x, BASE, -8, Blocks.FARMLAND);
                place(level, x, BASE + 1, -8, Blocks.WHEAT.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.CropBlock.AGE, 7));
            }
            for (int x = 5; x <= 12; x++) for (int z = -3; z <= 0; z++) {
                place(level, x, BASE - 1, z, Blocks.SAND);
                place(level, x, BASE, z, (x + z) % 2 == 0 ? Blocks.SEAGRASS : Blocks.WATER);
            }
        }
        if (!scene.name().equals("expansion_held_light")) {
            place(level, 5, BASE + 1, -4, Blocks.IRON_BLOCK);
            place(level, 5, BASE + 2, -4, Blocks.LANTERN);
            place(level, 3, BASE + 1, 3, Blocks.STONE_BRICK_STAIRS);
        }
    }

    private static void place(ServerLevel level, int x, int y, int z, Block block) {
        place(level, x, y, z, block.defaultBlockState());
    }

    private static void place(ServerLevel level, int x, int y, int z, net.minecraft.world.level.block.state.BlockState state) {
        level.setBlock(new BlockPos(ORIGIN + x, y, z), state, Block.UPDATE_CLIENTS);
    }
}
