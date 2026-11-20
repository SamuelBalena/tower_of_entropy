package com.towerofentropy.combat;
import java.util.*;
import java.nio.file.*;
import com.google.gson.GsonBuilder;
import com.towerofentropy.animation.*;
import com.towerofentropy.client.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT)
public final class A1RuntimeTest {
 private static final String REPORT_DIR=Boolean.getBoolean("toe.a3_3.regression")?"../animation_pipeline/reports/m002_a3_3/regression_a1":Boolean.getBoolean("toe.a3_2.regression")?"../animation_pipeline/reports/m002_a3_2/regression_a1":Boolean.getBoolean("toe.global.regression")?"../animation_pipeline/reports/m002_a3_1/regression_a1":Boolean.getBoolean("toe.a3.regression")?"../animation_pipeline/reports/m002_a3/regression_a1":Boolean.getBoolean("toe.a2.regression")?"../animation_pipeline/reports/m002_a2/regression_a1":Boolean.getBoolean("toe.a1_2.regression")?"../animation_pipeline/reports/m002_a1_2/regression_a1":Boolean.getBoolean("toe.a1_1.regression")?"../animation_pipeline/reports/m002_a1_1/regression_a1":"../animation_pipeline/reports/m002_a1";
 private static final List<Map<String,Object>> checks=new java.util.concurrent.CopyOnWriteArrayList<>(),trace=new java.util.concurrent.CopyOnWriteArrayList<>();
 private static final String[] cases={"combo","pause","heavy","cancel_light","cancel_heavy","iframe","natural","remap","context","natural_heavy","damage_light","damage_heavy","light_then_heavy","heavy_then_light","unsupported","shield","menu","chat","combo_damage","combo_restart","invalid_roll","death"};
 private static net.minecraft.world.entity.monster.Vindicator target;
 private static int title,ticks,quit,visit;private static boolean opening,done;private static long draws;
 private static com.mojang.blaze3d.platform.InputConstants.Key oldKey;
 private static long lastVisualImpact=-1;
 private static void check(String name,boolean pass,Object detail){checks.add(Map.of("test",name,"pass",pass,"detail",String.valueOf(detail)));com.mojang.logging.LogUtils.getLogger().info("TOE_A1 {} {} {}",name,pass,detail);}
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
  if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("toe.a1.test"))return;var mc=Minecraft.getInstance();
  if(done){if(++quit==30)mc.stop();return;}
  if(!opening&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null&&++title>60){opening=true;mc.options.pauseOnLostFocus=false;mc.createWorldOpenFlows().loadLevel(mc.screen,System.getProperty("toe.qa.world","TOE_R4"));}
  if(mc.player==null||mc.level==null||mc.getOverlay()!=null)return;++ticks;mc.setScreen(null);if(Boolean.getBoolean("toe.global.regression"))mc.mouseHandler.releaseMouse();
  var observed=CombatController.status(mc.player.getUUID());
  if(observed!=null&&observed.impactTick()>=observed.startedTick()&&observed.startedTick()>=0&&observed.impactTick()!=lastVisualImpact){
   lastVisualImpact=observed.impactTick();long delta=observed.impactTick()-observed.startedTick();
   String expected=delta==4?"UAL_sword":delta==5?"UAL_sword_b":delta==14?"UAL_sword_c":delta==12?"UAL_heavy":"invalid";
   var c=PlayerRendererR3.controller();check("visual_impact_sync_"+lastVisualImpact,c!=null&&expected.equals(c.getAnimationName())&&Math.abs(c.getAnimationTime()-delta/20f)<=.15f,"clip="+(c==null?"none":c.getAnimationName())+" seconds="+(c==null?-1:c.getAnimationTime())+" serverDelta="+delta);
  }
  if(ticks==5){preview(mc);mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);}
  if((visit>0||Boolean.getBoolean("toe.a1.restart"))&&ticks==75){check(visit>0?"reconnect_clean":"restart_clean",ClientCombatState.mode==CombatController.Mode.AUTO&&CombatController.mode(mc.player.getUUID())==CombatController.Mode.AUTO&&(CombatController.status(mc.player.getUUID())==null||!CombatController.status(mc.player.getUUID()).attacking())&&PlayerRendererR3.enabled&&PlayerRendererR3.drawCalls>0,"default AUTO/R3, no stale actions");finish();return;}
  if(visit>0||Boolean.getBoolean("toe.a1.restart"))return;
  if(Boolean.getBoolean("toe.a1.cold")){
   if(ticks==80)server(p->{try{fixture(p);CombatController.disable(p);LocomotionController.stop(p);}catch(Exception ex){check("cold_fixture",false,ex);}});
   if(ticks==100)server(p->{try{CombatController.enable(p);check("cold_input_accepted",CombatController.requestAttack(p,false).equals("accepted"),"same server task as enable, teleport stabilized");}catch(Exception ex){check("cold_setup",false,ex);}});
   if(ticks==130)server(p->{var s=CombatController.status(p.getUUID());check("cold_attack_completed",s!=null&&s.accepted()==1&&s.impacts()==1&&!s.attacking(),s);});
   if(ticks==145)finish();return;
  }
  if(Boolean.getBoolean("toe.a1.visual")){
   if(ticks==80)server(p->{try{fixture(p);}catch(Exception ex){check("visual_fixture",false,ex);}});
   if(ticks>=120&&ticks<210){int phase=ticks-120;if(phase%10==0)PlayerRendererR3.pose("heavy",phase*7/240f);if(phase%10==5)shot("heavy-phase-"+(phase/10));}
   if(ticks==215)PlayerRendererR3.pose("sword_b",.25f);
   if(ticks==220)shot("light-B-impact");
   if(ticks==225)PlayerRendererR3.pose("sword_c",.7f);
   if(ticks==230)shot("light-C-impact");
   if(ticks==235)PlayerRendererR3.pose("sword",.2f);
   if(ticks==240)shot("light-A-impact");
   if(ticks==245){check("heavy_visual_render",PlayerRendererR3.errors==0&&PlayerRendererR3.attachmentDraws>0,"R3 plus original wrist attachment");PlayerRendererR3.select(true);finish();}return;
  }
  if(ticks<80)return;int n=(ticks-80)/110,phase=(ticks-80)%110;
  if(n>=cases.length){int tail=ticks-80-cases.length*110;if(tail==0)server(p->{try{fixture(p);}catch(Exception ex){check("visual_fixture",false,ex);}});if(tail<120){if(tail>=30){int visualPhase=tail-30;if(visualPhase%10==0)PlayerRendererR3.pose("heavy",Math.min(70/30f,visualPhase*7/240f));if(visualPhase%10==5)shot("heavy-phase-"+(visualPhase/10));}return;}PlayerRendererR3.select(true);mc.level.disconnect();mc.clearLevel(new TitleScreen());opening=false;title=0;ticks=0;visit=1;return;}String name=cases[n];
  if(phase==0){draws=PlayerRendererR3.drawCalls;server(p->{try{fixture(p);if(name.equals("unsupported"))p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_PICKAXE));if(name.equals("shield"))p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));if(name.startsWith("damage")||name.equals("combo_damage")){target=new net.minecraft.world.entity.monster.Vindicator(net.minecraft.world.entity.EntityType.VINDICATOR,p.serverLevel());target.setPos(Boolean.getBoolean("toe.a3_3.regression")?.5:1.5,100,Boolean.getBoolean("toe.a3_3.regression")?3:2.5);if(Boolean.getBoolean("toe.a3_3.regression"))target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);target.setNoAi(true);target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);p.serverLevel().addFreshEntity(target);}}catch(Exception ex){check(name+"_fixture",false,ex);}});}
  if(phase==30){
   if(name.equals("remap")){oldKey=mc.options.keyAttack.getKey();mc.options.keyAttack.setKey(com.mojang.blaze3d.platform.InputConstants.getKey(org.lwjgl.glfw.GLFW.GLFW_KEY_R,0));KeyMapping.resetMapping();}
   if(name.equals("natural")||name.equals("remap")||name.equals("natural_heavy"))natural(mc,name.equals("natural_heavy"));
   else if(name.equals("context"))context(mc);
   else if(name.equals("unsupported")||name.equals("shield")||name.equals("menu")||name.equals("chat"))contextProbe(mc,name);
   else if(name.equals("iframe"))ActionNetwork.requestRoll(0,1);
   else ActionNetwork.requestAttack(name.equals("heavy")||name.equals("cancel_heavy")||name.equals("damage_heavy")||name.equals("heavy_then_light"));
  }
  if(phase==33&&name.startsWith("combo"))ActionNetwork.requestAttack(false);
  if(phase==36&&name.startsWith("combo"))ActionNetwork.requestAttack(false);
  if(phase==37&&name.equals("combo"))ActionNetwork.requestAttack(false);
  if(phase==31&&(name.equals("cancel_light")||name.equals("cancel_heavy"))){if(name.equals("cancel_light"))ActionNetwork.requestAttack(false);ActionNetwork.requestRoll(0,1);}
  if(phase==31&&name.equals("invalid_roll"))ActionNetwork.requestRoll(Float.NaN,1);
  if(phase==(Boolean.getBoolean("toe.a3_3.regression")?94:90)&&name.equals("combo_restart"))ActionNetwork.requestAttack(false);
  if(phase==31&&name.equals("death"))server(ServerPlayer::kill);
  if(phase==50&&name.equals("death"))mc.player.respawn();
  if(phase==75&&name.equals("pause"))ActionNetwork.requestAttack(false);
  if(phase==45&&name.equals("light_then_heavy"))ActionNetwork.requestAttack(true);
  if(phase==80&&name.equals("heavy_then_light"))ActionNetwork.requestAttack(false);
  if(phase>=30&&phase<100)server(p->{var s=CombatController.status(p.getUUID());if(s!=null)trace.add(Map.of("case",name,"tick",p.serverLevel().getGameTime(),"state",s,"action",CombatController.actionStatus(p)));if(name.equals("iframe")){var r=RollController.status(p.getUUID());if(r!=null&&r.started()){long age=p.serverLevel().getGameTime()-r.startTick();if(age==2||age==3||age==9||age==10)iframe(p,age);}}});
  if(phase==39&&(name.equals("combo")||name.equals("heavy")))shot(name+"-active");
  if(phase==55&&name.equals("combo"))shot("combo-C");
  if(phase==(Boolean.getBoolean("toe.a3_3.regression")&&name.equals("combo_restart")?108:100)){
   if(name.equals("remap")){mc.options.keyAttack.setKey(oldKey);KeyMapping.resetMapping();}
   check(name+"_render",PlayerRendererR3.errors==0&&(name.equals("shield")||PlayerRendererR3.drawCalls>draws),"draws="+(PlayerRendererR3.drawCalls-draws));
   if(name.equals("death")){check("respawn_client_mode_cleared",ClientCombatState.clip().isEmpty()&&!ClientRollMotion.busy()&&ClientCombatState.mode==CombatController.Mode.AUTO,"no stale action; AUTO may re-enable a sword retained by keepInventory");server(p->{var after=CombatController.status(p.getUUID());check("respawn_server_mode_cleared",after==null||!after.attacking(),"no stale action; retained equipment may create an idle session");});return;}
   server(p->{var s=CombatController.status(p.getUUID());int count=name.equals("combo_restart")?4:name.startsWith("combo")?3:name.equals("pause")||name.equals("light_then_heavy")||name.equals("heavy_then_light")?2:List.of("iframe","context","unsupported","shield","menu","chat").contains(name)?0:1;
    if(name.startsWith("damage")||name.equals("combo_damage"))check(name+"_raw_damage",target!=null&&Math.abs(target.getHealth()-(name.equals("combo_damage")?88:name.equals("damage_heavy")?94:96))<.01,"health="+(target==null?-1:target.getHealth()));
    check(name+"_accepted",s.accepted()==count,s);
    if(name.startsWith("cancel")){check(name+"_no_pending_damage",s.impacts()==0,s);var r=RollController.status(p.getUUID());check(name+"_physical_unchanged",r!=null&&!r.busy()&&Math.abs(r.distance()-2.5)<.08,r);}
    else check(name+"_single_impacts",s.impacts()==count,s);
    check(name+"_recovery",!s.attacking(),s);
   });
  }
 }
 private static void contextProbe(Minecraft mc,String name){
  if(name.equals("menu"))mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
  if(name.equals("chat"))mc.setScreen(new net.minecraft.client.gui.screens.ChatScreen(""));
  mc.hitResult=net.minecraft.world.phys.BlockHitResult.miss(mc.player.position(),net.minecraft.core.Direction.UP,mc.player.blockPosition());
  boolean use=name.equals("shield");var e=new net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered(use?1:0,use?mc.options.keyUse:mc.options.keyAttack,net.minecraft.world.InteractionHand.MAIN_HAND);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(e);check(name+"_vanilla_or_ui_preserved",!e.isCanceled(),"Forge event not consumed");mc.setScreen(null);
 }
 private static void natural(Minecraft mc,boolean heavy){
  var key=heavy?mc.options.keyUse:mc.options.keyAttack;
  mc.hitResult=new net.minecraft.world.phys.BlockHitResult(mc.player.position(),net.minecraft.core.Direction.UP,mc.player.blockPosition(),true);
  // Real Forge event with MISS target, remappable mapping and duplicate held invocation.
  mc.hitResult=net.minecraft.world.phys.BlockHitResult.miss(mc.player.position(),net.minecraft.core.Direction.UP,mc.player.blockPosition());
  var e=new net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered(heavy?1:0,key,net.minecraft.world.InteractionHand.MAIN_HAND);
  net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(e);check("input_consumed_"+key.getKey().getName(),e.isCanceled()&&!e.shouldSwingHand(),"Forge mapping event");
  var held=new net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered(heavy?1:0,key,net.minecraft.world.InteractionHand.MAIN_HAND);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(held);
 }
 private static void context(Minecraft mc){
  mc.hitResult=new net.minecraft.world.phys.BlockHitResult(mc.player.position(),net.minecraft.core.Direction.UP,mc.player.blockPosition(),false);
  for(boolean use:new boolean[]{false,true}){var e=new net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered(use?1:0,use?mc.options.keyUse:mc.options.keyAttack,net.minecraft.world.InteractionHand.MAIN_HAND);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(e);check(use?"block_use_preserved":"mining_preserved",!e.isCanceled(),"block hit");}
 }
 private static void iframe(ServerPlayer p,long age){
  var zombie=new net.minecraft.world.entity.monster.Zombie(net.minecraft.world.entity.EntityType.ZOMBIE,p.serverLevel());
  var combat=new net.minecraftforge.event.entity.living.LivingAttackEvent(p,p.damageSources().mobAttack(zombie),2);
  net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(combat);check("iframe_combat_"+age,combat.isCanceled()==(age>=3&&age<10),"server ticks");
  var arrow=new net.minecraft.world.entity.projectile.Arrow(p.serverLevel(),zombie);var projectile=new net.minecraftforge.event.entity.living.LivingAttackEvent(p,p.damageSources().arrow(arrow,zombie),2);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(projectile);check("iframe_hostile_projectile_"+age,projectile.isCanceled()==(age>=3&&age<10),"hostile owner");
  for(var source:List.of(p.damageSources().lava(),p.damageSources().fall(),p.damageSources().fellOutOfWorld())){var env=new net.minecraftforge.event.entity.living.LivingAttackEvent(p,source,2);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(env);check("environment_"+age+"_"+source.getMsgId(),!env.isCanceled(),"not canceled");}
 }
 private static void fixture(ServerPlayer p)throws Exception{
  CombatController.disable(p);LocomotionController.stop(p);var level=p.serverLevel();p.teleportTo(level,.5,100,.5,0,0);p.setDeltaMovement(0,0,0);p.setHealth(20);p.hurtTime=0;p.getAbilities().flying=false;p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
  for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)for(int y=99;y<=104;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),(y==99?Blocks.STONE:Blocks.AIR).defaultBlockState());
  for(var entity:level.getEntities((net.minecraft.world.entity.Entity)null,p.getBoundingBox().inflate(16)))if(!(entity instanceof net.minecraft.world.entity.player.Player))entity.discard();
  for(var slot:net.minecraft.world.entity.EquipmentSlot.values())p.setItemSlot(slot,ItemStack.EMPTY);p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));CombatController.enable(p);
 }
 @SuppressWarnings("unchecked") private static void preview(Minecraft mc){try{var info=mc.getConnection().getPlayerInfo(mc.player.getUUID());var f=info.getClass().getDeclaredField("textureLocations");f.setAccessible(true);var map=(Map<Object,Object>)f.get(info);map.put(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN,new net.minecraft.resources.ResourceLocation("minecraft","textures/entity/player/wide/steve.png"));map.remove(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.CAPE);var s=info.getClass().getDeclaredField("skinModel");s.setAccessible(true);s.set(info,"default");var t=info.getClass().getDeclaredField("pendingTextures");t.setAccessible(true);t.setBoolean(info,true);}catch(Exception ex){check("preview",false,ex);}}
 private static void shot(String name){net.minecraft.client.Screenshot.grab(Path.of(REPORT_DIR).toFile(),name+".png",Minecraft.getInstance().getMainRenderTarget(),c->{});}
 private static void server(java.util.function.Consumer<ServerPlayer> work){var mc=Minecraft.getInstance();var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);if(p!=null)work.accept(p);});}
 private static void finish(){try{Files.writeString(Path.of(REPORT_DIR+"/"+(Boolean.getBoolean("toe.a1.restart")?"RESTART":Boolean.getBoolean("toe.a1.visual")?"VISUAL":Boolean.getBoolean("toe.a1.cold")?"COLD":"RUNTIME")+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("checks",checks,"trace",trace,"renderErrors",PlayerRendererR3.errors)));}catch(Exception ex){throw new RuntimeException(ex);}done=true;}
}
