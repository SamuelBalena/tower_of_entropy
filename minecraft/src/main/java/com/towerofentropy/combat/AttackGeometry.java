package com.towerofentropy.combat;

/** Pure geometry shared by server selection and standalone boundary tests. */
public final class AttackGeometry {
    public static boolean inRegion(double dx, double dy, double dz, double yawDegrees, double reach, double arcDegrees) {
        if (!Double.isFinite(dx + dy + dz + yawDegrees + reach + arcDegrees)) return false;
        if (dx * dx + dy * dy + dz * dz > reach * reach) return false;
        double horizontal = Math.hypot(dx, dz);
        if (horizontal < 1e-8) return false;
        double yaw = Math.toRadians(yawDegrees);
        double forward = (-Math.sin(yaw) * dx + Math.cos(yaw) * dz) / horizontal;
        return forward + 1e-10 >= Math.cos(Math.toRadians(arcDegrees / 2));
    }
}
