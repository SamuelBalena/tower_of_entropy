package com.towerofentropy.combat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.towerofentropy.animation.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Authoritative bounded dash intent; vanilla travel handles terrain and gravity. */
@Mod.EventBusSubscriber(modid="towerofentropy")
public final class RollController {
 private static final Map<UUID,Session> SESSIONS=new ConcurrentHashMap<>();
 private static final Map<UUID,Status> LAST=new ConcurrentHashMap<>();
 public record Status(boolean busy,boolean started,long startTick,double x,double z,double distance,String reason){}
 private static final class Session {
  final boolean contextual;final RollRules.Direction direction;final ResourceKey<Level> dimension;final long requested;
  long start=-1;double originX,originZ,lastX,lastY,lastZ,distance;boolean motionEnded,finished;
  Session(ServerPlayer p,RollRules.Direction d,boolean contextual){this.contextual=contextual;direction=d;dimension=p.level().dimension();requested=p.serverLevel().getGameTime();lastX=p.getX();lastY=p.getY();lastZ=p.getZ();}
 }
 public static boolean busy(UUID id){return SESSIONS.containsKey(id);}
 public static boolean started(UUID id){var s=SESSIONS.get(id);return s!=null&&s.start>=0;}
 public static Status status(UUID id){return LAST.get(id);}
 private static void publish(ServerPlayer p,Session s,String reason){LAST.put(p.getUUID(),new Status(true,s.start>=0,s.start,s.direction.x(),s.direction.z(),s.distance,reason));}
 public static String requestContextual(ServerPlayer p,float left,float forward)throws Exception{
  if(WeaponReadiness.threat(p)!=WeaponReadiness.ThreatMode.COMBAT)return "combat inactive";
  var attack=CombatController.status(p.getUUID());
  if(attack!=null&&attack.attacking())return "attack committed";
  return request(p,left,forward,true);
 }
 public static String request(ServerPlayer p,float left,float forward)throws Exception{
  return request(p,left,forward,false);
 }
 private static String request(ServerPlayer p,float left,float forward,boolean contextual)throws Exception{
  if(HeavyAirLock.active(p))return "attack committed";
  CombatController.refresh(p);
  if(!contextual&&!CombatController.enabled(p))return "combat inactive";
  var direction=RollRules.direction(left,forward,p.getYRot());if(direction==null)return "invalid direction";
  if(busy(p.getUUID()))return "busy";
  if(!LocomotionController.supportedAction(p)||p.containerMenu!=p.inventoryMenu)return "unsupported state";
  p.setSprinting(false);
  // Validate before canceling: malformed/invalid Rolls never destroy an attack.
  LocomotionController.cancelAction(p,Boolean.getBoolean("toe.a12.legacy"));
  String result;try{result=LocomotionController.requestAction(p,VisualActionWindow.Action.ROLL);}catch(Exception ex){ActionNetwork.combatState(p,true,"",0,0);throw ex;}if(!result.equals("accepted")){ActionNetwork.combatState(p,true,"",0,0);return result;}
  var s=new Session(p,direction,contextual);SESSIONS.put(p.getUUID(),s);zeroHorizontal(p);
  // Polymesh owns the visual clock. Optional CPM registry readiness must not delay a physical action.
  animationStarted(p);return result;
 }
 public static void animationStarted(ServerPlayer p){var s=SESSIONS.get(p.getUUID());if(s==null||s.start>=0)return;s.start=p.serverLevel().getGameTime();s.originX=p.getX();s.originZ=p.getZ();s.lastX=p.getX();s.lastY=p.getY();s.lastZ=p.getZ();publish(p,s,"moving");ActionNetwork.rollState(p,true,s.start,s.direction.x(),s.direction.z());}
 public static boolean iframe(ServerPlayer p){var s=SESSIONS.get(p.getUUID());if(s==null||s.start<0)return false;long t=p.serverLevel().getGameTime()-s.start;return t>=3&&t<10;}
 public static void finished(ServerPlayer p,long start){
  var s=SESSIONS.get(p.getUUID());if(s==null||s.start!=start)return;s.finished=true;
  double dx=p.getX()-s.originX,dz=p.getZ()-s.originZ,progress=dx*s.direction.x()+dz*s.direction.z();
  if(!RollRules.movementAllowed(progress,Math.abs(dx*s.direction.z()-dz*s.direction.x()),Math.hypot(p.getX()-s.lastX,p.getZ()-s.lastZ),p.getY()-s.lastY)){p.connection.teleport(s.lastX,s.lastY,s.lastZ,p.getYRot(),p.getXRot());cancel(p,"movement bounds");LocomotionController.finishRoll(p);return;}
  s.distance=Math.max(0,progress);
  // The final vanilla position packet precedes this acknowledgement on the same ordered connection.
  if(p.serverLevel().getGameTime()-s.start>=RollRules.MOVE_TICKS-1){cancel(p,"complete");LocomotionController.finishRoll(p);}
 }
 private static void zeroHorizontal(ServerPlayer p){var v=p.getDeltaMovement();p.setDeltaMovement(0,v.y,0);p.hurtMarked=true;}
 public static void cancel(ServerPlayer p,String reason){var s=SESSIONS.remove(p.getUUID());if(s==null)return;
  if(s.contextual&&Boolean.getBoolean("toe.combat.roll.trace"))
   com.mojang.logging.LogUtils.getLogger().info("TOE_W0021_ROLL_END player={} reason={} distance={}",p.getScoreboardName(),reason,s.distance);
  zeroHorizontal(p);LAST.put(p.getUUID(),new Status(false,s.start>=0,s.start,s.direction.x(),s.direction.z(),s.distance,reason));ActionNetwork.rollState(p,false,s.start,s.direction.x(),s.direction.z());}
 private static boolean valid(ServerPlayer p,Session s){return p.isAlive()&&!p.isSpectator()&&(s.contextual?WeaponTags.isWeapon(p.getMainHandItem())&&WeaponReadiness.canUse(p,net.minecraft.world.InteractionHand.MAIN_HAND):CombatController.enabled(p))&&p.level().dimension()==s.dimension&&p.containerMenu==p.inventoryMenu&&!p.isInWaterOrBubble()&&!p.isInLava()&&!p.onClimbable()&&!p.isPassenger()&&!p.isFallFlying()&&!p.getAbilities().flying&&!p.isCrouching()&&!p.isUsingItem()&&p.hurtTime==0;}
 @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
  var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();if(server==null)return;
  for(var e:SESSIONS.entrySet()){
   var p=server.getPlayerList().getPlayer(e.getKey());var s=e.getValue();if(p==null){SESSIONS.remove(e.getKey());continue;}long now=p.serverLevel().getGameTime();
   if(!valid(p,s)||now-s.requested>180){cancel(p,"invalid state");LocomotionController.cancelAction(p);continue;}
   if(s.start<0){if(event.phase==TickEvent.Phase.START)zeroHorizontal(p);continue;}
   long elapsed=now-s.start;
   if(event.phase==TickEvent.Phase.END){
    double dx=p.getX()-s.originX,dz=p.getZ()-s.originZ,progress=dx*s.direction.x()+dz*s.direction.z(),lateral=Math.abs(dx*s.direction.z()-dz*s.direction.x()),step=Math.hypot(p.getX()-s.lastX,p.getZ()-s.lastZ);
    if(!RollRules.movementAllowed(progress,lateral,step,p.getY()-s.lastY)){p.connection.teleport(s.lastX,s.lastY,s.lastZ,p.getYRot(),p.getXRot());cancel(p,"movement bounds");LocomotionController.cancelAction(p);continue;}
    s.distance=Math.max(0,progress);s.lastX=p.getX();s.lastY=p.getY();s.lastZ=p.getZ();publish(p,s,s.motionEnded?"recovery":"moving");
    if((s.finished&&elapsed>=RollRules.RECOVERY_TICKS)||elapsed>=20){cancel(p,"complete");LocomotionController.finishRoll(p);}
   }
  }
 }
 @SubscribeEvent public static void death(LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p){cancel(p,"death");LocomotionController.cancelAction(p);}}
 @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)cancel(p,"logout");LAST.remove(e.getEntity().getUUID());}
 @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){SESSIONS.remove(e.getEntity().getUUID());LAST.remove(e.getEntity().getUUID());}
 @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p)cancel(p,"respawn");LAST.remove(e.getEntity().getUUID());}
 @SubscribeEvent public static void stop(ServerStoppedEvent e){SESSIONS.clear();LAST.clear();}
}
