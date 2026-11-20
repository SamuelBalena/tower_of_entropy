package com.towerofentropy.client;

import com.towerofentropy.combat.WeaponTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Samples the vanilla swing after gameplay has handled the click. */
@Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT)
public final class ClientSwingPose {
    private static LocalPlayer owner;
    private static int previousTick = Integer.MAX_VALUE;
    private static InteractionHand swingHand;
    private static InteractionHand attackHand;
    private static int attackTick = Integer.MIN_VALUE;
    private static String clip;
    private static boolean jab, mining, blockAttack, swordBlock;
    private static int miningStart, miningDuration;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void entityAttack(AttackEntityEvent event) {
        observe(event.getEntity(), InteractionHand.MAIN_HAND, false);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void emptyAttack(PlayerInteractEvent.LeftClickEmpty event) {
        observe(event.getEntity(), event.getHand(), false);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void blockAttack(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getUseItem() != Event.Result.DENY && !event.getLevel().getBlockState(event.getPos()).isAir()
                && (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START
                    || event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.CLIENT_HOLD)) {
            observe(event.getEntity(), event.getHand(), true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void input(InputEvent.InteractionKeyMappingTriggered event) {
        var player = Minecraft.getInstance().player;
        if (event.isUseItem()) {
            attackTick = Integer.MIN_VALUE;
            attackHand = null;
        } else if (event.isAttack() && event.getHand() == InteractionHand.OFF_HAND
                && !event.isCanceled() && event.shouldSwingHand()) {
            observe(player, event.getHand(), Minecraft.getInstance().hitResult != null
                    && Minecraft.getInstance().hitResult.getType() == HitResult.Type.BLOCK);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void use(PlayerInteractEvent event) {
        if (event.getEntity() != Minecraft.getInstance().player) return;
        if (event instanceof PlayerInteractEvent.RightClickBlock || event instanceof PlayerInteractEvent.RightClickItem
                || event instanceof PlayerInteractEvent.EntityInteract || event instanceof PlayerInteractEvent.EntityInteractSpecific) {
            attackTick = Integer.MIN_VALUE;
            attackHand = null;
        }
    }

    private static void observe(net.minecraft.world.entity.player.Player entity, InteractionHand hand, boolean block) {
        var player = Minecraft.getInstance().player;
        if (player == null || entity != player) return;
        selectOwner(player);
        attackHand = hand;
        blockAttack = block;
        attackTick = player.tickCount;
    }

    private static void selectOwner(LocalPlayer player) {
        if (player == owner) return;
        reset();
        owner = player;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        sample(Minecraft.getInstance().player);
    }

    private static void sample(LocalPlayer player) {
        selectOwner(player);
        if (player != null && sampleMining(player)) return;
        if (player == null || !player.swinging) {
            clip = null;
            swordBlock = false;
            previousTick = Integer.MAX_VALUE;
            return;
        }
        if (player.swingTime < previousTick || swingHand != player.swingingArm) {
            swingHand = player.swingingArm;
            boolean observedAttack = swingHand == attackHand && attackTick >= player.tickCount - 1 && !player.isUsingItem();
            attackTick = Integer.MIN_VALUE;
            attackHand = null;
            swordBlock = allowsSwordBlock(player, swingHand)
                    && (observedAttack && blockAttack || swingHand == InteractionHand.MAIN_HAND && swordMining(player));
            clip = observedAttack && SwingItemPolicy.allowsPunch(player.getItemInHand(swingHand)) ? nextPunch() : null;
        }
        previousTick = player.swingTime;
    }

    static boolean swordBlock(LocalPlayer player) {
        sample(player);
        return swordMining(player) || swordBlock && player.swinging && !player.isUsingItem()
                && allowsSwordBlock(player, swingHand);
    }

    static boolean swordBlockLeft(LocalPlayer player) {
        var hand = swordMining(player) ? InteractionHand.MAIN_HAND : swingHand;
        return (hand == InteractionHand.MAIN_HAND) == (player.getMainArm() == HumanoidArm.LEFT);
    }

    private static boolean swordMining(LocalPlayer player) {
        return allowsSwordBlock(player, InteractionHand.MAIN_HAND) && digging(player);
    }

    private static boolean allowsSwordBlock(LocalPlayer player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        return stack.getItem() instanceof SwordItem && !WeaponTags.isWeapon(stack);
    }

    static String clip(LocalPlayer player) {
        sample(player);
        return clip != null && !player.isUsingItem() && SwingItemPolicy.allowsPunch(player.getItemInHand(swingHand)) ? clip : null;
    }

    static boolean mirrored(LocalPlayer player) {
        var hand = mining ? InteractionHand.MAIN_HAND : swingHand;
        if (hand == null || clip == null) return false;
        if (hand == InteractionHand.MAIN_HAND && player.getItemInHand(hand).isEmpty()) return false;
        var arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        return jab != (arm == HumanoidArm.LEFT);
    }

    static float time(LocalPlayer player, float duration, float partial) {
        float phase = mining ? (player.tickCount - miningStart + partial) / miningDuration : player.getAttackAnim(partial);
        return duration * Mth.clamp(phase, 0, 1);
    }

    private static boolean sampleMining(LocalPlayer p) {
        boolean digging = digging(p) && SwingItemPolicy.allowsPunch(p.getMainHandItem());
        if (!digging) {
            if (mining) {
                mining = false;
                if (p.swinging && p.swingingArm != swingHand) previousTick = Integer.MAX_VALUE;
            }
            return false;
        }
        if (!mining || p.tickCount - miningStart >= miningDuration) {
            if (!mining && clip != null && p.swinging && swingHand == InteractionHand.MAIN_HAND && p.swingingArm == swingHand) {
                miningStart = p.tickCount - Math.max(0, p.swingTime);
            } else {
                miningStart = p.tickCount;
                clip = nextPunch();
            }
            var haste = p.getEffect(MobEffects.DIG_SPEED);
            var fatigue = p.getEffect(MobEffects.DIG_SLOWDOWN);
            miningDuration = Math.max(1, haste != null ? 5 - haste.getAmplifier()
                    : fatigue != null ? 8 + 2 * fatigue.getAmplifier() : 6);
        }
        mining = true;
        swordBlock = false;
        swingHand = InteractionHand.MAIN_HAND;
        previousTick = p.swingTime;
        return true;
    }

    static boolean needsVanilla(LocalPlayer p) {
        if (digging(p) && !SwingItemPolicy.allowsPunch(p.getMainHandItem())) return true;
        return p.swinging && clip(p) == null;
    }

    static boolean digging(LocalPlayer p) {
        var mc = Minecraft.getInstance();
        return mc.screen == null && mc.gameMode != null && mc.gameMode.isDestroying()
                && mc.options.keyAttack.isDown() && mc.hitResult != null
                && mc.hitResult.getType() == HitResult.Type.BLOCK
                && !p.isUsingItem();
    }

    private static String nextPunch() {
        jab = !jab;
        return jab ? "punch_jab_front" : "punch_cross_front";
    }

    static void reset() {
        owner = null;
        previousTick = Integer.MAX_VALUE;
        swingHand = null;
        attackHand = null;
        attackTick = Integer.MIN_VALUE;
        clip = null;
        jab = false;
        blockAttack = false;
        swordBlock = false;
        mining = false;
    }
}
