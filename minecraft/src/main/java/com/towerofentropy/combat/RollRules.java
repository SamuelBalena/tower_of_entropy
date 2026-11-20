package com.towerofentropy.combat;
/** Pure server policy, independent of either visual backend. */
public final class RollRules {
 public static final double DISTANCE=2.5,SPEED=DISTANCE/14;
 public static final int MOVE_TICKS=14,RECOVERY_TICKS=14;
 public record Direction(double x,double z){}
 public static boolean movementAllowed(double progress,double lateral,double step,double rise){return Double.isFinite(progress)&&Double.isFinite(lateral)&&Double.isFinite(step)&&Double.isFinite(rise)&&progress<=DISTANCE+.15&&progress>=-.35&&lateral>=0&&lateral<=.35&&step>=0&&step<=.8&&rise<=.7;}
 public static Direction direction(float left,float forward,float yaw){
  if(!Float.isFinite(left)||!Float.isFinite(forward)||!Float.isFinite(yaw)||Math.abs(left)>1||Math.abs(forward)>1)return null;
  double length=Math.hypot(left,forward);if(length<1e-5){forward=1;length=1;}
  double r=Math.toRadians(yaw),x=(left*Math.cos(r)-forward*Math.sin(r))/length,z=(forward*Math.cos(r)+left*Math.sin(r))/length;
  return new Direction(x,z);
 }
}
