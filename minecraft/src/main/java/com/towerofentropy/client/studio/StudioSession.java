package com.towerofentropy.client.studio;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class StudioSession {
    private record State(StudioProject project, Map<String, StudioProject.Transform> edits) {}
    StudioProject project;
    final Map<String, StudioProject.Transform> edits = new HashMap<>();
    double frame;
    boolean playing, autoKey;
    int speed = 2;
    static final double[] SPEEDS = {.25, .5, 1, 1.5, 2};
    private long time;
    private final ArrayDeque<State> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
    private StudioProject saved;

    StudioSession(StudioProject project) { this.project = saved = project; }
    boolean dirty() { return !project.equals(saved) || !edits.isEmpty(); }
    void saved(StudioProject snapshot) { saved = snapshot; }
    StudioProject.Transform transform(String bone) {
        var edited = edits.get(bone);
        return edited == null ? project.sample(bone, frame) : edited;
    }
    private State snapshot() { return new State(project, Map.copyOf(edits)); }
    void checkpoint() {
        undo.push(snapshot());
        if (undo.size() > 48) undo.removeLast();
        redo.clear();
    }
    void history(boolean forward) {
        var source = forward ? redo : undo;
        if (source.isEmpty()) return;
        (forward ? undo : redo).push(snapshot());
        var state = source.pop();
        project = state.project;
        edits.clear();
        edits.putAll(state.edits);
        frame = Math.min(frame, project.frames());
        playing = false;
    }
    void seek(double at) {
        captureEdits();
        frame = Math.max(0, Math.min(project.frames(), at));
        playing = false;
    }
    void toggle() {
        if (!playing) {
            captureEdits();
            if (frame >= project.frames()) frame = 0;
            time = System.nanoTime();
        }
        playing = !playing;
    }
    void advance() {
        if (!playing) return;
        long now = System.nanoTime();
        frame += (now - time) / 1e9 * project.fps() * SPEEDS[speed];
        time = now;
        if (frame < project.frames()) return;
        if (project.loop()) frame %= project.frames();
        else { frame = project.frames(); playing = false; }
    }
    void edit(String bone, StudioProject.Transform value) {
        playing = false;
        edits.put(bone, value);
        if (autoKey) key(bone, currentCurve(bone));
    }
    void key(String bone, StudioProject.Curve curve) {
        project = project.replace(null, new StudioProject.Key(bone, (int) Math.round(frame), transform(bone), curve));
        edits.remove(bone);
    }
    void captureEdits() {
        if (edits.isEmpty()) return;
        checkpoint();
        for (String bone : List.copyOf(edits.keySet())) key(bone, currentCurve(bone));
    }
    private StudioProject.Curve currentCurve(String bone) {
        int at = (int) Math.round(frame);
        return project.keys().stream().filter(key -> key.bone().equals(bone) && key.frame() == at)
                .findFirst().map(StudioProject.Key::curve).orElse(StudioProject.Curve.LINEAR);
    }
}
