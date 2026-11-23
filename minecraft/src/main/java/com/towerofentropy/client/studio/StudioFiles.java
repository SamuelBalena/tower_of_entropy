package com.towerofentropy.client.studio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;

final class StudioFiles {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private record Frame(int frame, StudioProject.Transform transform, StudioProject.Curve curve) {}
    private record Track(String bone, List<Frame> keyframes) {}
    private record Stored(int schema, String name, String model, String skeleton, boolean idle, int frames,
                          int fps, boolean loop, List<Track> tracks) {
        static Stored fromProject(StudioProject project) {
            var tracks = new java.util.LinkedHashMap<String, java.util.ArrayList<Frame>>();
            for (var key : project.keys()) tracks.computeIfAbsent(key.bone(), id -> new java.util.ArrayList<>())
                    .add(new Frame(key.frame(), key.transform(), key.curve()));
            var entries = tracks.entrySet().stream().map(entry -> new Track(entry.getKey(), entry.getValue())).toList();
            return new Stored(project.schema(), project.name(), project.model(), project.skeleton(),
                    project.idle(), project.frames(), project.fps(), project.loop(), entries);
        }

        StudioProject project() throws IOException {
            if (tracks == null || tracks.size() > 64) throw new IOException("Tracks de projeto inválidas");
            var keys = new java.util.ArrayList<StudioProject.Key>();
            var bones = new HashSet<String>();
            for (var track : tracks) {
                if (track == null || track.bone == null || !bones.add(track.bone) || track.keyframes == null || track.keyframes.isEmpty())
                    throw new IOException("Track de osso inválida");
                for (var key : track.keyframes) {
                    if (key == null || keys.size() >= 6000) throw new IOException("Keyframes inválidos");
                    keys.add(new StudioProject.Key(track.bone, key.frame, key.transform, key.curve));
                }
            }
            return new StudioProject(schema, name, model, skeleton, idle, frames, fps, loop, keys);
        }
    }
    private final Path directory;

    StudioFiles(Path game) {
        directory = game.resolve("config/towerofentropy/animation_studio/projects").toAbsolutePath().normalize();
    }

    private Path file(String name) throws IOException {
        if (name == null || !name.matches("[\\p{L}\\p{N}_ -]{1,48}") || !name.equals(name.strip())
                || name.matches("(?i)CON|PRN|AUX|NUL|COM[0-9]|LPT[0-9]")) throw new IOException("Nome de projeto inválido");
        Path result = directory.resolve(name + ".json");
        if (Files.isSymbolicLink(result) || !directory.toRealPath().equals(directory))
            throw new IOException("Caminho de projeto inválido");
        return result;
    }

    List<String> names() throws IOException {
        Files.createDirectories(directory);
        if (!directory.toRealPath().equals(directory)) throw new IOException("Diretório de projetos inválido");
        try (var files = Files.list(directory)) {
            return files.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .map(path -> path.getFileName().toString()).filter(name -> name.endsWith(".json"))
                    .map(name -> name.substring(0, name.length() - 5)).sorted().toList();
        }
    }

    StudioProject read(String name) throws IOException {
        Path path = file(name);
        if (Files.size(path) > 4_000_000) throw new IOException("Projeto excede 4 MB");
        try (var input = Files.newBufferedReader(path)) {
            var stored = JSON.fromJson(input, Stored.class);
            if (stored == null) throw new IOException("Projeto vazio");
            return stored.project();
        } catch (RuntimeException invalid) {
            throw new IOException("JSON de projeto inválido", invalid);
        }
    }

    void write(StudioProject project, boolean replace) throws IOException {
        Files.createDirectories(directory);
        var target = file(project.name());
        if (!replace && Files.exists(target, LinkOption.NOFOLLOW_LINKS)) throw new FileAlreadyExistsException(project.name());
        var temp = Files.createTempFile(directory, "studio-", ".tmp");
        try {
            try (var output = Files.newBufferedWriter(temp)) { JSON.toJson(Stored.fromProject(project), output); }
            if (!replace) Files.move(temp, target);
            else {
                try { Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } finally { Files.deleteIfExists(temp); }
    }

    void rename(String oldName, String newName) throws IOException {
        Path source = file(oldName), destination = file(newName);
        if (source.equals(destination)) return;
        StudioProject project = read(oldName);
        write(project.settings(newName, project.frames(), project.fps(), project.loop(), project.idle()), false);
        try { Files.delete(source); }
        catch (IOException failure) {
            try { Files.deleteIfExists(destination); }
            catch (IOException cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }
    void delete(String name) throws IOException { Files.delete(file(name)); }

    static void validate(StudioProject project, StudioRig rig) throws IOException {
        if (project == null || project.schema() != 1 || !rig.signature.equals(project.skeleton())
                || !rig.resource.toString().equals(project.model())
                || project.name() == null || project.frames() < 1 || project.frames() > 7200
                || project.fps() < 1 || project.fps() > 120 || project.keys().size() > 6000)
            throw new IOException("Modelo, esqueleto ou versão de projeto incompatível");
        var positions = new HashSet<String>();
        for (var key : project.keys()) {
            var bone = rig.bone(key.bone());
            if (bone == null || key.frame() < 0 || key.frame() > project.frames() || key.curve() == null
                    || key.transform() == null || !key.transform().valid(bone.parent() < 0)
                    || !positions.add(key.bone() + "/" + key.frame())) throw new IOException("Keyframe inválido");
        }
    }
}
