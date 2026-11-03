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
 *
 * <p>Stuck recovery: a marcher that has got no nearer its next waypoint for {@value #SIDESTEP_AFTER} re-paths (a player's
 * wall, a pit, a crowd at a gate) tries a sidestep, alternately left and right of its line; after {@value #SKIP_AFTER} it
 * gives that waypoint up and makes for the next. Giving up the last, it stops where it is and, {@value #REPLAN_TICKS}
 * ticks later, plans a fresh route from there (perhaps by another gate). It never breaks blocks and never loads a chunk.
 */
final class MarchGoal extends Goal {
	static final int LEG = 16;
	/** Re-paths (each {@value #REPATH} ticks) without getting a block nearer before it sidesteps, and before it gives up the waypoint. */
	static final int REPATH = 40;
	static final int SIDESTEP_AFTER = 3;
	static final int SKIP_AFTER = 6;
	/** How far to the side a sidestep goes. */
	static final int SIDESTEP = 6;
	/** Ticks after giving up its last waypoint before it plans a fresh route from where it stands (another gate, say). */
	static final int REPLAN_TICKS = 600;
	private final PathfinderMob mob;
	private final Supplier<@Nullable BlockPos> objective;
	private final double speed;
	private int repath;
    private final ArrayDeque<BlockPos> route = new ArrayDeque<>();
    private BlockPos plannedObjective;
    private boolean arrived;
    private boolean cityApproach;
    private @Nullable BlockPos waypoint;
    private double nearest = Double.MAX_VALUE;
    private int stalls;
    /** When a march that gave up (rather than arrived) plans again; -1 when it arrived or is still marching. */
    private int replanAt = -1;

	MarchGoal(PathfinderMob mob, Supplier<@Nullable BlockPos> objective, double speed) {
		this.mob = mob;
		this.objective = objective;
		this.speed = speed;
		setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		BlockPos to = objective.get();
        if (arrived && replanAt >= 0 && mob.tickCount >= replanAt) {
            // It gave its route up as stuck a while ago: plan a fresh one from here.
            plannedObjective = null;
            replanAt = -1;
        }
        if (to != null && !to.equals(plannedObjective)) {
            route.clear(); arrived = false; replanAt = -1; plannedObjective = to;
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
		waypoint = null;   // after a fight it may be anywhere: measure progress afresh
	}

	@Override
	public void tick() {
		if (--repath > 0) {
			return;
		}
		repath = REPATH;
        while (!route.isEmpty() && mob.position().subtract(Vec3.atBottomCenterOf(route.peek())).horizontalDistanceSqr() < 9) route.remove();
        BlockPos to = route.peek();
		if (to == null) {
            arrived = true; mob.getNavigation().stop();
			return;
		}
		Vec3 here = mob.position();
		Vec3 delta = Vec3.atBottomCenterOf(to).subtract(here);
		double flat = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        // Stuck recovery: progress toward this waypoint, re-path by re-path.
        if (!to.equals(waypoint)) { waypoint = to; nearest = flat; stalls = 0; }
        else if (flat < nearest - 1.0) { nearest = flat; stalls = 0; }
        else if (++stalls >= SKIP_AFTER) {
            route.remove();
            waypoint = null;
            stalls = 0;
            if (route.isEmpty()) { arrived = true; replanAt = mob.tickCount + REPLAN_TICKS; mob.getNavigation().stop(); }
            return;
        } else if (stalls >= SIDESTEP_AFTER && flat > 1.0e-3) {
            // Step aside, left then right of the line, to get round what is in the way.
            double side = stalls % 2 == 0 ? SIDESTEP : -SIDESTEP;
            int sx = (int) Math.floor(here.x - delta.z / flat * side), sz = (int) Math.floor(here.z + delta.x / flat * side);
            // Check the column before asking for its height: height lookup can load a missing chunk.
            BlockPos column = new BlockPos(sx, mob.blockPosition().getY(), sz);
            if (mob.level().isLoaded(column)) {
                BlockPos aside = new BlockPos(sx, mob.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz), sz);
                var detour = mob.getNavigation().createPath(aside, 0);
                if (detour != null) { mob.getNavigation().moveTo(detour, speed); return; }
            }
        }
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
