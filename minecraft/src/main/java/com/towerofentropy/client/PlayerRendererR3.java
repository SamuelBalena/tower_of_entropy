package com.towerofentropy.client;

import java.util.UUID;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.towerofentropy.animation.ToeCpmPlugin;
import dev.phe.polymesh.api.PolymeshApi;
import dev.phe.polymesh.animation.AnimationController;
import dev.phe.polymesh.client.GltfModelRenderer;
import dev.phe.polymesh.model.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** Official R3 wrapper around Polymesh. Rendering never changes an entity. */
@Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT)
public final class PlayerRendererR3 {
    public static final boolean PREVIOUS=Boolean.getBoolean("toe.a2.previous");
    public static final ResourceLocation MODEL=new ResourceLocation("towerofentropy",PREVIOUS?"steve_r3":VisualContext.PREVIOUS?"steve_a2":"steve_a3_wall_prototypes");
    private static final ResourceLocation SLIM_MODEL=new ResourceLocation("towerofentropy","steve_a3_slim_wall_prototypes");
    public static boolean enabled=true;
    public static UUID selected;
    public static String forcedClip;
    public static float frozenTime=-1;
    public static long drawCalls,attachmentDraws,localPosts,otherPre,otherCanceled,renderNanos,maxNanos;
    public static int errors,lastLight,lastVertices;
    public static float lastYaw;
    public static String fallbackReason="";
    public static long equipmentFallbackFrames;
    private static String notifiedReason="";
    private static RuntimeModel loaded;
    private static SkinRenderer renderer;
    private static boolean slimUnavailable;
    private static JsonObject poses;
    private static double lastClock=Double.NaN;
    private static String requested="";
    private static long combatStart=Long.MIN_VALUE;
    private static ResourceLocation skin;
    private static R3VisualPose visualPose;
    private static UpperBodyPose upperBody;
    private static Matrix4f[] displayed;
    private static double jogClock;
    private static double cyclePhase,playbackRate=1;
    private static final Matrix4f sampledAnchor=new Matrix4f();
    private static final Quaternionf wrist0=new Quaternionf(),wrist1=new Quaternionf();
    public static long pipelineNanos,poseNanos;
    public static R3VisualPose visualPose(){return visualPose;}
    public static Matrix4f[] displayed(){return displayed;}
    private static final class SkinRenderer extends GltfModelRenderer {
        SkinRenderer(ResourceLocation model){super(model);}
        @Override protected ResourceLocation resolveTexture(RuntimeMaterial material){return skin;}
    }
    public static AnimationController controller(){return renderer==null?null:renderer.getAnimationController();}
    public static RuntimeModel model(){return loaded;}
    public static void select(boolean on){
        enabled=on;slimUnavailable=false;selected=Minecraft.getInstance().player==null?null:Minecraft.getInstance().player.getUUID();
        forcedClip=null;frozenTime=-1;requested="";lastClock=Double.NaN;
        if(visualPose!=null)visualPose.reset();
    }
    public static void pose(String name,float time){forcedClip=name;frozenTime=time;requested="";lastClock=Double.NaN;}
    private static String base(){if(!PREVIOUS)return VisualContext.PREVIOUS?HorizontalLocomotion.MOTION.clip():VisualContext.locomotion();return ToeCpmPlugin.client!=null&&ToeCpmPlugin.client.getAnimationPlaying("toe_jog")==1?"jog":"idle";}
    public static boolean laboratory(){return Boolean.getBoolean("toe.r1")||Boolean.getBoolean("toe.r2")||Boolean.getBoolean("toe.r3")||Boolean.getBoolean("toe.r31")||Boolean.getBoolean("toe.qa");}
    private static String desired(){
        if(forcedClip!=null)return forcedClip;
        if(!PREVIOUS&&!VisualContext.PREVIOUS&&Minecraft.getInstance().player.isDeadOrDying())return "death";
        if(ClientRollMotion.busy())return ClientRollMotion.visual()||(!R3VisualPose.LEGACY&&ClientRollMotion.pending())?"roll":base();
        if(!ClientCombatState.clip().isEmpty())return ClientCombatState.clip();
        if(ToeCpmPlugin.client!=null){if(ToeCpmPlugin.client.getAnimationPlaying("toe_roll")==1&&!ClientRollMotion.recentlyFinished())return "roll";if(ToeCpmPlugin.client.getAnimationPlaying("toe_sword")==1)return "sword";}
        var p=Minecraft.getInstance().player;
        if(!PREVIOUS&&!VisualContext.PREVIOUS){String context=VisualContext.selected();if(context!=null)return context;}
        return !PREVIOUS?base():p!=null&&Math.hypot(p.getX()-p.xo,p.getZ()-p.zo)>.025?"jog":base();
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void render(RenderPlayerEvent.Pre event){
        if(laboratory())return;
        var local=Minecraft.getInstance().player;
        if(local==null)return;
        selected=local.getUUID();
        if(!event.getEntity().getUUID().equals(selected)){++otherPre;return;}
        if(!enabled||!(event.getEntity() instanceof AbstractClientPlayer player)||(!player.isAlive()&&(PREVIOUS||VisualContext.PREVIOUS||!player.isDeadOrDying()))||player.isSpectator()||player.isInvisible())return;
        boolean slim=player.getModelName().equals("slim");
        if(slim&&slimUnavailable){fallbackReason="Slim render error: vanilla/CPM";return;}
        if(!player.getModelName().equals("default")&&(!slim||PREVIOUS||VisualContext.PREVIOUS)){fallbackReason="unsupported skin variant: vanilla/CPM";return;}
        fallbackReason=equipmentReason(player);
        if(!fallbackReason.isEmpty()){
            ++equipmentFallbackFrames;
            if(!fallbackReason.equals(notifiedReason)){player.displayClientMessage(net.minecraft.network.chat.Component.literal("Renderlab: CPM/vanilla preserva "+fallbackReason+"; equipamento ainda nao homologado."),true);notifiedReason=fallbackReason;}
            return;
        }
        notifiedReason="";
        if(PREVIOUS||VisualContext.PREVIOUS){if(player.getPose()!=net.minecraft.world.entity.Pose.STANDING||player.isPassenger()||player.isFallFlying()
            ||player.isInWaterOrBubble()||player.isInLava()||player.onClimbable()||player.getAbilities().flying
            ||player.isUsingItem()||player.swinging||player.hurtTime>0||(PREVIOUS&&player.isSprinting())||(forcedClip==null&&!player.onGround()&&!ClientRollMotion.visual()))return;}
        else if(!player.isDeadOrDying()&&forcedClip==null&&VisualContext.unsupported()){fallbackReason="vanilla context";return;}
        if(VisualContext.PREVIOUS&&!PREVIOUS&&forcedClip==null&&!ClientRollMotion.busy()&&ClientCombatState.clip().isEmpty()&&HorizontalLocomotion.MOTION.directionalFallback()){fallbackReason="directional clip pending: "+HorizontalLocomotion.MOTION.state();return;}
        var mc=Minecraft.getInstance();
        // The local first-person body is not replaced. Vanilla/CPM hands remain the fallback.
        if(player==mc.player&&mc.options.getCameraType().isFirstPerson())return;
        String wanted=desired();
        if(!PREVIOUS&&!VisualContext.PREVIOUS&&forcedClip==null&&!ClientRollMotion.busy()
                &&ClientCombatState.clip().isEmpty()&&!wanted.startsWith("sword")&&!wanted.equals("roll")&&!wanted.equals("heavy")
                &&VisualContext.vanillaSwing(wanted)){
            fallbackReason="vanilla swing";
            return;
        }
        boolean swordBlock=!PREVIOUS&&!VisualContext.PREVIOUS&&forcedClip==null
                &&wanted.equals(com.towerofentropy.combat.CombatMove.LIGHT_ATTACK_A.clip)
                &&!ClientRollMotion.busy()&&ClientCombatState.clip().isEmpty()&&ClientSwingPose.swordBlock(local);
        var resource=slim?SLIM_MODEL:MODEL;
        var model=PolymeshApi.loadModel(resource);
        if(model==null){fallbackReason="missing model "+resource+": vanilla/CPM";return;}
        try{
            if(loaded!=model){loaded=model;renderer=new SkinRenderer(resource);requested="";upperBody=new UpperBodyPose(model);visualPose=R3VisualPose.LEGACY?null:new R3VisualPose(model,PREVIOUS?"visual-support.json":VisualContext.PREVIOUS?"a2-support.json":"a3-support.json");
                try(var in=mc.getResourceManager().open(new ResourceLocation("towerofentropy",PREVIOUS?"r3-poses.json":VisualContext.PREVIOUS?"a2-poses.json":"a3-poses.json"))){poses=JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
                if(!PREVIOUS&&!VisualContext.PREVIOUS){
                    try(var in=mc.getResourceManager().open(new ResourceLocation("towerofentropy","expansion-poses.json"))){
                        var additions=JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                        additions.entrySet().forEach(entry->poses.add(entry.getKey(),entry.getValue()));
                    }
                    for(String file:new String[]{"parkour-poses.json","wall-prototype-poses.json"}){
                        try(var in=mc.getResourceManager().open(new ResourceLocation("towerofentropy",file))){
                            var additions=JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                            additions.entrySet().forEach(entry->poses.add(entry.getKey(),entry.getValue()));
                        }
                    }
                }
            }
            long pipelineStart=System.nanoTime();
            var c=controller();boolean loop=HorizontalLocomotion.loop(wanted);
            if(!requested.equals(wanted)||(forcedClip==null&&!ClientCombatState.clip().isEmpty()&&combatStart!=ClientCombatState.started())){c.play("UAL_"+wanted,loop);if(R3VisualPose.LEGACY||!loop)c.setAnimationTime(0);requested=wanted;combatStart=ClientCombatState.started();}
            double now=player.level().getGameTime()+event.getPartialTick();
            if(!PREVIOUS&&Double.isFinite(lastClock)){
                double dt=Math.max(0,Math.min(.25,(now-lastClock)/20));
                String moving=VisualContext.PREVIOUS?HorizontalLocomotion.MOTION.clip():HorizontalLocomotion.gait();double target=HorizontalLocomotion.MOTION.state()==com.towerofentropy.animation.HorizontalMotion.State.IDLE?0:Math.max(.45,Math.min(1.5,HorizontalLocomotion.MOTION.speed()/HorizontalLocomotion.referenceSpeed(moving)));
                playbackRate+=(target-playbackRate)*(1-Math.exp(-dt/.10));
                double seconds=switch(moving){case "walk"->1.333333;case "sprint"->.666667;default->.933333;};cyclePhase=(cyclePhase+dt*playbackRate/seconds)%1;
            }
            if(!R3VisualPose.LEGACY&&Double.isFinite(lastClock)&&(wanted.equals("jog")||Math.hypot(player.getX()-player.xo,player.getZ()-player.zo)>.025))jogClock+=Math.max(0,Math.min(.25,(now-lastClock)/20));
            if(forcedClip==null&&ClientRollMotion.busy()&&ClientRollMotion.pending()&&!R3VisualPose.LEGACY)c.setAnimationTime(0);
            else if(forcedClip==null&&ClientRollMotion.visual())c.setAnimationTime(ClientRollMotion.visualTime(event.getPartialTick()));
            else if(forcedClip==null&&!ClientCombatState.clip().isEmpty())c.setAnimationTime(com.towerofentropy.combat.CombatMove.visualTime(ClientCombatState.clip(),ClientCombatState.time(event.getPartialTick())));
            else if(swordBlock){
                if(!c.isPlaying())c.play("UAL_"+wanted,false);
                c.setAnimationTime(ClientSwingPose.time(local,c.getCurrentAnimation().getDuration(),event.getPartialTick()));
            }
            else if(frozenTime>=0)c.setAnimationTime(frozenTime);
            else if(Double.isFinite(lastClock)&&now>lastClock)c.tick((float)Math.min(.25,(now-lastClock)/20));
            if(!PREVIOUS&&forcedClip==null&&loop&&!wanted.equals("idle")){var info=poses.getAsJsonObject(wanted);double offset=info.has("phaseOffset")?info.get("phaseOffset").getAsDouble():0;c.setAnimationTime((float)(((cyclePhase+offset)%1)*c.getCurrentAnimation().getDuration()));}
            else if(!R3VisualPose.LEGACY&&forcedClip==null&&wanted.equals("jog"))c.setAnimationTime((float)(jogClock%c.getCurrentAnimation().getDuration()));
            if(!PREVIOUS&&!VisualContext.PREVIOUS&&forcedClip==null&&((VisualContext.own(wanted)&&!ClientRollMotion.busy()&&ClientCombatState.clip().isEmpty())||wanted.equals("death"))){if(!c.isPlaying())c.play("UAL_"+wanted,loop);c.setAnimationTime(VisualContext.time(wanted,c.getCurrentAnimation().getDuration(),event.getPartialTick()));}
            lastClock=now;
            if(frozenTime<0&&!loop&&!c.isPlaying()&&!swordBlock&&!VisualContext.own(wanted)){c.play("UAL_"+base(),true);forcedClip=null;}
            Matrix4f[] globals=c.computeGlobalTransforms(0);
            if((swordBlock&&ClientSwingPose.swordBlockLeft(local))||(forcedClip==null&&wanted.startsWith("punch_")&&ClientSwingPose.mirrored(local)))upperBody.mirrorUpperBody(globals);
            if(!PREVIOUS&&!VisualContext.PREVIOUS&&wanted.startsWith("bow_"))UpperBodyPose.aim(c,globals,player.getXRot());
            boolean airSword=forcedClip==null&&!player.onGround()&&!ClientCombatState.clip().isEmpty()&&wanted.equals(ClientCombatState.clip());
            if(!PREVIOUS&&!VisualContext.PREVIOUS&&(airSword||swordBlock||VisualContext.upper(wanted))&&!wanted.startsWith("bow_")){String lower=player.isCrouching()?(HorizontalLocomotion.MOTION.speed()>.012?"crouch_move":"crouch_idle"):!player.onGround()?(airSword?VisualContext.airbornePose():"jump_loop"):HorizontalLocomotion.gait();globals=upperBody.compose(globals,lower,(float)cyclePhase,0,event.getPartialTick());}
            Matrix4f anchorTransform=null;
            if(!R3VisualPose.LEGACY){anchorTransform=sampleAnchor(wanted,c.getAnimationTime());globals=visualPose.apply(globals,wanted,ClientCombatState.started(),c.getAnimationTime(),now/20,anchorTransform,player.onGround());anchorTransform=visualPose.anchor();}
            displayed=globals;poseNanos+=System.nanoTime()-pipelineStart;
            skin=player.getSkinTextureLocation();lastYaw=ClientRollMotion.visual()||(!R3VisualPose.LEGACY&&ClientRollMotion.pending())?ClientRollMotion.yaw():wanted.startsWith("bow_")?player.getYRot()-UpperBodyPose.facing(model,c,globals):Mth.rotLerp(event.getPartialTick(),player.yBodyRotO,player.yBodyRot);lastLight=event.getPackedLight();
            if(forcedClip==null&&ClientWallVisual.owns(wanted))lastYaw=ClientWallVisual.yaw();
            if(!VisualContext.PREVIOUS&&(wanted.equals("backward")||wanted.startsWith("strafe_")))lastYaw=player.getYRot();
            if(!PREVIOUS&&!VisualContext.PREVIOUS&&forcedClip==null&&(airSword||wanted.equals("idle")||wanted.equals("walk")||wanted.equals("jog")||wanted.equals("sprint")||wanted.startsWith("jump_")))
                lastYaw=HorizontalLocomotion.travelYaw(event.getPartialTick(),lastYaw);
            long start=System.nanoTime();var ps=event.getPoseStack();ps.pushPose();
            try{
                // glTF is Y-up. Reflect X to preserve the original Minecraft model handedness.
                ps.mulPose(Axis.YP.rotationDegrees(180-lastYaw));ps.scale(-.9375f,.9375f,.9375f);
                if(wanted.startsWith("swim"))ps.translate(0,-.40,0);
                ps.last().pose().normal(ps.last().normal()); // exact inverse transpose for the handedness reflection
                renderer.renderModel(model,globals,ps.last(),event.getMultiBufferSource(),event.getPackedLight(),OverlayTexture.NO_OVERLAY,0xffffffff);
                String clip=c.getAnimationName();
                boolean bow=!VisualContext.PREVIOUS&&clip.startsWith("UAL_bow_");
                var actionHand=(clip.equals("UAL_consume")||bow)&&player.isUsingItem()?player.getUsedItemHand()
                    :bow&&!clip.equals("UAL_bow_idle")?VisualContext.bowHand:InteractionHand.MAIN_HAND;
                boolean dedicatedLeft=bow||clip.equals("UAL_consume")||clip.equals("UAL_torch");
                for(var hand:InteractionHand.values()){
                    var stack=player.getItemInHand(hand);
                    if(stack.isEmpty())continue;
                    if(hand==InteractionHand.MAIN_HAND
                            &&com.towerofentropy.combat.WeaponTags.isWeapon(stack)&&!ClientWeaponState.canUse(hand)){
                        sheathed(player,stack,ps,event,c,globals);
                        continue;
                    }
                    boolean left=dedicatedLeft?hand==actionHand
                        :(hand==InteractionHand.MAIN_HAND)==(player.getMainArm()==HumanoidArm.LEFT);
                    attachment(player,stack,left,bow&&hand==actionHand,dedicatedLeft,ps,event,c,globals,anchorTransform);
                }
            }finally{ps.popPose();}
            long elapsed=System.nanoTime()-start;renderNanos+=elapsed;maxNanos=Math.max(maxNanos,elapsed);++drawCalls;
            pipelineNanos+=System.nanoTime()-pipelineStart;
            // Cancel only after a complete successful draw; suppresses vanilla and CPM layers/items once.
            event.setCanceled(true);
        }catch(Exception ex){++errors;if(slim)slimUnavailable=true;else enabled=false;LogUtils.getLogger().error("TOE_R3 RENDER_DISABLED_FALLBACK",ex);}
    }
    private static JsonObject anchor(String clip,float time){
        var info=poses.getAsJsonObject(clip);if(info==null)info=poses.getAsJsonObject("idle");
        var frames=info.getAsJsonArray("frames");float duration=info.get("duration").getAsFloat()/1000;
        boolean loop=HorizontalLocomotion.loop(clip);float phase=loop?time%duration:Math.min(time,duration);
        int i=Math.min(frames.size()-1,(int)(phase/duration*(loop?frames.size():frames.size()-1)));
        for(var value:frames.get(i).getAsJsonObject().getAsJsonArray("components")){var a=value.getAsJsonObject();if(a.get("storeID").getAsInt()==303)return a;}
        throw new IllegalStateException("Original wrist anchor missing");
    }
    private static Matrix4f sampleAnchor(String clip,float time){
        return sampleAnchor(clip,time,303);
    }
    private static Matrix4f sampleAnchor(String clip,float time,int id){
        var info=poses.getAsJsonObject(clip);if(info==null)info=poses.getAsJsonObject("idle");var frames=info.getAsJsonArray("frames");float duration=info.get("duration").getAsFloat()/1000;
        boolean loop=HorizontalLocomotion.loop(clip);float phase=loop?time%duration:Math.min(time,duration);float index=phase/duration*(loop?frames.size():frames.size()-1);int i=Math.min(frames.size()-1,(int)index),j=loop?(i+1)%frames.size():Math.min(frames.size()-1,i+1);float t=index-i;
        JsonObject a=null,b=null;for(var v:frames.get(i).getAsJsonObject().getAsJsonArray("components"))if(v.getAsJsonObject().get("storeID").getAsInt()==id)a=v.getAsJsonObject();for(var v:frames.get(j).getAsJsonObject().getAsJsonArray("components"))if(v.getAsJsonObject().get("storeID").getAsInt()==id)b=v.getAsJsonObject();
        var p=a.getAsJsonObject("pos");var n=b.getAsJsonObject("pos");var r=a.getAsJsonObject("rotation");var s=b.getAsJsonObject("rotation");wrist0.rotationZYX(rad(r,"z"),rad(r,"y"),rad(r,"x"));wrist1.rotationZYX(rad(s,"z"),rad(s,"y"),rad(s,"x"));wrist0.slerp(wrist1,t).normalize();
        if(id==302){var pivot=new org.joml.Vector3f(1/16f,10/16f,-2/16f);wrist0.transform(pivot);return sampledAnchor.translation(1/16f-pivot.x,6/16f-pivot.y,-2/16f-pivot.z).rotate(wrist0);}
        return sampledAnchor.translation(Mth.lerp(t,p.get("x").getAsFloat(),n.get("x").getAsFloat())/16,Mth.lerp(t,p.get("y").getAsFloat(),n.get("y").getAsFloat())/16,Mth.lerp(t,p.get("z").getAsFloat(),n.get("z").getAsFloat())/16).rotate(wrist0);
    }
    private static void sheathed(AbstractClientPlayer player,ItemStack stack,PoseStack pose,
            RenderPlayerEvent.Pre event,AnimationController controller,Matrix4f[] globals){
        pose.pushPose();
        try{
            pose.mulPoseMatrix(globals[controller.getNodeIndex("1")]);
            pose.translate(0,.35,-.20);
            pose.mulPose(Axis.ZP.rotationDegrees(35));
            pose.scale(.83f,.83f,.83f);
            Minecraft.getInstance().getItemRenderer().renderStatic(player,stack,ItemDisplayContext.FIXED,false,
                    pose,event.getMultiBufferSource(),player.level(),event.getPackedLight(),OverlayTexture.NO_OVERLAY,player.getId());
            ++attachmentDraws;
        }finally{pose.popPose();}
    }

    private static void attachment(AbstractClientPlayer player,ItemStack stack,boolean leftItem,boolean bow,boolean dedicatedLeft,PoseStack ps,RenderPlayerEvent.Pre e,AnimationController c,Matrix4f[] globals,Matrix4f blendedAnchor){
        String hand=leftItem?"102":"103";
        Matrix4f forearm=R3VisualPose.LEGACY?c.getNodeGlobalTransform(hand,0):globals[c.getNodeIndex(hand)];if(forearm==null)return;
        if(leftItem)blendedAnchor=sampleAnchor(c.getAnimationName().replace("UAL_",""),c.getAnimationTime(),302);
        ps.pushPose();try{
            if(bow){
                // Vanilla bow quad: keep its grip on the source hand and align its arrow with real aim.
                var grip=forearm.transformPosition(new org.joml.Vector3f((player.getModelName().equals("slim")?.5f:1f)/16f,6/16f,2/16f));ps.translate(grip.x,grip.y,grip.z);
                ps.mulPose(Axis.YP.rotationDegrees(player.getYRot()-lastYaw));ps.mulPose(Axis.XP.rotationDegrees(-player.getXRot()));ps.mulPose(Axis.YP.rotationDegrees(-90));ps.mulPose(Axis.ZP.rotationDegrees(45));ps.scale(.9f,.9f,.9f);
                Minecraft.getInstance().getItemRenderer().renderStatic(player,stack,ItemDisplayContext.NONE,false,ps,e.getMultiBufferSource(),player.level(),e.getPackedLight(),OverlayTexture.NO_OVERLAY,player.getId());++attachmentDraws;return;
            }
            ps.mulPoseMatrix(forearm);
            if(player.getModelName().equals("slim"))ps.translate((leftItem?-.5f:.5f)/16f,0,0);
            ps.scale(1,1,-1); // glTF bone axes -> original CPM forearm axes
            if(blendedAnchor!=null)ps.mulPoseMatrix(blendedAnchor);
            else{String name=c.getAnimationName().replace("UAL_","");var a=anchor(name,c.getAnimationTime());var p=a.getAsJsonObject("pos");var r=a.getAsJsonObject("rotation");ps.translate(p.get("x").getAsFloat()/16,p.get("y").getAsFloat()/16,p.get("z").getAsFloat()/16);var q=new Quaternionf().rotationZYX(rad(r,"z"),rad(r,"y"),rad(r,"x"));ps.mulPose(q);}
            ps.mulPose(Axis.XP.rotationDegrees(-90));ps.mulPose(Axis.YP.rotationDegrees(180));ps.translate((leftItem&&!dedicatedLeft?-1:1)/16f,.125,-.625);
            ps.last().pose().normal(ps.last().normal()); // mulPoseMatrix does not update normals in MC 1.20.1
            Minecraft.getInstance().getItemRenderer().renderStatic(player,stack,leftItem?ItemDisplayContext.THIRD_PERSON_LEFT_HAND:ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,leftItem,ps,e.getMultiBufferSource(),player.level(),e.getPackedLight(),OverlayTexture.NO_OVERLAY,player.getId());
            ++attachmentDraws;
        }finally{ps.popPose();}
    }
    private static float rad(JsonObject r,String k){return (float)Math.toRadians(r.get(k).getAsFloat());}
    public static String equipmentReason(AbstractClientPlayer p){
        for(var part:new net.minecraft.world.entity.player.PlayerModelPart[]{net.minecraft.world.entity.player.PlayerModelPart.HAT,net.minecraft.world.entity.player.PlayerModelPart.JACKET,net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE,net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE,net.minecraft.world.entity.player.PlayerModelPart.LEFT_PANTS_LEG,net.minecraft.world.entity.player.PlayerModelPart.RIGHT_PANTS_LEG})if(!p.isModelPartShown(part))return "camadas da skin";
        if((PREVIOUS||VisualContext.PREVIOUS)&&p.getMainArm()!=HumanoidArm.RIGHT)return "mao esquerda";
        for(var item:p.getArmorSlots())if(!item.isEmpty())return "armadura";
        if((PREVIOUS||VisualContext.PREVIOUS)&&!p.getOffhandItem().isEmpty())return "offhand";
        if(p.getCloakTextureLocation()!=null&&p.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.CAPE))return "capa";
        return "";
    }
    @SubscribeEvent public static void post(RenderPlayerEvent.Post event){if(event.getEntity().getUUID().equals(selected))++localPosts;}
    @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true) public static void observe(RenderPlayerEvent.Pre event){if(!event.getEntity().getUUID().equals(selected)&&event.isCanceled())++otherCanceled;}
    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut e){enabled=true;slimUnavailable=false;selected=null;loaded=null;renderer=null;upperBody=null;VisualContext.reset();ClientSwingPose.reset();visualPose=null;displayed=null;jogClock=0;cyclePhase=0;playbackRate=1;HorizontalLocomotion.MOTION.reset();forcedClip=null;frozenTime=-1;lastClock=Double.NaN;requested="";}
}
