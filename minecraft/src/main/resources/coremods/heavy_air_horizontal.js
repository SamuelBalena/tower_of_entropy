var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var LISTENER = 'net/minecraft/server/network/ServerGamePacketListenerImpl';
var PACKET = 'net/minecraft/network/protocol/game/ServerboundMovePlayerPacket';
var LOCK = 'com/towerofentropy/combat/HeavyAirLock';

function initializeCoreMod() {
    return {
        'heavy_air_horizontal': {
            'target': { 'type': 'CLASS', 'name': LISTENER.replace(/\//g, '.') },
            'transformer': function (listener) {
                var handle = ASMAPI.mapMethod('m_7185_');
                var x = ASMAPI.mapMethod('m_134129_');
                var z = ASMAPI.mapMethod('m_134146_');
                var seenX = 0, seenZ = 0, replaced = 0;
                for (var i = 0; i < listener.methods.size(); i++) {
                    var method = listener.methods.get(i);
                    if (method.name !== handle || method.desc !== '(L' + PACKET + ';)V') continue;
                    var instructions = method.instructions.toArray();
                    for (var j = 0; j < instructions.length; j++) {
                        var node = instructions[j];
                        if (node.getOpcode() !== Opcodes.INVOKEVIRTUAL || node.owner !== PACKET ||
                                node.desc !== '(D)D') continue;
                        // Leave the first reads untouched for vanilla's invalid-packet check.
                        var axis = node.name === x && ++seenX === 2 ? 'x'
                            : node.name === z && ++seenZ === 2 ? 'z' : null;
                        if (axis === null) continue;
                        method.instructions.insertBefore(node, new VarInsnNode(Opcodes.ALOAD, 0));
                        method.instructions.set(node, new MethodInsnNode(Opcodes.INVOKESTATIC, LOCK, axis,
                            '(L' + PACKET + ';DL' + LISTENER + ';)D', false));
                        replaced++;
                    }
                    if (replaced === 2) method.maxStack += 1;
                }
                if (seenX !== 2 || seenZ !== 2 || replaced !== 2)
                    throw new Error('Unexpected ServerGamePacketListenerImpl movement reads');
                ASMAPI.log('INFO', 'Tower of Entropy airborne heavy movement bounds installed');
                return listener;
            }
        }
    };
}
