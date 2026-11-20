package com.towerofentropy.combat;

import net.minecraftforge.common.ForgeConfigSpec;

/** Server-owned prototype settings, snapshotted when an attack is accepted. */
public final class CombatConfig {
    public static final double DEFAULT_ADVANCE_A=.40,DEFAULT_ADVANCE_B=.50,DEFAULT_ADVANCE_C=.60,DEFAULT_ADVANCE_HEAVY=.70;
    public static final int DURATION_TICKS = 9; // ceil(433 ms / 50 ms)
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue REACH, ARC, DAMAGE;
    public static final ForgeConfigSpec.IntValue IMPACT, THREAT_GRACE;
    public static final ForgeConfigSpec.IntValue COMBO_WINDOW;
    public static final ForgeConfigSpec.DoubleValue HEAVY_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue ADVANCE_A,ADVANCE_B,ADVANCE_C,ADVANCE_HEAVY;
    public static final ForgeConfigSpec.IntValue MOVEMENT_DEFAULTS_REVISION;
    static {
        var b = new ForgeConfigSpec.Builder();
        b.push("basicIronSword");
        REACH = b.comment("Distance from player feet to target bounding-box center, blocks.").defineInRange("reach", 3.0, 0.25, 6.0);
        ARC = b.comment("Full horizontal arc in degrees; no targets behind the player.").defineInRange("arcDegrees", 90.0, 1.0, 180.0);
        DAMAGE = b.comment("Raw health points before vanilla armor, resistance and invulnerability.").defineInRange("damage", 4.0, 0.1, 40.0);
        IMPACT = b.comment("Legacy M002.0 setting retained for migration; A1 uses individual windows from CombatMove.")
            .defineInRange("impactTicks", 4, 1, DURATION_TICKS - 1);
        b.pop();
        COMBO_WINDOW=b.comment("Light combo continuation window after recovery, server ticks.").defineInRange("comboWindowTicks",24,1,100);
        HEAVY_DAMAGE=b.comment("Provisional heavy raw damage before vanilla mitigation.").defineInRange("heavyDamage",6.0,0.1,40.0);
        THREAT_GRACE=b.defineInRange("combatGraceTicks",60,5,600);
        b.push("attackMovement");
        ADVANCE_A=b.defineInRange("lightABlocks",DEFAULT_ADVANCE_A,0,.75);
        ADVANCE_B=b.defineInRange("lightBBlocks",DEFAULT_ADVANCE_B,0,.75);
        ADVANCE_C=b.defineInRange("lightCBlocks",DEFAULT_ADVANCE_C,0,.75);
        ADVANCE_HEAVY=b.defineInRange("heavyBlocks",DEFAULT_ADVANCE_HEAVY,0,.75);
        MOVEMENT_DEFAULTS_REVISION=b.comment("Internal one-time migration marker; custom distances are preserved.").defineInRange("defaultsRevision",0,0,1);
        b.pop();SPEC = b.build();
    }
    public record Settings(double reach, double arc, float damage, int impactTicks) { }
    public static double advance(CombatMove m){return switch(m){case LIGHT_ATTACK_A->ADVANCE_A.get();case LIGHT_ATTACK_B->ADVANCE_B.get();case LIGHT_ATTACK_C->ADVANCE_C.get();case HEAVY_ATTACK->ADVANCE_HEAVY.get();};}
    public static Settings snapshot() { return new Settings(REACH.get(), ARC.get(), DAMAGE.get().floatValue(), IMPACT.get()); }
}
