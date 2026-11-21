var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var IntInsnNode = Java.type('org.objectweb.asm.tree.IntInsnNode');
var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
var HOOK = 'com/towerofentropy/client/ContextualPlayerLayer';
var CHAIN = 'net/minecraft/client/renderer/PostChain';
var TARGET = 'Lcom/mojang/blaze3d/pipeline/RenderTarget;';
var PASS_ARGS = 'Ljava/lang/String;' + TARGET + TARGET;
var PASS = 'Lnet/minecraft/client/renderer/PostPass;';
var RENDERER = 'net/minecraft/client/renderer/entity/EntityRenderer';
var ARGS = 'Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I';

function initializeCoreMod() {
    return {
        'player_fade_texture_cache': {
            'target': {'type': 'CLASS', 'name': 'com.mojang.blaze3d.platform.GlStateManager'},
            'transformer': function (state) {
                // The post shader needs 13 samplers. OpenGL 3.2 guarantees at least 16 per fragment stage.
                var replaced = 0;
                for (var i = 0; i < state.methods.size(); i++) {
                    var method = state.methods.get(i);
                    if (method.name !== '<clinit>') continue;
                    var code = method.instructions.toArray();
                    for (var j = 1; j < code.length; j++) {
                        var range = code[j], size = code[j - 1];
                        if (range.getOpcode() !== Opcodes.INVOKESTATIC || range.owner !== 'java/util/stream/IntStream'
                                || range.name !== 'range' || size.getOpcode() !== Opcodes.BIPUSH || size.operand !== 12) continue;
                        // Oculus also modifies the literal 12. Preserve its injection point and
                        // enforce only our minimum after any upstream capacity adjustment.
                        var minimum = new InsnList();
                        minimum.add(new IntInsnNode(Opcodes.BIPUSH, 16));
                        minimum.add(new MethodInsnNode(Opcodes.INVOKESTATIC, 'java/lang/Math', 'max', '(II)I', false));
                        method.instructions.insert(size, minimum);
                        replaced++;
                    }
                }
                if (replaced !== 1) throw new Error('Expected one Blaze3D texture binding cache');
                return state;
            }
        },
        'player_fade_outputs': {
            'target': {'type': 'CLASS', 'name': 'net.minecraft.client.renderer.RenderStateShard'},
            'transformer': function (shards) {
                var fabulous = ASMAPI.mapMethod('m_91085_');
                var replaced = 0;
                for (var i = 0; i < shards.methods.size(); i++) {
                    var method = shards.methods.get(i);
                    var code = method.instructions.toArray();
                    for (var j = 0; j < code.length; j++) {
                        var node = code[j];
                        if (node.getOpcode() !== Opcodes.INVOKESTATIC
                                || node.owner !== 'net/minecraft/client/Minecraft'
                                || node.name !== fabulous || node.desc !== '()Z') continue;
                        method.instructions.set(node, new MethodInsnNode(Opcodes.INVOKESTATIC,
                            HOOK, 'useWorldTransparencyTargets', '()Z', false));
                        replaced++;
                    }
                }
                if (replaced !== 10) throw new Error('Expected ten Fabulous output routing predicates');
                return shards;
            }
        },
        'player_fade_post': {
            'target': {'type': 'CLASS', 'name': CHAIN.replace(/\//g, '.')},
            'transformer': function (chain) {
                var addPass = ASMAPI.mapMethod('m_110042_');
                var added = 0, closed = 0;
                for (var i = 0; i < chain.methods.size(); i++) {
                    var method = chain.methods.get(i);
                    if (method.name === 'close' && method.desc === '()V') {
                        var close = new InsnList();
                        close.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        close.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK,
                            'closePostChain', '(L' + CHAIN + ';)V', false));
                        method.instructions.insert(close);
                        closed++;
                    }
                    var code = method.instructions.toArray();
                    for (var j = 0; j < code.length; j++) {
                        var node = code[j];
                        if (node.getOpcode() !== Opcodes.INVOKEVIRTUAL || node.owner !== CHAIN
                                || node.name !== addPass || node.desc !== '(' + PASS_ARGS + ')' + PASS) continue;
                        method.instructions.set(node, new MethodInsnNode(Opcodes.INVOKESTATIC,
                            HOOK, 'addPostPass', '(L' + CHAIN + ';' + PASS_ARGS + ')' + PASS, false));
                        added++;
                    }
                }
                if (added !== 1 || closed !== 1) throw new Error('Expected Fabulous post-pass construction and cleanup');
                return chain;
            }
        },
        'player_fade': {
            'target': {'type': 'CLASS', 'name': 'net.minecraft.client.renderer.entity.EntityRenderDispatcher'},
            'transformer': function (dispatcher) {
                var dispatch = ASMAPI.mapMethod('m_114384_');
                var render = ASMAPI.mapMethod('m_7392_');
                var replaced = 0;
                for (var i = 0; i < dispatcher.methods.size(); i++) {
                    var method = dispatcher.methods.get(i);
                    if (method.name !== dispatch) continue;
                    var instructions = method.instructions.toArray();
                    for (var j = 0; j < instructions.length; j++) {
                        var node = instructions[j];
                        if (node.getOpcode() !== Opcodes.INVOKEVIRTUAL || node.owner !== RENDERER
                                || node.name !== render || node.desc !== '(' + ARGS + ')V') continue;
                        method.instructions.set(node, new MethodInsnNode(Opcodes.INVOKESTATIC,
                            'com/towerofentropy/client/ContextualPlayerLayer', 'render',
                            '(L' + RENDERER + ';' + ARGS + ')V', false));
                        replaced++;
                    }
                }
                if (replaced !== 1) throw new Error('Expected one EntityRenderer dispatch for contextual transparency');
                return dispatcher;
            }
        }
    };
}
