package com.towerofentropy.combat;
import java.util.*;import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.towerofentropy.animation.ActionNetwork;
/** Server envelope for an authorized strike. Reconciliation is exceptional, never the lunge mechanism. */
@Mod.EventBusSubscriber(modid="towerofentropy")
public final class AttackMovement {
 private static final Map<UUID,Session> SESSIONS=new ConcurrentHashMap<>();
 public record Status(long start,double distance,double measured,int corrections){}
 private static final Map<UUID,Status> LAST=new ConcurrentHashMap<>();
 private static final class Session{long start;int from,ticks,duration;double x,z,distance,external,lastImpulse;Vec3 origin,last;net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;}
 public static boolean committed(UUID id){return SESSIONS.containsKey(id);}
 public static Status status(UUID id){return LAST.get(id);}
 public static void begin(ServerPlayer p,CombatMove move,long start){
  var s=new Session();s.dimension=p.level().dimension();s.start=start;s.from=AttackMotionRules.from(move);s.ticks=AttackMotionRules.ticks(move);s.duration=move.durationTicks;
  double yaw=Math.toRadians(p.getYRot());s.x=-Math.sin(yaw);s.z=Math.cos(yaw);s.distance=CombatConfig.advance(move);s.origin=p.position();s.last=s.origin;
  p.setSprinting(false);var v=p.getDeltaMovement();
  SESSIONS.put(p.getUUID(),s);LAST.put(p.getUUID(),new Status(start,s.distance,0,0));
  ActionNetwork.attackMotion(p,true,start,s.x,s.z,s.distance,s.from,s.ticks,move.durationTicks,v.x,v.z);
 }
 public static void end(ServerPlayer p){var s=SESSIONS.remove(p.getUUID());if(s!=null)ActionNetwork.attackMotion(p,false,s.start,0,0,0,0,1,0,0,0);}
 @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
  if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p))return;
  var s=SESSIONS.get(p.getUUID());if(s==null)return;if(!p.onGround()){end(p);return;}var action=CombatController.status(p.getUUID());
  if(p.level().dimension()!=s.dimension||!CombatController.valid(p)){CombatController.cancel(p);return;}
  if(p.serverLevel().getGameTime()-s.start>=s.duration)return;
  if(action==null||!action.attacking()||!p.isAlive()||p.hurtTime>0){end(p);return;}
  // Piston movement is a world force, not manual input: release commitment instead of rejecting it.
  for(var pos:net.minecraft.core.BlockPos.betweenClosed(p.blockPosition().offset(-1,-1,-1),p.blockPosition().offset(1,1,1)))
   if(p.level().getBlockState(pos).is(net.minecraft.world.level.block.Blocks.MOVING_PISTON)){CombatController.cancel(p);return;}
  double dx=p.getX()-s.origin.x,dz=p.getZ()-s.origin.z;
  double allowed=AttackMotionRules.scheduled(s.distance,s.from,s.ticks,p.serverLevel().getGameTime()-s.start+2);
  double forward=dx*s.x+dz*s.z,lateral=Math.abs(dx*s.z-dz*s.x);
  if(forward>allowed+s.external+.08||forward< -s.external-.08||lateral>s.external+.08){
   p.connection.teleport(s.last.x,s.last.y,s.last.z,p.getYRot(),p.getXRot());
   LAST.put(p.getUUID(),new Status(s.start,s.distance,Math.max(0,forward),1));CombatController.cancel(p);return;
  }
  s.last=p.position();LAST.put(p.getUUID(),new Status(s.start,s.distance,Math.max(0,forward),0));
 }
 @SubscribeEvent public static void forces(TickEvent.ServerTickEvent e){
  if(e.phase!=TickEvent.Phase.START)return;var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();if(server==null)return;
  for(var entry:SESSIONS.entrySet()){var p=server.getPlayerList().getPlayer(entry.getKey());if(p==null)continue;var s=entry.getValue();double impulse=p.getDeltaMovement().horizontalDistance();
   boolean piston=false;for(var pos:net.minecraft.core.BlockPos.betweenClosed(p.blockPosition().offset(-1,-1,-1),p.blockPosition().offset(1,1,1)))if(p.level().getBlockState(pos).is(net.minecraft.world.level.block.Blocks.MOVING_PISTON)){piston=true;break;}
   if(piston){CombatController.cancel(p);continue;}
   // A server-created impulse can coast under vanilla friction. Never derive this credit from client positions.
   s.external+=Math.max(0,impulse-s.lastImpulse)*12;s.lastImpulse=impulse;
  }
 }
 @SubscribeEvent public static void teleport(net.minecraftforge.event.entity.EntityTeleportEvent e){if(e.getEntity() instanceof ServerPlayer p&&SESSIONS.containsKey(p.getUUID()))CombatController.cancel(p);}
 @SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){SESSIONS.remove(e.getEntity().getUUID());LAST.remove(e.getEntity().getUUID());}
 @SubscribeEvent public static void stop(net.minecraftforge.event.server.ServerStoppedEvent e){SESSIONS.clear();LAST.clear();}
}
