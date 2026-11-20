package com.towerofentropy.animation;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import com.tom.cpm.shared.io.ModelFile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import static com.towerofentropy.animation.LocomotionStateMachine.State.*;

/** Opt-in server observer. Never writes entity position, motion, attributes or collision. */
@Mod.EventBusSubscriber(modid = "towerofentropy")
public final class LocomotionController {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
    public record Status(LocomotionStateMachine.State state, boolean attached, int poseCommands,
                         int modelAssignments, double speed, double forward, int actionCommands) { }
    private static final class Session {
        final LocomotionStateMachine machine = new LocomotionStateMachine();
        final ModelFile model;
        final boolean actionsSupported;
        final VisualActionWindow actions = new VisualActionWindow();
        int actionCommands,pendingSince;
        double x, z;
        int ticks, readyAfter, poseCommands, assignments;
        boolean attached;
        LocomotionStateMachine.State applied;
        volatile Status status = new Status(VANILLA_FALLBACK, false, 0, 0, 0, 0, 0);
        Session(ServerPlayer player, ModelFile model, boolean actionsSupported) {
            this.model = model;this.actionsSupported=actionsSupported;x = player.getX(); z = player.getZ();
        }
    }
    public static Status status(UUID id) { Session s = SESSIONS.get(id); return s == null ? null : s.status; }

    public static boolean supported(ServerPlayer player) {
        return supportedAction(player)&&(!player.isSprinting()||com.towerofentropy.combat.RollController.busy(player.getUUID()));
    }
    public static boolean supportedAction(ServerPlayer player) {
        return (player.onGround()||com.towerofentropy.combat.RollController.started(player.getUUID())) && supportedSwordPosture(player);
    }
    public static boolean supportedSwordPosture(ServerPlayer player) {
        return player.isAlive()&&!player.isSpectator()&&!player.isCrouching()
            &&!player.isInWaterOrBubble()&&!player.isInLava()&&!player.onClimbable()&&!player.isPassenger()
            &&!player.isFallFlying()&&!player.getAbilities().flying
            &&player.getPose()==Pose.STANDING&&!player.isUsingItem()&&!player.swinging&&player.hurtTime==0;
    }
    public static void cancelAction(ServerPlayer player) {
        cancelAction(player,true);
    }
    public static void finishRoll(ServerPlayer p){var s=SESSIONS.get(p.getUUID());if(s!=null&&(s.actions.active()==VisualActionWindow.Action.ROLL||s.actions.pending()==VisualActionWindow.Action.ROLL)){s.actions.cancel();if(ToeCpmPlugin.common!=null)ToeCpmPlugin.common.playAnimation(Player.class,p,"toe_roll",0);}}
    public static void cancelAction(ServerPlayer player,boolean notifyCombat) {
        com.towerofentropy.combat.RollController.cancel(player,"action cancelled");
        Session s=SESSIONS.get(player.getUUID());
        if(s!=null&&ToeCpmPlugin.common!=null){var old=s.actions.cancel();
            if(old!=null)ToeCpmPlugin.common.playAnimation(Player.class,player,old.animation,0);}
        com.towerofentropy.combat.CombatController.cancel(player,notifyCombat);
    }

    public static void enable(ServerPlayer player, boolean stevePreview) throws Exception {
        if (ToeCpmPlugin.common == null) throw new IllegalStateException("CPM API not initialized");
        var name = stevePreview ? "toe-steve-ual1-locomotion-preview.cpmmodel" : "toe-steve-ual1-locomotion.cpmmodel";
        var actionName=stevePreview?"toe-steve-ual-actions-preview.cpmmodel":"toe-steve-ual-actions.cpmmodel";
        Path playerModels=net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("player_models");
        boolean actionsSupported=java.nio.file.Files.isRegularFile(playerModels.resolve(actionName));
        if(actionsSupported)name=actionName;
        Path modelPath=playerModels.resolve(name);
        if(!actionsSupported||!java.nio.file.Files.isRegularFile(modelPath)){modelPath=CompatibilityModels.actions(stevePreview);actionsSupported=true;}
        ModelFile model = ModelFile.load(modelPath.toFile());
        stop(player);
        SESSIONS.put(player.getUUID(), new Session(player, model, actionsSupported));
        LOG.info("TOE_M001_2 AUTO_ENABLED player={} preview={}", player.getScoreboardName(), stevePreview);
    }
    public static String requestAction(ServerPlayer player,VisualActionWindow.Action action) throws Exception {
        Session s=SESSIONS.get(player.getUUID());
        if(s==null){enable(player,false);s=SESSIONS.get(player.getUUID());}
        if(!s.actionsSupported)return "M001.3 models not prepared";
        if(!supported(player))
            return "unsupported state";
        boolean accepted=s.actions.request(action);
        if(accepted)s.pendingSince=s.ticks;
        LOG.info("TOE_M001_3 ACTION_REQUEST {} accepted={}",action,accepted);
        return accepted?"accepted":"busy; wait for current action";
    }
    /** Remove observation before any explicit model command, including M001.1 commands. */
    public static void stop(ServerPlayer player) {
        com.towerofentropy.combat.RollController.cancel(player,"animation stopped");
        com.towerofentropy.combat.CombatController.cancel(player);
        Session s = SESSIONS.remove(player.getUUID());
        if(s!=null&&ToeCpmPlugin.common!=null){var cancelled=s.actions.cancel();if(cancelled!=null)ToeCpmPlugin.common.playAnimation(Player.class,player,cancelled.animation,0);}
        if (s != null && s.attached && ToeCpmPlugin.common != null)
            ToeCpmPlugin.common.resetPlayerModel(Player.class, player);
        if (s != null) LOG.info("TOE_M001_2 AUTO_DISABLED player={} poseCommands={} modelAssignments={}",
            player.getScoreboardName(), s.poseCommands, s.assignments);
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        Session s = SESSIONS.get(player.getUUID());
        if (s == null) return;
        double dx = player.getX() - s.x, dz = player.getZ() - s.z;
        s.x = player.getX(); s.z = player.getZ(); ++s.ticks;
        double speed = Math.hypot(dx, dz), yaw = Math.toRadians(player.getYRot());
        double forward = speed > 1e-8 ? (-Math.sin(yaw) * dx + Math.cos(yaw) * dz) / speed : 1;
        if(com.towerofentropy.combat.RollController.busy(player.getUUID()))forward=1; // Roll direction is authoritative, not a forward-Jog classification.
        boolean unsupported = !supported(player)
            || speed > (com.towerofentropy.combat.RollController.busy(player.getUUID())?.8:.5); // Roll uses its stricter cumulative envelope as well.
        var previous = s.machine.state();
        var state = s.machine.update(new LocomotionStateMachine.Motion(speed, forward, unsupported));
        // A strafe/backstep may have left the observer in fallback before V arrived.
        // Do not cancel the newly accepted Roll during the selector's three-tick hysteresis.
        var combat=com.towerofentropy.combat.CombatController.status(player.getUUID());
        if((com.towerofentropy.combat.RollController.busy(player.getUUID())||(combat!=null&&combat.attacking()))&&!unsupported&&state==VANILLA_FALLBACK)
            state=speed>LocomotionStateMachine.STOP_SPEED?JOG:IDLE;
        if (state != previous) LOG.info("TOE_M001_2 STATE {} -> {} speed={} forward={} unsupported={}", previous, state, speed, forward, unsupported);
        try {
            if (state == VANILLA_FALLBACK) {
                if(combat!=null&&combat.attacking())LOG.info("TOE_A1 ATTACK_FALLBACK unsupported={} speed={} ground={} swing={} hurt={}",unsupported,speed,player.onGround(),player.swinging,player.hurtTime);
                com.towerofentropy.combat.RollController.cancel(player,"locomotion fallback");
                boolean independentSword = combat != null && combat.attacking()
                    && com.towerofentropy.combat.CombatController.valid(player)
                    && (!player.onGround() || !com.towerofentropy.combat.AttackMovement.committed(player.getUUID()));
                if (!independentSword) com.towerofentropy.combat.CombatController.cancel(player);
                var cancelled=s.actions.cancel();
                if(cancelled!=null){ToeCpmPlugin.common.playAnimation(Player.class,player,cancelled.animation,0);LOG.info("TOE_M001_3 ACTION_CANCEL {} fallback",cancelled);}
                if (s.attached) ToeCpmPlugin.common.resetPlayerModel(Player.class, player);
                s.attached = false; s.applied = null;
            } else {
                if (!s.attached) {
                    ToeCpmPlugin.common.setPlayerModel(Player.class, player, s.model, false);
                    s.attached = true; s.applied = null; ++s.assignments;
                    s.readyAfter = s.ticks + 12; // Wait for client model load and command registry acknowledgement.
                }
                String pose = state == IDLE ? "toe_idle" : "toe_jog";
                if (s.applied != state && s.ticks >= s.readyAfter
                    && ToeCpmPlugin.common.getAnimationPlaying(Player.class, player, pose) >= 0) {
                    // Custom poses share one exclusive slot. No simultaneous conflicting animations.
                    ToeCpmPlugin.common.playAnimation(Player.class, player, pose, 1);
                    s.applied = state; ++s.poseCommands;
                    LOG.info("TOE_M001_2 POSE_APPLIED {} count={}", pose, s.poseCommands);
                } else if (s.applied == null && s.ticks > s.readyAfter + 100) {
                    throw new IllegalStateException("CPM locomotion pose registry did not become ready");
                }
                var ended=s.actions.finishIfDue(s.ticks);
                if(ended!=null){ToeCpmPlugin.common.playAnimation(Player.class,player,ended.animation,0);LOG.info("TOE_M001_3 ACTION_END {}",ended);}
                var pending=s.actions.pending();
                if(pending!=null&&s.ticks>=s.readyAfter&&s.applied!=null
                    &&ToeCpmPlugin.common.getAnimationPlaying(Player.class,player,pending.animation)>=0){
                    var started=s.actions.start(s.ticks);
                    ToeCpmPlugin.common.playAnimation(Player.class,player,started.animation,1);
                    if(started==VisualActionWindow.Action.ROLL)com.towerofentropy.combat.RollController.animationStarted(player);
                    com.towerofentropy.combat.CombatController.animationStarted(player,started);
                    ++s.actionCommands;
                    LOG.info("TOE_M001_3 ACTION_START {} count={} durationMs={}",started,s.actionCommands,started.durationMs);
                }else if(pending!=null&&s.ticks-s.pendingSince>150){
                    com.towerofentropy.combat.RollController.cancel(player,"animation registry timeout");
                    com.towerofentropy.combat.CombatController.cancel(player);
                    s.actions.cancel();LOG.error("TOE_M001_3 ACTION_REQUEST_EXPIRED missing CPM action registry");
                }
            }
            var effective=s.actions.active()==null?state:LocomotionStateMachine.State.valueOf(s.actions.active().name());
            s.status = new Status(effective, s.attached, s.poseCommands, s.assignments, speed, forward,s.actionCommands);
        } catch (Exception ex) {
            LOG.error("TOE_M001_2 CONTROLLER_DISABLED_ON_ERROR", ex);
            stop(player);
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { SESSIONS.remove(event.getEntity().getUUID()); }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { SESSIONS.remove(event.getEntity().getUUID()); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) stop(player);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { SESSIONS.clear(); }
}
