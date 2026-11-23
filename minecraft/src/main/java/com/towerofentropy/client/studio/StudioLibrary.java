package com.towerofentropy.client.studio;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class StudioLibrary extends Screen {
    private final AnimationStudioScreen parent;
    private final List<String> projects;
    private int scroll, selected = -1, x, right;
    private Button open, rename, delete;

    StudioLibrary(AnimationStudioScreen parent, List<String> projects) {
        super(Component.literal("Projetos do Animation Studio"));
        this.parent = parent;
        this.projects = projects;
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
        updateButtons();
    }
    private Button button(int x, int width, String text, Runnable action) {
        return addRenderableWidget(new StudioButton(x, height - 26, width, 17, text, b -> action.run()));
    }
    private void updateButtons() { open.active = rename.active = delete.active = selected >= 0; }
    private int rows() { return Math.max(1, (height - 63) / 14); }
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
            gui.drawString(font, font.plainSubstrByWidth(projects.get(index), right - x - 12), x + 6, y + 2, 0xffeee7da);
        }
        super.render(gui, mx, my, partial);
    }
}
