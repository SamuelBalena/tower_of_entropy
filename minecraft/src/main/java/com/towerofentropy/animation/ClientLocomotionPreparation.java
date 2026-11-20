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

@Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT)
public final class ClientLocomotionPreparation {
    private static int ticks;
    private static boolean attempted;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || attempted || !Boolean.getBoolean("toe.locomotion.prepare")) return;
        if (++ticks < 100 || Minecraft.getInstance().getOverlay() != null) return;
        attempted = true;
        try { export(); } catch (Exception ex) { LogUtils.getLogger().error("TOE_M001_2 EXPORT_FAILED", ex); }
    }
    private static void export() throws Exception {
        Path folder = Path.of("../animation_pipeline/converted/m001_2");
        Editor editor = new Editor();
        editor.setUI(new ClientModelPreparation.LogUI());
        editor.loadDefaultPlayerModel();
        editor.project.load(folder.resolve("steve-ual1-locomotion.cpmproject").toFile()).join();
        ProjectIO.loadProject(editor, editor.project);
        editor.restitchTextures();
        if (editor.animations.size() != 2) throw new IllegalStateException("Expected only Idle/Jog");
        for (var anim : editor.animations) {
            boolean idle = anim.displayName.equals("toe_idle");
            if ((!idle && !anim.displayName.equals("toe_jog")) || anim.duration != (idle ? 2500 : 933)
                || anim.getFrames().size() != (idle ? 75 : 28) || !anim.loop || !anim.command)
                throw new IllegalStateException("Unexpected CPM locomotion animation");
        }
        var desc = new ModelDescription();
        desc.name = "TOE UAL1 Idle/Jog";
        desc.desc = "M001.2; Quaternius UAL1 CC0 retarget; opt-in locomotion";
        write(editor, folder, desc, "steve-ual1-locomotion");
        var skin = editor.textures.get(TextureSheetType.SKIN);
        try (var stream = Minecraft.getInstance().getResourceManager().open(new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png"))) {
            skin.setImage(Image.loadFrom(stream)); skin.setEdited(true); editor.restitchTextures();
        }
        write(editor, folder, desc, "steve-ual1-locomotion-preview");
        ProjectIO.saveProject(editor, editor.project);
        editor.project.save(folder.resolve("steve-ual1-locomotion-preview.cpmproject").toFile()).join();
        editor.textures.values().forEach(t -> t.free());
    }
    private static void write(Editor editor, Path folder, ModelDescription desc, String name) throws Exception {
        var file = folder.resolve(name + ".cpmmodel").toFile();
        Exporter.exportModel(editor, editor.ui, file, desc, false);
        var model = ModelFile.load(file);
        if (model.getDataBlock().length == 0) throw new IllegalStateException("Empty CPM model");
        Files.createDirectories(Path.of("player_models"));
        Files.copy(file.toPath(), Path.of("player_models/toe-" + name + ".cpmmodel"), StandardCopyOption.REPLACE_EXISTING);
        LogUtils.getLogger().info("TOE_M001_2 EXPORT_VALID name={} bytes={} animations=2 durations=2500,933", name, file.length());
    }
}
