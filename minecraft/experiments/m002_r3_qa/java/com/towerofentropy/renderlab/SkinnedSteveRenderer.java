package com.towerofentropy.renderlab;

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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** R3-only wrapper around the existing Polymesh skinning pipeline. Never changes an entity. */
@Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT)
public final class SkinnedSteveRenderer {
    public static final ResourceLocation MODEL=new ResourceLocation("towerofentropy",System.getProperty("toe.r31.mode","r31").equals("r2")?"r31_reference":System.getProperty("toe.r31.mode","r31").equals("r3")?"r31_r3":"r31_steve");
    public static boolean enabled;
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
    private static JsonObject poses;
    private static double lastClock=Double.NaN;
    private static String requested="";
    private static ResourceLocation skin;
    private static final class SkinRenderer extends GltfModelRenderer {
        SkinRenderer(ResourceLocation id){super(id);}
        @Override protected ResourceLocation resolveTexture(RuntimeMaterial material){return skin;}
    }
    public static AnimationController controller(){return renderer==null?null:renderer.getAnimationController();}
    public static RuntimeModel model(){return loaded;}
    public static void select(boolean on){
        enabled=on;selected=Minecraft.getInstance().player==null?null:Minecraft.getInstance().player.getUUID();
        forcedClip=null;frozenTime=-1;requested="";lastClock=Double.NaN;
    }
    public static void pose(String name,float time){forcedClip=name;frozenTime=time;requested="";lastClock=Double.NaN;}
    private static String base(){return ToeCpmPlugin.client!=null&&ToeCpmPlugin.client.getAnimationPlaying("toe_jog")==1?"jog":"idle";}
    private static String desired(){
        if(forcedClip!=null)return forcedClip;
        if(ToeCpmPlugin.client!=null){if(ToeCpmPlugin.client.getAnimationPlaying("toe_roll")==1)return "roll";if(ToeCpmPlugin.client.getAnimationPlaying("toe_sword")==1)return "sword";}
        return base();
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void render(RenderPlayerEvent.Pre event){
        if(!Boolean.getBoolean("toe.r31"))return;
        if(!event.getEntity().getUUID().equals(selected)){++otherPre;return;}
        if(!enabled||!(event.getEntity() instanceof AbstractClientPlayer player)||player.isSpectator()||player.isInvisible()||!player.getModelName().equals("default"))return;
        fallbackReason=equipmentReason(player);
        if(!fallbackReason.isEmpty()){
            ++equipmentFallbackFrames;
            if(!fallbackReason.equals(notifiedReason)){player.displayClientMessage(net.minecraft.network.chat.Component.literal("Renderlab: CPM/vanilla preserva "+fallbackReason+"; equipamento ainda nao homologado."),true);notifiedReason=fallbackReason;}
            return;
        }
        notifiedReason="";
        if(player.getPose()!=net.minecraft.world.entity.Pose.STANDING||player.isPassenger()||player.isFallFlying()
            ||player.isInWaterOrBubble()||player.isSprinting()||(forcedClip==null&&!player.onGround()))return;
        var mc=Minecraft.getInstance();
        // The local first-person body is not replaced. Vanilla/CPM hands remain the fallback.
        if(player==mc.player&&mc.options.getCameraType().isFirstPerson())return;
        String wanted=desired();
        // Idle/Jog/Sword draw the byte-identical approved R3 model. Only Roll opts into correctives.
        var modelId=System.getProperty("toe.r31.mode","r31").equals("r3")||!wanted.equals("roll")?new ResourceLocation("towerofentropy","r31_r3"):MODEL;
        var model=PolymeshApi.loadModel(modelId);
        if(model==null)return; // Missing assets: leave the approved renderer untouched.
        try{
            if(loaded!=model){loaded=model;renderer=new SkinRenderer(modelId);requested="";
                try(var in=mc.getResourceManager().open(new ResourceLocation("towerofentropy","r31-poses.json"))){poses=JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
            }
            var c=controller();boolean loop=wanted.equals("idle")||wanted.equals("jog");
            if(!requested.equals(wanted)){c.play("UAL_"+wanted,loop);c.setAnimationTime(0);requested=wanted;}
            double now=player.level().getGameTime()+event.getPartialTick();
            if(frozenTime>=0)c.setAnimationTime(frozenTime);
            else if(Double.isFinite(lastClock)&&now>lastClock)c.tick((float)Math.min(.25,(now-lastClock)/20));
            lastClock=now;
            if(frozenTime<0&&!loop&&!c.isPlaying()){c.play("UAL_"+base(),true);forcedClip=null;}
            Matrix4f[] globals=c.computeGlobalTransforms(0);
            skin=player.getSkinTextureLocation();lastYaw=Mth.rotLerp(event.getPartialTick(),player.yBodyRotO,player.yBodyRot);lastLight=event.getPackedLight();
            long start=System.nanoTime();var ps=event.getPoseStack();ps.pushPose();
            try{
                // glTF is Y-up. Reflect X to preserve the original Minecraft model handedness.
                ps.mulPose(Axis.YP.rotationDegrees(180-lastYaw));ps.scale(-.9375f,.9375f,.9375f);
                ps.last().pose().normal(ps.last().normal()); // exact inverse transpose for the handedness reflection
                renderer.renderModel(model,globals,ps.last(),event.getMultiBufferSource(),event.getPackedLight(),OverlayTexture.NO_OVERLAY,0xffffffff);
                if(!player.getMainHandItem().isEmpty())attachment(player,ps,event,c);
            }finally{ps.popPose();}
            long elapsed=System.nanoTime()-start;renderNanos+=elapsed;maxNanos=Math.max(maxNanos,elapsed);++drawCalls;
            // Cancel only after a complete successful draw; suppresses vanilla and CPM layers/items once.
            event.setCanceled(true);
        }catch(Exception ex){++errors;enabled=false;LogUtils.getLogger().error("TOE_R31 RENDER_DISABLED_FALLBACK",ex);}
    }
    private static JsonObject anchor(String clip,float time){
        var info=poses.getAsJsonObject(clip);if(info==null)info=poses.getAsJsonObject("idle");
        var frames=info.getAsJsonArray("frames");float duration=info.get("duration").getAsFloat()/1000;
        boolean loop=clip.equals("idle")||clip.equals("jog");float phase=loop?time%duration:Math.min(time,duration);
        int i=Math.min(frames.size()-1,(int)(phase/duration*(loop?frames.size():frames.size()-1)));
        for(var value:frames.get(i).getAsJsonObject().getAsJsonArray("components")){var a=value.getAsJsonObject();if(a.get("storeID").getAsInt()==303)return a;}
        throw new IllegalStateException("Original wrist anchor missing");
    }
    private static void attachment(AbstractClientPlayer player,PoseStack ps,RenderPlayerEvent.Pre e,AnimationController c){
        Matrix4f forearm=c.getNodeGlobalTransform("103",0);if(forearm==null)return;
        String name=c.getAnimationName().replace("UAL_","");var a=anchor(name,c.getAnimationTime());var p=a.getAsJsonObject("pos");var r=a.getAsJsonObject("rotation");
        ps.pushPose();try{
            ps.mulPoseMatrix(forearm);ps.scale(1,1,-1); // glTF bone axes -> original CPM forearm axes
            ps.translate(p.get("x").getAsFloat()/16,p.get("y").getAsFloat()/16,p.get("z").getAsFloat()/16);
            var q=new Quaternionf().rotationZYX(rad(r,"z"),rad(r,"y"),rad(r,"x"));ps.mulPose(q);
            ps.mulPose(Axis.XP.rotationDegrees(-90));ps.mulPose(Axis.YP.rotationDegrees(180));ps.translate(1/16f,.125,-.625);
            ps.last().pose().normal(ps.last().normal()); // mulPoseMatrix does not update normals in MC 1.20.1
            Minecraft.getInstance().getItemRenderer().renderStatic(player.getMainHandItem(),ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,e.getPackedLight(),OverlayTexture.NO_OVERLAY,ps,e.getMultiBufferSource(),player.level(),player.getId());
            ++attachmentDraws;
        }finally{ps.popPose();}
    }
    private static float rad(JsonObject r,String k){return (float)Math.toRadians(r.get(k).getAsFloat());}
    public static String equipmentReason(AbstractClientPlayer p){
        for(var item:p.getArmorSlots())if(!item.isEmpty())return "armadura";
        if(!p.getOffhandItem().isEmpty())return "offhand";
        if(p.getCloakTextureLocation()!=null&&p.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.CAPE))return "capa";
        return "";
    }
    @SubscribeEvent public static void post(RenderPlayerEvent.Post event){if(event.getEntity().getUUID().equals(selected))++localPosts;}
    @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true) public static void observe(RenderPlayerEvent.Pre event){if(!event.getEntity().getUUID().equals(selected)&&event.isCanceled())++otherCanceled;}
    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut e){enabled=false;selected=null;loaded=null;renderer=null;lastClock=Double.NaN;requested="";}
}
