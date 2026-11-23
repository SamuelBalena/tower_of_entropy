package com.towerofentropy.client.studio;

import java.util.List;

/** Deterministic curve and time-grid cases, runnable without Minecraft. */
public final class StudioProjectCases {
    public static void main(String[] args) {
        verify(StudioProject.Curve.LINEAR, .25f);
        verify(StudioProject.Curve.STEP, 0);
        verify(StudioProject.Curve.SMOOTH, .15625f);
        var invalid = new StudioProject.Transform(Float.NaN, 0, 0, 0, 0, 0, 1);
        if (invalid.valid(true)) throw new AssertionError("non-finite translation");
        if (new StudioProject.Transform(.25f, 0, 0, 0, 0, 0, 1).valid(false))
            throw new AssertionError("translation restricted to the root");
        rotations();
        rings();
        System.out.println("TOE_STUDIO_PROJECT_CASES PASS");
    }

    private static void verify(StudioProject.Curve curve, float quarter) {
        var end = new StudioProject.Transform(1, 0, 0, 0, 0, 0, 1);
        var clip = new StudioProject(1, "Teste", "model", "rig", false, 30, 30, true,
                List.of(new StudioProject.Key("pelvis", 0, StudioProject.Transform.IDENTITY, curve),
                        new StudioProject.Key("pelvis", 20, end, StudioProject.Curve.LINEAR)));
        close(clip.sample("pelvis", 5).x(), quarter);
        close(clip.sample("pelvis", 20).x(), 1);
        var retimed = clip.retime(60, 60, false);
        close(retimed.sample("pelvis", 10).x(), quarter);
        if (retimed.keys().get(1).frame() != 40) throw new AssertionError("reference FPS retiming");
        var session = new StudioSession(clip);
        session.autoKey = true;
        session.edit("pelvis", end);
        if (session.project.keys().get(0).curve() != curve) throw new AssertionError("auto key keeps the outgoing curve");
    }

    private static void close(float actual, float expected) {
        if (Math.abs(actual - expected) > .00001f) throw new AssertionError(actual + " != " + expected);
    }

    private static void rotations() {
        var local = StudioRotation.local();
        var mirrored = new StudioRotation(new org.joml.Vector3f[]{new org.joml.Vector3f(0, -1, 0),
                new org.joml.Vector3f(0, 0, -1), new org.joml.Vector3f(1, 0, 0)},
                new String[]{"Torção", "Abertura", "Frente"}, new int[]{2, 1, 0}, 3, true);
        for (var mode : List.of(local, mirrored)) {
            for (float[] angles : new float[][]{{15, 0, 0}, {-15, 0, 0}, {0, 45, 0}, {0, -45, 0},
                    {35, 90, -25}, {-50, -90, 67}, {15, 25, -75}, {0, 0, 0}}) {
                var q = mode.compose(angles);
                var restored = mode.compose(mode.degrees(q));
                var test = new org.joml.Vector3f(.3f, .6f, .7f);
                if (q.transform(new org.joml.Vector3f(test)).distance(restored.transform(test)) > .00002f)
                    throw new AssertionError("Euler round-trip");
                close(restored.lengthSquared(), 1);
            }
        }
    }

    private static void rings() {
        var ring = new StudioRing(0, new org.joml.Vector3f(), new org.joml.Vector3f(20, 0, 0),
                new org.joml.Vector3f(0, -14, 12));
        var drag = new StudioRing.Drag(ring.hit(20, 0), 20, 0);
        double result = 0;
        for (int i = 1; i <= 80; i++) {
            var point = ring.point(i * .1);
            result = drag.move(point.x, point.y, false);
        }
        close((float) result, 8);
        var end = ring.point(8);
        close((float) drag.move(end.x, end.y, false), 8);

        var precise = new StudioRing.Drag(ring.hit(20, 0), 20, 0);
        var quarter = ring.point(Math.PI / 2);
        var half = ring.point(Math.PI);
        close((float) precise.move(quarter.x, quarter.y, true), (float) (Math.PI / 10));
        close((float) precise.move(half.x, half.y, false), (float) (Math.PI * .6));

        var edge = new StudioRing(0, new org.joml.Vector3f(), new org.joml.Vector3f(20, 0, 0),
                new org.joml.Vector3f(0, 0, 20));
        var tangent = new StudioRing.Drag(edge.hit(0, 0), 0, 0);
        close((float) tangent.move(-10, 0, false), .5f);
        close((float) tangent.move(0, 0, false), 0);
    }
}
