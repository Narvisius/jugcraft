package io.github.jimbozoomer.jugcraft.raiders;

import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Raider crews (docs/features/raiders.md, "Crews"): the Raider Walker's driver sits in the open hatch on its roof, and
 * the Raider Blimp's pilot at the helm of its gondola. A crew member is an ordinary raider infantryman of its raid,
 * riding its machine: the game draws it, moves it with the machine and saves it inside the machine as it does any
 * rider, so it is never duplicated or left behind by an unload and reload.
 *
 * <ul>
 * <li><b>The machine needs its crew.</b> A machine that was crewed and has lost its crew (killed, or knocked off) fails
 * after {@value #ABANDON_TICKS} ticks: it smokes, stops fighting and goes down, as if destroyed. Machines from before
 * crews existed (never crewed) keep working as they always did.</li>
 * <li><b>The crew does not add firepower.</b> While aboard, the crew member's own fighting and marching goals stand
 * idle ({@link Afoot}); the machine fights with its own weapon, as before. Raids crew their machines from the party's
 * own grunts (then gunners), so a party is no larger than before.</li>
 * <li><b>When the machine falls, the crew bails out</b> and fights on foot as the infantry it is: from the blimp with
 * {@value #PARACHUTE_TICKS} ticks of slow falling.</li>
 * <li>A driver in the walker's armoured hatch takes half damage and never suffocates in a low ceiling.</li>
 * </ul>
 */
public final class RaiderCrew {
	/** How far a sitting raider's hips are above its feet (the humanoid model's leg pivot, 12 px). */
	static final double SITTING_HIPS = 0.75;
	/**
	 * Where the driver sits in the walker, in blocks before turning with it: hips 66 px up, 3 px behind the middle, so
	 * the head and shoulders stand out of the hatch (raiders.py WALKER_HATCH).
	 */
	public static final Vec3 WALKER_SEAT = new Vec3(0.0, 66.0 / 16.0 - SITTING_HIPS, -0.2);
	/** Where the pilot sits in the blimp's gondola: on its deck (2.2 px up at the blimp's scale), behind the wheel. */
	public static final Vec3 BLIMP_SEAT = new Vec3(0.0, 0.19 - SITTING_HIPS, 0.3);
	/** Ticks a crewed machine keeps going after its crew is gone, before it fails. */
	public static final int ABANDON_TICKS = 40;
	/** How long the blimp's crew drifts down after bailing out. */
	public static final int PARACHUTE_TICKS = 600;
	/** The share of damage a driver in the walker's armoured hatch takes. */
	public static final float HATCH_COVER = 0.5F;
	/** A crew member's head turns at most this far from the way its machine faces. */
	static final float HEAD_TURN = 75.0F;

	private RaiderCrew() {
	}

	/** Whether {@code entity} is a raider machine with a seat for crew. */
	public static boolean isMachine(@Nullable Entity entity) {
		return entity instanceof RaiderWalker || entity instanceof RaiderBlimp;
	}

	/** Whether {@code entity} is a crew member aboard its machine now. */
	public static boolean aboard(Entity entity) {
		return entity instanceof RaiderInfantry && isMachine(entity.getVehicle());
	}

	/** The machine's living crew member, or null. */
	public static @Nullable RaiderInfantry crew(Entity machine) {
		for (Entity passenger : machine.getPassengers()) {
			if (passenger instanceof RaiderInfantry crew && crew.isAlive()) {
				return crew;
			}
		}
		return null;
	}

	/** Where the machine's seat is, turned with it. */
	static Vec3 seat(Mob machine) {
		Vec3 seat = machine instanceof RaiderWalker ? WALKER_SEAT : BLIMP_SEAT;
		return seat.yRot(-machine.getYRot() * Mth.DEG_TO_RAD);
	}

	/**
	 * Puts {@code crew} in the machine's seat and marks the machine crewed. Joins the machine's raid if it has one.
	 * False (nothing changed) when the seat is taken or the crew cannot ride. Both are normally in the world already; a
	 * crew seated before either is added must be added with its machine ({@code addFreshEntityWithPassengers}).
	 */
	public static boolean board(Mob machine, RaiderInfantry crew) {
		if (!isMachine(machine) || crew(machine) != null || crew.isPassenger() || crew.level() != machine.level()) {
			return false;
		}
		if (machine instanceof Raider raider && raider.raid() != null && raider.objective() != null && crew.raid() == null) {
			crew.joinRaid(raider.raid(), raider.objective());
		}
		crew.snapTo(machine.getX(), machine.getY(), machine.getZ(), machine.getYRot(), 0.0F);
		if (!crew.startRiding(machine, true, true)) {
			return false;
		}
		// No persistence of its own: a rider never despawns while aboard (the game keeps riders), a raid's crew stays
		// for its raid, and the crew of a summoned machine that despawned may despawn too rather than linger.
		if (machine instanceof RaiderWalker walker) {
			walker.setCrewed(true);
		} else if (machine instanceof RaiderBlimp blimp) {
			blimp.setCrewed(true);
		}
		return true;
	}

	/**
	 * A machine made by a command or a spawn egg brings its own crew: one grunt, belonging to no raid (a raid crews its
	 * machines from its party instead, in {@link RaiderRaids#start}). Called from the machine's finalizeSpawn, before
	 * the machine is in the world: the grunt is only seated here, and the game's spawn paths (the summon command, spawn
	 * eggs) then add the machine with its passengers, so it is added once. Does nothing if it already has a rider.
	 */
	static void crewOnSpawn(ServerLevel level, Mob machine, EntitySpawnReason reason) {
		if (reason == EntitySpawnReason.EVENT || machine.isVehicle()) {
			return;
		}
		RaiderInfantry grunt = JugcraftRaiders.GRUNT.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (grunt != null) {
			board(machine, grunt);
		}
	}

	/** Each tick, on both sides: a crew member faces the way its machine does, its head free to turn {@value #HEAD_TURN}°. */
	static void face(LivingEntity crew, Entity machine) {
		float body = machine instanceof LivingEntity living ? living.yBodyRot : machine.getYRot();
		float bodyBefore = machine instanceof LivingEntity living ? living.yBodyRotO : machine.yRotO;
		crew.yBodyRot = body;
		crew.yBodyRotO = bodyBefore;
		crew.setYRot(body);
		crew.setYHeadRot(body + Mth.clamp(Mth.wrapDegrees(crew.getYHeadRot() - body), -HEAD_TURN, HEAD_TURN));
	}

	/**
	 * A crewed machine without its crew: smokes for {@value #ABANDON_TICKS} ticks, then goes down as if destroyed (its
	 * death counts toward the raid, and it drops what it would have). Returns whether the machine is out of action
	 * (so it does nothing else this tick).
	 */
	static boolean abandoned(ServerLevel level, Mob machine, int ticks) {
		if (ticks % 5 == 1) {
			level.sendParticles(ParticleTypes.LARGE_SMOKE, machine.getX(), machine.getY() + machine.getBbHeight() * 0.6, machine.getZ(), 4,
					machine.getBbWidth() / 3, 0.3, machine.getBbWidth() / 3, 0.02);
		}
		if (ticks == 1) {
			level.playSound(null, machine.getX(), machine.getY(), machine.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.0F, 0.6F);
			machine.setTarget(null);
			machine.getNavigation().stop();
		}
		if (ticks >= ABANDON_TICKS && machine.isAlive()) {
			// Its crew may have gone because their raid ended (a crew member checks its raid on its own clock): then the
			// machine leaves as its raid's raiders do, with no drops, rather than going down.
			if (machine instanceof Raider raider && raider.raid() != null && !RaiderRaids.active(level, raider.raid())) {
				RaidMember.withdraw(level, machine);
			} else {
				machine.hurtServer(level, level.damageSources().generic(), Float.MAX_VALUE);
			}
		}
		return true;
	}

	/** The machine has fallen: its crew bails out, from the blimp with time to drift down. */
	static void bailOut(Mob machine) {
		for (Entity passenger : java.util.List.copyOf(machine.getPassengers())) {
			if (passenger instanceof RaiderInfantry crew) {
				crew.stopRiding();
				if (machine instanceof RaiderBlimp) {
					crew.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING,
							PARACHUTE_TICKS, 0, false, false));
				}
			}
		}
	}

	/**
	 * A goal a crew member only pursues on foot: while it is aboard its machine it stands idle (the machine does the
	 * fighting and the moving). Wraps the goal, keeping its flags and every behaviour.
	 */
	public static final class Afoot extends Goal {
		private final Mob mob;
		private final Goal goal;

		public Afoot(Mob mob, Goal goal) {
			this.mob = mob;
			this.goal = goal;
			EnumSet<Goal.Flag> flags = EnumSet.noneOf(Goal.Flag.class);
			flags.addAll(goal.getFlags());
			setFlags(flags);
		}

		public Goal goal() {
			return goal;
		}

		@Override
		public boolean canUse() {
			return !aboard(mob) && goal.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return !aboard(mob) && goal.canContinueToUse();
		}

		@Override
		public boolean isInterruptable() {
			return goal.isInterruptable();
		}

		@Override
		public void start() {
			goal.start();
		}

		@Override
		public void stop() {
			goal.stop();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return goal.requiresUpdateEveryTick();
		}

		@Override
		public void tick() {
			goal.tick();
		}
	}
}
