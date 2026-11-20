package com.towerofentropy.packaged;
@net.minecraftforge.fml.common.Mod("toe_globalqa")
public final class PackagedProbe {
    public PackagedProbe(){
        var code=com.towerofentropy.client.PlayerRendererR3.class.getProtectionDomain().getCodeSource().getLocation();
        var asset=com.towerofentropy.client.PlayerRendererR3.class.getResource("/assets/towerofentropy/models/gltf/steve_a3_slim.glb");
        com.mojang.logging.LogUtils.getLogger().info("TOE_PACKAGED_SOURCE code={} asset={}",code,asset);
        if(!code.toString().contains(".jar")||asset==null||!asset.toString().contains(".jar"))throw new IllegalStateException("Packaged QA must load mod code and assets from the built JAR");
    }
}
