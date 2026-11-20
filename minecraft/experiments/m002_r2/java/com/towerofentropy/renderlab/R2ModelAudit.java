package com.towerofentropy.renderlab;

import java.util.*;
import dev.phe.polymesh.animation.AnimationController;
import dev.phe.polymesh.model.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Measures the loaded library model, not just the original JSON. */
public final class R2ModelAudit {
    public static Map<String,Object> inspect(RuntimeModel m){
        int weighted=0,blended=0;float maxWeightError=0,maxBindError=0,maxBlendDifference=0;
        var bind=new Matrix4f[m.getNodeCount()];for(int i=0;i<bind.length;i++)global(m,i,bind);
        var s=m.getSkins().get(0);var joints=s.getJointNodeIndices();var ibm=s.getInverseBindMatrices();
        for(int i=0;i<joints.length;i++){var mat=new Matrix4f(bind[joints[i]]).mul(ibm[i]);for(int x=0;x<4;x++)for(int y=0;y<4;y++)maxBindError=Math.max(maxBindError,Math.abs(mat.get(x,y)-(x==y?1:0)));}
        var c=new AnimationController(m);c.play("UAL_jog",true);c.setAnimationTime(7/30f);var pose=c.computeGlobalTransforms(0);
        for(var mesh:m.getMeshes()){
            var w=mesh.getJointWeights();var ji=mesh.getJointIndices();if(w==null)continue;
            for(int v=0;v<mesh.getVertexCount();v++){
                ++weighted;float sum=0,max=-1;int nonzero=0,dominant=0;
                var p=new Vector4f(mesh.getPositions()[v*3],mesh.getPositions()[v*3+1],mesh.getPositions()[v*3+2],1);
                var total=new Vector4f();
                for(int k=0;k<4;k++){float weight=w[v*4+k];sum+=weight;if(weight<=0)continue;++nonzero;
                    int j=ji[v*4+k];if(weight>max){max=weight;dominant=j;}
                    var matrix=new Matrix4f(pose[joints[j]]).mul(ibm[j]);total.add(matrix.transform(new Vector4f(p)).mul(weight));
                }
                maxWeightError=Math.max(maxWeightError,Math.abs(sum-1));
                if(nonzero>1){++blended;var rigid=new Matrix4f(pose[joints[dominant]]).mul(ibm[dominant]).transform(new Vector4f(p));maxBlendDifference=Math.max(maxBlendDifference,new Vector3f(total.x-rigid.x,total.y-rigid.y,total.z-rigid.z).length());}
            }
        }
        return Map.of("weightedVertices",weighted,"blendedVertices",blended,"maxWeightSumError",maxWeightError,"maxBindIdentityError",maxBindError,"maxBlendVsRigidBlocks",maxBlendDifference,"joints",joints.length,"animations",m.getAnimations().stream().map(RuntimeAnimation::getName).toList());
    }
    private static Matrix4f global(RuntimeModel m,int i,Matrix4f[] matrices){
        if(matrices[i]!=null)return matrices[i];var n=m.getNodes().get(i);var p=n.getParentIndex();
        return matrices[i]=p<0?new Matrix4f(n.getLocalTransform()):new Matrix4f(global(m,p,matrices)).mul(n.getLocalTransform());
    }
}
