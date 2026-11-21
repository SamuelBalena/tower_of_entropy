package com.towerofentropy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;

/** Extends the vanilla sprint predicates while leaving input and travel untouched. */
public final class DirectionalSprint {
    private DirectionalSprint() {}

    public static boolean canStart(boolean vanilla, LocalPlayer player) {
        return vanilla || (CombatSprint.down(Minecraft.getInstance().options.keySprint)
                && (sidewaysOrBackward(player.input, player) || player.isUnderWater() && aquaticImpulse(player.input)));
    }

    public static boolean hasImpulse(Input input, LocalPlayer player) {
        return input.hasForwardImpulse() || sidewaysOrBackward(input, player)
                || (player.isInWater() && (player.isUnderWater() || player.isSwimming()) && aquaticImpulse(input));
    }

    private static boolean aquaticImpulse(Input input) {
        return Math.abs(input.leftImpulse) > 1e-5f || Math.abs(input.forwardImpulse) > 1e-5f;
    }

    private static boolean sidewaysOrBackward(Input input, LocalPlayer player) {
        return (Math.abs(input.leftImpulse) >= .8f || input.forwardImpulse <= -.8f)
                && !player.isInWaterOrBubble() && !player.isInLava() && !player.isInFluidType()
                && !player.isPassenger() && !player.getAbilities().flying
                && !player.isSpectator() && !player.onClimbable()
                && !player.isShiftKeyDown() && !player.isUsingItem()
                && !ClientRollMotion.busy() && !ClientAttackMotion.active();
    }
}
