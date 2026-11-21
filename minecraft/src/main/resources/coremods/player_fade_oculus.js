var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var FieldNode = Java.type('org.objectweb.asm.tree.FieldNode');
var FieldInsnNode = Java.type('org.objectweb.asm.tree.FieldInsnNode');
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode');
var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var IntInsnNode = Java.type('org.objectweb.asm.tree.IntInsnNode');

var ROOT = 'net/irisshaders/iris/';
var HOOK = 'com/towerofentropy/client/ContextualPlayerLayer';
var FRAMEBUFFER = ROOT + 'gl/framebuffer/GlFramebuffer';
var PIPELINE = ROOT + 'pipeline/IrisRenderingPipeline';
var TARGETS = ROOT + 'targets/RenderTargets';
var VANILLA_TARGET = 'com/mojang/blaze3d/pipeline/RenderTarget';
var FRAMEBUFFER_TYPE = 'L' + FRAMEBUFFER + ';';
var PRIVATE_TARGET = 'toe$fadeFramebuffer';

function invoke(name, desc) {
    return new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, name, desc, false);
}

function wrapShader(shader) {
    var wrapped = 0, resets = 0;
    var clear = ASMAPI.mapMethod('m_173362_');
    var apply = ASMAPI.mapMethod('m_173363_');
    var bindWrite = ASMAPI.mapMethod('m_83947_');
    for (var i = 0; i < shader.methods.size(); i++) {
        var method = shader.methods.get(i);
        var code = method.instructions.toArray();
        for (var j = 0; j < code.length; j++) {
            var call = code[j];
            if (call.getOpcode() !== Opcodes.INVOKEVIRTUAL) continue;
            if ((method.name === apply || method.name === 'm_173363_') &&
                    call.owner === FRAMEBUFFER && call.name === 'bind' && call.desc === '()V') {
                var original = new LabelNode();
                var done = new LabelNode();
                var guard = new InsnList();
                guard.add(invoke('bindOculusCapture', '()Z'));
                guard.add(new JumpInsnNode(Opcodes.IFEQ, original));
                guard.add(new InsnNode(Opcodes.POP));
                guard.add(new JumpInsnNode(Opcodes.GOTO, done));
                guard.add(original);
                method.instructions.insertBefore(call, guard);
                method.instructions.insert(call, done);
                wrapped++;
            }
            if ((method.name === clear || method.name === 'm_173362_') && call.owner === VANILLA_TARGET
                    && (call.name === bindWrite || call.name === 'm_83947_') && call.desc === '(Z)V') {
                method.instructions.set(call,
                    invoke('restoreOculusShaderTarget', '(L' + VANILLA_TARGET + ';Z)V'));
                resets++;
            }
        }
    }
    if (wrapped !== 2 || resets !== 1) throw new Error('Unsupported Oculus shader target layout');
    return shader;
}

function insertComposite(method, afterWorld) {
    var noComposite = new LabelNode();
    var targetReady = new LabelNode();
    var code = new InsnList();
    code.add(invoke('hasOculusComposite', '()Z'));
    code.add(new JumpInsnNode(Opcodes.IFEQ, noComposite));
    code.add(new VarInsnNode(Opcodes.ALOAD, 0));
    code.add(new FieldInsnNode(Opcodes.GETFIELD, PIPELINE, PRIVATE_TARGET, FRAMEBUFFER_TYPE));
    code.add(new JumpInsnNode(Opcodes.IFNONNULL, targetReady));
    // Iris owns this framebuffer and refreshes its depth attachment with the other gbuffer targets.
    code.add(new VarInsnNode(Opcodes.ALOAD, 0));
    code.add(new VarInsnNode(Opcodes.ALOAD, 0));
    code.add(new FieldInsnNode(Opcodes.GETFIELD, PIPELINE, 'renderTargets', 'L' + TARGETS + ';'));
    code.add(new VarInsnNode(Opcodes.ALOAD, 0));
    code.add(new FieldInsnNode(Opcodes.GETFIELD, PIPELINE, 'flippedAfterTranslucent', 'Lcom/google/common/collect/ImmutableSet;'));
    code.add(new InsnNode(Opcodes.ICONST_1));
    code.add(new IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_INT));
    code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, TARGETS, 'createGbufferFramebuffer',
        '(Lcom/google/common/collect/ImmutableSet;[I)' + FRAMEBUFFER_TYPE, false));
    code.add(new FieldInsnNode(Opcodes.PUTFIELD, PIPELINE, PRIVATE_TARGET, FRAMEBUFFER_TYPE));
    code.add(targetReady);
    code.add(new VarInsnNode(Opcodes.ALOAD, 0));
    code.add(new FieldInsnNode(Opcodes.GETFIELD, PIPELINE, PRIVATE_TARGET, FRAMEBUFFER_TYPE));
    code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, FRAMEBUFFER, 'getId', '()I', false));
    code.add(invoke('composeOculus', '(I)V'));
    code.add(noComposite);
    code.add(invoke('endOculusWorld', '()V'));
    method.instructions.insert(afterWorld, code);
}

function wrapPipeline(pipeline) {
    var begin = 0, end = 0;
    for (var i = 0; i < pipeline.methods.size(); i++) {
        var method = pipeline.methods.get(i);
        if (method.desc !== '()V') continue;
        if (method.name === 'beginLevelRendering') {
            method.instructions.insert(invoke('beginOculusWorld', '()V'));
            begin++;
        } else if (method.name === 'finalizeLevelRendering') {
            // Iris masks writes from vanilla shaders until it ends the shader-world phase.
            // Compose after its own state transition, before it runs the pack's post passes.
            var instructions = method.instructions.toArray();
            for (var j = 0; j < instructions.length; j++) {
                var node = instructions[j];
                if (node.getOpcode() === Opcodes.PUTFIELD && node.owner === PIPELINE
                        && node.name === 'isRenderingWorld' && node.desc === 'Z') {
                    insertComposite(method, node);
                    end++;
                }
            }
        }
    }
    if (begin !== 1 || end !== 1) throw new Error('Unsupported Oculus world pipeline layout');
    pipeline.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, PRIVATE_TARGET, FRAMEBUFFER_TYPE, null, null));
    return pipeline;
}

function initializeCoreMod() {
    return {
        'oculus_extended': {
            'target': {'type': 'CLASS', 'name': (ROOT + 'pipeline/programs/ExtendedShader').replace(/\//g, '.')},
            'transformer': wrapShader
        },
        'oculus_fallback': {
            'target': {'type': 'CLASS', 'name': (ROOT + 'pipeline/programs/FallbackShader').replace(/\//g, '.')},
            'transformer': wrapShader
        },
        'oculus_world': {
            'target': {'type': 'CLASS', 'name': PIPELINE.replace(/\//g, '.')},
            'transformer': wrapPipeline
        }
    };
}
