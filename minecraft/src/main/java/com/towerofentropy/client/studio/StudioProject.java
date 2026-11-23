package com.towerofentropy.client.studio;

import java.util.ArrayList;
import java.util.List;
import org.joml.Quaternionf;

record StudioProject(int schema, String name, String model, String skeleton, boolean idle,
                     int frames, int fps, boolean loop, List<Key> keys) {
    enum Curve { LINEAR, STEP, SMOOTH }

    record Transform(float x, float y, float z, float qx, float qy, float qz, float qw) {
        static final Transform IDENTITY = new Transform(0, 0, 0, 0, 0, 0, 1);

        Quaternionf rotation() { return new Quaternionf(qx, qy, qz, qw); }

        Transform rotate(Quaternionf rotation) {
            rotation.normalize();
            return new Transform(x, y, z, rotation.x, rotation.y, rotation.z, rotation.w);
        }

        boolean valid(boolean root) {
            return Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z)
                    && Math.abs(x) <= 2 && Math.abs(y) <= 2 && Math.abs(z) <= 2
                    && (root || x == 0 && y == 0 && z == 0)
                    && Math.abs(rotation().lengthSquared() - 1) < 0.002f;
        }
    }

    record Key(String bone, int frame, Transform transform, Curve curve) {}

    StudioProject {
        keys = keys.stream().sorted(java.util.Comparator.comparing(Key::bone).thenComparingInt(Key::frame)).toList();
    }

    StudioProject settings(String title, int duration, int rate, boolean repeat, boolean idleReference) {
        return new StudioProject(schema, title, model, skeleton, idleReference, duration, rate, repeat,
                keys.stream().filter(key -> key.frame <= duration).toList());
    }

    StudioProject replace(Key old, Key next) {
        var copy = new ArrayList<>(keys);
        copy.remove(old);
        if (next != null) {
            copy.removeIf(key -> key.bone.equals(next.bone) && key.frame == next.frame);
            copy.add(next);
        }
        return new StudioProject(schema, name, model, skeleton, idle, frames, fps, loop, copy);
    }

    StudioProject retime(int duration, int rate, boolean idleReference) {
        var adjusted = new ArrayList<Key>(keys.size());
        var used = new java.util.HashSet<String>();
        for (var key : keys) {
            int frame = (int) Math.round((double) key.frame * rate / fps);
            if (frame > duration) throw new IllegalArgumentException("Mova as chaves que excedem a duração");
            if (!used.add(key.bone + "/" + frame)) throw new IllegalArgumentException("FPS escolhido sobrepõe chaves");
            adjusted.add(new Key(key.bone, frame, key.transform, key.curve));
        }
        return new StudioProject(schema, name, model, skeleton, idleReference, duration, rate, loop, adjusted);
    }

    Transform sample(String bone, double frame) {
        Key left = null, right = null;
        for (var key : keys) {
            if (!key.bone.equals(bone)) continue;
            if (key.frame <= frame && (left == null || key.frame > left.frame)) left = key;
            if (key.frame > frame && (right == null || key.frame < right.frame)) right = key;
        }
        if (left == null && right == null) return Transform.IDENTITY;
        if (left == null) return right.transform;
        if (right == null || left.curve == Curve.STEP) return left.transform;
        float t = (float) ((frame - left.frame) / (right.frame - left.frame));
        if (left.curve == Curve.SMOOTH) t = t * t * (3 - 2 * t);
        var a = left.transform;
        var b = right.transform;
        var q = a.rotation().slerp(b.rotation(), t).normalize();
        return new Transform(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t,
                q.x, q.y, q.z, q.w);
    }
}
