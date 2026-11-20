package com.towerofentropy.combat;

import java.util.*;import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Reconcile only a voluntary jump reported during a grounded strike; never clamp falling or world impulses. */
@Mod.EventBusSubscriber(modid="towerofentropy")
public final class AttackJumpGuard {
 private record Denied(double y,long until){}
 private static final Map<UUID,Denied> DENIED=new ConcurrentHashMap<>();
 private static final Map<UUID,Long> EXTERNAL_UNTIL=new ConcurrentHashMap<>();
 private static final Map<UUID,net.minecraft.world.phys.Vec3> BEFORE=new ConcurrentHashMap<>();
 private static final Map<UUID,Integer> REJECTED=new ConcurrentHashMap<>();
 private static boolean attacking(ServerPlayer p){return AttackMovement.committed(p.getUUID());}
 public static int rejected(UUID id){return REJECTED.getOrDefault(id,0);}
 @SubscribeEvent(priority=EventPriority.HIGHEST) public static void forces(TickEvent.ServerTickEvent e){
  var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();if(server==null)return;
  if(e.phase==TickEvent.Phase.END){for(var p:server.getPlayerList().getPlayers())reconcile(p);return;}
  for(var p:server.getPlayerList().getPlayers())if(attacking(p)){BEFORE.put(p.getUUID(),p.getDeltaMovement());if(p.getDeltaMovement().y>.08)EXTERNAL_UNTIL.put(p.getUUID(),p.serverLevel().getGameTime()+6);}
 }
 @SubscribeEvent public static void knockback(LivingKnockBackEvent e){if(e.getEntity() instanceof ServerPlayer p)EXTERNAL_UNTIL.put(p.getUUID(),p.serverLevel().getGameTime()+10);}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void jump(LivingEvent.LivingJumpEvent e){
  if(!(e.getEntity() instanceof ServerPlayer p)||!attacking(p)||p.hurtTime>0||!p.onGround())return;
  if(EXTERNAL_UNTIL.getOrDefault(p.getUUID(),-1L)>=p.serverLevel().getGameTime())return;
  DENIED.put(p.getUUID(),new Denied(p.getY(),p.serverLevel().getGameTime()+3));REJECTED.merge(p.getUUID(),1,Integer::sum);
  var v=BEFORE.getOrDefault(p.getUUID(),net.minecraft.world.phys.Vec3.ZERO);p.setDeltaMovement(v.x,0,v.z);p.setSprinting(false);p.setJumping(false);
 }
 @SubscribeEvent(priority=EventPriority.HIGHEST) public static void reconcile(TickEvent.PlayerTickEvent e){
  if(e.player instanceof ServerPlayer p)reconcile(p);
 }
 private static void reconcile(ServerPlayer p){
  var d=DENIED.get(p.getUUID());if(d==null)return;long now=p.serverLevel().getGameTime();
  boolean support=p.level().getBlockCollisions(p,new net.minecraft.world.phys.AABB(p.getX()-.2,d.y-.12,p.getZ()-.2,p.getX()+.2,d.y+.001,p.getZ()+.2)).iterator().hasNext();
  if(!attacking(p)||now>d.until||p.getY()<d.y-.02||!support||p.hurtTime>0||EXTERNAL_UNTIL.getOrDefault(p.getUUID(),-1L)>=now){DENIED.remove(p.getUUID());return;}
  if(p.getY()>d.y+.01){var v=p.getDeltaMovement();p.connection.teleport(p.getX(),d.y,p.getZ(),p.getYRot(),p.getXRot());p.setDeltaMovement(v.x,0,v.z);}
  p.setOnGround(true);p.setJumping(false);
 }
 private static void clear(UUID id){DENIED.remove(id);EXTERNAL_UNTIL.remove(id);BEFORE.remove(id);REJECTED.remove(id);}
 @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){clear(e.getEntity().getUUID());}
 @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){clear(e.getEntity().getUUID());}
 @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){clear(e.getEntity().getUUID());}
 @SubscribeEvent public static void stop(net.minecraftforge.event.server.ServerStoppedEvent e){DENIED.clear();EXTERNAL_UNTIL.clear();BEFORE.clear();REJECTED.clear();}
}
