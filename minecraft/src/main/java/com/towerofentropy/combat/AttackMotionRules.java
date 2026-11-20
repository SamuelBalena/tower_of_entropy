package com.towerofentropy.combat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.*;
/** Shared collision/terrain policy; distances and direction originate on the server. */
public final class AttackMotionRules {
 public static int from(CombatMove m){return switch(m){case LIGHT_ATTACK_A->1;case LIGHT_ATTACK_B->2;case LIGHT_ATTACK_C->10;case HEAVY_ATTACK->8;};}
 public static int ticks(CombatMove m){return switch(m){case LIGHT_ATTACK_A,LIGHT_ATTACK_B->4;case LIGHT_ATTACK_C,HEAVY_ATTACK->6;};}
 public static double scheduled(double distance,int from,int ticks,double elapsed){return distance*Math.max(0,Math.min(1,(elapsed-from)/ticks));}
 public static Vec3 safeStep(Entity p,double x,double z,double amount){
  if(amount<=0||!p.onGround())return Vec3.ZERO;
  double lo=0,hi=amount;
  if(clear(p,x*hi,z*hi))return new Vec3(x*hi,0,z*hi);
  for(int i=0;i<10;i++){double mid=(lo+hi)/2;if(clear(p,x*mid,z*mid))lo=mid;else hi=mid;}
  return new Vec3(x*lo,0,z*lo);
 }
 private static boolean clear(Entity p,double dx,double dz){
  AABB box=p.getBoundingBox().move(dx,0,dz);
  if(!p.level().noCollision(p,box.deflate(.001)))return false;
  if(!p.level().getEntities(p,box.deflate(.01),e->e.isPushable()&&!e.isSpectator()).isEmpty())return false;
  for(double x:new double[]{box.minX+.07,box.maxX-.07})for(double z:new double[]{box.minZ+.07,box.maxZ-.07})
   if(!p.level().getBlockCollisions(p,new AABB(x-.025,box.minY-.15,z-.025,x+.025,box.minY+.001,z+.025)).iterator().hasNext())return false;
  return true;
 }
}
