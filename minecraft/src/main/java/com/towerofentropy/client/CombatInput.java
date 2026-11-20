package com.towerofentropy.client;

import com.towerofentropy.animation.ActionNetwork;
import com.towerofentropy.combat.WeaponTags;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Consumes the remappable ATTACK mapping only in the implemented combat context. */
@Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT)
public final class CombatInput {
 private static boolean heldAttack,heldUse;
 @SubscribeEvent(priority=EventPriority.HIGHEST)
 public static void weaponBlock(InputEvent.InteractionKeyMappingTriggered event) {
  var mc=Minecraft.getInstance();
  if(mc.player!=null&&!ClientWeaponState.canUse(event.getHand())&&event.isAttack()){
   event.setSwingHand(false);
   event.setCanceled(true);
   return;
  }
  if(!event.isAttack()||mc.player==null||mc.hitResult==null||mc.hitResult.getType()!=HitResult.Type.BLOCK
          ||!WeaponTags.isWeapon(mc.player.getItemInHand(event.getHand())))return;
  event.setSwingHand(false);
  event.setCanceled(true);
  if(mc.gameMode!=null&&mc.gameMode.isDestroying())mc.gameMode.stopDestroyBlock();
  if(event.getHand()==InteractionHand.MAIN_HAND&&mc.player.getMainHandItem().getItem() instanceof SwordItem
          &&canUseCombo(mc)) lightAttack(mc);
 }
 @SubscribeEvent public static void useItem(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
  if(!event.getLevel().isClientSide || !event.getEntity().isLocalPlayer() || ClientWeaponState.canUse(event.getHand()))return;
  event.setCancellationResult(net.minecraft.world.InteractionResult.PASS);
  event.setCanceled(true);
 }
 @SubscribeEvent public static void useBlock(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
  if(event.getLevel().isClientSide && event.getEntity().isLocalPlayer() && !ClientWeaponState.canUse(event.getHand()))
   event.setUseItem(net.minecraftforge.eventbus.api.Event.Result.DENY);
 }
 @SubscribeEvent public static void interaction(InputEvent.InteractionKeyMappingTriggered e){
  var mc=Minecraft.getInstance();
  if(!canUseCombo(mc)||!ClientWeaponState.canUse(e.getHand()))return;
  if(!(mc.player.getMainHandItem().getItem() instanceof SwordItem))return;
  if(e.getKeyMapping()==mc.options.keyUse){
   if(!mc.player.getOffhandItem().isEmpty()||mc.player.isUsingItem()){heldUse=true;return;}
   if(mc.hitResult!=null&&mc.hitResult.getType()==HitResult.Type.BLOCK){heldUse=true;return;}
   if(mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit&&!(hit.getEntity() instanceof net.minecraft.world.entity.monster.Enemy)){heldUse=true;return;}
   e.setSwingHand(false);e.setCanceled(true);
   if(e.getHand()==InteractionHand.MAIN_HAND&&!heldUse){heldUse=true;ClientAttackMotion.pending();ActionNetwork.requestAttack(true);}
   return;
  }
  // Mining and contextual USE (including offhand/shields) retain vanilla semantics.
  if(e.getKeyMapping()!=mc.options.keyAttack||e.getHand()!=InteractionHand.MAIN_HAND)return;
  if(mc.hitResult!=null&&mc.hitResult.getType()==HitResult.Type.BLOCK){heldAttack=true;return;}
  e.setSwingHand(false);e.setCanceled(true);
  lightAttack(mc);
 }
 private static boolean canUseCombo(Minecraft mc){
  return mc.player!=null&&mc.level!=null&&mc.player.isAlive()&&!mc.player.isSpectator()
      &&mc.screen==null&&mc.getOverlay()==null&&ClientCombatState.available();
 }
 private static void lightAttack(Minecraft mc){
  if(heldAttack)return;
  heldAttack=true;
  if(mc.player.onGround())ClientAttackMotion.pending();
  ActionNetwork.requestAttack(false);
 }
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
  if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
  if(mc.screen!=null||mc.getOverlay()!=null){heldAttack=true;heldUse=true;}
  else{if(!mc.options.keyAttack.isDown())heldAttack=false;if(!mc.options.keyUse.isDown())heldUse=false;}
 }
}
