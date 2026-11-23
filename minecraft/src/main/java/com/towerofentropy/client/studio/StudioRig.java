package com.towerofentropy.client.studio;

import com.mojang.blaze3d.vertex.PoseStack;
import com.towerofentropy.client.PlayerRendererR3;
import dev.phe.polymesh.client.GltfModelRenderer;
import dev.phe.polymesh.model.RuntimeMaterial;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

final class StudioRig extends GltfModelRenderer {
    record Bone(int index, String id, String label, int parent, int depth) {}
    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("pelvis", "Raiz / quadril"), Map.entry("waist", "Cintura"),
            Map.entry("0", "Cabeça"), Map.entry("1", "Tronco"),
            Map.entry("2", "Braço esquerdo"), Map.entry("3", "Braço direito"),
            Map.entry("102", "Cotovelo esquerdo"), Map.entry("103", "Cotovelo direito"),
            Map.entry("4", "Perna esquerda"), Map.entry("5", "Perna direita"),
            Map.entry("104", "Joelho esquerdo"), Map.entry("105", "Joelho direito"));
    final List<Bone> bones;
    final String signature;
    final ResourceLocation resource;
    private final ResourceLocation skin;
    private final Matrix4f[] rest, idle, globals, locals;
    private final boolean[] ready;
    private final Map<Integer, Bone> byIndex = new HashMap<>();
    private final Map<Integer, StudioRotation> anatomy = new HashMap<>();
    private StudioProject lastProject;
    private Map<String, StudioProject.Transform> lastEdits = Map.of();
    private double lastFrame = -1;
    long evaluationNanos, renderNanos, evaluations, draws;

    StudioRig(boolean slim, ResourceLocation skin) {
        this(slim ? new ResourceLocation("towerofentropy", "steve_a3_slim_wall_prototypes") : PlayerRendererR3.MODEL, skin);
    }

    private StudioRig(ResourceLocation resource, ResourceLocation skin) {
        super(resource);
        this.resource = resource;
        this.skin = skin;
        var model = getModel();
        if (model == null || model.getSkins().isEmpty()) throw new IllegalStateException("Modelo R3 indisponível");
        var joints = new java.util.TreeSet<Integer>();
        for (var meshSkin : model.getSkins()) for (int joint : meshSkin.getJointNodeIndices()) joints.add(joint);
        var order = new ArrayList<Bone>();
        for (int i = 0; i < model.getNodeCount(); i++) {
            var node = model.getNodes().get(i);
            if (node.getParentIndex() < 0) visit(i, -1, 0, joints, order);
        }
        bones = List.copyOf(order);
        if (bones.isEmpty() || bones.stream().map(Bone::id).distinct().count() != bones.size())
            throw new IllegalStateException("Identificadores de ossos incompatíveis");
        var hierarchy = new StringBuilder();
        for (var bone : bones) {
            var parent = byIndex.get(bone.parent);
            hierarchy.append(bone.id).append('/').append(parent == null ? "-" : parent.id).append(':');
            var matrix = model.getNodes().get(bone.index).getLocalTransform();
            for (int column = 0; column < 4; column++) for (int row = 0; row < 4; row++)
                hierarchy.append(Math.round(matrix.get(column, row) * 100000)).append(',');
        }
        signature = UUID.nameUUIDFromBytes(hierarchy.toString().getBytes(StandardCharsets.UTF_8)).toString();
        int count = model.getNodeCount();
        rest = new Matrix4f[count];
        idle = new Matrix4f[count];
        globals = new Matrix4f[count];
        locals = new Matrix4f[count];
        ready = new boolean[count];
        var controller = getAnimationController();
        controller.play("UAL_idle", false);
        controller.setAnimationTime(0);
        var reference = controller.computeGlobalTransforms(0);
        for (int i = 0; i < count; i++) {
            var node = model.getNodes().get(i);
            int parent = node.getParentIndex();
            rest[i] = new Matrix4f(node.getLocalTransform());
            idle[i] = parent < 0 ? new Matrix4f(reference[i]) : new Matrix4f(reference[parent]).invert().mul(reference[i]);
            globals[i] = new Matrix4f();
            locals[i] = new Matrix4f();
        }
        mapAnatomy();
    }

    private void mapAnatomy() {
        var bind = new Matrix4f[rest.length];
        for (int i = 0; i < bind.length; i++) bindGlobal(i, bind);
        var up = position(bind, "1").sub(position(bind, "pelvis")).normalize();
        var left = position(bind, "2").sub(position(bind, "3")).normalize();
        var forward = new Vector3f(up).cross(left).normalize();
        left.set(forward).cross(up).normalize();
        for (var bone : bones) {
            String child = switch (bone.id) {
                case "2", "3", "4", "5" -> "10" + bone.id;
                default -> null;
            };
            Vector3f[] axes;
            int[] order;
            String[] labels;
            int count = 3;
            if (child != null) {
                var down = position(bind, child).sub(position(bind, bone.id)).normalize();
                var outward = new Vector3f(left).mul(bone.id.equals("2") || bone.id.equals("4") ? 1 : -1);
                var frontAxis = new Vector3f(down).cross(forward).normalize();
                var sideAxis = new Vector3f(down).cross(outward);
                sideAxis.fma(-sideAxis.dot(frontAxis), frontAxis).normalize();
                axes = new Vector3f[]{down, sideAxis, frontAxis};
                labels = new String[]{"Torção", "Abrir/fechar", "Frente/trás"};
                order = new int[]{2, 1, 0};
            } else if (bone.id.startsWith("10")) {
                var down = position(bind, bone.id).sub(position(bind, byIndex.get(bone.parent).id)).normalize();
                var hinge = new Vector3f(down).cross(forward).normalize().mul(bone.id.endsWith("2") || bone.id.endsWith("3") ? 1 : -1);
                var third = new Vector3f(hinge).cross(down).normalize();
                axes = new Vector3f[]{down, third, hinge};
                labels = new String[]{"", "", "Flexão/extensão"};
                order = new int[]{2, 0, 1};
                count = 1;
            } else {
                axes = new Vector3f[]{new Vector3f(up), new Vector3f(forward), new Vector3f(left).negate()};
                labels = bone.id.equals("0") ? new String[]{"Olhar lados", "Inclinar", "Olhar cima/baixo"}
                        : new String[]{"Torção", "Inclinação lateral", "Inclinação frontal"};
                order = new int[]{2, 1, 0};
            }
            var inverse = new Matrix4f(bind[bone.index]).invert();
            for (var axis : axes) inverse.transformDirection(axis).normalize();
            anatomy.put(bone.index, new StudioRotation(axes, labels, order, count, true));
        }
    }

    private Vector3f position(Matrix4f[] bind, String id) {
        return bind[bone(id).index].getTranslation(new Vector3f());
    }

    private Matrix4f bindGlobal(int index, Matrix4f[] bind) {
        if (bind[index] == null) {
            int parent = getModel().getNodes().get(index).getParentIndex();
            bind[index] = parent < 0 ? new Matrix4f(rest[index]) : new Matrix4f(bindGlobal(parent, bind)).mul(rest[index]);
        }
        return bind[index];
    }

    StudioRotation rotation(Bone bone, boolean anatomical) {
        return anatomical ? anatomy.get(bone.index) : StudioRotation.local();
    }

    private void visit(int index, int parent, int depth, java.util.Set<Integer> joints, List<Bone> order) {
        var node = getModel().getNodes().get(index);
        if (joints.contains(index)) {
            var bone = new Bone(index, node.getName(), LABELS.getOrDefault(node.getName(), node.getName()), parent, depth);
            order.add(bone);
            byIndex.put(index, bone);
            parent = index;
            depth++;
        }
        for (int child : node.getChildren()) visit(child, parent, depth, joints, order);
    }

    Bone bone(String id) {
        for (var bone : bones) if (bone.id.equals(id)) return bone;
        return null;
    }

    Matrix4f[] evaluate(StudioSession session) {
        if (lastProject == session.project && lastFrame == session.frame && lastEdits.equals(session.edits)) return globals;
        long start = System.nanoTime();
        lastProject = session.project;
        lastFrame = session.frame;
        lastEdits = Map.copyOf(session.edits);
        var reference = session.project.idle() ? idle : rest;
        java.util.Arrays.fill(ready, false);
        for (int i = 0; i < locals.length; i++) locals[i].set(reference[i]);
        for (var bone : bones) {
            var transform = session.transform(bone.id);
            var matrix = locals[bone.index];
            matrix.setTranslation(matrix.m30() + transform.x(), matrix.m31() + transform.y(), matrix.m32() + transform.z());
            matrix.rotate(transform.rotation());
        }
        for (int i = 0; i < globals.length; i++) global(i);
        evaluationNanos += System.nanoTime() - start;
        evaluations++;
        return globals;
    }

    private Matrix4f global(int index) {
        if (!ready[index]) {
            int parent = getModel().getNodes().get(index).getParentIndex();
            if (parent < 0) globals[index].set(locals[index]);
            else globals[index].set(global(parent)).mul(locals[index]);
            ready[index] = true;
        }
        return globals[index];
    }

    void draw(Matrix4f[] pose, PoseStack stack, MultiBufferSource.BufferSource buffers) {
        long start = System.nanoTime();
        renderModel(getModel(), pose, stack.last(), buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1);
        buffers.endBatch();
        renderNanos += System.nanoTime() - start;
        draws++;
    }

    @Override protected ResourceLocation resolveTexture(RuntimeMaterial material) { return skin; }
}
