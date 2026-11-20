package com.towerofentropy.client;

import com.google.gson.*;
import com.towerofentropy.animation.VisualTuning;
import dev.phe.polymesh.model.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/** Local-TRS snapshot blending around the existing Polymesh sampler/renderer. No entity writes. */
public final class R3VisualPose {
 public static final boolean LEGACY=Boolean.getBoolean("toe.a12.legacy");
 private final RuntimeModel model;
 private final Matrix4f[] globals,local,inverse;
 private final boolean[] inverseReady,ready;
 private final Vector3f[] displayedP,sourceP,displayedS,sourceS;
 private final Quaternionf[] displayedQ,sourceQ;
 private final Vector3f p=new Vector3f(),s=new Vector3f();
 private final Quaternionf q=new Quaternionf();
 private final Vector3f anchorP=new Vector3f(),anchorFromP=new Vector3f(),anchorTargetP=new Vector3f();
 private final Quaternionf anchorQ=new Quaternionf(),anchorFromQ=new Quaternionf(),anchorTargetQ=new Quaternionf();
 private final Matrix4f anchor=new Matrix4f();
 private final Vector3f gripPivot=new Vector3f(-1/16f,10/16f,-2/16f),gripFixed=new Vector3f(-1/16f,6/16f,-2/16f),gripRotated=new Vector3f();
 private final List<Sole> soles=new ArrayList<>();
 private final JsonObject support;
 private String key="",previousClip="";
 private double clock=Double.NaN,transitionStart;
 private float duration,fromGround,ground;
 private boolean initialized;
 public float alpha=1,confidence,rawSole,finalSole;
 public float groundBlocks(){return ground*.9375f;}
 public float transitionSeconds(){return duration;}
 public Matrix4f anchor(){return anchor;}
 public float gripError(){anchor.transformPosition(gripPivot,gripRotated);return gripRotated.distance(gripFixed);}
 private record Sole(int node,float weight,Vector3f bindPoint,int sample){}
 private int sampleCount;
 private final float[] heights;

 public R3VisualPose(RuntimeModel model)throws Exception{this(model,"visual-support.json");}
 public R3VisualPose(RuntimeModel model,String supportFile)throws Exception{
  this.model=model;int n=model.getNodeCount();globals=new Matrix4f[n];local=new Matrix4f[n];inverse=new Matrix4f[n];inverseReady=new boolean[n];ready=new boolean[n];
  displayedP=new Vector3f[n];sourceP=new Vector3f[n];displayedS=new Vector3f[n];sourceS=new Vector3f[n];displayedQ=new Quaternionf[n];sourceQ=new Quaternionf[n];
  for(int i=0;i<n;i++){globals[i]=new Matrix4f();local[i]=new Matrix4f();inverse[i]=new Matrix4f();displayedP[i]=new Vector3f();sourceP[i]=new Vector3f();displayedS[i]=new Vector3f(1);sourceS[i]=new Vector3f(1);displayedQ[i]=new Quaternionf();sourceQ[i]=new Quaternionf();}
  try(var input=Minecraft.getInstance().getResourceManager().open(new ResourceLocation("towerofentropy",supportFile))){support=JsonParser.parseReader(new java.io.InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
  for(var node:model.getNodes()){
   if(!node.getName().startsWith("limb_4")&&!node.getName().startsWith("limb_5"))continue;
   if(node.getSkinIndex()<0)continue;var skin=model.getSkins().get(node.getSkinIndex());
   for(int mi:node.getMeshIndices()){
    var mesh=model.getMeshes().get(mi);var pos=mesh.getPositions();var weights=mesh.getJointWeights();var indices=mesh.getJointIndices();if(weights==null)continue;
    float minimum=Float.POSITIVE_INFINITY;for(int v=0;v<mesh.getVertexCount();v++)minimum=Math.min(minimum,pos[v*3+1]);
    for(int v=0;v<mesh.getVertexCount();v++)if(pos[v*3+1]<=minimum+.0001f){
     int sample=sampleCount++;
     for(int k=0;k<4;k++)if(weights[v*4+k]>0){int j=indices[v*4+k];var point=new Vector3f(pos[v*3],pos[v*3+1],pos[v*3+2]);skin.getInverseBindMatrices()[j].transformPosition(point);soles.add(new Sole(skin.getJointNodeIndices()[j],weights[v*4+k],point,sample));}
    }
   }
  }heights=new float[sampleCount];
 }
 public void reset(){initialized=false;key="";clock=Double.NaN;ground=0;}
 private static float smooth(float v){v=Math.max(0,Math.min(1,v));return v*v*(3-2*v);}
 private static boolean attack(String clip){return clip.startsWith("sword")||clip.equals("heavy");}
 private float blendDuration(String from,String to){
  if(to.equals("jump_start")&&from.startsWith("wall_slide"))return .07f;
  if(to.startsWith("punch_"))return .05f;
  if((from.equals("sword_c")||from.equals("heavy"))&&(HorizontalLocomotion.loop(to)||to.startsWith("jump_")))
   return (to.startsWith("jump_")?.12f:VisualTuning.RETURN.get().floatValue())*.5f;
  if(to.equals("roll"))return VisualTuning.ROLL.get().floatValue();
  if(to.equals("heavy"))return VisualTuning.HEAVY.get().floatValue();
  if(!PlayerRendererR3.PREVIOUS&&!VisualContext.PREVIOUS&&(VisualContext.own(from)||VisualContext.own(to)))return to.startsWith("hit_")?.06f:.12f;
  if(attack(from)&&HorizontalLocomotion.loop(to))return VisualTuning.RETURN.get().floatValue();
   if(!PlayerRendererR3.PREVIOUS&&from.equals("roll")&&HorizontalLocomotion.loop(to))return .08f;
  if(!PlayerRendererR3.PREVIOUS&&HorizontalLocomotion.loop(from)&&HorizontalLocomotion.loop(to))return VisualTuning.LOCOMOTION.get().floatValue();
  return VisualTuning.CHAIN.get().floatValue();
 }
 public Matrix4f[] apply(Matrix4f[] raw,String clip,long generation,float time,double now,Matrix4f targetAnchor,boolean grounded){
  float dt=Double.isFinite(clock)?(float)Math.max(0,Math.min(.25,now-clock)):0;
  if(Double.isFinite(clock)&&now-clock>.4)initialized=false;
  String next=clip+":"+(attack(clip)?generation:0);
  if(!next.equals(key)){
   duration=initialized?blendDuration(previousClip,clip):0;transitionStart=now;fromGround=ground;
   if(initialized&&duration>0){captureSource();anchorFromP.set(anchorP);anchorFromQ.set(anchorQ);}
   key=next;previousClip=clip;
  }
  alpha=duration<=0?1:smooth((float)((now-transitionStart)/duration));
  if(alpha>=1){
   // No active blend: retain the exact sampler matrices, without inversions/decomposition.
   for(int i=0;i<globals.length;i++)globals[i].set(raw[i]);
  }else{
  Arrays.fill(inverseReady,false);Arrays.fill(ready,false);
  for(int i=0;i<local.length;i++){
   int parent=model.getNodes().get(i).getParentIndex();
   if(parent>=0){if(!inverseReady[parent]){inverse[parent].set(raw[parent]).invert();inverseReady[parent]=true;}local[i].set(inverse[parent]).mul(raw[i]);}else local[i].set(raw[i]);
   local[i].getTranslation(p);local[i].getUnnormalizedRotation(q).normalize();local[i].getScale(s);
   if(alpha<1&&initialized){displayedP[i].set(sourceP[i]).lerp(p,alpha);displayedQ[i].set(sourceQ[i]).slerp(q,alpha).normalize();displayedS[i].set(sourceS[i]).lerp(s,alpha);}else{displayedP[i].set(p);displayedQ[i].set(q);displayedS[i].set(s);}
   local[i].translationRotateScale(displayedP[i],displayedQ[i],displayedS[i]);
  }
  for(int i=0;i<local.length;i++)global(i);
  }
  targetAnchor.getTranslation(anchorTargetP);targetAnchor.getUnnormalizedRotation(anchorTargetQ).normalize();
  if(alpha<1&&initialized){anchorP.set(anchorFromP).lerp(anchorTargetP,alpha);anchorQ.set(anchorFromQ).slerp(anchorTargetQ,alpha).normalize();}else{anchorP.set(anchorTargetP);anchorQ.set(anchorTargetQ);}
  // The approved anchor satisfies p + q*K = F. Preserve that constraint between samples too.
  anchorQ.transform(gripPivot,gripRotated);anchorP.set(gripFixed).sub(gripRotated);
  anchor.translation(anchorP).rotate(anchorQ);
  rawSole=soleHeight(globals);confidence=grounded&&VisualTuning.SUPPORT.get()?contact(clip,time):0;
  float limit=VisualTuning.GROUND_LIMIT.get().floatValue()/.9375f;
  float target=-Math.min(limit,Math.max(0,rawSole-.002f))*confidence;
  if(!support.has(clip)&&alpha<1)ground=fromGround*(1-alpha);
  else ground+=(target-ground)*(1-(float)Math.exp(-3*dt/VisualTuning.GROUND_RESPONSE.get()));
  if(!initialized)ground=target;
  for(var matrix:globals)matrix.m31(matrix.m31()+ground);
  finalSole=rawSole+ground;clock=now;initialized=true;return globals;
 }
 private Matrix4f global(int i){if(ready[i])return globals[i];int parent=model.getNodes().get(i).getParentIndex();if(parent>=0)globals[i].set(global(parent)).mul(local[i]);else globals[i].set(local[i]);ready[i]=true;return globals[i];}
 private void captureSource(){
  Arrays.fill(inverseReady,false);
  for(int i=0;i<globals.length;i++){
   int parent=model.getNodes().get(i).getParentIndex();
   if(parent>=0){if(!inverseReady[parent]){inverse[parent].set(globals[parent]).invert();inverseReady[parent]=true;}local[i].set(inverse[parent]).mul(globals[i]);}
   else{local[i].set(globals[i]);local[i].m31(local[i].m31()-ground);}
   local[i].getTranslation(sourceP[i]);local[i].getUnnormalizedRotation(sourceQ[i]).normalize();local[i].getScale(sourceS[i]);
  }
 }
 private float contact(String clip,float time){
  if(!support.has(clip))return 0;var data=support.getAsJsonObject(clip);var values=data.getAsJsonArray("confidence");float index=Math.max(0,Math.min(values.size()-1,time/data.get("duration").getAsFloat()*(values.size()-1)));int i=(int)index,j=Math.min(i+1,values.size()-1);return MthLerp(index-i,values.get(i).getAsFloat(),values.get(j).getAsFloat());
 }
 private static float MthLerp(float t,float a,float b){return a+(b-a)*t;}
 public float sourceContact(String clip,float time){return contact(clip,time);}
 public float soleHeight(Matrix4f[] matrices){
  Arrays.fill(heights,0);for(var v:soles){var m=matrices[v.node];var p=v.bindPoint;heights[v.sample]+=v.weight*(m.m01()*p.x+m.m11()*p.y+m.m21()*p.z+m.m31());}
  float min=Float.POSITIVE_INFINITY;for(float h:heights)min=Math.min(min,h);return Float.isFinite(min)?min:0;
 }
}
