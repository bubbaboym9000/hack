package com.nightfall.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class TargetUtil {
    private TargetUtil() {
    }

    /** Whether the entity is a living, attackable entity of one of the enabled kinds. */
    public static boolean isValid(Entity e, boolean players, boolean mobs, boolean animals) {
        Minecraft mc = Minecraft.getInstance();
        if (!(e instanceof LivingEntity living)) {
            return false;
        }
        if (e == mc.player || !living.isAlive() || living instanceof ArmorStand || living.isSpectator()) {
            return false;
        }
        if (e instanceof Player) {
            return players;
        }
        if (e instanceof Enemy) {
            return mobs;
        }
        // Animals, villagers, squid and everything else passive.
        return animals;
    }

    /** Closest valid entity within range, or null. */
    public static Entity closest(double range, boolean players, boolean mobs, boolean animals) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) {
            return null;
        }

        Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity e : mc.level.getEntities(p, p.getBoundingBox().inflate(range), x -> true)) {
            if (!isValid(e, players, mobs, animals)) {
                continue;
            }
            double d = p.distanceTo(e);
            if (d > range || d >= bestDistance) {
                continue;
            }
            best = e;
            bestDistance = d;
        }
        return best;
    }

    /** Turns the player's view towards the centre of the entity. */
    public static void face(Entity target) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) {
            return;
        }
        Vec3 eye = p.getEyePosition();
        Vec3 to = target.getBoundingBox().getCenter();
        double dx = to.x - eye.x;
        double dy = to.y - eye.y;
        double dz = to.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        p.setYRot(yaw);
        p.setXRot(Mth.clamp(pitch, -90.0f, 90.0f));
    }
}
