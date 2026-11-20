package com.towerofentropy.animation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import com.tom.cpm.shared.MinecraftClientAccess;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import static com.towerofentropy.animation.LocomotionStateMachine.State.*;

/** Explicit diagnostic only, in the M001.1 isolated New World save. Never enabled by normal launch. */
@Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT)
public final class ClientLocomotionSmokeTest {
    private static int titleTicks, ticks, visit;
    private static boolean opening, finished, initialized, oldPause;
    private static int oldFov;
    private static CameraType oldCamera;
    private static Vec3 movementStart;
    private static double baselineDistance;
    private static float oldYaw, oldPitch;
    private static final List<Map<String,Object>> RESULTS = new ArrayList<>();
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        boolean restart = Boolean.getBoolean("toe.locomotion.restart");
        if (event.phase != TickEvent.Phase.END || finished || (!restart && !Boolean.getBoolean("toe.locomotion.test"))) return;
        Minecraft mc = Minecraft.getInstance();
        if (!initialized) {
            oldPause=mc.options.pauseOnLostFocus; oldFov=mc.options.fov().get(); oldCamera=mc.options.getCameraType();
            mc.options.pauseOnLostFocus=false; initialized=true;
        }
        if (!opening && mc.screen instanceof TitleScreen && mc.getOverlay()==null && ++titleTicks>=150) {
            if (!mc.gameDirectory.toPath().resolve("saves/New World/level.dat").toFile().isFile()) {
                LogUtils.getLogger().error("TOE_M001_2 TEST_ABORT no isolated test world"); finished=true; restore(mc); return;
            }
            opening=true; mc.createWorldOpenFlows().loadLevel(mc.screen,"New World");
        }
        if (!opening || mc.player==null || mc.level==null || mc.getOverlay()!=null) return;
        ++ticks;
        if (ticks==30) {
            oldYaw=mc.player.getYRot(); oldPitch=mc.player.getXRot();
            mc.setScreen(null); mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); mc.options.fov().set(55);
        }
        // Test fixture only: keep input aligned with the cleared +Z lane, regardless of saved camera angle.
        if(ticks>=30) { mc.player.setYRot(0); mc.player.setXRot(0); mc.options.keyAttack.setDown(false); mc.options.keyUse.setDown(false); }
        if (restart) {
            if (ticks==100) { check(mc,"client_restart_disabled",null); screenshot(mc,"restart-vanilla"); finish(mc); }
            return;
        }
        if (visit==1) {
            if(ticks==100) { check(mc,"reconnect_disabled",null); command(mc,"toe_animtest idle_steve"); }
            if(ticks==140) { screenshot(mc,"legacy-idle"); record("legacy_idle_loaded",currentDefinitionLoaded(),"M001.1 command still renders"); command(mc,"toe_animtest off"); }
            if(ticks==170) { check(mc,"legacy_off",null); finish(mc); }
            return;
        }
        if(ticks==40) {
            // Clear only this disposable dev world's test lane; no physics/attribute changes.
            command(mc,"fill ~-12 ~-1 ~-8 ~12 ~-1 ~48 minecraft:stone");
            command(mc,"fill ~-12 ~ ~-8 ~12 ~6 ~48 minecraft:air");
            command(mc,"toe_animtest off");
        }
        boolean forward = ticks>=100&&ticks<140 || ticks>=250&&ticks<290 || ticks>=330&&ticks<365
            || ticks>=450&&ticks<520 || ticks>=550&&ticks<580;
        mc.options.keyUp.setDown(forward);
        mc.options.keyLeft.setDown(ticks>=390&&ticks<420);
        mc.options.keyDown.setDown(ticks>=420&&ticks<450);
        mc.options.keyJump.setDown(ticks>=500&&ticks<520);
        mc.options.keyShift.setDown(ticks>=550&&ticks<580);
        if(ticks==100 || ticks==250) movementStart=mc.player.position();
        if(ticks==140) { baselineDistance=mc.player.position().distanceTo(movementStart); record("baseline_walk",baselineDistance>0,"distance="+baselineDistance); }
        if(ticks==170) command(mc,"toe_animtest auto_steve");
        if(ticks==230) { check(mc,"idle",IDLE); screenshot(mc,"idle"); }
        if(ticks==275 || ticks==285) { check(mc,"jog_"+ticks,JOG); screenshot(mc,"jog-"+ticks); }
        if(ticks==290) {
            double actual=mc.player.position().distanceTo(movementStart);
            record("physics_distance",Math.abs(actual-baselineDistance)<0.001,"baseline="+baselineDistance+" auto="+actual);
        }
        if(ticks==320) { check(mc,"stop_to_idle",IDLE); screenshot(mc,"stop"); }
        if(ticks==355) check(mc,"repeat_jog",JOG);
        if(ticks==385) check(mc,"repeat_idle",IDLE);
        if(ticks==410) { check(mc,"strafe_fallback",VANILLA_FALLBACK); screenshot(mc,"strafe-fallback"); }
        if(ticks==440) check(mc,"reverse_fallback",VANILLA_FALLBACK);
        if(ticks==490) check(mc,"forward_resume",JOG);
        if(ticks==507 || ticks==514) {
            check(mc,"air_"+ticks,VANILLA_FALLBACK);
            record("airborne_"+ticks,!mc.player.onGround(),"dy="+mc.player.getDeltaMovement().y);
            screenshot(mc,"air-"+ticks);
        }
        if(ticks==570) { check(mc,"crouch_fallback",VANILLA_FALLBACK); screenshot(mc,"crouch-fallback"); }
        if(ticks==615) check(mc,"landed_idle",IDLE);
        if(ticks==620) command(mc,"toe_animtest off");
        if(ticks==650) { check(mc,"off",null); screenshot(mc,"off"); }
        if(ticks==670) command(mc,"toe_animtest auto");
        if(ticks==710) { check(mc,"classic_player_skin",IDLE); screenshot(mc,"player-skin"); }
        if(ticks==730) {
            mc.level.disconnect(); mc.clearLevel(new TitleScreen());
            opening=false; titleTicks=0; ticks=0; visit=1;
        }
    }
    private static void command(Minecraft mc,String command) { mc.player.connection.sendCommand(command); }
    private static void screenshot(Minecraft mc,String name) {
        Screenshot.grab(mc.gameDirectory,"toe-m001_2-"+name+".png",mc.getMainRenderTarget(),msg->LogUtils.getLogger().info("TOE_M001_2 SCREENSHOT {}",msg.getString()));
    }
    private static boolean currentDefinitionLoaded() {
        var p=MinecraftClientAccess.get().getCurrentClientPlayer();
        return p!=null && p.getModelDefinition()!=null;
    }
    private static void check(Minecraft mc,String label,LocomotionStateMachine.State expected) {
        var status=LocomotionController.status(mc.player.getUUID());
        var p=MinecraftClientAccess.get().getCurrentClientPlayer();
        var def=p==null?null:p.getModelDefinition();
        String pose=p==null?"null":String.valueOf(p.persistentState.currentPose);
        boolean hasAutoModel=def!=null && def.getAnimations().getCommandActionsMap().containsKey("toe_jog");
        boolean pass=expected==null ? status==null&&!hasAutoModel : status!=null&&status.state()==expected;
        if(expected==IDLE || expected==JOG) pass &= hasAutoModel && pose.contains(expected==IDLE?"toe_idle":"toe_jog");
        if(expected==VANILLA_FALLBACK) pass &= !hasAutoModel;
        record(label,pass,"status="+status+" pose="+pose+" hasAutoModel="+hasAutoModel+" position="+mc.player.position());
    }
    private static void record(String label,boolean pass,String detail) {
        Map<String,Object> row=new LinkedHashMap<>();row.put("test",label);row.put("pass",pass);row.put("detail",detail);RESULTS.add(row);
        LogUtils.getLogger().info("TOE_M001_2 CHECK {} pass={} {}",label,pass,detail);
    }
    private static void restore(Minecraft mc) {
        mc.options.keyUp.setDown(false);mc.options.keyDown.setDown(false);mc.options.keyLeft.setDown(false);
        mc.options.keyJump.setDown(false);mc.options.keyShift.setDown(false);
        mc.options.pauseOnLostFocus=oldPause;mc.options.fov().set(oldFov);mc.options.setCameraType(oldCamera);
        if(mc.player!=null){mc.player.setYRot(oldYaw);mc.player.setXRot(oldPitch);}
    }
    private static void finish(Minecraft mc) {
        restore(mc);finished=true;
        try {
            String name=Boolean.getBoolean("toe.locomotion.restart")?"restart":"smoke";
            Files.writeString(Path.of("../.logs/m001_2-"+name+"-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(RESULTS));
        } catch(Exception ex) { LogUtils.getLogger().error("TOE_M001_2 RESULT_WRITE_FAILED",ex); }
        LogUtils.getLogger().info("TOE_M001_2 TEST_DONE passed={} total={}",RESULTS.stream().filter(r->Boolean.TRUE.equals(r.get("pass"))).count(),RESULTS.size());
    }
}
