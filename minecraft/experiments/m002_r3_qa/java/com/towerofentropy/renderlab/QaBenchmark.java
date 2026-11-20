package com.towerofentropy.renderlab;

import java.nio.file.*;
import java.lang.management.ManagementFactory;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

/** QA instrumentation only. No change to assets, controller code or gameplay. */
@Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT)
public final class QaBenchmark {
 private static final String[] CLIPS={"idle","jog","sword","roll"};
 private static final double[] DURATION={2.5,28/30d,13/30d,44/30d};
 private static final ArrayList<Map<String,Object>> RESULTS=new ArrayList<>(),CAPTURES=new ArrayList<>();
 private static final Map<String,Object> META=new LinkedHashMap<>();
 private record Case(int round,String profile,int clip){}
 private static final List<Case> CASES=new ArrayList<>();
 static{for(int round=0;round<3;round++)for(int clip=0;clip<4;clip++)for(String profile:round%2==0?new String[]{"r3","r31"}:new String[]{"r31","r3"})CASES.add(new Case(round,profile,clip));}
 private static final com.sun.management.OperatingSystemMXBean OS=(com.sun.management.OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
 private static int title,ticks,index=-1,mode=-1,captureStage=-1,captureFrame=-1,quit;
 private static boolean opening,ready,done,measuring,gpu;
 private static long epoch,segmentStart,lastStart,frameStart,drawStart,nanoStart,cpuStart,gcStart,compileStart;
 private static long gpuFrame;
 private static final ArrayList<Double> FRAME=new ArrayList<>(),SPAN=new ArrayList<>(),GPU=new ArrayList<>();
 private static final int[][] QUERY=new int[32][2];private static final long[] OWNER=new long[32];
 private static int qslot=-1;
 private static double phase;
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
  if(event.phase!=TickEvent.Phase.END||!Boolean.getBoolean("toe.qa"))return;
  var mc=Minecraft.getInstance();if(done){if(++quit==40)mc.stop();return;}
  if(!opening&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null&&++title>100){opening=true;mc.options.pauseOnLostFocus=false;mc.createWorldOpenFlows().loadLevel(mc.screen,"TOE_QA");}
  if(mc.player==null||mc.level==null||mc.getOverlay()!=null)return;
  ticks++;if(ticks==20){setup(mc);}
  if(!ready&&ticks>=140){ready=true;epoch=System.nanoTime();metadata(mc);SkinnedSteveRenderer.select(true);}
 }
 @SubscribeEvent public static void render(TickEvent.RenderTickEvent event){
  if(!Boolean.getBoolean("toe.qa")||!ready||done)return;
  var mc=Minecraft.getInstance();if(mc.player==null||mc.screen!=null)return;
  long now=System.nanoTime();
  if(event.phase==TickEvent.Phase.START){
   frameStart=now;
   double elapsed=(now-epoch)/1e9;
   if(elapsed<192){int warm=(int)(elapsed/6);apply(warm%2==0?"r3":"r31",warm/2%4,(elapsed%6),90);lastStart=now;return;}
   int next=(int)((elapsed-192)/13);
   if(next<CASES.size()){
    if(index!=next){if(measuring)finishWindow();index=next;mode=next;segmentStart=now;lastStart=0;measuring=false;var c=CASES.get(index);LogUtils.getLogger().info("TOE_QA WINDOW {} round={} profile={} clip={}",index,c.round,c.profile,CLIPS[c.clip]);}
    var c=CASES.get(index);double local=(now-segmentStart)/1e9;
    apply(c.profile,c.clip,local,90);
    if(local>=3&&!measuring){FRAME.clear();SPAN.clear();GPU.clear();drawStart=SkinnedSteveRenderer.renderNanos;nanoStart=now;cpuStart=OS.getProcessCpuTime();gcStart=gcMillis();compileStart=ManagementFactory.getCompilationMXBean().getTotalCompilationTime();measuring=true;lastStart=0;}
    if(measuring&&lastStart!=0)FRAME.add((now-lastStart)/1e6);lastStart=now;
    qslot=-1;
    if(measuring&&gpu){for(int s=0;s<32;s++)if(OWNER[s]>=0&&GL15.glGetQueryObjecti(QUERY[s][1],GL15.GL_QUERY_RESULT_AVAILABLE)!=0){if(OWNER[s]==index){long a=GL33.glGetQueryObjectui64(QUERY[s][0],GL15.GL_QUERY_RESULT),b=GL33.glGetQueryObjectui64(QUERY[s][1],GL15.GL_QUERY_RESULT);GPU.add((b-a)/1e6);}OWNER[s]=-1;}
     int slot=(int)(gpuFrame++%32);if(OWNER[slot]<0){qslot=slot;OWNER[slot]=index;GL33.glQueryCounter(QUERY[slot][0],GL33.GL_TIMESTAMP);}}
   }else{
    if(measuring){finishWindow();measuring=false;save(false);}
    capture(mc,now);
   }
  }else{
   if(measuring){SPAN.add((now-frameStart)/1e6);if(qslot>=0){GL33.glQueryCounter(QUERY[qslot][1],GL33.GL_TIMESTAMP);qslot=-1;}}
   if(index>=CASES.size()||!measuring&&RESULTS.size()==CASES.size())captureEnd(mc,now);
  }
 }
 private static void apply(String profile,int clip,double seconds,float yaw){
  System.setProperty("toe.r31.mode",profile);String name=CLIPS[clip];if(!name.equals(SkinnedSteveRenderer.forcedClip))SkinnedSteveRenderer.pose(name,0);phase=seconds%DURATION[clip];SkinnedSteveRenderer.frozenTime=(float)phase;
  var p=Minecraft.getInstance().player;p.setYRot(0);p.yRotO=0;p.setYHeadRot(yaw);p.yHeadRotO=yaw;p.yBodyRot=yaw;p.yBodyRotO=yaw;
 }
 private static void finishWindow(){
  var c=CASES.get(index);double[] t=FRAME.stream().mapToDouble(Double::doubleValue).sorted().toArray();double mean=Arrays.stream(t).average().orElse(Double.NaN);int n=Math.max(1,(int)Math.ceil(t.length*.01));double slow=Arrays.stream(t,t.length-n,t.length).average().orElse(Double.NaN);
  var row=new LinkedHashMap<String,Object>();row.put("round",c.round+1);row.put("profile",c.profile);row.put("clip",CLIPS[c.clip]);row.put("frames",t.length);row.put("seconds",(System.nanoTime()-nanoStart)/1e9);row.put("meanFrameMs",mean);row.put("meanFps",1000/mean);row.put("onePercentLowFps",1000/slow);row.put("renderEventSpanMs",SPAN.stream().mapToDouble(Double::doubleValue).average().orElse(0));row.put("processCpuOneCorePercent",100d*(OS.getProcessCpuTime()-cpuStart)/(System.nanoTime()-nanoStart));row.put("cpuThreads",OS.getAvailableProcessors());row.put("drawCpuMsPerFrame",(SkinnedSteveRenderer.renderNanos-drawStart)/1e6/Math.max(1,SPAN.size()));row.put("gpuSamples",GPU.size());if(!GPU.isEmpty())row.put("gpuTimestampSpanMs",GPU.stream().mapToDouble(Double::doubleValue).average().orElse(0));row.put("gcMs",gcMillis()-gcStart);row.put("jitCompileMs",ManagementFactory.getCompilationMXBean().getTotalCompilationTime()-compileStart);row.put("frameTimesMs",FRAME.toArray());RESULTS.add(row);LogUtils.getLogger().info("TOE_QA RESULT profile={} clip={} fps={} low={} frameMs={}",c.profile,CLIPS[c.clip],1000/mean,1000/slow,mean);
 }
 private static long captureEpoch;
 private static void capture(Minecraft mc,long now){
  // Two uninterrupted 1x Roll recordings, outside measured windows. Frame grabs are timestamped.
  if(captureEpoch==0)captureEpoch=now;
  double t=(now-captureEpoch)/1e9;int stage=(int)(t/4);if(stage>=2){save(true);done=true;return;}
  if(captureStage!=stage){captureStage=stage;captureFrame=-1;SkinnedSteveRenderer.select(true);}
  String profile=stage==0?"r3":"r31";double local=t%4;apply(profile,3,Math.max(0,local-1),0);
  if(local<1||local>1+DURATION[3])SkinnedSteveRenderer.pose("idle",0);
 }
 private static void captureEnd(Minecraft mc,long now){
  if(captureEpoch==0||done)return;double t=(now-captureEpoch)/1e9,local=t%4;int stage=(int)(t/4);if(stage>=2||local<1||local>1+DURATION[3])return;int frame=Math.min(44,(int)((local-1)*30));if(frame==captureFrame)return;captureFrame=frame;String profile=stage==0?"r3":"r31",name=String.format(Locale.ROOT,"qa-%s-normal-%02d.png",profile,frame+1);Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),msg->{});CAPTURES.add(Map.of("profile",profile,"frame",frame+1,"seconds",local-1,"file",name));
 }
 private static long gcMillis(){return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b->Math.max(0,b.getCollectionTime())).sum();}
 private static void save(boolean complete){try{var out=new LinkedHashMap<String,Object>();out.put("complete",complete);out.put("metadata",META);out.put("windows",RESULTS);out.put("normalSpeedCaptures",CAPTURES);out.put("renderErrors",SkinnedSteveRenderer.errors);Files.writeString(Path.of("../.logs/qa-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out));}catch(Exception e){throw new RuntimeException(e);}}
 private static void metadata(Minecraft mc){
  META.put("glVendor",GL11.glGetString(GL11.GL_VENDOR));META.put("glRenderer",GL11.glGetString(GL11.GL_RENDERER));META.put("glVersion",GL11.glGetString(GL11.GL_VERSION));META.put("java",System.getProperty("java.version"));META.put("os",System.getProperty("os.name"));META.put("cpuThreads",OS.getAvailableProcessors());META.put("resolution",List.of(mc.getWindow().getWidth(),mc.getWindow().getHeight()));META.put("renderDistance",mc.options.renderDistance().get());META.put("simulationDistance",mc.options.simulationDistance().get());META.put("vsync",mc.options.enableVsync().get());META.put("maxFps",mc.options.framerateLimit().get());META.put("graphics",mc.options.graphicsMode().get().toString());META.put("warmupSeconds",192);META.put("perWindowWarmupSeconds",3);META.put("measureSeconds",10);META.put("onePercentLowDefinition","1000 / arithmetic mean of slowest ceil(1% N) start-to-start frame intervals");
  gpu=GL.getCapabilities().OpenGL33||GL.getCapabilities().GL_ARB_timer_query;META.put("gpuTimerSupported",gpu);Arrays.fill(OWNER,-1);if(gpu)for(int[] pair:QUERY){pair[0]=GL15.glGenQueries();pair[1]=GL15.glGenQueries();}
 }
 @SuppressWarnings("unchecked") private static void setup(Minecraft mc){
  mc.setScreen(null);mc.options.hideGui=true;mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);mc.options.fov().set(50);mc.options.enableVsync().set(false);mc.options.framerateLimit().set(260);mc.getWindow().updateVsync(false);
  try{var info=mc.getConnection().getPlayerInfo(mc.player.getUUID());var f=info.getClass().getDeclaredField("textureLocations");f.setAccessible(true);var map=(Map<Object,Object>)f.get(info);map.put(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN,new ResourceLocation("minecraft","textures/entity/player/wide/steve.png"));map.remove(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.CAPE);var s=info.getClass().getDeclaredField("skinModel");s.setAccessible(true);s.set(info,"default");var p=info.getClass().getDeclaredField("pendingTextures");p.setAccessible(true);p.setBoolean(info,true);}catch(Exception e){throw new RuntimeException(e);}
  var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);var level=p.serverLevel();p.teleportTo(level,.5,100,.5,0,0);p.getAbilities().flying=false;level.setDayTime(6000);level.setWeatherParameters(100000,0,false,false);level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,level.getServer());level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,level.getServer());
   for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)for(int y=99;y<=104;y++)level.setBlockAndUpdate(new net.minecraft.core.BlockPos(x,y,z),(y==99?net.minecraft.world.level.block.Blocks.STONE:net.minecraft.world.level.block.Blocks.AIR).defaultBlockState());
   for(var entity:level.getEntities((net.minecraft.world.entity.Entity)null,p.getBoundingBox().inflate(32)))if(!(entity instanceof net.minecraft.world.entity.player.Player))entity.discard();for(var slot:net.minecraft.world.entity.EquipmentSlot.values())p.setItemSlot(slot,ItemStack.EMPTY);p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));});
 }
}
