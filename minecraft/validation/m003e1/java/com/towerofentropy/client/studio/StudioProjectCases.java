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
    }

    private static void close(float actual, float expected) {
        if (Math.abs(actual - expected) > .00001f) throw new AssertionError(actual + " != " + expected);
    }
}
