package com.towerofentropy.combat;
import java.util.*;
import java.nio.file.*;
import com.google.gson.GsonBuilder;
import com.towerofentropy.client.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Identical warmup/workloads in separate baseline/polished JVMs. No screenshots during measurement. */
@Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT)
public final class A2Benchmark {
 private static final String[] WORK={"idle","walk","sprint","transitions"};
 private static final List<Map<String,Object>> ROWS=new ArrayList<>();
 private static List<Double> pipeline,pose,draw,frame;
 private static int title,ticks,quit;private static boolean opening,done,measuring;
 private static long lastDraw,lastPipe,lastPose,lastRender,lastClock;
 private static long coldDraw,coldPipeline,coldClock;private static final List<Map<String,Object>> COLD=new ArrayList<>();
 private static long useDraw,usePipeline,useClock;private static final Map<String,List<Map<String,Object>>> FIRST=new LinkedHashMap<>();
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
  if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("toe.a2.benchmark"))return;var mc=Minecraft.getInstance();
  if(done){if(++quit==35)mc.stop();return;}
  if(!opening&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null&&++title>60){opening=true;mc.options.pauseOnLostFocus=false;mc.createWorldOpenFlows().loadLevel(mc.screen,"TOE_A2");}
  if(mc.player==null||mc.level==null||mc.getOverlay()!=null)return;++ticks;
  if(ticks==5){A12RuntimeTest.preview(mc);mc.setScreen(null);mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);mc.options.fov().set(50);mc.options.hideGui=true;}
  if(ticks==20){var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);try{A12RuntimeTest.fixture(p,true);CombatController.disable(p);p.serverLevel().setDayTime(6000);p.serverLevel().setWeatherParameters(0,6000,false,false);p.serverLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,mc.getSingleplayerServer());p.serverLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,mc.getSingleplayerServer());}catch(Exception ex){throw new RuntimeException(ex);}});}
  if(ticks<320){if(ticks==100)PlayerRendererR3.pose("heavy",.06f);if(ticks==150)PlayerRendererR3.pose("sword_b",.3f);if(ticks==200)PlayerRendererR3.pose("jog",.3f);if(ticks==250)PlayerRendererR3.pose(PlayerRendererR3.PREVIOUS?"jog":"walk",.3f);if(ticks==280)PlayerRendererR3.pose(PlayerRendererR3.PREVIOUS?"jog":"sprint",.3f);return;}
  int index=(ticks-320)/120,phase=(ticks-320)%120;
  if(index>=12){save(mc);done=true;return;}
  String work=WORK[index%4];
  if(phase==0){measuring=false;pipeline=new ArrayList<>();pose=new ArrayList<>();draw=new ArrayList<>();frame=new ArrayList<>();ROWS.add(Map.of("round",index/4+1,"workload",work,"pipelineMs",pipeline,"poseMs",pose,"drawMs",draw,"frameMs",frame));if(!work.equals("transitions"))PlayerRendererR3.pose(work.equals("idle")?"idle":PlayerRendererR3.PREVIOUS?"jog":work,work.equals("idle")?.4f:.3f);}
  if(work.equals("transitions")&&phase%5==0){String[] clips={"idle",PlayerRendererR3.PREVIOUS?"jog":"walk",PlayerRendererR3.PREVIOUS?"jog":"sprint","jog"};float[] times={.4f,.05f,.15f,.3f};int i=phase/5%4;PlayerRendererR3.pose(clips[i],times[i]);}
  if(phase==20){lastDraw=PlayerRendererR3.drawCalls;lastPipe=PlayerRendererR3.pipelineNanos;lastPose=PlayerRendererR3.poseNanos;lastRender=PlayerRendererR3.renderNanos;lastClock=0;measuring=true;}
  if(phase==119)measuring=false;
 }
 @SubscribeEvent public static void render(TickEvent.RenderTickEvent e){
  if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("toe.a2.benchmark"))return;long now=System.nanoTime();
  if(COLD.size()<120&&PlayerRendererR3.drawCalls>coldDraw){long n=PlayerRendererR3.drawCalls-coldDraw;COLD.add(Map.of("pipelineMs",(PlayerRendererR3.pipelineNanos-coldPipeline)/1e6/n,"frameMs",coldClock==0?0:(now-coldClock)/1e6));coldClock=now;coldDraw=PlayerRendererR3.drawCalls;coldPipeline=PlayerRendererR3.pipelineNanos;}
  if(PlayerRendererR3.drawCalls>useDraw){var c=PlayerRendererR3.controller();if(c!=null){var list=FIRST.computeIfAbsent(c.getAnimationName(),k->new ArrayList<>());if(list.size()<30)list.add(Map.of("pipelineMs",(PlayerRendererR3.pipelineNanos-usePipeline)/1e6/(PlayerRendererR3.drawCalls-useDraw),"frameMs",useClock==0?0:(now-useClock)/1e6));}useDraw=PlayerRendererR3.drawCalls;usePipeline=PlayerRendererR3.pipelineNanos;useClock=now;}
  if(!measuring)return;long count=PlayerRendererR3.drawCalls-lastDraw;if(count<=0)return;
  pipeline.add((PlayerRendererR3.pipelineNanos-lastPipe)/1e6/count);pose.add((PlayerRendererR3.poseNanos-lastPose)/1e6/count);draw.add((PlayerRendererR3.renderNanos-lastRender)/1e6/count);if(lastClock>0)frame.add((now-lastClock)/1e6);
  lastDraw=PlayerRendererR3.drawCalls;lastPipe=PlayerRendererR3.pipelineNanos;lastPose=PlayerRendererR3.poseNanos;lastRender=PlayerRendererR3.renderNanos;lastClock=now;
 }
 private static void save(Minecraft mc){try{
  var env=new LinkedHashMap<String,Object>();env.put("width",mc.getWindow().getWidth());env.put("height",mc.getWindow().getHeight());env.put("renderDistance",mc.options.renderDistance().get());env.put("simulationDistance",mc.options.simulationDistance().get());env.put("graphics",mc.options.graphicsMode().get().toString());env.put("vsync",mc.options.enableVsync().get());env.put("frameLimit",mc.options.framerateLimit().get());env.put("fov",mc.options.fov().get());env.put("warmupTicks",320);
  var out=Path.of("../animation_pipeline/reports/m002_a2/"+(PlayerRendererR3.PREVIOUS?"before":"after")+"/BENCHMARK.json");Files.writeString(out,new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("first30FramesPerClip",FIRST,"coldFirst120EligibleFrames",COLD,"environment",env,"rounds",ROWS,"renderErrors",PlayerRendererR3.errors,"previous",PlayerRendererR3.PREVIOUS)));
 }catch(Exception ex){throw new RuntimeException(ex);}}
}
