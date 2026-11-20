var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var InsnList = Java.type('org.objectweb.asm.tree.InsnList');

var LOCAL_PLAYER = 'net/minecraft/client/player/LocalPlayer';
var INPUT = 'net/minecraft/client/player/Input';
var HOOK = 'com/towerofentropy/client/DirectionalSprint';

function initializeCoreMod() {
    return {
        'directional_sprint': {
            'target': { 'type': 'CLASS', 'name': LOCAL_PLAYER.replace(/\//g, '.') },
            'transformer': function (localPlayer) {
                var start = ASMAPI.mapMethod('m_108733_');
                var step = ASMAPI.mapMethod('m_8107_');
                var forward = ASMAPI.mapMethod('m_108577_');
                var sprintKey = ASMAPI.mapField('f_92091_');
                var down = ASMAPI.mapMethod('m_90857_');
                var sprintReads = 0;
                var starts = 0;
                var continuations = 0;
                for (var i = 0; i < localPlayer.methods.size(); i++) {
                    var method = localPlayer.methods.get(i);
                    var instructions = method.instructions.toArray();
                    for (var j = 0; j < instructions.length; j++) {
                        var insn = instructions[j];
                        if (method.name === step && method.desc === '()V' && insn.getOpcode() === Opcodes.GETFIELD
                                && insn.owner === 'net/minecraft/client/Options' && insn.name === sprintKey) {
                            var next = insn.getNext();
                            if (next.getOpcode() === Opcodes.INVOKEVIRTUAL && next.name === down && next.desc === '()Z') {
                                method.instructions.set(next, new MethodInsnNode(Opcodes.INVOKESTATIC,
                                    'com/towerofentropy/client/CombatSprint', 'down', '(Lnet/minecraft/client/KeyMapping;)Z', false));
                                sprintReads++;
                            }
                        }
                        if (method.name === start && method.desc === '()Z' && insn.getOpcode() === Opcodes.IRETURN) {
                            var call = new InsnList();
                            call.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, 'canStart',
                                '(ZL' + LOCAL_PLAYER + ';)Z', false));
                            method.instructions.insertBefore(insn, call);
                            starts++;
                        }
                        if (method.name === step && method.desc === '()V' && insn.getOpcode() === Opcodes.INVOKEVIRTUAL
                                && insn.owner === INPUT && insn.name === forward && insn.desc === '()Z') {
                            method.instructions.insertBefore(insn, new VarInsnNode(Opcodes.ALOAD, 0));
                            method.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, 'hasImpulse',
                                '(L' + INPUT + ';L' + LOCAL_PLAYER + ';)Z', false));
                            continuations++;
                        }
                    }
                }
                if (starts !== 1 || continuations !== 1 || sprintReads !== 2) {
                    throw new Error('Expected one start and one continuation predicate in LocalPlayer');
                }
                ASMAPI.log('INFO', 'Tower of Entropy directional sprint predicates installed');
                return localPlayer;
            }
        }
    };
}
