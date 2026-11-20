package com.towerofentropy.client;

import com.towerofentropy.combat.WeaponTags;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

/** Positive eligibility for generic attack clips. Unknown equipment gets the vanilla renderer. */
final class SwingItemPolicy {
    private static final TagKey<Item> SPECIALIZED = tag("towerofentropy", "specialized_equipment");
    private static final TagKey<Item> COMMON = tag("towerofentropy", "generic_attack_items");
    private static final TagKey<Item> FORGE_TOOLS = tag("forge", "tools");
    private static final Set<ToolAction> TOOL_ACTIONS = Stream.of(ToolActions.DEFAULT_AXE_ACTIONS,
            ToolActions.DEFAULT_HOE_ACTIONS, ToolActions.DEFAULT_SHOVEL_ACTIONS, ToolActions.DEFAULT_PICKAXE_ACTIONS,
            ToolActions.DEFAULT_SWORD_ACTIONS, ToolActions.DEFAULT_SHEARS_ACTIONS, ToolActions.DEFAULT_SHIELD_ACTIONS,
            ToolActions.DEFAULT_FISHING_ROD_ACTIONS).flatMap(Set::stream).collect(Collectors.toUnmodifiableSet());

    private SwingItemPolicy() {}

    static boolean allowsPunch(ItemStack stack) {
        if (stack.isEmpty()) return true;
        var item = stack.getItem();
        if (WeaponTags.isWeapon(stack) || stack.is(SPECIALIZED) || stack.is(FORGE_TOOLS) || stack.getMaxDamage() > 0
                || item instanceof TieredItem || item instanceof ProjectileWeaponItem || item instanceof Equipable
                || item.getEquipmentSlot(stack) != null) return false;
        var animation = stack.getUseAnimation();
        if (animation != UseAnim.NONE && animation != UseAnim.EAT && animation != UseAnim.DRINK) return false;
        if (TOOL_ACTIONS.stream().anyMatch(stack::canPerformAction)) return false;
        return stack.is(COMMON) || item.getClass() == Item.class || item.getClass() == BlockItem.class
                || item.getClass() == StandingAndWallBlockItem.class;
    }

    private static TagKey<Item> tag(String namespace, String name) {
        return TagKey.create(Registries.ITEM, new ResourceLocation(namespace, name));
    }
}
