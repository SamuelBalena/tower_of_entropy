package com.towerofentropy.animation;

import java.util.Optional;
import com.towerofentropy.combat.CombatDiagnostics;
import com.towerofentropy.movement.ParkourPose;
import com.towerofentropy.movement.WallSlide;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** Own-action intent only: no positions, damage values, target IDs or vanilla attack packets. */
public final class ActionNetwork {
    /** Do not intercept vanilla controls before the matching server channel is present. */
    public static boolean ready(net.minecraft.network.Connection connection){return connection!=null&&CHANNEL.isRemotePresent(connection);}
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(
        new ResourceLocation("towerofentropy","visual_actions"),()->"13","13"::equals,"13"::equals);
    private record ToggleWeapon(){}
    private record CombatRoll(float left, float forward){}
    private record WeaponUpdate(com.towerofentropy.combat.WeaponReadiness.WeaponState weapon,
                                com.towerofentropy.combat.WeaponReadiness.ThreatMode threat, int slot, int itemId){}
    private record AttackMotion(boolean on,long start,double x,double z,double distance,int from,int ticks,int duration,double externalX,double externalZ){}
    private record CombatMode(com.towerofentropy.combat.CombatController.Mode mode){}
    private record CombatState(boolean enabled,String clip,int duration,long started,boolean heavyAir){}
    private record RollFinished(long start){}
    private record AttackRequest(boolean heavy){}
    private record Request(VisualActionWindow.Action action,float left,float forward) { }
    private record RollState(boolean enabled,long start,double x,double z){}
    private record WallSlideState(ParkourPose pose, float yaw) {}
    private record ParkourInput(float strafe, float forward) {}
    public static void register(){
        CHANNEL.messageBuilder(ToggleWeapon.class, 9, NetworkDirection.PLAY_TO_SERVER)
            .encoder((message, buffer) -> {}).decoder(buffer -> new ToggleWeapon())
            .consumerMainThread((message, ctx) -> {
                var p = ctx.get().getSender();
                if (p != null) { incoming(p); com.towerofentropy.combat.WeaponReadiness.toggle(p); }
            }).add();
        CHANNEL.messageBuilder(WeaponUpdate.class, 10, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((state, buffer) -> { buffer.writeEnum(state.weapon()); buffer.writeEnum(state.threat()); buffer.writeByte(state.slot()); buffer.writeVarInt(state.itemId()); })
            .decoder(buffer -> new WeaponUpdate(buffer.readEnum(com.towerofentropy.combat.WeaponReadiness.WeaponState.class),
                    buffer.readEnum(com.towerofentropy.combat.WeaponReadiness.ThreatMode.class), buffer.readUnsignedByte(), buffer.readVarInt()))
            .consumerMainThread((state, ctx) -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> com.towerofentropy.client.ClientWeaponState.accept(state.weapon(), state.threat(), state.slot(), state.itemId())))
            .add();
        CHANNEL.messageBuilder(CombatRoll.class, 11, NetworkDirection.PLAY_TO_SERVER)
            .encoder((state, buffer) -> { buffer.writeFloat(state.left()); buffer.writeFloat(state.forward()); })
            .decoder(buffer -> new CombatRoll(buffer.readFloat(), buffer.readFloat()))
            .consumerMainThread((state, ctx) -> {
                var p = ctx.get().getSender();
                if (p == null) return;
                incoming(p);
                try {
                    String result = com.towerofentropy.combat.RollController.requestContextual(p,state.left(),state.forward());
                    if (Boolean.getBoolean("toe.combat.roll.trace"))
                        LogUtils.getLogger().info("TOE_W0021_ROLL player={} result={} ground={} hurt={} swing={}",
                                p.getScoreboardName(), result, p.onGround(), p.hurtTime, p.swinging);
                    if (!result.equals("accepted") && !com.towerofentropy.combat.RollController.busy(p.getUUID()))
                        rollState(p,false,-1,0,0);
                } catch (Exception error) {
                    failed(p);
                    rollState(p,false,-1,0,0);
                    LogUtils.getLogger().warn("TOE contextual roll rejected", error);
                }
            }).add();

        CHANNEL.messageBuilder(ParkourInput.class, 8, NetworkDirection.PLAY_TO_SERVER)
            .encoder((state, b) -> { b.writeFloat(state.strafe()); b.writeFloat(state.forward()); })
            .decoder(b -> new ParkourInput(b.readFloat(), b.readFloat()))
            .consumerMainThread((state, ctx) -> {
                var player = ctx.get().getSender();
                if (player != null) WallSlide.jump(player, state.strafe(), state.forward());
            }).add();
        CHANNEL.messageBuilder(WallSlideState.class, 7, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((state, buffer) -> { buffer.writeEnum(state.pose()); buffer.writeFloat(state.yaw()); })
            .decoder(buffer -> new WallSlideState(buffer.readEnum(ParkourPose.class), buffer.readFloat()))
            .consumerMainThread((state, context) -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.towerofentropy.client.ClientWallVisual.accept(state.pose(), state.yaw())))
            .add();
        CHANNEL.registerMessage(6,RollFinished.class,(s,b)->b.writeLong(s.start()),b->new RollFinished(b.readLong()),(s,c)->{var ctx=c.get();ctx.enqueueWork(()->{var p=ctx.getSender();if(p!=null){incoming(p);com.towerofentropy.combat.RollController.finished(p,s.start());}});ctx.setPacketHandled(true);},Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(5,AttackMotion.class,(s,b)->{b.writeBoolean(s.on);b.writeLong(s.start);b.writeDouble(s.x);b.writeDouble(s.z);b.writeDouble(s.distance);b.writeVarInt(s.from);b.writeVarInt(s.ticks);b.writeVarInt(s.duration);b.writeDouble(s.externalX);b.writeDouble(s.externalZ);},b->new AttackMotion(b.readBoolean(),b.readLong(),b.readDouble(),b.readDouble(),b.readDouble(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readDouble(),b.readDouble()),(s,c)->{var ctx=c.get();ctx.enqueueWork(()->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->com.towerofentropy.client.ClientAttackMotion.accept(s.on,s.start,s.x,s.z,s.distance,s.from,s.ticks,s.duration,s.externalX,s.externalZ)));ctx.setPacketHandled(true);},Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(4,CombatMode.class,(s,b)->b.writeEnum(s.mode()),b->new CombatMode(b.readEnum(com.towerofentropy.combat.CombatController.Mode.class)),(s,c)->{var ctx=c.get();ctx.enqueueWork(()->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->com.towerofentropy.client.ClientCombatState.mode=s.mode()));ctx.setPacketHandled(true);},Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(2,CombatState.class,(s,b)->{b.writeBoolean(s.enabled());b.writeUtf(s.clip(),32);b.writeVarInt(s.duration());b.writeLong(s.started());b.writeBoolean(s.heavyAir());},b->new CombatState(b.readBoolean(),b.readUtf(32),b.readVarInt(),b.readLong(),b.readBoolean()),(s,c)->{var ctx=c.get();ctx.enqueueWork(()->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->com.towerofentropy.client.ClientCombatState.accept(s.enabled(),s.clip(),s.duration(),s.started(),s.heavyAir())));ctx.setPacketHandled(true);},Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(3,AttackRequest.class,(s,b)->b.writeBoolean(s.heavy()),b->new AttackRequest(b.readBoolean()),(s,c)->{var ctx=c.get();ctx.enqueueWork(()->{var p=ctx.getSender();if(p!=null)try{incoming(p);String result=com.towerofentropy.combat.CombatController.requestAttack(p,s.heavy());var state=com.towerofentropy.combat.CombatController.status(p.getUUID());if(!result.equals("accepted")&&!result.equals("queued")&&(state==null||!state.attacking()))attackMotion(p,false,-1,0,0,0,0,1,0,0,0);}catch(Exception ex){failed(p);LogUtils.getLogger().warn("TOE attack rejected",ex);}});ctx.setPacketHandled(true);},Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(0,Request.class,(request,buffer)->{buffer.writeEnum(request.action());buffer.writeFloat(request.left());buffer.writeFloat(request.forward());},
            buffer->new Request(buffer.readEnum(VisualActionWindow.Action.class),buffer.readFloat(),buffer.readFloat()),(request,supplier)->{
                var context=supplier.get();
                context.enqueueWork(()->{var player=context.getSender();if(player!=null)try{incoming(player);
                    if(request.action()==VisualActionWindow.Action.SWORD_ATTACK)
                        com.towerofentropy.combat.CombatController.inputSword(player);
                    else {String result=com.towerofentropy.combat.RollController.request(player,request.left(),request.forward());if(!result.equals("accepted")&&!com.towerofentropy.combat.RollController.busy(player.getUUID()))rollState(player,false,-1,0,0);}
                }catch(Exception ex){failed(player);LogUtils.getLogger().warn("TOE_M001_3 KEY_ACTION_FAILED",ex);}});
                context.setPacketHandled(true);
            },Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1,RollState.class,(state,b)->{b.writeBoolean(state.enabled());b.writeLong(state.start());b.writeDouble(state.x());b.writeDouble(state.z());},b->new RollState(b.readBoolean(),b.readLong(),b.readDouble(),b.readDouble()),(state,supplier)->{var context=supplier.get();context.enqueueWork(()->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->com.towerofentropy.client.ClientRollMotion.accept(state.enabled(),state.start(),state.x(),state.z())));context.setPacketHandled(true);},Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    private static void incoming(net.minecraft.server.level.ServerPlayer player) {
        var diagnostics = CombatDiagnostics.of(player);
        if (diagnostics != null) diagnostics.incoming();
    }
    private static void outgoing(net.minecraft.server.level.ServerPlayer player) {
        var diagnostics = CombatDiagnostics.of(player);
        if (diagnostics != null) diagnostics.outgoing();
    }
    private static void failed(net.minecraft.server.level.ServerPlayer player) {
        var diagnostics = CombatDiagnostics.of(player);
        if (diagnostics != null) diagnostics.error();
    }

    public static void request(VisualActionWindow.Action action){CHANNEL.sendToServer(new Request(action,0,1));}
    public static void attackMotion(net.minecraft.server.level.ServerPlayer p,boolean on,long start,double x,double z,double distance,int from,int ticks,int duration,double externalX,double externalZ){outgoing(p); CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->p),new AttackMotion(on,start,x,z,distance,from,ticks,duration,externalX,externalZ));}
    public static void requestAttack(boolean heavy){CHANNEL.sendToServer(new AttackRequest(heavy));}
    public static void combatMode(net.minecraft.server.level.ServerPlayer p,com.towerofentropy.combat.CombatController.Mode mode){outgoing(p); CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->p),new CombatMode(mode));}
    public static void combatState(net.minecraft.server.level.ServerPlayer p,boolean enabled,String clip,int duration,int elapsed){
        combatState(p,enabled,clip,duration,elapsed,false);
    }
    public static void combatState(net.minecraft.server.level.ServerPlayer p,boolean enabled,String clip,int duration,int elapsed,boolean heavyAir){
        outgoing(p); CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->p),new CombatState(enabled,clip,duration,p.serverLevel().getGameTime()-elapsed,heavyAir));
    }
    public static void toggleWeapon(){CHANNEL.sendToServer(new ToggleWeapon());}
    public static void requestCombatRoll(float left,float forward){CHANNEL.sendToServer(new CombatRoll(left,forward));}
    public static void weaponState(net.minecraft.server.level.ServerPlayer p,
            com.towerofentropy.combat.WeaponReadiness.WeaponState weapon,
            com.towerofentropy.combat.WeaponReadiness.ThreatMode threat){
        p.level().getProfiler().incrementCounter("toeWeaponPacket");
        outgoing(p); CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->p),new WeaponUpdate(weapon,threat,p.getInventory().selected,net.minecraft.world.item.Item.getId(com.towerofentropy.combat.EffectiveEquipment.selected(p).getItem())));
    }
    public static void parkourPose(net.minecraft.server.level.ServerPlayer player, ParkourPose pose, float yaw) {
        CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), new WallSlideState(pose, yaw));
    }
    public static void requestParkour(float strafe, float forward) {
        CHANNEL.sendToServer(new ParkourInput(strafe, forward));
    }
    public static void finishRoll(long start){CHANNEL.sendToServer(new RollFinished(start));}
    public static void requestRoll(float left,float forward){CHANNEL.sendToServer(new Request(VisualActionWindow.Action.ROLL,left,forward));}
    public static void rollState(net.minecraft.server.level.ServerPlayer player,boolean enabled,long start,double x,double z){outgoing(player); CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->player),new RollState(enabled,start,x,z));}
}
