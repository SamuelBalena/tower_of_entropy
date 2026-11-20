package com.towerofentropy;

import net.minecraftforge.fml.common.Mod;

@Mod(TowerOfEntropy.MOD_ID)
public final class TowerOfEntropy {
    public static final String MOD_ID = "towerofentropy";

    public TowerOfEntropy() {
        com.towerofentropy.animation.ActionNetwork.register();
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT,
            com.towerofentropy.client.camera.CameraSettings.SPEC, "towerofentropy-camera-client.toml");
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,
            com.towerofentropy.movement.SprintJump.SPEC, "towerofentropy-jump-server.toml");
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,
            com.towerofentropy.movement.WallSlide.SPEC, "towerofentropy-wall-server.toml");
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT,
            com.towerofentropy.animation.VisualTuning.SPEC,"towerofentropy-visual-client.toml");
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,
            com.towerofentropy.combat.CombatConfig.SPEC, "towerofentropy-combat-server.toml");
    }
}
