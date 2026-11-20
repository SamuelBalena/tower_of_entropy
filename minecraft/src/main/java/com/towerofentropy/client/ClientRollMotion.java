package com.towerofentropy.client;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import com.towerofentropy.combat.RollRules;
/** Prediction from an approved server direction; never accepts a client distance or speed. */
@Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT)
public final class ClientRollMotion {
 private static boolean active,moveNow,completionSent;private static long start=-1,finishedUntil;private static double x,z;private static int age,moved,pendingAge,lastMoveTick=-1;
 public static void accept(boolean enabled,long serverStart,double dx,double dz){boolean wasActive=active;CombatSprint.answered();ClientAttackMotion.rollAnswered();if(enabled&&!R3VisualPose.LEGACY)ClientCombatState.canceledIntoRoll();if(!enabled||start!=serverStart){age=0;moved=0;pendingAge=0;moveNow=false;completionSent=false;lastMoveTick=-1;}active=enabled;start=serverStart;x=dx;z=dz;var p=Minecraft.getInstance().player;if(p!=null&&((!enabled&&wasActive)||(enabled&&serverStart<0))){var v=p.getDeltaMovement();p.setDeltaMovement(0,v.y,0);}}
 public static boolean busy(){return active;}
 public static boolean recentlyFinished(){var l=Minecraft.getInstance().level;return l!=null&&l.getGameTime()<finishedUntil;}
 public static boolean pending(){return active&&start<0;}
 public static double elapsed(float partial){return start<0?-1:Math.max(0,age+partial);}
 public static boolean visual(){return active&&!completionSent&&start>=0&&elapsed(0)<30;}
 public static float visualTime(float partial){double t=Math.min(.7,elapsed(partial)/20);return (float)(t<=.5?t*2:1+(t-.5)*((44/30d-1)/.2));}
 public static float yaw(){return (float)Math.toDegrees(Math.atan2(-x,z));}
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase==TickEvent.Phase.END&&active&&start>=0&&moved>=RollRules.MOVE_TICKS&&!completionSent){com.towerofentropy.animation.ActionNetwork.finishRoll(start);var p=Minecraft.getInstance().player;if(p!=null){finishedUntil=p.level().getGameTime()+10;var v=p.getDeltaMovement();p.setDeltaMovement(0,v.y,0);}completionSent=true;moveNow=false;return;}if(e.phase!=TickEvent.Phase.START||!active)return;var p=Minecraft.getInstance().player;if(p==null){active=false;return;}if(start>=0)age++;else pendingAge++;if(age>36||pendingAge>180){accept(false,start,x,z);return;}}
 private static void apply(net.minecraft.client.player.LocalPlayer p){var v=p.getDeltaMovement();double speed=moveNow?RollRules.SPEED:0;p.setDeltaMovement(x*speed,v.y,z*speed);p.setSprinting(false);}
 @SubscribeEvent public static void input(MovementInputUpdateEvent e){if(!active)return;var p=Minecraft.getInstance().player;if(p==null)return;if(lastMoveTick!=p.tickCount){lastMoveTick=p.tickCount;moveNow=start>=0&&moved<RollRules.MOVE_TICKS;if(moveNow)moved++;}var input=e.getInput();input.forwardImpulse=0;input.leftImpulse=0;input.up=false;input.down=false;input.left=false;input.right=false;input.jumping=false;apply(Minecraft.getInstance().player);}
 @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){active=false;start=-1;finishedUntil=0;lastMoveTick=-1;moved=0;age=0;pendingAge=0;completionSent=false;}
}
