package com.towerofentropy.animation;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import com.tom.cpm.shared.MinecraftClientAccess;
import com.tom.cpm.shared.model.PlayerModelParts;
import com.tom.cpm.shared.animation.VanillaPose;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Explicit dev-only smoke test in the isolated world created for M001.1. */
@Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT)
public final class ClientIdleSmokeTest {
    private static final Logger LOG = LogUtils.getLogger();
    private static int titleTicks, worldTicks;
    private static boolean opened, finished, oldPause;
    private static int oldFov;
    private static CameraType oldCamera;
    private static Vec3 initial;

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !Boolean.getBoolean("toe.animtest.visual")) return;
        Minecraft mc = Minecraft.getInstance();
        if (!opened && mc.screen instanceof TitleScreen && mc.getOverlay() == null && ++titleTicks >= 150) {
            if (!mc.gameDirectory.toPath().resolve("saves/New World/level.dat").toFile().isFile()) {
                LOG.error("TOE_M001_1 SMOKE_ABORT: isolated New World test save not found");
                finished = true;
                return;
            }
            opened = true;
            oldPause = mc.options.pauseOnLostFocus;
            oldFov = mc.options.fov().get();
            oldCamera = mc.options.getCameraType();
            mc.options.pauseOnLostFocus = false;
            mc.createWorldOpenFlows().loadLevel(mc.screen, "New World");
        }
        if (!opened || mc.player == null || mc.level == null || mc.getOverlay() != null) return;
        ++worldTicks;
        if (worldTicks == 60) {
            mc.setScreen(null);
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            mc.options.fov().set(55);
            initial = mc.player.position();
            mc.player.connection.sendCommand("toe_animtest idle_steve");
            LOG.info("TOE_M001_1 SMOKE_IDLE_START pos={}", initial);
        }
        if (worldTicks == 160 || worldTicks == 173 || worldTicks == 186) {
            int phase = worldTicks - 160;
            Screenshot.grab(mc.gameDirectory, "toe-m001_1-idle-" + phase + ".png", mc.getMainRenderTarget(),
                message -> LOG.info("TOE_M001_1 SCREENSHOT {}", message.getString()));
            LOG.info("TOE_M001_1 SMOKE_POSITION phase={} drift={}", phase, mc.player.position().distanceTo(initial));
            var player = MinecraftClientAccess.get().getCurrentClientPlayer();
            var def = player.getModelDefinition0();
            LOG.info("TOE_M001_1 RUNTIME_MODEL phase={} state={} error={} blocked={}", phase,
                def == null ? null : def.getResolveState(), def == null ? null : def.getError(), def == null ? null : def.getBlockReason());
            if (def != null && def.getModelElementFor(PlayerModelParts.LEFT_ARM) != null) {
                var arm = def.getModelElementFor(PlayerModelParts.LEFT_ARM).getMainRoot();
                LOG.info("TOE_M001_1 RUNTIME_ARM phase={} rotation={} standingTriggers={}", phase,
                    arm.rotation.asVec3f(true), def.getAnimations().getPoseAnimations(VanillaPose.STANDING));
            }
        }
        if (worldTicks == 200) {
            mc.setScreen(new InventoryScreen(mc.player));
            LOG.info("TOE_M001_1 INVENTORY_SCREEN={} type={}", mc.screen instanceof AbstractContainerScreen<?>, mc.screen.getClass().getSimpleName());
        }
        if (worldTicks == 210) {
            mc.setScreen(null);
            mc.player.connection.sendCommand("toe_animtest off");
        }
        if (worldTicks == 240) {
            Screenshot.grab(mc.gameDirectory, "toe-m001_1-off.png", mc.getMainRenderTarget(),
                message -> LOG.info("TOE_M001_1 SCREENSHOT {}", message.getString()));
            LOG.info("TOE_M001_1 SMOKE_DONE drift={}", mc.player.position().distanceTo(initial));
            initial = mc.player.position();
            mc.options.keyUp.setDown(true);
        }
        if (worldTicks == 260) {
            mc.options.keyUp.setDown(false);
            LOG.info("TOE_M001_1 VANILLA_MOVEMENT distance={}", mc.player.position().distanceTo(initial));
            mc.options.pauseOnLostFocus = oldPause;
            mc.options.fov().set(oldFov);
            mc.options.setCameraType(oldCamera);
            finished = true;
        }
    }
}
