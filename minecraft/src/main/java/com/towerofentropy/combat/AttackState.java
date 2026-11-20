package com.towerofentropy.combat;

/** Server timeline; the visual registry can be pending without advancing the damage clock. */
public final class AttackState {
    private long started = -1;
    private boolean spent;
    public void start(long tick) { if (started < 0) started = tick; }
    public boolean started() { return started >= 0; }
    public long startedAt() { return started; }
    public boolean consumeImpact(long tick, int impactTicks) {
        if (!started() || spent || tick < started + impactTicks) return false;
        spent = true; return true;
    }
    public boolean finished(long tick, int durationTicks) { return started() && tick >= started + durationTicks; }
}
