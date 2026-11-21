var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var INVENTORY = 'net/minecraft/world/entity/player/Inventory';
var STACK = 'net/minecraft/world/item/ItemStack';
var STATE = 'net/minecraft/world/level/block/state/BlockState';
var HOOK = 'com/towerofentropy/combat/EffectiveEquipment';

function initializeCoreMod() {
    return {
        'weapon_player_list': {
            'target': { 'type': 'CLASS', 'name': 'net.minecraft.client.gui.Gui' },
            'transformer': function (gui) {
                var list = ASMAPI.mapField('f_92099_');
                var isDown = ASMAPI.mapMethod('m_90857_');
                var mapping = 'net/minecraft/client/KeyMapping';
                var replaced = 0;
                for (var i = 0; i < gui.methods.size(); i++) {
                    var method = gui.methods.get(i);
                    var code = method.instructions.toArray();
                    for (var j = 1; j < code.length; j++) {
                        var node = code[j];
                        var field = code[j - 1];
                        if (node.getOpcode() !== Opcodes.INVOKEVIRTUAL || node.owner !== mapping
                                || node.name !== isDown || field.getOpcode() !== Opcodes.GETFIELD
                                || field.owner !== 'net/minecraft/client/Options' || field.name !== list) continue;
                        method.instructions.set(node, new MethodInsnNode(Opcodes.INVOKESTATIC,
                            'com/towerofentropy/animation/ActionKeys', 'playerListDown', '(L' + mapping + ';)Z', false));
                        replaced++;
                    }
                }
                if (replaced !== 1) throw new Error('Expected one player list binding in Gui');
                return gui;
            }
        },
        'effective_hand': {
            'target': { 'type': 'CLASS', 'name': 'net.minecraft.world.entity.player.Player' },
            'transformer': function (player) {
                var selected = ASMAPI.mapMethod('m_36056_');
                var equipment = ASMAPI.mapMethod('m_6844_');
                var correctTool = ASMAPI.mapMethod('m_36298_');
                var replaced = 0;
                for (var i = 0; i < player.methods.size(); i++) {
                    var method = player.methods.get(i);
                    if (method.name !== equipment && method.name !== correctTool) continue;
                    var code = method.instructions.toArray();
                    for (var j = 0; j < code.length; j++) {
                        var node = code[j];
                        if (node.getOpcode() !== Opcodes.INVOKEVIRTUAL || node.owner !== INVENTORY
                                || node.name !== selected || node.desc !== '()L' + STACK + ';') continue;
                        method.instructions.set(node, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, 'mainHand',
                            '(L' + INVENTORY + ';)L' + STACK + ';', false));
                        replaced++;
                    }
                }
                if (replaced !== 2) throw new Error('Expected main-hand and correct-tool reads in Player');
                return player;
            }
        },
        'effective_mining_speed': {
            'target': { 'type': 'CLASS', 'name': INVENTORY.replace(/\//g, '.') },
            'transformer': function (inventory) {
                var methodName = ASMAPI.mapMethod('m_36020_');
                var replaced = 0;
                for (var i = 0; i < inventory.methods.size(); i++) {
                    var method = inventory.methods.get(i);
                    if (method.name !== methodName || method.desc !== '(L' + STATE + ';)F') continue;
                    var code = method.instructions.toArray();
                    for (var j = 0; j < code.length; j++) {
                        var node = code[j];
                        if (node.getOpcode() !== Opcodes.INVOKEVIRTUAL || node.owner !== STACK
                                || node.desc !== '(L' + STATE + ';)F') continue;
                        method.instructions.insertBefore(node, new VarInsnNode(Opcodes.ALOAD, 0));
                        method.instructions.set(node, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, 'destroySpeed',
                            '(L' + STACK + ';L' + STATE + ';L' + INVENTORY + ';)F', false));
                        method.maxStack++;
                        replaced++;
                    }
                }
                if (replaced !== 1) throw new Error('Expected one stack mining-speed read in Inventory');
                return inventory;
            }
        }
    };
}
