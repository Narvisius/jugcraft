package io.github.jimbozoomer.jugcraft.raiders;

import java.util.EnumSet;
import java.util.function.Supplier;
import java.util.ArrayDeque;
import io.github.jimbozoomer.jugcraft.town.Town;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Marches a raider on its raid's objective while it has nobody to fight: in legs of up to {@value #LEG} blocks (paths
 * are short), each to the ground on the way. It stops within three blocks, where it mills about looking for
 * a fight.
 */
final class MarchGoal extends Goal {
	static final int LEG = 16;
	private final PathfinderMob mob;
	private final Supplier<@Nullable BlockPos> objective;
	private final double speed;
	private int repath;
    private final ArrayDeque<BlockPos> route = new ArrayDeque<>();
    private BlockPos plannedObjective;
    private boolean arrived;
    private boolean cityApproach;

	MarchGoal(PathfinderMob mob, Supplier<@Nullable BlockPos> objective, double speed) {
		this.mob = mob;
		this.objective = objective;
		this.speed = speed;
		setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		BlockPos to = objective.get();
        if (to != null && !to.equals(plannedObjective)) {
            route.clear(); arrived = false; plannedObjective = to;
            BlockPos origin = Town.origin(mob.level());
            cityApproach = origin != null && to.equals(Town.centre(origin));
            if (cityApproach) {
                route.addAll(CityRaidRoute.approach(origin, mob.position()));
                // Siege walkers cannot fit below the raised portcullis; fight
                // from the open forecourt, in sight of the gate defenders.
                if (mob.getBbHeight() > 4 && route.size() >= 3) { route.removeLast(); route.removeLast(); }
            } else route.add(to);
        }
        return to != null && !arrived && mob.getTarget() == null;
	}

	@Override
	public boolean canContinueToUse() {
		return canUse();
	}

	@Override
	public void start() {
		repath = 0;
	}

	@Override
	public void tick() {
		if (--repath > 0) {
			return;
		}
		repath = 40;
        while (!route.isEmpty() && mob.position().subtract(Vec3.atBottomCenterOf(route.peek())).horizontalDistanceSqr() < 9) route.remove();
        BlockPos to = route.peek();
		if (to == null) {
            arrived = true; mob.getNavigation().stop();
			return;
		}
		Vec3 here = mob.position();
		Vec3 delta = Vec3.atBottomCenterOf(to).subtract(here);
		double flat = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        // Let vanilla see the complete local bend/gate, instead of ending a
        // leg on an obstacle that it could otherwise walk around.
		Vec3 step = flat <= LEG * 2 ? Vec3.atBottomCenterOf(to) : here.add(delta.x / flat * LEG, 0, delta.z / flat * LEG);
		int x = (int) Math.floor(step.x);
		int z = (int) Math.floor(step.z);
        BlockPos stepPos = new BlockPos(x, to.getY(), z);
        if (!mob.level().isLoaded(stepPos)) return;
        // Heightmaps point at gatehouse roofs. Prefer the road beneath them.
        int y = to.getY();
        if (!mob.level().getBlockState(stepPos).getCollisionShape(mob.level(), stepPos).isEmpty()
                || !mob.level().getBlockState(stepPos.below()).isSolid())
            y = mob.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        var path = mob.getNavigation().createPath(new BlockPos(x, y, z), 0);
        // Outside the protected city route, a partial path brings siege troops
        // to the enclosing wall so their ladder goal can operate. Stopping at
        // the gathering point prevents them from ever reaching that wall.
        if (path != null && (path.canReach() || !cityApproach)) mob.getNavigation().moveTo(path, speed);
        else mob.getNavigation().stop();
	}

    @Override public void stop() { mob.getNavigation().stop(); }
}
