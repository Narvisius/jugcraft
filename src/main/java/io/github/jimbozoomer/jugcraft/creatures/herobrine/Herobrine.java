package io.github.jimbozoomer.jugcraft.creatures.herobrine;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Herobrine (docs/features/herobrine.md): a figure who stares at the one player he came for, and is gone the moment
 * anyone comes within {@value #VANISH_DISTANCE} blocks, stares back too long, or hits him. He never attacks, breaks a
 * block, carries a name tag or is saved with the world: {@link HerobrineEncounters} places him for one encounter, as one
 * of the {@link Variant}s, and he removes himself. While he stands he notes every attentive player who really sees him
 * (on their screen, nothing in the way, for {@value #SEEN_TICKS} ticks); when he goes, the encounter ends with those
 * witnesses.
 */
public final class Herobrine extends PathfinderMob {
	/** A distant encounter ends when any player comes this close, in blocks. */
	public static final double VANISH_DISTANCE = 12.0;
	/** A distant encounter ends after the player has looked straight at him for this many ticks. */
	public static final int STARE_TICKS = 40;
	/** The longest a distant encounter lasts, in ticks. */
	public static final int DISTANT_TICKS = 600;
	/** The longest a close encounter lasts, in ticks: time to turn round. */
	public static final int CLOSE_TICKS = 60;
	/** A close encounter ends once the player has looked at him this many ticks: long enough to know what they saw. */
	public static final int CLOSE_STARE_TICKS = 12;
	/** A player has seen him once he has been on their screen, unobstructed, for this many ticks. */
	public static final int SEEN_TICKS = 10;
	/** Players further than this cannot see him, and his target going further ends the encounter, in blocks. */
	public static final double WITNESS_RANGE = 96.0;
	/** A second look: once seen, he moves or goes after his player has looked away this many ticks. */
	public static final int LOOK_AWAY_TICKS = 10;
	/** A follower sends one footstep for this many ticks of walking. */
	public static final int FOOTSTEP_EVERY = 6;
	/** How loud a follower's footsteps are: quiet, and only his player hears them. */
	public static final float FOOTSTEP_VOLUME = 0.3F;
	/** A follower's footsteps sound this far behind the player, towards him, in blocks (quiet sounds carry 16). */
	public static final double FOOTSTEP_FROM = 8.0;
	/** How much of his player's path a follower remembers, in points. */
	private static final int MAX_TRAIL = 400;
	/** How near the middle of a player's view he must be to count as on their screen: the cosine of the angle. */
	private static final double ON_SCREEN_COSINE = 0.6;
	/** How directly a player must look at him to count as staring: the cosine of the angle off their aim. */
	private static final double DISTANT_STARE_COSINE = 0.985;
	private static final double CLOSE_STARE_COSINE = 0.85;

	/** What kind of encounter he is here for: to be glimpsed at a distance, or to stand right behind someone. */
	public enum Kind {
		DISTANT, CLOSE
	}

	/** How he goes about it. Every variant but {@link #BEHIND} is a distant encounter. */
	public enum Variant {
		/** He stands and watches, on a ridge or in a clearing where he can be seen, by several players if they are together. */
		WATCHER(Kind.DISTANT),
		/** He walks the player's own path behind them, keeping his distance, freezing while watched; his steps are faint. */
		FOLLOWER(Kind.DISTANT),
		/** He watches; once seen, the next time the player looks away he is gone, or standing somewhere else. */
		SECOND_LOOK(Kind.DISTANT),
		/** He stands at the far, dark end of a straight tunnel the player is not facing. */
		TUNNEL(Kind.DISTANT),
		/** He stands right behind the player. */
		BEHIND(Kind.CLOSE);

		public final Kind kind;

		Variant(Kind kind) {
			this.kind = kind;
		}
	}

	/** Why he went. */
	public enum Outcome {
		/** His player looked straight at him long enough. */
		STARED_DOWN,
		/** A player came within {@value #VANISH_DISTANCE} blocks of a distant encounter. */
		APPROACHED,
		/** A player hit him. */
		HIT,
		/** His time ran out (or, on a second look, he was simply gone when they looked back). */
		EXPIRED,
		/** His player logged out, died, left the dimension or went out of range. */
		TARGET_LOST,
		/** He was removed some other way: his chunk unloaded, the server stopped, or a command. */
		REMOVED
	}

	private Variant variant = Variant.WATCHER;
	private @Nullable UUID target;
	private UUID encounter = UUID.randomUUID();
	private int life = DISTANT_TICKS;
	private int stared;
	private @Nullable Outcome outcome;
	private final Map<UUID, Integer> watching = new HashMap<>();
	private final Set<UUID> witnesses = new LinkedHashSet<>();
	private final Deque<Vec3> trail = new ArrayDeque<>();
	private double followDistance;
	private int steps;
	private int away;
	private boolean relocated;

	public Herobrine(EntityType<? extends Herobrine> type, Level level) {
		super(type, level);
		setNoAi(true);
	}

	public static AttributeSupplier.Builder attributes() {
		return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	/**
	 * Sends him, where he already stands, for {@code player} as encounter {@code id}, and turns him their way. A follower
	 * keeps the distance he starts at, and walks the straight line to the player before following their own steps.
	 */
	public void haunt(Variant variant, Player player, UUID id) {
		this.variant = variant;
		target = player.getUUID();
		encounter = id;
		life = variant.kind == Kind.CLOSE ? CLOSE_TICKS : DISTANT_TICKS;
		stared = 0;
		trail.clear();
		if (variant == Variant.FOLLOWER) {
			followDistance = position().distanceTo(player.position());
			for (double d = 0.5; d < followDistance; d += 0.5) {
				trail.addLast(position().lerp(player.position(), d / followDistance));
			}
		}
		face(player);
	}

	public Kind kind() {
		return variant.kind;
	}

	public Variant variant() {
		return variant;
	}

	public @Nullable UUID target() {
		return target;
	}

	public UUID encounter() {
		return encounter;
	}

	/** Every player who has really seen him so far, in the order they did. */
	public Set<UUID> witnesses() {
		return Collections.unmodifiableSet(witnesses);
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level) || isRemoved()) {
			return;
		}
		Player player = target == null ? null : level.getPlayerByUUID(target);
		if (player == null || !player.isAlive() || player.isSpectator() || distanceToSqr(player) > WITNESS_RANGE * WITNESS_RANGE) {
			end(level, Outcome.TARGET_LOST);
			return;
		}
		if (variant == Variant.FOLLOWER) {
			follow(level, player);
		}
		face(player);
		noteWitnesses(level);
		if (variant.kind == Kind.DISTANT && someoneClose(level, position())) {
			end(level, Outcome.APPROACHED);
			return;
		}
		if (variant == Variant.SECOND_LOOK && secondLook(level, player)) {
			return;
		}
		stared = isStaredAtBy(player) ? stared + 1 : 0;
		if (stared >= (variant.kind == Kind.CLOSE ? CLOSE_STARE_TICKS : STARE_TICKS)) {
			end(level, Outcome.STARED_DOWN);
		} else if (--life <= 0) {
			end(level, Outcome.EXPIRED);
		}
	}

	/**
	 * A follower remembers where his player walks, and while they are not looking at him walks there himself, as far
	 * behind as he started; a step sounds faintly for them every {@value #FOOTSTEP_EVERY} ticks he walks. When they
	 * stop, he stops, and so do his steps.
	 */
	private void follow(ServerLevel level, Player player) {
		Vec3 at = player.position();
		if (trail.isEmpty() || trail.peekLast().distanceToSqr(at) > 0.09) {
			trail.addLast(at);
			if (trail.size() > MAX_TRAIL) {
				trail.removeFirst();
			}
		}
		if (isOnScreenOf(player)) {
			return;
		}
		boolean moved = false;
		while (!trail.isEmpty() && trail.peekFirst().distanceToSqr(at) > followDistance * followDistance) {
			Vec3 next = trail.removeFirst();
			BlockPos feet = BlockPos.containing(next);
			if (HerobrineEncounters.standable(level, feet)) {
				setPos(next.x, feet.getY(), next.z);
				moved = true;
			}
		}
		if (moved && ++steps % FOOTSTEP_EVERY == 0 && player instanceof ServerPlayer listener) {
			SoundEvent step = level.getBlockState(blockPosition().below()).getSoundType().getStepSound();
			Vec3 from = at.add(position().subtract(at).normalize().scale(FOOTSTEP_FROM));
			listener.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(step), SoundSource.HOSTILE,
					from.x, from.y, from.z, FOOTSTEP_VOLUME, 0.85F, level.getRandom().nextLong()));
		}
	}

	/**
	 * A second look: once his player has seen him and then looked away for {@value #LOOK_AWAY_TICKS} ticks, he is gone,
	 * or (half the time, if there is a place) standing somewhere else they could see him. Once only. Returns whether he
	 * moved or went this tick.
	 */
	private boolean secondLook(ServerLevel level, Player player) {
		if (relocated || !witnesses.contains(player.getUUID())) {
			return false;
		}
		away = isOnScreenOf(player) ? 0 : away + 1;
		if (away < LOOK_AWAY_TICKS) {
			return false;
		}
		relocated = true;
		Vec3 next = player instanceof ServerPlayer moved && level.getRandom().nextBoolean()
				? HerobrineEncounters.relocation(level, moved, position()) : null;
		if (next == null) {
			outcome = Outcome.EXPIRED;
			discard();
			return true;
		}
		snapTo(next.x, next.y, next.z, getYRot(), getXRot());
		stared = 0;
		life = Math.max(life, DISTANT_TICKS / 2);
		return true;
	}

	/**
	 * Counts, for each player, the ticks in a row he has been on their screen while they were attentive; any other tick
	 * (looking away, something in the way, distracted, out of range) starts their count again. {@value #SEEN_TICKS} in
	 * a row makes them a witness: scattered glimpses never add up to a sighting.
	 */
	private void noteWitnesses(ServerLevel level) {
		Set<UUID> watchingNow = new HashSet<>();
		for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(WITNESS_RANGE),
				p -> !p.isSpectator() && p.isAlive())) {
			// Only someone paying attention can see him: not idle, asleep, in a menu or in danger.
			if (witnesses.contains(player.getUUID()) || !(player instanceof ServerPlayer watcher)
					|| !HerobrineEncounters.attentive(watcher, level.getServer().getTickCount()) || !isOnScreenOf(player)) {
				continue;
			}
			watchingNow.add(player.getUUID());
			if (watching.merge(player.getUUID(), 1, Integer::sum) >= SEEN_TICKS) {
				witness(player);
			}
		}
		watching.keySet().retainAll(watchingNow);
	}

	private void witness(Player player) {
		if (witnesses.add(player.getUUID())) {
			HerobrineEncounters.seen(this, player.getUUID(), variant);
		}
	}

	/** Whether a player (not spectating) is within {@value #VANISH_DISTANCE} blocks of {@code at}. */
	static boolean someoneClose(ServerLevel level, Vec3 at) {
		return !level.getEntitiesOfClass(Player.class, new AABB(at, at).inflate(VANISH_DISTANCE),
				p -> !p.isSpectator() && p.distanceToSqr(at) < VANISH_DISTANCE * VANISH_DISTANCE).isEmpty();
	}

	/** Whether he is on {@code player}'s screen, in range, with nothing in the way. */
	public boolean isOnScreenOf(Player player) {
		return within(player, ON_SCREEN_COSINE) && distanceToSqr(player) <= WITNESS_RANGE * WITNESS_RANGE && clearSight(player);
	}

	/** Whether {@code player} is looking straight at him, with nothing in the way. */
	public boolean isStaredAtBy(Player player) {
		return within(player, variant.kind == Kind.CLOSE ? CLOSE_STARE_COSINE : DISTANT_STARE_COSINE) && clearSight(player);
	}

	/** A line of sight, cast only when every chunk along it is already loaded: a look never loads the world. */
	private boolean clearSight(Player player) {
		return HerobrineEncounters.corridorLoaded(level(), player.getEyePosition(), getEyePosition()) && player.hasLineOfSight(this);
	}

	private boolean within(Player player, double cosine) {
		Vec3 toHim = getEyePosition().subtract(player.getEyePosition());
		double length = toHim.length();
		return length < 1.0E-4 || player.getViewVector(1.0F).dot(toHim.scale(1.0 / length)) > cosine;
	}

	private void face(Player player) {
		double dx = player.getX() - getX();
		double dz = player.getZ() - getZ();
		double dy = player.getEyeY() - getEyeY();
		float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		setYRot(yaw);
		yBodyRot = yaw;
		setYHeadRot(yaw);
		setXRot(pitch);
	}

	/** Ends the encounter: a wisp of smoke where he stood, and he is gone. */
	public void end(ServerLevel level, Outcome why) {
		if (outcome == null) {
			outcome = why;
		}
		level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.0, getZ(), 6, 0.2, 0.5, 0.2, 0.0);
		discard();
	}

	@Override
	public void remove(RemovalReason reason) {
		if (level() instanceof ServerLevel && !isRemoved()) {
			HerobrineEncounters.ended(this, outcome == null ? Outcome.REMOVED : outcome);
		}
		super.remove(reason);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.hurtServer(level, source, amount);
		}
		// A blow or an arrow from a player meets nothing: he is gone, and nobody is hurt. Whoever struck him saw him.
		if (source.getEntity() instanceof Player player) {
			witness(player);
			end(level, Outcome.HIT);
		}
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}
}
