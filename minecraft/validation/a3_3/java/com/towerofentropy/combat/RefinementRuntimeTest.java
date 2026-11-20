package com.towerofentropy.combat;
import java.util.*;import java.nio.file.*;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import com.towerofentropy.client.*;import com.towerofentropy.animation.*;
import net.minecraft.client.*;import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.level.ServerPlayer;import net.minecraft.world.entity.*;
import net.minecraft.core.BlockPos;import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;import net.minecraftforge.fml.common.Mod;import net.minecraftforge.eventbus.api.SubscribeEvent;import net.minecraftforge.event.TickEvent;
@Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT)
public final class RefinementRuntimeTest {
 static final Path OUT=Path.of("../animation_pipeline/reports/m002_a3_3");
 static final String[] CASES=Boolean.getBoolean("toe.a3_3.quick")?new String[]{"A","combo","heavy","jump_a","hold_jump","autojump","jump_tamper"}:new String[]{"A","combo","heavy","heavy_sprint","wall","edge","cancel_a","cancel_heavy","jump_a","jump_b","jump_c","jump_heavy","hold_jump","remap_jump","autojump","jump_tamper","knockback","world_up","fall","roll_sprint","slim"};
 static final List<Map<String,Object>> checks=new java.util.concurrent.CopyOnWriteArrayList<>(),frames=new ArrayList<>();
 static int title,ticks,index=-1,quit,visit,locked,releaseAge=-1,rejectedBefore;static boolean opening,done,sawAttack;static double x,z,maxDuring,maxHeld,maxAfter;static String scene;static long actionStart=-1,draws;static InputConstants.Key oldJump;static boolean oldAuto;
 static void check(String n,boolean b,Object d){checks.add(Map.of("test",n,"pass",b,"detail",String.valueOf(d)));com.mojang.logging.LogUtils.getLogger().info("TOE_A3_3 {} {} {}",n,b,d);}
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e)throws Exception{
  if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("toe.a3_3.test")||Boolean.getBoolean("toe.a1_2.iframes"))return;var mc=Minecraft.getInstance();if(done){if(++quit==25)mc.stop();return;}
  if(!opening&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null&&++title>60){opening=true;mc.options.pauseOnLostFocus=false;mc.createWorldOpenFlows().loadLevel(mc.screen,"Global Survival one");}
  if(mc.player==null||mc.level==null||mc.getOverlay()!=null)return;ticks++;if(mc.screen!=null)mc.setScreen(null);if(!mc.mouseHandler.isMouseGrabbed())mc.mouseHandler.grabMouse();mc.player.setYRot(0);mc.player.setXRot(0);
  if(ticks==5){A12RuntimeTest.preview(mc);mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);mc.options.hideGui=true;Files.createDirectories(OUT);oldJump=mc.options.keyJump.getKey();oldAuto=mc.options.autoJump().get();}
  if(visit>0){if(ticks==70){check("reconnect_clear",!ClientAttackMotion.active()&&!ClientRollMotion.busy()&&!ClientAttackJumpGate.waitingRelease(),"no stale commitment/jump latch");finish(mc);}return;}
  if(ticks<90)return;int i=(ticks-90)/150,phase=(ticks-90)%150;
  if(i>=CASES.length){release(mc);restore(mc);mc.level.disconnect();mc.clearLevel(new TitleScreen());opening=false;title=0;ticks=0;visit=1;return;}
  scene=CASES[i];
  if(i!=index){index=i;release(mc);restore(mc);locked=0;releaseAge=-1;sawAttack=false;maxDuring=100;maxHeld=100;maxAfter=100;draws=PlayerRendererR3.drawCalls;mc.options.autoJump().set(scene.equals("autojump"));
   if(scene.equals("remap_jump")){mc.options.keyJump.setKey(InputConstants.Type.KEYSYM.getOrCreate(82));KeyMapping.resetMapping();}
   if(scene.equals("slim"))slim(mc);server(p->{try{fixture(p,scene);rejectedBefore=AttackJumpGuard.rejected(p.getUUID());}catch(Exception ex){check("fixture_"+scene,false,ex);}});
  }
  boolean moving=scene.equals("heavy_sprint")&&phase>=15&&phase<105||scene.equals("autojump")&&phase>=80&&phase<110;
  mc.options.keyUp.setDown(moving);mc.options.keySprint.setDown(scene.equals("heavy_sprint")&&moving);
  boolean jump=switch(scene){case "jump_a"->phase>=32&&phase<=35;case "jump_b"->phase>=42&&phase<=47;case "jump_c"->phase>=55&&phase<=61;case "jump_heavy"->phase>=35&&phase<=42;case "hold_jump","remap_jump"->phase>=35&&phase<=90||phase>=95&&phase<=97;default->false;};
  KeyMapping.set(mc.options.keyJump.getKey(),jump);
  if(phase==29){x=mc.player.getX();z=mc.player.getZ();}
  if(phase==30){if(scene.equals("roll_sprint"))KeyMapping.click(ActionKeys.ROLL.getKey());else if(scene.equals("wall"))ActionNetwork.requestAttack(true);else KeyMapping.click(heavy(scene)?mc.options.keyUse.getKey():mc.options.keyAttack.getKey());}
  if(Set.of("combo","jump_b","jump_c").contains(scene)&&phase==35)KeyMapping.click(mc.options.keyAttack.getKey());
  if(Set.of("combo","jump_c").contains(scene)&&phase==38)KeyMapping.click(mc.options.keyAttack.getKey());
  if(scene.startsWith("cancel_")&&phase==33)KeyMapping.click(ActionKeys.ROLL.getKey());
  if(scene.equals("autojump")&&phase>=32&&phase<=37){var f=net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField(net.minecraft.client.player.LocalPlayer.class,"f_108613_");f.setInt(mc.player,1);}
  if(scene.equals("jump_tamper")&&phase==33)mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(mc.player.getX(),mc.player.getY()+.42,mc.player.getZ(),false));
  if(scene.equals("knockback")&&phase==34)server(p->{p.hurt(p.damageSources().generic(),1);p.knockback(.35,1,0);});
  if(scene.equals("world_up")&&phase==34)server(p->{p.setDeltaMovement(0,.48,0);p.hurtMarked=true;});
  if(scene.equals("fall")&&phase==34)server(p->{p.teleportTo(p.serverLevel(),p.getX(),104,p.getZ(),0,0);p.setOnGround(false);});
  if(!ClientCombatState.clip().isEmpty()){actionStart=ClientCombatState.started();sawAttack=true;}
  if(ClientAttackMotion.active()){locked++;maxDuring=Math.max(maxDuring,mc.player.getY());if(phase==34||phase==44)check(scene+"_wasd",mc.player.input.forwardImpulse==0&&mc.player.input.leftImpulse==0,"committed input");}
  else if(sawAttack&&releaseAge<0)releaseAge=(int)(mc.level.getGameTime()-actionStart);
  if(phase>=35&&phase<=91&&(!scene.equals("autojump")||phase<80))maxHeld=Math.max(maxHeld,mc.player.getY());if(phase>=95)maxAfter=Math.max(maxAfter,mc.player.getY());
  if(phase>=30&&phase<=115){var c=PlayerRendererR3.controller();var row=new LinkedHashMap<String,Object>();row.put("case",scene);row.put("phase",phase);row.put("x",mc.player.getX());row.put("y",mc.player.getY());row.put("z",mc.player.getZ());row.put("lock",ClientAttackMotion.active());row.put("jumpInput",mc.player.input.jumping);row.put("jumpHeld",mc.options.keyJump.isDown());row.put("latch",ClientAttackJumpGate.waitingRelease());row.put("clip",c==null?"none":c.getAnimationName());row.put("animationTime",c==null?0:c.getAnimationTime());frames.add(row);}
  if(Set.of(38,43,60,62,64,66,70,90,100).contains(phase))Screenshot.grab(OUT.toFile(),scene+"-"+phase+".png",mc.getMainRenderTarget(),m->{});
  if(phase==125){String name=scene;double moved=Math.hypot(mc.player.getX()-x,mc.player.getZ()-z),during=maxDuring,held=maxHeld,after=maxAfter;int age=releaseAge,count=locked;server(p->verify(p,name,moved,during,held,after,age,count));}
 }
 static boolean heavy(String n){return Set.of("heavy","heavy_sprint","cancel_heavy","jump_heavy","hold_jump","remap_jump").contains(n);}
 static void fixture(ServerPlayer p,String n)throws Exception{LocomotionController.cancelAction(p);A12RuntimeTest.fixture(p,true);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getFoodData().setFoodLevel(20);p.invulnerableTime=0;
  if(n.equals("wall"))for(int xx=-2;xx<=2;xx++)for(int y=100;y<=103;y++)p.serverLevel().setBlockAndUpdate(new BlockPos(xx,y,1),Blocks.STONE.defaultBlockState());
  if(n.equals("edge")){for(int xx=-3;xx<=3;xx++)for(int zz=1;zz<=4;zz++)p.serverLevel().setBlockAndUpdate(new BlockPos(xx,99,zz),Blocks.AIR.defaultBlockState());p.teleportTo(p.serverLevel(),.5,100,.65,0,0);}
  if(n.equals("autojump"))p.serverLevel().setBlockAndUpdate(new BlockPos(0,100,2),Blocks.STONE.defaultBlockState());
 }
 static void verify(ServerPlayer p,String n,double moved,double during,double held,double after,int age,int count){
  var a=CombatController.status(p.getUUID());var motion=AttackMovement.status(p.getUUID());
  check(n+"_renderer",PlayerRendererR3.errors==0,"errors="+PlayerRendererR3.errors);check(n+"_released",!ClientAttackMotion.active()&&!ClientRollMotion.busy(),"no stuck action");
  if(n.equals("roll_sprint")||n.startsWith("cancel_")){var r=RollController.status(p.getUUID());check(n+"_roll",r!=null&&r.started()&&Math.abs(r.distance()-2.5)<.08,r);check(n+"_queue_clear",CombatController.actionStatus(p).queued()==0,CombatController.actionStatus(p));return;}
  check(n+"_authorized",a!=null&&a.accepted()>0&&count>0,a+" locked="+count);
  check(n+"_no_horizontal_reconcile",motion!=null&&motion.corrections()==0,motion);
  if(Set.of("A","combo","heavy","slim").contains(n)){double expected=n.equals("combo")?1.5:n.equals("heavy")?.7:.4;check(n+"_distance",Math.abs(moved-expected)<.035,"measured="+moved+" expected="+expected);}
  if(heavy(n)){check(n+"_recovery",age>=35&&age<=37,"release age="+age+" ticks");check(n+"_impact",a!=null&&a.impacts()==1&&a.impactTick()-a.startedTick()==12,a);}
  if(n.startsWith("jump_")&&!n.equals("jump_tamper")){int hits=n.equals("jump_b")?2:n.equals("jump_c")?3:1;check(n+"_not_cancelled",a!=null&&a.impacts()==hits&&during<100.01,a+" maxY="+during);}
  if(n.equals("hold_jump")||n.equals("remap_jump")){check(n+"_no_buffer",held<100.01,"held maxY="+held);check(n+"_fresh_jump",after>100.3,"new input maxY="+after);}
  if(n.equals("autojump")){check("autojump_blocked",during<100.01&&held<100.01,"maxY="+during+" before moving="+held);check("autojump_exploration",after>100.3,"normal movement maxY="+after);}
  if(n.equals("jump_tamper"))check("server_jump_rejected_without_cancel",AttackJumpGuard.rejected(p.getUUID())>rejectedBefore&&a!=null&&a.impacts()==1&&p.getY()<100.01,"rejections="+AttackJumpGuard.rejected(p.getUUID())+" "+a);
  if(Set.of("knockback","world_up","fall").contains(n))check(n+"_vertical_preserved",during>100.1||held>100.1,"during="+during+" max="+held);
  if(n.equals("wall"))check("wall_collision",p.getZ()<=.701,"pos="+p.position());
  if(n.equals("edge"))check("edge_protection",p.onGround()&&p.getY()>99.9&&p.getZ()<=.8,"pos="+p.position());
  if(n.equals("combo"))check("combo_three_impacts",a!=null&&a.impacts()==3,a);
  if(n.equals("A")){check("defaults_migrated",CombatConfig.MOVEMENT_DEFAULTS_REVISION.get()==1&&CombatConfig.ADVANCE_A.get()==.4&&CombatConfig.ADVANCE_B.get()==.5&&CombatConfig.ADVANCE_C.get()==.6&&CombatConfig.ADVANCE_HEAVY.get()==.7,"server config revision="+CombatConfig.MOVEMENT_DEFAULTS_REVISION.get());check("custom_tuple_preserved",!CombatConfigMigration.legacy(.18,.35,.28,.32),"custom tuple is not migrated");}
 }
 static void server(java.util.function.Consumer<ServerPlayer> work){var mc=Minecraft.getInstance();var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);if(p!=null)work.accept(p);});}
 static void release(Minecraft mc){for(var k:new KeyMapping[]{mc.options.keyUp,mc.options.keySprint,mc.options.keyJump,mc.options.keyAttack,mc.options.keyUse})k.setDown(false);}
 static void restore(Minecraft mc){if(oldJump!=null){mc.options.keyJump.setKey(oldJump);KeyMapping.resetMapping();mc.options.autoJump().set(oldAuto);}}
 @SuppressWarnings("unchecked") static void slim(Minecraft mc)throws Exception{var info=mc.getConnection().getPlayerInfo(mc.player.getUUID());var f=info.getClass().getDeclaredField("textureLocations");f.setAccessible(true);((Map<Object,Object>)f.get(info)).put(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN,new net.minecraft.resources.ResourceLocation("minecraft","textures/entity/player/slim/alex.png"));var s=info.getClass().getDeclaredField("skinModel");s.setAccessible(true);s.set(info,"slim");}
 static void finish(Minecraft mc)throws Exception{restore(mc);Files.writeString(OUT.resolve(Boolean.getBoolean("toe.a3_3.quick")?"QUICK.json":"REFINEMENT_RUNTIME.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("checks",checks,"frames",frames)));done=true;}
}
