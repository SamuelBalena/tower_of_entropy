var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode');
var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode');
var FrameNode = Java.type('org.objectweb.asm.tree.FrameNode');
var MethodNode = Java.type('org.objectweb.asm.tree.MethodNode');

var CAMERA = 'net/minecraft/client/Camera';
var CLIENT = 'com/towerofentropy/client/camera/';

function initializeCoreMod() {
    return {
        'shoulder_camera': {
            'target': { 'type': 'CLASS', 'name': 'net.minecraft.client.Camera' },
            'transformer': function (camera) {
                var setup = ASMAPI.mapMethod('m_90575_');
                var maxZoom = ASMAPI.mapMethod('m_90566_');
                var matches = 0;
                for (var i = 0; i < camera.methods.size(); i++) {
                    var method = camera.methods.get(i);
                    if (method.name !== setup ||
                        method.desc !== '(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V') continue;
                    var instructions = method.instructions.toArray();
                    for (var j = 3; j < instructions.length; j++) {
                        var call = instructions[j];
                        if (call.getOpcode() !== Opcodes.INVOKEVIRTUAL || call.owner !== CAMERA
                            || call.name !== maxZoom || call.desc !== '(D)D') continue;
                        // Vanilla third-person dolly: ALOAD camera, ALOAD camera, LDC 4, getMaxZoom.
                        var anchor = instructions[j - 3];
                        if (anchor.getOpcode() !== Opcodes.ALOAD || instructions[j - 2].getOpcode() !== Opcodes.ALOAD
                            || instructions[j - 1].getOpcode() !== Opcodes.LDC)
                            throw new Error('Unexpected Camera.setup third-person dolly');
                        var vanilla = new LabelNode();
                        var hook = new InsnList();
                        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        hook.add(new VarInsnNode(Opcodes.FLOAD, 5));
                        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CLIENT + 'ShoulderCamera',
                            'apply', '(L' + CAMERA + ';F)Z', false));
                        hook.add(new JumpInsnNode(Opcodes.IFEQ, vanilla));
                        hook.add(new InsnNode(Opcodes.RETURN));
                        hook.add(vanilla);
                        hook.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
                        method.instructions.insertBefore(anchor, hook);
                        matches++;
                    }
                }
                if (matches !== 1) throw new Error('Expected one Camera.setup third-person dolly');
                camera.interfaces.add(CLIENT + 'CameraPoseAccess');
                var bridge = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, 'toeSetPose',
                    '(Lnet/minecraft/world/phys/Vec3;FF)V', null, null);
                bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
                bridge.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, CAMERA,
                    ASMAPI.mapMethod('m_90581_'), '(Lnet/minecraft/world/phys/Vec3;)V', false));
                bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                bridge.instructions.add(new VarInsnNode(Opcodes.FLOAD, 2));
                bridge.instructions.add(new VarInsnNode(Opcodes.FLOAD, 3));
                bridge.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, CAMERA,
                    ASMAPI.mapMethod('m_90572_'), '(FF)V', false));
                bridge.instructions.add(new InsnNode(Opcodes.RETURN));
                bridge.maxStack = 3;
                bridge.maxLocals = 4;
                camera.methods.add(bridge);
                ASMAPI.log('INFO', 'Tower of Entropy shoulder camera hook installed');
                return camera;
            }
        }
    };
}
