package com.towerofentropy.client.studio;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT)
public final class StudioRuntimeQa {
    private static final boolean ENABLED = Boolean.getBoolean("toe.studio.qa");
    private static final boolean SLIM = Boolean.getBoolean("toe.studio.qa.slim");
    private static int step, ticks, wait;
    private static boolean opening, finished;
    private static StudioProject saved;
    private static StudioProject recoveryExpected;
    private static Vec3 position;
    private static int selectedSlot;
    private static String forced;
    private static long started, gizmoNanos;
    private static boolean pausedForCapture;

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || finished || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        try {
            require(++ticks < 1600, "tempo limite, etapa " + step);
            if (!opening && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                opening = true;
                mc.createWorldOpenFlows().loadLevel(mc.screen, "Global Survival one");
                return;
            }
            if (mc.player == null || mc.getOverlay() != null || ++wait < 4) return;
            switch (step) {
                case 0 -> {
                    if (mc.screen != null || !mc.player.onGround()) return;
                    position = mc.player.position();
                    selectedSlot = mc.player.getInventory().selected;
                    forced = com.towerofentropy.client.PlayerRendererR3.forcedClip;
                    mc.getWindow().setWindowed(854, 480);
                    open();
                }
                case 1 -> {
                    var screen = studio(mc);
                    if (screen == null || (boolean) field(screen, "loading")) return;
                    click(screen, "Ajustes");
                    fields(mc.screen, "2", "30", "neutra", SLIM ? "slim" : "classic");
                }
                case 2 -> {
                    var screen = studio(mc);
                    var rig = rig(screen);
                    require(rig.bones.size() == 12, "esqueleto real");
                    require(rig.resource.getPath().contains("slim") == SLIM, "variante");
                    click(screen, "Modo");
                    select(screen, "3");
                    click(screen, "+ Key");
                }
                case 3 -> {
                    pose(studio(mc), 15, "0", "0", "-75");
                    require(rig(studio(mc)).draws > 0, "renderer Polymesh");
                }
                case 4 -> pose(studio(mc), 30, "0", "25", "-103");
                case 5 -> pose(studio(mc), 45, "0", "-22", "-79");
                case 6 -> pose(studio(mc), 60, "0", "0", "0");
                case 7 -> {
                    var s = studio(mc);
                    var session = session(s);
                    require(session.project.keys().size() == 5, "cinco chaves");
                    float q = Math.abs(session.project.sample("3", 7.5).qz());
                    require(q > 0.30f && q < 0.34f, "interpolação quaternion");
                    seek(s, 15);
                    var rig = rig(s);
                    int child = rig.bone("103").index();
                    var before = rig.evaluate(session)[child].getTranslation(new Vector3f());
                    select(s, "0");
                    rotate(s, "0", "20", "0");
                    require(Math.abs(session.transform("0").qy()) > .15, "inspector cabeça");
                    click(s, "Desfazer");
                    for (String id : List.of("waist", "1", "2", "102", "103", "4", "5", "104", "105")) {
                        select(s, id);
                        rotate(s, "20", "0", "0");
                        require(Math.abs(session.transform(id).qx()) > .15, "rotação local " + id);
                        click(s, "Desfazer");
                    }
                    select(s, "3");
                    require(before.distance(rig.evaluate(session)[child].getTranslation(new Vector3f())) < .0001, "histórico");
                    seek(s, 0);
                    var neutral = rig.evaluate(session)[child].getTranslation(new Vector3f());
                    require(before.distance(neutral) > .12, "descendente acompanhou o braço");
                    seek(s, 15);
                }
                case 8 -> {
                    var s = studio(mc);
                    StudioViewport view = field(s, "viewport");
                    java.util.Map<Integer, Vector3f> handles = field(view, "handles");
                    var head = rig(s).bone("0");
                    var point = handles.get(head.index());
                    s.mouseClicked(point.x, point.y, 0);
                    s.mouseReleased(point.x, point.y, 0);
                    require(((StudioRig.Bone) field(s, "selected")).id().equals("0"), "picking no viewport");
                }
                case 9 -> {
                    var s = studio(mc);
                    StudioViewport view = field(s, "viewport");
                    long input = System.nanoTime();
                    dragRing(mc, s, 1, true, false);
                    gizmoNanos = System.nanoTime() - input;
                    require(!session(s).transform("0").equals(StudioProject.Transform.IDENTITY), "gizmo alterou a pose");
                    click(s, "Desfazer");
                    select(s, "pelvis");
                    List<EditBox> values = field(s, "transforms");
                    values.get(3).setValue("0.15");
                    enter(s, values.get(3));
                    require(Math.abs(session(s).transform("pelvis").x() - .15f) < .0001, "translação local da raiz");
                    click(s, "Desfazer");
                    select(s, "3");
                    double x = ((int) field(s, "left") + (int) field(s, "right")) / 2.0;
                    s.mouseDragged(x, 90, 1, 27, 3);
                    s.mouseScrolled(x, 90, 1);
                    require((float) field(view, "zoom") > 1, "zoom e órbita");
                    click(s, "Foco");
                    click(s, "Câmera");
                    click(mc.screen, "Livre");
                }
                case 10 -> {
                    var s = studio(mc);
                    var session = session(s);
                    int row = rig(s).bones.indexOf((StudioRig.Bone) field(s, "selected")) - (int) field(s, "trackScroll");
                    int y = (int) field(s, "bottom") + 47 + row * 12;
                    int a = frameX(s, 15), b = frameX(s, 18);
                    rotate(s, "0", "0", "-80");
                    s.mouseClicked(a, y, 0);
                    s.mouseDragged(b, y, 0, b - a, 0);
                    s.mouseReleased(b, y, 0);
                    require(session.project.keys().stream().anyMatch(k -> k.frame() == 18
                            && Math.abs(k.transform().qz() + .642788f) < .002)
                            && session.project.keys().stream().noneMatch(k -> k.frame() == 15), "arraste leva a pose atual");
                    click(s, "Desfazer");
                    require(session.project.keys().stream().anyMatch(k -> k.frame() == 15), "desfazer arraste");
                    click(s, "Refazer");
                    require(session.project.keys().stream().anyMatch(k -> k.frame() == 18), "refazer arraste");
                    s.mouseClicked(b, y, 0);
                    s.mouseReleased(b, y, 0);
                    click(s, "Apagar");
                    require(session.project.keys().size() == 4, "apagar chave");
                    click(s, "Desfazer");
                    click(s, "Desfazer");
                    require(session.project.keys().size() == 5, "restaurar cinco chaves");
                    pose(s, 15, "0", "0", "-75");
                    seek(s, 9);
                    click(s, "Auto");
                    rotate(s, "0", "0", "-20");
                    require(session.project.keys().size() == 6, "auto key");
                    click(s, "Desfazer");
                    click(s, "Auto");
                    seek(s, 0);
                    click(s, "Loop");
                    click(s, "Play");
                    started = System.nanoTime();
                }
                case 11 -> {
                    var s = studio(mc);
                    var session = session(s);
                    if (!pausedForCapture) {
                        if (System.nanoTime() - started < 2_300_000_000L) return;
                        require(session.playing && session.frame < 30, "loop em tempo real");
                        click(s, "Play");
                        seek(s, 15);
                        pausedForCapture = true;
                        wait = 0;
                        return;
                    }
                    var rig = rig(s);
                    LogUtils.getLogger().info("TOE_STUDIO PROFILE slim={} draws={} evaluations={} meanEvalMs={} meanRenderMs={} gizmoMs={} heapMiB={} fps={}",
                            SLIM, rig.draws, rig.evaluations, rig.evaluationNanos / (rig.evaluations * 1e6),
                            rig.renderNanos / (rig.draws * 1e6), gizmoNanos / 1e6,
                            (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576, mc.getFps());
                    shot(mc, "854x480");
                    mc.getWindow().setWindowed(1280, 720);
                }
                case 12 -> {
                    if (wait < 16) return;
                    shot(mc, "1280x720");
                    mc.getWindow().setWindowed(1604, 881);
                }
                case 13 -> {
                    if (wait < 16) return;
                    shot(mc, "1604x881");
                    mc.getWindow().setWindowed(1920, 1080);
                }
                case 14 -> {
                    if (wait < 16) return;
                    if (mc.getWindow().getWidth() == 1920 && mc.getWindow().getHeight() == 1080) shot(mc, "1920x1080");
                    click(studio(mc), "Salvar");
                    fields(mc.screen, "teste_aceno");
                }
                case 15 -> {
                    var s = studio(mc);
                    if ((boolean) field(s, "loading")) return;
                    saved = session(s).project;
                    require(saved.name().equals("teste_aceno") && !session(s).dirty(), "salvar JSON");
                    s.onClose();
                }
                case 16 -> {
                    require(mc.screen == null, "fechamento");
                    require(mc.player.getInventory().selected == selectedSlot && mc.player.position().distanceTo(position) < .20
                            && java.util.Objects.equals(forced, com.towerofentropy.client.PlayerRendererR3.forcedClip), "isolamento");
                    mc.setScreen(new ChatScreen("/toe_animstudio"));
                    require(mc.screen.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0), "comando cliente");
                }
                case 17 -> {
                    var s = studio(mc);
                    if (s == null) return;
                    click(s, "Abrir");
                }
                case 18 -> {
                    if (!(mc.screen instanceof StudioLibrary library)) return;
                    library.mouseClicked(mc.getWindow().getGuiScaledWidth() / 2.0, 36, 0);
                    click(library, "Abrir");
                }
                case 19 -> {
                    var s = studio(mc);
                    if (s == null || (boolean) field(s, "loading")) return;
                    require(saved.equals(session(s).project), "reabrir preserva transformações");
                    click(s, "Play");
                    require(session(s).playing, "reprodução reaberta");
                    click(s, "Play");
                    select(s, "1");
                    rotate(s, "0", "18", "0");
                    s.onClose();
                    require(mc.screen instanceof ConfirmScreen, "confirmar saída suja");
                    click(mc.screen, ComponentNames.CANCEL);
                }
                case 20 -> {
                    var s = studio(mc);
                    require(session(s).dirty(), "cancelar preserva pose");
                    var path = mc.gameDirectory.toPath().resolve("config/towerofentropy/animation_studio/projects/invalido.json");
                    Files.writeString(path, "{ projeto inválido");
                    s.libraryOpen("invalido");
                    require(mc.screen instanceof ConfirmScreen, "confirmar troca suja");
                    click(mc.screen, ComponentNames.YES);
                }
                case 21 -> {
                    var s = studio(mc);
                    if (s == null || (boolean) field(s, "loading")) return;
                    String status = field(s, "status");
                    require(status.contains("inválido") && saved.equals(session(s).project), "JSON inválido preserva sessão");
                    click(s, "Salvar +");
                    fields(mc.screen, "copia_qa");
                }
                case 22 -> {
                    var s = studio(mc);
                    if ((boolean) field(s, "loading")) return;
                    require(!session(s).dirty(), "salvar como");
                    click(s, "Abrir");
                }
                case 23 -> {
                    if (!(mc.screen instanceof StudioLibrary library)) return;
                    librarySelect(library, "copia_qa");
                    click(library, "Renomear");
                    fields(mc.screen, "copia_renomeada");
                }
                case 24 -> {
                    if (!(mc.screen instanceof StudioLibrary library)) return;
                    librarySelect(library, "copia_renomeada");
                    click(library, "Excluir");
                    require(mc.screen instanceof ConfirmScreen, "confirmar exclusão");
                    click(mc.screen, ComponentNames.YES);
                }
                case 25 -> {
                    if (!(mc.screen instanceof StudioLibrary library)) return;
                    List<String> projects = field(library, "projects");
                    require(projects.contains("teste_aceno") && !projects.contains("copia_qa")
                            && !projects.contains("copia_renomeada"), "biblioteca após exclusão");
                    click(library, "Voltar");
                    var s = studio(mc);
                    var root = mc.gameDirectory.toPath().resolve("config/towerofentropy/animation_studio/projects");
                    var files = new StudioFiles(mc.gameDirectory.toPath());
                    var original = files.read("teste_aceno");
                    if (!SLIM) {
                        var originalPath = mc.gameDirectory.toPath().resolve("teste_aceno_m003e1.json");
                        var previous = StudioFiles.readPath(originalPath);
                        require(original.equals(previous), "schema 1 e cinco poses M003.E1");
                        Files.copy(originalPath, root.resolve("legado.json"));
                    } else {
                        files.write(original.settings("legado", original.frames(), original.fps(), original.loop(), original.idle()), false);
                    }
                    s.libraryOpen("legado");
                }
                case 26 -> {
                    var s = studio(mc);
                    if ((boolean) field(s, "loading")) return;
                    var p = session(s).project;
                    require(p.keys().equals(saved.keys()) && p.frames() == 60, "reabrir projeto anterior");
                    select(s, "0");
                    rotate(s, "15", "0", "0");
                    click(s, "Salvar");
                    require(mc.screen instanceof ConfirmScreen, "substituição pede confirmação");
                    click(mc.screen, ComponentNames.YES);
                }
                case 27 -> {
                    var s = studio(mc);
                    if ((boolean) field(s, "loading")) return;
                    var root = mc.gameDirectory.toPath().resolve("config/towerofentropy/animation_studio/projects");
                    require(StudioFiles.readPath(root.resolve(".backups/legado.json")).keys().equals(saved.keys()), "backup anterior");
                    var files = new StudioFiles(mc.gameDirectory.toPath());
                    var p = session(s).project;
                    rejectWrite(files, p.settings("../inválido", p.frames(), p.fps(), p.loop(), p.idle()), "nome inválido");
                    rejectWrite(files, p, "conflito de nome");
                    Files.createDirectory(root.resolve("bloqueado.json"));
                    rejectWrite(files, p.settings("bloqueado", p.frames(), p.fps(), p.loop(), p.idle()), "falha de destino");
                    var exported = mc.gameDirectory.toPath().resolve("copia-exportada.json");
                    StudioFiles.export(p, exported);
                    require(StudioFiles.readPath(exported).equals(p), "cópia JSON externa");
                    s.libraryImport(exported.toString(), "importado");
                }
                case 28 -> {
                    if (!(mc.screen instanceof StudioLibrary library)) return;
                    librarySelect(library, "importado");
                    click(library, "Duplicar");
                    fields(mc.screen, "duplicado");
                }
                case 29 -> {
                    if (!(mc.screen instanceof StudioLibrary library)) return;
                    List<String> names = field(library, "projects");
                    require(names.containsAll(List.of("legado", "importado", "duplicado")), "importar e duplicar");
                    click(library, "Voltar");
                    var s = studio(mc);
                    click(s, "Novo");
                    click(s, "Ajustes");
                    fields(mc.screen, "2", "30", "neutra", SLIM ? "slim" : "classic");
                    if (!(boolean) field(s, "anatomical")) click(s, "Modo");
                }
                case 30 -> {
                    var s = studio(mc);
                    require((boolean) field(s, "anatomical"), "modo anatômico padrão");
                    for (String leg : List.of("4", "5")) {
                        select(s, leg);
                        var neutral = limb(s, leg);
                        rotate(s, "0", "0", "45");
                        require(limb(s, leg).z < neutral.z - .10f, "perna para frente " + leg);
                        rotate(s, "0", "45", "0");
                        require((limb(s, leg).x - neutral.x) * (leg.equals("4") ? 1 : -1) > .10f,
                                "abertura espelhada da perna " + leg);
                        rotate(s, "0", "0", "0");
                    }
                    for (String hinge : List.of("102", "103", "104", "105")) {
                        select(s, hinge);
                        List<EditBox> fields = field(s, "transforms");
                        require(!fields.get(0).visible && !fields.get(1).visible && fields.get(2).visible,
                                "modo anatômico expõe só flexão " + hinge);
                        fields.get(2).setValue("45");
                        enter(s, fields.get(2));
                        require(Math.abs(session(s).transform(hinge).qw() - .92388f) < .001, "flexão " + hinge);
                        fields = field(s, "transforms");
                        fields.get(2).setValue("0");
                        enter(s, fields.get(2));
                    }
                    for (String arm : List.of("3", "2")) {
                        select(s, arm);
                        Vector3f neutral = limb(s, arm);
                        rotate(s, "0", "0", "45");
                        require(limb(s, arm).z < neutral.z - .10f, "azul para frente " + arm);
                        rotate(s, "0", "0", "-45");
                        require(limb(s, arm).z > neutral.z + .10f, "azul para trás " + arm);
                        rotate(s, "0", "45", "0");
                        require((limb(s, arm).x - neutral.x) * (arm.equals("2") ? 1 : -1) > .10f, "verde abre " + arm);
                        rotate(s, "0", "-45", "0");
                        require((limb(s, arm).x - neutral.x) * (arm.equals("2") ? -1 : 1) > .10f, "verde fecha " + arm);
                        rotate(s, "45", "0", "0");
                        require(limb(s, arm).distance(neutral) < .0001f, "vermelho torce sem deslocar " + arm);
                        rotate(s, "15", "-15", "90");
                        var q = session(s).transform(arm);
                        click(s, "Modo");
                        click(s, "Modo");
                        require(q.equals(session(s).transform(arm)), "troca de modo preserva pose");
                        rotate(s, "0", "0", "0");
                    }
                    select(s, "3");
                    click(s, "+ Key");
                    click(s, "Câmera");
                    click(mc.screen, "Frente");
                }
                case 31 -> {
                    var s = studio(mc);
                    dragRing(mc, s, 2, true, false);
                    require(limb(s, "3").z < -.03, "arraste azul frontal");
                    pose(s, 15, "0", "0", "45");
                    click(s, "Câmera");
                    click(mc.screen, "Esquerda");
                }
                case 32 -> {
                    shot(mc, "braco-direito-frente");
                    var s = studio(mc);
                    var before = limb(s, "3");
                    dragRing(mc, s, 2, false, true);
                    require(limb(s, "3").distance(before) < .0001, "arraste lateral cancelado");
                    select(s, "2");
                    rotate(s, "0", "45", "0");
                    click(s, "Câmera");
                    click(mc.screen, "Costas");
                }
                case 33 -> {
                    shot(mc, "braco-esquerdo-abertura");
                    var s = studio(mc);
                    dragRing(mc, s, 2, true, true);
                    require(limb(s, "2").z > -.001, "Esc restaurou o rascunho");
                    rotate(s, "0", "0", "0");
                    select(s, "3");
                    pose(s, 30, "0", "45", "0");
                    click(s, "Câmera");
                    click(mc.screen, "Superior");
                }
                case 34 -> {
                    var s = studio(mc);
                    dragRing(mc, s, 0, true, true);
                    pose(s, 45, "30", "30", "35");
                    pose(s, 60, "0", "0", "0");
                    click(s, "Câmera");
                    click(mc.screen, "Livre");
                }
                case 35 -> {
                    shot(mc, "anatomico-orbital");
                    var s = studio(mc);
                    dragRing(mc, s, 1, true, true);
                    seek(s, 0);
                    click(s, "Play");
                    started = System.nanoTime();
                }
                case 36 -> {
                    if (System.nanoTime() - started < 400_000_000L) return;
                    var s = studio(mc);
                    require(session(s).frame > 10 && session(s).frame < 21, "reprodução a 1x");
                    click(s, "Play");
                    seek(s, 0);
                    for (int i = 0; i < 3; i++) click(s, "Vel.");
                    click(s, "Play");
                    started = System.nanoTime();
                }
                case 37 -> {
                    if (System.nanoTime() - started < 750_000_000L) return;
                    var s = studio(mc);
                    require(session(s).frame > 3 && session(s).frame < 9, "reprodução a 0,25x");
                    click(s, "Play");
                    click(s, "Salvar");
                    fields(mc.screen, "teste_bracos_precisao");
                }
                case 38 -> {
                    var s = studio(mc);
                    if ((boolean) field(s, "loading")) return;
                    require(session(s).project.keys().stream().filter(k -> k.bone().equals("3")).count() == 5, "cinco chaves anatômicas");
                    select(s, "2");
                    rotate(s, "30", "15", "0");
                    recoveryExpected = session(s).recoverySnapshot();
                    setField(s, "lastAutosave", System.nanoTime() - 16_000_000_000L);
                    s.tick();
                }
                case 39 -> {
                    var s = studio(mc);
                    var files = new StudioFiles(mc.gameDirectory.toPath());
                    if (files.recoveries().isEmpty()) return;
                    require(files.readRecovery(files.recoveries().get(0)).equals(recoveryExpected), "autosave separado incluiu o rascunho");
                    mc.setScreen(null);
                    open();
                }
                case 40 -> {
                    if (mc.screen == null || !mc.screen.getTitle().getString().equals("Recuperação de projetos")) return;
                    click(mc.screen, "Ignorar");
                    click(studio(mc), "Abrir");
                }
                case 41 -> {
                    if (!(mc.screen instanceof StudioLibrary library)) return;
                    click(library, "Recuperar");
                }
                case 42 -> {
                    if (mc.screen == null || !mc.screen.getTitle().getString().equals("Recuperação de projetos")) return;
                    click(mc.screen, "Recuperar");
                }
                case 43 -> {
                    var s = studio(mc);
                    if (s == null || (boolean) field(s, "loading")) return;
                    require(session(s).project.equals(recoveryExpected) && session(s).dirty()
                            && field(s, "currentFile") == null, "recuperação sem substituir");
                    var p = new StudioFiles(mc.gameDirectory.toPath()).read("teste_bracos_precisao");
                    require(!p.equals(recoveryExpected) && p.keys().stream().filter(k -> k.bone().equals("3")).count() == 5,
                            "reabertura da animação e original protegido");
                    var rig = rig(s);
                    LogUtils.getLogger().info("TOE_STUDIO PASS slim={} oculus={} draws={} evalMs={} renderMs={} heapMiB={} fps={}",
                            SLIM, ModList.get().isLoaded("oculus"), rig.draws, rig.evaluationNanos / 1e6, rig.renderNanos / 1e6,
                            (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576,
                            mc.getFps());
                    finished = true;
                    mc.stop();
                }
                default -> throw new IllegalStateException("Etapa inválida");
            }
            wait = 0;
            step++;
        } catch (Exception | AssertionError error) {
            LogUtils.getLogger().error("TOE_STUDIO FAIL step=" + step, error);
            finished = true;
            mc.stop();
        }
    }

    private static final class ComponentNames {
        static final String CANCEL = net.minecraft.network.chat.CommonComponents.GUI_NO.getString();
        static final String YES = net.minecraft.network.chat.CommonComponents.GUI_YES.getString();
    }
    private static void open() { KeyMapping.click(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F8)); }
    private static AnimationStudioScreen studio(Minecraft mc) { return mc.screen instanceof AnimationStudioScreen s ? s : null; }
    private static StudioSession session(AnimationStudioScreen s) throws Exception { return field(s, "session"); }
    private static StudioRig rig(AnimationStudioScreen s) throws Exception { return field(s, "rig"); }
    private static void require(boolean value, String name) { if (!value) throw new AssertionError(name); }
    private static void click(Screen screen, String text) {
        for (var child : screen.children()) if (child instanceof Button button && button.getMessage().getString().equals(text)) {
            double x = button.getX() + button.getWidth() / 2.0, y = button.getY() + button.getHeight() / 2.0;
            require(screen.mouseClicked(x, y, 0), "botão " + text);
            screen.mouseReleased(x, y, 0);
            return;
        }
        throw new AssertionError("Botão ausente " + text);
    }
    private static void fields(Screen screen, String... values) {
        var boxes = screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).toList();
        require(boxes.size() == values.length, "campos do diálogo");
        for (int i = 0; i < values.length; i++) boxes.get(i).setValue(values[i]);
        enter(screen, boxes.get(0));
    }
    private static void librarySelect(StudioLibrary library, String name) throws Exception {
        List<String> entries = field(library, "projects");
        int index = entries.indexOf(name);
        require(index >= 0, "biblioteca " + name);
        library.mouseClicked(library.width / 2.0, 36 + index * 14, 0);
    }

    private static void rejectWrite(StudioFiles files, StudioProject project, String reason) throws Exception {
        try { files.write(project, false); }
        catch (java.io.IOException expected) { return; }
        throw new AssertionError(reason);
    }

    private static Vector3f limb(AnimationStudioScreen s, String arm) throws Exception {
        var rig = rig(s);
        var pose = rig.evaluate(session(s));
        return pose[rig.bone("10" + arm).index()].getTranslation(new Vector3f())
                .sub(pose[rig.bone(arm).index()].getTranslation(new Vector3f()));
    }

    private static void dragRing(Minecraft mc, AnimationStudioScreen s, int channel, boolean positive, boolean cancel) throws Exception {
        StudioViewport view = field(s, "viewport");
        List<StudioRing> rings = field(view, "rings");
        var ring = rings.stream().filter(r -> r.channel() == channel).findFirst().orElseThrow();
        double best = -1;
        Vector3f start = null;
        StudioRing.Hit picked = null;
        double determinant = ring.u().x * ring.v().y - ring.u().y * ring.v().x;
        boolean edge = Math.abs(determinant) <= .17 * Math.max(ring.u().lengthSquared(), ring.v().lengthSquared());
        for (int i = 0; i < StudioRing.SEGMENTS; i++) {
            var point = ring.point(i * StudioRing.STEP);
            if (point.x <= (int) field(s, "left") + 3 || point.x >= (int) field(s, "right") - 3
                    || point.y <= 62 || point.y >= (int) field(s, "bottom") - 3) continue;
            if (view.pick(point.x, point.y, 2) >= 0) continue;
            var hit = view.ring(point.x, point.y);
            if (hit == null || hit.ring() != ring) continue;
            double tangent = Math.hypot(-Math.sin(hit.angle()) * ring.u().x + Math.cos(hit.angle()) * ring.v().x,
                    -Math.sin(hit.angle()) * ring.u().y + Math.cos(hit.angle()) * ring.v().y);
            if (tangent > best) { best = tangent; start = point; picked = hit; }
        }
        require(start != null && best > 4, "anel selecionável channel=" + channel);
        double direction = positive ? 1 : -1;
        var mouse = mc.mouseHandler;
        var move = mouse.getClass().getDeclaredMethod("onMove", long.class, double.class, double.class);
        var press = mouse.getClass().getDeclaredMethod("onPress", long.class, int.class, int.class, int.class);
        move.setAccessible(true);
        press.setAccessible(true);
        var window = mc.getWindow();
        double scaleX = (double) window.getScreenWidth() / s.width, scaleY = (double) window.getScreenHeight() / s.height;
        move.invoke(mouse, window.getWindow(), start.x * scaleX, start.y * scaleY);
        press.invoke(mouse, window.getWindow(), 0, GLFW.GLFW_PRESS, 0);
        require((int) field(s, "axis") == channel, "MouseHandler selecionou o anel " + channel);
        for (int i = 1; i <= 6; i++) {
            double phase = direction * i * .09;
            var point = edge ? new Vector3f(start)
                    .fma((float)(-Math.sin(picked.angle()) * phase), ring.u())
                    .fma((float)(Math.cos(picked.angle()) * phase), ring.v())
                    : ring.point(picked.angle() + phase);
            move.invoke(mouse, window.getWindow(), point.x * scaleX, point.y * scaleY);
        }
        require((double) field(s, "dragDegrees") * direction > 10, "ângulo assinado, edge=" + edge);
        if (cancel) s.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
        press.invoke(mouse, window.getWindow(), 0, GLFW.GLFW_RELEASE, 0);
    }
    private static void enter(Screen screen, EditBox box) {
        screen.mouseClicked(box.getX() + 2, box.getY() + 2, 0);
        screen.mouseReleased(box.getX() + 2, box.getY() + 2, 0);
        require(screen.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0), "aplicar campos");
        if (Minecraft.getInstance().screen == screen) screen.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
    }
    private static void select(AnimationStudioScreen s, String id) throws Exception {
        var rig = rig(s);
        int index = rig.bones.indexOf(rig.bone(id));
        int rows = Math.max(1, ((int) field(s, "bottom") - 53) / 12);
        while (index < (int) field(s, "treeScroll")) s.mouseScrolled(14, 73, 1);
        while (index >= (int) field(s, "treeScroll") + rows) s.mouseScrolled(14, 73, -1);
        int y = 56 + (index - (int) field(s, "treeScroll")) * 12;
        s.mouseClicked(14, y, 0);
        s.mouseReleased(14, y, 0);
        require(((StudioRig.Bone) field(s, "selected")).id().equals(id), "hierarquia " + id);
    }
    private static void rotate(AnimationStudioScreen s, String x, String y, String z) throws Exception {
        List<EditBox> fields = field(s, "transforms");
        fields.get(0).setValue(x);
        fields.get(1).setValue(y);
        fields.get(2).setValue(z);
        enter(s, fields.get(0));
    }
    private static void pose(AnimationStudioScreen s, int frame, String x, String y, String z) throws Exception {
        seek(s, frame);
        rotate(s, x, y, z);
        click(s, "+ Key");
    }
    private static int frameX(AnimationStudioScreen s, int frame) throws Exception {
        int start = Math.min(118, (int) field(s, "left"));
        return start + Math.round((float) frame / session(s).project.frames() * (s.width - 11 - start));
    }
    private static void seek(AnimationStudioScreen s, int frame) throws Exception {
        int x = frameX(s, frame), y = (int) field(s, "bottom") + 37;
        s.mouseClicked(x, y, 0);
        s.mouseReleased(x, y, 0);
        require(Math.round(session(s).frame) == frame, "playhead " + frame);
    }
    private static void shot(Minecraft mc, String resolution) {
        LogUtils.getLogger().info("TOE_STUDIO FRAMEBUFFER {}x{}", mc.getWindow().getWidth(), mc.getWindow().getHeight());
        Screenshot.grab(mc.gameDirectory, "studio-" + resolution + ".png", mc.getMainRenderTarget(), ignored -> {});
    }
    @SuppressWarnings("unchecked") private static <T> T field(Object instance, String name) throws Exception {
        var field = instance.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(instance);
    }
    private static void setField(Object instance, String name, Object value) throws Exception {
        var field = instance.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(instance, value);
    }
}
