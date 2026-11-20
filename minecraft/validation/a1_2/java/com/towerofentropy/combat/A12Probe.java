package com.towerofentropy.combat;
import java.util.*;
import com.towerofentropy.client.*;
import org.joml.*;
import dev.phe.polymesh.model.RuntimeModel;

/** Read-only measurements of matrices actually handed to the existing renderer. */
final class A12Probe {
 private static RuntimeModel model;
 private static R3VisualPose sole;
 static Map<String,Object> sample(){
  var m=PlayerRendererR3.model();var displayed=PlayerRendererR3.displayed();var c=PlayerRendererR3.controller();
  if(m==null||displayed==null||c==null)return Map.of();
  try{if(model!=m){model=m;sole=new R3VisualPose(m);}}catch(Exception ex){throw new RuntimeException(ex);}
  var raw=c.computeGlobalTransforms(0);var result=new LinkedHashMap<String,Object>();var bones=new LinkedHashMap<String,Object>();
  float maxScaleError=0,maxLengthError=0;boolean finite=true;
  var joints=new HashSet<Integer>();for(int j:m.getSkins().get(0).getJointNodeIndices())joints.add(j);
  for(int j:joints){var a=displayed[j];var b=raw[j];var q=a.getUnnormalizedRotation(new Quaternionf()).normalize();var p=a.getTranslation(new Vector3f());
   bones.put(m.getNodes().get(j).getName(),Map.of("p",new float[]{p.x,p.y,p.z},"q",new float[]{q.x,q.y,q.z,q.w}));
   var sa=a.getScale(new Vector3f());var sb=b.getScale(new Vector3f());maxScaleError=java.lang.Math.max(maxScaleError,sa.distance(sb));finite&=a.isFinite();
   int parent=m.getNodes().get(j).getParentIndex();if(joints.contains(parent)){float al=p.distance(displayed[parent].getTranslation(new Vector3f())),bl=b.getTranslation(new Vector3f()).distance(raw[parent].getTranslation(new Vector3f()));maxLengthError=java.lang.Math.max(maxLengthError,java.lang.Math.abs(al-bl));}
  }
  var visual=PlayerRendererR3.visualPose();String clip=c.getAnimationName().replace("UAL_","");
  result.put("bones",bones);result.put("finite",finite);result.put("scaleError",maxScaleError);result.put("boneLengthError",maxLengthError);
  result.put("rawSoleBlocks",sole.soleHeight(raw)*.9375);result.put("soleBlocks",sole.soleHeight(displayed)*.9375);result.put("sourceContact",sole.sourceContact(clip,c.getAnimationTime()));
  result.put("blend",visual==null?1:visual.alpha);result.put("blendSeconds",visual==null?0:visual.transitionSeconds());result.put("groundBlocks",visual==null?0:visual.groundBlocks());
  result.put("gripError",visual==null?0:visual.gripError());
  return result;
 }
}
