package com.towerofentropy.client;
import com.towerofentropy.animation.HorizontalMotion;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Observes real vanilla state. Never changes movement, health, item use or interaction results. */
@Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT)
public final class VisualContext {
 public static final boolean PREVIOUS=Boolean.getBoolean("toe.a3.previous");
 static InteractionHand bowHand=InteractionHand.MAIN_HAND;
 private static boolean wasGround=true,wasBow,wallFlight,groundJump;private static float health=20,lastHurt;private static double lastVelocity;private static int age,lastHit=-1000,takeoff=-1000,landing=-1000,release=-1000,bowTicks;private static String hit="hit_chest";private static java.util.UUID owner;
 public static void reset(){wallFlight=false;groundJump=false;owner=null;age=0;wasGround=true;wasBow=false;bowHand=InteractionHand.MAIN_HAND;lastHit=takeoff=landing=release=-1000;health=20;lastHurt=0;lastVelocity=0;}
 static void wallJumpStarted(){wallFlight=true;takeoff=age;}
 static void wallContactResumed(){wallFlight=false;groundJump=false;}
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var p=Minecraft.getInstance().player;if(p==null){reset();return;}if(!p.getUUID().equals(owner)){reset();owner=p.getUUID();health=p.getHealth();wasGround=p.onGround();}age++;
  if(wasGround&&!p.onGround()){wallFlight=false;groundJump=p.getDeltaMovement().y>0;takeoff=age;}
  if(!wasGround&&p.onGround())landing=age;
  if(p.onGround()&&age-landing>=4){wallFlight=false;groundJump=false;}
  wasGround=p.onGround();
  boolean bow=p.isUsingItem()&&p.getUseItem().getItem() instanceof BowItem;
  if(bow){bowTicks=p.getTicksUsingItem();bowHand=p.getUsedItemHand();}if(wasBow&&!bow&&bowTicks>=3&&p.getItemInHand(bowHand).getItem() instanceof BowItem&&!p.isDeadOrDying())release=age;wasBow=bow;
  double velocity=p.getDeltaMovement().horizontalDistance();
  if(p.hurtTime>lastHurt&&p.getHealth()<health&&age-lastHit>=7&&!ClientRollMotion.busy()){lastHit=age;hit="hit_chest";}
  if(age-lastHit<2&&velocity-lastVelocity>.07)hit="hit_knockback";
  health=p.getHealth();lastHurt=p.hurtTime;lastVelocity=velocity;
 }
 public static String locomotion(){return switch(HorizontalLocomotion.MOTION.state()){case MOVE_BACKWARD->"backward";case STRAFE_LEFT->"strafe_left";case STRAFE_RIGHT->"strafe_right";default->HorizontalLocomotion.MOTION.clip();};}
 public static String selected(){var mc=Minecraft.getInstance();var p=mc.player;if(p==null||PREVIOUS)return null;
  if(p.isDeadOrDying())return "death";
  if(p.isPassenger()||p.isFallFlying()||p.onClimbable()||p.getAbilities().flying||p.isInLava()||p.getPose()==Pose.SWIMMING&&!p.isInWater())return null;
  if(p.isInWater()){return p.getDeltaMovement().lengthSqr()>.003?"swim":"swim_idle";}
  if(age-lastHit<4)return hit;
  String parkour=ClientWallVisual.clip();if(parkour!=null)return parkour;
  if(wallFlight){
   if(!p.onGround())return airClip(true);
   if(age-landing<4)return "jump_land";
  }
  if(p.isUsingItem()){var animation=p.getUseItem().getUseAnimation();if(p.getUseItem().getItem() instanceof BowItem)return p.getTicksUsingItem()<20?"bow_load":"bow_hold";if(animation==UseAnim.EAT||animation==UseAnim.DRINK)return "consume";if(animation!=UseAnim.NONE)return null;}
  if(age-release<5&&p.getItemInHand(bowHand).getItem() instanceof BowItem)return "bow_release";
  if((ClientSwingPose.digging(p)||p.swinging&&p.swingingArm==InteractionHand.MAIN_HAND)&&mc.options.keyAttack.isDown()&&p.getMainHandItem().getItem() instanceof AxeItem&&mc.hitResult instanceof BlockHitResult b&&p.level().getBlockState(b.getBlockPos()).is(BlockTags.LOGS))return "chop";
  if(ClientSwingPose.swordBlock(p))return com.towerofentropy.combat.CombatMove.LIGHT_ATTACK_A.clip;
  String punch=ClientSwingPose.clip(p);if(punch!=null)return punch;
  if(!p.onGround())return airClip(p.getDeltaMovement().y>0);
  if(p.isCrouching())return HorizontalLocomotion.MOTION.speed()>.012?"crouch_move":"crouch_idle";
  if(age-landing<4&&(groundJump||HorizontalLocomotion.MOTION.speed()<.08))return "jump_land";
  if(HorizontalLocomotion.gait().equals("sprint"))return "sprint";
  if(p.getMainHandItem().getItem() instanceof BowItem)return HorizontalLocomotion.MOTION.speed()>.025?locomotion():"bow_idle";
  if(p.getMainHandItem().is(net.minecraft.world.item.Items.TORCH)&&HorizontalLocomotion.MOTION.state()==HorizontalMotion.State.IDLE)return "torch";
  return HorizontalLocomotion.gait();
 }
 static String airbornePose(){var p=Minecraft.getInstance().player;return airClip(wallFlight||p.getDeltaMovement().y>0);}
 private static String airClip(boolean takingOff){return takingOff&&age-takeoff<3?"jump_start":"jump_loop";}
 public static boolean own(String c){return c!=null&&(ClientWallVisual.owns(c)||c.startsWith("punch_")||c.startsWith("bow_")||c.startsWith("jump_")||c.startsWith("crouch_")||c.startsWith("hit_")||c.equals("death")||c.equals("consume")||c.equals("chop")||c.equals("torch")||c.startsWith("swim")||c.equals("backward")||c.startsWith("strafe_"));}
 public static boolean loop(String c){return c.equals("wall_slide")||c.equals("wall_slide_push")||c.equals("ledge_hang")|| c.equals("crouch_idle")||c.equals("crouch_move")||c.equals("jump_loop")||c.equals("swim")||c.equals("swim_idle")||c.equals("bow_hold")||c.equals("bow_idle")||c.equals("torch")||c.equals("chop")||c.equals("backward")||c.startsWith("strafe_");}
 public static boolean upper(String c){return c.startsWith("punch_")||c.startsWith("bow_")||c.startsWith("hit_")||c.equals("consume")||c.equals("chop")||c.equals("torch");}
 public static float time(String c,float duration,float partial){var p=Minecraft.getInstance().player;float t=(age+partial)/20f;
  if(ClientWallVisual.owns(c))return ClientWallVisual.time(c,duration,partial);
  if(c.startsWith("punch_"))return ClientSwingPose.time(p,duration,partial);
  if(c.equals("death"))return Math.min(duration,((p.deathTime+partial)/20f)*duration);
  if(c.equals("jump_start"))return Math.max(0,duration-.25f)+Math.min(.25f,(age-takeoff+partial)/3f*.25f);
  if(c.equals("jump_land"))return Math.min(duration,(age-landing+partial)/4f*duration);
  if(c.startsWith("hit_"))return Math.min(duration,(age-lastHit+partial)/4f*duration);
  if(c.equals("bow_load"))return Math.min(duration,(p.getTicksUsingItem()+partial)/20f*duration);
  if(c.equals("bow_hold"))return ((p.getTicksUsingItem()-20+partial)/20f)%duration;
  if(c.equals("bow_release"))return Math.min(duration,(age-release+partial)/5f*duration);
  if(c.equals("consume"))return Math.min(duration,(p.getTicksUsingItem()+partial)/p.getUseItem().getUseDuration()*duration);
  if(c.equals("jump_loop"))return ((age-takeoff+partial)/20f)%duration;
  return t%duration;
 }
 static boolean vanillaSwing(String selected) {
  var p=Minecraft.getInstance().player;
  return selected!=null && !ClientWallVisual.owns(selected) && !(wallFlight && selected.startsWith("jump_"))
    && !selected.startsWith("hit_") && !selected.startsWith("punch_")
    && !selected.equals("death") && !selected.equals("consume") && !selected.equals("chop")
    && (!selected.startsWith("bow_") || selected.equals("bow_idle")) && ClientSwingPose.needsVanilla(p);
 }
 public static boolean unsupported(){
  var p=Minecraft.getInstance().player;
  if(p==null)return true;
  if(p.getPose()==Pose.SLEEPING||p.isPassenger()||p.isFallFlying()||p.onClimbable()||p.getAbilities().flying
    ||p.isInLava()||p.getPose()==Pose.SWIMMING&&!p.isInWater())return true;
  if(p.isUsingItem()){
   var used=p.getUseItem();
   if(used.getItem() instanceof BowItem)
    return !p.onGround()||p.isInWater()||p.isCrouching()||Math.abs(p.getXRot())>65||HorizontalLocomotion.MOTION.speed()>.025;
   var animation=used.getUseAnimation();
   if(animation!=UseAnim.EAT&&animation!=UseAnim.DRINK&&animation!=UseAnim.NONE)return true;
  }
  return p.isCrouching()&&HorizontalLocomotion.MOTION.directionalFallback()&&ClientSwingPose.clip(p)==null;
 }
}
