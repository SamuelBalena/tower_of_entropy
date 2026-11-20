package com.towerofentropy.animation;

import java.nio.file.*;
import java.util.*;
import java.lang.reflect.Proxy;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import com.tom.cpl.util.Image;
import com.tom.cpm.shared.editor.Editor;
import com.tom.cpm.shared.editor.Exporter;
import com.tom.cpm.shared.editor.project.ProjectIO;
import com.tom.cpm.shared.editor.util.ModelDescription;
import com.tom.cpm.shared.io.ModelFile;
import com.tom.cpm.shared.model.TextureSheetType;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Opt-in lab source set only. No combat or production-controller changes. */
@Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT)
public final class R1Laboratory {
    private static volatile String MODE=System.getProperty("toe.r1.mode","baseline");
    private static final String[] CLIPS={"idle","jog","sword","roll"};
    private static final Path OUT=Path.of("../animation_pipeline/converted/m002_r1");
    private static final List<Map<String,Object>> RESULTS=new ArrayList<>();
    private static JsonObject poses;
    private static JsonArray active;
    private static int ticks,titleTicks,index=-1,quit;
    private static boolean exported,opening,done,fmPresent;
    private static boolean fmActive;
    private static int providerCalls;
    private static String activeClip="idle";
    private static boolean moving;
    private static float clock;
    private static volatile int generation;
    private static int serverGeneration=-1;
    private static boolean manualInitialized;
    private static volatile boolean manualStopped;
    private static long readyGameTime;
    private static long actionEnd;
    private static String manualAction;
    private static void exportModels() throws Exception {
        poses=JsonParser.parseString(Files.readString(OUT.resolve("poses.json"))).getAsJsonObject();
        try(var stream=Minecraft.getInstance().getResourceManager().open(new ResourceLocation("minecraft","textures/entity/player/wide/steve.png"))){Files.copy(stream,OUT.resolve("steve.png"),StandardCopyOption.REPLACE_EXISTING);}
        for(String variant:new String[]{"baseline","fixed"})for(String clip:CLIPS)export(variant+"-"+clip);
        export("steve-r1-fixed");
        try{Class.forName("net.fullmotion.api.FullMotion");fmPresent=true;registerFullMotion();}catch(ClassNotFoundException ignored){}
        record("export",true,"9 experimental CPM projects exported by installed CPM; FullMotion="+fmPresent);
    }
    private static void export(String name)throws Exception{
        Editor e=new Editor();e.setUI(new ClientModelPreparation.LogUI());e.loadDefaultPlayerModel();
        e.project.load(OUT.resolve(name+".cpmproject").toFile()).join();ProjectIO.loadProject(e,e.project);
        var skin=e.textures.get(TextureSheetType.SKIN);
        try(var stream=Minecraft.getInstance().getResourceManager().open(new ResourceLocation("minecraft","textures/entity/player/wide/steve.png"))){skin.setImage(Image.loadFrom(stream));skin.setEdited(true);e.restitchTextures();}
        var d=new ModelDescription();d.name="TOE R1 "+name;d.desc="Isolated surface experiment; UAL timings preserved";
        var file=OUT.resolve(name+".cpmmodel").toFile();Exporter.exportModel(e,e.ui,file,d,false);ModelFile.load(file);
        e.textures.values().forEach(t->t.free());
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!Boolean.getBoolean("toe.r1"))return;
        var mc=Minecraft.getInstance();
        if(done){if(++quit==40&&(Boolean.parseBoolean(System.getProperty("toe.r1.auto","true"))||Boolean.getBoolean("toe.r1.interactiveTest")))mc.stop();return;}
        if(!exported&&mc.getOverlay()==null&&++titleTicks>100){
            exported=true;try{exportModels();}catch(Exception ex){LogUtils.getLogger().error("TOE_R1 EXPORT_FAILED",ex);record("export",false,ex.toString());finish();return;}
        }
        if(!exported||poses==null)return;
        if(!opening&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){opening=true;mc.options.pauseOnLostFocus=false;mc.createWorldOpenFlows().loadLevel(mc.screen,"TOE_R1");}
        if(mc.player==null||mc.level==null||mc.getOverlay()!=null)return;
        ++ticks;if(ticks==20||Boolean.parseBoolean(System.getProperty("toe.r1.auto","true"))){mc.options.hideGui=true;mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);mc.options.fov().set(45);}
        mc.options.keyAttack.setDown(false);mc.options.keyUse.setDown(false);
        if(ticks==20){previewSkin(mc);server(p->{
            com.towerofentropy.combat.CombatController.disable(p);LocomotionController.stop(p);
            var level=p.serverLevel();p.teleportTo(level,0.5,100,.5,0,0);p.getAbilities().flying=false;p.setDeltaMovement(0,0,0);
            level.setDayTime(6000);
            for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)for(int y=99;y<=104;y++)level.setBlockAndUpdate(new net.minecraft.core.BlockPos(x,y,z),(y==99?net.minecraft.world.level.block.Blocks.STONE:net.minecraft.world.level.block.Blocks.AIR).defaultBlockState());
            for(var entity:level.getEntities((net.minecraft.world.entity.Entity)null,p.getBoundingBox().inflate(16)))if(!(entity instanceof Player))entity.discard();
            p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));
        });}
        if(ticks<80)return;
        if(!Boolean.parseBoolean(System.getProperty("toe.r1.auto","true"))){
            if(!manualInitialized){manualInitialized=true;select(MODE,"idle",true);mc.options.hideGui=false;}
            if(Boolean.getBoolean("toe.r1.interactiveTest"))interactiveTest(mc);
            if(moving&&!manualStopped){clock+=.05f;var info=poses.getAsJsonObject(activeClip);var frames=info.getAsJsonArray("frames");float duration=info.get("duration").getAsFloat()/1000;boolean loop=activeClip.equals("idle")||activeClip.equals("jog");if(!loop&&fmActive&&clock>=duration){select(MODE,"idle",true);return;}float t=loop?clock%duration:Math.min(clock,duration);int n=Math.min(frames.size()-1,(int)(t/duration*(loop?frames.size():frames.size()-1)));active=frames.get(n).getAsJsonObject().getAsJsonArray("components");}
            return;
        }
        int next=(ticks-80)/80,phase=(ticks-80)%80;
        if(next>=4){finish();return;}
        if(index!=next){index=next;activeClip=CLIPS[index];active=poses.getAsJsonObject(activeClip).getAsJsonObject("frame").getAsJsonArray("components");clock=0;try{apply(activeClip);}catch(Exception ex){record(activeClip,false,ex.toString());}}
        if(phase==35||phase==55){
            var state=LocomotionController.status(mc.player.getUUID());
            boolean custom=com.tom.cpm.shared.MinecraftClientAccess.get().getCurrentClientPlayer().getModelDefinition()!=null;
            int pose=ToeCpmPlugin.client.getAnimationPlaying("toe_r1");
            record(activeClip+"_"+phase,state==null&&(MODE.equals("fullmotion")?fmActive&&providerCalls>0:custom&&pose==1),"CPM definition="+custom+" pose="+pose+" FM="+fmActive+" providerCalls="+providerCalls+" skin="+mc.player.getSkinTextureLocation()+" model="+mc.player.getModelName()+" pos="+mc.player.position());
            Screenshot.grab(mc.gameDirectory,"r1-"+MODE+"-"+activeClip+"-"+phase+".png",mc.getMainRenderTarget(),msg->{});
        }
        if(phase==45){mc.player.setYRot(-55);mc.player.setYHeadRot(-55);mc.player.yBodyRot=-55;} // second view, same frozen pose
        if(phase==70){mc.player.setYRot(0);mc.player.setYHeadRot(0);mc.player.yBodyRot=0;}
    }
    private static void apply(String clip)throws Exception{
        ++generation;
        boolean useFm=MODE.equals("fullmotion")||MODE.equals("conflict");fmActive=useFm;
        providerCalls=0;
        if(useFm&&!fmPresent)throw new IllegalStateException("Full Motion not loaded");
        if(fmPresent)Class.forName("net.fullmotion.client.FullMotionConfig").getField("enabled").setBoolean(null,useFm);
        server(p->{try{
            readyGameTime=p.serverLevel().getGameTime()+12;
            manualAction=null;
            for(String old:new String[]{"toe_sword","toe_roll"})if(ToeCpmPlugin.common.getAnimationPlaying(Player.class,p,old)>=0)ToeCpmPlugin.common.playAnimation(Player.class,p,old,0);
            if(MODE.equals("fullmotion"))ToeCpmPlugin.common.resetPlayerModel(Player.class,p);
            else {
                String variant=MODE.equals("fixed")?"fixed":"baseline";
                Path file=moving?(variant.equals("fixed")?OUT.resolve("steve-r1-fixed.cpmmodel"):Path.of("player_models/toe-steve-ual-actions-preview.cpmmodel")):OUT.resolve(variant+"-"+clip+".cpmmodel");
                ToeCpmPlugin.common.setPlayerModel(Player.class,p,ModelFile.load(file.toFile()),false);
            }
        }catch(Exception ex){LogUtils.getLogger().error("TOE_R1 APPLY_FAILED",ex);}});
        // Pose commands may become ready later; handled by the server observer below.
    }
    @Mod.EventBusSubscriber(modid="towerofentropy")
    public static final class ServerPose {
        @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
            if(!Boolean.getBoolean("toe.r1")||manualStopped||MODE.equals("fullmotion")||e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p))return;
            if(ToeCpmPlugin.common==null)return;
            if(manualAction!=null&&p.serverLevel().getGameTime()>=actionEnd){ToeCpmPlugin.common.playAnimation(Player.class,p,manualAction,0);manualAction=null;}
            if(!moving){if(ToeCpmPlugin.common.getAnimationPlaying(Player.class,p,"toe_r1")==0)ToeCpmPlugin.common.playAnimation(Player.class,p,"toe_r1",1);}
            else if(serverGeneration!=generation&&p.serverLevel().getGameTime()>=readyGameTime){String name="toe_"+activeClip;
                if(ToeCpmPlugin.common.getAnimationPlaying(Player.class,p,name)>=0){
                    if(activeClip.equals("sword")||activeClip.equals("roll"))ToeCpmPlugin.common.playAnimation(Player.class,p,"toe_idle",1);
                    ToeCpmPlugin.common.playAnimation(Player.class,p,name,1);serverGeneration=generation;
                    if(activeClip.equals("sword")||activeClip.equals("roll")){manualAction=name;actionEnd=p.serverLevel().getGameTime()+(activeClip.equals("sword")?11:32);}
                }
            }
        }
    }
    private static void interactiveTest(Minecraft mc){
        if(ticks==100||ticks==160||ticks==220||ticks==280){String clip=ticks==100?"idle":ticks==160?"jog":ticks==220?"sword":"roll";mc.player.connection.sendCommand("toe_r1 fixed "+clip);}
        if(ticks==140||ticks==200||ticks==237||ticks==300){String clip=ticks==140?"idle":ticks==200?"jog":ticks==237?"sword":"roll";int value=ToeCpmPlugin.client.getAnimationPlaying("toe_"+clip);record("interactive_"+clip,value==1,"native CPM command="+value);}
        if(ticks==255||ticks==335){String clip=ticks==255?"sword":"roll";int value=ToeCpmPlugin.client.getAnimationPlaying("toe_"+clip);record("interactive_end_"+clip,value==0,"bounded one-shot parameter="+value);}
        if(ticks==350)mc.player.connection.sendCommand("toe_r1 off");
        if(ticks==390){int value=ToeCpmPlugin.client.getAnimationPlaying("toe_idle");record("interactive_off",value<0,"CPM reset="+value);finish();}
    }
    public static void select(String mode,String clip,boolean play){
        Minecraft.getInstance().execute(()->{try{manualStopped=false;MODE=mode;activeClip=clip;moving=play;clock=0;active=poses.getAsJsonObject(clip).getAsJsonObject("frame").getAsJsonArray("components");apply(clip);}catch(Exception ex){record("manual_select",false,ex.toString());}});
    }
    @Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT)
    public static final class Commands {
        @SubscribeEvent public static void register(net.minecraftforge.event.RegisterCommandsEvent event){
            var root=net.minecraft.commands.Commands.literal("toe_r1").requires(s->s.hasPermission(2));
            root.then(net.minecraft.commands.Commands.literal("off").executes(c->{Minecraft.getInstance().execute(()->{try{manualStopped=true;fmActive=false;if(fmPresent)Class.forName("net.fullmotion.client.FullMotionConfig").getField("enabled").setBoolean(null,false);server(p->ToeCpmPlugin.common.resetPlayerModel(Player.class,p));}catch(Exception ex){record("manual_off",false,ex.toString());}});return 1;}));
            for(String mode:new String[]{"baseline","fixed","fullmotion","conflict"}){
                var branch=net.minecraft.commands.Commands.literal(mode);
                for(String clip:CLIPS)branch.then(net.minecraft.commands.Commands.literal(clip).executes(c->{select(mode,clip,true);return 1;})
                    .then(net.minecraft.commands.Commands.literal("freeze").executes(c->{select(mode,clip,false);return 1;})));
                root.then(branch);
            }
            event.getDispatcher().register(root);
        }
    }
    @SuppressWarnings("unchecked") private static void previewSkin(Minecraft mc){
        try{var info=mc.getConnection().getPlayerInfo(mc.player.getUUID());
            var map=info.getClass().getDeclaredField("textureLocations");map.setAccessible(true);
            ((Map<Object,Object>)map.get(info)).put(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN,new ResourceLocation("minecraft","textures/entity/player/wide/steve.png"));
            var model=info.getClass().getDeclaredField("skinModel");model.setAccessible(true);model.set(info,"default");
            var pending=info.getClass().getDeclaredField("pendingTextures");pending.setAccessible(true);pending.setBoolean(info,true);
        }catch(Exception ex){record("local_steve_preview",false,ex.toString());}
    }
    private static JsonObject component(int id){for(var v:active)if(v.getAsJsonObject().get("storeID").getAsInt()==id)return v.getAsJsonObject();return null;}
    private static float angle(JsonObject c,String key){float a=c.getAsJsonObject("rotation").get(key).getAsFloat();return (float)Math.toRadians(a>180?a-360:a);}
    private static void registerFullMotion()throws Exception{
        Class<?> api=Class.forName("net.fullmotion.api.FullMotion"),provider=Class.forName("net.fullmotion.api.PoseProvider");
        Object proxy=Proxy.newProxyInstance(provider.getClassLoader(),new Class[]{provider},(o,m,a)->{
            return switch(m.getName()){
                case "isActive" -> fmActive&&active!=null&&a[0]==Minecraft.getInstance().player;
                case "body" -> new Quaternionf();
                case "offset" -> new Vector3f();
                case "movesTorso" -> true;
                case "limbs" -> {++providerCalls;float[][] limbs=new float[4][];int[] ids={3,2,5,4};for(int i=0;i<4;i++){var c=component(ids[i]);var low=component(ids[i]+100);limbs[i]=new float[]{angle(c,"x"),angle(c,"y"),angle(c,"z"),angle(low,"x"),0};}yield limbs;}
                case "adjustModel" -> {var model=(HumanoidModel<?>)a[1];int[] ids={0,1,3,2,5,4};var parts=new net.minecraft.client.model.geom.ModelPart[]{model.head,model.body,model.rightArm,model.leftArm,model.rightLeg,model.leftLeg};for(int i=0;i<ids.length;i++){var c=component(ids[i]);var p=c.getAsJsonObject("pos");parts[i].setPos(p.get("x").getAsFloat(),p.get("y").getAsFloat(),p.get("z").getAsFloat());parts[i].setRotation(angle(c,"x"),angle(c,"y"),angle(c,"z"));}model.hat.copyFrom(model.head);yield null;}
                case "toString" -> "TOE_R1_UAL_PoseProvider";
                case "hashCode" -> System.identityHashCode(o);
                case "equals" -> o==a[0];
                default -> null;
            };
        });
        api.getMethod("registerPose",ResourceLocation.class,int.class,provider).invoke(null,new ResourceLocation("towerofentropy","r1_ual"),10000,proxy);
    }
    private static void server(java.util.function.Consumer<ServerPlayer> action){var mc=Minecraft.getInstance();var s=mc.getSingleplayerServer();var id=mc.player.getUUID();s.execute(()->{var p=s.getPlayerList().getPlayer(id);if(p!=null)action.accept(p);});}
    private static void record(String test,boolean pass,String detail){RESULTS.add(Map.of("test",test,"pass",pass,"detail",detail));LogUtils.getLogger().info("TOE_R1 CHECK {} pass={} {}",test,pass,detail);}
    private static void finish(){
        try{Files.writeString(Path.of("../.logs/r1-"+MODE+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(RESULTS));}catch(Exception ex){LogUtils.getLogger().error("TOE_R1 REPORT_FAILED",ex);}
        LogUtils.getLogger().info("TOE_R1 DONE mode={} checks={}",MODE,RESULTS.size());done=true;
    }
}
