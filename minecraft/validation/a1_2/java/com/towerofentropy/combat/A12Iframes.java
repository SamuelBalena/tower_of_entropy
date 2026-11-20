package com.towerofentropy.combat;
import java.util.*;
import java.nio.file.*;
import com.google.gson.GsonBuilder;
import com.towerofentropy.animation.ActionKeys;
import com.towerofentropy.client.PlayerRendererR3;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Server-tick boundary checks, immune to client sampling skipping tick 10. */
@Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT)
public final class A12Iframes {
 private static final List<Map<String,Object>> checks=new java.util.concurrent.CopyOnWriteArrayList<>();
 private static final Set<Long> seen=java.util.concurrent.ConcurrentHashMap.newKeySet();
 private static volatile UUID id;
 private static int title,ticks,quit;private static boolean opening,done;
 private static void check(String name,boolean pass,Object detail){checks.add(Map.of("test",name,"pass",pass,"detail",String.valueOf(detail)));com.mojang.logging.LogUtils.getLogger().info("TOE_A12_IFRAMES {} {} {}",name,pass,detail);}
 @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){
  if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("toe.a1_2.iframes"))return;var mc=Minecraft.getInstance();if(done){if(++quit==30)mc.stop();return;}
  if(!opening&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null&&++title>60){opening=true;mc.options.pauseOnLostFocus=false;mc.createWorldOpenFlows().loadLevel(mc.screen,Boolean.getBoolean("toe.a3_3.regression")?"Global Survival one":Boolean.getBoolean("toe.a3_2.regression")?"Global Survival one":Boolean.getBoolean("toe.a3.regression")?"TOE_A3":Boolean.getBoolean("toe.a2.regression")?"TOE_A2":"TOE_A1_2");}
  if(mc.player==null||mc.getOverlay()!=null)return;++ticks;id=mc.player.getUUID();
  if(ticks==5){mc.setScreen(null);A12RuntimeTest.preview(mc);mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);}
  if(ticks==80)mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);try{A12RuntimeTest.fixture(p,true);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.invulnerableTime=0;}catch(Exception ex){throw new RuntimeException(ex);}});
  if(ticks==110)KeyMapping.click(ActionKeys.ROLL.getKey());
  if(ticks==155){check("all_boundaries_seen",seen.containsAll(List.of(2L,3L,9L,10L)),seen);check("renderer_safe",PlayerRendererR3.errors==0,"errors="+PlayerRendererR3.errors);try{Files.writeString(Path.of(Boolean.getBoolean("toe.a3_3.regression")?"../animation_pipeline/reports/m002_a3_3/IFRAMES.json":Boolean.getBoolean("toe.a3_2.regression")?"../animation_pipeline/reports/m002_a3_2/IFRAMES.json":Boolean.getBoolean("toe.a3.regression")?"../animation_pipeline/reports/m002_a3/IFRAMES.json":Boolean.getBoolean("toe.a2.regression")?"../animation_pipeline/reports/m002_a2/IFRAMES.json":"../animation_pipeline/reports/m002_a1_2/IFRAMES.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("checks",checks,"serverAges",seen)));}catch(Exception ex){throw new RuntimeException(ex);}done=true;}
 }
 @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){
  if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("toe.a1_2.iframes")||id==null)return;
  var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();if(server==null)return;var p=server.getPlayerList().getPlayer(id);if(p==null)return;var r=RollController.status(id);if(r==null||!r.busy()||!r.started())return;
  long age=p.serverLevel().getGameTime()-r.startTick();if(!List.of(2L,3L,9L,10L).contains(age)||!seen.add(age))return;
  var zombie=new net.minecraft.world.entity.monster.Zombie(net.minecraft.world.entity.EntityType.ZOMBIE,p.serverLevel());var arrow=new net.minecraft.world.entity.projectile.Arrow(p.serverLevel(),zombie);
  var melee=new net.minecraftforge.event.entity.living.LivingAttackEvent(p,p.damageSources().mobAttack(zombie),2);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(melee);check("melee_"+age,melee.isCanceled()==(age==3||age==9),"start="+r.startTick()+" tick="+p.serverLevel().getGameTime());
  var projectile=new net.minecraftforge.event.entity.living.LivingAttackEvent(p,p.damageSources().arrow(arrow,zombie),2);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(projectile);check("projectile_"+age,projectile.isCanceled()==(age==3||age==9),"hostile owner");
  for(var source:List.of(p.damageSources().lava(),p.damageSources().fall(),p.damageSources().fellOutOfWorld())){var env=new net.minecraftforge.event.entity.living.LivingAttackEvent(p,source,2);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(env);check("environment_"+age+"_"+source.getMsgId(),!env.isCanceled(),"preserved");}
  if(age==3||age==10){float health=p.getHealth();p.hurt(p.damageSources().mobAttack(zombie),2);check("actual_health_"+age,Math.abs(p.getHealth()-(age==3?health:health-2))<.001,"before="+health+" after="+p.getHealth());}
 }
}
