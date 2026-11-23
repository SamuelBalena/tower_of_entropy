package com.towerofentropy.client.qa;

import com.towerofentropy.client.lighting.DynamicLightAttachments;
import com.towerofentropy.client.lighting.DynamicLightAttachments.Attachment;
import com.towerofentropy.client.lighting.DynamicLightAttachments.Socket;
import com.towerofentropy.client.lighting.EquippedLightSources;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.logging.LogUtils;
import java.util.Map;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import java.io.File;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT)
public final class EquippedLightGallery implements EquippedLightFixture {
    private static final int X = 1060, Y = 84, Z = 18;
    private static final ResourceLocation BACK_ID = new ResourceLocation("towerofentropy", "qa_back_lantern");
    private static final ItemStack LANTERN = new ItemStack(Items.LANTERN);
    private static DynamicLightAttachments.Registration rear;

    private boolean skinInstalled;

    public EquippedLightGallery() {}

    @Override
    public void setClientScene(Minecraft mc, String scene) {
        if (scene.startsWith("equipped_") && !skinInstalled) {
            installSkin(mc);
            skinInstalled = true;
        }
        if (rear != null) {
            rear.close();
            rear = null;
        }
        if (scene.startsWith("equipped_back")) {
            rear = DynamicLightAttachments.register(BACK_ID, (player, partial) ->
                    List.of(new Attachment(BACK_ID, Socket.BACK_CENTER, LANTERN, null, null, true)));
        }
        if (scene.startsWith("equipped_back") || scene.equals("equipped_dual") || scene.equals("equipped_swap"))
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void drawRearLantern(RenderPlayerEvent.Pre event) {
        if (rear == null || !(event.getEntity() instanceof AbstractClientPlayer player)
                || player != Minecraft.getInstance().player) return;
        var playerPosition = player.getPosition(event.getPartialTick());
        var offset = EquippedLightSources.socketPosition(player, Socket.BACK_CENTER, event.getPartialTick())
                .subtract(playerPosition);
        var pose = event.getPoseStack();
        pose.pushPose();
        try {
            pose.translate(offset.x, offset.y, offset.z);
            pose.scale(.72f, .72f, .72f);
            Minecraft.getInstance().getItemRenderer().renderStatic(LANTERN, ItemDisplayContext.GROUND,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose, event.getMultiBufferSource(),
                    player.level(), player.getId());
        } finally {
            pose.popPose();
        }
    }

    @Override
    public void prepare(ServerPlayer player, String scene) {
        var server = player.getServer();
        var level = server.overworld();
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        level.setDayTime(18000);
        level.setWeatherParameters(20000, 0, false, false);
        level.setRainLevel(0);
        level.setThunderLevel(0);
        player.setGameMode(GameType.SURVIVAL);
        player.setInvulnerable(true);
        player.getInventory().clearContent();
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(main(scene)));
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(off(scene)));
        if (scene.equals("equipped_inventory")) player.getInventory().setItem(15, new ItemStack(Items.TORCH));
        for (int x = -8; x <= 12; x++) for (int z = -9; z <= 9; z++) {
            level.setBlock(new BlockPos(X+x, Y, Z+z), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            for (int y = 1; y <= 6; y++) {
                boolean edge = x == -8 || x == 12 || Math.abs(z) == 9 || y == 6;
                level.setBlock(new BlockPos(X+x, Y+y, Z+z),
                        (edge ? Blocks.DEEPSLATE_TILES : Blocks.AIR).defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        Block screen = switch (scene) {
            case "equipped_wall" -> Blocks.STONE;
            case "equipped_glass" -> Blocks.GLASS;
            case "equipped_slab" -> Blocks.STONE_SLAB;
            case "equipped_bars" -> Blocks.IRON_BARS;
            case "equipped_fence" -> Blocks.OAK_FENCE;
            case "equipped_stairs" -> Blocks.STONE_STAIRS;
            case "equipped_trapdoor" -> Blocks.IRON_TRAPDOOR;
            case "equipped_leaves" -> Blocks.OAK_LEAVES;
            default -> Blocks.AIR;
        };
        for (int z = -5; z < 6; z++)
            level.setBlock(new BlockPos(X+1, Y+1, Z+z), screen.defaultBlockState(), Block.UPDATE_CLIENTS);
        if (scene.equals("equipped_door_closed") || scene.equals("equipped_door_open")) {
            var door = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.OPEN, scene.equals("equipped_door_open"))
                    .setValue(DoorBlock.FACING, net.minecraft.core.Direction.EAST);
            var base = new BlockPos(X+2, Y+1, Z);
            level.setBlock(base, door, Block.UPDATE_CLIENTS);
            level.setBlock(base.above(), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_CLIENTS);
            for (int z = -4; z <= 4; z++) if (z != 0) {
                level.setBlock(base.offset(0, 0, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(base.offset(0, 1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        player.teleportTo(level, X+.5, Y+1, Z+.5, -90, pitch(scene));
    }

    @Override
    public void tick(Minecraft mc, String scene, int tick, int warmup) {
        mc.player.setYRot(-90);
        mc.player.setXRot(pitch(scene));
        if (tick == warmup) assertEquipment(mc, scene);
        boolean swap = scene.equals("equipped_swap");
        if (!swap && !scene.equals("equipped_back_motion")) return;
        mc.options.keyUp.setDown(tick >= warmup - 12 && tick < warmup + 32);
        if (swap && tick == warmup + 11) {
            var server = mc.getSingleplayerServer();
            var id = mc.player.getUUID();
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(id);
                if (player != null) player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            });
        }
        if (swap && tick == warmup + 26 && !mc.player.getMainHandItem().isEmpty())
            throw new IllegalStateException("Client did not receive the equipment removal");
        if (!swap && tick == warmup + 39) mc.options.setCameraType(CameraType.FIRST_PERSON);
        if (!swap && tick == warmup + 51) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        if (tick == warmup + 5 || tick == warmup + 27 || (!swap && tick == warmup + 47))
            Screenshot.grab(new File("."), "s00281-" + scene + "-" + tick + ".png", mc.getMainRenderTarget(), ignored -> {});
    }

    private static float pitch(String scene) {
        return scene.equals("equipped_wall") || scene.equals("equipped_glass") ? 18 : 27;
    }

    private static void assertEquipment(Minecraft mc, String scene) {
        String expected = Boolean.getBoolean("toe.s001.slim") ? "slim" : "default";
        if (!mc.player.getModelName().equals(expected)) throw new IllegalStateException("Wrong skin model in " + scene);
        LogUtils.getLogger().info("TOE_EQUIPPED_QA {} skin={} camera={}", scene, expected, mc.options.getCameraType());
        if (!matches(mc.player.getMainHandItem(), main(scene)) || !matches(mc.player.getOffhandItem(), off(scene)))
            throw new IllegalStateException("Wrong client hand equipment in " + scene);
    }

    @SuppressWarnings("unchecked")
    private static void installSkin(Minecraft mc) {
        boolean slim = Boolean.getBoolean("toe.s001.slim");
        var texture = new ResourceLocation("minecraft", slim ? "textures/entity/player/slim/steve.png"
                : "textures/entity/player/wide/steve.png");
        var info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        if (info == null) throw new IllegalStateException("No player info for equipment fixture");
        try {
            var locations = info.getClass().getDeclaredField("textureLocations");
            var model = info.getClass().getDeclaredField("skinModel");
            var pending = info.getClass().getDeclaredField("pendingTextures");
            locations.setAccessible(true);
            model.setAccessible(true);
            pending.setAccessible(true);
            var map = (Map<MinecraftProfileTexture.Type, ResourceLocation>) locations.get(info);
            map.put(MinecraftProfileTexture.Type.SKIN, texture);
            map.remove(MinecraftProfileTexture.Type.CAPE);
            model.set(info, slim ? "slim" : "default");
            pending.setBoolean(info, true);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Could not install the fixture skin", error);
        }
    }

    private static boolean matches(ItemStack stack, Item expected) {
        return expected == Items.AIR ? stack.isEmpty() : stack.is(expected);
    }

    private static Item main(String scene) {
        return switch (scene) {
            case "equipped_torch_off", "equipped_lantern_off", "equipped_back", "equipped_back_motion", "equipped_inventory" -> Items.AIR;
            case "equipped_lantern_main", "equipped_dual" -> Items.LANTERN;
            case "equipped_redstone" -> Items.REDSTONE_TORCH;
            case "equipped_soul" -> Items.SOUL_TORCH;
            default -> Items.TORCH;
        };
    }

    private static Item off(String scene) {
        return switch (scene) {
            case "equipped_torch_off" -> Items.TORCH;
            case "equipped_lantern_off" -> Items.LANTERN;
            case "equipped_dual" -> Items.SOUL_LANTERN;
            default -> Items.AIR;
        };
    }
}
