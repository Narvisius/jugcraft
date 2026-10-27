package io.github.jimbozoomer.jugcraft.creatures.herobrine;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jspecify.annotations.Nullable;

/**
 * A guard's glance (docs/features/herobrine.md): a guard who sees Herobrine within {@value #RANGE} blocks turns to look at
 * him for {@value #GLANCE_TICKS} game ticks (two seconds), once per encounter, and then goes back to what it was doing.
 * The glance is timed by the guard's own tick count, not by counting calls: goals are weighed only every other tick, and
 * a goal is ticked every tick only when it asks to be. The guard pauses its patrol to look:
 * the goal never sets a target or moves toward him, and gives way to any fight (it will not start while the guard
 * has a target). Herobrine is neither a monster nor an enemy, so no guard's attack goal or gun ever picks him. Lines of
 * sight are only cast over loaded chunks.
 */
public final class HerobrineGlanceGoal extends Goal {
	/** How long a guard looks, in game ticks. */
	public static final int GLANCE_TICKS = 40;
	/** How far away a guard notices him, in blocks. */
	public static final double RANGE = 32.0;

	private final Mob guard;
	private final BooleanSupplier onDuty;
	private @Nullable Herobrine seen;
	private @Nullable UUID lastEncounter;
	private int until;
	private int nextScan;

	/** A glance for {@code guard}, while {@code onDuty} says it is a guard. */
	public HerobrineGlanceGoal(Mob guard, BooleanSupplier onDuty) {
		this.guard = guard;
		this.onDuty = onDuty;
		// A concurrent patrol turns the body and clamps the head away from the apparition.
		// Own MOVE for these two seconds so the guard can actually finish its glance.
		setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		// Look round for him every half second at most (goals are weighed every other tick, so count, do not divide).
		if (guard.tickCount < nextScan || guard.getTarget() != null || !onDuty.getAsBoolean()) {
			return false;
		}
		nextScan = guard.tickCount + 10;
		seen = guard.level().getEntitiesOfClass(Herobrine.class, guard.getBoundingBox().inflate(RANGE),
						herobrine -> herobrine.isAlive() && !herobrine.encounter().equals(lastEncounter) && guard.distanceToSqr(herobrine) <= RANGE * RANGE
								&& HerobrineEncounters.corridorLoaded(guard.level(), guard.getEyePosition(), herobrine.getEyePosition())
								&& guard.hasLineOfSight(herobrine))
				.stream().min(Comparator.comparingDouble(guard::distanceToSqr)).orElse(null);
		return seen != null;
	}

	@Override
	public void start() {
		guard.getNavigation().stop();
		until = guard.tickCount + GLANCE_TICKS;
		lastEncounter = seen.encounter();
	}

	@Override
	public boolean canContinueToUse() {
		return guard.tickCount < until && seen != null && seen.isAlive() && guard.getTarget() == null;
	}

	/** Turn the head every tick, so the look is smooth. */
	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (seen != null) {
			guard.getLookControl().setLookAt(seen, 30.0F, 30.0F);
		}
	}

	@Override
	public void stop() {
		seen = null;
	}
}
