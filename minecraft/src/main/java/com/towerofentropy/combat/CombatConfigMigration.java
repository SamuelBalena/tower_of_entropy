package com.towerofentropy.combat;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;
/** Upgrade only the exact previous default tuple, once per config. */
@Mod.EventBusSubscriber(modid="towerofentropy",bus=Mod.EventBusSubscriber.Bus.MOD)
public final class CombatConfigMigration {
 @SubscribeEvent public static void loading(ModConfigEvent.Loading e){migrate(e);}
 @SubscribeEvent public static void reloading(ModConfigEvent.Reloading e){migrate(e);}
 private static void migrate(ModConfigEvent e){
  if(e.getConfig().getSpec()!=CombatConfig.SPEC||CombatConfig.MOVEMENT_DEFAULTS_REVISION.get()!=0)return;
  if(legacy(CombatConfig.ADVANCE_A.get(),CombatConfig.ADVANCE_B.get(),CombatConfig.ADVANCE_C.get(),CombatConfig.ADVANCE_HEAVY.get())){
   CombatConfig.ADVANCE_A.set(CombatConfig.DEFAULT_ADVANCE_A);CombatConfig.ADVANCE_B.set(CombatConfig.DEFAULT_ADVANCE_B);CombatConfig.ADVANCE_C.set(CombatConfig.DEFAULT_ADVANCE_C);CombatConfig.ADVANCE_HEAVY.set(CombatConfig.DEFAULT_ADVANCE_HEAVY);
  }
  CombatConfig.MOVEMENT_DEFAULTS_REVISION.set(1);e.getConfig().save();
 }
 public static boolean legacy(double a,double b,double c,double h){return Math.abs(a-.18)<1e-9&&Math.abs(b-.22)<1e-9&&Math.abs(c-.28)<1e-9&&Math.abs(h-.32)<1e-9;}
}
