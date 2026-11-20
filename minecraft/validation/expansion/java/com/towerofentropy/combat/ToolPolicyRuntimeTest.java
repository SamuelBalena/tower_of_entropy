package com.towerofentropy.combat;

import com.mojang.logging.LogUtils;
import com.towerofentropy.client.PlayerRendererR3;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.CameraType;
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
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

@Mod.EventBusSubscriber(modid = "towerofentropy", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ToolPolicyRuntimeTest {
    static final boolean ENABLED = Boolean.getBoolean("toe.tool.policy.test");
    private static final int START = 2320, STAGE = 55;
    private static final BlockPos WALL = new BlockPos(0, 101, 2);
    private static final ResourceLocation MATERIAL = id("qa_policy_material");
    private static final ResourceLocation TOOL = id("qa_policy_tool");
    private static final ResourceLocation UNKNOWN = id("qa_policy_unknown");
    private static final Set<String> PUNCHES = Set.of("UAL_punch_jab_front", "UAL_punch_cross_front");
    private static final Set<String> punches = new HashSet<>();
    private record Scenario(String name, Item main, Item off, String action, String expected) {}
    private static Scenario[] cases;
    private static long previousDraws, previousPosts;
    private static boolean passed, mixed, placement;
    private static int minePulses;
    private static volatile int target = -1;
    private static volatile float targetHealth = 20;

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        if (!ENABLED) return;
        event.register(Registries.ITEM, MATERIAL, () -> new Item(new Item.Properties()));
        event.register(Registries.ITEM, TOOL, () -> new Item(new Item.Properties()) {
            @Override public boolean canPerformAction(ItemStack stack, ToolAction action) {
                return action == ToolActions.PICKAXE_DIG;
            }
        });
        event.register(Registries.ITEM, UNKNOWN, () -> new Item(new Item.Properties()) {});
    }

    private static Scenario[] scenarios() {
        return new Scenario[]{
            air("empty", Items.AIR, "punch"),
            air("torch", Items.TORCH, "punch"),
            air("common_block", Items.COBBLESTONE, "punch"),
            air("sword", Items.IRON_SWORD, "UAL_sword"),
            air("axe", Items.IRON_AXE, "vanilla swing"),
            air("pickaxe", Items.IRON_PICKAXE, "vanilla swing"),
            air("shovel", Items.IRON_SHOVEL, "vanilla swing"),
            air("hoe", Items.IRON_HOE, "vanilla swing"),
            air("shears", Items.SHEARS, "vanilla swing"),
            air("fishing_rod", Items.FISHING_ROD, "vanilla swing"),
            air("shield", Items.SHIELD, "vanilla swing"),
            air("bow", Items.BOW, "vanilla swing"),
            air("crossbow", Items.CROSSBOW, "vanilla swing"),
            air("trident", Items.TRIDENT, "vanilla swing"),
            air("forge_material", ForgeRegistries.ITEMS.getValue(MATERIAL), "punch"),
            air("forge_tool", ForgeRegistries.ITEMS.getValue(TOOL), "vanilla swing"),
            air("forge_ambiguous", ForgeRegistries.ITEMS.getValue(UNKNOWN), "vanilla swing"),
            new Scenario("shield_offhand_does_not_block_main", Items.TORCH, Items.SHIELD, "air", "punch"),
            new Scenario("offhand_attack", Items.IRON_PICKAXE, Items.FEATHER, "offhand", "punch"),
            new Scenario("offhand_tool", Items.FEATHER, Items.IRON_PICKAXE, "offhand", "vanilla swing"),
            new Scenario("entity_attack_tool", Items.IRON_PICKAXE, Items.AIR, "entity", "vanilla swing"),
            new Scenario("pickaxe_mining", Items.DIAMOND_PICKAXE, Items.AIR, "mine", "vanilla swing"),
            new Scenario("axe_chop", Items.IRON_AXE, Items.AIR, "chop", "UAL_chop"),
            new Scenario("block_placement", Items.COBBLESTONE, Items.AIR, "place", "vanilla swing"),
            new Scenario("shield_block", Items.AIR, Items.SHIELD, "use", "vanilla context"),
            new Scenario("bow_load", Items.BOW, Items.AIR, "use", "UAL_bow_"),
            new Scenario("consume", Items.APPLE, Items.AIR, "use", "UAL_consume")
        };
    }

    private static Scenario air(String name, Item item, String expected) {
        return new Scenario(name, item, Items.AIR, "air", expected);
    }

    static boolean tick(Minecraft mc, int age, String clip) {
        if (age < START) return false;
        if (cases == null) cases = scenarios();
        int index = (age - START) / STAGE;
        int phase = (age - START) % STAGE;
        if (index >= cases.length) {
            check("alternate", punches.equals(PUNCHES), punches);
            check("entity_damage", targetHealth < 20, targetHealth);
            check("held_mining", minePulses >= 4, minePulses);
            check("placed_block", placement, placement);
            check("skin", mc.player.getModelName().equals(Boolean.getBoolean("toe.qa.slim") ? "slim" : "default"),
                    mc.player.getModelName());
            return true;
        }

        var c = cases[index];
        var player = mc.player;
        if (phase == 0) {
            passed = mixed = false;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.options.hideGui = true;
            mc.mouseHandler.grabMouse();
            A3RuntimeTest.server(server -> {
                var level = server.serverLevel();
                var inventory = server.getInventory();
                inventory.selected = 8;
                inventory.setItem(8, new ItemStack(c.main, 1));
                inventory.offhand.set(0, new ItemStack(c.off, 1));
                inventory.setItem(9, new ItemStack(Items.ARROW, 8));
                server.getFoodData().setFoodLevel(c.name.equals("consume") ? 10 : 20);
                server.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(8));
                server.inventoryMenu.broadcastFullState();
                for (int y = 100; y < 116; y++)
                    level.setBlockAndUpdate(new BlockPos(0, y, 1), Blocks.AIR.defaultBlockState());
                var block = switch (c.action) {
                    case "mine" -> Blocks.OBSIDIAN;
                    case "chop" -> Blocks.OAK_LOG;
                    case "place" -> Blocks.STONE;
                    default -> Blocks.AIR;
                };
                level.setBlockAndUpdate(WALL, block.defaultBlockState());
                server.teleportTo(level, .5, 100, .5, 0, 0);
                if (c.action.equals("entity")) {
                    var zombie = new Zombie(EntityType.ZOMBIE, level);
                    zombie.setNoAi(true);
                    zombie.setPersistenceRequired();
                    zombie.setPos(2.5, 100, .5);
                    level.addFreshEntity(zombie);
                    target = zombie.getId();
                }
            });
        }
        if (phase >= 6 && phase <= 48) {
            player.setYRot(0);
            player.setXRot(c.action.equals("air") ? -70 : 0);
        }
        if (phase == 16) switch (c.action) {
            case "offhand" -> {
                // A mod can explicitly initiate an offhand attack. Unmarked raw swings are use/unknown.
                MinecraftForge.EVENT_BUS.post(new InputEvent.InteractionKeyMappingTriggered(
                        0, mc.options.keyAttack, InteractionHand.OFF_HAND));
                player.swing(InteractionHand.OFF_HAND);
            }
            case "entity" -> {
                if (mc.level.getEntity(target) != null) {
                    mc.gameMode.attack(player, mc.level.getEntity(target));
                    player.swing(InteractionHand.MAIN_HAND);
                }
            }
            case "place", "use" -> KeyMapping.click(mc.options.keyUse.getKey());
            default -> KeyMapping.click(mc.options.keyAttack.getKey());
        }
        if (phase >= 16 && phase < 45) {
            if (c.action.equals("mine") || c.action.equals("chop")) mc.options.keyAttack.setDown(true);
            if (c.action.equals("use")) mc.options.keyUse.setDown(true);
        }
        if (phase > 16 && phase < 45) {
            boolean r3Drew = PlayerRendererR3.drawCalls > previousDraws;
            boolean vanillaDrew = PlayerRendererR3.localPosts > previousPosts;
            boolean sawPunch = r3Drew && PUNCHES.contains(clip);
            mixed |= sawPunch && !c.expected.equals("punch");
            boolean matched = switch (c.expected) {
                case "punch" -> sawPunch && !vanillaDrew;
                case "vanilla context", "vanilla swing" -> vanillaDrew && !r3Drew
                        && PlayerRendererR3.fallbackReason.equals(c.expected);
                default -> r3Drew && !vanillaDrew && clip.startsWith(c.expected);
            };
            if (matched && sawPunch) punches.add(clip);
            if (matched && c.action.equals("mine") && mc.gameMode.isDestroying()) minePulses++;
            if (matched && !passed) {
                passed = true;
                capture(mc, c.name);
            }
        }
        previousDraws = PlayerRendererR3.drawCalls;
        previousPosts = PlayerRendererR3.localPosts;
        if (phase == 48) {
            mc.options.keyAttack.setDown(false);
            mc.options.keyUse.setDown(false);
            if (c.action.equals("entity")) A3RuntimeTest.server(server -> {
                if (server.serverLevel().getEntity(target) instanceof Zombie zombie) {
                    targetHealth = zombie.getHealth();
                    zombie.discard();
                }
            });
            if (c.action.equals("place")) A3RuntimeTest.server(server ->
                    placement = server.serverLevel().getBlockState(WALL.north()).is(Blocks.COBBLESTONE));
            check(c.name, passed && !mixed, c.expected + " observed=" + passed + " unwantedPunch=" + mixed);
        }
        return false;
    }

    private static ResourceLocation id(String path) { return new ResourceLocation("towerofentropy", path); }

    private static void capture(Minecraft mc, String name) {
        var directory = A3RuntimeTest.DIR.toFile();
        directory.mkdirs();
        Screenshot.grab(directory, name + ".png", mc.getMainRenderTarget(), message -> {});
    }

    private static void check(String name, boolean pass, Object detail) {
        LogUtils.getLogger().info("TOE_TOOL_POLICY {} {} {}", name, pass, detail);
    }
}
