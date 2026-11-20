package com.towerofentropy.combat;
import java.util.*;
import java.nio.file.*;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import com.towerofentropy.animation.*;
import com.towerofentropy.client.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
@Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT)
public final class RollRuntimeTest {
 private static final String[] CASES=Boolean.getBoolean("toe.roll.upgrade")?new String[]{"cold_model"}:new String[]{"forward","back","left","right","forward_left","forward_right","back_left","back_right","key_forward","key_back","key_left","key_right","key_diag","rotated_yaw","wall","edge","spam","attack_busy","off_cancel","hurt_cancel","menu_cancel","water_cancel","armor","offhand","cape","firstperson","invalid_air","invalid_input","combat_off","cold_model","death"};
 private static final List<Map<String,Object>> CHECKS=new java.util.concurrent.CopyOnWriteArrayList<>(),SAMPLES=new java.util.concurrent.CopyOnWriteArrayList<>();
 private static int title,ticks,quit,visit;private static boolean opening,done;
 private static double endX,endZ;
 private static long caseStart;
 private static long initialDraws;
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
  if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("toe.roll.test"))return;var mc=Minecraft.getInstance();if(done){if(++quit==40)mc.stop();return;}
  if(!opening&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null&&++title>100){opening=true;mc.options.pauseOnLostFocus=false;mc.createWorldOpenFlows().loadLevel(mc.screen,System.getProperty("toe.qa.world","TOE_R4"));}
  if(mc.player==null||mc.level==null||mc.getOverlay()!=null)return;ticks++;if(Boolean.getBoolean("toe.global.regression"))mc.mouseHandler.releaseMouse();
  if(ticks==5){preview(mc);mc.setScreen(null);mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);}
  if(Boolean.getBoolean("toe.roll.restart")){if(ticks==100){record("restart_clean",PlayerRendererR3.enabled&&PlayerRendererR3.drawCalls>0&&!ClientRollMotion.busy()&&RollController.status(mc.player.getUUID())==null&&CombatController.status(mc.player.getUUID())==null,"fresh JVM uses R3 without activation or stale actions");finish();}return;}
  if(visit>0){if(ticks==100){record("reconnect_clean",!ClientRollMotion.busy()&&RollController.status(mc.player.getUUID())==null&&PlayerRendererR3.enabled,"R3 default; no lingering Roll");finish();}return;}
  if(ticks==20){mc.setScreen(null);mc.options.hideGui=true;mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);preview(mc);if(Boolean.getBoolean("toe.global.regression"))server(p->{try{fixture(p,"forward");}catch(Exception ex){record("warmup_fixture",false,ex.toString());}});}
  if(ticks<100)return;int index=(ticks-100)/100,phase=(ticks-100)%100;
  if(index>=CASES.length){if(phase==0)server(p->{try{fixture(p,"forward");}catch(Exception ex){record("reconnect_setup",false,ex.toString());}});if(phase==30)ActionNetwork.requestRoll(0,1);if(phase==34){mc.level.disconnect();mc.clearLevel(new TitleScreen());opening=false;title=0;ticks=0;visit=1;}return;}
  String name=CASES[index];
  if(phase==0){mc.options.setCameraType(name.equals("firstperson")?CameraType.FIRST_PERSON:CameraType.THIRD_PERSON_FRONT);cape(mc,name.equals("cape"));server(p->{try{fixture(p,name);}catch(Exception ex){record(name+"_fixture",false,ex.toString());}});}
  if(phase==30){initialDraws=PlayerRendererR3.drawCalls;if(name.equals("attack_busy"))ActionNetwork.request(VisualActionWindow.Action.SWORD_ATTACK);if(name.equals("cold_model"))server(p->{try{CombatController.enable(p);}catch(Exception ex){record("cold_enable",false,ex.toString());}});}
  if(name.startsWith("key_")){mc.options.keyUp.setDown(phase>=30&&phase<58&&(name.equals("key_forward")||name.equals("key_diag")));mc.options.keyDown.setDown(phase>=30&&phase<58&&name.equals("key_back"));mc.options.keyLeft.setDown(phase>=30&&phase<58&&(name.equals("key_left")||name.equals("key_diag")));mc.options.keyRight.setDown(phase>=30&&phase<58&&name.equals("key_right"));}
  if(phase==32){float left=name.contains("left")?1:name.contains("right")?-1:0,forward=name.startsWith("back")?-1:name.equals("left")||name.equals("right")?0:1;if(name.equals("invalid_input"))left=Float.NaN;if(name.startsWith("key_"))net.minecraft.client.KeyMapping.click(ActionKeys.ROLL.getKey());else ActionNetwork.requestRoll(left,forward);}
  if(name.equals("spam")&&phase>=33&&phase<40)ActionNetwork.requestRoll(0,1);
  if(phase==36&&name.equals("off_cancel"))mc.player.connection.sendCommand("toe_combat off");
  if(phase==36&&name.equals("hurt_cancel"))server(p->p.hurt(p.damageSources().generic(),2));
  if(phase==36&&name.equals("water_cancel"))server(p->p.serverLevel().setBlockAndUpdate(p.blockPosition(),Blocks.WATER.defaultBlockState()));
  if(phase==36&&name.equals("menu_cancel"))server(p->p.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,player)->net.minecraft.world.inventory.ChestMenu.threeRows(id,inv),net.minecraft.network.chat.Component.literal("Roll interruption fixture"))));
  if(phase==36&&name.equals("death"))server(ServerPlayer::kill);
  if(phase==50&&name.equals("death"))mc.player.respawn();
  if(phase==50&&name.equals("menu_cancel"))mc.player.closeContainer();
  if(phase==38&&!name.equals("attack_busy")&&!name.equals("invalid_air")&&!name.equals("invalid_input")&&!name.equals("combat_off"))server(p->{try{var s=CombatController.status(p.getUUID());if(s!=null)record(name+"_attack_excluded",!CombatController.requestSword(p).equals("accepted"),"attack while Roll pending/active refused");}catch(Exception ex){record(name+"_attack_excluded",false,ex.toString());}});
  if(phase>=32&&phase<85)server(p->{var s=RollController.status(p.getUUID());if(s!=null)SAMPLES.add(Map.of("case",name,"tick",p.serverLevel().getGameTime(),"x",p.getX(),"y",p.getY(),"z",p.getZ(),"roll",s,"clientX",mc.player==null?0:mc.player.getX(),"clientZ",mc.player==null?0:mc.player.getZ()));});
  if(phase==55){Screenshot.grab(mc.gameDirectory,(Boolean.getBoolean("toe.a3.regression")?"m002a3-roll-":Boolean.getBoolean("toe.a2.regression")?"m002a2-roll-":Boolean.getBoolean("toe.a1_2.regression")?"m002a12-roll-":Boolean.getBoolean("toe.a1_1.regression")?"m002a11-roll-":Boolean.getBoolean("toe.a1.regression")?"m002a1-roll-":"m0021-")+name+".png",mc.getMainRenderTarget(),msg->{});}
  if(phase==75)server(p->{endX=p.getX();endZ=p.getZ();});
  if(phase==88)server(p->{
   var s=RollController.status(p.getUUID());boolean rejected=(!Boolean.getBoolean("toe.a1.regression")&&name.equals("attack_busy"))||name.equals("invalid_air")||name.equals("invalid_input")||name.equals("combat_off");double distance=Math.hypot(p.getX()-.5,p.getZ()-.5);
   if(name.equals("death")){var combat=CombatController.status(p.getUUID());record("death_respawn_clean",p.isAlive()&&s==null&&(combat==null||!combat.attacking()),"respawn removed actions; AUTO may retain an idle combat session with keepInventory");}
   else if(rejected)record(name+"_no_physical_roll",s==null||!s.started()||s.startTick()<caseStart,"status="+s+" distance="+distance);
   else if(name.equals("wall"))record(name+"_collision",distance<.8&&distance>=0,"distance="+distance+" status="+s);
   else if(name.endsWith("_cancel"))record(name+"_cancel",s!=null&&!s.busy()&&distance<2.5,"distance="+distance+" status="+s);
   else {double actual=name.startsWith("key_")&&s!=null?s.distance():distance;record(name+"_distance",s!=null&&Math.abs(actual-2.5)<.08,"distance="+actual+" status="+s);record(name+"_normal_finish",s!=null&&s.reason().equals("complete"),"status="+s);}
   if(name.equals("edge"))record("edge_gravity",p.getY()<99.5,"y="+p.getY());
   record(name+"_idle_clean",!RollController.busy(p.getUUID())&&!ClientRollMotion.busy(),"server="+s+" client busy="+ClientRollMotion.busy());
   if(name.equals("water_cancel"))record("water_current_remains_vanilla",!RollController.busy(p.getUUID())&&!ClientRollMotion.busy()&&p.getDeltaMovement().horizontalDistance()<RollRules.SPEED,"Roll released; vanilla fluid drift="+Math.hypot(p.getX()-endX,p.getZ()-endZ)+" speed="+p.getDeltaMovement().horizontalDistance());
   else record(name+"_no_velocity_accumulation",Math.hypot(p.getX()-endX,p.getZ()-endZ)<.02&&p.getDeltaMovement().horizontalDistance()<.01,"drift="+Math.hypot(p.getX()-endX,p.getZ()-endZ));
   if(!rejected&&!name.endsWith("_cancel")&&!name.equals("death"))record(name+"_render_backend",(name.equals("armor")||name.equals("offhand")||name.equals("cape")||name.equals("firstperson"))?PlayerRendererR3.drawCalls==initialDraws:PlayerRendererR3.drawCalls>initialDraws,"draws="+(PlayerRendererR3.drawCalls-initialDraws));
   if(name.equals("rotated_yaw"))record("yaw_direction",s!=null&&s.x()<-.99&&Math.abs(s.z())<.01,"status="+s);
   if(name.equals("key_diag"))record("wasd_diagonal_normalized",s!=null&&Math.abs(Math.hypot(s.x(),s.z())-1)<1e-6&&s.x()>.7&&s.z()>.7,"status="+s);
  });
 }
 private static void fixture(ServerPlayer p,String name)throws Exception{
  CombatController.disable(p);LocomotionController.stop(p);var level=p.serverLevel();
  caseStart=level.getGameTime();
  for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++)for(int y=95;y<=105;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),(y==99||y==95?Blocks.STONE:Blocks.AIR).defaultBlockState());
  for(var entity:level.getEntities((net.minecraft.world.entity.Entity)null,p.getBoundingBox().inflate(40)))if(!(entity instanceof net.minecraft.world.entity.player.Player))entity.discard();
  level.setDayTime(6000);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=false;p.onUpdateAbilities();p.setHealth(20);p.clearFire();p.removeAllEffects();p.hurtTime=0;p.teleportTo(level,.5,100,.5,0,0);p.setDeltaMovement(0,0,0);p.setOnGround(true);p.fallDistance=0;
  for(var slot:net.minecraft.world.entity.EquipmentSlot.values())p.setItemSlot(slot,ItemStack.EMPTY);p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));
  if(name.equals("wall"))for(int x=-2;x<=2;x++)for(int y=100;y<=103;y++)level.setBlockAndUpdate(new BlockPos(x,y,1),Blocks.STONE.defaultBlockState());
  if(name.equals("edge"))for(int x=-7;x<=7;x++)for(int z=1;z<=7;z++)level.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.AIR.defaultBlockState());
  if(name.equals("armor"))p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,new ItemStack(Items.IRON_CHESTPLATE));
  if(name.equals("offhand"))p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));
  if(!name.equals("cold_model")){LocomotionController.enable(p,false);if(!name.equals("combat_off"))CombatController.enable(p);}
  if(name.equals("hurt_cancel"))p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
  if(name.equals("rotated_yaw"))p.connection.teleport(.5,100,.5,90,0);
  if(name.equals("invalid_air")){p.teleportTo(level,.5,110,.5,0,0);p.getAbilities().flying=true;p.onUpdateAbilities();}
 }
 @SuppressWarnings("unchecked") private static void cape(Minecraft mc,boolean on){try{var info=mc.getConnection().getPlayerInfo(mc.player.getUUID());var f=info.getClass().getDeclaredField("textureLocations");f.setAccessible(true);var map=(Map<Object,Object>)f.get(info);if(on)map.put(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.CAPE,new net.minecraft.resources.ResourceLocation("minecraft","textures/entity/elytra.png"));else map.remove(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.CAPE);}catch(Exception ex){record("cape_fixture",false,ex.toString());}}
 @SuppressWarnings("unchecked") private static void preview(Minecraft mc){try{var info=mc.getConnection().getPlayerInfo(mc.player.getUUID());var f=info.getClass().getDeclaredField("textureLocations");f.setAccessible(true);var map=(Map<Object,Object>)f.get(info);map.put(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN,new net.minecraft.resources.ResourceLocation("minecraft","textures/entity/player/wide/steve.png"));map.remove(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.CAPE);var s=info.getClass().getDeclaredField("skinModel");s.setAccessible(true);s.set(info,"default");var t=info.getClass().getDeclaredField("pendingTextures");t.setAccessible(true);t.setBoolean(info,true);}catch(Exception ex){record("preview",false,ex.toString());}}
 private static void server(java.util.function.Consumer<ServerPlayer> action){var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();var id=mc.player.getUUID();server.execute(()->{var p=server.getPlayerList().getPlayer(id);if(p!=null)action.accept(p);});}
 private static void record(String name,boolean pass,String detail){CHECKS.add(Map.of("test",name,"pass",pass,"detail",detail));LogUtils.getLogger().info("TOE_M002_1 CHECK {} pass={} {}",name,pass,detail);}
 private static void finish(){try{Files.writeString(Path.of(Boolean.getBoolean("toe.global.regression")?"../animation_pipeline/reports/m002_a3_1/ROLL_REGRESSION.json":Boolean.getBoolean("toe.a3.regression")?"../animation_pipeline/reports/m002_a3/ROLL_REGRESSION.json":Boolean.getBoolean("toe.a2.regression")?"../animation_pipeline/reports/m002_a2/ROLL_REGRESSION.json":Boolean.getBoolean("toe.a1_2.regression")?"../animation_pipeline/reports/m002_a1_2/ROLL_REGRESSION.json":Boolean.getBoolean("toe.a1_1.regression")?"../animation_pipeline/reports/m002_a1_1/ROLL_REGRESSION.json":Boolean.getBoolean("toe.a1.regression")?"../animation_pipeline/reports/m002_a1/ROLL_REGRESSION.json":"../.logs/m0021-"+(Boolean.getBoolean("toe.roll.upgrade")?"upgrade":Boolean.getBoolean("toe.roll.restart")?"restart":"runtime")+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("checks",CHECKS,"samples",SAMPLES,"renderErrors",PlayerRendererR3.errors)));}catch(Exception ex){throw new RuntimeException(ex);}LogUtils.getLogger().info("TOE_M002_1 DONE passed={} total={}",CHECKS.stream().filter(r->Boolean.TRUE.equals(r.get("pass"))).count(),CHECKS.size());done=true;}
}
