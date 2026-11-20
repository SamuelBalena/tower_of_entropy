package com.towerofentropy.animation;
import java.nio.file.*;
/** Bundled fallback cache, separate from user-authored CPM models. */
final class CompatibilityModels {
 static Path actions(boolean preview)throws Exception{
  String name="toe-steve-ual-actions"+(preview?"-preview":"")+".cpmmodel";
  Path path=net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("towerofentropy").resolve("compat").resolve(name);Files.createDirectories(path.getParent());
  // Refresh only our dedicated cache. Never overwrite player_models or custom skins.
  try(var input=CompatibilityModels.class.getResourceAsStream("/assets/towerofentropy/compat/"+name)){
   if(input==null)throw new IllegalStateException("Bundled compatibility model missing");Files.copy(input,path,StandardCopyOption.REPLACE_EXISTING);
  }return path;
 }
}
