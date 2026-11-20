package com.towerofentropy.renderlab;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Local integrated-server laboratory commands; no packet accepting damage or entity coordinates. */
@Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT)
public final class RenderLabCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent e){
        var root=Commands.literal("toe_renderlab").requires(s->s.hasPermission(2));
        root.then(Commands.literal("skinned").executes(c->{Minecraft.getInstance().execute(()->SkinnedSteveRenderer.select(true));c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("Renderlab R2 ativo. /toe_renderlab off restaura o render anterior."),false);return 1;}));
        root.then(Commands.literal("off").executes(c->{Minecraft.getInstance().execute(()->SkinnedSteveRenderer.select(false));c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("Renderlab R2 desligado; render anterior restaurado."),false);return 1;}));
        for(String name:new String[]{"idle","jog","roll","sword"})root.then(Commands.literal(name).executes(c->{Minecraft.getInstance().execute(()->SkinnedSteveRenderer.pose(name,-1));return 1;}));
        e.getDispatcher().register(root);
    }
}
