package com.towerofentropy.combat;

import com.mojang.logging.LogUtils;
import com.towerofentropy.client.PlayerRendererR3;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

@Mod.EventBusSubscriber(modid = "towerofentropy", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GlobalPunchRuntimeTest {
    static final boolean ENABLED = Boolean.getBoolean("toe.global.punch.test");
    static final int END = 2660;
    private static final ResourceLocation FIXTURE_ITEM = new ResourceLocation("towerofentropy", "qa_generic_item");
    private static final BlockPos MINE = new BlockPos(0, 101, 2);
    private static final Set<String> CLIPS = Set.of("UAL_punch_jab_front", "UAL_punch_cross_front");
    private static final Set<String> tested = new HashSet<>(), sequence = new HashSet<>();
    private static long drawStart, itemStart, vanillaStart;
    private static volatile int target = -1;
    private static volatile float health = 20;
    private static boolean chopped;
    private static int heldVanilla;
    private static final String[] CASES = {"empty", "block", "pickaxe_entity", "generic_item", "dual_main", "dual_off"};

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        if (ENABLED) event.register(Registries.ITEM, FIXTURE_ITEM, () -> new Item(new Item.Properties()));
    }

    static void tick(Minecraft mc, int age, String clip) {
        var p = mc.player;
        if (age == 2310) A3RuntimeTest.server(server -> {
            server.teleportTo(server.serverLevel(), .5, 100, .5, 0, 0);
            server.getInventory().selected = 8;
            server.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(8));
            for (int y = 100; y < 116; y++)
                server.serverLevel().setBlockAndUpdate(new BlockPos(0, y, 1), Blocks.AIR.defaultBlockState());
            var zombie = new Zombie(EntityType.ZOMBIE, server.serverLevel());
            zombie.setPos(3, 100, .5);
            zombie.setNoAi(true);
            zombie.setPersistenceRequired();
            server.serverLevel().addFreshEntity(zombie);
            target = zombie.getId();
        });
        if (age >= 2330 && age < 2510) {
            int index = (age - 2330) / 30;
            int step = (age - 2330) % 30;
            if (step == 0) {
                Item item = switch (index) {
                    case 1 -> Items.COBBLESTONE;
                    case 2 -> Items.IRON_PICKAXE;
                    case 3 -> ForgeRegistries.ITEMS.getValue(FIXTURE_ITEM);
                    case 4, 5 -> Items.STICK;
                    default -> Items.AIR;
                };
                equip(item, index >= 4 ? Items.FEATHER : Items.AIR);
            }
            if (index != 2 && index != 5 && step == 5) p.setXRot(-90);
            if (step == 14) p.setXRot(0);
            if (step == 12) {
                drawStart = PlayerRendererR3.drawCalls;
                itemStart = PlayerRendererR3.attachmentDraws;
                vanillaStart = PlayerRendererR3.localPosts;
                if (index != 2 && index != 5) {
                    KeyMapping.click(mc.options.keyAttack.getKey());
                } else {
                    if (index == 2 && mc.level.getEntity(target) != null)
                        mc.gameMode.attack(p, mc.level.getEntity(target));
                    p.swing(index == 5 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
                }
            }
            if (step >= 13 && step < 19 && (index == 2 || index == 5)
                    && PlayerRendererR3.localPosts > vanillaStart
                    && PlayerRendererR3.fallbackReason.equals("vanilla swing") && tested.add(CASES[index])) {
                capture(mc, CASES[index]);
            }
            if (step >= 16 && step < 19 && index != 2 && index != 5 && CLIPS.contains(clip) && PlayerRendererR3.drawCalls > drawStart
                    && PlayerRendererR3.localPosts == vanillaStart && PlayerRendererR3.fallbackReason.isEmpty()
                    && (index == 0 ? p.getMainHandItem().isEmpty() && p.getOffhandItem().isEmpty()
                        : PlayerRendererR3.attachmentDraws - itemStart
                            == (index >= 4 ? 2 : 1) * (PlayerRendererR3.drawCalls - drawStart))
                    && (index < 4 || p.swingingArm == (index == 5 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND))
                    && tested.add(CASES[index])) {
                if (index >= 3) sequence.add(clip);
                capture(mc, CASES[index]);
            }
        }
        if (age == 2510) {
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            equip(Items.DIAMOND_PICKAXE, Items.AIR);
            A3RuntimeTest.server(server -> {
                if (server.serverLevel().getEntity(target) instanceof Zombie zombie) {
                    health = zombie.getHealth();
                    zombie.discard();
                }
                server.serverLevel().setBlockAndUpdate(MINE, Blocks.OBSIDIAN.defaultBlockState());
                server.teleportTo(server.serverLevel(), .5, 100, .5, 0, 0);
            });
        }
        if (age >= 2530 && age < 2580) {
            p.setYRot(0);
            p.setXRot(0);
            if (age == 2530) {
                mc.mouseHandler.grabMouse();
                KeyMapping.click(mc.options.keyAttack.getKey());
            }
            mc.options.keyAttack.setDown(true);
            if (mc.gameMode.isDestroying() && PlayerRendererR3.fallbackReason.equals("vanilla swing")
                    && ++heldVanilla == 1) capture(mc, "mining-vanilla-tool");
        }
        if (age == 2580) {
            mc.options.keyAttack.setDown(false);
            equip(Items.IRON_AXE, Items.AIR);
            A3RuntimeTest.server(server -> server.serverLevel().setBlockAndUpdate(MINE, Blocks.OAK_LOG.defaultBlockState()));
        }
        if (age >= 2600 && age < 2620) {
            if (age == 2600) KeyMapping.click(mc.options.keyAttack.getKey());
            mc.options.keyAttack.setDown(true);
            if (clip.equals("UAL_chop") && !chopped) {
                chopped = true;
                capture(mc, "chop");
            }
        }
        if (age == 2620) mc.options.keyAttack.setDown(false);
        if (age == END) {
            for (String name : CASES) check(name, tested.contains(name), "expected R3 or vanilla presentation");
            check("alternate", sequence.equals(CLIPS), sequence);
            check("entity_damage", health > 0 && health < 20, health);
            check("tool_held_mine", heldVanilla >= 4, heldVanilla);
            check("chop_priority", chopped, clip);
            check("skin", p.getModelName().equals(Boolean.getBoolean("toe.qa.slim") ? "slim" : "default"), p.getModelName());
        }
    }

    private static void equip(Item main, Item off) {
        A3RuntimeTest.server(p -> {
            p.getInventory().setItem(8, new ItemStack(main));
            p.getInventory().offhand.set(0, new ItemStack(off));
            p.inventoryMenu.broadcastFullState();
        });
    }

    private static void capture(Minecraft mc, String name) {
        var directory = A3RuntimeTest.DIR.toFile();
        directory.mkdirs();
        Screenshot.grab(directory, name + ".png", mc.getMainRenderTarget(), message -> {});
    }

    private static void check(String name, boolean pass, Object details) {
        LogUtils.getLogger().info("TOE_GLOBAL_PUNCH {} {} {}", name, pass, details);
    }
}
