package com.towerofentropy.client.studio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

final class AnimationStudioScreen extends Screen {
    private static final int TEXT = 0xffeee7da, MUTED = 0xffb4aca0, GOLD = 0xffe4bd77, PANEL = 0xff242529;
    private final Screen previous;
    private final StudioViewport viewport = new StudioViewport();
    private StudioRig rig;
    private StudioSession session;
    private StudioFiles files;
    private StudioRig.Bone selected;
    private StudioProject.Key selectedKey;
    private final List<EditBox> transforms = new ArrayList<>();
    private boolean slim, loading, timelineDrag, keyDrag, dirtyGizmo;
    private int axis = -1, left, right, bottom, treeScroll, trackScroll, fieldStep;
    private double dragFrame;
    private String currentFile, status = "";
    private StudioProject.Curve curve = StudioProject.Curve.LINEAR;

    AnimationStudioScreen(Screen previous) {
        super(Component.literal("Tower Animation Studio"));
        this.previous = previous;
    }

    @Override protected void init() {
        left = Math.max(90, Math.min(155, width / 5));
        right = width - Math.max(96, Math.min(167, width / 5));
        bottom = height - Math.max(100, Math.min(158, height / 3));
        transforms.clear();
        if (files == null) {
            files = new StudioFiles(minecraft.gameDirectory.toPath());
            try {
                installRig(minecraft.player != null && minecraft.player.getModelName().equals("slim"), true);
                newProject();
            } catch (RuntimeException | LinkageError error) {
                status = "Polymesh R3 indisponível";
                com.mojang.logging.LogUtils.getLogger().warn("Could not open Animation Studio", error);
            }
        }
        button(6, 19, 35, "Novo", () -> discard(() -> { newProject(); refresh(); }));
        button(43, 19, 38, "Abrir", this::openManager);
        button(83, 19, 42, "Salvar", () -> save(false));
        button(127, 19, 52, "Salvar +", () -> save(true));
        boolean compact = width < 355;
        button(181, 19, compact ? 30 : 49, compact ? "Undo" : "Desfazer", () -> history(false));
        button(compact ? 213 : 232, 19, compact ? 30 : 43, compact ? "Redo" : "Refazer", () -> history(true));
        button(compact ? 245 : 277, 19, 42, "Ajustes", this::settings);
        button(width - 35, 19, 29, "Sair", this::onClose);
        if (session == null) return;
        int toolWidth = Math.min(45, (right - left - 8) / 3);
        button(left + 2, 41, toolWidth, "Ossos", () -> viewport.overlay = !viewport.overlay);
        button(left + 4 + toolWidth, 41, toolWidth, "Câmera", viewport::reset);
        button(left + 6 + toolWidth * 2, 41, toolWidth, "Foco", () -> viewport.focus(selected, (left + right) / 2f, (bottom + 59) / 2f));
        int fieldWidth = Math.min(91, width - right - 29);
        fieldStep = Math.max(10, Math.min(15, (bottom - 78) / 6));
        for (int i = 0; i < 6; i++) {
            var field = new EditBox(font, right + 22, 63 + i * fieldStep, fieldWidth, fieldStep, Component.literal(i < 3 ? "Rotação" : "Translação"));
            field.setMaxLength(12);
            field.setFilter(value -> value.matches("[-+0-9.,]*"));
            field.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
                    i < 3 ? "Rotação local em graus. Enter aplica." : "Translação da raiz. Enter aplica.")));
            transforms.add(addRenderableWidget(field));
        }
        button(right + 6, bottom - 17, 39, "Osso", () -> resetPose(false));
        button(right + 47, bottom - 17, 41, "Pose", () -> resetPose(true));
        int row = bottom + 4;
        String[] labels = {"|<", "Play", "Stop", ">|", "Loop", "Vel.", "+ Key", "Auto", "Curva", "Apagar"};
        int[] sizes = {24, 38, 34, 24, 38, 38, 43, 38, 52, 44};
        Runnable[] actions = {() -> seek(0), this::togglePlayback, () -> seek(0),
                () -> seek(session.project.frames()), this::toggleLoop, () -> session.speed = (session.speed + 1) % StudioSession.SPEEDS.length,
                this::addKey, () -> session.autoKey = !session.autoKey, this::cycleCurve, this::deleteKey};
        int position = 6;
        for (int i = 0; i < labels.length; i++) {
            int size = sizes[i] - Math.max(0, (403 - width + 9) / 10);
            button(position, row, size, font.plainSubstrByWidth(labels[i], size - 3), actions[i]);
            position += size + 2;
        }
        refresh();
    }

    private void button(int x, int y, int w, String label, Runnable action) {
        addRenderableWidget(new StudioButton(x, y, w, 15, label, b -> { if (!loading) action.run(); }));
    }

    private void installRig(boolean slim, boolean currentSkin) {
        var skin = currentSkin && minecraft.player != null ? minecraft.player.getSkinTextureLocation()
                : new ResourceLocation("minecraft", "textures/entity/player/" + (slim ? "slim" : "wide") + "/steve.png");
        rig = new StudioRig(slim, skin);
        this.slim = slim;
        selected = rig.bones.get(0);
    }

    private void newProject() {
        if (rig == null) return;
        session = new StudioSession(new StudioProject(1, "Sem título", rig.resource.toString(), rig.signature,
                false, 30, 30, false, List.of()));
        selectedKey = null;
        currentFile = null;
        treeScroll = trackScroll = 0;
        status = "Novo projeto";
    }

    @Override public boolean isPauseScreen() { return true; }
    @Override public void removed() {
        if (session != null) session.playing = false;
        axis = -1;
        keyDrag = timelineDrag = false;
    }
    @Override public void onClose() { if (!loading) discard(() -> minecraft.setScreen(previous)); }
    private void discard(Runnable action) {
        if (session != null && session.dirty()) {
            confirm("Descartar alterações?", "O projeto tem alterações ainda não salvas.", action);
        } else action.run();
    }
    private void confirm(String title, String detail, Runnable yes) {
        minecraft.setScreen(new ConfirmScreen(answer -> {
            minecraft.setScreen(this);
            if (answer) yes.run();
        }, Component.literal(title), Component.literal(detail)));
    }

    private void refresh() {
        if (session == null || transforms.isEmpty()) return;
        var transform = session.transform(selected.id());
        var angles = transform.rotation().getEulerAnglesXYZ(new Vector3f()).mul((float) (180 / Math.PI));
        float[] values = {angles.x, angles.y, angles.z, transform.x(), transform.y(), transform.z()};
        for (int i = 0; i < transforms.size(); i++) {
            transforms.get(i).setValue(String.format(Locale.ROOT, "%.5f", values[i]));
            transforms.get(i).active = i < 3 || selected.parent() < 0;
            transforms.get(i).visible = transforms.get(i).active;
        }
    }

    private void applyFields() {
        var values = new float[6];
        try {
            for (int i = 0; i < values.length; i++) values[i] = Float.parseFloat(transforms.get(i).getValue().replace(',', '.'));
            var delta = new StudioProject.Transform(values[3], values[4], values[5], 0, 0, 0, 1)
                    .rotate(new Quaternionf().rotationXYZ((float) Math.toRadians(values[0]),
                            (float) Math.toRadians(values[1]), (float) Math.toRadians(values[2])));
            if (!delta.valid(selected.parent() < 0)) throw new NumberFormatException();
            session.checkpoint();
            session.edit(selected.id(), delta);
            status = session.autoKey ? "Chave atualizada" : "Pose pendente · + Key registra";
        } catch (NumberFormatException invalid) { status = "Transformação inválida"; }
        refresh();
    }

    private void select(StudioRig.Bone bone) {
        selected = bone;
        selectedKey = null;
        int index = rig.bones.indexOf(bone);
        trackScroll = Math.max(0, Math.min(index, rig.bones.size() - trackRows()));
        refresh();
    }
    private void seek(double target) {
        if (!session.edits.isEmpty()) status = "Pose anterior registrada como chave";
        session.seek(target);
        refresh();
    }
    private void togglePlayback() {
        if (!session.edits.isEmpty()) status = "Pose anterior registrada como chave";
        session.toggle();
        refresh();
    }
    private void history(boolean redo) {
        if (session == null) return;
        session.history(redo);
        selectedKey = null;
        status = redo ? "Histórico · refazer" : "Histórico · desfazer";
        refresh();
    }
    private void addKey() {
        session.checkpoint();
        if (session.edits.isEmpty()) session.key(selected.id(), curve);
        else for (String id : List.copyOf(session.edits.keySet())) session.key(id, curve);
        selectedKey = keyAt(selected.id(), (int) Math.round(session.frame));
        status = "Chave registrada";
    }
    private StudioProject.Key keyAt(String bone, int frame) {
        return session.project.keys().stream().filter(key -> key.frame() == frame && key.bone().equals(bone)).findFirst().orElse(null);
    }
    private void deleteKey() {
        if (selectedKey == null || !session.project.keys().contains(selectedKey)) return;
        session.checkpoint();
        session.project = session.project.replace(selectedKey, null);
        selectedKey = null;
        refresh();
    }
    private void toggleLoop() {
        var p = session.project;
        session.checkpoint();
        session.project = p.settings(p.name(), p.frames(), p.fps(), !p.loop(), p.idle());
    }
    private void cycleCurve() {
        curve = StudioProject.Curve.values()[(curve.ordinal() + 1) % StudioProject.Curve.values().length];
        if (selectedKey != null && session.project.keys().contains(selectedKey)) {
            session.checkpoint();
            var next = new StudioProject.Key(selectedKey.bone(), selectedKey.frame(), selectedKey.transform(), curve);
            session.project = session.project.replace(selectedKey, next);
            selectedKey = next;
        }
    }
    private void resetPose(boolean all) {
        session.checkpoint();
        if (all) for (var bone : rig.bones) session.edit(bone.id(), StudioProject.Transform.IDENTITY);
        else session.edit(selected.id(), StudioProject.Transform.IDENTITY);
        refresh();
    }

    @Override public void render(GuiGraphics gui, int mx, int my, float partial) {
        gui.fill(0, 0, width, height, 0xff171719);
        gui.fill(0, 0, width, 38, PANEL);
        gui.drawString(font, title, 6, 5, GOLD);
        if (session != null) {
            session.advance();
            String name = session.project.name() + (session.dirty() ? " *" : "");
            gui.drawString(font, font.plainSubstrByWidth(name, Math.max(0, width - 147)), 141, 5, TEXT);
            gui.fill(3, 40, left - 2, bottom - 1, PANEL);
            gui.fill(right + 2, 40, width - 3, bottom - 1, PANEL);
            if (right > left + 2 && bottom > 61) viewport.render(gui, rig, session, selected, left, 58, right, bottom);
            drawTree(gui);
            drawInspector(gui);
            drawTimeline(gui);
        }
        gui.drawString(font, font.plainSubstrByWidth(status, width - 12), 6, height - 10, GOLD);
        super.render(gui, mx, my, partial);
    }

    private int treeRows() { return Math.max(1, (bottom - 53) / 12); }
    private int trackRows() { return Math.max(1, (height - bottom - 57) / 12); }
    private void drawTree(GuiGraphics gui) {
        gui.drawString(font, "ESQUELETO", 8, 43, GOLD);
        gui.enableScissor(3, 53, left - 2, bottom);
        for (int i = 0; i < treeRows() && treeScroll + i < rig.bones.size(); i++) {
            var bone = rig.bones.get(treeScroll + i);
            int y = 54 + i * 12;
            int x = 8 + bone.depth() * 5;
            if (bone.equals(selected)) gui.fill(5, y - 1, left - 4, y + 10, 0xff594a34);
            gui.drawString(font, boneLabel(bone, left - x - 4), x, y, TEXT);
        }
        gui.disableScissor();
    }
    private void drawInspector(GuiGraphics gui) {
        gui.drawString(font, "TRANSFORMAÇÃO", right + 6, 43, GOLD);
        gui.drawString(font, font.plainSubstrByWidth(selected.label(), width - right - 13), right + 6, 56, TEXT);
        for (int i = 0; i < 3; i++) {
            gui.drawString(font, "R" + "XYZ".charAt(i), right + 7, 64 + i * fieldStep, StudioViewport.AXIS_COLORS[i]);
            if (selected.parent() < 0)
                gui.drawString(font, "T" + "XYZ".charAt(i), right + 7, 64 + (i + 3) * fieldStep, MUTED);
        }
        if (selected.parent() >= 0 && bottom >= 136) {
            var parent = rig.bones.stream().filter(bone -> bone.index() == selected.parent()).findFirst().orElseThrow();
            gui.drawString(font, "Pai · " + boneLabel(parent, width - right - 34), right + 7, 66 + 3 * fieldStep, MUTED);
            gui.drawString(font, "Enter aplica", right + 7, 78 + 3 * fieldStep, MUTED);
        }
    }
    private String boneLabel(StudioRig.Bone bone, int available) {
        String label = bone.label();
        if (font.width(label) > available) label = label.replace(" esquerdo", " E").replace(" direito", " D");
        return font.plainSubstrByWidth(label, available);
    }
    private int trackX() { return Math.min(118, left); }
    private int frameX(double frame) {
        return trackX() + (int) Math.round(frame / session.project.frames() * Math.max(1, width - 11 - trackX()));
    }
    private int frameAt(double x) {
        return (int) Math.round(Math.max(0, Math.min(1, (x - trackX()) / Math.max(1, width - 11 - trackX()))) * session.project.frames());
    }
    private void drawTimeline(GuiGraphics gui) {
        gui.fill(3, bottom, width - 3, height - 12, PANEL);
        String summary = String.format(Locale.ROOT, "%.2f / %.2fs  ·  %d fps  ·  %.2f×  ·  %s  ·  Loop %s  ·  Auto %s  ·  %s",
                session.frame / session.project.fps(), (double) session.project.frames() / session.project.fps(),
                session.project.fps(), StudioSession.SPEEDS[session.speed], session.playing ? "Reproduzindo" : "Pausa",
                session.project.loop() ? "✓" : "—", session.autoKey ? "✓" : "—", curve.name());
        gui.drawString(font, font.plainSubstrByWidth(summary, width - 12), 6, bottom + 21, GOLD);
        int top = bottom + 34;
        for (int i = 0; i <= 4; i++) {
            int x = frameX(session.project.frames() * i / 4.0);
            String value = String.format(Locale.ROOT, "%.2f", (double) session.project.frames() * i / (4 * session.project.fps()));
            gui.drawString(font, value, Math.min(x, width - 8 - font.width(value)), top, MUTED);
        }
        for (int row = 0; row < trackRows() && trackScroll + row < rig.bones.size(); row++) {
            var bone = rig.bones.get(trackScroll + row);
            int y = top + 11 + row * 12;
            gui.fill(6, y - 1, width - 7, y + 10, bone.equals(selected) ? 0xff3e3528 : 0xff2d2e33);
            gui.drawString(font, boneLabel(bone, trackX() - 13), 8, y, TEXT);
            for (var key : session.project.keys()) if (key.bone().equals(bone.id())) {
                int x = frameX(keyDrag && key.equals(selectedKey) ? dragFrame : key.frame());
                gui.fill(x - 3, y + 1, x + 4, y + 8, key.equals(selectedKey) ? 0xffffe4a8 : 0xffbb9155);
            }
        }
        int play = frameX(session.frame);
        gui.fill(play, top + 9, play + 1, height - 13, GOLD);
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (loading) return true;
        if (super.mouseClicked(x, y, button)) return true;
        if (session == null || button != 0) return false;
        if (x < left && y >= 54 && y < bottom) {
            int i = treeScroll + ((int) y - 54) / 12;
            if (i < rig.bones.size()) select(rig.bones.get(i));
            return true;
        }
        if (x >= left && x < right && y >= 59 && y < bottom) {
            axis = viewport.axis(x, y);
            dirtyGizmo = false;
            if (axis >= 0) return true;
            int joint = viewport.pick(x, y);
            for (var bone : rig.bones) if (bone.index() == joint) { select(bone); break; }
            return true;
        }
        if (y >= bottom + 34 && y < height - 12) {
            int row = y < bottom + 45 ? -1 : ((int) y - bottom - 45) / 12;
            if (row >= 0 && trackScroll + row < rig.bones.size()) {
                var bone = rig.bones.get(trackScroll + row);
                selected = bone;
                selectedKey = null;
                var key = session.project.keys().stream().filter(candidate -> candidate.bone().equals(bone.id())
                                && Math.abs(frameX(candidate.frame()) - x) < 6)
                        .min(java.util.Comparator.comparingDouble(candidate -> Math.abs(frameX(candidate.frame()) - x))).orElse(null);
                if (key != null) {
                    seek(key.frame());
                    selectedKey = keyAt(bone.id(), key.frame());
                    curve = selectedKey.curve();
                    dragFrame = key.frame();
                    keyDrag = true;
                    return true;
                }
                refresh();
            }
            if (x >= trackX()) { timelineDrag = true; seek(frameAt(x)); }
            return true;
        }
        return false;
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (session == null || loading) return false;
        if (axis >= 0 && button == 0) {
            if (!dirtyGizmo) { session.checkpoint(); dirtyGizmo = true; }
            var value = session.transform(selected.id());
            var local = value.rotation().rotateAxis((float) ((dx - dy) * .018), axis == 0 ? 1 : 0, axis == 1 ? 1 : 0, axis == 2 ? 1 : 0);
            session.edit(selected.id(), value.rotate(local));
            status = session.autoKey ? "Chave atualizada" : "Pose pendente · + Key registra";
            refresh();
            return true;
        }
        if (keyDrag && button == 0) { dragFrame = frameAt(x); return true; }
        if (timelineDrag && button == 0) { seek(frameAt(x)); return true; }
        if (x >= left && x < right && y >= 58 && y < bottom && (button == 1 || button == 2)) {
            viewport.drag(dx, dy, button == 2 || hasShiftDown());
            return true;
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        if (button == 0) {
            if (keyDrag && selectedKey != null && dragFrame != selectedKey.frame()) {
                var next = new StudioProject.Key(selectedKey.bone(), (int) dragFrame, selectedKey.transform(), selectedKey.curve());
                if (keyAt(next.bone(), next.frame()) == null) {
                    session.checkpoint();
                    session.project = session.project.replace(selectedKey, next);
                    selectedKey = next;
                    seek(next.frame());
                } else status = "Já existe uma chave nesse quadro";
            }
            axis = -1;
            keyDrag = timelineDrag = false;
        }
        return super.mouseReleased(x, y, button);
    }
    @Override public boolean mouseScrolled(double x, double y, double amount) {
        if (session == null) return false;
        int delta = amount > 0 ? -1 : 1;
        if (y >= bottom) trackScroll = Math.max(0, Math.min(Math.max(0, rig.bones.size() - trackRows()), trackScroll + delta));
        else if (x < left) treeScroll = Math.max(0, Math.min(Math.max(0, rig.bones.size() - treeRows()), treeScroll + delta));
        else if (x < right && y >= 58) viewport.zoom(amount);
        else return false;
        return true;
    }
    @Override public boolean keyPressed(int key, int scan, int mods) {
        if (loading) return true;
        if (session == null) return super.keyPressed(key, scan, mods);
        if (hasControlDown() && key == GLFW.GLFW_KEY_S) { save(false); return true; }
        if (hasControlDown() && (key == GLFW.GLFW_KEY_Z || key == GLFW.GLFW_KEY_Y)) {
            history(key == GLFW.GLFW_KEY_Y || hasShiftDown()); return true;
        }
        if (getFocused() instanceof EditBox field && field.isFocused()) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { applyFields(); return true; }
            if (key == GLFW.GLFW_KEY_ESCAPE) { setFocused(null); refresh(); return true; }
            return super.keyPressed(key, scan, mods);
        }
        if (key == GLFW.GLFW_KEY_SPACE) { togglePlayback(); return true; }
        if (key == GLFW.GLFW_KEY_DELETE) { deleteKey(); return true; }
        if (key == GLFW.GLFW_KEY_HOME) { seek(0); return true; }
        return super.keyPressed(key, scan, mods);
    }

    private void settings() {
        if (session == null) return;
        var p = session.project;
        minecraft.setScreen(new StudioDialog(this, "Configuração", List.of("Duração em segundos", "FPS", "Referência: neutra / idle", "Modelo: classic / slim"),
                List.of(Double.toString((double) p.frames() / p.fps()), Integer.toString(p.fps()), p.idle() ? "idle" : "neutra",
                        slim ? "slim" : "classic"), fields -> {
            double seconds = Double.parseDouble(fields.get(0).replace(',', '.'));
            int fps = Integer.parseInt(fields.get(1));
            int frames = (int) Math.round(seconds * fps);
            String pose = fields.get(2).toLowerCase(Locale.ROOT);
            String model = fields.get(3).toLowerCase(Locale.ROOT);
            if (!Double.isFinite(seconds) || seconds <= 0 || seconds * fps > 7200 || frames < 1 || fps < 1 || fps > 120
                    || !List.of("neutra", "idle").contains(pose) || !List.of("classic", "slim").contains(model))
                throw new IllegalArgumentException("Revise duração, FPS, referência e modelo");
            boolean changeModel = model.equals("slim") != slim;
            if (changeModel && (!p.keys().isEmpty() || !session.edits.isEmpty()))
                throw new IllegalArgumentException("Escolha a variante em um projeto vazio");
            double newFrame = session.frame * fps / p.fps();
            if (!session.edits.isEmpty() && newFrame > frames)
                throw new IllegalArgumentException("Mova as chaves que excedem a duração");
            var adjusted = p.retime(frames, fps, pose.equals("idle"));
            if (changeModel) {
                installRig(model.equals("slim"), false);
                newProject();
                adjusted = session.project.retime(frames, fps, pose.equals("idle"));
            }
            session.checkpoint();
            session.project = adjusted;
            session.frame = Math.min(frames, newFrame);
            selectedKey = null;
            refresh();
        }));
    }

    private void save(boolean as) {
        if (session == null) return;
        if (currentFile == null || as) {
            minecraft.setScreen(new StudioDialog(this, "Salvar projeto", List.of("Nome"), List.of(currentFile == null ? "" : currentFile),
                    fields -> saveNamed(fields.get(0), false)));
        } else saveNamed(currentFile, true);
    }
    private void saveNamed(String name, boolean replace) {
        session.captureEdits();
        var p = session.project;
        var snapshot = p.settings(name, p.frames(), p.fps(), p.loop(), p.idle());
        var model = rig;
        async(() -> { StudioFiles.validate(snapshot, model); files.write(snapshot, replace); return snapshot; }, saved -> {
            session.project = saved;
            session.saved(saved);
            currentFile = name;
            status = "Salvo · " + name;
        });
    }
    private void openManager() {
        if (session == null) return;
        async(files::names, projects -> minecraft.setScreen(new StudioLibrary(this, projects)));
    }
    void libraryOpen(String name) {
        minecraft.setScreen(this);
        discard(() -> async(() -> files.read(name), project -> {
            try {
                if (project == null) throw new IOException("Projeto vazio");
                boolean wanted = project.model() != null && project.model().equals("towerofentropy:steve_a3_slim_wall_prototypes");
                var candidate = wanted == slim ? rig : new StudioRig(wanted,
                        new ResourceLocation("minecraft", "textures/entity/player/" + (wanted ? "slim" : "wide") + "/steve.png"));
                StudioFiles.validate(project, candidate);
                if (!name.equals(project.name())) project = project.settings(name, project.frames(), project.fps(), project.loop(), project.idle());
                rig = candidate;
                slim = wanted;
                session = new StudioSession(project);
                currentFile = name;
                selected = rig.bones.get(0);
                selectedKey = null;
                treeScroll = trackScroll = 0;
                status = "Aberto · " + name;
                refresh();
            } catch (IOException | RuntimeException error) { status = error.getMessage(); }
        }));
    }
    void libraryRename(String old, String name) {
        async(() -> { files.rename(old, name); return name; }, renamed -> {
            if (old.equals(currentFile)) currentFile = renamed;
            openManager();
        });
    }
    void libraryDelete(String name) {
        confirm("Excluir " + name + "?", "O arquivo será removido da biblioteca.", () ->
                async(() -> { files.delete(name); return name; }, deleted -> {
                    if (deleted.equals(currentFile)) currentFile = null;
                    openManager();
                }));
    }
    private interface Operation<T> { T run() throws IOException; }
    private <T> void async(Operation<T> operation, Consumer<T> success) {
        loading = true;
        status = "Aguarde…";
        CompletableFuture.supplyAsync(() -> {
            try { return operation.run(); }
            catch (IOException | RuntimeException error) { throw new java.util.concurrent.CompletionException(error); }
        }, Util.ioPool()).whenComplete((result, error) -> minecraft.execute(() -> {
            loading = false;
            if (error == null) success.accept(result);
            else {
                Throwable cause = error.getCause();
                if (cause instanceof java.nio.file.FileAlreadyExistsException) status = "Nome já existe. Escolha outro nome.";
                else status = "Projeto: " + (cause == null ? error.getMessage() : cause.getMessage());
            }
        }));
    }
}
