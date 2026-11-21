package com.towerofentropy.combat;

import java.lang.reflect.Field;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

@Mod.EventBusSubscriber(modid = "towerofentropy")
public final class WeaponBlockProtection {
    private WeaponBlockProtection() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attackBlock(LeftClickBlock event) {
        if (event.getAction() == LeftClickBlock.Action.ABORT
                || !EffectiveEquipment.blocksMining(event.getEntity())) return;
        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player) {
            MiningState.abort(player);
            var pos = event.getPos();
            if (player.canReach(pos, 1.5) && player.serverLevel().hasChunkAt(pos))
                player.connection.send(new ClientboundBlockUpdatePacket(player.serverLevel(), pos));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breakSpeed(PlayerEvent.BreakSpeed event) {
        if (EffectiveEquipment.blocksMining(event.getEntity())) event.setNewSpeed(0);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breakBlock(BlockEvent.BreakEvent event) {
        if (EffectiveEquipment.blocksMining(event.getPlayer())) event.setCanceled(true);
    }

    static void abort(ServerPlayer player) {
        MiningState.abort(player);
    }

    /** Forge exposes no method to abort both active and delayed destruction. Clear both vanilla states. */
    private static final class MiningState {
        private static final Field ACTIVE = field("f_9249_");
        private static final Field ACTIVE_POS = field("f_9251_");
        private static final Field DELAYED = field("f_9253_");
        private static final Field DELAYED_POS = field("f_9254_");
        private static final Field LAST_STAGE = field("f_9256_");

        private static Field field(String name) {
            return ObfuscationReflectionHelper.findField(ServerPlayerGameMode.class, name);
        }

        private static void abort(ServerPlayer player) {
            var mode = player.gameMode;
            try {
                boolean active = ACTIVE.getBoolean(mode);
                boolean delayed = DELAYED.getBoolean(mode);
                if (!active && !delayed) return;
                if (active) clear(player, (BlockPos) ACTIVE_POS.get(mode));
                if (delayed) clear(player, (BlockPos) DELAYED_POS.get(mode));
                ACTIVE.setBoolean(mode, false);
                DELAYED.setBoolean(mode, false);
                LAST_STAGE.setInt(mode, -1);
            } catch (IllegalAccessException failure) {
                throw new IllegalStateException("Cannot clear vanilla mining state", failure);
            }
        }

        private static void clear(ServerPlayer player, BlockPos pos) {
            player.serverLevel().destroyBlockProgress(player.getId(), pos, -1);
        }
    }
}
