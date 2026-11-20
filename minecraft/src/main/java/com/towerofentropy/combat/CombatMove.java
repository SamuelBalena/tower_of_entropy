package com.towerofentropy.combat;

/** Server-owned sword clips. Other weapon categories remain reserved. */
public enum CombatMove {
    LIGHT_ATTACK_A("sword",9,4), LIGHT_ATTACK_B("sword_b",11,5), LIGHT_ATTACK_C("sword_c",26,14), HEAVY_ATTACK("heavy",25,12);
    public enum WeaponCategory { SWORD, SPEAR, AXE, BOW, UNARMED, UNSUPPORTED }
    public final String clip;
    public final int durationTicks,impactTicks;
    CombatMove(String clip,int duration,int impact){this.clip=clip;durationTicks=duration;impactTicks=impact;}
    public CombatMove next(){return this==LIGHT_ATTACK_A?LIGHT_ATTACK_B:this==LIGHT_ATTACK_B?LIGHT_ATTACK_C:LIGHT_ATTACK_A;}
    /** Keep the strike window at original speed and retime only the return to the ready pose. */
    public static float visualTime(String clip,float seconds){
        // Keep the first three/four ticks beyond impact at authored playback speed.
        if (clip.equals("sword_c")) return recovery(previousTime(seconds, 17, LIGHT_ATTACK_C.durationTicks), 1f, 1.75f, 2f);
        if (clip.equals("heavy")) {
            seconds = previousTime(seconds, 16, HEAVY_ATTACK.durationTicks);
            if (seconds <= 1.5f) return seconds;
            double u = Math.max(0, Math.min(1, (seconds - 1.5) / .2));
            return (float)(1.5 + (70/30d - 1.5) * (.24*u + 2.28*u*u - 1.52*u*u*u));
        }
        return seconds;
    }

    private static float previousTime(float seconds, int recoveryStartTick, int endTick) {
        float start = recoveryStartTick / 20f;
        float end = endTick / 20f;
        if (seconds <= start) return seconds;
        float progress = Math.min(1, (seconds - start) / (end - start));
        // Ease into phase compression to avoid a sudden change in limb velocity.
        return start + (end - start) * (progress + 2 * progress * progress - progress * progress * progress);
    }

    private static float recovery(float time, float begins, float ends, float assetEnd) {
        if (time <= begins) return time;
        float u = Math.min(1, (time - begins) / (ends - begins));
        return begins + (ends - begins) * u + (assetEnd - ends) * u * u;
    }
}
