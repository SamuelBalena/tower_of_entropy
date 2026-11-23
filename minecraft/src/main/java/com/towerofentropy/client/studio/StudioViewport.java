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
    private final java.util.List<StudioRing> rings = new java.util.ArrayList<>(3);
    private Matrix4f[] globals;
    boolean overlay = true;

    StudioViewport() { reset(); }
    void reset() { yaw = 200; pitch = 9; zoom = 1; panX = panY = 0; }
    void view(int index) {
        if (index == 0) { reset(); return; }
        yaw = switch (index) { case 1 -> 180; case 2 -> 0; case 3 -> 90; case 4 -> 270; default -> 180; };
        pitch = index == 5 ? 90 : 0;
        panX = panY = 0;
    }
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
                int x, int y, int right, int bottom, boolean anatomical, int active) {
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
            rings.clear();
            if (!overlay) return;
            stack.pushPose();
            try {
                stack.translate(0, 0, 420);
                for (var bone : rig.bones) {
                    var point = handles.get(bone.index());
                    if (bone.parent() >= 0) line(gui, handles.get(bone.parent()), point,
                            selected != null && bone.parent() == selected.index() ? 0xffffdb8b : 0xffaa935f);
                    dot(gui, point, bone.equals(selected) ? 0xffffdb8b : 0xffd9c9a6, bone.equals(selected) ? 3 : 1);
                }
                if (selected != null) {
                    var pivot = handles.get(selected.index());
                    var delta = session.transform(selected.id()).rotation();
                    var posed = globals[selected.index()];
                    var reference = new Matrix4f(posed).rotate(delta.conjugate());
                    var rotation = rig.rotation(selected, anatomical);
                    for (int i = 0; i < 3; i++) {
                        if (rotation.labels()[i].isEmpty()) continue;
                        var world = (rotation.followsPose(i) ? posed : reference)
                                .transformDirection(new Vector3f(rotation.axes()[i])).normalize();
                        var seed = Math.abs(world.y) < .85f ? new Vector3f(0, 1, 0) : new Vector3f(0, 0, 1);
                        var u = seed.cross(world).normalize();
                        var v = new Vector3f(world).cross(u);
                        float radius = 18 + i * 3;
                        float pixels = projection.transformDirection(new Vector3f(1, 0, 0)).length();
                        var ring = new StudioRing(i, pivot, projection.transformDirection(u.mul(radius / pixels)),
                                projection.transformDirection(v.mul(radius / pixels)));
                        rings.add(ring);
                        Vector3f start = ring.point(0);
                        for (int segment = 1; segment <= StudioRing.SEGMENTS; segment++) {
                            var end = ring.point(segment * StudioRing.STEP);
                            int opacity = start.z + end.z < 2 * pivot.z ? 0x85000000 : 0xe0000000;
                            int color = active == i ? AXIS_COLORS[i] : (AXIS_COLORS[i] & 0xffffff) | opacity;
                            line(gui, start, end, color);
                            start = end;
                        }
                    }
                }
            } finally { stack.popPose(); }
        } finally { gui.disableScissor(); }
    }
    StudioRing.Hit ring(double x, double y) {
        if (!overlay) return null;
        StudioRing.Hit nearest = null;
        double closest = 4;
        for (var ring : rings) {
            if (distance(ring.pivot(), x, y) < 6) continue;
            var hit = ring.hit(x, y);
            if (hit.distance() < closest) { closest = hit.distance(); nearest = hit; }
        }
        return nearest;
    }
    int pick(double x, double y) {
        return pick(x, y, 8);
    }
    int pick(double x, double y, double radius) {
        if (!overlay) return -1;
        int index = -1;
        double closest = radius;
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
