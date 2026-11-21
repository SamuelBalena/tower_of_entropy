package com.towerofentropy.client.camera;

/** A continuous fade driven by the projected size of a collision-constrained player. */
final class CharacterOpacity {
    private CharacterOpacity() {}

    static double target(double clearance, double comfortable, double close, double minimum) {
        double visible = Math.max(0, Math.min(1, (clearance - close) / (comfortable - close)));
        double smooth = visible * visible * (3 - 2 * visible);
        return minimum + (1 - minimum) * smooth;
    }

    static double update(double current, double target, double seconds) {
        double value = CameraDamping.approach(current, target, seconds, target < current ? .065 : .14);
        return Math.abs(value - target) < .002 ? target : value;
    }
}
