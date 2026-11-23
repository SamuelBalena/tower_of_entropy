package com.towerofentropy.client.studio;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class StudioLibrary extends Screen {
    private final AnimationStudioScreen parent;
    private final List<String> projects;
    private final java.util.Map<String, StudioFiles.Summary> summaries;
    private final boolean recovery;
    private int scroll, selected = -1, x, right;
    private Button open, rename, delete, duplicate, export;

    StudioLibrary(AnimationStudioScreen parent, List<String> projects, java.util.Map<String, StudioFiles.Summary> summaries, boolean recovery) {
        super(Component.literal("Projetos do Animation Studio"));
        this.parent = parent;
        this.projects = projects;
        this.summaries = summaries;
        this.recovery = recovery;
    }
    @Override protected void init() {
        x = Math.max(8, (width - 310) / 2);
        right = width - x;
        open = button(x, 64, "Abrir", () -> parent.libraryOpen(projects.get(selected)));
        rename = button(x + 68, 77, "Renomear", () -> {
            String name = projects.get(selected);
            minecraft.setScreen(new StudioDialog(parent, "Renomear projeto", List.of("Novo nome"), List.of(name),
                    values -> parent.libraryRename(name, values.get(0))));
        });
        delete = button(x + 149, 68, "Excluir", () -> parent.libraryDelete(projects.get(selected)));
        button(right - 60, 60, "Voltar", this::onClose);
        int cell = (right - x - 10) / 5;
        duplicate = top(x, cell, "Duplicar", () -> {
            String source = projects.get(selected);
            minecraft.setScreen(new StudioDialog(parent, "Duplicar projeto", List.of("Nome da cópia"), List.of(source + "_copia"),
                    fields -> parent.libraryCopy(source, fields.get(0))));
        });
        top(x + cell + 2, cell, "Importar", () -> minecraft.setScreen(new StudioDialog(parent, "Importar projeto",
                List.of("Caminho do .json", "Nome na biblioteca"), List.of("", ""),
                fields -> parent.libraryImport(fields.get(0), fields.get(1)))));
        export = top(x + 2 * (cell + 2), cell, "Exportar", () -> {
            String source = projects.get(selected);
            minecraft.setScreen(new StudioDialog(parent, "Exportar cópia", List.of("Caminho de um novo .json"), List.of(""),
                    fields -> parent.libraryExport(source, fields.get(0))));
        });
        top(x + 3 * (cell + 2), cell, "Pasta", () -> { onClose(); parent.libraryFolder(); });
        top(x + 4 * (cell + 2), cell, "Recuperar", () -> { onClose(); parent.checkRecovery(true); }).active = recovery;
        updateButtons();
    }
    private Button button(int x, int width, String text, Runnable action) {
        return addRenderableWidget(new StudioButton(x, height - 26, width, 17, text, b -> action.run()));
    }
    private Button top(int x, int width, String text, Runnable action) {
        return addRenderableWidget(new StudioButton(x, height - 46, width, 17, text, b -> action.run()));
    }
    private void updateButtons() { open.active = rename.active = delete.active = duplicate.active = export.active = selected >= 0; }
    private int rows() { return Math.max(1, (height - 79) / 14); }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && mx >= x && mx < right && my >= 30 && my < 30 + rows() * 14) {
            int index = scroll + ((int) my - 30) / 14;
            if (index < projects.size()) { selected = index; updateButtons(); }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }
    @Override public boolean mouseScrolled(double x, double y, double amount) {
        scroll = Math.max(0, Math.min(Math.max(0, projects.size() - rows()), scroll + (amount < 0 ? 1 : -1)));
        return true;
    }
    @Override public void render(GuiGraphics gui, int mx, int my, float partial) {
        gui.fill(0, 0, width, height, 0xff171719);
        gui.drawString(font, title, x, 10, 0xffe4bd77);
        if (projects.isEmpty()) gui.drawString(font, "Salve um projeto para abri-lo aqui.", x, 30, 0xffeee7da);
        for (int row = 0; row < rows() && scroll + row < projects.size(); row++) {
            int index = scroll + row, y = 30 + row * 14;
            gui.fill(x, y, right, y + 13, index == selected ? 0xff594a34 : 0xff242529);
            String name = projects.get(index);
            var summary = summaries.get(name);
            String detail = summary == null ? "Inválido / incompatível" : String.format(java.util.Locale.ROOT,
                    "%s · %.2f s · %d FPS", summary.model() != null && summary.model().contains("slim") ? "Slim" : "Classic",
                    (double) summary.frames() / summary.fps(), summary.fps());
            int info = font.width(detail);
            gui.drawString(font, font.plainSubstrByWidth(name, right - x - 18 - info), x + 6, y + 2, 0xffeee7da);
            gui.drawString(font, detail, right - info - 6, y + 2, 0xffb4aca0);
        }
        super.render(gui, mx, my, partial);
    }
}
