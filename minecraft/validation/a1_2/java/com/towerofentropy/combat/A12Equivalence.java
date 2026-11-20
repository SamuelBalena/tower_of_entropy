package com.towerofentropy.combat;
import com.towerofentropy.client.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import java.nio.file.*;
import java.util.Map;
@Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT)
public final class A12Equivalence {
 static int title,ticks,samples;static boolean opening,done;static double matrixError,anchorError,groundError;static A12ReferencePose reference;static R3VisualPose instance;
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e)throws Exception{
  if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("toe.a1_2.equivalence"))return;var mc=Minecraft.getInstance();
  if(done){mc.stop();return;}
  if(!opening&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null&&++title>60){opening=true;mc.options.pauseOnLostFocus=false;mc.createWorldOpenFlows().loadLevel(mc.screen,"TOE_A1_2");}
  if(mc.player==null||mc.level==null||mc.getOverlay()!=null)return;++ticks;
  if(ticks==5){A12RuntimeTest.preview(mc);mc.setScreen(null);mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);}
  if(ticks==20){var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{try{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);A12RuntimeTest.fixture(p,true);CombatController.disable(p);}catch(Exception ex){throw new RuntimeException(ex);}});}
  if(ticks>=80){String[] clips={"idle","heavy","roll","sword_b","sword_c","jog","heavy","jog"};int phase=(ticks-80)%80,index=phase/10;float[] offsets={.2f,0,0,.12f,1.6f,.2f,0,.3f};if(phase%10==0)PlayerRendererR3.pose(clips[index],offsets[index]);else PlayerRendererR3.frozenTime=offsets[index]+(phase%10)/20f;}
  if(ticks==420){Files.writeString(Path.of("../animation_pipeline/reports/m002_a1_2/EQUIVALENCE.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("pass",samples>500&&matrixError<.00003&&anchorError<.00003&&groundError<.00003,"samples",samples,"maxMatrixError",matrixError,"maxAnchorError",anchorError,"maxGroundErrorBlocks",groundError)));done=true;}
 }
 @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true) public static void render(RenderPlayerEvent.Pre e)throws Exception{
  if(!Boolean.getBoolean("toe.a1_2.equivalence")||!e.isCanceled()||e.getEntity()!=Minecraft.getInstance().player||PlayerRendererR3.visualPose()==null)return;
  if(instance!=PlayerRendererR3.visualPose()){instance=PlayerRendererR3.visualPose();reference=new A12ReferencePose(PlayerRendererR3.model());}
  var c=PlayerRendererR3.controller();String clip=c.getAnimationName().replace("UAL_","");var method=PlayerRendererR3.class.getDeclaredMethod("sampleAnchor",String.class,float.class);method.setAccessible(true);
  Matrix4f a=(Matrix4f)method.invoke(null,clip,c.getAnimationTime());var raw=c.computeGlobalTransforms(0);var expected=reference.apply(raw,clip,ClientCombatState.started(),c.getAnimationTime(),(e.getEntity().level().getGameTime()+e.getPartialTick())/20.0,a,e.getEntity().onGround());var actual=PlayerRendererR3.displayed();
  for(int i=0;i<actual.length;i++)matrixError=Math.max(matrixError,error(expected[i],actual[i]));anchorError=Math.max(anchorError,error(reference.anchor(),instance.anchor()));groundError=Math.max(groundError,Math.abs(reference.groundBlocks()-instance.groundBlocks()));++samples;
 }
 static double error(Matrix4f a,Matrix4f b){float[] x=new float[16],y=new float[16];a.get(x);b.get(y);double d=0;for(int i=0;i<16;i++)d=Math.max(d,Math.abs(x[i]-y[i]));return d;}
}
