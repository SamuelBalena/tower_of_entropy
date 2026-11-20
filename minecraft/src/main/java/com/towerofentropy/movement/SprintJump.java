package com.towerofentropy.movement;

import com.towerofentropy.combat.CombatController;
import com.towerofentropy.combat.RollController;
import java.util.Collections;
import java.util.Map;
import java.util.IdentityHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Evaluates the same vanilla ground-jump event on the server and on the predicting client. */
@Mod.EventBusSubscriber(modid = "towerofentropy")
public final class SprintJump {
    private static final ForgeConfigSpec.DoubleValue BONUS;
    public static final ForgeConfigSpec SPEC;
    private static final float VANILLA_KICK = .2f;
    // The integrated server and client have separate player objects with the same entity ID.
    private static final Map<Player, Travel> TRAVEL = Collections.synchronizedMap(new IdentityHashMap<>());

    static {
        var builder = new ForgeConfigSpec.Builder();
        BONUS = builder.comment("Fraction of the vanilla sprint takeoff impulse added once along ground travel.")
                .defineInRange("sprintJumpBonus", .15, 0, .30);
        SPEC = builder.build();
    }

    private record Travel(Vec3 position, Vec3 direction, long tick, long movedAt, boolean grounded) {}

    private SprintJump() {}

    @SubscribeEvent
    public static void recordTravel(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var player = event.player;
        var prior = TRAVEL.get(player);
        Vec3 at = player.position();
        long now = player.level().getGameTime();
        Vec3 direction = Vec3.ZERO;
        long movedAt = now;
        if (prior != null && prior.grounded && player.onGround() && now-prior.tick <= 2 && now >= prior.tick) {
            double dx = at.x-prior.position.x, dz = at.z-prior.position.z;
            double distance = Math.hypot(dx, dz);
            // Ignore teleports, apparent motion below the noise threshold and non-finite samples.
            if (distance >= .025 && distance <= .9) direction = new Vec3(dx/distance, 0, dz/distance);
            else if (distance < .025 && now-prior.movedAt <= 1) {
                // One missing movement packet can leave a duplicate accepted position.
                direction = prior.direction;
                movedAt = prior.movedAt;
            }
        }
        TRAVEL.put(player, new Travel(at, direction, now, movedAt, player.onGround()));
    }

    @SubscribeEvent
    public static void leaveLevel(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Player player) TRAVEL.remove(player);
    }

    static Vec3 takeoff(Player player, Vec3 vanilla) {
        if (!SPEC.isLoaded() || !player.onGround() || !player.isSprinting() || player.isUsingItem()
                || player.hurtTime > 0 || player.isShiftKeyDown()) return vanilla;
        if (player instanceof ServerPlayer server) {
            var combat = CombatController.status(server.getUUID());
            if (RollController.busy(server.getUUID()) || (combat != null && combat.attacking())
                    || WallSlide.engaged(server)) return vanilla;
        }
        var travel = TRAVEL.get(player);
        long now = player.level().getGameTime();
        if (travel == null || !travel.grounded || now < travel.tick || now-travel.tick > 2
                || travel.direction == Vec3.ZERO) return vanilla;
        // Vanilla already added its yaw-directed kick before firing LivingJumpEvent.
        // Retain momentum already present, then replace only that kick with a travel-directed one.
        float yaw = player.getYRot() * ((float)Math.PI / 180);
        double kick = VANILLA_KICK * (1 + BONUS.get());
        return vanilla.add(Mth.sin(yaw) * VANILLA_KICK + travel.direction.x * kick,
                0, -Mth.cos(yaw) * VANILLA_KICK + travel.direction.z * kick);
    }
}
