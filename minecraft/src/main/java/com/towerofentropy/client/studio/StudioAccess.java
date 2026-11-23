package com.towerofentropy.client.studio;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT)
public final class StudioAccess {
    private static final KeyMapping OPEN = new KeyMapping("key.towerofentropy.animation_studio",
            KeyConflictContext.IN_GAME, com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F8, "key.categories.towerofentropy");

    @Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(OPEN); }
    }

    @SubscribeEvent public static void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("toe_animstudio").executes(context -> {
            Minecraft.getInstance().tell(StudioAccess::open);
            return 1;
        }));
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && OPEN.consumeClick()) {
            if (Minecraft.getInstance().screen == null) open();
        }
    }

    static void open() {
        var client = Minecraft.getInstance();
        if (!(client.screen instanceof AnimationStudioScreen))
            client.setScreen(new AnimationStudioScreen(client.screen instanceof net.minecraft.client.gui.screens.ChatScreen ? null : client.screen));
    }
}
