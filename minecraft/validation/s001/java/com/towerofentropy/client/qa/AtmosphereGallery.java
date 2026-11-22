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
        new Scene("fog_after_sunrise", "forest", 24020, 0)
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
        boolean elevated = scene.name().equals("aurora_clear_air");
        int cameraHeight = elevated ? 244 : BASE + 2;
        if (elevated) {
            for (int x = -4; x <= 4; ++x) {
                for (int z = -4; z <= 4; ++z) place(level, x, cameraHeight - 1, z, Blocks.SNOW_BLOCK);
            }
        }
        player.teleportTo(level, ORIGIN + 0.5, cameraHeight, 0.5, -91.0F,
                elevated ? -27.0F : scene.name().startsWith("aurora") ? -15.0F : -6.0F);
        var biome = level.getBiome(BlockPos.containing(ORIGIN + 0.5, cameraHeight, 0.5)).unwrapKey().orElseThrow();
        if (!biome.location().getPath().equals(scene.biome())) {
            throw new IllegalStateException("Expected biome " + scene.biome() + ", received " + biome.location());
        }
        LogUtils.getLogger().info("TOE_ATMOS_SCENE {} biome={} time={} weather={} cells={}",
                scene.name(), biome.location(), scene.time(), scene.weather(), replaced);
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

    private static void place(ServerLevel level, int x, int y, int z, Block block) {
        level.setBlock(new BlockPos(ORIGIN + x, y, z), block.defaultBlockState(), Block.UPDATE_CLIENTS);
    }
}
