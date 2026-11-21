package com.towerofentropy.combat;

import com.mojang.logging.LogUtils;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT)
public final class CombatCrowdAudit {
    private static final boolean ENABLED = Boolean.getBoolean("toe.w0023.qa");
    private static final int WINDOW = 120, WARMUP = 30;
    private static final String[] SCENES = {"A", "B", "C", "D", "E", "F", "G", "H_control"};
    private static final int[] COUNTS = {0, 0, 1, 10, 30, 30, 30, 30};
    private static final List<Zombie> zombies = new ArrayList<>();
    private static final Samples ticks = new Samples(), frames = new Samples();
    private static final MemorySamples memory = new MemorySamples();
    private static boolean opening;
    private static int title, serverTick = -80, frameScene = -1, frameTick = -1;
    private static volatile int signal = -1;
    private static volatile boolean done;
    private static long tickStart, previousFrame;

    @SubscribeEvent
    public static void client(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (!opening && mc.screen instanceof TitleScreen && mc.getOverlay() == null && ++title > 45) {
            opening = true;
            mc.options.pauseOnLostFocus = false;
            mc.createWorldOpenFlows().loadLevel(mc.screen, "Global Survival one");
        }
        if (mc.player == null) return;
        if (done) { mc.stop(); return; }
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        if (signal < 0 || signal == frameTick || mc.screen != null) return;
        frameTick = signal;
        int scene = frameTick / WINDOW, offset = frameTick % WINDOW;
        if ((scene == 1 || scene == 5) && offset >= WARMUP) {
            if (offset % 2 == 0) key(GLFW.GLFW_KEY_TAB, GLFW.GLFW_PRESS);
            else key(GLFW.GLFW_KEY_TAB, GLFW.GLFW_RELEASE);
            key(GLFW.GLFW_KEY_TAB, GLFW.GLFW_REPEAT);
        }
        if (scene == 6) {
            if (offset == 40 || offset == 100) com.towerofentropy.animation.ActionNetwork.requestAttack(false);
            if (offset == 62) com.towerofentropy.animation.ActionNetwork.requestAttack(true);
            if (offset == 91) com.towerofentropy.animation.ActionNetwork.requestCombatRoll(0, -1);
        }
    }

    private static void key(int code, int action) {
        MinecraftForge.EVENT_BUS.post(new InputEvent.Key(code, 0, action, 0));
    }

    @SubscribeEvent
    public static void render(TickEvent.RenderTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END || signal < 0) return;
        int timestamp = signal;
        int scene = timestamp / WINDOW;
        if (frameScene != scene) {
            if (frameScene >= 0) reportFrames();
            frames.clear();
            frameScene = scene;
            previousFrame = 0;
        }
        long now = System.nanoTime();
        if (timestamp % WINDOW >= WARMUP && previousFrame != 0) frames.add(now - previousFrame);
        previousFrame = now;
        if (done) reportFrames();
    }

    private static void reportFrames() {
        if (frameScene >= SCENES.length || frames.size < 2) return;
        LogUtils.getLogger().info("TOE_W0023_QA {} FRAME {}", SCENES[frameScene], frames.summary(true));
        frames.clear();
    }

    @SubscribeEvent
    public static void server(TickEvent.ServerTickEvent event) {
        if (!ENABLED || done) return;
        var players = event.getServer().getPlayerList().getPlayers();
        if (players.isEmpty()) return;
        ServerPlayer p = players.get(0);
        if (event.phase == TickEvent.Phase.START) {
            serverTick++;
            if (serverTick == -79) arena(p);
            if (serverTick < 0) return;
            int scene = serverTick / WINDOW, offset = serverTick % WINDOW;
            if (scene == SCENES.length) { signal = serverTick; done = true; return; }
            if (offset == 0) setup(p, scene);
            if (offset == WARMUP) {
                LogUtils.getLogger().info("TOE_W0023_QA {} BEGIN mobs={}", SCENES[scene], COUNTS[scene]);
                if (!Boolean.getBoolean("toe.w0023.withoutDiagnostics")) CombatDiagnostics.start(p);
                ticks.clear();
                memory.start();
            }
            tickStart = System.nanoTime();
        } else if (serverTick >= 0 && serverTick % WINDOW >= WARMUP) {
            ticks.add(System.nanoTime() - tickStart);
            memory.sample();
            if (serverTick % WINDOW == WINDOW - 1) {
                CombatDiagnostics.stop(p);
                LogUtils.getLogger().info("TOE_W0023_QA {} SERVER {} {}", SCENES[serverTick / WINDOW],
                        ticks.summary(false), memory.summary());
            }
        }
        if (event.phase == TickEvent.Phase.END) signal = serverTick;
    }

    private static void arena(ServerPlayer p) {
        var level = p.serverLevel();
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, p.server);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, p.server);
        level.setDayTime(18000);
        p.setGameMode(GameType.SURVIVAL);
        p.setInvulnerable(true);
        p.teleportTo(level, .5, 160, .5, 0, 0);
        for (int x = -20; x <= 20; x++)
            for (int z = -20; z <= 20; z++)
                level.setBlock(new BlockPos(x, 159, z), Blocks.STONE.defaultBlockState(), 3);
    }

    private static void setup(ServerPlayer p, int scene) {
        zombies.forEach(Zombie::discard);
        zombies.clear();
        var level = p.serverLevel();
        p.teleportTo(level, .5, 160, .5, 0, 0);
        p.getInventory().clearContent();
        p.getInventory().selected = 0;
        p.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
        p.inventoryMenu.broadcastFullState();
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
        if (scene >= 2 && scene <= 6) WeaponReadiness.toggle(p);
        for (int i = 0; i < COUNTS[scene]; i++) {
            Zombie zombie = EntityType.ZOMBIE.create(level);
            double angle = i * Math.PI * 2 / COUNTS[scene];
            zombie.setPos(.5 + Math.sin(angle) * 16, 160, .5 + Math.cos(angle) * 16);
            zombie.setPersistenceRequired();
            level.addFreshEntity(zombie);
            zombie.setTarget(p);
            zombies.add(zombie);
        }
    }

    private static final class Samples {
        final long[] values = new long[4096];
        int size;
        long sum;
        void add(long nanos) { if (size < values.length) { values[size++] = nanos; sum += nanos; } }
        void clear() { size = 0; sum = 0; }
        String summary(boolean fps) {
            Arrays.sort(values, 0, size);
            int tail = Math.max(1, (int) Math.ceil(size * .01));
            long worst = 0;
            for (int i = size - tail; i < size; i++) worst += values[i];
            return String.format(Locale.ROOT, "n=%d meanMs=%.3f p95Ms=%.3f p99Ms=%.3f", size,
                    sum / (1e6 * size), percentile(.95), percentile(.99))
                    + (fps ? String.format(Locale.ROOT, " fps=%.2f low1=%.2f", 1e9 * size / sum, 1e9 * tail / worst) : "");
        }
        double percentile(double p) { return values[(int) Math.ceil(size * p) - 1] / 1e6; }
    }

    private static final class MemorySamples {
        private final java.lang.management.MemoryMXBean heap = ManagementFactory.getMemoryMXBean();
        private final List<java.lang.management.GarbageCollectorMXBean> collectors = ManagementFactory.getGarbageCollectorMXBeans();
        long startHeap, peak, startGc, startCollections;
        void start() {
            startHeap = peak = heap.getHeapMemoryUsage().getUsed();
            startGc = gcTime();
            startCollections = collections();
        }
        void sample() { peak = Math.max(peak, heap.getHeapMemoryUsage().getUsed()); }
        long gcTime() { return collectors.stream().mapToLong(c -> Math.max(0, c.getCollectionTime())).sum(); }
        long collections() { return collectors.stream().mapToLong(c -> Math.max(0, c.getCollectionCount())).sum(); }
        String summary() {
            return "heapStartMiB=" + (startHeap >> 20) + " heapPeakMiB=" + (peak >> 20)
                    + " gcCount=" + (collections() - startCollections) + " gcMs=" + (gcTime() - startGc);
        }
    }
}
