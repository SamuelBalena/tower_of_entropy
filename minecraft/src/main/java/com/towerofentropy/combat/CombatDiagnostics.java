package com.towerofentropy.combat;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Optional server-thread counters for one selected player. */
@Mod.EventBusSubscriber(modid = "towerofentropy")
public final class CombatDiagnostics {
    private static final boolean DEFAULT_ENABLED = Boolean.parseBoolean(System.getProperty(
            "toe.combat.diagnostics.default", Boolean.toString(!FMLEnvironment.production)));
    private static boolean autoSelected;
    private static CombatDiagnostics active;
    private ServerPlayer player;
    private final long since;
    private int scans, examined, censuses, censusMobs, targeting, toggleRequests, toggles, states, speedChanges;
    private int packetsIn, packetsOut, rollsStarted, rollsCompleted, rollsInterrupted, errors;
    private String lastRollEnd = "none";
    private final Timing scanTime = new Timing(), serverTickTime = new Timing();
    private long censusNanos, tickStarted;

    private CombatDiagnostics(ServerPlayer player) {
        this.player = player;
        since = player.level().getGameTime();
    }

    public static CombatDiagnostics of(ServerPlayer player) {
        var current = active;
        return current != null && current.player == player ? current : null;
    }

    public static void start(ServerPlayer player) {
        autoSelected = true;
        if (active != null) active.flush();
        active = new CombatDiagnostics(player);
    }

    public static void stop(ServerPlayer player) {
        var current = of(player);
        if (current != null) {
            current.flush();
            active = null;
        }
    }

    public static void disable() {
        autoSelected = true;
        if (active != null) stop(active.player);
    }

    static void clear() {
        active = null;
        autoSelected = false;
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (DEFAULT_ENABLED && !autoSelected && event.getEntity() instanceof ServerPlayer player) start(player);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (active != null && event.getEntity() instanceof ServerPlayer player
                && player.getUUID().equals(active.player.getUUID())) active.player = player;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        var current = active;
        if (current == null) return;
        if (event.phase == TickEvent.Phase.START) {
            current.tickStarted = System.nanoTime();
            return;
        }
        if (current.tickStarted != 0) {
            current.serverTickTime.add(System.nanoTime() - current.tickStarted);
            current.tickStarted = 0;
        }
        if (current.player.level().getGameTime() - current.since >= 200) start(current.player);
    }

    boolean needsCensus() { return censuses == 0 && scans > 0; }

    void scan(int visited, int matches, long elapsed, boolean census) {
        if (census) {
            censuses++;
            censusMobs = visited;
            targeting = matches;
            censusNanos = elapsed;
        } else {
            scans++;
            examined += visited;
            scanTime.add(elapsed);
        }
    }

    public void incoming() { packetsIn++; }
    public void outgoing() { packetsOut++; }
    public void error() { errors++; }
    void toggleRequest() { toggleRequests++; }
    void toggleAccepted() { toggles++; }
    void state() { states++; }
    void speedChange() { speedChanges++; }
    void rollStarted() { rollsStarted++; }
    void rollEnded(String reason) {
        lastRollEnd = reason;
        if ("complete".equals(reason)) rollsCompleted++;
        else rollsInterrupted++;
    }

    private void flush() {
        LogUtils.getLogger().info(
                "TOE_W0023 ticks={} weapon={} threat={} toggles={}/{} states={} speed={} packetsIn={} packetsOut={} "
                + "scans={} examined={} scanUsTotal={} scanUsMax={} censuses={} censusMobs={} aiming={} censusUs={} "
                + "rollStarted={} rollCompleted={} rollInterrupted={} lastRollEnd={} errors={} serverTicks={} serverTickUsTotal={} serverTickUsMax={}",
                player.level().getGameTime() - since, WeaponReadiness.posture(player), WeaponReadiness.threat(player),
                toggles, toggleRequests, states, speedChanges, packetsIn, packetsOut, scans, examined, scanTime.total / 1000,
                scanTime.max / 1000, censuses, censusMobs, targeting, censusNanos / 1000,
                rollsStarted, rollsCompleted, rollsInterrupted, lastRollEnd, errors,
                serverTickTime.samples, serverTickTime.total / 1000, serverTickTime.max / 1000);
    }

    /** Allocation-free samples within a window. Additional categories can add their own timing. */
    private static final class Timing {
        int samples;
        long total, max;

        void add(long nanos) {
            samples++;
            total += nanos;
            max = Math.max(max, nanos);
        }
    }
}
