package io.github.jimbozoomer.jugcraft.town;

import io.github.jimbozoomer.jugcraft.artillery.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Fixed city services; no chunk tickets, player ownership, ammunition inventories or salvage. */
public final class TownDefense {
    public static final int RANGE = 250;
    private TownDefense() {}

    public static LivingEntity target(ServerLevel level, net.minecraft.world.entity.Entity shooter, Vec3 from) {
        return target(level, shooter, from, candidate -> true);
    }

    public static LivingEntity target(ServerLevel level, net.minecraft.world.entity.Entity shooter, Vec3 from,
            java.util.function.Predicate<LivingEntity> reachable) {
        LivingEntity best = null;
        double distance = RANGE * RANGE;
        for (var candidate : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, from).inflate(RANGE), AutomatedDefense::hostile)) {
            double d = candidate.distanceToSqr(from);
            if (d >= distance || !reachable.test(candidate) || !visible(level, shooter, from, candidate.getBoundingBox().getCenter())) continue;
            best = candidate; distance = d;
        }
        return best;
    }

    public static boolean visible(ServerLevel level, net.minecraft.world.entity.Entity shooter, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter)).getType() == HitResult.Type.MISS;
    }

    public static void crew(ServerLevel level, Townsfolk guard) {
        if (!guard.role().equals("flak_guard") || guard.spot() < 0 || guard.isPassenger()
                || !level.isPositionEntityTicking(guard.home())) return;
        var guns = level.getEntitiesOfClass(FlakGun.class, new AABB(guard.home()).inflate(4),
                gun -> guard.home().equals(gun.townStation()));
        FlakGun gun;
        if (guns.isEmpty()) {
            gun = JugcraftArtillery.FLAK_GUN.create(level, EntitySpawnReason.STRUCTURE);
            if (gun == null) return;
            gun.setTownStation(guard.home());
            gun.setPos(Vec3.atBottomCenterOf(guard.home()));
            if (!level.noCollision(gun) || !level.addFreshEntity(gun)) return;
        } else {
            gun = guns.getFirst();
            for (int i = 1; i < guns.size(); i++) guns.get(i).discard();
        }
        guard.getNavigation().stop();
        guard.setPos(Vec3.atBottomCenterOf(guard.home()));
        guard.startRiding(gun);
    }
}
