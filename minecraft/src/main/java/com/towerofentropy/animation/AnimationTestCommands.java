package com.towerofentropy.animation;

import java.nio.file.Path;
import com.mojang.brigadier.Command;
import com.tom.cpm.shared.io.ModelFile;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in, operator-only diagnostic. No player movement or combat changes. */
@Mod.EventBusSubscriber(modid = "towerofentropy")
public final class AnimationTestCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("toe_animtest")
            .requires(s -> s.hasPermission(2))
            .then(Commands.literal("idle").executes(c -> {
                try {
                    var player = c.getSource().getPlayerOrException();
                    if (ToeCpmPlugin.common == null) throw new IllegalStateException("CPM API not initialized");
                    var file = Path.of("player_models", "toe-steve-ual1-idle.cpmmodel").toFile();
                    var model = ModelFile.load(file);
                    LocomotionController.stop(player);
                    ToeCpmPlugin.common.setPlayerModel(Player.class, player, model, false);
                    c.getSource().sendSuccess(() -> Component.literal("UAL1 Idle_Loop: 2.5s loop while standing. F5 for third person. /toe_animtest off to restore."), false);
                    return Command.SINGLE_SUCCESS;
                } catch (Exception ex) {
                    c.getSource().sendFailure(Component.literal("CPM idle test failed: " + ex.getMessage()));
                    return 0;
                }
            }))
            .then(Commands.literal("off").executes(c -> {
                if (ToeCpmPlugin.common == null) return 0;
                LocomotionController.stop(c.getSource().getPlayerOrException());
                ToeCpmPlugin.common.resetPlayerModel(Player.class, c.getSource().getPlayerOrException());
                c.getSource().sendSuccess(() -> Component.literal("CPM test disabled; previous local model / vanilla restored."), false);
                return Command.SINGLE_SUCCESS;
            }))
            .then(Commands.literal("idle_steve").executes(c -> {
                try {
                    var player = c.getSource().getPlayerOrException();
                    if (ToeCpmPlugin.common == null) throw new IllegalStateException("CPM API not initialized");
                    var model = ModelFile.load(Path.of("player_models", "toe-steve-ual1-idle-preview.cpmmodel").toFile());
                    LocomotionController.stop(player);
                    ToeCpmPlugin.common.setPlayerModel(Player.class, player, model, false);
                    c.getSource().sendSuccess(() -> Component.literal("Steve preview: UAL1 Idle_Loop. /toe_animtest off to restore."), false);
                    return Command.SINGLE_SUCCESS;
                } catch (Exception ex) {
                    c.getSource().sendFailure(Component.literal("CPM Steve preview failed: " + ex.getMessage()));
                    return 0;
                }
            }))
            .then(Commands.literal("auto").executes(c -> enableAuto(c.getSource(), false)))
            .then(Commands.literal("auto_steve").executes(c -> enableAuto(c.getSource(), true)))
            .then(Commands.literal("roll").executes(c -> action(c.getSource(),VisualActionWindow.Action.ROLL)))
            .then(Commands.literal("sword").executes(c -> action(c.getSource(),VisualActionWindow.Action.SWORD_ATTACK))));
    }
    private static int action(net.minecraft.commands.CommandSourceStack source,VisualActionWindow.Action action){
        try{String result=LocomotionController.requestAction(source.getPlayerOrException(),action);
            source.sendSuccess(()->Component.literal("Visual "+action+": "+result),false);
            return result.equals("accepted")?Command.SINGLE_SUCCESS:0;
        }catch(Exception ex){source.sendFailure(Component.literal("Visual action failed: "+ex.getMessage()));return 0;}
    }
    private static int enableAuto(net.minecraft.commands.CommandSourceStack source, boolean preview) {
        try {
            LocomotionController.enable(source.getPlayerOrException(), preview);
            source.sendSuccess(() -> Component.literal("Automatic Idle/Jog enabled. /toe_animtest off restores vanilla."), false);
            return Command.SINGLE_SUCCESS;
        } catch (Exception ex) {
            source.sendFailure(Component.literal("CPM automatic test failed: " + ex.getMessage()));
            return 0;
        }
    }
}
