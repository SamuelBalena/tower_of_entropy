package com.towerofentropy.animation;
import java.nio.file.*;
import com.tom.cpl.util.Image;
import com.tom.cpm.shared.editor.*;
import com.tom.cpm.shared.editor.project.ProjectIO;
import com.tom.cpm.shared.editor.util.ModelDescription;
import com.tom.cpm.shared.model.TextureSheetType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
/** Exports isolated original CPM poses with the installed official CPM serializer. */
public final class R31CpmPreparation {
 public static void export()throws Exception{
  var out=Path.of("../animation_pipeline/converted/m002_r3_1");
  for(int frame:new int[]{4,11,15,23,26,27,30}){
   String name="baseline-roll-"+frame;Editor e=new Editor();e.setUI(new ClientModelPreparation.LogUI());e.loadDefaultPlayerModel();e.project.load(out.resolve(name+".cpmproject").toFile()).join();ProjectIO.loadProject(e,e.project);
   var skin=e.textures.get(TextureSheetType.SKIN);try(var in=Minecraft.getInstance().getResourceManager().open(new ResourceLocation("minecraft","textures/entity/player/wide/steve.png"))){skin.setImage(Image.loadFrom(in));skin.setEdited(true);e.restitchTextures();}
   var d=new ModelDescription();d.name="R3.1 original Roll frame "+frame;d.desc="Unchanged original CPM frame; private comparison";Exporter.exportModel(e,e.ui,out.resolve(name+".cpmmodel").toFile(),d,false);e.textures.values().forEach(t->t.free());
  }
 }
}
