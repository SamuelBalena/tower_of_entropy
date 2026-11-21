package com.towerofentropy.combat;

import com.mojang.logging.LogUtils;
import com.towerofentropy.client.PlayerRendererR3;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Samples frame intervals and integrated server tick work in the same fixture on either revision. */
@Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT)
public final class WeaponPerformanceProbe {
    private static final boolean ENABLED = Boolean.getBoolean("toe.w002.profile");
    private static final boolean COUNTERS = Boolean.getBoolean("toe.w002.counters");
    private static final Window[] WINDOWS = {
        new Window("sheathed", 13, 24),
        new Window("drawn", 33, 40),
        new Window("drawn_zombie_without_target", 44, 54),
        new Window("attack_passives", 111, 124),
        new Window("combat", 141, 155),
        new Window("grace", 172, 202),
        new Window("roll", 262, 279),
        new Window("heavy", 341, 357),
        new Window("repeated_combat_transitions", 402, 670),
        new Window("empty", 775, 805)
    };
    private static long previousFrame, tickStart;
    private static Window frameWindow, tickWindow;
    private static boolean capturedCounters;

    @SubscribeEvent
    public static void frame(TickEvent.RenderTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END || Minecraft.getInstance().player == null) return;
        long now = System.nanoTime();
        var current = window();
        if (frameWindow != current) {
            if (frameWindow != null) frameWindow.reportFrames();
            frameWindow = current;
            if (current != null) current.startFrames();
        } else if (current != null) {
            current.frames.add(now - previousFrame);
        }
        previousFrame = now;
    }

    @SubscribeEvent
    public static void server(TickEvent.ServerTickEvent event) {
        if (!ENABLED) return;
        if (event.phase == TickEvent.Phase.START) {
            int age = WeaponDynamicRuntimeTest.profileAge();
            if (COUNTERS && !capturedCounters && age >= 830) {
                capturedCounters = true;
                event.getServer().startRecordingMetrics(report -> {
                    var path = java.nio.file.Path.of(System.getProperty("toe.a3.output"), "server-profile.txt");
                    var profile = report.getProfilerResults();
                    int packets = counter(profile, "toeWeaponPacket");
                    int transitions = counter(profile, "toeWeaponTransition");
                    int modifiers = counter(profile, "toeCombatSpeedChange");
                    boolean stable = packets >= 2 && packets <= 8 && packets == transitions && modifiers == transitions;
                    LogUtils.getLogger().info("TOE_W002 transition_packet_and_attribute_budget {} packets={} transitions={} modifiers={}",
                            stable ? "PASS" : "FAIL", packets, transitions, modifiers);
                    if (report.saveResults(path))
                        LogUtils.getLogger().info("TOE_W0022_PROFILE server counters saved to {}", path);
                }, ignored -> {});
            }
            var current = window();
            if (tickWindow != current) {
                if (tickWindow != null) tickWindow.reportTicks();
                tickWindow = current;
            }
            tickStart = System.nanoTime();
        } else if (tickWindow != null) {
            tickWindow.ticks.add(System.nanoTime() - tickStart);
        }
    }

    private static int counter(String profile, String name) {
        var matcher = java.util.regex.Pattern.compile("#" + name + " (\\d+)/").matcher(profile);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private static Window window() {
        int age = WeaponDynamicRuntimeTest.profileAge();
        for (var candidate : WINDOWS)
            if (age >= candidate.first && age < candidate.end) return candidate;
        return null;
    }

    private static final class Window {
        final String label;
        final int first, end;
        final Samples frames = new Samples(), ticks = new Samples();
        long draws, pipeline, pose;

        Window(String label, int first, int end) {
            this.label = label;
            this.first = first;
            this.end = end;
        }

        void startFrames() {
            draws = PlayerRendererR3.drawCalls;
            pipeline = PlayerRendererR3.pipelineNanos;
            pose = PlayerRendererR3.poseNanos;
        }

        void reportFrames() {
            long count = PlayerRendererR3.drawCalls - draws;
            LogUtils.getLogger().info("TOE_W0022_PROFILE {} FRAME {} R3draws={} R3ms={} poseMs={}",
                    label, frames.summary(), count, mean(PlayerRendererR3.pipelineNanos - pipeline, count),
                    mean(PlayerRendererR3.poseNanos - pose, count));
        }

        void reportTicks() {
            LogUtils.getLogger().info("TOE_W0022_PROFILE {} SERVER {}", label, ticks.summary());
        }
    }

    private static double mean(long nanos, long count) {
        return count == 0 ? Double.NaN : nanos / (1e6 * count);
    }

    private static final class Samples {
        private final long[] values = new long[8192];
        private int size;
        private long total;

        void add(long nanos) {
            if (size == values.length) return;
            values[size++] = nanos;
            total += nanos;
        }

        String summary() {
            if (size == 0) return "n=0";
            Arrays.sort(values, 0, size);
            return String.format(java.util.Locale.ROOT, "n=%d meanMs=%.3f p95Ms=%.3f maxMs=%.3f",
                    size, mean(total, size), values[(int) Math.ceil(size * .95) - 1] / 1e6, values[size - 1] / 1e6);
        }
    }
}
