package com.towerofentropy.animation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import com.mojang.logging.LogUtils;
import com.tom.cpl.util.Image;
import com.tom.cpm.shared.editor.Editor;
import com.tom.cpm.shared.editor.Exporter;
import com.tom.cpm.shared.editor.project.ProjectIO;
import com.tom.cpm.shared.editor.util.ModelDescription;
import com.tom.cpm.shared.io.ModelFile;
import com.tom.cpm.shared.model.TextureSheetType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="towerofentropy",value=Dist.CLIENT)
public final class ClientActionPreparation {
    private static int ticks;private static boolean attempted;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||attempted||!Boolean.getBoolean("toe.actions.prepare"))return;
        if(++ticks<100||Minecraft.getInstance().getOverlay()!=null)return;
        attempted=true;try{export();}catch(Exception ex){LogUtils.getLogger().error("TOE_M001_3 EXPORT_FAILED",ex);}
    }
    private static void export()throws Exception{
        Path folder=Path.of("../animation_pipeline/converted/m001_3");Editor editor=new Editor();
        editor.setUI(new ClientModelPreparation.LogUI());editor.loadDefaultPlayerModel();
        editor.project.load(folder.resolve("steve-ual-actions.cpmproject").toFile()).join();
        ProjectIO.loadProject(editor,editor.project);editor.restitchTextures();
        if(editor.animations.size()!=4)throw new IllegalStateException("Expected four animations");
        var expected=java.util.Map.of("toe_idle",2500,"toe_jog",933,"toe_roll",1467,"toe_sword",433);
        for(var a:editor.animations){boolean action=a.displayName.equals("toe_roll")||a.displayName.equals("toe_sword");
            if(!expected.containsKey(a.displayName)||a.duration!=expected.get(a.displayName)||a.loop==action||!a.command)
                throw new IllegalStateException("Invalid CPM action metadata: "+a.displayName);
        }
        var desc=new ModelDescription();desc.name="TOE UAL visual actions";desc.desc="M001.3 visual only; UAL1/UAL2 Quaternius CC0";
        write(editor,folder,desc,"steve-ual-actions");
        var skin=editor.textures.get(TextureSheetType.SKIN);
        try(var stream=Minecraft.getInstance().getResourceManager().open(new ResourceLocation("minecraft","textures/entity/player/wide/steve.png"))){
            skin.setImage(Image.loadFrom(stream));skin.setEdited(true);editor.restitchTextures();
        }
        write(editor,folder,desc,"steve-ual-actions-preview");
        ProjectIO.saveProject(editor,editor.project);editor.project.save(folder.resolve("steve-ual-actions-preview.cpmproject").toFile()).join();
        editor.textures.values().forEach(t->t.free());
    }
    private static void write(Editor e,Path folder,ModelDescription desc,String name)throws Exception{
        var file=folder.resolve(name+".cpmmodel").toFile();Exporter.exportModel(e,e.ui,file,desc,false);
        var model=ModelFile.load(file);if(model.getDataBlock().length==0)throw new IllegalStateException("Empty model");
        Files.createDirectories(Path.of("player_models"));Files.copy(file.toPath(),Path.of("player_models/toe-"+name+".cpmmodel"),StandardCopyOption.REPLACE_EXISTING);
        LogUtils.getLogger().info("TOE_M001_3 EXPORT_VALID name={} bytes={} animations=4",name,file.length());
    }
}
