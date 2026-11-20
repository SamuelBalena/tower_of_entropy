package com.towerofentropy.renderlab;
import java.util.*;
import dev.phe.polymesh.animation.AnimationController;
import dev.phe.polymesh.model.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
/** Reads actual loaded morph channels and evaluates the same morph-before-LBS order as Polymesh. */
public final class R31MorphAudit {
 public static List<Map<String,Object>> inspect(RuntimeModel model){
  var out=new ArrayList<Map<String,Object>>();var c=new AnimationController(model);c.play("UAL_roll",false);var skin=model.getSkins().get(0);var joints=skin.getJointNodeIndices();var ibm=skin.getInverseBindMatrices();
  for(int frame=0;frame<45;frame++){
   c.setAnimationTime(frame/30f);var globals=c.computeGlobalTransforms(0);float maxDelta=0,sum=0,minRing=Float.POSITIVE_INFINITY;
   for(int ni=0;ni<model.getNodeCount();ni++){
    var node=model.getNodes().get(ni);var weights=c.getNodeMorphWeights(ni);if(weights!=null)for(float w:weights)sum+=w;
    for(int mi:node.getMeshIndices()){
     var mesh=model.getMeshes().get(mi);var pos=mesh.getPositions();var morph=mesh.getMorphPositionDeltas();var ji=mesh.getJointIndices();var jw=mesh.getJointWeights();if(jw==null)continue;
     var rings=new TreeMap<Float,LinkedHashMap<String,Vector3f[]>>();
     for(int v=0;v<mesh.getVertexCount();v++){
      var base=new Vector4f(pos[v*3],pos[v*3+1],pos[v*3+2],1);var p=new Vector4f(base);
      if(weights!=null&&morph!=null)for(int k=0;k<Math.min(weights.length,morph.length);k++)if(weights[k]!=0&&morph[k]!=null){p.x+=weights[k]*morph[k][v*3];p.y+=weights[k]*morph[k][v*3+1];p.z+=weights[k]*morph[k][v*3+2];}
      var actual=new Vector4f();var original=new Vector4f();
      for(int k=0;k<4;k++)if(jw[v*4+k]>0){int j=ji[v*4+k];var mat=new Matrix4f(globals[joints[j]]).mul(ibm[j]);actual.add(mat.transform(new Vector4f(p)).mul(jw[v*4+k]));original.add(mat.transform(new Vector4f(base)).mul(jw[v*4+k]));}
      maxDelta=Math.max(maxDelta,new Vector3f(actual.x-original.x,actual.y-original.y,actual.z-original.z).length());
      if(!node.getName().endsWith("_outer")){float level=Math.round(base.y*100000f)/100000f;var ring=rings.computeIfAbsent(level,k->new LinkedHashMap<>());ring.putIfAbsent(String.format(Locale.ROOT,"%.6f/%.6f",base.x,base.z),new Vector3f[]{new Vector3f(base.x,base.y,base.z),new Vector3f(actual.x,actual.y,actual.z)});}
     }
     for(var ring:rings.values())if(ring.size()==4){var points=new ArrayList<>(ring.values());float cx=0,cz=0;for(var p:points){cx+=p[0].x/4;cz+=p[0].z/4;}final float x=cx,z=cz;points.sort(Comparator.comparingDouble(p->Math.atan2(p[0].z-z,p[0].x-x)));float a=area(points,0);if(a>1e-8)minRing=Math.min(minRing,area(points,1)/a);}
    }
   }
   var row=new LinkedHashMap<String,Object>();row.put("frame",frame+1);row.put("weightSum",sum);row.put("maxSkinnedDeltaBlocks",maxDelta);row.put("minBaseRingAreaRatio",minRing);out.add(row);
  }return out;
 }
 private static float area(List<Vector3f[]>p,int column){var a=p.get(0)[column];return new Vector3f(p.get(1)[column]).sub(a).cross(new Vector3f(p.get(2)[column]).sub(a)).length()/2+new Vector3f(p.get(2)[column]).sub(a).cross(new Vector3f(p.get(3)[column]).sub(a)).length()/2;}
}
