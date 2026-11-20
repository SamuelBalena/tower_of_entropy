package com.towerofentropy.combat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class WeaponTags {
    public static final TagKey<Item> WEAPONS = TagKey.create(Registries.ITEM, new ResourceLocation("toe", "weapons"));

    private WeaponTags() {}

    public static boolean isWeapon(ItemStack stack) {
        return stack.is(WEAPONS);
    }
}
