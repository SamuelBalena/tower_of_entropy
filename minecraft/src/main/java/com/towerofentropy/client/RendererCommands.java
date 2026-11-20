package com.towerofentropy.client;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import com.mojang.brigadier.CommandDispatcher;
@Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT)
public final class RendererCommands {
 public static CommandDispatcher<CommandSourceStack> dispatcher;
 @SubscribeEvent public static void register(RegisterClientCommandsEvent e){
  if(PlayerRendererR3.laboratory())return;dispatcher=e.getDispatcher();var root=Commands.literal("toe_renderlab");
  root.then(Commands.literal("skinned").executes(c->{PlayerRendererR3.select(true);return 1;}));
  root.then(Commands.literal("off").executes(c->{PlayerRendererR3.select(false);return 1;}));
  root.then(Commands.literal("status").executes(c->{net.minecraft.client.Minecraft.getInstance().player.displayClientMessage(net.minecraft.network.chat.Component.literal("R3="+PlayerRendererR3.enabled+" fallback="+PlayerRendererR3.fallbackReason+" wallSlide="+ClientWallVisual.active()),false);return 1;}));
  for(String clip:new String[]{"idle","jog","roll","sword"})root.then(Commands.literal(clip).executes(c->{PlayerRendererR3.pose(clip,-1);return 1;}));
  dispatcher.register(root);
  var profiles=Commands.literal("animprofile");
  for(var profile:ClientWallVisual.Profile.values()){
   profiles.then(Commands.literal(profile.name().toLowerCase(java.util.Locale.ROOT)).executes(c->{
    ClientWallVisual.profile(profile);
    var player=net.minecraft.client.Minecraft.getInstance().player;
    if(player!=null)player.displayClientMessage(net.minecraft.network.chat.Component.literal("Perfil de animacao: "+profile.name()),false);
    return 1;
   }));
  }
  dispatcher.register(Commands.literal("toe").then(profiles));
 }
}
