package com.towerofentropy.client;
import dev.phe.polymesh.model.RuntimeModel;
import dev.phe.polymesh.animation.AnimationController;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.*;
/** Local bone composition on the same approved rig; legs/hips retain physical-state locomotion. */
public final class UpperBodyPose {
 private static final Set<String> UPPER=Set.of("waist","1","0","2","3","102","103");
 private final RuntimeModel model;private final AnimationController lower;private final Matrix4f[] local,world;private final boolean[] ready;
 public UpperBodyPose(RuntimeModel m){model=m;lower=new AnimationController(m);local=new Matrix4f[m.getNodeCount()];world=new Matrix4f[local.length];ready=new boolean[local.length];for(int i=0;i<local.length;i++){local[i]=new Matrix4f();world[i]=new Matrix4f();}}
 // Mirror skin deformation in model space, then restore each target joint's bind pose.
 public void mirrorUpperBody(Matrix4f[] pose){
  var skin=model.getSkins().get(0);
  int[] joints=skin.getJointNodeIndices();
  var inverseBind=skin.getInverseBindMatrices();
  Matrix4f reflection=new Matrix4f().scaling(-1,1,1);
  for(int target=0;target<joints.length;target++){
   String name=model.getNodes().get(joints[target]).getName();
   if(!UPPER.contains(name))continue;
   String sourceName=switch(name){case "2"->"3";case "3"->"2";case "102"->"103";case "103"->"102";default->name;};
   int source=0;
   while(!model.getNodes().get(joints[source]).getName().equals(sourceName))source++;
   local[joints[target]].set(inverseBind[target]).invert();
   world[joints[target]].set(reflection).mul(pose[joints[source]]).mul(inverseBind[source])
     .mul(reflection).mul(local[joints[target]]);
  }
  for(int joint:joints)if(UPPER.contains(model.getNodes().get(joint).getName()))pose[joint].set(world[joint]);
 }
 public Matrix4f[] compose(Matrix4f[] upper,String clip,float phase,float pitch,float partial){lower.play("UAL_"+clip,true);float duration=lower.getCurrentAnimation().getDuration();lower.setAnimationTime(clip.startsWith("jump_")?VisualContext.time(clip,duration,partial):phase*duration);var base=lower.computeGlobalTransforms(0);Arrays.fill(ready,false);
  for(int i=0;i<local.length;i++){var n=model.getNodes().get(i);String name=n.getName();boolean useBase=name.equals("pelvis")||name.equals("4")||name.equals("5")||name.equals("104")||name.equals("105");var source=useBase?base:upper;int p=n.getParentIndex();if(p>=0)local[i].set(source[p]).invert().mul(source[i]);else local[i].set(source[i]);}
  for(int i=0;i<local.length;i++)global(i);
  if(pitch!=0){int hip=lower.getNodeIndex("pelvis");var p=world[hip].getTranslation(new Vector3f());var rotation=new Matrix4f().translation(p).rotateX((float)Math.toRadians(-pitch)).translate(-p.x,-p.y,-p.z);for(int i=0;i<world.length;i++){String n=model.getNodes().get(i).getName();if(UPPER.contains(n))rotation.mul(world[i],world[i]);}}
  return world;
 }
 private Matrix4f global(int i){if(ready[i])return world[i];int p=model.getNodes().get(i).getParentIndex();if(p>=0)world[i].set(global(p)).mul(local[i]);else world[i].set(local[i]);ready[i]=true;return world[i];}
 public static void aim(AnimationController c,Matrix4f[] matrices,float pitch){if(pitch==0)return;var p=matrices[c.getNodeIndex("pelvis")].getTranslation(new Vector3f());var rotation=new Matrix4f().translation(p).rotateX((float)Math.toRadians(-pitch)).translate(-p.x,-p.y,-p.z);for(String name:UPPER){int i=c.getNodeIndex(name);rotation.mul(matrices[i],matrices[i]);}}
 public static float facing(RuntimeModel m,AnimationController c,Matrix4f[] matrices){int h=c.getNodeIndex("0");var skin=m.getSkins().get(0);int[] nodes=skin.getJointNodeIndices();for(int j=0;j<nodes.length;j++)if(nodes[j]==h){var direction=new Matrix4f(matrices[h]).mul(skin.getInverseBindMatrices()[j]).transformDirection(new Vector3f(0,0,-1));return (float)Math.toDegrees(Math.atan2(-direction.x,-direction.z));}return 0;}
}
