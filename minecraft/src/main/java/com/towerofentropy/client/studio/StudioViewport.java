package com.towerofentropy.client.studio;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;
import org.joml.Vector3f;

final class StudioViewport {
    static final int[] AXIS_COLORS = {0xffe27f72, 0xff9dcc86, 0xff8cace4};
    private float yaw, pitch, zoom, panX, panY;
    private final Map<Integer, Vector3f> handles = new HashMap<>();
    private final Vector3f[] axes = {new Vector3f(), new Vector3f(), new Vector3f()};
    private Matrix4f[] globals;
    boolean overlay = true;

    StudioViewport() { reset(); }
    void reset() { yaw = 200; pitch = 9; zoom = 1; panX = panY = 0; }
    void drag(double x, double y, boolean pan) {
        if (pan) { panX += (float) x; panY += (float) y; }
        else { yaw += (float) x * .8f; pitch = Math.max(-80, Math.min(80, pitch + (float) y * .8f)); }
    }
    void zoom(double amount) { zoom = Math.max(.4f, Math.min(3, zoom * (float) Math.pow(1.12, amount))); }
    void focus(StudioRig.Bone bone, float cx, float cy) {
        var point = handles.get(bone.index());
        if (point != null) { panX += cx - point.x; panY += cy - point.y; }
    }
    void render(GuiGraphics gui, StudioRig rig, StudioSession session, StudioRig.Bone selected,
                int x, int y, int right, int bottom) {
        gui.enableScissor(x, y, right, bottom);
        try {
            gui.fill(x, y, right, bottom, 0xff1a1b1e);
            int cy = (y + bottom) / 2;
            for (int row = cy; row < bottom; row += 9) gui.fill(x, row, right, row + 1, 0xff27272a);
            gui.flush();
            globals = rig.evaluate(session);
            var stack = gui.pose();
            stack.pushPose();
            Matrix4f projection;
            try {
                float scale = Math.min((bottom - y) * .43f, (right - x) * .65f) * zoom;
                stack.translate((x + right) * .5f + panX, cy + panY, 220);
                stack.scale(-scale, -scale, scale);
                stack.mulPose(Axis.XP.rotationDegrees(pitch));
                stack.mulPose(Axis.YP.rotationDegrees(yaw));
                stack.translate(0, -.93f, 0);
                projection = new Matrix4f(stack.last().pose());
                Lighting.setupForEntityInInventory();
                rig.draw(globals, stack, Minecraft.getInstance().renderBuffers().bufferSource());
            } finally {
                Lighting.setupFor3DItems();
                stack.popPose();
            }
            handles.clear();
            for (var bone : rig.bones)
                handles.put(bone.index(), projection.transformPosition(globals[bone.index()].getTranslation(new Vector3f())));
            if (!overlay) return;
            stack.pushPose();
            try {
                stack.translate(0, 0, 420);
                for (var bone : rig.bones) {
                    var point = handles.get(bone.index());
                    if (bone.parent() >= 0) line(gui, handles.get(bone.parent()), point, 0xffaa935f);
                    dot(gui, point, bone.equals(selected) ? 0xffffdb8b : 0xffd9c9a6, bone.equals(selected) ? 3 : 1);
                }
                if (selected != null) {
                    var pivot = handles.get(selected.index());
                    var local = new Matrix4f(projection).mul(globals[selected.index()]);
                    for (int i = 0; i < axes.length; i++) {
                        axes[i].zero().setComponent(i, .37f);
                        local.transformPosition(axes[i]);
                        var direction = new Vector3f(axes[i]).sub(pivot);
                        float length = (float) Math.hypot(direction.x, direction.y);
                        if (length < 4) axes[i].set(pivot.x + (i - 1) * 10, pivot.y + 14, 0);
                        line(gui, pivot, axes[i], AXIS_COLORS[i]);
                        dot(gui, axes[i], AXIS_COLORS[i], 2);
                    }
                }
            } finally { stack.popPose(); }
        } finally { gui.disableScissor(); }
    }
    int axis(double x, double y) {
        if (!overlay) return -1;
        int axis = -1;
        double closest = 5;
        for (int i = 0; i < axes.length; i++) {
            double distance = distance(axes[i], x, y);
            if (distance < closest) { closest = distance; axis = i; }
        }
        return axis;
    }
    int pick(double x, double y) {
        if (!overlay) return -1;
        int index = -1;
        double closest = 8;
        for (var entry : handles.entrySet()) {
            double distance = distance(entry.getValue(), x, y);
            if (distance < closest) { closest = distance; index = entry.getKey(); }
        }
        return index;
    }
    private static double distance(Vector3f v, double x, double y) { return Math.hypot(x - v.x, y - v.y); }
    private static void dot(GuiGraphics gui, Vector3f v, int color, int size) {
        int x = Math.round(v.x), y = Math.round(v.y);
        gui.fill(x - size, y - size, x + size + 1, y + size + 1, color);
    }
    private static void line(GuiGraphics gui, Vector3f a, Vector3f b, int color) {
        var stack = gui.pose();
        stack.pushPose();
        stack.translate(a.x, a.y, 0);
        stack.mulPose(Axis.ZP.rotation((float) Math.atan2(b.y - a.y, b.x - a.x)));
        gui.fill(0, 0, (int) Math.hypot(b.x - a.x, b.y - a.y) + 1, 1, color);
        stack.popPose();
    }
}
