package com.towerofentropy.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Server timeline; no local damage prediction. */
@Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT)
public final class ClientCombatState {
    public static boolean enabled;
    public static com.towerofentropy.combat.CombatController.Mode mode=com.towerofentropy.combat.CombatController.Mode.AUTO;
    public static boolean available(){var mc=Minecraft.getInstance();var p=mc.player;return mc.getConnection()!=null&&com.towerofentropy.animation.ActionNetwork.ready(mc.getConnection().getConnection())&&mode!=com.towerofentropy.combat.CombatController.Mode.OFF&&p!=null&&(mode==com.towerofentropy.combat.CombatController.Mode.ON||p.getMainHandItem().getItem() instanceof net.minecraft.world.item.SwordItem);}
    private static String clip="";
    private static boolean heavyAir;
    private static long started;
    private static int duration;
    public static void accept(boolean on,String animation,int ticks,long start,boolean airborneHeavy){
        heavyAir=airborneHeavy;
        if(animation.isEmpty())ClientAttackMotion.clear();
        enabled=on;clip=animation;duration=ticks;started=start;
    }
    public static boolean heavyAir(){return heavyAir&&!clip().isEmpty();}
    public static long started(){return started;}
    public static void canceledIntoRoll(){clip="";ClientAttackMotion.clear();}
    public static float time(float partial){var l=Minecraft.getInstance().level;return l==null?0:Math.max(0,l.getGameTime()-started+partial)/20f;}
    public static String clip(){return !clip.isEmpty()&&time(0)<duration/20f?clip:"";}
    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut e){enabled=false;clip="";mode=com.towerofentropy.combat.CombatController.Mode.AUTO;}
}
