package com.towerofentropy.client.studio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

final class StudioButton extends Button {
    StudioButton(int x, int y, int width, int height, String name, OnPress action) {
        super(x, y, width, height, Component.literal(name), action, DEFAULT_NARRATION);
    }

    @Override protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partial) {
        int x = getX(), y = getY();
        int border = isFocused() ? 0xffe4bd77 : isHovered ? 0xffa99166 : 0xff48423a;
        int background = active && isHoveredOrFocused() ? 0xff594a34 : 0xff303033;
        gui.fill(x, y, x + width, y + height, border);
        gui.fill(x + 1, y + 1, x + width - 1, y + height - 1, background);
        gui.drawCenteredString(Minecraft.getInstance().font, getMessage(), x + width / 2, y + (height - 8) / 2,
                active ? 0xffeee7da : 0xffa49a8d);
    }
}
