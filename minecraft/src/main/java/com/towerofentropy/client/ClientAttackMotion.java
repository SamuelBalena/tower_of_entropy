package com.towerofentropy.client;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.MoverType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import com.towerofentropy.combat.AttackMotionRules;
/** Approved one-shot motion; no key polling, targets, damage or client-selected distance. */
@Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT)
public final class ClientAttackMotion {
 private static boolean active;private static long start;private static double x,z,distance,consumed;private static int from,ticks,duration,lastTick=-1,pendingTicks,rollHold;
 public static void rollRequested(){if(active())rollHold=10;}
 public static void rollAnswered(){rollHold=0;}
 public static boolean active(){return (pendingTicks>0||active)&&!ClientRollMotion.busy();}
 public static void pending(){var p=Minecraft.getInstance().player;if(!active()&&p!=null&&p.hurtTime==0&&p.onGround()){pendingTicks=10;var v=p.getDeltaMovement();p.setDeltaMovement(0,v.y,0);p.setSprinting(false);}}
 public static void accept(boolean on,long began,double dx,double dz,double d,int f,int n,int total,double externalX,double externalZ){
  var p=Minecraft.getInstance().player;
  if(on&&p!=null&&!p.onGround()){clear();return;}
  if(on&&active&&start==began)return;
  boolean continuation=active;
  active=on;pendingTicks=0;rollHold=0;consumed=0;start=began;x=dx;z=dz;distance=d;from=f;ticks=n;duration=total;lastTick=-1;
  if(on&&p!=null&&p.hurtTime==0){var v=p.getDeltaMovement();if(!continuation)p.setDeltaMovement(externalX,v.y,externalZ);p.setSprinting(false);}
 }
 public static void clear(){active=false;pendingTicks=0;rollHold=0;}
 @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent e){if(e.phase!=net.minecraftforge.event.TickEvent.Phase.START)return;if(pendingTicks>0)pendingTicks--;if(rollHold>0)rollHold--;var p=Minecraft.getInstance().player;if(active&&(p==null||!p.isAlive()||p.level().getGameTime()-start>duration+2))clear();}
 @SubscribeEvent public static void input(MovementInputUpdateEvent e){
  var p=Minecraft.getInstance().player;if(p==null||e.getEntity()!=p)return;
  if(ClientCombatState.heavyAir()&&p.hurtTime==0){
   var input=e.getInput();input.forwardImpulse=0;input.leftImpulse=0;
   input.up=false;input.down=false;input.left=false;input.right=false;input.jumping=false;
   p.setSprinting(false);
   var v=p.getDeltaMovement();
   double drag=com.towerofentropy.combat.HeavyAirLock.AIR_DRAG;
   p.setDeltaMovement(v.x*drag,v.y,v.z*drag);
   return;
  }
  if(!active())return;
  if(!p.onGround()){clear();return;}
  var in=e.getInput();in.forwardImpulse=0;in.leftImpulse=0;in.up=false;in.down=false;in.left=false;in.right=false;p.setSprinting(false);
  if(p.hurtTime>0){clear();return;}if(pendingTicks>0||rollHold>0)return;
  if(lastTick==p.tickCount)return;lastTick=p.tickCount;
  double age=p.level().getGameTime()-start;if(age<0||age>=duration)return;
  if(age<from||age>=from+ticks)return;
  double step=Math.min(distance-consumed,Math.min(distance/ticks*2,(distance-consumed)/Math.max(1,from+ticks-age)));consumed+=step;
  if(step>0)p.move(MoverType.SELF,AttackMotionRules.safeStep(p,x,z,step));
 }
 @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){clear();}
}
