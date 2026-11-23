package com.towerofentropy.client.studio;

import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Rotation channels in the same bind-relative space as schema 1 quaternions. */
record StudioRotation(Vector3f[] axes, String[] labels, int[] order, int count, boolean anatomical) {
    static StudioRotation local() {
        return new StudioRotation(new Vector3f[]{new Vector3f(1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, 1)},
                new String[]{"X local", "Y local", "Z local"}, new int[]{0, 1, 2}, 3, false);
    }

    boolean followsPose(int channel) { return !anatomical || count == 3 && order[2] == channel; }

    private Quaternionf basis() {
        var x = axes[order[0]];
        var y = axes[order[1]];
        return new Quaternionf().setFromNormalized(new Matrix3f().setColumn(0, x).setColumn(1, y)
                .setColumn(2, new Vector3f(x).cross(y))).normalize();
    }

    private float handedness() {
        return Math.signum(new Vector3f(axes[order[0]]).cross(axes[order[1]]).dot(axes[order[2]]));
    }

    Quaternionf compose(float[] degrees) {
        var b = basis();
        var local = new Quaternionf().rotationXYZ(radians(degrees[order[0]]), radians(degrees[order[1]]),
                radians(degrees[order[2]] * handedness()));
        return new Quaternionf(b).mul(local).mul(b.conjugate()).normalize();
    }

    float[] degrees(Quaternionf value) {
        var b = basis();
        var local = new Quaternionf(b).conjugate().mul(value).mul(b).normalize();
        var m = new Matrix3f().set(local);
        double cosine = Math.hypot(m.m00(), m.m10());
        double y = Math.atan2(m.m20(), cosine);
        double x = cosine < 0.000001 ? Math.atan2(Math.copySign(1, y) * m.m01(), m.m11())
                : Math.atan2(-m.m21(), m.m22());
        double z = cosine < 0.000001 ? 0 : Math.atan2(-m.m10(), m.m00());
        var result = new float[3];
        result[order[0]] = (float) Math.toDegrees(x);
        result[order[1]] = (float) Math.toDegrees(y);
        result[order[2]] = (float) Math.toDegrees(z) * handedness();
        return result;
    }

    Quaternionf drag(Quaternionf original, int channel, float radians) {
        var change = new Quaternionf().fromAxisAngleRad(axes[channel], radians);
        return (followsPose(channel) ? new Quaternionf(original).mul(change) : change.mul(original)).normalize();
    }

    private static float radians(float degrees) { return (float) Math.toRadians(degrees); }
}
