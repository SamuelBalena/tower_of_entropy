package com.towerofentropy.client.studio;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

final class StudioDialog extends Screen {
    private final Screen parent;
    private final List<String> labels, defaults;
    private final Consumer<List<String>> accept;
    private final List<EditBox> fields = new ArrayList<>();
    private String error = "";
    private int x, y;

    StudioDialog(Screen parent, String title, List<String> labels, List<String> defaults, Consumer<List<String>> accept) {
        super(Component.literal(title));
        this.parent = parent;
        this.labels = labels;
        this.defaults = defaults;
        this.accept = accept;
    }
    @Override protected void init() {
        var values = fields.isEmpty() ? defaults : fields.stream().map(EditBox::getValue).toList();
        fields.clear();
        x = (width - 226) / 2;
        y = (height - 44 - labels.size() * 29) / 2;
        for (int i = 0; i < labels.size(); i++) {
            var field = new EditBox(font, x + 9, y + 26 + i * 29, 208, 15, Component.literal(labels.get(i)));
            field.setMaxLength(48);
            field.setValue(values.get(i));
            fields.add(addRenderableWidget(field));
        }
        int bottom = y + 14 + labels.size() * 29;
        addRenderableWidget(new StudioButton(x + 9, bottom, 98, 16, "Aplicar", b -> submit()));
        addRenderableWidget(new StudioButton(x + 119, bottom, 98, 16, "Cancelar", b -> onClose()));
        setInitialFocus(fields.get(0));
    }
    private void submit() {
        minecraft.setScreen(parent);
        try { accept.accept(fields.stream().map(EditBox::getValue).toList()); }
        catch (RuntimeException invalid) {
            error = invalid instanceof NumberFormatException ? "Confira os valores numéricos." : invalid.getMessage();
            minecraft.setScreen(this);
        }
    }
    @Override public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { submit(); return true; }
        return super.keyPressed(key, scan, mods);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public void render(GuiGraphics gui, int mx, int my, float partial) {
        gui.fill(0, 0, width, height, 0xff18181b);
        gui.fill(x, y, x + 226, y + 44 + labels.size() * 29, 0xff27282c);
        gui.drawString(font, title, x + 9, y + 6, 0xffe4bd77);
        for (int i = 0; i < labels.size(); i++) gui.drawString(font, labels.get(i), x + 9, y + 16 + i * 29, 0xffeee7da);
        gui.drawString(font, font.plainSubstrByWidth(error == null ? "Valor inválido" : error, 208),
                x + 9, y + 33 + labels.size() * 29, 0xffe27f72);
        super.render(gui, mx, my, partial);
    }
}
