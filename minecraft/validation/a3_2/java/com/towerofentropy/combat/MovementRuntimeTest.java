package com.towerofentropy.combat;
import java.util.*;import java.nio.file.*;
import com.google.gson.GsonBuilder;
import com.towerofentropy.client.*;import com.towerofentropy.animation.*;
import net.minecraft.client.*;import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.level.ServerPlayer;import net.minecraft.world.item.*;import net.minecraft.world.entity.*;
import net.minecraft.core.BlockPos;import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;import net.minecraftforge.fml.common.Mod;import net.minecraftforge.eventbus.api.SubscribeEvent;import net.minecraftforge.event.TickEvent;
@Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT)
public final class MovementRuntimeTest {
 static final Path OUT=Path.of("../animation_pipeline/reports/m002_a3_2");
 static final String[] CASES=Boolean.getBoolean("toe.a3_2.directions")?new String[]{"roll_back","roll_left","roll_right","cancel_back","roll_tamper"}:Boolean.getBoolean("toe.a3_2.quick")?new String[]{"roll_sprint","A","combo","heavy","walk_attack","sprint_attack","edge"}:new String[]{"roll_idle","roll_walk","roll_sprint","roll_diag","roll_sprint_diag","roll_wall","A","combo","heavy","walk_attack","sprint_attack","wall","edge","spam","cancel_a","cancel_b","cancel_c","cancel_heavy","knockback","world_push","tamper","turn","firstperson","slim"};
 static final List<Map<String,Object>> checks=new java.util.concurrent.CopyOnWriteArrayList<>(),frames=new ArrayList<>();
 static int title,ticks,index=-1,quit,visit;static boolean opening,done;static double x,z;static int locked;static String scene;static long draws;
 static void check(String n,boolean b,Object d){checks.add(Map.of("test",n,"pass",b,"detail",String.valueOf(d)));com.mojang.logging.LogUtils.getLogger().info("TOE_A3_2 {} {} {}",n,b,d);}
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e)throws Exception{
  if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("toe.a3_2.test")||Boolean.getBoolean("toe.a1_2.iframes"))return;var mc=Minecraft.getInstance();if(done){if(++quit==25)mc.stop();return;}
  if(!opening&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null&&++title>60){opening=true;mc.options.pauseOnLostFocus=false;mc.createWorldOpenFlows().loadLevel(mc.screen,"Global Survival one");}
  if(mc.player==null||mc.level==null||mc.getOverlay()!=null)return;ticks++;if(mc.screen!=null)mc.setScreen(null);if(!mc.mouseHandler.isMouseGrabbed())mc.mouseHandler.grabMouse();mc.player.setYRot(0);mc.player.setXRot("wall".equals(scene)?-85:0);
  if(ticks==5){A12RuntimeTest.preview(mc);mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);mc.options.hideGui=true;Files.createDirectories(OUT);}
  if(visit>0){if(ticks==70){check("reconnect_clear",!ClientAttackMotion.active()&&!ClientRollMotion.busy()&&ClientCombatState.mode==CombatController.Mode.AUTO,"no stuck action after world reload");finish();}return;}
  if(ticks<90)return;int i=(ticks-90)/150,phase=(ticks-90)%150;
  if(i>=CASES.length){release(mc);mc.level.disconnect();mc.clearLevel(new TitleScreen());opening=false;title=0;ticks=0;visit=1;return;}
  scene=CASES[i];
  if(i!=index){index=i;release(mc);locked=0;draws=PlayerRendererR3.drawCalls;mc.options.setCameraType(scene.equals("firstperson")?CameraType.FIRST_PERSON:CameraType.THIRD_PERSON_FRONT);if(scene.equals("slim"))slim(mc);server(p->{try{fixture(p,scene);}catch(Exception ex){check("fixture_"+scene,false,ex);}});}
  boolean walking=Set.of("roll_walk","roll_sprint","roll_diag","roll_sprint_diag","walk_attack","sprint_attack").contains(scene);
  mc.options.keyUp.setDown(walking&&phase>=15&&phase<110);mc.options.keyDown.setDown((scene.equals("roll_back")||scene.equals("cancel_back"))&&phase>=15&&phase<110);mc.options.keyRight.setDown(scene.equals("roll_right")&&phase>=15&&phase<110);mc.options.keyLeft.setDown((scene.contains("diag")||scene.equals("roll_left"))&&phase>=15&&phase<110);mc.options.keySprint.setDown(scene.contains("sprint")&&phase>=15&&phase<110);
  if(phase==29){x=mc.player.getX();z=mc.player.getZ();}
  if(phase==30){if(scene.startsWith("roll_"))KeyMapping.click(ActionKeys.ROLL.getKey());else if(scene.equals("wall"))ActionNetwork.requestAttack(true);else KeyMapping.click(scene.equals("heavy")||scene.equals("cancel_heavy")?mc.options.keyUse.getKey():mc.options.keyAttack.getKey());}
  if((scene.equals("combo")||scene.equals("cancel_b")||scene.equals("cancel_c"))&&(phase==35||phase==38))KeyMapping.click(mc.options.keyAttack.getKey());
  if(scene.equals("spam")&&phase>=31&&phase<45)KeyMapping.click(mc.options.keyAttack.getKey());
  if(scene.startsWith("cancel_")&&phase==(scene.equals("cancel_c")?55:scene.equals("cancel_b")?43:33))KeyMapping.click(ActionKeys.ROLL.getKey());
  if(scene.equals("knockback")&&phase==34)server(p->{p.hurt(p.damageSources().generic(),1);p.knockback(.35,1,0);});
  if(scene.equals("world_push")&&phase==34)server(p->{p.setDeltaMovement(-.2,p.getDeltaMovement().y,0);p.hurtMarked=true;});
  if(scene.equals("tamper")&&phase==33){mc.player.move(MoverType.SELF,new net.minecraft.world.phys.Vec3(.13,0,0));mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(mc.player.getX(),mc.player.getY(),mc.player.getZ(),mc.player.onGround()));}
  if(scene.equals("roll_tamper")&&phase==33){var r=RollController.status(mc.player.getUUID());if(r!=null){mc.player.move(MoverType.SELF,new net.minecraft.world.phys.Vec3(.4,0,0));mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(mc.player.getX(),mc.player.getY(),mc.player.getZ(),mc.player.onGround()));ActionNetwork.finishRoll(r.startTick());}}
  if(scene.equals("turn")&&phase>=32&&phase<80)mc.player.setYRot(90);
  if(ClientAttackMotion.active()){locked++;checkOnceInput(mc,phase);}
  if(phase==55&&scene.startsWith("roll_")&&!scene.equals("roll_wall"))check(scene+"_released",!ClientRollMotion.busy(),"release <=25 ticks from V; physical phase14");
  if(phase>=30&&phase<=110&&phase%2==0){var c=PlayerRendererR3.controller();frames.add(Map.of("case",scene,"phase",phase,"x",mc.player.getX(),"z",mc.player.getZ(),"lock",ClientAttackMotion.active(),"roll",ClientRollMotion.busy(),"clip",c==null?"none":c.getAnimationName()));}
  if(phase==38||phase==45||phase==52||phase==70)Screenshot.grab(OUT.toFile(),scene+"-"+phase+".png",mc.getMainRenderTarget(),m->{});
  if(phase==125){String name=scene;double moved=Math.hypot(mc.player.getX()-x,mc.player.getZ()-z);int count=locked;server(p->verify(p,name,moved,count));}
 }
 static void checkOnceInput(Minecraft mc,int phase){if(phase==34||phase==40)check(scene+"_input_"+phase,mc.player.input.forwardImpulse==0&&mc.player.input.leftImpulse==0&&!mc.player.isSprinting(),"horizontal input neutralized");}
 static void fixture(ServerPlayer p,String name)throws Exception{
  LocomotionController.cancelAction(p);A12RuntimeTest.fixture(p,true);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getFoodData().setFoodLevel(20);p.invulnerableTime=0;
  if(name.equals("wall")||name.equals("roll_wall"))for(int xx=-2;xx<=2;xx++)for(int y=100;y<=103;y++)p.serverLevel().setBlockAndUpdate(new BlockPos(xx,y,1),Blocks.STONE.defaultBlockState());
  if(name.equals("edge")){for(int xx=-3;xx<=3;xx++)for(int zz=1;zz<=4;zz++)p.serverLevel().setBlockAndUpdate(new BlockPos(xx,99,zz),Blocks.AIR.defaultBlockState());p.teleportTo(p.serverLevel(),.5,100,.65,0,0);}
 }
 static void verify(ServerPlayer p,String name,double moved,int lock){
  var a=CombatController.status(p.getUUID());var motion=AttackMovement.status(p.getUUID());
  check(name+"_errors",PlayerRendererR3.errors==0,"Polymesh errors="+PlayerRendererR3.errors);
  check(name+"_clear",!ClientAttackMotion.active()&&!ClientRollMotion.busy(),"released after action");
  if(name.startsWith("roll_")){var r=RollController.status(p.getUUID());check(name+"_started",r!=null&&r.started(),r);if(name.equals("roll_tamper"))check("roll_tamper_rejected",r!=null&&r.reason().equals("movement bounds")&&Math.abs(p.getX()-.5)<.02,r+" position="+p.position());else check(name+"_distance",r!=null&&(name.equals("roll_wall")?r.distance()<.3:Math.abs(r.distance()-2.5)<.09),r);return;}
  if(name.startsWith("cancel_")){var r=RollController.status(p.getUUID());check(name+"_roll",r!=null&&r.started(),r);if(name.equals("cancel_back"))check("cancel_back_direction",r!=null&&r.z()<-.99,"raw remapped backward key survives attack input suppression");check(name+"_queue_clear",CombatController.actionStatus(p).queued()==0,CombatController.actionStatus(p));return;}
  check(name+"_committed",lock>0,"locked client ticks="+lock);
  check(name+"_server_validation",motion!=null&&motion.corrections()==(name.equals("tamper")?1:0),motion);
  if(Set.of("A","combo","heavy","slim").contains(name)){double expected=name.equals("combo")?.68:name.equals("heavy")?.32:.18;check(name+"_distance",Math.abs(moved-expected)<.035,"measured="+moved+" expected="+expected);}
  if(name.equals("combo"))check("combo_impacts",a!=null&&a.impacts()==3,a);
  if(name.equals("heavy"))check("heavy_impact",a!=null&&a.impacts()==1,a);
  if(name.equals("edge"))check("edge_safe",p.onGround()&&p.getY()>99.9&&p.getZ()<=.8&&motion!=null&&motion.measured()<.18,"ground preserved; advance clipped at edge; pos="+p.position());
  if(name.equals("wall"))check("wall_safe",p.getZ()<=.701,"pos="+p.position());
  if(name.equals("knockback"))check("external_force_preserved",p.getX()<.45,"pos="+p.position());
  if(name.equals("world_push"))check("world_force_preserved",p.getX()<.45,"pos="+p.position());
  if(name.equals("turn"))check("initial_direction",Math.abs(p.getX()-.5)<.02&&p.getZ()>.65,p.position());
  if(name.equals("firstperson"))check("firstperson_fallback",PlayerRendererR3.drawCalls==draws,"no custom body draw");
  if(name.equals("slim"))check("slim_preserved",net.minecraft.client.Minecraft.getInstance().player.getModelName().equals("slim")&&PlayerRendererR3.drawCalls>draws,"automatic Slim body");
  if(Set.of("A","combo","heavy").contains(name))check(name+"_no_residual",p.getDeltaMovement().horizontalDistance()<.01,p.getDeltaMovement());
  if(name.equals("spam"))check("spam_bounded",a!=null&&a.accepted()<=3&&moved<=.72,a+" moved="+moved);
 }
 static void server(java.util.function.Consumer<ServerPlayer> work){var mc=Minecraft.getInstance();var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);if(p!=null)work.accept(p);});}
 static void release(Minecraft mc){for(var k:new KeyMapping[]{mc.options.keyUp,mc.options.keyDown,mc.options.keyLeft,mc.options.keyRight,mc.options.keySprint,mc.options.keyAttack,mc.options.keyUse})k.setDown(false);}
 @SuppressWarnings("unchecked") static void slim(Minecraft mc)throws Exception{var info=mc.getConnection().getPlayerInfo(mc.player.getUUID());var f=info.getClass().getDeclaredField("textureLocations");f.setAccessible(true);((Map<Object,Object>)f.get(info)).put(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN,new net.minecraft.resources.ResourceLocation("minecraft","textures/entity/player/slim/alex.png"));var s=info.getClass().getDeclaredField("skinModel");s.setAccessible(true);s.set(info,"slim");}
 static void finish()throws Exception{Files.writeString(OUT.resolve(Boolean.getBoolean("toe.a3_2.directions")?"DIRECTIONS.json":Boolean.getBoolean("toe.a3_2.quick")?"QUICK.json":"MOVEMENT_RUNTIME.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("checks",checks,"frames",frames)));done=true;}
}
