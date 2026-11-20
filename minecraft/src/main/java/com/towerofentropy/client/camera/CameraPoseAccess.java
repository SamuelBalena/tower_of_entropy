package com.towerofentropy.client.camera;

import net.minecraft.world.phys.Vec3;

/** Implemented by the Camera coremod bridge so vanilla also updates its block position and basis. */
public interface CameraPoseAccess {
    void toeSetPose(Vec3 position, float yaw, float pitch);
}
