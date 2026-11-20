package com.towerofentropy.renderlab;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import com.tom.cpm.shared.io.ModelFile;
import com.towerofentropy.animation.*;
import dev.phe.polymesh.api.PolymeshApi;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT)
public final class R3VisualTest {
    private static final String[] CLIPS={"idle","idle","idle","idle","jog","jog","jog","jog","sword","sword","sword","sword","roll","roll","roll","roll","sword","sword"};
    private static final float[] YAWS={0,90,180,270,0,90,180,270,0,90,180,270,0,90,180,270,0,0};
    private static final Map<String,Float> TIMES=Map.of("idle",25/30f,"jog",7/30f,"sword",6/30f,"roll",22/30f);
    private static final List<Map<String,Object>> RESULTS=new ArrayList<>();
    private static int title,ticks,quit,stage=-1;
    private static boolean opening,done,audited,combatAttached;
    private static long beforeDraw,beforePost,beforeItem;
    private static long beforeOther;
    private static net.minecraft.client.player.RemotePlayer replica;
    private static final long START_MILLIS=System.currentTimeMillis();
    private static final List<Map<String,Object>> IMPACTS=new ArrayList<>();
    private static long lastImpact=-1;
    private static boolean combatReportSaved;
    private static int visit;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("toe.r3"))return;
        var mc=Minecraft.getInstance();
        if(Boolean.getBoolean("toe.combat.test")){
            if(mc.player==null){combatAttached=false;return;}
            if(!combatAttached){preview(mc);SkinnedSteveRenderer.select(true);combatAttached=true;}
            var status=com.towerofentropy.combat.CombatController.status(mc.player.getUUID());
            if(status!=null&&status.impacts()>0&&status.impactTick()!=lastImpact){
                lastImpact=status.impactTick();var c=SkinnedSteveRenderer.controller();String clip=c==null?"none":c.getAnimationName();float time=c==null?-1:c.getAnimationTime();
                boolean sync="UAL_sword".equals(clip)&&time>=.08f&&time<=.35f&&status.impactTick()-status.startedTick()==4;
                IMPACTS.add(Map.of("pass",sync,"startTick",status.startedTick(),"impactTick",status.impactTick(),"renderClip",clip,"renderSecondsAtImpact",time,"draws",SkinnedSteveRenderer.drawCalls));
                LogUtils.getLogger().info("TOE_R3 COMBAT_SYNC pass={} clip={} seconds={} serverDelta={}",sync,clip,time,status.impactTick()-status.startedTick());
            }
            Path result=Path.of("../.logs/m002_0-runtime-results.json");
            try{if(!combatReportSaved&&Files.isRegularFile(result)&&Files.getLastModifiedTime(result).toMillis()>START_MILLIS){
                var data=new LinkedHashMap<String,Object>();data.put("impactSamples",IMPACTS);data.put("allSyncChecksPassed",!IMPACTS.isEmpty()&&IMPACTS.stream().allMatch(r->Boolean.TRUE.equals(r.get("pass"))));data.put("draws",SkinnedSteveRenderer.drawCalls);data.put("renderErrors",SkinnedSteveRenderer.errors);data.put("meanCpuDrawMs",SkinnedSteveRenderer.drawCalls==0?0:SkinnedSteveRenderer.renderNanos/1e6/SkinnedSteveRenderer.drawCalls);
                Files.writeString(Path.of("../.logs/r3-combat-render.json"),new GsonBuilder().setPrettyPrinting().create().toJson(data));combatReportSaved=true;
            }}catch(Exception ex){LogUtils.getLogger().error("TOE_R3 COMBAT_TRACE_SAVE",ex);}
            return;
        }
        if(done){if(++quit==40)mc.stop();return;}
        if(!opening&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null&&++title>100){opening=true;mc.options.pauseOnLostFocus=false;mc.createWorldOpenFlows().loadLevel(mc.screen,"TOE_R3");}
        if(mc.player==null||mc.level==null||mc.getOverlay()!=null)return;
        ++ticks;
        if(Boolean.getBoolean("toe.r3.restart")||visit>0){if(ticks==100){record(Boolean.getBoolean("toe.r3.restart")?"restart_disabled":"reconnect_disabled",!SkinnedSteveRenderer.enabled&&com.towerofentropy.combat.CombatController.status(mc.player.getUUID())==null&&LocomotionController.status(mc.player.getUUID())==null,"renderer, combat and observer cleared");finish();}return;}
        if(ticks==20){preview(mc);mc.setScreen(null);mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);mc.options.fov().set(50);mc.options.hideGui=true;server(p->{
            var level=p.serverLevel();p.teleportTo(level,.5,100,.5,0,0);p.getAbilities().flying=false;p.setDeltaMovement(0,0,0);level.setDayTime(6000);
            for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)for(int y=99;y<=104;y++)level.setBlockAndUpdate(new net.minecraft.core.BlockPos(x,y,z),(y==99?net.minecraft.world.level.block.Blocks.STONE:net.minecraft.world.level.block.Blocks.AIR).defaultBlockState());
            for(var entity:level.getEntities((net.minecraft.world.entity.Entity)null,p.getBoundingBox().inflate(16)))if(!(entity instanceof Player))entity.discard();
            p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));
            for(var slot:new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET,net.minecraft.world.entity.EquipmentSlot.OFFHAND})p.setItemSlot(slot,ItemStack.EMPTY);
            try{LocomotionController.enable(p,true);}catch(Exception ex){record("prepare_cpm",false,ex.toString());}
        });}
        if(Boolean.getBoolean("toe.r3.stress")){
            if(ticks<140)return;
            String[] clips={"jog","sword","roll","roll"};float[] time={10/30f,5/30f,14/30f,14/30f};float[] yaw={90,270,90,270};
            int n=(ticks-140)/65,phase=(ticks-140)%65;
            if(n>=clips.length){finish();return;}
            mc.player.setYRot(0);mc.player.yRotO=0;mc.player.setYHeadRot(yaw[n]);mc.player.yHeadRotO=yaw[n];mc.player.yBodyRot=yaw[n];mc.player.yBodyRotO=yaw[n];
            if(stage!=n){stage=n;SkinnedSteveRenderer.select(true);SkinnedSteveRenderer.pose(clips[n],time[n]);beforeDraw=SkinnedSteveRenderer.drawCalls;beforeItem=SkinnedSteveRenderer.attachmentDraws;beforePost=SkinnedSteveRenderer.localPosts;}
            if(phase==45){record("stress_"+clips[n]+"_"+(int)yaw[n],SkinnedSteveRenderer.drawCalls>beforeDraw&&SkinnedSteveRenderer.attachmentDraws-beforeItem==SkinnedSteveRenderer.drawCalls-beforeDraw&&SkinnedSteveRenderer.localPosts==beforePost&&SkinnedSteveRenderer.errors==0&&org.lwjgl.opengl.GL11.glGetError()==0,"maximum source pelvis/chest sample t="+time[n]);shot("stress-"+clips[n]+"-"+(int)yaw[n]);}
            return;
        }
        if(ticks==120&&System.getProperty("toe.r3.mode","r3").equals("cpm"))server(LocomotionController::stop);
        if(ticks<140)return;
        if(!Boolean.parseBoolean(System.getProperty("toe.r3.auto","true"))){if(ticks==140){mc.options.hideGui=false;SkinnedSteveRenderer.select(!System.getProperty("toe.r3.mode","r3").equals("cpm"));}return;}
        if(!audited){var model=PolymeshApi.loadModel(SkinnedSteveRenderer.MODEL);if(model==null){if(ticks>240){record("model_load",false,"Polymesh model unavailable");finish();}return;}
            var audit=R3ModelAudit.inspect(model);record("loaded_weighted_mesh",((Number)audit.get("blendedVertices")).intValue()>0&&((Number)audit.get("maxBindIdentityError")).doubleValue()<1e-4&&((Number)audit.get("maxBlendVsRigidBlocks")).doubleValue()>1e-5,audit.toString());
            if(!System.getProperty("toe.r3.mode","r3").equals("r2"))record("loaded_torso_blending",((Number)audit.get("torsoBlendedVertices")).intValue()>0&&((Number)audit.get("maxTorsoVsRigidBlocks")).doubleValue()>1e-5,"actual Polymesh torso: "+audit);
            try{Files.writeString(Path.of("../animation_pipeline/reports/M002_R3_RUNTIME_MODEL_AUDIT.json"),new GsonBuilder().setPrettyPrinting().create().toJson(audit));}catch(Exception ex){record("audit_save",false,ex.toString());}audited=true;
        }
        String mode=System.getProperty("toe.r3.mode","r3");boolean baseline=mode.equals("cpm");
        int next=(ticks-140)/65,phase=(ticks-140)%65,total=baseline?16:CLIPS.length;
        if(next<total){String clip=CLIPS[next];String label=clip+"-"+(next==16?"early-":next==17?"late-":"")+(int)YAWS[next];
            // Camera yaw remains zero; rotate the displayed body independently for genuine side/back views.
            mc.player.setYRot(0);mc.player.yRotO=0;mc.player.setYHeadRot(YAWS[next]);mc.player.yHeadRotO=YAWS[next];mc.player.yBodyRot=YAWS[next];mc.player.yBodyRotO=YAWS[next];
            if(stage!=next){stage=next;beforeDraw=SkinnedSteveRenderer.drawCalls;beforePost=SkinnedSteveRenderer.localPosts;beforeItem=SkinnedSteveRenderer.attachmentDraws;
                if(baseline){SkinnedSteveRenderer.select(false);server(p->{try{LocomotionController.stop(p);ToeCpmPlugin.common.setPlayerModel(Player.class,p,ModelFile.load(Path.of("../animation_pipeline/converted/m002_r1/baseline-"+clip+".cpmmodel").toFile()),false);}catch(Exception ex){record("baseline_apply",false,ex.toString());}});}
                else{SkinnedSteveRenderer.select(true);SkinnedSteveRenderer.pose(clip,next==16?2/30f:next==17?10/30f:TIMES.get(clip));}
            }
            if(baseline&&phase==20)server(p->ToeCpmPlugin.common.playAnimation(Player.class,p,"toe_r1",1));
            if(phase==45){boolean drew=SkinnedSteveRenderer.drawCalls>beforeDraw;int glError=org.lwjgl.opengl.GL11.glGetError();int cpmPose=ToeCpmPlugin.client.getAnimationPlaying("toe_r1");boolean itemOnce=SkinnedSteveRenderer.attachmentDraws-beforeItem==SkinnedSteveRenderer.drawCalls-beforeDraw;record(mode+"_"+label,(baseline?!drew&&cpmPose==1:drew&&SkinnedSteveRenderer.localPosts==beforePost&&itemOnce)&&glError==0,"draws="+(SkinnedSteveRenderer.drawCalls-beforeDraw)+" items="+(SkinnedSteveRenderer.attachmentDraws-beforeItem)+" posts="+(SkinnedSteveRenderer.localPosts-beforePost)+" CPM="+cpmPose+" GLerror="+glError+" light="+SkinnedSteveRenderer.lastLight+" yaw="+SkinnedSteveRenderer.lastYaw+" pos="+mc.player.position());shot(mode+"-"+label);}
            return;
        }
        int tail=ticks-(140+total*65);
        if(tail==0){mc.player.connection.sendCommand("toe_renderlab off");beforeDraw=SkinnedSteveRenderer.drawCalls;beforePost=SkinnedSteveRenderer.localPosts;server(p->{try{LocomotionController.enable(p,true);}catch(Exception ex){record("fallback_apply",false,ex.toString());}});}
        // The C2S command crosses two queues; measure after it has had time to arrive.
        if(tail==15){beforeDraw=SkinnedSteveRenderer.drawCalls;beforePost=SkinnedSteveRenderer.localPosts;}
        if(tail==55){int idle=ToeCpmPlugin.client.getAnimationPlaying("toe_idle");record("off_restores_cpm",!SkinnedSteveRenderer.enabled&&idle==1&&SkinnedSteveRenderer.drawCalls==beforeDraw&&SkinnedSteveRenderer.localPosts>beforePost,"CPM idle="+idle+" posts="+(SkinnedSteveRenderer.localPosts-beforePost));shot(mode+"-off-cpm");}
        if(tail==70){SkinnedSteveRenderer.select(true);SkinnedSteveRenderer.pose("idle",0);mc.options.setCameraType(CameraType.FIRST_PERSON);beforeDraw=SkinnedSteveRenderer.drawCalls;}
        if(tail==100){record("first_person_fallback",SkinnedSteveRenderer.drawCalls==beforeDraw,"body not drawn in first person; native hands retained");shot(mode+"-first-person");}
        if(tail==115){mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);SkinnedSteveRenderer.pose("jog",-1);
            beforeOther=SkinnedSteveRenderer.otherPre;replica=new net.minecraft.client.player.RemotePlayer(mc.level,new com.mojang.authlib.GameProfile(UUID.fromString("00000000-0000-0000-0000-000000000202"),"R3Replica"));replica.setPos(-.8,100,.5);mc.level.addPlayer(902002,replica);
        }
        if(tail==150)shot(mode+"-other-player");
        if(tail==155){record("embedded_clip_playback",SkinnedSteveRenderer.controller()!=null&&SkinnedSteveRenderer.controller().getAnimationTime()>0,"clip="+SkinnedSteveRenderer.controller().getAnimationName());record("other_player_untouched",SkinnedSteveRenderer.otherPre>beforeOther&&SkinnedSteveRenderer.otherCanceled==0,"replica Pre="+(SkinnedSteveRenderer.otherPre-beforeOther)+" canceled="+SkinnedSteveRenderer.otherCanceled);if(replica!=null)replica.discard();}
        if(tail==170)server(p->p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,new ItemStack(Items.IRON_CHESTPLATE)));
        if(tail==190||tail==240||tail==290){beforeDraw=SkinnedSteveRenderer.drawCalls;beforePost=SkinnedSteveRenderer.localPosts;}
        if(tail==205||tail==255||tail==305){String reason=tail==205?"armadura":tail==255?"offhand":"capa";record("equipment_fallback_"+reason,SkinnedSteveRenderer.fallbackReason.equals(reason)&&SkinnedSteveRenderer.drawCalls==beforeDraw&&SkinnedSteveRenderer.localPosts>beforePost,"reason="+SkinnedSteveRenderer.fallbackReason+" draws="+(SkinnedSteveRenderer.drawCalls-beforeDraw)+" nativePosts="+(SkinnedSteveRenderer.localPosts-beforePost));shot(mode+"-fallback-"+reason);}
        if(tail==220)server(p->{p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,ItemStack.EMPTY);p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));});
        if(tail==270){server(p->p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,ItemStack.EMPTY));cape(mc,true);}
        if(tail==320){cape(mc,false);beforeDraw=SkinnedSteveRenderer.drawCalls;}
        if(tail==350){record("resume_after_equipment",SkinnedSteveRenderer.fallbackReason.isEmpty()&&SkinnedSteveRenderer.drawCalls>beforeDraw,"reason="+SkinnedSteveRenderer.fallbackReason);record("render_errors",SkinnedSteveRenderer.errors==0,"errors="+SkinnedSteveRenderer.errors);}
        if(tail==365){mc.level.disconnect();mc.clearLevel(new TitleScreen());opening=false;title=0;ticks=0;visit=1;}
    }
    @Mod.EventBusSubscriber(modid="towerofentropy") public static final class FrozenPose {
        @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){if(Boolean.getBoolean("toe.r3")&&System.getProperty("toe.r3.mode","").equals("cpm")&&e.phase==TickEvent.Phase.END&&e.player instanceof ServerPlayer p&&ToeCpmPlugin.common!=null&&ToeCpmPlugin.common.getAnimationPlaying(Player.class,p,"toe_r1")==0)ToeCpmPlugin.common.playAnimation(Player.class,p,"toe_r1",1);}
    }
    @SuppressWarnings("unchecked") private static void preview(Minecraft mc){try{var info=mc.getConnection().getPlayerInfo(mc.player.getUUID());var f=info.getClass().getDeclaredField("textureLocations");f.setAccessible(true);((Map<Object,Object>)f.get(info)).put(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN,new ResourceLocation("minecraft","textures/entity/player/wide/steve.png"));var s=info.getClass().getDeclaredField("skinModel");s.setAccessible(true);s.set(info,"default");var p=info.getClass().getDeclaredField("pendingTextures");p.setAccessible(true);p.setBoolean(info,true);}catch(Exception ex){record("preview_skin",false,ex.toString());}}
    @SuppressWarnings("unchecked") private static void cape(Minecraft mc,boolean on){try{var info=mc.getConnection().getPlayerInfo(mc.player.getUUID());var f=info.getClass().getDeclaredField("textureLocations");f.setAccessible(true);var map=(Map<Object,Object>)f.get(info);if(on)map.put(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.CAPE,new ResourceLocation("minecraft","textures/entity/elytra.png"));else map.remove(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.CAPE);}catch(Exception ex){record("cape_fixture",false,ex.toString());}}
    private static void server(java.util.function.Consumer<ServerPlayer> action){var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();var id=mc.player.getUUID();server.execute(()->{var p=server.getPlayerList().getPlayer(id);if(p!=null)action.accept(p);});}
    private static void shot(String label){var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,"r3-"+label+".png",mc.getMainRenderTarget(),msg->{});}
    private static void record(String name,boolean pass,String detail){RESULTS.add(Map.of("test",name,"pass",pass,"detail",detail));LogUtils.getLogger().info("TOE_R3 CHECK {} pass={} {}",name,pass,detail);}
    private static void finish(){try{var report=new LinkedHashMap<String,Object>();report.put("checks",RESULTS);report.put("draws",SkinnedSteveRenderer.drawCalls);report.put("meanCpuDrawMs",SkinnedSteveRenderer.drawCalls==0?0:SkinnedSteveRenderer.renderNanos/1e6/SkinnedSteveRenderer.drawCalls);report.put("maxCpuDrawMs",SkinnedSteveRenderer.maxNanos/1e6);Files.writeString(Path.of("../.logs/r3-"+(Boolean.getBoolean("toe.r3.stress")?"stress":Boolean.getBoolean("toe.r3.restart")?"restart":System.getProperty("toe.r3.mode","r3"))+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(Exception ex){LogUtils.getLogger().error("TOE_R3 SAVE_FAILED",ex);}LogUtils.getLogger().info("TOE_R3 DONE passed={} total={}",RESULTS.stream().filter(r->Boolean.TRUE.equals(r.get("pass"))).count(),RESULTS.size());done=true;}
}
