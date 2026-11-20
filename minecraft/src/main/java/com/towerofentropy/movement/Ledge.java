package com.towerofentropy.movement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

/** A ledge is the top of a full block with a clear standing-sized route. */
record Ledge(BlockPos block, Direction wall, Vec3 hang, Vec3 raised, Vec3 stand) {
    static final int CLIMB_TICKS = 18;

    static Ledge find(ServerPlayer player, WallSlide.Contact contact) {
        var block = contact.block();
        double top = block.getY() + 1;
        if (Math.abs(top - player.getY() - 1.76) > .22) return null;
        var wall = contact.side();
        var hang = new Vec3(player.getX(), top - 1.76, player.getZ());
        var raised = new Vec3(hang.x, top + .015, hang.z);
        var stand = raised.add(wall.getStepX() * .68, 0, wall.getStepZ() * .68);
        if (!BlockPos.containing(stand.x, top - .01, stand.z).equals(block)) return null;
        var ledge = new Ledge(block, wall, hang, raised, stand);
        return ledge.clearRoute(player) && ledge.clearStep(player, player.position(), hang) ? ledge : null;
    }

    boolean clearRoute(ServerPlayer p) {
        var level = p.level();
        return level.getBlockState(block).isCollisionShapeFullBlock(level, block)
                && clearStep(p, hang, raised) && clearStep(p, raised, stand);
    }

    boolean clearStep(ServerPlayer p, Vec3 from, Vec3 to) {
        var body = p.getDimensions(Pose.STANDING).makeBoundingBox(from.x, from.y, from.z).deflate(.0001);
        return p.level().noCollision(p, body.expandTowards(to.subtract(from)));
    }

    Vec3 step(int elapsed) {
        if (elapsed <= 11) return hang.lerp(raised, Mth.smoothstep(elapsed / 11.0));
        return raised.lerp(stand, Mth.smoothstep((elapsed - 11) / 7.0));
    }
}
