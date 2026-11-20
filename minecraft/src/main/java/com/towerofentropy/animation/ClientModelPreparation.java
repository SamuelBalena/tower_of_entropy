package com.towerofentropy.animation;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Function;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import com.tom.cpl.gui.Frame;
import com.tom.cpl.gui.UI;
import com.tom.cpl.gui.elements.PopupPanel;
import com.tom.cpm.shared.editor.Editor;
import com.tom.cpm.shared.editor.Exporter;
import com.tom.cpm.shared.editor.project.ProjectIO;
import com.tom.cpm.shared.editor.util.ModelDescription;
import com.tom.cpm.shared.io.ModelFile;
import com.tom.cpm.shared.model.TextureSheetType;
import com.tom.cpl.util.Image;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Dev-only export: uses the installed CPM editor/serializer, not a guessed model binary. */
@Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT)
public final class ClientModelPreparation {
    private static final Logger LOG = LogUtils.getLogger();
    private static int ticks;
    private static boolean attempted;

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || attempted || !Boolean.getBoolean("toe.animtest.prepare")) return;
        if (++ticks < 100 || Minecraft.getInstance().getOverlay() != null) return;
        attempted = true;
        try { prepare(); } catch (Exception ex) { LOG.error("TOE_M001_1 EXPORT_FAILED", ex); }
    }

    private static void prepare() throws Exception {
        Path folder = Path.of("../animation_pipeline/converted/m001_1");
        File project = folder.resolve("steve-ual1-idle.cpmproject").toFile();
        if (!project.isFile()) throw new IllegalStateException("Run retarget-idle.cjs first");
        Editor editor = new Editor();
        editor.setUI(new LogUI());
        editor.loadDefaultPlayerModel();
        editor.project.load(project).join();
        ProjectIO.loadProject(editor, editor.project);
        editor.restitchTextures();
        if (editor.animations.size() != 1 || editor.animations.get(0).duration != 2500 || !editor.animations.get(0).loop
            || editor.animations.get(0).getFrames().size() != 75)
            throw new IllegalStateException("CPM project animation validation failed");
        ModelDescription desc = new ModelDescription();
        desc.name = "TOE Steve - UAL1 Idle";
        desc.desc = "M001.1 experimental retarget; UAL1 Quaternius CC0; 2.5 seconds";
        File exported = folder.resolve("steve-ual1-idle.cpmmodel").toFile();
        Exporter.exportModel(editor, editor.ui, exported, desc, false);
        ModelFile parsed = ModelFile.load(exported);
        if (parsed.getDataBlock().length == 0) throw new IllegalStateException("Empty exported model");
        Files.createDirectories(Path.of("player_models"));
        Files.copy(exported.toPath(), Path.of("player_models/toe-steve-ual1-idle.cpmmodel"), StandardCopyOption.REPLACE_EXISTING);
        LOG.info("TOE_M001_1 EXPORT_VALID project={} frames=75 duration=2500 parts={} modelBytes={} dataBytes={}",
            project, editor.elements.size(), exported.length(), parsed.getDataBlock().length);
        // Separate local preview only; the primary model above retains the player's skin.
        var skin = editor.textures.get(TextureSheetType.SKIN);
        try (var stream = Minecraft.getInstance().getResourceManager().open(
                new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png"))) {
            skin.setImage(Image.loadFrom(stream));
            skin.setEdited(true);
            editor.restitchTextures();
        }
        File preview = folder.resolve("steve-ual1-idle-preview.cpmmodel").toFile();
        Exporter.exportModel(editor, editor.ui, preview, desc, false);
        ModelFile.load(preview);
        Files.copy(preview.toPath(), Path.of("player_models/toe-steve-ual1-idle-preview.cpmmodel"), StandardCopyOption.REPLACE_EXISTING);
        com.tom.cpm.shared.editor.project.ProjectIO.saveProject(editor, editor.project);
        editor.project.save(folder.resolve("steve-ual1-idle-preview.cpmproject").toFile()).join();
        LOG.info("TOE_M001_1 STEVE_PREVIEW_VALID modelBytes={}", preview.length());
        editor.textures.values().forEach(t -> t.free());
    }

    static final class LogUI implements UI {
        @Override public String i18nFormat(String key, Object... args) { return key; }
        @Override public void displayMessagePopup(String title, String text, String close) {
            if (title.contains("error")) throw new IllegalStateException(title + ": " + text);
            LOG.info("TOE_M001_1 CPM export: {} {}", title, text);
        }
        @Override public void executeLater(Runnable action) { Minecraft.getInstance().execute(action); }
        @Override public void displayPopup(Function<Frame, PopupPanel> factory) { throw new IllegalStateException("CPM export requires interactive popup / overflow"); }
        @Override public void displayConfirm(String title, String msg, Runnable ok, Runnable cancel, String yes, String no) { throw new IllegalStateException("Unexpected CPM confirmation"); }
        @Override public void onGuiException(String msg, Throwable error, boolean fatal) { throw new IllegalStateException(msg, error); }
    }
}
