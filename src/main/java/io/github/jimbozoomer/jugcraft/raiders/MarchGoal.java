package io.github.jimbozoomer.jugcraft.raiders;

import java.util.EnumSet;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Marches a raider on its raid's objective while it has nobody to fight: in legs of up to {@value #LEG} blocks (paths
 * are short), each to the ground on the way. It stops within {@value #ARRIVED} blocks, where it mills about looking for
 * a fight.
 */
final class MarchGoal extends Goal {
	static final int LEG = 16;
	static final int ARRIVED = 6;
	private final PathfinderMob mob;
	private final Supplier<@Nullable BlockPos> objective;
	private final double speed;
	private int repath;

	MarchGoal(PathfinderMob mob, Supplier<@Nullable BlockPos> objective, double speed) {
		this.mob = mob;
		this.objective = objective;
		this.speed = speed;
		setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		BlockPos to = objective.get();
		return to != null && mob.getTarget() == null && mob.distanceToSqr(Vec3.atBottomCenterOf(to)) > ARRIVED * ARRIVED;
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
		if (--repath > 0 && !mob.getNavigation().isDone()) {
			return;
		}
		repath = 40;
		BlockPos to = objective.get();
		if (to == null) {
			return;
		}
		Vec3 here = mob.position();
		Vec3 delta = Vec3.atBottomCenterOf(to).subtract(here);
		double flat = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
		Vec3 step = flat <= LEG ? Vec3.atBottomCenterOf(to) : here.add(delta.x / flat * LEG, 0, delta.z / flat * LEG);
		int x = (int) Math.floor(step.x);
		int z = (int) Math.floor(step.z);
		int y = mob.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		if (!mob.getNavigation().moveTo(x + 0.5, y, z + 0.5, speed)) {
			// No path (a wall, a cliff): head straight for it and let the raider climb what it can.
			mob.getMoveControl().setWantedPosition(x + 0.5, y, z + 0.5, speed);
		}
	}
}
