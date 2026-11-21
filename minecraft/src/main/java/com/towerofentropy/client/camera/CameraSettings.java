package com.towerofentropy.client.camera;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CameraSettings {
    public static final double MIN_DISTANCE = 2;
    public static final double MAX_DISTANCE = 8;
    public static final double DEFAULT_SHOULDER = 0.9775;
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.IntValue COMPOSITION_VERSION;
    static final ForgeConfigSpec.DoubleValue DISTANCE;
    static final ForgeConfigSpec.IntValue ZOOM_BINDING_VERSION, SHOULDER_BINDING_VERSION;
    static final ForgeConfigSpec.DoubleValue SHOULDER_OFFSET;
    static final ForgeConfigSpec.DoubleValue HEIGHT_OFFSET;
    static final ForgeConfigSpec.BooleanValue RIGHT_SHOULDER;
    static final ForgeConfigSpec.DoubleValue FADE_MINIMUM, FADE_CLOSE, FADE_COMFORTABLE;

    static {
        var builder = new ForgeConfigSpec.Builder();
        DISTANCE = builder.defineInRange("distance", 4.0, MIN_DISTANCE, MAX_DISTANCE);
        SHOULDER_OFFSET = builder.defineInRange("shoulderOffset", DEFAULT_SHOULDER, 0.0, 1.5);
        HEIGHT_OFFSET = builder.defineInRange("heightOffset", 0.25, -0.5, 1.0);
        RIGHT_SHOULDER = builder.define("rightShoulder", true);
        COMPOSITION_VERSION = builder.defineInRange("compositionVersion", 0, 0, 1);
        ZOOM_BINDING_VERSION = builder.defineInRange("zoomBindingVersion", 0, 0, 1);
        SHOULDER_BINDING_VERSION = builder.defineInRange("shoulderBindingVersion", 0, 0, 1);
        builder.push("characterFade");
        FADE_MINIMUM = builder.defineInRange("minimumOpacity", .12, 0.0, 1.0);
        FADE_CLOSE = builder.defineInRange("veryCloseDistance", .95, .25, 1.45);
        FADE_COMFORTABLE = builder.defineInRange("comfortableDistance", 2.35, 1.5, 4.0);
        builder.pop();
        SPEC = builder.build();
    }

    @SubscribeEvent
    public static void load(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() != SPEC || COMPOSITION_VERSION.get() != 0) return;
        if (SHOULDER_OFFSET.get() == 0.85) SHOULDER_OFFSET.set(DEFAULT_SHOULDER);
        COMPOSITION_VERSION.set(1);
        COMPOSITION_VERSION.save();
    }

    private CameraSettings() {}
}
