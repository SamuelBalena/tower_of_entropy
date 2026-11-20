package com.towerofentropy.client.camera;

/** Exponential damping evaluated in seconds, independent of render cadence. */
public final class CameraDamping {
    private CameraDamping() {}

    public static double approach(double current, double target, double seconds, double timeConstant) {
        return current + (target - current) * -Math.expm1(-seconds / timeConstant);
    }

    static double follow(double current, double target, double seconds, double timeConstant, double maxLag) {
        double filtered = approach(current, target, seconds, timeConstant);
        return Math.max(target - maxLag, Math.min(target + maxLag, filtered));
    }
}
