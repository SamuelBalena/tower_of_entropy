package com.towerofentropy.client.qa;

import com.mojang.logging.LogUtils;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

/** Samples render submission and GPU timestamps without waiting for query results. */
final class LightingFrameProfile {
    private final int[] starts = new int[8], ends = new int[8];
    private final boolean[] pending = new boolean[8];
    private int slot, active = -1, frames, gpuFrames;
    private long cpuStart, cpuNanos, gpuNanos;
    private boolean timestamps;
    private long[] bridgeStart;

    void reset() {
        for (int i = 0; i < starts.length; i++) {
            if (starts[i] != 0) {
                GL15.glDeleteQueries(starts[i]);
                GL15.glDeleteQueries(ends[i]);
                starts[i] = ends[i] = 0;
            }
            pending[i] = false;
        }
        slot = frames = gpuFrames = 0;
        active = -1;
        cpuStart = cpuNanos = gpuNanos = 0;
        timestamps = GL.getCapabilities().OpenGL33;
        bridgeStart = bridgeCounters();
    }

    void sample(TickEvent.Phase phase) {
        if (phase == TickEvent.Phase.START) {
            cpuStart = System.nanoTime();
            if (timestamps && (frames & 15) == 0) {
                harvest(slot);
                if (!pending[slot]) {
                    if (starts[slot] == 0) {
                        starts[slot] = GL15.glGenQueries();
                        ends[slot] = GL15.glGenQueries();
                    }
                    active = slot;
                    GL33.glQueryCounter(starts[active], GL33.GL_TIMESTAMP);
                    slot = (slot + 1) % starts.length;
                }
            }
        } else if (cpuStart != 0) {
            cpuNanos += System.nanoTime() - cpuStart;
            cpuStart = 0;
            frames++;
            if (active >= 0) {
                GL33.glQueryCounter(ends[active], GL33.GL_TIMESTAMP);
                pending[active] = true;
                active = -1;
            }
        }
    }

    private void harvest(int index) {
        if (!pending[index] || GL15.glGetQueryObjecti(ends[index], GL15.GL_QUERY_RESULT_AVAILABLE) == 0) return;
        long start = GL33.glGetQueryObjecti64(starts[index], GL15.GL_QUERY_RESULT);
        long end = GL33.glGetQueryObjecti64(ends[index], GL15.GL_QUERY_RESULT);
        pending[index] = false;
        gpuNanos += end - start;
        gpuFrames++;
    }

    void report(String scene) {
        for (int i = 0; i < starts.length; i++) harvest(i);
        LogUtils.getLogger().info("TOE_LIGHT_FRAME {} cpuFrames={} cpuMs={} gpuFrames={} gpuMs={}",
                scene, frames, frames == 0 ? 0 : cpuNanos / (frames * 1e6),
                gpuFrames, gpuFrames == 0 ? 0 : gpuNanos / (gpuFrames * 1e6));
        var end = bridgeCounters();
        if (bridgeStart != null && end != null) {
            for (int i = 0; i < end.length; i++) end[i] -= bridgeStart[i];
            LogUtils.getLogger().info(
                    "TOE_LIGHT_BRIDGE {} sourceMs={} collisionMs={} uploadMs={} boundRows={} activeFrames={} cellReads={}",
                    scene, end[0] / 1e6, end[1] / 1e6, end[2] / 1e6, end[3], end[4], end[5]);
        }
    }

    private static long[] bridgeCounters() {
        try {
            return (long[]) Class.forName("com.towerofentropy.client.lighting.DynamicLightTexture")
                    .getMethod("profileCounters").invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException unavailable) {
            return null; // Standalone QA and the published reference have no bridge counters.
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not read the collision bridge counters", failure);
        }
    }
}
