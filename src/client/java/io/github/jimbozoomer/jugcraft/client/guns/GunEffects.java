package io.github.jimbozoomer.jugcraft.client.guns;

import io.github.jimbozoomer.jugcraft.guns.GunItem;
import io.github.jimbozoomer.jugcraft.guns.GunTracePayload;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * What a shot shows besides the gun's own animation (slice 6), on this client only:
 * <ul>
 * <li>the muzzle flash, drawn by {@link GunFlashLayer} for two ticks after each shot. Shots are known here
 * as they happen: the player's own as they fire (GunsClient), others' from the server's GunActionPayload;</li>
 * <li>the spent casing the owner's animations eject at their "eject_casing" cue ({@link #eject}): the round's
 * {@link JugcraftGuns#CASINGS} particle, thrown out to the gun's right. A paper cartridge leaves no case, so a
 * muzzle-loader's cue puffs a little smoke from its lock instead;</li>
 * <li>black powder's white cloud in front of the muzzle with each shot;</li>
 * <li>a bayonet's thrust (slice 7): the gun driven forward and back, in the stabber's hands and, seen from outside,
 * with their arms ({@link #thrust});</li>
 * <li>slice 8C: a flame gun's burst, a spray of flames along the look to the gun's reach; and a rotary gun's barrels,
 * spinning up while the trigger is held and running down after ({@link #barrelTurn}).</li>
 * <li>slice 8D: an energy weapon's shot, where the server says it went ({@link #trace}): a beam of cyan light from the
 * muzzle to its end, or an arc of sparks jagging from the muzzle to each creature it leapt to. Its casing cue vents
 * sparks.</li>
 * <li>slice 10G, two guns at once: each hand's gun has its own flash, and its shots and casings come from its side.</li>
 * </ul>
 */
public final class GunEffects {
	/**
	 * Ticks after a shot until its flash is gone. The level's clock moves on in the tick the shot is fired, so the first
	 * frame drawn after it is a tick old: the flash shows full for its first tick and fades over its second.
	 */
	static final float FLASH_TICKS = 3.0F;
	/** A shot older than this is forgotten. */
	private static final long FORGET_TICKS = 20;
	/**
	 * Each shooter's last shot, by entity id and hand ({@link #key}; slice 10G: a gun in each hand flashes on its own), to
	 * game time. Pruned once it holds more than a few.
	 */
	private static final Map<Long, Long> SHOTS = new HashMap<>();
	/** Ticks a bayonet thrust takes, out and back (slice 7). */
	static final float THRUST_TICKS = 6.0F;
	/** Each stabber's last stab, the same way. */
	private static final Map<Integer, Long> STABS = new HashMap<>();
	/** Each rotary gun holder's barrels (slice 8C), by entity id. Pruned like the shots. */
	private static final Map<Integer, Barrels> BARRELS = new HashMap<>();
	/** The barrels' top speed, in radians a tick: a quarter turn, one barrel on, each two ticks (a shot each). */
	static final double BARREL_SPEED = Math.PI / 4.0;
	/** Ticks the barrels take to run down once the trigger is let go. */
	static final double RUN_DOWN_TICKS = 30.0;
	/** Ticks a shot (another player's, seen from here) keeps the barrels driven. */
	private static final long SHOT_DRIVES = 4;
	/** Flashes drawn and casings (or puffs) thrown, counted for the client game tests. */
	private static long flashes;
	private static long ejected;
	/** Energy weapon shots drawn (slice 8D), counted for the client game tests. */
	private static long traced;
	/** A beam's light: the Beam Pistol's cyan rings. */
	private static final DustParticleOptions BEAM_LIGHT = new DustParticleOptions(0x5FE8F2, 0.9F);
	/** Blocks between two motes of a beam, and between two sparks of an arc. */
	private static final double BEAM_STEP = 0.3;
	private static final double ARC_STEP = 0.25;
	/** The most motes or sparks one stretch of a shot is drawn with. */
	private static final int MOST_MOTES = 200;

	private GunEffects() {
	}

	/** The gun this entity holds fired, now. */
	public static void shot(LivingEntity shooter) {
		shot(shooter, InteractionHand.MAIN_HAND);
	}

	/** The gun in this hand of this entity fired, now (slice 10G: a gun in each hand). */
	public static void shot(LivingEntity shooter, InteractionHand hand) {
		long now = shooter.level().getGameTime();
		if (SHOTS.size() > 32) {
			SHOTS.values().removeIf(time -> now - time > FORGET_TICKS || time > now);
		}
		SHOTS.put(key(shooter.getId(), hand), now);
		ItemStack held = shooter.getItemInHand(hand);
		if (hand == InteractionHand.MAIN_HAND && held.getItem() instanceof GunItem gun && JugcraftGuns.spinUp(gun) > 0) {
			drive(shooter, now + SHOT_DRIVES);
		}
		if (held.getItem() instanceof GunItem gun && JugcraftGuns.shot(gun).equals(JugcraftGuns.FLAME)
				&& shooter.level() instanceof ClientLevel level) {
			flames(level, shooter, gun);
		}
		if (held.getItem() instanceof GunItem gun && gun.spec().ammo().equals("paper_cartridge")
				&& shooter.level() instanceof ClientLevel level) {
			Vec3 eye = shooter.getEyePosition();
			Vec3 look = shooter.getLookAngle();
			RandomSource random = shooter.getRandom();
			for (int i = 0; i < 4; i++) {
				double ahead = 0.9 + i * 0.25;
				level.addParticle(ParticleTypes.CLOUD, eye.x + look.x * ahead, eye.y + look.y * ahead - 0.15, eye.z + look.z * ahead,
						look.x * 0.04 + random.nextGaussian() * 0.01, 0.01 + random.nextGaussian() * 0.01,
						look.z * 0.04 + random.nextGaussian() * 0.01);
			}
		}
	}

	/**
	 * A flame gun's burst (slice 8C): flames thrown from the muzzle along the look, spread over the jet, fast enough to
	 * reach about the gun's range before they die down (a flame slows by 4% a tick).
	 */
	private static void flames(ClientLevel level, LivingEntity shooter, GunItem gun) {
		Vec3 eye = shooter.getEyePosition();
		Vec3 look = shooter.getLookAngle();
		RandomSource random = shooter.getRandom();
		Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0));
		right = right.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
		if (shooter.getMainArm() == HumanoidArm.LEFT) {
			right = right.scale(-1.0);
		}
		Vec3 nozzle = eye.add(look.scale(0.9)).add(right.scale(0.2)).add(0.0, -0.2, 0.0);
		double speed = gun.spec().range() * 0.04 / 0.8;
		double width = Math.toRadians(gun.spec().hipSpread());
		for (int i = 0; i < 8; i++) {
			Vec3 way = look.add(random.nextGaussian() * width * 0.5, random.nextGaussian() * width * 0.5,
					random.nextGaussian() * width * 0.5).normalize().scale(speed * (0.7 + random.nextFloat() * 0.5));
			level.addParticle(i % 4 == 0 ? ParticleTypes.SMOKE : ParticleTypes.FLAME, nozzle.x, nozzle.y, nozzle.z, way.x, way.y, way.z);
		}
	}

	/**
	 * An energy weapon's shot, as the server tells it (slice 8D): from the shooter's muzzle to each point in turn, a beam
	 * of light, or an arc of sparks that jags about the straight line.
	 */
	public static void trace(GunTracePayload payload) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null || payload.points().isEmpty() || !(level.getEntity(payload.shooter()) instanceof LivingEntity shooter)) {
			return;
		}
		traced++;
		RandomSource random = shooter.getRandom();
		Vec3 from = muzzle(shooter, payload.offHand());
		for (Vec3 to : payload.points()) {
			if (payload.kind() == GunTracePayload.BEAM) {
				motes(level, from, to);
			} else {
				sparks(level, from, to, random);
			}
			from = to;
		}
	}

	/** A beam's light along this stretch. */
	private static void motes(ClientLevel level, Vec3 from, Vec3 to) {
		Vec3 way = to.subtract(from);
		int count = (int) Math.min(MOST_MOTES, Math.ceil(way.length() / BEAM_STEP));
		for (int i = 0; i <= count; i++) {
			Vec3 at = from.add(way.scale(count == 0 ? 0.0 : i / (double) count));
			level.addParticle(BEAM_LIGHT, at.x, at.y, at.z, 0.0, 0.0, 0.0);
		}
	}

	/** An arc along this stretch: sparks along a line jagged at a few points, fewer as it nears its ends. */
	private static void sparks(ClientLevel level, Vec3 from, Vec3 to, RandomSource random) {
		Vec3 way = to.subtract(from);
		double length = way.length();
		int bends = Math.max(2, (int) Math.min(8, length / 1.5));
		Vec3 last = from;
		for (int bend = 1; bend <= bends; bend++) {
			double along = bend / (double) bends;
			double swing = bend == bends ? 0.0 : Math.min(0.4, length * 0.06);
			Vec3 next = from.add(way.scale(along)).add(random.nextGaussian() * swing, random.nextGaussian() * swing,
					random.nextGaussian() * swing);
			Vec3 piece = next.subtract(last);
			int count = (int) Math.min(MOST_MOTES / bends, Math.ceil(piece.length() / ARC_STEP));
			for (int i = 0; i <= count; i++) {
				Vec3 at = last.add(piece.scale(count == 0 ? 0.0 : i / (double) count));
				level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 0.0, 0.0, 0.0);
			}
			last = next;
		}
	}

	/**
	 * About where the shooter's muzzle is: ahead of the eye, a little to the side of the hand holding the gun and down
	 * (slice 10G: the other hand's gun is on the other side).
	 */
	private static Vec3 muzzle(LivingEntity shooter, boolean offHand) {
		Vec3 eye = shooter.getEyePosition();
		Vec3 look = shooter.getLookAngle();
		Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0));
		right = right.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
		if ((shooter.getMainArm() == HumanoidArm.LEFT) != offHand) {
			right = right.scale(-1.0);
		}
		return eye.add(look.scale(0.9)).add(right.scale(0.2)).add(0.0, -0.2, 0.0);
	}

	/** Energy weapon shots drawn so far (for the client game tests). */
	public static long traced() {
		return traced;
	}

	/** This entity's rotary gun's trigger is held, now: its barrels are driven for this tick (slice 8C). */
	public static void spinning(LivingEntity holder) {
		drive(holder, holder.level().getGameTime() + 1);
	}

	/** Another player set their barrels spinning: they are driven while they spin up, and longer if shots follow. */
	public static void spun(LivingEntity holder, int spinUp) {
		drive(holder, holder.level().getGameTime() + spinUp + SHOT_DRIVES);
	}

	private static void drive(LivingEntity holder, long until) {
		long now = holder.level().getGameTime();
		if (BARRELS.size() > 32) {
			BARRELS.values().removeIf(barrels -> now - barrels.drivenUntil > FORGET_TICKS + RUN_DOWN_TICKS || barrels.drivenUntil > now + 100);
		}
		Barrels barrels = BARRELS.computeIfAbsent(holder.getId(), id -> new Barrels());
		barrels.drivenUntil = Math.max(barrels.drivenUntil, until);
	}

	/**
	 * How far this entity's rotary gun's barrels have turned (radians), at this frame. They speed up evenly to
	 * {@link #BARREL_SPEED} over the gun's spin-up while driven, and run down evenly over {@link #RUN_DOWN_TICKS} after;
	 * the turn is added up frame by frame, so it carries on smoothly whatever the drive does.
	 */
	public static float barrelTurn(int entityId, long gameTime, float partialTick, int spinUp) {
		Barrels barrels = BARRELS.get(entityId);
		if (barrels == null) {
			return 0.0F;
		}
		double now = gameTime + partialTick;
		double step = Double.isNaN(barrels.time) ? 0.0 : Math.max(0.0, Math.min(5.0, now - barrels.time));
		barrels.time = now;
		boolean driven = gameTime <= barrels.drivenUntil;
		double before = barrels.speed;
		barrels.speed = driven ? Math.min(BARREL_SPEED, before + step * BARREL_SPEED / Math.max(1, spinUp))
				: Math.max(0.0, before - step * BARREL_SPEED / RUN_DOWN_TICKS);
		barrels.turn = (barrels.turn + step * (before + barrels.speed) / 2.0) % (Math.PI * 2.0);
		return (float) barrels.turn;
	}

	/** This entity stabbed with its gun's bayonet, now (slice 7). */
	public static void stabbed(LivingEntity stabber) {
		long now = stabber.level().getGameTime();
		if (STABS.size() > 32) {
			STABS.values().removeIf(time -> now - time > FORGET_TICKS || time > now);
		}
		STABS.put(stabber.getId(), now);
	}

	/** How far into its thrust this entity's bayonet is: 0 at rest, 1 at full reach, out and back over THRUST_TICKS. */
	public static float thrust(int entityId, long gameTime, float partialTick) {
		Long stab = STABS.get(entityId);
		if (stab == null || stab > gameTime) {
			return 0.0F;
		}
		float age = gameTime - stab + partialTick;
		return age < THRUST_TICKS ? (float) Math.sin(Math.PI * age / THRUST_TICKS) : 0.0F;
	}

	/** Ticks since the last shot of the gun in this hand of this entity, if its flash still shows; otherwise -1. */
	static float flashAge(int entityId, InteractionHand hand, long gameTime, float partialTick) {
		Long shot = SHOTS.get(key(entityId, hand));
		if (shot == null || shot > gameTime) {
			return -1.0F;
		}
		float age = gameTime - shot + partialTick;
		return age < FLASH_TICKS ? age : -1.0F;
	}

	/** The game time of the last shot of the gun in this hand of this entity (picks the flash's frame and turn), or 0. */
	static long lastShot(int entityId, InteractionHand hand) {
		return SHOTS.getOrDefault(key(entityId, hand), 0L);
	}

	/** A shot's key: the shooter's entity id and the hand its gun is in. */
	private static long key(int entityId, InteractionHand hand) {
		return (long) entityId << 1 | (hand == InteractionHand.OFF_HAND ? 1L : 0L);
	}

	static void flashDrawn() {
		flashes++;
	}

	/** Flashes drawn so far (for the client game tests). */
	public static long flashes() {
		return flashes;
	}

	/** Casings and lock puffs thrown so far (for the client game tests). */
	public static long ejected() {
		return ejected;
	}

	/**
	 * An animation's "eject_casing" cue on this gun, held by this entity (the player's own when unknown): the round's
	 * spent case flies from the ejection port out to the gun's side and a little up, tumbling. {@code left}: whether the gun
	 * is drawn in a left hand (slice 10G: the other hand's gun throws its cases out to its own side); when unknown, the
	 * holder's main arm decides.
	 */
	static void eject(GunItem gun, int ownerId, @Nullable Boolean left) {
		Minecraft client = Minecraft.getInstance();
		ClientLevel level = client.level;
		if (level == null) {
			return;
		}
		Entity at = ownerId >= 0 ? level.getEntity(ownerId) : client.player;
		if (!(at instanceof LivingEntity holder)) {
			return;
		}
		Vec3 eye = holder.getEyePosition();
		Vec3 look = holder.getLookAngle();
		Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0));
		right = right.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
		if (left != null ? left : holder.getMainArm() == HumanoidArm.LEFT) {
			right = right.scale(-1.0);
		}
		Vec3 port = eye.add(look.scale(0.5)).add(right.scale(0.3)).add(0.0, -0.3, 0.0);
		RandomSource random = holder.getRandom();
		ejected++;
		SimpleParticleType casing = JugcraftGuns.CASINGS.get(gun.spec().ammo());
		if (casing == null) {
			// No case: a muzzle-loader's lock puffs smoke; an energy weapon (slice 8D) vents sparks.
			SimpleParticleType puff = JugcraftGuns.charge(gun) > 0 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.SMOKE;
			level.addParticle(puff, port.x, port.y + 0.1, port.z, 0.0, 0.03, 0.0);
			level.addParticle(puff, port.x, port.y + 0.15, port.z, 0.0, 0.04, 0.0);
			return;
		}
		Vec3 moving = holder.getDeltaMovement();
		Vec3 throwTo = right.scale(0.12 + random.nextFloat() * 0.04).add(look.scale(-0.02))
				.add(moving.x, Math.max(0.0, moving.y), moving.z);
		level.addParticle(casing, port.x, port.y, port.z, throwTo.x + random.nextGaussian() * 0.01,
				0.14 + random.nextFloat() * 0.04, throwTo.z + random.nextGaussian() * 0.01);
	}

	/** A holder's rotary barrels: driven until this game time; their speed (radians a tick), turn and the frame time. */
	private static final class Barrels {
		long drivenUntil = Long.MIN_VALUE;
		double speed;
		double turn;
		double time = Double.NaN;
	}
}
