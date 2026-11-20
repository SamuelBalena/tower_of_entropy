package com.towerofentropy.combat;
import java.nio.file.*;import java.util.*;import com.google.gson.GsonBuilder;
import com.towerofentropy.animation.*;import com.towerofentropy.client.*;
import net.minecraft.client.*;import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.level.block.Blocks;import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT)
public final class A2RuntimeTest {
 static final Path DIR=Path.of("../animation_pipeline/reports/m002_a2/"+(PlayerRendererR3.PREVIOUS?"before":"after"));
 static final String[] SCENES=Boolean.getBoolean("toe.a2.extra")?new String[]{"gait_cycle","remapped_sprint"}:new String[]{"idle","walk","jog","sprint","backward","left","right","diagonal","reverse","start_stop","speed_change","sword_jog","attack_return","roll_return","wall","uneven","jump","crouch"};
 static com.mojang.blaze3d.platform.InputConstants.Key originalSprint;
 static final List<Map<String,Object>> SERIES=new ArrayList<>(),CHECKS=new ArrayList<>();static List<Map<String,Object>> frames;
 static int title,ticks,quit,visit;static boolean opening,done;static long draws;static String name;public static float orbit;
 static void check(String n,boolean p,Object d){CHECKS.add(Map.of("test",n,"pass",p,"detail",String.valueOf(d)));com.mojang.logging.LogUtils.getLogger().info("TOE_A2 {} {} {}",n,p,d);}
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e)throws Exception{
  if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("toe.a2.test")||Boolean.getBoolean("toe.a2.benchmark")||Boolean.getBoolean("toe.a1_2.iframes"))return;var mc=Minecraft.getInstance();
  if(done){if(++quit==25)mc.stop();return;}
  if(!opening&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null&&++title>60){opening=true;mc.options.pauseOnLostFocus=false;mc.createWorldOpenFlows().loadLevel(mc.screen,"TOE_A2");}
  if(mc.player==null||mc.level==null||mc.getOverlay()!=null)return;++ticks;
  if(ticks==5){A12RuntimeTest.preview(mc);mc.setScreen(null);mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);mc.options.fov().set(50);mc.options.hideGui=true;}
  if(visit>0||Boolean.getBoolean("toe.a2.restart")){if(ticks==70){check("lifecycle_auto",ClientCombatState.mode==CombatController.Mode.AUTO,"AUTO default");check("lifecycle_renderer",PlayerRendererR3.drawCalls>0&&PlayerRendererR3.errors==0,"normal R3");check("lifecycle_actions_clear",!ClientRollMotion.busy()&&ClientCombatState.clip().isEmpty(),"no stuck action");save();done=true;}return;}
  if(ticks<90)return;int elapsed=ticks-90,sceneCount=Boolean.getBoolean("toe.a2.quick")?4:SCENES.length,views=Boolean.getBoolean("toe.a2.quick")?1:3;
  int index=elapsed/100,phase=elapsed%100;
  if(index>=sceneCount*views){release(mc);orbit=0;mc.level.disconnect();mc.clearLevel(new TitleScreen());opening=false;title=0;ticks=0;visit=1;return;}
  name=SCENES[index/views];String angle=new String[]{"front","side","back"}[index%views];orbit=(index%views)*90;
  if(phase==0){release(mc);frames=new ArrayList<>();SERIES.add(Map.of("name",name,"angle",angle,"frames",frames));Files.createDirectories(DIR.resolve("screenshots/"+name+"/"+angle));draws=PlayerRendererR3.drawCalls;final String scene=name;server(p->{try{A12RuntimeTest.fixture(p,scene.contains("sword")||scene.contains("attack")||scene.contains("roll"));
    if(scene.equals("walk")||scene.equals("speed_change")||scene.equals("gait_cycle"))for(int x=-3;x<=3;x++)for(int z=-2;z<=12;z++)p.serverLevel().setBlockAndUpdate(new BlockPos(x,99,z),Blocks.SOUL_SAND.defaultBlockState());
    if(scene.equals("wall"))for(int x=-2;x<=2;x++)for(int y=100;y<103;y++)p.serverLevel().setBlockAndUpdate(new BlockPos(x,y,2),Blocks.STONE.defaultBlockState());
    if(scene.equals("uneven"))for(int x=-2;x<=2;x++)for(int z=2;z<10;z+=2)p.serverLevel().setBlockAndUpdate(new BlockPos(x,100,z),Blocks.STONE_SLAB.defaultBlockState());
   }catch(Exception ex){check("fixture",false,ex);}});}
  boolean moving=phase>=20&&phase<70&&!name.equals("idle");boolean forward=moving&&!Set.of("backward","left","right").contains(name);
  if(name.equals("reverse")&&phase>=45)forward=false;
  if(name.equals("start_stop"))forward=moving&&(phase<38||phase>=55);
  if(name.equals("sprint")&&phase>=58)forward=false;
  if(name.equals("gait_cycle")&&phase==35)server(p->{for(int x=-3;x<=3;x++)for(int z=-2;z<=12;z++)p.serverLevel().setBlockAndUpdate(new BlockPos(x,99,z),Blocks.STONE.defaultBlockState());});
  if(name.equals("gait_cycle")&&phase==58)server(p->{p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getFoodData().setFoodLevel(6);});
  if(name.equals("remapped_sprint")&&phase==5){originalSprint=mc.options.keySprint.getKey();mc.options.keySprint.setKey(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(org.lwjgl.glfw.GLFW.GLFW_KEY_R));KeyMapping.resetMapping();}
  if(name.equals("speed_change")&&phase==45)server(p->{for(int x=-3;x<=3;x++)for(int z=-2;z<=12;z++)p.serverLevel().setBlockAndUpdate(new BlockPos(x,99,z),Blocks.STONE.defaultBlockState());});
  mc.options.keyUp.setDown(forward);mc.options.keyDown.setDown(moving&&(name.equals("backward")||name.equals("reverse")&&phase>=45));mc.options.keyLeft.setDown(moving&&(name.equals("left")||name.equals("diagonal")));mc.options.keyRight.setDown(moving&&name.equals("right"));
  mc.options.keySprint.setDown(moving&&(name.equals("sprint")||name.equals("speed_change")&&phase>=52));mc.options.keyShift.setDown(name.equals("crouch")&&moving);mc.options.keyJump.setDown(name.equals("jump")&&phase>=30&&phase<34);
  if(name.equals("gait_cycle"))mc.options.keySprint.setDown(moving&&phase>=45&&phase<58);
  if(name.equals("remapped_sprint"))KeyMapping.set(mc.options.keySprint.getKey(),moving&&phase<58);
  if(name.equals("remapped_sprint")&&phase==92){mc.options.keySprint.setDown(false);mc.options.keySprint.setKey(originalSprint);KeyMapping.resetMapping();check("sprint_binding_restored",mc.options.keySprint.getKey().equals(originalSprint),"temporary R mapping restored");}
  if(name.equals("attack_return")&&phase==27)KeyMapping.click(mc.options.keyAttack.getKey());
  if(name.equals("roll_return")&&phase==27)KeyMapping.click(ActionKeys.ROLL.getKey());
  if(phase>=16&&phase<=86&&phase%2==0){String file=name+"/"+angle+"/"+String.format(java.util.Locale.ROOT,"%03d.png",phase);Screenshot.grab(DIR.toFile(),file,mc.getMainRenderTarget(),m->{});var row=new LinkedHashMap<String,Object>();var c=PlayerRendererR3.controller();row.put("src","screenshots/"+file);row.put("t",phase/20.0);row.put("clip",c==null?"none":c.getAnimationName());row.put("clipTime",c==null?0:c.getAnimationTime());row.put("state",HorizontalLocomotion.MOTION.state().name());row.put("speed",HorizontalLocomotion.MOTION.speed());row.put("sprinting",mc.player.isSprinting());row.put("x",mc.player.getX());row.put("z",mc.player.getZ());row.put("onGround",mc.player.onGround());row.put("fallback",PlayerRendererR3.fallbackReason);row.put("draws",PlayerRendererR3.drawCalls);row.put("visual",A12Probe.sample());frames.add(row);}
  if(phase==92){check(name+"_"+angle+"_renderer",PlayerRendererR3.errors==0,"draws="+(PlayerRendererR3.drawCalls-draws));if(!PlayerRendererR3.PREVIOUS&&!Set.of("backward","left","right","jump","crouch").contains(name))check(name+"_"+angle+"_draws",PlayerRendererR3.drawCalls>draws,"custom renderer active");}
 }
 static void server(java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> work){var mc=Minecraft.getInstance();var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->work.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(id)));}
 static void release(Minecraft mc){for(var k:new KeyMapping[]{mc.options.keyUp,mc.options.keyDown,mc.options.keyLeft,mc.options.keyRight,mc.options.keySprint,mc.options.keyShift,mc.options.keyJump})k.setDown(false);}
 static void save()throws Exception{Files.createDirectories(DIR);Files.writeString(DIR.resolve(Boolean.getBoolean("toe.a2.restart")?"RESTART.json":Boolean.getBoolean("toe.a2.extra")?"EXTRA.json":Boolean.getBoolean("toe.a2.quick")?"QUICK.json":"RUNTIME.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("checks",CHECKS,"series",SERIES,"renderErrors",PlayerRendererR3.errors)));}
 @SubscribeEvent public static void camera(net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles e)throws Exception{if(!Boolean.getBoolean("toe.a2.test")||orbit==0||Minecraft.getInstance().player==null)return;var center=Minecraft.getInstance().player.getEyePosition((float)e.getPartialTick());var d=e.getCamera().getPosition().subtract(center);double a=Math.toRadians(orbit);var m=Camera.class.getDeclaredMethod("setPosition",double.class,double.class,double.class);m.setAccessible(true);m.invoke(e.getCamera(),center.x+d.x*Math.cos(a)-d.z*Math.sin(a),center.y+d.y,center.z+d.x*Math.sin(a)+d.z*Math.cos(a));e.setYaw(e.getYaw()+orbit);}
}
