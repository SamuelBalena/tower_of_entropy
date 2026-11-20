package com.towerofentropy.combat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import com.towerofentropy.animation.*;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Explicit, disposable integrated-server fixture only. Never active in ordinary runClient. */
@Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT)
public final class ClientCombatSmokeTest {
    private static final String[] CASES={"no_sword","iron_front","out_of_range","wall","behind","outside_arc",
        "inside_arc","two_targets","passive_and_player","spam","invulnerability","normal_death","off_before_impact",
        "weapon_swap","jump_cancel","visual_command","roll_busy","combat_off_visual","rotated_yaw","vertical_range",
        "impact_direction_changed","impact_target_moved","skeleton","alternate_weapon","offhand_only",
        "armor","animation_off","roll_damage_free","jog_baseline","jog_attack"};
    private static final List<Map<String,Object>> RESULTS=new CopyOnWriteArrayList<>();
    private static final List<Mob> TARGETS=new ArrayList<>(); // server thread only
    private static int titleTicks,ticks,visit,quitTicks;
    private static boolean opening,finished,initialized,oldPause,oldHide;
    private static CameraType oldCamera;
    private static double beforeX,beforeZ;
    private static double baselineDistance;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        boolean restart=Boolean.getBoolean("toe.combat.restart");
        if(event.phase!=TickEvent.Phase.END||(!restart&&!Boolean.getBoolean("toe.combat.test")))return;
        Minecraft mc=Minecraft.getInstance();
        if(finished){if(++quitTicks==50)mc.stop();return;}
        if(!initialized){oldPause=mc.options.pauseOnLostFocus;oldHide=mc.options.hideGui;oldCamera=mc.options.getCameraType();mc.options.pauseOnLostFocus=false;initialized=true;}
        if(!opening&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null&&++titleTicks>=100){
            if(!Files.isRegularFile(mc.gameDirectory.toPath().resolve("saves/New World/level.dat"))){record("fixture_exists",false,"New World missing; nothing modified");finish(mc);return;}
            opening=true;mc.createWorldOpenFlows().loadLevel(mc.screen,"New World");
        }
        if(!opening||mc.player==null||mc.level==null||mc.getOverlay()!=null)return;
        ++ticks;
        if(restart||visit>0){
            if(ticks==80)server(mc,p->{record(restart?"restart_disabled":"reconnect_disabled",CombatController.status(p.getUUID())==null&&LocomotionController.status(p.getUUID())==null,"combat and locomotion sessions absent");});
            if(ticks==100)finish(mc);
            return;
        }
        if(ticks==20){mc.setScreen(null);mc.options.hideGui=true;mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);server(mc,p->{
            record("default_disabled",CombatController.status(p.getUUID())==null,"mode off on fresh login");
            var root=p.server.getCommands().getDispatcher().getRoot();
            var anim=root.getChild("toe_animtest");
            record("legacy_commands",java.util.stream.Stream.of("idle","idle_steve","auto","auto_steve","off","roll","sword").allMatch(n->anim.getChild(n)!=null),"all seven animation commands registered");
            record("combat_commands",java.util.stream.Stream.of("on","off","status").allMatch(n->root.getChild("toe_combat").getChild(n)!=null),"operator-only command tree");
            int denied=p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(0).withSuppressedOutput(),"toe_combat on");
            record("operator_guard",denied==0&&CombatController.status(p.getUUID())==null,"unprivileged command refused");
            try{fixture(p,"iron_front");LocomotionController.enable(p,true);CombatController.enable(p);}catch(Exception ex){record("prepare",false,ex.toString());}
        });}
        if(ticks<160)return;
        int index=(ticks-160)/80,phase=(ticks-160)%80;
        if(index>=CASES.length){
            if(phase==0)server(mc,p->{try{fixture(p,"iron_front");CombatController.enable(p);}catch(Exception ex){record("reconnect_setup",false,ex.toString());}});
            if(phase==20)KeyMapping.click(ActionKeys.SWORD.getKey());
            if(phase==23){mc.level.disconnect();mc.clearLevel(new TitleScreen());opening=false;titleTicks=0;ticks=0;visit=1;}
            return;
        }
        String name=CASES[index];
        mc.options.keyUp.setDown((name.equals("jog_baseline")||name.equals("jog_attack"))&&phase>=10&&phase<50);
        if(phase==0)server(mc,p->{try{fixture(p,name);}catch(Exception ex){record(name+"_setup",false,ex.toString());}});
        if(phase==18&&name.equals("jog_attack"))server(mc,p->TARGETS.get(0).teleportTo(1.6,100,p.getZ()+3.2));
        if(phase==14&&name.equals("roll_busy"))KeyMapping.click(ActionKeys.ROLL.getKey());
        if(phase==20){
            if(name.equals("jog_baseline")) { /* reference: no action */ }
            else if(name.equals("roll_damage_free"))KeyMapping.click(ActionKeys.ROLL.getKey());
            else if(name.equals("visual_command"))mc.player.connection.sendCommand("toe_animtest sword");
            else KeyMapping.click(ActionKeys.SWORD.getKey());
        }
        if(name.equals("spam")&&phase>20&&phase<28)KeyMapping.click(ActionKeys.SWORD.getKey());
        if(phase==22)server(mc,p->{
            if(name.equals("iron_front"))record("no_early_damage",Math.abs(TARGETS.get(0).getHealth()-20)<.001,"health="+TARGETS.get(0).getHealth());
            if(name.equals("off_before_impact"))command(p,"toe_combat off");
            if(name.equals("animation_off"))command(p,"toe_animtest off");
            if(name.equals("weapon_swap"))p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            if(name.equals("jump_cancel")){p.setOnGround(false);p.setDeltaMovement(0,.42,0);p.hurtMarked=true;}
            if(name.equals("impact_direction_changed"))p.connection.teleport(p.getX(),p.getY(),p.getZ(),180,0);
            if(name.equals("impact_target_moved"))TARGETS.get(0).teleportTo(.5,100,8.5);
            if(name.equals("invulnerability"))TARGETS.get(0).hurt(p.damageSources().generic(),4);
        });
        if(phase==23&&name.equals("iron_front")){
            record("cpm_sword_visible",ToeCpmPlugin.client.getAnimationPlaying("toe_sword")==1,"CPM client gesture parameter");shot(mc,"sword-before-impact");
        }
        if(phase==28&&(name.equals("iron_front")||name.equals("normal_death")||name.equals("roll_busy")))shot(mc,name);
        if(phase==36&&name.equals("jog_attack"))server(mc,p->{var s=LocomotionController.status(p.getUUID());record("attack_returns_jog",s!=null&&s.state()==LocomotionStateMachine.State.JOG,"status="+s);});
        if(phase==60)server(mc,p->verify(p,name));
    }
    private static void fixture(ServerPlayer p,String name) throws Exception {
        CombatController.disable(p);LocomotionController.cancelAction(p);
        if(LocomotionController.status(p.getUUID())==null)LocomotionController.enable(p,true);
        var level=p.serverLevel();
        for(var e:level.getEntities((Entity)null,new AABB(-12,90,-12,12,115,28)))if(!(e instanceof net.minecraft.world.entity.player.Player))e.discard();
        TARGETS.clear();
        for(int x=-6;x<=6;x++)for(int z=-7;z<=24;z++){
            level.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.STONE.defaultBlockState());
            for(int y=100;y<=106;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
        }
        level.setDayTime(18000); // Hostiles do not burn; test-only world edit.
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,10000,0,false,false));
        p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=false;
        p.teleportTo(level,.5,100,.5,0,0);p.setDeltaMovement(0,0,0);p.setOnGround(true);p.fallDistance=0;
        p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
        if(name.equals("no_sword"))p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
        if(name.equals("alternate_weapon"))p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));
        if(name.equals("offhand_only")){p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.IRON_SWORD));}
        double x=.5,y=100,z=2.5;
        // A physical dash now reaches the old NPC position. Keep this target within
        // sword reach/arc but off the dash corridor, isolating action/damage assertions
        // from vanilla entity pushing after recovery. Terrain collisions have their own suite.
        if(name.equals("roll_busy")||name.equals("roll_damage_free")){x=1.5;z=3.0;}
        if(name.equals("out_of_range"))z=4.1;
        if(name.equals("behind"))z=-1.5;
        if(name.equals("outside_arc")){x=2.5;z=1;}
        if(name.equals("inside_arc")){x=1.8;z=2;}
        if(name.equals("rotated_yaw")){p.connection.teleport(.5,100,.5,90,0);x=-1.5;z=.5;}
        if(name.equals("vertical_range"))y=104;
        if(name.equals("jog_baseline")||name.equals("jog_attack")){x=4.5;z=6.5;}
        Mob target=spawn(p,name.equals("passive_and_player")?EntityType.COW:name.equals("skeleton")?EntityType.SKELETON:EntityType.ZOMBIE,x,y,z);
        if(name.equals("normal_death"))target.setHealth(3);
        if(name.equals("armor"))target.getAttribute(Attributes.ARMOR).setBaseValue(10);
        if(name.equals("two_targets"))spawn(p,EntityType.ZOMBIE,1.2,100,2.8);
        if(name.equals("wall"))for(int bx=-1;bx<=1;bx++)for(int by=100;by<=103;by++)level.setBlockAndUpdate(new BlockPos(bx,by,1),Blocks.STONE.defaultBlockState());
        if(name.equals("passive_and_player"))record("player_ineligible",!SwordTargeting.eligible(p),"real ServerPlayer remains excluded by target predicate");
        if(!name.equals("combat_off_visual"))command(p,"toe_combat on");
        command(p,"toe_combat status");
        beforeX=p.getX();beforeZ=p.getZ();
    }
    private static Mob spawn(ServerPlayer p,EntityType<? extends Mob> type,double x,double y,double z){
        var mob=type.create(p.serverLevel());mob.moveTo(x,y,z,180,0);mob.setNoAi(true);mob.setPersistenceRequired();
        if(mob.getAttribute(Attributes.ARMOR)!=null)mob.getAttribute(Attributes.ARMOR).setBaseValue(0);
        for(var slot:EquipmentSlot.values())mob.setItemSlot(slot,ItemStack.EMPTY);
        p.serverLevel().addFreshEntity(mob);TARGETS.add(mob);return mob;
    }
    private static void verify(ServerPlayer p,String name){
        var s=CombatController.status(p.getUUID());
        var loc=LocomotionController.status(p.getUUID());
        boolean expectsHit=java.util.Set.of("iron_front","inside_arc","two_targets","spam","normal_death","rotated_yaw","skeleton","armor","jog_attack","passive_and_player").contains(name);
        boolean rejected=java.util.Set.of("no_sword","alternate_weapon","offhand_only","roll_busy").contains(name);
        boolean visualOnly=java.util.Set.of("combat_off_visual","visual_command","roll_damage_free","jog_baseline").contains(name);
        boolean cancelled=java.util.Set.of("off_before_impact","weapon_swap","jump_cancel","animation_off").contains(name);
        float health=TARGETS.get(0).getHealth();
        float expected=name.equals("normal_death")?0:name.equals("armor")?17.28f:name.equals("passive_and_player")?6:expectsHit||name.equals("invulnerability")?16:20;
        record(name+"_health",Math.abs(health-expected)<.001,"health="+health+" expected="+expected+" status="+s);
        if(expectsHit)record(name+"_once",s!=null&&s.accepted()==1&&s.impacts()==1&&s.hits()==1,"status="+s);
        if(!expectsHit&&!cancelled&&!visualOnly&&!rejected)record(name+"_miss",s!=null&&s.impacts()==1&&s.hits()==0,"status="+s);
        if(rejected)record(name+"_rejected",s!=null&&s.accepted()==0&&s.impacts()==0,"status="+s);
        if(visualOnly)record(name+"_no_combat",s==null||s.accepted()==0,"status="+s);
        if(cancelled)record(name+"_cancelled",s==null||s.impacts()==0,"status="+s);
        if(name.equals("two_targets"))record("single_target_only",TARGETS.get(1).getHealth()==20,"second health="+TARGETS.get(1).getHealth());
        if(name.equals("iron_front"))record("impact_four_ticks",s!=null&&s.impactTick()-s.startedTick()==4,"status="+s);
        if(name.equals("normal_death"))record("vanilla_death",TARGETS.get(0).isDeadOrDying(),"normal LivingEntity death");
        record(name+"_returns_idle",(name.equals("animation_off")?loc==null:loc!=null&&loc.state()==LocomotionStateMachine.State.IDLE)&&(s==null||!s.attacking()),"locomotion="+loc);
        double distance=Math.hypot(p.getX()-beforeX,p.getZ()-beforeZ);
        if(name.equals("jog_baseline")){baselineDistance=distance;record("jog_baseline_moved",distance>5,"distance="+distance);}
        else if(name.equals("jog_attack"))record("jog_attack_vanilla_distance",Math.abs(distance-baselineDistance)<.001,"baseline="+baselineDistance+" attack="+distance);
        else if(name.equals("roll_busy")||name.equals("roll_damage_free"))record(name+"_physical_distance",Math.abs(distance-RollRules.DISTANCE)<.08,"M002.1 replaces the previous no-displacement assertion; distance="+distance);
        else record(name+"_no_player_displacement",distance<1e-6,"horizontal drift="+distance);
    }
    private static void command(ServerPlayer p,String command){p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withSuppressedOutput(),command);}
    private static void server(Minecraft mc,Consumer<ServerPlayer> action){
        var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
        server.execute(()->{try{var p=server.getPlayerList().getPlayer(id);if(p!=null)action.accept(p);}catch(Exception ex){record("runtime_exception",false,ex.toString());LogUtils.getLogger().error("TOE_M002_0 TEST_EXCEPTION",ex);}});
    }
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"toe-m002_0-"+name+".png",mc.getMainRenderTarget(),msg->{});}
    private static void record(String name,boolean pass,String detail){var row=new LinkedHashMap<String,Object>();row.put("test",name);row.put("pass",pass);row.put("detail",detail);RESULTS.add(row);LogUtils.getLogger().info("TOE_M002_0 CHECK {} pass={} {}",name,pass,detail);}
    private static void finish(Minecraft mc){
        mc.options.keyUp.setDown(false);
        mc.options.pauseOnLostFocus=oldPause;mc.options.hideGui=oldHide;mc.options.setCameraType(oldCamera);
        try{String name=Boolean.getBoolean("toe.combat.restart")?"restart":"runtime";Files.writeString(Path.of("../.logs/m002_0-"+name+"-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(RESULTS));}catch(Exception ex){LogUtils.getLogger().error("TOE_M002_0 TEST_WRITE",ex);}
        LogUtils.getLogger().info("TOE_M002_0 TEST_DONE passed={} total={}",RESULTS.stream().filter(r->Boolean.TRUE.equals(r.get("pass"))).count(),RESULTS.size());
        finished=true;
    }
}
