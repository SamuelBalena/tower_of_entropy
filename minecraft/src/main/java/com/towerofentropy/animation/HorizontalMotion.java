package com.towerofentropy.animation;
/** Visual selection only. Speed is blocks/tick, direction relative to player yaw. */
public final class HorizontalMotion {
 public enum State {IDLE,WALK,JOG,SPRINT,MOVE_BACKWARD,STRAFE_LEFT,STRAFE_RIGHT}
 private State state=State.IDLE;private double speed;
 public State state(){return state;}public double speed(){return speed;}
 public boolean directionalFallback(){return state==State.MOVE_BACKWARD||state==State.STRAFE_LEFT||state==State.STRAFE_RIGHT;}
 public String clip(){return switch(state){case IDLE->"idle";case WALK->"walk";case SPRINT->"sprint";default->"jog";};}
 public void reset(){state=State.IDLE;speed=0;}
 public State update(double dx,double dz,float yaw,boolean sprint){
  double actual=Math.hypot(dx,dz);if(!Double.isFinite(actual)||actual>1){reset();return state;}
  speed+=(actual-speed)*.55;
  if(actual<.003||speed<(state==State.IDLE?.018:.009)){state=State.IDLE;return state;}
  double r=Math.toRadians(yaw),forward=(-Math.sin(r)*dx+Math.cos(r)*dz)/actual,left=(Math.cos(r)*dx+Math.sin(r)*dz)/actual;
  if(forward<(state==State.MOVE_BACKWARD?-.30:-.48))state=State.MOVE_BACKWARD;
  else if(Math.abs(left)>(directionalFallback()?.77:.87))state=left>0?State.STRAFE_LEFT:State.STRAFE_RIGHT;
  else if(sprint&&speed>.14)state=State.SPRINT;
  else if(speed<(state==State.WALK?.155:.115))state=State.WALK;
  else state=State.JOG;
  return state;
 }
}
