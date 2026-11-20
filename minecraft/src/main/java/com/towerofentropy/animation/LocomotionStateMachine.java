package com.towerofentropy.animation;

/** Pure selection logic. Units: blocks per server tick; horizontal forward cosine. */
public final class LocomotionStateMachine {
    public enum State { IDLE, JOG, ROLL, SWORD_ATTACK, VANILLA_FALLBACK }
    public record Motion(double speed, double forward, boolean unsupported) { }
    public static final double START_SPEED = 0.025, STOP_SPEED = 0.0125;
    public static final double ENTER_FORWARD = 0.65, HOLD_FORWARD = 0.45;
    private State state = State.VANILLA_FALLBACK, candidate = state;
    private int stableTicks;

    public State update(Motion motion) {
        State next;
        if (motion.unsupported() || !Double.isFinite(motion.speed()) || !Double.isFinite(motion.forward())) {
            next = State.VANILLA_FALLBACK;
        } else if (motion.speed() <= STOP_SPEED) {
            next = State.IDLE;
        } else if (motion.forward() < (state == State.JOG ? HOLD_FORWARD : ENTER_FORWARD)) {
            next = State.VANILLA_FALLBACK;
        } else if (motion.speed() >= START_SPEED || state == State.JOG) {
            next = State.JOG;
        } else {
            next = State.IDLE;
        }
        if (next == State.VANILLA_FALLBACK) {
            state = candidate = next;
            stableTicks = 0;
        } else if (next == state) {
            candidate = state;
            stableTicks = 0;
        } else {
            if (candidate != next) { candidate = next; stableTicks = 0; }
            if (++stableTicks >= 3) { state = next; stableTicks = 0; }
        }
        return state;
    }
    public State state() { return state; }
}
