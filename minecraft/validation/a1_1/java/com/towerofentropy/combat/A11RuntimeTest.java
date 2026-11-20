package com.towerofentropy.combat;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.towerofentropy.animation.*;
import com.towerofentropy.client.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Isolated QA only. Uses vanilla KeyMapping click dispatch, the real network and renderer. */
@Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT)
public final class A11RuntimeTest {
    private static final Path REPORT=Path.of("../animation_pipeline/reports/m002_a1_1");
    private static final List<Map<String,Object>> CHECKS=new java.util.concurrent.CopyOnWriteArrayList<>();
    private static final List<Map<String,Object>> SERIES=new ArrayList<>();
    private static final String[] SCENES={"combo","heavy","combo_jog","pause","cancel_a","cancel_b","cancel_c","cancel_heavy"};
    private static final String[] ANGLES={"front","side","back"};
    private static final String[] TESTS={"equip","off","on","auto","swap_attack","unsupported","shield","block_use","mining","held_heavy","remap","death"};
    private static List<Map<String,Object>> frames;
    private static net.minecraft.world.entity.monster.Vindicator target;
    private static com.mojang.blaze3d.platform.InputConstants.Key attackKey,useKey;
    private static int title,ticks,visit,quit;
    private static boolean opening,done;
    private static long draws;
    public static float orbit;
    private static final int VISUAL_TICKS=SCENES.length*3*120;

    private static void check(String name,boolean pass,Object detail){
        CHECKS.add(Map.of("test",name,"pass",pass,"detail",String.valueOf(detail)));
        com.mojang.logging.LogUtils.getLogger().info("TOE_A11 {} pass={} {}",name,pass,detail);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!Boolean.getBoolean("toe.a1_1.test"))return;
        var mc=Minecraft.getInstance();
        if(done){if(++quit==35)mc.stop();return;}
        if(!opening&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null&&++title>60){
            opening=true;mc.options.pauseOnLostFocus=false;
            mc.createWorldOpenFlows().loadLevel(mc.screen,"TOE_A1_1");
        }
        if(mc.player==null||mc.level==null||mc.getOverlay()!=null)return;
        ++ticks;
        if(ticks==5){preview(mc);mc.setScreen(null);mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);mc.options.fov().set(50);mc.options.hideGui=true;}
        if((visit>0||Boolean.getBoolean("toe.a1_1.restart"))&&ticks==70){
            check(visit>0?"reconnect_auto":"restart_auto",ClientCombatState.mode==CombatController.Mode.AUTO&&PlayerRendererR3.enabled&&PlayerRendererR3.drawCalls>0,"AUTO and default R3 without commands");
            server(p->{check("lifecycle_server_auto",CombatController.mode(p.getUUID())==CombatController.Mode.AUTO,"no persisted ON/OFF override");var s=CombatController.status(p.getUUID());check("lifecycle_sword_activation",!CombatController.supportedWeapon(p)||(CombatController.enabled(p)&&s!=null&&!s.attacking()),"saved supported sword is activated automatically");});
        }
        if(visit>0||Boolean.getBoolean("toe.a1_1.restart")){if(ticks==85)finish();return;}
        if(ticks==65)check("initial_auto_without_commands",ClientCombatState.mode==CombatController.Mode.AUTO&&PlayerRendererR3.drawCalls>0,"before any diagnostic mode command");
        if(ticks<100)return;
        int elapsed=ticks-100;
        if(elapsed<VISUAL_TICKS){visual(mc,elapsed/120,elapsed%120);return;}
        orbit=0;
        int index=(elapsed-VISUAL_TICKS)/110,phase=(elapsed-VISUAL_TICKS)%110;
        if(index<TESTS.length){technical(mc,TESTS[index],phase);return;}
        if(phase==0){server(p->{try{CombatController.disable(p);p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));}catch(Exception ex){check("reconnect_setup",false,ex);}});}
        if(phase==15){mc.level.disconnect();mc.clearLevel(new TitleScreen());opening=false;title=0;ticks=0;visit=1;}
    }
    private static void visual(Minecraft mc,int index,int phase){
        String scene=SCENES[index/3],angle=ANGLES[index%3];orbit=(index%3)*90;
        if(phase==0){
            frames=new ArrayList<>();SERIES.add(Map.of("name",scene,"angle",angle,"frames",frames));
            try{Files.createDirectories(REPORT.resolve("screenshots/"+scene+"/"+angle));}catch(Exception ex){throw new RuntimeException(ex);}
            draws=PlayerRendererR3.drawCalls;
            server(p->{try{fixture(p,true);}catch(Exception ex){check(scene+"_fixture",false,ex);}});
        }
        if(phase==18)server(p->check(scene+"_"+angle+"_auto",CombatController.mode(p.getUUID())==CombatController.Mode.AUTO&&CombatController.enabled(p),"sword activates with no enable command"));
        if(phase==20)click(mc,scene.equals("heavy")||scene.equals("cancel_heavy"));
        if(phase==23&&(scene.equals("combo")||scene.equals("combo_jog")||scene.equals("cancel_b")||scene.equals("cancel_c")))click(mc,false);
        if(phase==26&&(scene.equals("combo")||scene.equals("combo_jog")||scene.equals("cancel_c")))click(mc,false);
        if(phase==65&&scene.equals("pause"))click(mc,false);
        if((scene.equals("cancel_a")&&phase==22)||(scene.equals("cancel_b")&&phase==31)||(scene.equals("cancel_c")&&phase==43)||(scene.equals("cancel_heavy")&&phase==28))KeyMapping.click(ActionKeys.ROLL.getKey());
        mc.options.keyUp.setDown(scene.equals("combo_jog")&&phase>=78&&phase<102);
        if(phase>=10&&phase<=100){
            String file=String.format(java.util.Locale.ROOT,"%s/%s/%03d.png",scene,angle,phase-10);
            Screenshot.grab(REPORT.toFile(),file,mc.getMainRenderTarget(),message->{});
            var c=PlayerRendererR3.controller();
            var row=new LinkedHashMap<String,Object>();row.put("src","screenshots/"+file);row.put("t",(phase-10)/20.0);row.put("tick",mc.level.getGameTime());
            row.put("clip",c==null?"none":c.getAnimationName());row.put("clipTime",c==null?-1:c.getAnimationTime());row.put("yaw",PlayerRendererR3.lastYaw);frames.add(row);
            var status=CombatController.status(mc.player.getUUID());if(status!=null)row.put("combat",status);
        }
        if(phase==108){
            check(scene+"_"+angle+"_renderer",PlayerRendererR3.errors==0&&PlayerRendererR3.drawCalls>draws,"draws="+(PlayerRendererR3.drawCalls-draws));
            server(p->{var s=CombatController.status(p.getUUID());int expected=scene.equals("combo")||scene.equals("combo_jog")?3:scene.equals("pause")?2:scene.equals("cancel_b")?2:scene.equals("cancel_c")?3:1;
                check(scene+"_"+angle+"_inputs",s!=null&&s.accepted()==expected,s);
                int impacts=scene.equals("cancel_a")||scene.equals("cancel_heavy")?0:scene.equals("cancel_b")?1:scene.equals("cancel_c")?2:expected;
                check(scene+"_"+angle+"_impacts",s!=null&&s.impacts()==impacts,s);
                check(scene+"_"+angle+"_recovery",s!=null&&!s.attacking()&&!RollController.busy(p.getUUID()),s);
            });
        }
    }
    private static void technical(Minecraft mc,String name,int phase){
        if(phase==0){
            release(mc);mc.setScreen(null);draws=PlayerRendererR3.drawCalls;
            server(p->{try{fixture(p,!name.equals("equip")&&!name.equals("unsupported")&&!name.equals("mining"));
                if(name.equals("unsupported")||name.equals("mining"))p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_PICKAXE));
                if(name.equals("shield"))p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));
                if(name.equals("block_use"))p.serverLevel().setBlockAndUpdate(new BlockPos(0,100,2),Blocks.CHEST.defaultBlockState());
                if(name.equals("mining"))p.serverLevel().setBlockAndUpdate(new BlockPos(0,100,2),Blocks.STONE.defaultBlockState());
                if(name.equals("off")||name.equals("on"))p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack(),"toe_combat "+name);
                if(name.equals("auto")){CombatController.disable(p);p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack(),"toe_combat auto");}
                if(name.equals("remap")||name.equals("held_heavy")||name.equals("unsupported")||name.equals("off"))spawnTarget(p);
            }catch(Exception ex){check(name+"_fixture",false,ex);}});
        }
        if(phase==18&&(name.equals("block_use")||name.equals("mining"))){mc.player.setXRot(20);mc.player.xRotO=20;}
        if(phase==20){
            if(name.equals("equip"))server(p->{p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));});
            else if(name.equals("remap")){attackKey=mc.options.keyAttack.getKey();useKey=mc.options.keyUse.getKey();mc.options.keyAttack.setKey(com.mojang.blaze3d.platform.InputConstants.getKey(org.lwjgl.glfw.GLFW.GLFW_KEY_F6,0));mc.options.keyUse.setKey(com.mojang.blaze3d.platform.InputConstants.getKey(org.lwjgl.glfw.GLFW.GLFW_KEY_F7,0));KeyMapping.resetMapping();click(mc,false);}
            else click(mc,name.equals("held_heavy")||name.equals("shield")||name.equals("block_use"));
        }
        if(phase==22&&name.equals("equip"))click(mc,false);
        if(phase==22&&name.equals("swap_attack"))server(p->p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.STICK)));
        if(phase==21&&name.equals("death"))server(ServerPlayer::kill);
        if(phase==45&&name.equals("death"))mc.player.respawn();
        if(phase==40&&name.equals("remap"))click(mc,true);
        mc.options.keyUse.setDown((name.equals("held_heavy")&&phase>=20&&phase<85)||(name.equals("shield")&&phase>=20&&phase<40));
        if(phase==28&&name.equals("shield"))check("offhand_shield_used",mc.player.isUsingItem(),"vanilla shield receives USE");
        if(phase==30&&name.equals("block_use")){check("chest_opened",mc.screen instanceof net.minecraft.client.gui.screens.inventory.ContainerScreen,"vanilla chest GUI");mc.player.closeContainer();mc.setScreen(null);}
        if(phase==70&&name.equals("remap")){mc.options.keyAttack.setKey(attackKey);mc.options.keyUse.setKey(useKey);KeyMapping.resetMapping();}
        if(phase==95){
            release(mc);mc.player.setXRot(0);mc.player.xRotO=0;
            server(p->{var s=CombatController.status(p.getUUID());
                if(name.equals("off")){check("off_persists_with_sword",CombatController.mode(p.getUUID())==CombatController.Mode.OFF&&s==null,"OFF never auto-reenables");check("off_vanilla_damage",target!=null&&target.getHealth()<100,"health="+(target==null?-1:target.getHealth()));}
                else if(name.equals("unsupported")){check("unsupported_vanilla",s==null&&target!=null&&target.getHealth()<100,"health="+(target==null?-1:target.getHealth()));}
                else if(name.equals("mining"))check("mining_breaks_block",p.serverLevel().getBlockState(new BlockPos(0,100,2)).isAir(),"custom combat did not consume mining");
                else if(name.equals("swap_attack"))check("equipment_swap_cancels",s==null&&!RollController.busy(p.getUUID()),"no remaining attack after unsupported item");
                else if(name.equals("death"))check("respawn_returns_auto",CombatController.mode(p.getUUID())==CombatController.Mode.AUTO&&!RollController.busy(p.getUUID()),"no stuck action");
                else{int accepted=name.equals("remap")?2:name.equals("shield")||name.equals("block_use")?0:1;check(name+"_accepted",s!=null&&s.accepted()==accepted,s);check(name+"_single_impacts",s!=null&&s.impacts()==accepted,s);}
                if(name.equals("remap"))check("remapped_damage_no_duplicate",target!=null&&Math.abs(target.getHealth()-90)<.01,"health="+(target==null?-1:target.getHealth()));
                if(name.equals("held_heavy"))check("held_heavy_no_repeat",target!=null&&Math.abs(target.getHealth()-94)<.01,"health="+(target==null?-1:target.getHealth()));
            });
        }
    }
    private static void click(Minecraft mc,boolean heavy){KeyMapping.click((heavy?mc.options.keyUse:mc.options.keyAttack).getKey());}
    private static void release(Minecraft mc){mc.options.keyUp.setDown(false);mc.options.keyUse.setDown(false);mc.options.keyAttack.setDown(false);}
    private static void fixture(ServerPlayer p,boolean sword)throws Exception{
        // First scene retains the natural AUTO mode; later scenes clear diagnostic overrides.
        if(CombatController.mode(p.getUUID())!=CombatController.Mode.AUTO)CombatController.automatic(p);
        LocomotionController.cancelAction(p);
        var level=p.serverLevel();p.teleportTo(level,.5,100,.5,0,0);p.setDeltaMovement(0,0,0);p.setHealth(20);p.hurtTime=0;p.fallDistance=0;p.setSprinting(false);p.getAbilities().flying=false;p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++)for(int y=99;y<=104;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),(y==99?Blocks.STONE:Blocks.AIR).defaultBlockState());
        for(var entity:level.getEntities((net.minecraft.world.entity.Entity)null,p.getBoundingBox().inflate(20)))if(!(entity instanceof net.minecraft.world.entity.player.Player))entity.discard();
        for(var slot:EquipmentSlot.values())p.setItemSlot(slot,ItemStack.EMPTY);
        CombatController.refresh(p); // AUTO + empty main hand removes the previous measurement session.
        if(sword)p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));
        // Equipping the sword is observed by the regular server tick, not an enable call.
    }
    private static void spawnTarget(ServerPlayer p){target=new net.minecraft.world.entity.monster.Vindicator(net.minecraft.world.entity.EntityType.VINDICATOR,p.serverLevel());target.setPos(.5,100,2.5);target.setNoAi(true);target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);p.serverLevel().addFreshEntity(target);}
    @SuppressWarnings("unchecked") private static void preview(Minecraft mc){try{var info=mc.getConnection().getPlayerInfo(mc.player.getUUID());var f=info.getClass().getDeclaredField("textureLocations");f.setAccessible(true);var map=(Map<Object,Object>)f.get(info);map.put(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN,new net.minecraft.resources.ResourceLocation("minecraft","textures/entity/player/wide/steve.png"));map.remove(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.CAPE);var s=info.getClass().getDeclaredField("skinModel");s.setAccessible(true);s.set(info,"default");var t=info.getClass().getDeclaredField("pendingTextures");t.setAccessible(true);t.setBoolean(info,true);}catch(Exception ex){check("preview",false,ex);}}
    private static void server(java.util.function.Consumer<ServerPlayer> work){var mc=Minecraft.getInstance();var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);if(p!=null)work.accept(p);});}
    private static void finish(){try{Files.createDirectories(REPORT);String json=new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("checks",CHECKS,"series",SERIES,"renderErrors",PlayerRendererR3.errors));Files.writeString(REPORT.resolve(Boolean.getBoolean("toe.a1_1.restart")?"RESTART.json":"RUNTIME.json"),json);}catch(Exception ex){throw new RuntimeException(ex);}done=true;}

    /** Orbit the render camera only; player input, facing, action clips and physics are untouched. */
    @SubscribeEvent public static void camera(net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles event){
        if(!Boolean.getBoolean("toe.a1_1.test")||orbit==0||Minecraft.getInstance().player==null)return;
        try{
            var p=Minecraft.getInstance().player;var center=p.getEyePosition((float)event.getPartialTick());var pos=event.getCamera().getPosition();var d=pos.subtract(center);double a=Math.toRadians(orbit),x=d.x*Math.cos(a)-d.z*Math.sin(a),z=d.x*Math.sin(a)+d.z*Math.cos(a);
            var method=net.minecraft.client.Camera.class.getDeclaredMethod("setPosition",double.class,double.class,double.class);method.setAccessible(true);method.invoke(event.getCamera(),center.x+x,center.y+d.y,center.z+z);event.setYaw(event.getYaw()+orbit);
        }catch(Exception ex){orbit=0;check("camera_orbit",false,ex);}
    }
}
