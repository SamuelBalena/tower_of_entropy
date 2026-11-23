var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');

function initializeCoreMod() {
    return {
        equippedLightCollision: {
            target: { type: 'CLASS', name: 'net.minecraft.client.multiplayer.ClientLevel' },
            transformer: function (node) {
                var setBlock = ASMAPI.mapMethod('m_6933_');
                var setVerified = ASMAPI.mapMethod('m_233653_');
                var grid = 'com/towerofentropy/client/lighting/EquippedLightCollisionGrid';
                var installed = 0;
                for (var i = 0; i < node.methods.size(); i++) {
                    var method = node.methods.get(i);
                    var stateArgs = '(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;';
                    var local = (method.name === setBlock || method.name === 'setBlock') && method.desc === stateArgs + 'II)Z';
                    var verified = (method.name === setVerified || method.name === 'setServerVerifiedBlockState') &&
                        method.desc === stateArgs + 'I)V';
                    if (!local && !verified) continue;
                    var update = new InsnList();
                    update.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    update.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    update.add(new MethodInsnNode(Opcodes.INVOKESTATIC, grid, 'blockChanged',
                        '(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/core/BlockPos;)V', false));
                    method.instructions.insert(update);
                    installed++;
                }
                // The client keeps scanning on ticks if a loader does not apply this hook.
                if (installed === 2) {
                    for (var j = 0; j < node.methods.size(); j++) {
                        var constructor = node.methods.get(j);
                        if (constructor.name !== '<init>') continue;
                        var code = constructor.instructions.toArray();
                        for (var k = 0; k < code.length; k++) {
                            if (code[k].getOpcode() !== Opcodes.RETURN) continue;
                            var enable = new InsnList();
                            enable.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            enable.add(new MethodInsnNode(Opcodes.INVOKESTATIC, grid, 'enable',
                                '(Lnet/minecraft/client/multiplayer/ClientLevel;)V', false));
                            constructor.instructions.insertBefore(code[k], enable);
                        }
                    }
                }
                return node;
            }
        }
    };
}
