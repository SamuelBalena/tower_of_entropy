package com.towerofentropy.combat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.mojang.logging.LogUtils;
import com.towerofentropy.animation.LocomotionController;
import com.towerofentropy.animation.VisualActionWindow.Action;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.SwordItem;
import com.towerofentropy.animation.ActionNetwork;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** All mutations run on the logical server. No persistent combat mode or client damage authority. */
@Mod.EventBusSubscriber(modid="towerofentropy")
public final class CombatController {
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
    public enum Mode { AUTO, ON, OFF }
    private static final Map<UUID,Mode> MODES=new ConcurrentHashMap<>();
    public static Mode mode(UUID id){return MODES.getOrDefault(id,Mode.AUTO);}
    public static boolean supportedWeapon(ServerPlayer p){return EffectiveEquipment.selected(p).getItem() instanceof SwordItem;}
    private static boolean desired(ServerPlayer p){return p.isAlive()&&!p.isSpectator()&&mode(p.getUUID())!=Mode.OFF&&(mode(p.getUUID())==Mode.ON||supportedWeapon(p));}
    public record Status(boolean attacking, boolean started, int accepted, int impacts, int hits,
                         long startedTick, long impactTick, int targetId) { }
    private static final class Session {
        AttackState attack;
        CombatMove move,next=CombatMove.LIGHT_ATTACK_A;
        int queued;
        long continuation=-1,lastInput=-1;
        CombatConfig.Settings settings;
        ResourceKey<Level> dimension;
        long requested, lastStart=-1, lastImpact=-1;
        int accepted, impacts, hits, target=-1;
        volatile Status status = new Status(false,false,0,0,0,-1,-1,-1);
        void publish() { status = new Status(attack!=null, attack!=null&&attack.started(), accepted, impacts, hits, lastStart, lastImpact, target); }
    }
    public static boolean enabled(ServerPlayer player) { return desired(player); }
    public static Status status(UUID id) { var s=SESSIONS.get(id); return s==null?null:s.status; }
    public record ActionStatus(String weapon,String action,String clip,int queued,String nextLight,long comboDeadline){}
    public static ActionStatus actionStatus(ServerPlayer p){
        var s=SESSIONS.get(p.getUUID());String weapon=p.getMainHandItem().getItem() instanceof SwordItem?"SWORD":"UNSUPPORTED";
        String action="IDLE",clip="idle";
        var locomotion=LocomotionController.status(p.getUUID());
        if(locomotion!=null){action=locomotion.state().name();clip=action.equals("JOG")?"jog":"idle";}
        if(RollController.busy(p.getUUID())){action="ROLL";clip="roll";}
        else if(s!=null&&s.attack!=null){action=s.move.name();clip=s.move.clip;}
        return new ActionStatus(weapon,action,clip,s==null?0:s.queued,s==null?"LIGHT_ATTACK_A":s.next.name(),s==null?-1:s.continuation);
    }
    public static void enable(ServerPlayer player) throws Exception {
        MODES.put(player.getUUID(),Mode.ON);ActionNetwork.combatMode(player,Mode.ON);
        refresh(player);
    }
    private static void activate(ServerPlayer player) throws Exception {
        if(SESSIONS.containsKey(player.getUUID()))return;
        if (LocomotionController.status(player.getUUID())==null) LocomotionController.enable(player,false);
        SESSIONS.put(player.getUUID(),new Session());
        ActionNetwork.combatState(player,true,"",0,0);
    }
    public static void disable(ServerPlayer player) {
        MODES.put(player.getUUID(),Mode.OFF);ActionNetwork.combatMode(player,Mode.OFF);
        deactivate(player);
    }
    public static void automatic(ServerPlayer player)throws Exception{
        MODES.remove(player.getUUID());ActionNetwork.combatMode(player,Mode.AUTO);refresh(player);
    }
    private static void deactivate(ServerPlayer player){
        HeavyAirLock.end(player);
        var s=SESSIONS.remove(player.getUUID());
        if(s!=null&&(s.attack!=null||RollController.busy(player.getUUID()))) LocomotionController.cancelAction(player);
        ActionNetwork.combatState(player,false,"",0,0);
    }
    public static void refresh(ServerPlayer p)throws Exception{
        if(desired(p))activate(p);
        else if(SESSIONS.containsKey(p.getUUID()))deactivate(p);
    }
    @SubscribeEvent public static void automaticTick(TickEvent.PlayerTickEvent e){
        if(e.phase==TickEvent.Phase.START&&e.player instanceof ServerPlayer p)try{refresh(p);}catch(Exception ex){LogUtils.getLogger().error("TOE AUTO activation failed",ex);}
    }
    public static String requestSword(ServerPlayer player) throws Exception {
        return requestAttack(player,false);
    }
    public static String requestAttack(ServerPlayer player,boolean heavy) throws Exception {
        refresh(player);
        var s=SESSIONS.get(player.getUUID());
        if(s==null) return "combat disabled";
        if(RollController.busy(player.getUUID()))return "busy";
        if(!valid(player)) return "sword in main hand and supported state required";
        if(heavy&&(!player.getOffhandItem().isEmpty()||player.isUsingItem()))return "contextual use";
        long now=player.serverLevel().getGameTime();
        if(s.lastInput==now)return "duplicate input";
        s.lastInput=now;
        if(s.attack!=null){if(heavy||s.move==CombatMove.HEAVY_ATTACK)return "busy";if(s.queued>=2)return "queue full";++s.queued;return "queued";}
        if(now>s.continuation)s.next=CombatMove.LIGHT_ATTACK_A;
        start(player,s,heavy?CombatMove.HEAVY_ATTACK:s.next);
        return "accepted";
    }
    private static void start(ServerPlayer player,Session s,CombatMove move){
        s.move=move;
        s.attack=new AttackState(); s.settings=CombatConfig.snapshot(); s.dimension=player.level().dimension();
        if(move==CombatMove.HEAVY_ATTACK)s.settings=new CombatConfig.Settings(s.settings.reach(),s.settings.arc(),CombatConfig.HEAVY_DAMAGE.get().floatValue(),move.impactTicks);
        s.requested=player.serverLevel().getGameTime(); ++s.accepted; s.publish();
        s.lastStart=s.requested;s.attack.start(s.lastStart);s.publish();
        boolean heavyAir=move==CombatMove.HEAVY_ATTACK&&!player.onGround();
        if(heavyAir) HeavyAirLock.begin(player);
        ActionNetwork.combatState(player,true,move.clip,move.durationTicks,0,heavyAir);
        if(player.onGround()) AttackMovement.begin(player,move,s.lastStart);
        else AttackMovement.end(player);
    }
    public static boolean valid(ServerPlayer p) {
        return desired(p) && supportedWeapon(p)
            && WeaponReadiness.canUse(p,net.minecraft.world.InteractionHand.MAIN_HAND)
            && p.containerMenu==p.inventoryMenu && LocomotionController.supportedSwordPosture(p)
            && !com.towerofentropy.movement.WallSlide.holdsPlayer(p);
    }
    /** Called only after the real CPM playAnimation start succeeds, never when merely queued. */
    public static void animationStarted(ServerPlayer player, Action action) {
        var s=SESSIONS.get(player.getUUID());
        if(s!=null&&s.attack!=null&&!s.attack.started()&&action==Action.SWORD_ATTACK) {
            s.lastStart=player.serverLevel().getGameTime(); s.attack.start(s.lastStart); s.publish();
        }
    }
    public static void cancel(ServerPlayer player) {
        cancel(player,true);
    }
    public static void cancel(ServerPlayer player,boolean notify) {
        AttackMovement.end(player);
        HeavyAirLock.end(player);
        var s=SESSIONS.get(player.getUUID());
        if(s!=null){boolean had=s.attack!=null;s.attack=null;s.queued=0;s.next=CombatMove.LIGHT_ATTACK_A;s.continuation=-1;s.publish();if(had&&notify)ActionNetwork.combatState(player,true,"",0,0);}
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if(server==null)return;
        for(var entry:SESSIONS.entrySet()) {
            var player=server.getPlayerList().getPlayer(entry.getKey()); var s=entry.getValue();
            if(player==null){SESSIONS.remove(entry.getKey());continue;}
            if(s.attack==null)continue;
            long now=player.serverLevel().getGameTime();
            if(!valid(player)||player.level().dimension()!=s.dimension||now-s.requested>155) {
                LogUtils.getLogger().info("TOE_A1 ATTACK_CANCEL valid={} ground={} swing={} sprint={} hurt={} menu={} elapsed={}",valid(player),player.onGround(),player.swinging,player.isSprinting(),player.hurtTime,player.containerMenu==player.inventoryMenu,now-s.requested);
                LocomotionController.cancelAction(player);cancel(player);continue;
            }
            if(s.attack.consumeImpact(now,s.move.impactTicks)) {
                ++s.impacts;s.lastImpact=now;
                var hit=SwordTargeting.impact(player,s.settings);s.target=hit.targetId();if(hit.damaged())++s.hits;
                LogUtils.getLogger().info("TOE_M002_0 IMPACT player={} start={} tick={} target={} damaged={}",player.getScoreboardName(),s.lastStart,now,s.target,hit.damaged());
            }
            if(s.attack.finished(now,s.move.durationTicks)){
                s.next=s.move.next();s.continuation=now+CombatConfig.COMBO_WINDOW.get();
                boolean end=s.move==CombatMove.LIGHT_ATTACK_C||s.move==CombatMove.HEAVY_ATTACK;
                s.attack=null;
                HeavyAirLock.end(player);
                if(end)s.queued=0;
                if(s.queued>0){--s.queued;start(player,s,s.next);}
                else {AttackMovement.end(player);ActionNetwork.combatState(player,true,"",0,0);}
            }
            s.publish();
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){SESSIONS.remove(e.getEntity().getUUID());MODES.remove(e.getEntity().getUUID());}
    private static void reset(net.minecraft.world.entity.player.Player player){SESSIONS.remove(player.getUUID());MODES.remove(player.getUUID());if(player instanceof ServerPlayer p){HeavyAirLock.end(p);AttackMovement.end(p);ActionNetwork.combatMode(p,Mode.AUTO);ActionNetwork.combatState(p,false,"",0,0);}}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){reset(e.getEntity());}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){reset(e.getEntity());}
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){SESSIONS.clear();MODES.clear();}
    public static void inputSword(ServerPlayer player) throws Exception {
        if(!WeaponReadiness.canUse(player,net.minecraft.world.InteractionHand.MAIN_HAND))return;
        if(enabled(player)) {
            String result=requestSword(player);
            if(!result.equals("accepted")&&!result.contains("busy"))player.displayClientMessage(Component.literal("TOE combat: "+result),true);
        } else LocomotionController.requestAction(player,Action.SWORD_ATTACK);
    }
}
