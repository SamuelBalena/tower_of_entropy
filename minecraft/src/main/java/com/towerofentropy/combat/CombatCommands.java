package com.towerofentropy.combat;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="towerofentropy")
public final class CombatCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("toe_combat").requires(s->s.hasPermission(2))
            .then(Commands.literal("diagnostics")
                .then(Commands.literal("on").executes(c -> {
                    CombatDiagnostics.start(c.getSource().getPlayerOrException());
                    c.getSource().sendSuccess(() -> Component.literal("TOE diagnostics on. See latest.log."), false);
                    return 1;
                }))
                .then(Commands.literal("off").executes(c -> {
                    CombatDiagnostics.disable();
                    c.getSource().sendSuccess(() -> Component.literal("TOE diagnostics off."), false);
                    return 1;
                })))
            .then(Commands.literal("auto").executes(c->{try{CombatController.automatic(c.getSource().getPlayerOrException());c.getSource().sendSuccess(()->Component.literal("TOE combat AUTO: sword activates automatically."),false);return 1;}catch(Exception ex){c.getSource().sendFailure(Component.literal(ex.getMessage()));return 0;}}))
            .then(Commands.literal("on").executes(c->{
                try{CombatController.enable(c.getSource().getPlayerOrException());
                    c.getSource().sendSuccess(()->Component.literal("TOE combat ON: ATTACK light A/B/C; USE heavy (sword); V Roll. /toe_combat off"),false);return 1;
                }catch(Exception ex){c.getSource().sendFailure(Component.literal("Combat unavailable: "+ex.getMessage()));return 0;}
            }))
            .then(Commands.literal("off").executes(c->{CombatController.disable(c.getSource().getPlayerOrException());
                c.getSource().sendSuccess(()->Component.literal("TOE combat OFF; J restored to visual test."),false);return 1;}))
            .then(Commands.literal("status").executes(c->{var s=CombatController.status(c.getSource().getPlayerOrException().getUUID());
                var roll=RollController.status(c.getSource().getPlayerOrException().getUUID());
                var action=CombatController.actionStatus(c.getSource().getPlayerOrException());
                var mode=CombatController.mode(c.getSource().getPlayerOrException().getUUID());
                boolean active=CombatController.enabled(c.getSource().getPlayerOrException());
                c.getSource().sendSuccess(()->Component.literal("TOE combat mode="+mode+" active="+active+" "+(s==null?"inactive":s.toString())+" Action="+action+" Roll="+roll),false);return 1;})));
    }
}
