package io.github.jimbozoomer.jugcraft.guns;

import com.geckolib.animatable.GeoItem;
import java.util.EnumSet;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/**
 * Guns in the hands of mobs (slice 10F, docs/features/guns.md): the raider gunners (raiders/RaiderInfantry) carry the
 * service arms ({@link #ARMS}) and fire them with the guns' own numbers, through the players' bullet code
 * ({@link GunShots#bullets}). Keep the numbers in sync with tools/guns.py; tools/check_mod_data.py checks them.
 * <ul>
 * <li>A shot does {@value #DAMAGE} of the gun's damage a pellet. The bullet damage type scales with the game's
 * difficulty against players, as a monster's attack does.</li>
 * <li>A gunner closes on its target until it can see it from within its gun's reach, then fires in bursts: an automatic
 * gun {@value #BURST} shots at its interval, any other one shot, then it waits {@value #PAUSE} ticks. Each shot goes from
 * its eye at the middle of its target, strayed by the gun's spread from the hip.</li>
 * <li>It counts its magazine and, when it is empty, reloads for the gun's own reload time. It needs no rounds, so the
 * gun in its hand stays empty: one that drops (a chance of {@value #DROP}, more with Looting) holds none.</li>
 * <li>Its bullets pass through its own side ({@link Gunner#spares}), marker stands and what it rides.</li>
 * <li>The clients that see it play the shot and the reload on its gun, as they do another player's
 * ({@link GunActionPayload}).</li>
 * </ul>
 */
public final class MobGuns {
	/** The share of the gun's damage a mob's shot does. */
	public static final float DAMAGE = 0.5F;
	/** Shots in an automatic gun's burst. */
	public static final int BURST = 3;
	/** Ticks a gunner waits after a burst (or a single shot). */
	public static final int PAUSE = 30;
	/** The chance a gunner's gun drops when a player kills it (Looting adds to it, as to any mob's equipment). */
	public static final float DROP = 0.085F;
	/** The reach, in blocks, a gun not in {@link #ARMS} is fired from (or its range, if shorter). */
	public static final int REACH = 16;
	/** The guns raider gunners carry: how many in ten carry each, and how near they close to fire it (blocks). */
	public static final List<Arm> ARMS = List.of(new Arm("sentry_pistol", 4, 16), new Arm("garrison_rifle", 3, 24),
			new Arm("breacher", 3, 8));

	/** One of {@link #ARMS}: the gun, how many in ten carry it, and the reach it is fired from. */
	public record Arm(String gun, int weight, int reach) {
	}

	/** A mob that fires the gun in its hand ({@link FireGoal}). */
	public interface Gunner {
		/** Whether its bullets pass through this creature (its own side). */
		boolean spares(LivingEntity other);
	}

	private MobGuns() {
	}

	/**
	 * A gun for a new gunner, chosen by the arms' weights. On the server it gets its animation id at once, so the
	 * clients play its first shot on it (a mob's equipment is not ticked as an inventory's is).
	 */
	public static ItemStack arm(Level level, RandomSource random) {
		ItemStack gun = arm(random);
		if (level instanceof ServerLevel server) {
			GeoItem.getOrAssignId(gun, server);
		}
		return gun;
	}

	/** A gun for a new gunner, chosen by the arms' weights. */
	public static ItemStack arm(RandomSource random) {
		int roll = random.nextInt(ARMS.stream().mapToInt(Arm::weight).sum());
		for (Arm arm : ARMS) {
			roll -= arm.weight();
			if (roll < 0) {
				return new ItemStack(JugcraftGuns.GUNS.get(arm.gun()));
			}
		}
		return new ItemStack(JugcraftGuns.GUNS.get(ARMS.getFirst().gun()));
	}

	/** How near a mob closes on its target to fire this gun (blocks). */
	public static int reach(GunItem gun) {
		for (Arm arm : ARMS) {
			if (arm.gun().equals(gun.name())) {
				return arm.reach();
			}
		}
		return Math.min(REACH, gun.spec().range());
	}

	/** Whether a mob can fire this gun: it fires bullets (the other kinds of shot are the players' alone). */
	public static boolean fires(GunItem gun) {
		return "bullet".equals(JugcraftGuns.shot(gun));
	}

	/** One shot of the gun in the shooter's main hand at its target: the bullets, the sound and the clients told. */
	static void fire(ServerLevel level, Mob shooter, LivingEntity target, ItemStack stack, GunItem gun) {
		// The gun's animations play by its id (a gun given to the mob some other way may not have one yet).
		GeoItem.getOrAssignId(stack, level);
		GunSpec spec = GunItem.spec(stack);
		Vec3 eye = shooter.getEyePosition();
		Vec3 look = target.getBoundingBox().getCenter().subtract(eye).normalize();
		GunShots.bullets(level, shooter, eye, look, spec, spec.hipSpread(), spec.damage() * DAMAGE, JugcraftGuns.ignites(gun),
				foe -> foe != shooter && foe.isAlive() && !foe.isSpectator() && !(foe instanceof ArmorStand stand && stand.isMarker())
						&& foe.getRootVehicle() != shooter.getRootVehicle() && !(shooter instanceof Gunner gunner && gunner.spares(foe)),
				foe -> true);
		level.playSound(null, eye.x, eye.y, eye.z, JugcraftGuns.sound("guns." + gun.name() + ".fire"), SoundSource.HOSTILE,
				GunItem.volume(stack), 0.95F + shooter.getRandom().nextFloat() * 0.1F);
		shooter.gameEvent(GameEvent.PROJECTILE_SHOOT);
		announce(shooter, GunActionPayload.SHOOT, 0);
	}

	/** Tells the clients that see the mob what its gun does, to play it. */
	static void announce(Mob shooter, int action, int rounds) {
		GunActionPayload payload = new GunActionPayload(shooter.getId(), action, rounds);
		for (ServerPlayer watcher : PlayerLookup.tracking(shooter)) {
			if (ServerPlayNetworking.canSend(watcher, GunActionPayload.TYPE)) {
				ServerPlayNetworking.send(watcher, payload);
			}
		}
	}

	/**
	 * A mob's fight with the gun in its main hand (see the class comment): it closes until it can see its target from
	 * within the gun's reach, then fires in bursts and reloads. It keeps its magazine count while it lives.
	 */
	public static class FireGoal extends Goal {
		/** Ticks it must have seen its target before it stops closing in. */
		private static final int SIGHTED = 5;
		private final PathfinderMob mob;
		private final double speed;
		/** Rounds left in the magazine; -1 until the gun is first fired. */
		private int rounds = -1;
		private int wait;
		private int reloading;
		private int burst;
		private int seen;

		public FireGoal(PathfinderMob mob, double speed) {
			this.mob = mob;
			this.speed = speed;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		/** Rounds left in its magazine (-1 before it first fires). */
		public int rounds() {
			return rounds;
		}

		/** Whether it is reloading. */
		public boolean reloading() {
			return reloading > 0;
		}

		@Override
		public boolean canUse() {
			LivingEntity target = mob.getTarget();
			return target != null && target.isAlive() && mob.getMainHandItem().getItem() instanceof GunItem gun && fires(gun);
		}

		@Override
		public void stop() {
			seen = 0;
			burst = 0;
			mob.getNavigation().stop();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity target = mob.getTarget();
			ItemStack stack = mob.getMainHandItem();
			if (target == null || !(stack.getItem() instanceof GunItem gun) || !(mob.level() instanceof ServerLevel level)) {
				return;
			}
			GunSpec spec = GunItem.spec(stack);
			if (rounds < 0 || rounds > spec.capacity()) {
				rounds = spec.capacity();
			}
			int reach = reach(gun);
			boolean sees = mob.getSensing().hasLineOfSight(target);
			seen = sees ? seen + 1 : 0;
			boolean near = mob.distanceToSqr(target) <= (double) reach * reach;
			if (near && seen >= SIGHTED) {
				mob.getNavigation().stop();
			} else {
				mob.getNavigation().moveTo(target, speed);
			}
			mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
			if (reloading > 0) {
				if (--reloading == 0) {
					rounds = spec.capacity();
				}
				return;
			}
			if (wait > 0) {
				wait--;
				return;
			}
			if (!sees || !near) {
				return;
			}
			if (rounds <= 0) {
				reloading = Math.max(1, spec.reloadTicks(spec.capacity()));
				burst = 0;
				announce(mob, GunActionPayload.RELOAD, spec.capacity());
				return;
			}
			fire(level, mob, target, stack, gun);
			rounds--;
			burst++;
			if (spec.auto() && burst < BURST && rounds > 0) {
				wait = spec.interval();
			} else {
				burst = 0;
				wait = Math.max(spec.interval(), PAUSE);
			}
		}
	}
}
