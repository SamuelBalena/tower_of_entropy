package com.towerofentropy.animation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import com.tom.cpm.shared.MinecraftClientAccess;
import com.tom.cpm.shared.model.PlayerModelParts;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import static com.towerofentropy.animation.LocomotionStateMachine.State.*;

/** Opt-in validation in the disposable New World save. Not a gameplay controller. */
@Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT)
public final class ClientActionSmokeTest {
    private static int titleTicks,ticks,visit;
    private static boolean initialized,opening,finished,oldPause,oldHideGui;
    private static int oldFov,oldSlot;
    private static float oldYaw,oldPitch;
    private static CameraType oldCamera;
    private static InputConstants.Key oldRollKey;
    private static Vec3 start,rollStart;
    private static double baseline;
    private static final List<Map<String,Object>> RESULTS=new ArrayList<>();
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        boolean restart=Boolean.getBoolean("toe.actions.restart");
        if(event.phase!=TickEvent.Phase.END||finished||(!restart&&!Boolean.getBoolean("toe.actions.test")))return;
        Minecraft mc=Minecraft.getInstance();
        // Observe the original M001.2 suite in a fresh process without taking over its camera/inputs.
        if(restart&&Boolean.getBoolean("toe.locomotion.test")){
            if(mc.player!=null&&mc.level!=null&&mc.getOverlay()==null){
                ++ticks;
                if(ticks==40){cmd(mc,"fill ~-8 ~-1 ~-8 ~8 ~-1 ~48 minecraft:stone");cmd(mc,"fill ~-8 ~ ~-8 ~8 ~31 ~48 minecraft:air");isolateFixture(mc);}
                if(ticks==100){check(mc,"restart_disabled",null);finished=true;saveResults();}
            }
            return;
        }
        if(!initialized){oldPause=mc.options.pauseOnLostFocus;oldHideGui=mc.options.hideGui;oldFov=mc.options.fov().get();oldCamera=mc.options.getCameraType();oldRollKey=ActionKeys.ROLL.getKey();mc.options.pauseOnLostFocus=false;initialized=true;}
        if(!opening&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null&&++titleTicks>=150){
            if(!mc.gameDirectory.toPath().resolve("saves/New World/level.dat").toFile().isFile()){LogUtils.getLogger().error("TOE_M001_3 TEST_ABORT no isolated save");finish(mc);return;}
            opening=true;mc.createWorldOpenFlows().loadLevel(mc.screen,"New World");
        }
        if(!opening||mc.player==null||mc.level==null||mc.getOverlay()!=null)return;
        ++ticks;
        if(ticks==30){oldYaw=mc.player.getYRot();oldPitch=mc.player.getXRot();oldSlot=mc.player.getInventory().selected;mc.setScreen(null);mc.options.hideGui=true;mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);mc.options.fov().set(55);mc.player.getInventory().selected=8;}
        if(ticks>=30){mc.player.setYRot(0);mc.player.setXRot(0);mc.options.keyAttack.setDown(false);mc.options.keyUse.setDown(false);}
        if(restart){if(ticks==100){check(mc,"restart_disabled",null);shot(mc,"restart");finish(mc);}return;}
        if(visit==1){if(ticks==100){check(mc,"reconnect_disabled",null);finish(mc);}return;}
        if(ticks==40){cmd(mc,"time set day");cmd(mc,"weather clear");cmd(mc,"fill ~-8 ~-1 ~-8 ~8 ~-1 ~48 minecraft:stone");cmd(mc,"fill ~-8 ~ ~-8 ~8 ~31 ~48 minecraft:air");isolateFixture(mc);cmd(mc,"toe_animtest off");}
        mc.options.keyUp.setDown(ticks>=450&&ticks<490||ticks>=520&&ticks<590||ticks>=620&&ticks<660);
        mc.options.keyJump.setDown(ticks>=845&&ticks<856);
        ActionKeys.ROLL.setDown(ticks>=230&&ticks<267);
        if(ticks==170)cmd(mc,"toe_animtest auto_steve");
        if(ticks==215){check(mc,"initial_idle",IDLE);record("key_registration",ActionKeys.ROLL.getDefaultKey().getValue()==GLFW.GLFW_KEY_V&&ActionKeys.SWORD.getDefaultKey().getValue()==GLFW.GLFW_KEY_J&&ActionKeys.ROLL.getCategory().equals("key.categories.towerofentropy"),"Forge mappings V/J, Tower of Entropy category");}
        if(ticks==230){rollStart=mc.player.position();ActionKeys.ROLL.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_B));KeyMapping.resetMapping();KeyMapping.click(ActionKeys.ROLL.getKey());}
        if(ticks==240||ticks==250)KeyMapping.click(ActionKeys.ROLL.getKey()); // Synthetic repeat while held: must be suppressed.
        if(ticks==236||ticks==246||ticks==256){check(mc,"roll_key_"+ticks,ROLL);shot(mc,"roll-"+ticks);}
        if(ticks==245)cmd(mc,"toe_animtest roll");
        if(ticks==275){ActionKeys.ROLL.setKey(oldRollKey);KeyMapping.resetMapping();check(mc,"roll_to_idle",IDLE);record("stationary_roll_no_displacement",mc.player.position().distanceTo(rollStart)<1e-6,"drift="+mc.player.position().distanceTo(rollStart));record("roll_one_start",commands(mc)==1,"commands="+commands(mc));}
        if(ticks==300)mc.setScreen(new ChatScreen(""));
        if(ticks==301)KeyMapping.click(ActionKeys.SWORD.getKey());
        if(ticks==310){mc.setScreen(null);record("chat_suppressed",commands(mc)==1,"commands="+commands(mc));}
        if(ticks==320)mc.setScreen(new InventoryScreen(mc.player));
        if(ticks==321)KeyMapping.click(ActionKeys.ROLL.getKey());
        if(ticks==325)mc.setScreen(null);
        if(ticks==330)mc.setScreen(new KeyBindsScreen(new TitleScreen(),mc.options));
        if(ticks==332){
            try{for(var f:KeyBindsScreen.class.getDeclaredFields())if(f.getType().getSimpleName().equals("KeyBindsList")){f.setAccessible(true);((net.minecraft.client.gui.components.AbstractSelectionList<?>)f.get(mc.screen)).setScrollAmount(10000);}}catch(Exception ex){LogUtils.getLogger().info("TOE_M001_3 controls screenshot scroll unavailable",ex);}
            KeyMapping.click(ActionKeys.SWORD.getKey());
        }
        if(ticks==335)shot(mc,"controls");
        if(ticks==340){mc.setScreen(null);record("menus_suppressed",commands(mc)==1,"commands="+commands(mc));}
        if(ticks==345)cmd(mc,"item replace entity @s weapon.mainhand with minecraft:iron_sword");
        if(ticks==350){start=mc.player.position();cmd(mc,"toe_animtest sword");}
        if(ticks==351)cmd(mc,"toe_animtest sword");
        if(ticks==352)cmd(mc,"toe_animtest roll");
        if(ticks==354||ticks==358){check(mc,"sword_"+ticks,SWORD_ATTACK);record("iron_sword_"+ticks,mc.player.getMainHandItem().is(Items.IRON_SWORD),mc.player.getMainHandItem().toString());shot(mc,"sword-"+ticks);}
        if(ticks==370){check(mc,"sword_to_idle",IDLE);record("stationary_sword_no_displacement",mc.player.position().distanceTo(start)<1e-6,"drift="+mc.player.position().distanceTo(start));record("sword_first_wins",commands(mc)==2,"commands="+commands(mc));}
        if(ticks==390)KeyMapping.click(ActionKeys.ROLL.getKey());
        if(ticks==395)cmd(mc,"toe_animtest sword");
        if(ticks==402){check(mc,"roll_rejects_sword",ROLL);record("roll_first_wins",commands(mc)==3,"commands="+commands(mc));shot(mc,"roll-sword-equipped");}
        if(ticks==435)check(mc,"second_roll_returns",IDLE);
        if(ticks==450||ticks==520||ticks==620)start=mc.player.position();
        if(ticks==490){baseline=mc.player.position().distanceTo(start);record("baseline_forward",baseline>0,"distance="+baseline);}
        if(ticks==535)cmd(mc,"toe_animtest roll");
        if(ticks==545){check(mc,"jog_to_roll",ROLL);shot(mc,"moving-roll");}
        if(ticks==560){double d=mc.player.position().distanceTo(start);record("roll_forward_physics",Math.abs(d-baseline)<.001,"baseline="+baseline+" action="+d);}
        if(ticks==580)check(mc,"roll_to_jog",JOG);
        if(ticks==610)check(mc,"moving_roll_stop",IDLE);
        if(ticks==630)cmd(mc,"toe_animtest sword");
        if(ticks==635){check(mc,"jog_to_sword",SWORD_ATTACK);shot(mc,"moving-sword");}
        if(ticks==650)check(mc,"sword_to_jog",JOG);
        if(ticks==660){double d=mc.player.position().distanceTo(start);record("sword_forward_physics",Math.abs(d-baseline)<.001,"baseline="+baseline+" action="+d);}
        if(ticks==680)cmd(mc,"item replace entity @s weapon.mainhand with minecraft:air");
        if(ticks==710)KeyMapping.click(ActionKeys.SWORD.getKey());
        if(ticks==715){check(mc,"sword_key_empty_hand",SWORD_ATTACK);record("unequipped",mc.player.getMainHandItem().isEmpty(),"main hand empty");shot(mc,"sword-empty");}
        if(ticks==738)check(mc,"empty_sword_returns",IDLE);
        if(ticks==750)cmd(mc,"toe_animtest roll");
        if(ticks==757)check(mc,"before_off_active",ROLL);
        if(ticks==760)cmd(mc,"toe_animtest off");
        if(ticks==770){check(mc,"off_mid_action",null);shot(mc,"off");}
        if(ticks==790)cmd(mc,"toe_animtest auto_steve");
        if(ticks==835)cmd(mc,"toe_animtest roll");
        if(ticks==842)check(mc,"before_jump_active",ROLL);
        if(ticks==850){check(mc,"jump_cancels_action",VANILLA_FALLBACK);shot(mc,"jump-fallback");}
        if(ticks==900)check(mc,"landing_clears_residue",IDLE);
        if(ticks==910)cmd(mc,"toe_animtest roll");
        if(ticks==915){mc.level.disconnect();mc.clearLevel(new TitleScreen());opening=false;titleTicks=0;ticks=0;visit=1;}
    }
    private static int commands(Minecraft mc){var s=LocomotionController.status(mc.player.getUUID());return s==null?-1:s.actionCommands();}
    private static void isolateFixture(Minecraft mc){
        // Disposable test world only: remove non-player entities without damage, drops or player-stat writes.
        var server=mc.getSingleplayerServer();var center=mc.player.position();
        if(server!=null)server.execute(()->{
            var entities=java.util.stream.StreamSupport.stream(server.overworld().getAllEntities().spliterator(),false)
                .filter(e->!(e instanceof net.minecraft.world.entity.player.Player)&&e.distanceToSqr(center)<128*128).toList();
            entities.forEach(net.minecraft.world.entity.Entity::discard);
            LogUtils.getLogger().info("TOE_M001_3 TEST_FIXTURE isolated {} non-player entities",entities.size());
        });
    }
    private static void cmd(Minecraft mc,String c){mc.player.connection.sendCommand(c);}
    private static void shot(Minecraft mc,String label){Screenshot.grab(mc.gameDirectory,"toe-m001_3-"+label+".png",mc.getMainRenderTarget(),msg->LogUtils.getLogger().info("TOE_M001_3 SCREENSHOT {}",msg.getString()));}
    private static void check(Minecraft mc,String label,LocomotionStateMachine.State expected){
        var status=LocomotionController.status(mc.player.getUUID());var player=MinecraftClientAccess.get().getCurrentClientPlayer();var def=player==null?null:player.getModelDefinition();
        boolean custom=def!=null&&def.getAnimations().getCommandActionsMap().containsKey("toe_roll");
        int roll=ToeCpmPlugin.client.getAnimationPlaying("toe_roll"),sword=ToeCpmPlugin.client.getAnimationPlaying("toe_sword");
        boolean pass=expected==null?status==null&&!custom:status!=null&&status.state()==expected;
        if(expected==ROLL)pass&=custom&&roll==1&&sword!=1;
        if(expected==SWORD_ATTACK)pass&=custom&&sword==1&&roll!=1;
        if(expected==IDLE||expected==JOG)pass&=custom&&roll!=1&&sword!=1;
        if(expected==VANILLA_FALLBACK)pass&=!custom;
        String rotation=def==null?"none":String.valueOf(def.getModelElementFor(PlayerModelParts.BODY).getMainRoot().rotation.asVec3f(true));
        record(label,pass,"status="+status+" roll="+roll+" sword="+sword+" body="+rotation+" pos="+mc.player.position());
    }
    private static void record(String label,boolean pass,String detail){var row=new LinkedHashMap<String,Object>();row.put("test",label);row.put("pass",pass);row.put("detail",detail);RESULTS.add(row);LogUtils.getLogger().info("TOE_M001_3 CHECK {} pass={} {}",label,pass,detail);}
    private static void finish(Minecraft mc){
        mc.options.keyUp.setDown(false);mc.options.keyJump.setDown(false);ActionKeys.ROLL.setDown(false);ActionKeys.SWORD.setDown(false);
        ActionKeys.ROLL.setKey(oldRollKey);KeyMapping.resetMapping();
        mc.options.pauseOnLostFocus=oldPause;mc.options.hideGui=oldHideGui;mc.options.fov().set(oldFov);mc.options.setCameraType(oldCamera);
        if(mc.player!=null){mc.player.setYRot(oldYaw);mc.player.setXRot(oldPitch);mc.player.getInventory().selected=oldSlot;}
        finished=true;
        saveResults();
    }
    private static void saveResults(){
        try{String n=Boolean.getBoolean("toe.actions.restart")?"restart":"smoke";Files.writeString(Path.of("../.logs/m001_3-"+n+"-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(RESULTS));}catch(Exception ex){LogUtils.getLogger().error("TOE_M001_3 result write failed",ex);}
        LogUtils.getLogger().info("TOE_M001_3 TEST_DONE passed={} total={}",RESULTS.stream().filter(r->Boolean.TRUE.equals(r.get("pass"))).count(),RESULTS.size());
    }
}
