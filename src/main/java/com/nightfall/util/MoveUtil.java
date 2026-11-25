package com.nightfall.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Options;
import net.minecraft.world.phys.Vec3;

public final class MoveUtil {
    private MoveUtil() {
    }

    public static boolean isMoving() {
        Options o = Minecraft.getInstance().options;
        return o.keyUp.isDown() || o.keyDown.isDown() || o.keyLeft.isDown() || o.keyRight.isDown();
    }

    /** True when you are on the ground and the block just ahead of you (in your motion) has no floor. */
    public static boolean edgeAhead() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !mc.player.onGround()) {
            return false;
        }
        Vec3 v = mc.player.getDeltaMovement();
        double px = mc.player.getX() + v.x * 2.5;
        double pz = mc.player.getZ() + v.z * 2.5;
        BlockPos below = BlockPos.containing(px, mc.player.getY() - 0.5, pz);
        return mc.level.getBlockState(below).getCollisionShape(mc.level, below).isEmpty();
    }

    /**
     * Horizontal velocity vector (x, 0, z) of the given length in the direction the player is
     * steering, based on the held movement keys and the current yaw.
     */
    public static Vec3 direction(double speed) {
        Minecraft mc = Minecraft.getInstance();
        Options o = mc.options;
        double forward = (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0);
        double strafe = (o.keyLeft.isDown() ? 1 : 0) - (o.keyRight.isDown() ? 1 : 0);
        if (forward == 0 && strafe == 0) {
            return Vec3.ZERO;
        }

        double yaw = Math.toRadians(mc.player.getYRot());
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);

        double x = -sin * forward + cos * strafe;
        double z = cos * forward + sin * strafe;
        double length = Math.sqrt(x * x + z * z);
        return new Vec3(x / length * speed, 0, z / length * speed);
    }
}
