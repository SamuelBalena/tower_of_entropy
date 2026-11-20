package com.towerofentropy.combat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Only attributed combat damage; environmental damage never becomes globally invulnerable. */
@Mod.EventBusSubscriber(modid="towerofentropy")
public final class RollDamageFilter {
 @SubscribeEvent public static void attack(LivingAttackEvent event){
  if(!(event.getEntity() instanceof ServerPlayer p)||!RollController.iframe(p))return;
  var source=event.getSource();var attacker=source.getEntity();
  if(source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))return;
  boolean projectile=source.is(DamageTypeTags.IS_PROJECTILE);
  if(projectile?attacker instanceof Enemy:attacker instanceof LivingEntity&&source.getDirectEntity()==attacker)event.setCanceled(true);
 }
}
