package com.towerofentropy.animation;
import net.minecraftforge.common.ForgeConfigSpec;
/** Client presentation settings and the action binding migration marker. */
public final class VisualTuning {
 public static final ForgeConfigSpec SPEC;
 public static final ForgeConfigSpec.DoubleValue HEAVY,RETURN,CHAIN,ROLL,GROUND_RESPONSE,GROUND_LIMIT,LOCOMOTION;
 public static final ForgeConfigSpec.BooleanValue SUPPORT;
 static final ForgeConfigSpec.BooleanValue WEAPON_BINDING_MIGRATED;
 static{var b=new ForgeConfigSpec.Builder();b.push("transitions");
  HEAVY=b.defineInRange("heavyEntrySeconds",.12,0,.20);
  RETURN=b.defineInRange("returnToLocomotionSeconds",.14,0,.25);
  CHAIN=b.defineInRange("lightChainSeconds",0.0,0,.12);
  ROLL=b.defineInRange("rollVisualSeconds",.06,0,.10);
  LOCOMOTION=b.defineInRange("locomotionSeconds",.14,.06,.25);b.pop();b.push("sourceContact");
  SUPPORT=b.define("enabled",true);
  GROUND_RESPONSE=b.defineInRange("settleSeconds",.06,.02,.15);
  GROUND_LIMIT=b.defineInRange("maxVisualLoweringBlocks",.27,0,.30);b.pop();WEAPON_BINDING_MIGRATED=b.define("weaponBindingMigrated",false);SPEC=b.build();
 }
}
