package com.towerofentropy.combat;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.towerofentropy.client.*;
import com.towerofentropy.animation.ActionNetwork;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Test driver only. No activation command, circuit or saved activation marker. */
@Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT)
public final class GlobalRuntimeTest {
    static final Path OUT=Path.of((Boolean.getBoolean("toe.a3_3.regression")?"../animation_pipeline/reports/m002_a3_3/global":Boolean.getBoolean("toe.a3_2.regression")?"../animation_pipeline/reports/m002_a3_2/global":"../animation_pipeline/reports/m002_a3_1")+(Boolean.getBoolean("toe.global.packaged")?"/packaged":""));
    static final List<Map<String,Object>> checks=new ArrayList<>();
    static final String survival="Global Survival "+System.getProperty("toe.global.session","one");
    static final String creative="Global Creative "+System.getProperty("toe.global.session","one");
    static int visit,ticks,title,quit;static boolean opening,done;static long before;
    static double rollX,rollZ;
    static void check(String name,boolean pass,Object detail){checks.add(Map.of("test",name,"pass",pass,"detail",String.valueOf(detail)));com.mojang.logging.LogUtils.getLogger().info("TOE_GLOBAL {} {} {}",name,pass,detail);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e)throws Exception{
        if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("toe.global.test"))return;
        var mc=Minecraft.getInstance();
        if(done){if(++quit==25)mc.stop();return;}
        if(!opening&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null&&++title>50){
            opening=true;mc.options.pauseOnLostFocus=false;
            String name=visit==1?creative:survival;
            if(Files.exists(Path.of("saves",name,"level.dat")))mc.createWorldOpenFlows().loadLevel(mc.screen,name);
            else mc.createWorldOpenFlows().createFreshLevel(name,new LevelSettings(name,visit==1?GameType.CREATIVE:GameType.SURVIVAL,false,net.minecraft.world.Difficulty.PEACEFUL,false,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(visit==1?912341:521779,false,false),WorldPresets::createNormalWorldDimensions);
        }
        if(mc.player==null||mc.level==null||mc.getOverlay()!=null)return;
        ++ticks;
        if(ticks==20){mc.setScreen(null);mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);before=PlayerRendererR3.drawCalls;}
        if(ticks==80){
            if(Boolean.getBoolean("toe.global.packaged"))check("visit_"+visit+"_packaged_source",PlayerRendererR3.class.getProtectionDomain().getCodeSource().getLocation().toString().contains(".jar"),PlayerRendererR3.class.getProtectionDomain().getCodeSource().getLocation());
            check("visit_"+visit+"_defaults",PlayerRendererR3.enabled&&ClientCombatState.mode==CombatController.Mode.AUTO&&!PlayerRendererR3.laboratory(),"normal defaults without commands");
            check("visit_"+visit+"_network",ActionNetwork.ready(mc.getConnection().getConnection()),"integrated server protocol present");
            check("visit_"+visit+"_natural_skin",PlayerRendererR3.errors==0,mc.player.getModelName()+" draws="+(PlayerRendererR3.drawCalls-before)+" fallback="+PlayerRendererR3.fallbackReason);
            skin(mc,"default","wide/steve");before=PlayerRendererR3.drawCalls;
        }
        if(ticks==120){
            check("visit_"+visit+"_classic",PlayerRendererR3.drawCalls>before&&PlayerRendererR3.errors==0,"Classic R3 draws="+(PlayerRendererR3.drawCalls-before));
            Files.createDirectories(OUT);Screenshot.grab(OUT.toFile(),"visit-"+visit+"-classic.png",mc.getMainRenderTarget(),m->{});
            skin(mc,"slim","slim/alex");before=PlayerRendererR3.drawCalls;
        }
        if(ticks==160){
            check("visit_"+visit+"_slim",PlayerRendererR3.drawCalls>before&&PlayerRendererR3.errors==0,"Slim derivative draws="+(PlayerRendererR3.drawCalls-before));
            Screenshot.grab(OUT.toFile(),"visit-"+visit+"-slim.png",mc.getMainRenderTarget(),m->{});
            skin(mc,"default","wide/steve");before=PlayerRendererR3.drawCalls;
        }
        if(Boolean.getBoolean("toe.global.packaged")&&visit==0){
            if(ticks==170){var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().getPlayer(id).setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD)));}
            if(ticks==180)check("packaged_sword_auto",ClientCombatState.available()&&ClientCombatState.enabled,"ordinary equipment observed in AUTO");
            if(ticks==210)ActionNetwork.requestAttack(false);
            if(ticks==218)check("packaged_sword_animation",ClientCombatState.clip().equals("sword")&&PlayerRendererR3.controller().getAnimationName().equals("UAL_sword"),"server intent -> client timeline -> packaged model");
            if(ticks==260){rollX=mc.player.getX();rollZ=mc.player.getZ();ActionNetwork.requestRoll(0,1);}
            if(ticks==268)check("packaged_roll_animation",ClientRollMotion.busy()&&PlayerRendererR3.controller().getAnimationName().equals("UAL_roll"),"server validated Roll");
            if(ticks==300){double distance=Math.hypot(mc.player.getX()-rollX,mc.player.getZ()-rollZ);check("packaged_roll_distance",Math.abs(distance-2.5)<.08&&!ClientRollMotion.busy(),"distance="+distance);}
        }
        if(ticks==(Boolean.getBoolean("toe.global.packaged")&&visit==0?320:200)){
            check("visit_"+visit+"_skin_switch",PlayerRendererR3.drawCalls>before,"R3 resumes after Slim fallback");
            check("visit_"+visit+"_clear",!ClientRollMotion.busy()&&ClientCombatState.clip().isEmpty(),"no stuck action after world entry");
            check("visit_"+visit+"_gamemode",mc.getSingleplayerServer().getWorldData().getGameType()==(visit==1?GameType.CREATIVE:GameType.SURVIVAL),"vanilla world settings");
            mc.level.disconnect();mc.clearLevel(new TitleScreen());visit++;ticks=0;title=0;opening=false;
            if(visit==3){Files.writeString(OUT.resolve("GLOBAL_RUNTIME.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("checks",checks,"worlds",List.of(survival,creative),"note","new Survival, new Creative, reopen Survival; skin injection is QA only; no circuit")));done=true;}
        }
    }
    @SuppressWarnings("unchecked") static void skin(Minecraft mc,String model,String texture)throws Exception{
        var info=mc.getConnection().getPlayerInfo(mc.player.getUUID());
        var f=info.getClass().getDeclaredField("textureLocations");f.setAccessible(true);
        var map=(Map<Object,Object>)f.get(info);map.put(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN,new net.minecraft.resources.ResourceLocation("minecraft","textures/entity/player/"+texture+".png"));map.remove(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.CAPE);
        var s=info.getClass().getDeclaredField("skinModel");s.setAccessible(true);s.set(info,model);
        var t=info.getClass().getDeclaredField("pendingTextures");t.setAccessible(true);t.setBoolean(info,true);
    }
}
