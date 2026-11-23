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
    private boolean slim, loading, timelineDrag, keyDrag, anatomical = true;
    private int axis = -1, left, right, bottom, treeScroll, trackScroll, fieldStep;
    private static final int[] SNAP = {0, 1, 5, 15};
    private static final String[] VIEWS = {"Livre", "Frente", "Costas", "Esquerda", "Direita", "Superior"};
    private int snap, view;
    private StudioRing.Drag rotationDrag;
    private StudioProject.Transform initialRotation, previousDraft;
    private StudioRotation dragRotation;
    private double dragDegrees;
    private double dragFrame;
    private String currentFile, status = "";
    private StudioProject.Curve curve = StudioProject.Curve.LINEAR;
    private String recoverySlot;
    private long lastAutosave = System.nanoTime();
    private StudioProject lastRecovery;
    private CompletableFuture<Void> recoveryWrite = CompletableFuture.completedFuture(null);
    private record Recovery(String slot, String name) {}
    private record Library(List<String> names, java.util.Map<String, StudioFiles.Summary> summaries, boolean recovery) {}

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
                checkRecovery(false);
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
        int toolWidth = Math.min(53, (right - left - 12) / 5);
        button(left + 2, 41, toolWidth, "Ossos", () -> viewport.overlay = !viewport.overlay);
        button(left + 4 + toolWidth, 41, toolWidth, "Câmera", this::viewMenu);
        button(left + 6 + toolWidth * 2, 41, toolWidth, "Foco", () -> viewport.focus(selected, (left + right) / 2f, (bottom + 59) / 2f));
        button(left + 8 + toolWidth * 3, 41, toolWidth, "Modo", () -> { anatomical = !anatomical; refresh(); });
        button(left + 10 + toolWidth * 4, 41, toolWidth, "Snap", () -> snap = (snap + 1) % SNAP.length);
        int fieldWidth = Math.min(57, (width - right) / 2);
        fieldStep = Math.max(10, Math.min(15, (bottom - 78) / 6));
        for (int i = 0; i < 6; i++) {
            var field = new EditBox(font, width - fieldWidth - 7, 63 + i * fieldStep, fieldWidth, fieldStep, Component.literal(i < 3 ? "Rotação" : "Translação"));
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
        clearOwnRecovery();
        session = new StudioSession(new StudioProject(1, "Sem título", rig.resource.toString(), rig.signature,
                false, 30, 30, false, List.of()));
        selectedKey = null;
        currentFile = null;
        treeScroll = trackScroll = 0;
        status = "Novo projeto";
    }

    @Override public boolean isPauseScreen() { return true; }
    @Override public void removed() {
        finishDrag(false);
        if (session != null) session.playing = false;
        axis = -1;
        keyDrag = timelineDrag = false;
    }
    @Override public void onClose() {
        if (!loading) discard(() -> { clearOwnRecovery(); minecraft.setScreen(previous); });
    }

    @Override public void tick() {
        if (session == null || !session.dirty() || loading || session.playing || rotationDrag != null
                || keyDrag || timelineDrag || !recoveryWrite.isDone()
                || System.nanoTime() - lastAutosave < 15_000_000_000L) return;
        lastAutosave = System.nanoTime();
        var snapshot = session.recoverySnapshot();
        if (snapshot.equals(lastRecovery)) return;
        if (recoverySlot == null) recoverySlot = java.util.UUID.randomUUID().toString();
        String slot = recoverySlot;
        var model = rig;
        recoveryWrite = CompletableFuture.runAsync(() -> {
            try {
                StudioFiles.validate(snapshot, model);
                files.writeRecovery(slot, snapshot);
            } catch (IOException error) { throw new java.util.concurrent.CompletionException(error); }
        }, Util.ioPool()).whenComplete((ignored, error) -> minecraft.execute(() -> {
            if (!slot.equals(recoverySlot)) return;
            if (error == null) lastRecovery = snapshot;
            else status = "Falha na recuperação · " + error.getCause().getMessage();
        }));
    }

    private void clearOwnRecovery() {
        String slot = recoverySlot;
        recoverySlot = null;
        lastRecovery = null;
        lastAutosave = System.nanoTime();
        if (slot != null) recoveryWrite.handle((ignored, failure) -> null).thenRunAsync(() -> {
            try { files.clearRecovery(slot); }
            catch (IOException error) {
                com.mojang.logging.LogUtils.getLogger().warn("Não foi possível remover a recuperação do Studio", error);
            }
        }, Util.ioPool());
    }
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
        var rotation = rig.rotation(selected, anatomical);
        var angles = rotation.degrees(transform.rotation());
        float[] values = {angles[0], angles[1], angles[2], transform.x(), transform.y(), transform.z()};
        for (int i = 0; i < transforms.size(); i++) {
            transforms.get(i).setValue(String.format(Locale.ROOT, "%.5f", values[i]));
            transforms.get(i).active = i < 3 ? !rotation.labels()[i].isEmpty() : selected.parent() < 0;
            transforms.get(i).visible = transforms.get(i).active;
            if (i < 3) transforms.get(i).setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.literal(rotation.labels()[i] + " · graus · Enter aplica")));
        }
    }

    private void applyFields() {
        var values = new float[6];
        try {
            for (int i = 0; i < values.length; i++) values[i] = Float.parseFloat(transforms.get(i).getValue().replace(',', '.'));
            var delta = new StudioProject.Transform(values[3], values[4], values[5], 0, 0, 0, 1)
                    .rotate(rig.rotation(selected, anatomical).compose(values));
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
            if (right > left + 2 && bottom > 61) {
                viewport.render(gui, rig, session, selected, left, 58, right, bottom, anatomical, axis);
                String guide = (anatomical ? "Anatômico" : "Eixos locais") + "  ·  "
                        + (SNAP[snap] == 0 ? "Livre" : SNAP[snap] + "°") + "  ·  " + VIEWS[view];
                gui.drawString(font, font.plainSubstrByWidth(guide, right - left - 8), left + 4, bottom - 11, GOLD);
            }
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
        var rotation = rig.rotation(selected, anatomical);
        int labelWidth = transforms.get(0).getX() - right - 10;
        for (int i = 0; i < 3; i++) {
            gui.drawString(font, font.plainSubstrByWidth(rotation.labels()[i], labelWidth),
                    right + 7, 64 + i * fieldStep, StudioViewport.AXIS_COLORS[i]);
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
        if (loading || rotationDrag != null) return true;
        if (super.mouseClicked(x, y, button)) return true;
        if (session == null || button != 0) return false;
        if (x < left && y >= 54 && y < bottom) {
            int i = treeScroll + ((int) y - 54) / 12;
            if (i < rig.bones.size()) select(rig.bones.get(i));
            return true;
        }
        if (x >= left && x < right && y >= 59 && y < bottom) {
            int preciseJoint = viewport.pick(x, y, 2);
            var hit = preciseJoint < 0 ? viewport.ring(x, y) : null;
            if (hit != null) {
                axis = hit.ring().channel();
                rotationDrag = new StudioRing.Drag(hit, x, y);
                dragRotation = rig.rotation(selected, anatomical);
                initialRotation = session.transform(selected.id());
                previousDraft = session.edits.get(selected.id());
                dragDegrees = 0;
                session.playing = false;
                return true;
            }
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
        if (rotationDrag != null && button == 0) {
            dragDegrees = Math.toDegrees(rotationDrag.move(x, y, hasShiftDown()));
            int increment = SNAP[snap];
            if (increment != 0) dragDegrees = Math.rint(dragDegrees / increment) * increment;
            var q = dragRotation.drag(initialRotation.rotation(), axis, (float) Math.toRadians(dragDegrees % 360));
            session.edits.put(selected.id(), initialRotation.rotate(q));
            status = String.format(Locale.ROOT, "%s · %s · %+.1f° · atual %.1f°", selected.label(),
                    dragRotation.labels()[axis], dragDegrees, dragRotation.degrees(q)[axis]);
            refresh();
            return true;
        }
        if (rotationDrag != null) return true;
        if (keyDrag && button == 0) { dragFrame = frameAt(x); return true; }
        if (timelineDrag && button == 0) { seek(frameAt(x)); return true; }
        if (x >= left && x < right && y >= 58 && y < bottom && (button == 1 || button == 2)) {
            viewport.drag(dx, dy, button == 2 || hasShiftDown());
            view = 0;
            return true;
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        if (button == 0) {
            if (rotationDrag != null) { finishDrag(true); return true; }
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
        if (rotationDrag != null) return true;
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
        if (rotationDrag != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) finishDrag(false);
            return true;
        }
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

    private void finishDrag(boolean apply) {
        if (rotationDrag == null) return;
        String id = selected.id();
        var result = session.transform(id);
        if (previousDraft == null) session.edits.remove(id);
        else session.edits.put(id, previousDraft);
        if (apply && Math.abs(dragDegrees % 360) > .0001 && !result.equals(initialRotation)) {
            session.checkpoint();
            session.edit(id, result);
            status = session.autoKey ? "Chave atualizada" : "Pose pendente · + Key registra";
        } else if (!apply) status = "Rotação cancelada";
        rotationDrag = null;
        axis = -1;
        refresh();
    }

    private void viewMenu() {
        minecraft.setScreen(new Screen(Component.literal("Vistas do modelo")) {
            @Override protected void init() {
                for (int i = 0; i < VIEWS.length; i++) {
                    int index = i;
                    addRenderableWidget(new StudioButton(width / 2 - 66, height / 2 - 53 + i * 18, 132, 16,
                            VIEWS[i], b -> { viewport.view(index); view = index; onClose(); }));
                }
            }
            @Override public void onClose() { minecraft.setScreen(AnimationStudioScreen.this); }
            @Override public void render(GuiGraphics gui, int x, int y, float partial) {
                gui.fill(0, 0, width, height, 0xff18181b);
                gui.drawCenteredString(font, title, width / 2, height / 2 - 72, GOLD);
                super.render(gui, x, y, partial);
            }
        });
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
        } else if (session.dirty()) {
            String name = currentFile;
            confirm("Substituir " + name + "?", "A versão anterior ficará em .backups.", () -> saveNamed(name, true));
        } else status = "Já salvo · " + currentFile + ".json";
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
            clearOwnRecovery();
            status = "Salvo · " + name + ".json";
        });
    }
    private void openManager() {
        if (session == null) return;
        async(() -> {
            var names = files.names();
            return new Library(names, files.summaries(names), !files.recoveries().isEmpty());
        }, library -> minecraft.setScreen(new StudioLibrary(this, library.names, library.summaries, library.recovery)));
    }
    void libraryOpen(String name) {
        minecraft.setScreen(this);
        discard(() -> async(() -> files.read(name), project -> {
            try {
                var candidate = projectRig(project);
                StudioFiles.validate(project, candidate);
                if (!name.equals(project.name())) project = project.settings(name, project.frames(), project.fps(), project.loop(), project.idle());
                installProject(project, candidate, name);
                status = "Aberto · " + name;
            } catch (IOException | RuntimeException error) { status = error.getMessage(); }
        }));
    }
    private StudioRig projectRig(StudioProject project) {
        boolean wanted = "towerofentropy:steve_a3_slim_wall_prototypes".equals(project.model());
        return wanted == slim ? rig : new StudioRig(wanted,
                new ResourceLocation("minecraft", "textures/entity/player/" + (wanted ? "slim" : "wide") + "/steve.png"));
    }

    private void installProject(StudioProject project, StudioRig candidate, String name) {
        clearOwnRecovery();
        rig = candidate;
        slim = candidate.resource.getPath().contains("slim");
        session = new StudioSession(project);
        currentFile = name;
        selected = rig.bones.get(0);
        selectedKey = null;
        treeScroll = trackScroll = 0;
        refresh();
    }

    void checkRecovery(boolean reportEmpty) {
        async(() -> {
            var result = new ArrayList<Recovery>();
            for (String slot : files.recoveries()) {
                if (slot.equals(recoverySlot)) continue;
                try { result.add(new Recovery(slot, files.readRecovery(slot).name())); }
                catch (IOException invalid) { result.add(new Recovery(slot, "Rascunho indisponível")); }
            }
            return result;
        }, result -> {
            if (!result.isEmpty()) recoverMenu(result, 0);
            else status = reportEmpty ? "Nenhum rascunho para recuperar" : "Novo projeto";
        });
    }

    private void recoverMenu(List<Recovery> entries, int index) {
        Recovery item = entries.get(index);
        minecraft.setScreen(new Screen(Component.literal("Recuperação de projetos")) {
            @Override protected void init() {
                int x = width / 2 - 65, y = height / 2 - 24;
                addRenderableWidget(new StudioButton(x, y, 130, 16, "Recuperar", b -> {
                    minecraft.setScreen(AnimationStudioScreen.this);
                    discard(() -> async(() -> files.readRecovery(item.slot), project -> {
                        try {
                            var model = projectRig(project);
                            StudioFiles.validate(project, model);
                            installProject(project, model, null);
                            recoverySlot = item.slot;
                            session.recovered();
                            status = "Rascunho recuperado · Salvar + cria um projeto";
                        } catch (IOException | RuntimeException failure) { status = failure.getMessage(); }
                    }));
                }));
                addRenderableWidget(new StudioButton(x, y + 19, 130, 16, "Ignorar", b -> onClose()));
                addRenderableWidget(new StudioButton(x, y + 38, 130, 16, "Descartar recuperação", b -> {
                    minecraft.setScreen(AnimationStudioScreen.this);
                    confirm("Descartar recuperação?", "O rascunho separado será excluído.", () ->
                            async(() -> { files.clearRecovery(item.slot); return true; }, ignored -> checkRecovery(true)));
                }));
                if (entries.size() > 1) addRenderableWidget(new StudioButton(x, y + 57, 130, 16, "Próximo rascunho",
                        b -> recoverMenu(entries, (index + 1) % entries.size())));
            }
            @Override public void onClose() {
                minecraft.setScreen(AnimationStudioScreen.this);
                status = "Recuperação disponível na biblioteca";
            }
            @Override public void render(GuiGraphics gui, int x, int y, float partial) {
                gui.fill(0, 0, width, height, 0xff18181b);
                gui.drawCenteredString(font, title, width / 2, height / 2 - 53, GOLD);
                gui.drawCenteredString(font, font.plainSubstrByWidth(item.name, width - 16), width / 2, height / 2 - 39, TEXT);
                super.render(gui, x, y, partial);
            }
        });
    }

    void libraryFolder() {
        async(files::directory, path -> { Util.getPlatform().openFile(path.toFile()); openManager(); });
    }

    void libraryCopy(String source, String name) {
        async(() -> files.read(source), project -> importProject(project, name));
    }

    void libraryImport(String path, String name) {
        async(() -> StudioFiles.readPath(java.nio.file.Path.of(path)), project -> importProject(project, name));
    }

    private void importProject(StudioProject project, String name) {
        try {
            StudioFiles.validate(project, projectRig(project));
            var copy = project.settings(name, project.frames(), project.fps(), project.loop(), project.idle());
            async(() -> { files.write(copy, false); return true; }, ignored -> openManager());
        } catch (IOException | RuntimeException error) { status = "Projeto: " + error.getMessage(); }
    }

    void libraryExport(String source, String path) {
        async(() -> files.read(source), project -> {
            try {
                StudioFiles.validate(project, projectRig(project));
                async(() -> { StudioFiles.export(project, java.nio.file.Path.of(path)); return true; },
                        ignored -> status = "Cópia JSON exportada");
            } catch (IOException | RuntimeException error) { status = "Projeto: " + error.getMessage(); }
        });
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
