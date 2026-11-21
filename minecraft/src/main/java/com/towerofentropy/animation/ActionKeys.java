package com.towerofentropy.animation;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ActionKeys {
    public static KeyMapping ROLL,SWORD,WEAPON;
    @SubscribeEvent public static void register(RegisterKeyMappingsEvent event){
        ROLL=new KeyMapping("key.towerofentropy.roll",KeyConflictContext.IN_GAME,InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_V,"key.categories.towerofentropy");
        SWORD=new KeyMapping("key.towerofentropy.sword",KeyConflictContext.IN_GAME,InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_J,"key.categories.towerofentropy");
        WEAPON=new KeyMapping("key.towerofentropy.weapon",KeyConflictContext.IN_GAME,InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_TAB,"key.categories.towerofentropy");
        com.towerofentropy.client.ClientWeaponState.registerEquipment();
        event.register(ROLL);event.register(SWORD);event.register(WEAPON);
    }
    private static void migrateWeaponBinding(Minecraft mc) {
        if (mc.getOverlay() != null || !VisualTuning.SPEC.isLoaded() || VisualTuning.WEAPON_BINDING_MIGRATED.get()) return;
        var legacy = InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_H);
        if (WEAPON.getKeyModifier() == KeyModifier.NONE && WEAPON.getKey().equals(legacy)) {
            WEAPON.setKey(WEAPON.getDefaultKey());
            KeyMapping.resetMapping();
            mc.options.save();
        }
        VisualTuning.WEAPON_BINDING_MIGRATED.set(true);
        VisualTuning.WEAPON_BINDING_MIGRATED.save();
    }

    public static boolean playerListDown(KeyMapping list) {
        var mc = Minecraft.getInstance();
        return list.isDown() && !(WEAPON != null && mc.screen == null && mc.getOverlay() == null
                && WEAPON.getKey().equals(list.getKey()) && WEAPON.isConflictContextAndModifierActive()
                && mc.getConnection() != null && ActionNetwork.ready(mc.getConnection().getConnection()));
    }

    @Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT)
    public static final class Input {
        private static boolean heldRoll,heldSword,pendingWeapon;

        @SubscribeEvent public static void keyboard(InputEvent.Key event) {
            if (WEAPON != null && WEAPON.matches(event.getKey(), event.getScanCode())) toggle(event.getAction());
        }

        @SubscribeEvent public static void mouse(InputEvent.MouseButton.Post event) {
            if (WEAPON != null && WEAPON.matchesMouse(event.getButton())) toggle(event.getAction());
        }

        private static void toggle(int action) {
            if (action != GLFW.GLFW_PRESS || !WEAPON.isConflictContextAndModifierActive()) return;
            var mc = Minecraft.getInstance();
            if (mc.player != null && mc.screen == null && mc.getOverlay() == null && mc.getConnection() != null
                    && ActionNetwork.ready(mc.getConnection().getConnection())) pendingWeapon = true;
        }
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
            if(event.phase!=TickEvent.Phase.END||ROLL==null||SWORD==null)return;
            boolean roll=false,sword=false;
            while(ROLL.consumeClick())roll=true;
            while(SWORD.consumeClick())sword=true;
            while(WEAPON.consumeClick()) { /* The physical press is handled above. */ }
            boolean weapon = pendingWeapon;
            pendingWeapon = false;
            Minecraft mc=Minecraft.getInstance();
            migrateWeaponBinding(mc);
            if(mc.player==null||mc.level==null||mc.screen!=null||mc.getOverlay()!=null||mc.getConnection()==null||!ActionNetwork.ready(mc.getConnection().getConnection())){
                // Require a release after menus as well; a held typing key must not become an action on close.
                heldRoll=true;heldSword=true;return;
            }
            if(weapon)ActionNetwork.toggleWeapon();
            else if(roll&&!heldRoll){com.towerofentropy.client.ClientAttackMotion.rollRequested();ActionNetwork.requestRoll((mc.options.keyLeft.isDown()?1:0)-(mc.options.keyRight.isDown()?1:0),(mc.options.keyUp.isDown()?1:0)-(mc.options.keyDown.isDown()?1:0));}
            else if(sword&&!heldSword)ActionNetwork.request(VisualActionWindow.Action.SWORD_ATTACK);
            heldRoll=ROLL.isDown();heldSword=SWORD.isDown();
        }
    }
}
