package com.towerofentropy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.phe.polymesh.model.RuntimeModel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Vanilla surfaces and material passes, skinned against the completed R3 pose. */
final class R3Armor {
    private static final String[] JOINTS = {"0", "1", "waist", "pelvis", "2", "102", "3", "103", "4", "104", "5", "105"};
    private static final float[] TORSO_CUTS = {0, 2, 4, 6, 8, 10, 12};
    private static final float[] ARM_CUTS = {0, 4, 5, 6, 7, 8, 12};
    private static final float[] LEG_CUTS = {12, 13, 14, 17, 18, 19, 24};
    private final int[] nodes = new int[JOINTS.length];
    private final Matrix4f[] inverseBind = new Matrix4f[JOINTS.length];
    private final Matrix4f[] position = new Matrix4f[JOINTS.length];
    private final Matrix3f[] normal = new Matrix3f[JOINTS.length];
    private final Vector3f first = new Vector3f(), second = new Vector3f();
    private final HumanoidArmorLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>, ArmorModel> layer;
    private long frame;

    static boolean supports(ItemStack stack) {
        return stack.isEmpty() || (stack.getItem() instanceof ArmorItem armor
                && armor.getMaterial() instanceof ArmorMaterials
                && BuiltInRegistries.ITEM.getKey(armor).getNamespace().equals("minecraft"));
    }

    R3Armor(RuntimeModel model, PlayerRenderer playerRenderer, boolean slim) {
        var skin = model.getSkins().get(0);
        int[] jointNodes = skin.getJointNodeIndices();
        for (int i = 0; i < JOINTS.length; i++) {
            int index = 0;
            while (index < jointNodes.length && !model.getNodes().get(jointNodes[index]).getName().equals(JOINTS[i])) index++;
            if (index == jointNodes.length) throw new IllegalStateException("Missing R3 armor joint " + JOINTS[i]);
            nodes[i] = jointNodes[index];
            inverseBind[i] = skin.getInverseBindMatrices()[index];
            position[i] = new Matrix4f();
            normal[i] = new Matrix3f();
        }
        var mc = Minecraft.getInstance();
        var models = mc.getEntityModels();
        var inner = new ArmorModel(models.bakeLayer(slim ? ModelLayers.PLAYER_SLIM_INNER_ARMOR : ModelLayers.PLAYER_INNER_ARMOR), slim);
        var outer = new ArmorModel(models.bakeLayer(slim ? ModelLayers.PLAYER_SLIM_OUTER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR), slim);
        layer = new HumanoidArmorLayer<>(playerRenderer, inner, outer, mc.getModelManager());
    }

    void render(AbstractClientPlayer player, Matrix4f[] globals, PoseStack pose, MultiBufferSource buffers, int light) {
        ++frame;
        for (int i = 0; i < JOINTS.length; i++) {
            position[i].set(globals[nodes[i]]).mul(inverseBind[i]);
            normal[i].set(position[i]).invert().transpose();
        }
        // Vanilla owns texture selection, dye, overlays, trims, glint and slot visibility.
        layer.render(pose, buffers, light, player, 0, 0, 0, 0, 0, 0);
    }

    private final class ArmorModel extends HumanoidModel<AbstractClientPlayer> {
        private final Surface[] surfaces;

        ArmorModel(ModelPart root, boolean slim) {
            super(root);
            surfaces = new Surface[]{
                    new Surface(head, 0, slim), new Surface(hat, 0, slim), new Surface(body, 1, slim),
                    new Surface(leftArm, 2, slim), new Surface(rightArm, 3, slim),
                    new Surface(leftLeg, 4, slim), new Surface(rightLeg, 5, slim)
            };
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer out, int light, int overlay,
                float r, float g, float b, float a) {
            for (var surface : surfaces)
                if (surface.part.visible) surface.emit(pose.last(), out, light, overlay, r, g, b, a);
        }
    }

    private final class Surface {
        final ModelPart part;
        final BoundVertex[] mesh;
        final float[] transformed;
        long lastFrame = -1;

        Surface(ModelPart part, int region, boolean slim) {
            this.part = part;
            var capture = new QuadCapture(region, slim);
            part.render(new PoseStack(), capture, 0, 0);
            mesh = capture.result.toArray(BoundVertex[]::new);
            transformed = new float[mesh.length * 6];
        }

        void emit(PoseStack.Pose pose, VertexConsumer out, int light, int overlay, float r, float g, float b, float a) {
            if (lastFrame != frame) bakePose(pose);
            for (int i = 0; i < mesh.length; i++) {
                int j = i * 6;
                var uv = mesh[i].source;
                out.vertex(transformed[j], transformed[j + 1], transformed[j + 2], r, g, b, a, uv.u, uv.v,
                        overlay, light, transformed[j + 3], transformed[j + 4], transformed[j + 5]);
            }
        }

        private void bakePose(PoseStack.Pose pose) {
            for (int i = 0; i < mesh.length; i++) {
                var vertex = mesh[i];
                var source = vertex.source;
                int j = i * 6;
                position[vertex.proximal].transformPosition(source.x, source.y, source.z, first);
                if (vertex.weight > 0) first.lerp(position[vertex.distal].transformPosition(source.x, source.y, source.z, second), vertex.weight);
                pose.pose().transformPosition(first);
                transformed[j] = first.x;
                transformed[j + 1] = first.y;
                transformed[j + 2] = first.z;
                normal[vertex.proximal].transform(source.nx, source.ny, source.nz, first);
                if (vertex.weight > 0) first.lerp(normal[vertex.distal].transform(source.nx, source.ny, source.nz, second), vertex.weight);
                pose.normal().transform(first).normalize();
                transformed[j + 3] = first.x;
                transformed[j + 4] = first.y;
                transformed[j + 5] = first.z;
            }
            lastFrame = frame;
        }
    }

    private record SourceVertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        SourceVertex interpolate(SourceVertex to, float amount) {
            return new SourceVertex(x + (to.x - x) * amount, y + (to.y - y) * amount, z + (to.z - z) * amount,
                    u + (to.u - u) * amount, v + (to.v - v) * amount, nx, ny, nz);
        }
    }

    private record BoundVertex(SourceVertex source, int proximal, int distal, float weight) {
        static BoundVertex bind(SourceVertex source, int region) {
            float y = (1.5f - source.y) * 16;
            if (region == 0) return new BoundVertex(source, 0, 0, 0);
            if (region == 1) return y <= 6 ? blend(source, 1, 2, y / 6) : blend(source, 2, 3, (y - 6) / 6);
            int upper = region * 2;
            if (region <= 3) return blend(source, upper, upper + 1, (y - 5) / 2);
            if (y < 14) return blend(source, 3, upper, (y - 12) / 2);
            return blend(source, upper, upper + 1, (y - 17) / 2);
        }

        private static BoundVertex blend(SourceVertex source, int proximal, int distal, float weight) {
            // R3 regularizes the equal-weight ring to avoid a singular blend in a deep fold.
            float w = Math.max(0, Math.min(1, weight));
            return new BoundVertex(source, proximal, distal, w == .5f ? .4f : w);
        }
    }

    /** Captures the vanilla cube UVs once, splitting lateral quads across the R3 bend rings. */
    private static final class QuadCapture implements VertexConsumer {
        private final int region;
        private final boolean slim;
        private final List<BoundVertex> result = new ArrayList<>();
        private final SourceVertex[] quad = new SourceVertex[4];
        private int vertices;

        QuadCapture(int region, boolean slim) {
            this.region = region;
            this.slim = slim;
        }

        @Override
        public void vertex(float x, float y, float z, float red, float green, float blue, float alpha,
                float u, float v, int overlay, int light, float nx, float ny, float nz) {
            if (slim && (region == 2 || region == 3) && Math.abs(x) > .375f) x -= Math.copySign(1f / 16, x);
            quad[vertices++] = new SourceVertex(x, 1.5f - y, z, u, v, nx, -ny, nz);
            if (vertices == 4) {
                emitQuad();
                vertices = 0;
            }
        }

        private void emitQuad() {
            if (region == 0 || quad[0].ny != 0) {
                add(quad[0], quad[1], quad[2], quad[3]);
                return;
            }
            int start = quad[0].y == quad[1].y ? 0 : 1;
            var a = quad[start];
            var b = quad[(start + 1) % 4];
            var c = quad[(start + 2) % 4];
            var d = quad[(start + 3) % 4];
            float[] cuts = region == 1 ? TORSO_CUTS : region <= 3 ? ARM_CUTS : LEG_CUTS;
            float fromY = (1.5f - a.y) * 16, toY = (1.5f - d.y) * 16;
            var left = a;
            var right = b;
            boolean descending = toY > fromY;
            for (int i = 0; i < cuts.length; i++) {
                float y = cuts[descending ? i : cuts.length - 1 - i];
                float t = (y - fromY) / (toY - fromY);
                if (t <= 0 || t >= 1) continue;
                var nextLeft = a.interpolate(d, t);
                var nextRight = b.interpolate(c, t);
                add(left, right, nextRight, nextLeft);
                left = nextLeft;
                right = nextRight;
            }
            add(left, right, c, d);
        }

        private void add(SourceVertex a, SourceVertex b, SourceVertex c, SourceVertex d) {
            result.add(BoundVertex.bind(a, region));
            result.add(BoundVertex.bind(b, region));
            result.add(BoundVertex.bind(c, region));
            result.add(BoundVertex.bind(d, region));
        }

        private UnsupportedOperationException unexpected() {
            return new UnsupportedOperationException("Expected complete vertices from vanilla armor cubes");
        }
        @Override public VertexConsumer vertex(double x, double y, double z) { throw unexpected(); }
        @Override public VertexConsumer color(int r, int g, int b, int a) { throw unexpected(); }
        @Override public VertexConsumer uv(float u, float v) { throw unexpected(); }
        @Override public VertexConsumer overlayCoords(int u, int v) { throw unexpected(); }
        @Override public VertexConsumer uv2(int u, int v) { throw unexpected(); }
        @Override public VertexConsumer normal(float x, float y, float z) { throw unexpected(); }
        @Override public void endVertex() { throw unexpected(); }
        @Override public void defaultColor(int r, int g, int b, int a) { throw unexpected(); }
        @Override public void unsetDefaultColor() { throw unexpected(); }
    }
}
