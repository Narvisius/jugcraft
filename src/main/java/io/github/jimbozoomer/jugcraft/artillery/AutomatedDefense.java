package io.github.jimbozoomer.jugcraft.artillery;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Shared acquisition and impact policy for unattended guns. Manual gunners retain existing PvP rules. */
public final class AutomatedDefense {
    private AutomatedDefense() {}

    public static boolean hostile(LivingEntity target) {
        return target.isAlive() && target instanceof Enemy && !(target instanceof Player)
                && !io.github.jimbozoomer.jugcraft.nouveau.NouveauSummons.owned(target)
                && !(target instanceof OwnableEntity ownable && ownable.getOwnerReference() != null);
    }

    public static boolean clearToFire(ServerLevel level, Vec3 point, double radius) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(point, point).inflate(radius),
                e -> e.isAlive() && !e.isSpectator() && !hostile(e) && e.distanceToSqr(point) <= radius * radius).isEmpty();
    }
}
