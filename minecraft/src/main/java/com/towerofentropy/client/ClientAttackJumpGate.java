package com.towerofentropy.client;

import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

/** A held jump must be released after commitment. No user setting or world velocity is disabled. */
@Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT)
public final class ClientAttackJumpGate {
 private static final Field AUTO_JUMP=ObfuscationReflectionHelper.findField(LocalPlayer.class,"f_108613_");
 private static final Field JUMP_DELAY=ObfuscationReflectionHelper.findField(LivingEntity.class,"f_20954_");
 private static boolean releaseRequired,blockedThisTick;
 private static double previousY;
 public static boolean waitingRelease(){return releaseRequired;}
 private static void timers(LocalPlayer p){try{AUTO_JUMP.setInt(p,0);JUMP_DELAY.setInt(p,0);}catch(IllegalAccessException e){throw new IllegalStateException("Cannot clear committed jump timers",e);}}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void input(MovementInputUpdateEvent e){
  var mc=Minecraft.getInstance();var p=mc.player;if(p==null||e.getEntity()!=p)return;
  boolean held=mc.options.keyJump.isDown(),attack=ClientAttackMotion.active();
  if(!held&&mc.screen==null)releaseRequired=false;
  if(attack&&held)releaseRequired=true;
  if(attack||releaseRequired){previousY=p.getDeltaMovement().y;e.getInput().jumping=false;p.setJumping(false);timers(p);blockedThisTick=true;}
 }
 @SubscribeEvent public static void jump(LivingEvent.LivingJumpEvent e){
  var p=Minecraft.getInstance().player;if(p==null||e.getEntity()!=p||(!ClientAttackMotion.active()&&!releaseRequired))return;
  var v=p.getDeltaMovement();p.setDeltaMovement(v.x,previousY,v.z);p.setJumping(false);blockedThisTick=true;
 }
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void tick(TickEvent.ClientTickEvent e){if(e.phase==TickEvent.Phase.END&&blockedThisTick){var p=Minecraft.getInstance().player;if(p!=null)timers(p);blockedThisTick=false;}}
 @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut e){releaseRequired=false;blockedThisTick=false;previousY=0;}
}
