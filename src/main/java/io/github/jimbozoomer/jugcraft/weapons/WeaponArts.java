package io.github.jimbozoomer.jugcraft.weapons;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.mixin.AttackStrengthAccessor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Weapon arts (Arms V, batch 48, docs/features/arms-v.md): the special move of each Arms V kind ({@link JugcraftArms#ARTS}),
 * used with the use key with the arm in the main hand ({@link ArmItem#use}). Each plays its own animation on every client
 * that sees the wielder ({@link WeaponArtPayload}; client/arms/ArmsMotion) and deals its damage in a shape of its own, on
 * the server:
 * <ul>
 * <li>{@link JugcraftArms.Move#CYCLONE}: three spins, each striking every foe all round and drawing it in;</li>
 * <li>{@link JugcraftArms.Move#IAIDO}: a dash; every foe passed is marked, and the cut lands on all of them a moment after
 * the dash ends;</li>
 * <li>{@link JugcraftArms.Move#LEAP_SLAM}: a leap; where the wielder lands, every foe about is struck, hardest at the
 * centre and harder the further below the take-off, and thrown up;</li>
 * <li>{@link JugcraftArms.Move#FLURRY}: quick jabs at the foe ahead, each landing in full and holding it in reach, then a
 * driving finish;</li>
 * <li>{@link JugcraftArms.Move#CRESCENT}: a wave that runs ahead, through every foe in its way (each weakening it), until
 * a block stops it;</li>
 * <li>{@link JugcraftArms.Move#CHAIN_LASH}: a chain at the first foe in line, which is struck and hauled in, then reaped
 * as it arrives;</li>
 * <li>{@link JugcraftArms.Move#SEVEN_CUTS} (Arms VI): seven quick cuts across every foe ahead, each drawn in the air as an
 * arc of the katana's colour.</li>
 * </ul>
 * Every hit is the arm's attack damage times the move's share, struck through vanilla's thrust attack (Player.stabAttack:
 * enchantments, knockback, wear, the item's hit hooks) at a full charge, after letting go of the foe's damage cooldown,
 * so a quick run of hits all land. The foes are those a two-handed blow may strike ({@link TwoHanded#target},
 * {@link TwoHanded#allowed}: never allies, tamed pets of the wielder, their mount or foes other code protects); a pull or
 * a throw follows only a hit that landed. The arm then needs its art's cooldown (an item cooldown, shown on the hotbar)
 * before the art is ready again; plain blows are not held back.
 *
 * <p>While busy with an art the wielder cannot swing or hit with the arm (vanilla's hit is refused, as for two-handed
 * arms), and is slowed by the art's share. Switching away from the arm, dying or leaving ends it. An art that moves its
 * wielder (iaido, the leap) uses vanilla's impulse rule for falls: only a drop below where it began counts. None works
 * from the saddle but the flurry, the crescent, the chain and the seven cuts. The work each tick is over the arts in
 * progress only.
 */
public final class WeaponArts {
	/** The movement modifier an art puts on its wielder while they are busy with it. */
	public static final Identifier SLOW = Jugcraft.id("weapon_art");
	/** The colours of the seven cuts' arcs: crimson for a bronze katana, pale gold for a steel one. */
	private static final int CUT_BRONZE = 0xC41E2A;
	private static final int CUT_STEEL = 0xF2D27A;
	/** An attack-strength ticker past any arm's delay: every hit of an art is at a full charge. */
	private static final int FULL_CHARGE = 1000;
	private static final Map<UUID, Active> ACTIVE = new HashMap<>();
	private static long now;

	private WeaponArts() {
	}

	/** An art in progress: its wielder and arm, when it began, where and which way, and what the move remembers. */
	private static final class Active {
		final ServerPlayer player;
		final ItemStack stack;
		final JugcraftArms.Art art;
		final long start;
		final Vec3 origin;
		final Vec3 heading;
		/** Iaido: the foes the dash passed; crescent: the foes the wave struck. */
		final List<LivingEntity> marked = new ArrayList<>();
		/** Chain lash: the foe the chain caught. */
		LivingEntity caught;
		/** Crescent: the wave's front, once loosed, and whether something stopped it. */
		Vec3 wave;
		boolean stopped;
		/** Leap slam: whether it has left the ground, and landed. */
		boolean airborne;
		boolean landed;

		Active(ServerPlayer player, ItemStack stack, JugcraftArms.Art art, long start) {
			this.player = player;
			this.stack = stack;
			this.art = art;
			this.start = start;
			this.origin = player.position();
			this.heading = level(player);
		}
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(WeaponArtPayload.TYPE, WeaponArtPayload.CODEC);
		ServerTickEvents.END_SERVER_TICK.register(WeaponArts::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> forget(handler.getPlayer()));
	}

	/** The art of the arm in this stack, or null. */
	public static JugcraftArms.Art art(ItemStack stack) {
		return stack.getItem() instanceof ArmItem arm ? JugcraftArms.ARTS.get(arm.kind()) : null;
	}

	/** Whether this player has an art in progress (a crescent's wave may still be running after they are free). */
	public static boolean active(Player player) {
		return ACTIVE.containsKey(player.getUUID());
	}

	/** Whether this player is busy with an art: in its animation, or in the air on a leap. */
	public static boolean busy(Player player) {
		Active active = ACTIVE.get(player.getUUID());
		if (active == null) {
			return false;
		}
		return active.art.move() == JugcraftArms.Move.LEAP_SLAM ? !active.landed : now - active.start < active.art.ticks();
	}

	/**
	 * Starts the art of the arm in this player's main hand, if they may: not on cooldown, not busy with an art or a
	 * two-handed swing, alive and not gliding; a two-handed arm needs a free off hand; the cyclone, iaido and the leap
	 * need to be out of the saddle, and the leap on the ground. Puts the arm on its cooldown and tells the clients.
	 */
	public static boolean start(ServerPlayer player, ItemStack stack) {
		JugcraftArms.Art art = art(stack);
		if (art == null || stack != player.getMainHandItem() || !player.isAlive() || player.isSpectator() || player.isFallFlying()
				|| busy(player) || TwoHanded.swinging(player) || player.getCooldowns().isOnCooldown(stack)) {
			return false;
		}
		JugcraftArms.Move move = art.move();
		if (TwoHanded.heavy(stack) != null && TwoHanded.offHandBusy(player)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.two_handed.off_hand"));
			return false;
		}
		if (player.isPassenger() && (move == JugcraftArms.Move.CYCLONE || move == JugcraftArms.Move.IAIDO
				|| move == JugcraftArms.Move.LEAP_SLAM)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.arms.art.riding"));
			return false;
		}
		if (move == JugcraftArms.Move.LEAP_SLAM && !grounded(player)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.arms.art.ground"));
			return false;
		}
		Active active = new Active(player, stack, art, now);
		ACTIVE.put(player.getUUID(), active);
		player.getCooldowns().addCooldown(stack, art.cooldown());
		if (art.slow() > 0.0F) {
			player.setSprinting(false);
		}
		slow(player, art.slow());
		ServerLevel level = (ServerLevel) player.level();
		if (move == JugcraftArms.Move.LEAP_SLAM || move == JugcraftArms.Move.IAIDO) {
			// The move's own height is forgiven: only a drop below where it began counts towards a fall.
			player.setIgnoreFallDamageFromCurrentImpulse(true, player.position());
		}
		if (move == JugcraftArms.Move.LEAP_SLAM) {
			push(player, new Vec3(active.heading.x * JugcraftArms.LEAP_FORWARD, JugcraftArms.LEAP_UP, active.heading.z * JugcraftArms.LEAP_FORWARD));
			level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 0.1, player.getZ(), 10, 0.4, 0.05, 0.4, 0.02);
			sound(player, SoundEvents.MACE_SMASH_AIR, 1.0F);
		}
		announce(player, 0);
		return true;
	}

	private static void tick(MinecraftServer server) {
		now = server.getTickCount();
		if (ACTIVE.isEmpty()) {
			return;
		}
		for (Iterator<Active> it = ACTIVE.values().iterator(); it.hasNext();) {
			Active active = it.next();
			ServerPlayer player = active.player;
			boolean held = !player.isRemoved() && player.isAlive() && !player.isSpectator() && player.getMainHandItem() == active.stack;
			long t = now - active.start;
			if (held) {
				step(active, t);
			}
			if (!held || t >= last(active) || active.art.move() == JugcraftArms.Move.LEAP_SLAM && active.landed) {
				it.remove();
				slow(player, 0.0F);
			} else if (t >= active.art.ticks() && active.art.move() != JugcraftArms.Move.LEAP_SLAM) {
				// Free again: only a crescent's wave may still be running.
				slow(player, 0.0F);
			}
		}
	}

	/** The last tick of an art: its animation, or later if its move runs on (iaido's cut, the wave, the reap, the air). */
	private static long last(Active active) {
		int ticks = active.art.ticks();
		return switch (active.art.move()) {
			case CYCLONE -> Math.max(ticks, JugcraftArms.CYCLONE_FIRST + (JugcraftArms.CYCLONE_HITS - 1) * JugcraftArms.CYCLONE_EVERY);
			case IAIDO -> Math.max(ticks, JugcraftArms.IAIDO_START + JugcraftArms.IAIDO_DASH + JugcraftArms.IAIDO_DELAY);
			case LEAP_SLAM -> JugcraftArms.LEAP_MAX_AIR;
			case FLURRY -> Math.max(ticks, JugcraftArms.FLURRY_FIRST + JugcraftArms.FLURRY_JABS * JugcraftArms.FLURRY_EVERY);
			case CRESCENT -> Math.max(ticks, JugcraftArms.CRESCENT_RELEASE + JugcraftArms.CRESCENT_TICKS);
			case CHAIN_LASH -> Math.max(ticks, JugcraftArms.LASH_REAP);
			case SEVEN_CUTS -> Math.max(ticks, JugcraftArms.CUTS_FIRST + (JugcraftArms.CUTS_COUNT - 1) * JugcraftArms.CUTS_EVERY);
		};
	}

	/** One tick of an art, t ticks after it began (1 on the first tick after the use). */
	private static void step(Active active, long t) {
		switch (active.art.move()) {
			case CYCLONE -> {
				long n = t - JugcraftArms.CYCLONE_FIRST;
				if (n >= 0 && n % JugcraftArms.CYCLONE_EVERY == 0 && n / JugcraftArms.CYCLONE_EVERY < JugcraftArms.CYCLONE_HITS) {
					cyclone(active, (int) (n / JugcraftArms.CYCLONE_EVERY));
				}
			}
			case IAIDO -> iaido(active, t);
			case LEAP_SLAM -> {
				// Lands once back on the ground after leaving it (or, held down by a low ceiling, where it stands).
				boolean grounded = grounded(active.player);
				active.airborne |= !grounded;
				if (t >= JugcraftArms.LEAP_MIN_AIR && grounded && (active.airborne || t >= JugcraftArms.LEAP_STUCK)) {
					slam(active);
				}
			}
			case FLURRY -> {
				long n = t - JugcraftArms.FLURRY_FIRST;
				if (n >= 0 && n % JugcraftArms.FLURRY_EVERY == 0 && n / JugcraftArms.FLURRY_EVERY <= JugcraftArms.FLURRY_JABS) {
					jab(active, n / JugcraftArms.FLURRY_EVERY == JugcraftArms.FLURRY_JABS);
				}
			}
			case CRESCENT -> crescent(active, t);
			case CHAIN_LASH -> {
				if (t == JugcraftArms.LASH_THROW) {
					lash(active);
				} else if (t == JugcraftArms.LASH_REAP) {
					reap(active);
				}
			}
			case SEVEN_CUTS -> {
				long n = t - JugcraftArms.CUTS_FIRST;
				if (n >= 0 && n % JugcraftArms.CUTS_EVERY == 0 && n / JugcraftArms.CUTS_EVERY < JugcraftArms.CUTS_COUNT) {
					cut(active, (int) (n / JugcraftArms.CUTS_EVERY));
				}
			}
		}
	}

	// ---------------------------------------------------------------- the moves

	/** One turn of the cyclone: every foe all round within reach, no knockback, drawn in instead. */
	private static void cyclone(Active active, int turn) {
		ServerPlayer player = active.player;
		ServerLevel level = (ServerLevel) player.level();
		for (LivingEntity foe : around(player, player.position(), JugcraftArms.CYCLONE_RADIUS, JugcraftArms.CYCLONE_TARGETS)) {
			if (hit(player, foe, JugcraftArms.CYCLONE_SHARE, false, false)) {
				Vec3 in = new Vec3(player.getX() - foe.getX(), 0.0, player.getZ() - foe.getZ());
				if (in.lengthSqr() > 0.25) {
					Vec3 pull = in.normalize().scale(JugcraftArms.CYCLONE_PULL * resist(foe));
					push(foe, new Vec3(pull.x, foe.getDeltaMovement().y, pull.z));
				}
			}
		}
		for (int i = 0; i < 8; i++) {
			double angle = i * Math.PI / 4.0 + turn * 0.4;
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + Math.cos(angle) * 1.8, player.getY(0.5),
					player.getZ() + Math.sin(angle) * 1.8, 1, 0.0, 0.0, 0.0, 0.0);
		}
		sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F + turn * 0.12F);
	}

	/** Iaido: the dash, marking every foe it passes, then the cut on all of them at once. */
	private static void iaido(Active active, long t) {
		ServerPlayer player = active.player;
		ServerLevel level = (ServerLevel) player.level();
		long dashEnd = JugcraftArms.IAIDO_START + JugcraftArms.IAIDO_DASH;
		if (t >= JugcraftArms.IAIDO_START && t < dashEnd) {
			double speed = JugcraftArms.IAIDO_SPEED;
			push(player, new Vec3(active.heading.x * speed, Math.min(player.getDeltaMovement().y, 0.0), active.heading.z * speed));
			level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2, player.getZ(), 3, 0.2, 0.1, 0.2, 0.01);
			if (t == JugcraftArms.IAIDO_START) {
				sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.6F);
			}
		}
		if (t >= JugcraftArms.IAIDO_START && t <= dashEnd) {
			mark(active);
		}
		if (t == dashEnd) {
			push(player, new Vec3(active.heading.x * 0.15, Math.min(player.getDeltaMovement().y, 0.0), active.heading.z * 0.15));
		}
		if (t == dashEnd + JugcraftArms.IAIDO_DELAY) {
			double reach = JugcraftArms.IAIDO_REACH;
			for (LivingEntity foe : active.marked) {
				if (foe.isAlive() && foe.distanceToSqr(player) <= reach * reach && TwoHanded.allowed(player, level, foe)
						&& hit(player, foe, JugcraftArms.IAIDO_SHARE, true, false)) {
					level.sendParticles(ParticleTypes.SWEEP_ATTACK, foe.getX(), foe.getY(0.5), foe.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
					level.sendParticles(ParticleTypes.CRIT, foe.getX(), foe.getY(0.5), foe.getZ(), 12, 0.3, 0.4, 0.3, 0.3);
				}
			}
			sound(player, SoundEvents.PLAYER_ATTACK_CRIT, 0.7F);
		}
	}

	/** Marks every foe within IAIDO_WIDTH of the dash so far (from where it began to where the wielder is). */
	private static void mark(Active active) {
		ServerPlayer player = active.player;
		ServerLevel level = (ServerLevel) player.level();
		Vec3 from = active.origin;
		Vec3 to = player.position();
		double width = JugcraftArms.IAIDO_WIDTH;
		double low = Math.min(from.y, to.y) - 0.5;
		double high = Math.max(from.y, to.y) + player.getBbHeight() + 0.5;
		AABB swept = new AABB(Math.min(from.x, to.x) - width - 1.0, low, Math.min(from.z, to.z) - width - 1.0,
				Math.max(from.x, to.x) + width + 1.0, high, Math.max(from.z, to.z) + width + 1.0);
		for (LivingEntity foe : level.getEntitiesOfClass(LivingEntity.class, swept)) {
			if (active.marked.size() >= JugcraftArms.IAIDO_TARGETS) {
				return;
			}
			if (active.marked.contains(foe) || !TwoHanded.target(player, foe) || foe.getBoundingBox().maxY < low
					|| foe.getBoundingBox().minY > high || flatDistance(foe.position(), from, to) > width + foe.getBbWidth() / 2.0) {
				continue;
			}
			active.marked.add(foe);
		}
	}

	/** The leap's landing: every foe about, harder at the centre and the further below the take-off, thrown up. */
	private static void slam(Active active) {
		ServerPlayer player = active.player;
		ServerLevel level = (ServerLevel) player.level();
		active.landed = true;
		Vec3 at = player.position();
		double drop = Math.min(JugcraftArms.LEAP_DROP_MAX, Math.max(0.0, active.origin.y - at.y));
		double radius = JugcraftArms.LEAP_RADIUS;
		for (LivingEntity foe : around(player, at, radius, JugcraftArms.LEAP_TARGETS)) {
			double distance = Math.sqrt(Math.pow(foe.getX() - at.x, 2) + Math.pow(foe.getZ() - at.z, 2));
			double falloff = 1.0 - (1.0 - JugcraftArms.LEAP_EDGE) * Math.min(1.0, distance / radius);
			float share = (float) (JugcraftArms.LEAP_SHARE * falloff + JugcraftArms.LEAP_PER_BLOCK * drop);
			if (hit(player, foe, share, false, false)) {
				Vec3 out = new Vec3(foe.getX() - at.x, 0.0, foe.getZ() - at.z);
				out = out.lengthSqr() > 1.0E-4 ? out.normalize().scale(0.4) : Vec3.ZERO;
				double resist = resist(foe);
				push(foe, new Vec3(out.x * resist, JugcraftArms.LEAP_LIFT * resist, out.z * resist));
			}
		}
		level.sendParticles(ParticleTypes.POOF, at.x, at.y + 0.1, at.z, 30, radius * 0.4, 0.05, radius * 0.4, 0.03);
		for (int i = 0; i < 12; i++) {
			double angle = i * Math.PI / 6.0;
			level.sendParticles(ParticleTypes.CLOUD, at.x + Math.cos(angle) * radius * 0.8, at.y + 0.1, at.z + Math.sin(angle) * radius * 0.8,
					2, 0.1, 0.05, 0.1, 0.02);
		}
		sound(player, SoundEvents.MACE_SMASH_GROUND_HEAVY, 0.9F);
		announce(player, 1);
	}

	/** One jab of the flurry (or its finish) at the nearest foe within the arm's reach ahead. */
	private static void jab(Active active, boolean finish) {
		ServerPlayer player = active.player;
		ServerLevel level = (ServerLevel) player.level();
		List<LivingEntity> foes = TwoHanded.foes(player, active.stack, new JugcraftArms.Heavy(0, JugcraftArms.FLURRY_ARC, 1, 1));
		if (!foes.isEmpty()) {
			LivingEntity foe = foes.getFirst();
			// A jab holds the foe where it is (vanilla's hit would knock it back out of reach); only the finish drives it off.
			Vec3 motion = foe.getDeltaMovement();
			if (hit(player, foe, finish ? JugcraftArms.FLURRY_FINISH : JugcraftArms.FLURRY_SHARE, finish, false)) {
				if (!finish) {
					push(foe, motion);
				}
				level.sendParticles(ParticleTypes.CRIT, foe.getX(), foe.getY(0.6), foe.getZ(), finish ? 14 : 5, 0.2, 0.3, 0.2, 0.2);
			}
		}
		sound(player, finish ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_WEAK, finish ? 0.9F : 1.4F);
	}

	/** The crescent: loosed at CRESCENT_RELEASE, then each tick it runs on, striking the foes it passes, until stopped. */
	private static void crescent(Active active, long t) {
		ServerPlayer player = active.player;
		ServerLevel level = (ServerLevel) player.level();
		if (t == JugcraftArms.CRESCENT_RELEASE) {
			active.wave = new Vec3(player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ());
			sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.6F);
		}
		if (active.wave == null || active.stopped || t >= JugcraftArms.CRESCENT_RELEASE + JugcraftArms.CRESCENT_TICKS) {
			return;
		}
		Vec3 from = active.wave;
		Vec3 to = from.add(active.heading.scale(JugcraftArms.CRESCENT_SPEED));
		BlockHitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (block.getType() != HitResult.Type.MISS) {
			to = block.getLocation();
			active.stopped = true;
		}
		double width = JugcraftArms.CRESCENT_WIDTH;
		AABB swept = new AABB(from, to).inflate(width + 1.0, 1.5, width + 1.0);
		List<LivingEntity> passed = new ArrayList<>();
		for (LivingEntity foe : level.getEntitiesOfClass(LivingEntity.class, swept)) {
			if (!active.marked.contains(foe) && TwoHanded.target(player, foe) && foe.getBoundingBox().maxY >= from.y - 1.5
					&& foe.getBoundingBox().minY <= from.y + 1.5 && flatDistance(foe.position(), from, to) <= width + foe.getBbWidth() / 2.0) {
				passed.add(foe);
			}
		}
		passed.sort(Comparator.comparingDouble(foe -> foe.distanceToSqr(from)));
		for (LivingEntity foe : passed) {
			if (active.marked.size() >= JugcraftArms.CRESCENT_TARGETS) {
				break;
			}
			if (!TwoHanded.allowed(player, level, foe)) {
				continue;
			}
			float share = JugcraftArms.CRESCENT_SHARE * Math.max(0.0F, 1.0F - JugcraftArms.CRESCENT_FADE * active.marked.size());
			active.marked.add(foe);
			hit(player, foe, share, true, false);
		}
		// The crescent: its tips trail behind its middle.
		Vec3 across = new Vec3(-active.heading.z, 0.0, active.heading.x);
		for (int i = -3; i <= 3; i++) {
			double side = i * width / 3.0;
			Vec3 point = to.add(across.scale(side)).subtract(active.heading.scale(0.08 * i * i));
			level.sendParticles(ParticleTypes.END_ROD, point.x, point.y + 0.25 * Math.abs(i) / 3.0, point.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, to.x, to.y, to.z, 1, 0.0, 0.0, 0.0, 0.0);
		active.wave = to;
	}

	/** The chain: the first foe along the view, up to LASH_RANGE and short of any block, struck and hauled in. */
	private static void lash(Active active) {
		ServerPlayer player = active.player;
		ServerLevel level = (ServerLevel) player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getViewVector(1.0F).scale(JugcraftArms.LASH_RANGE));
		BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (block.getType() != HitResult.Type.MISS) {
			end = block.getLocation();
		}
		LivingEntity first = null;
		double nearest = Double.MAX_VALUE;
		for (LivingEntity foe : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(1.0))) {
			if (!TwoHanded.target(player, foe)) {
				continue;
			}
			double along = enters(foe.getBoundingBox().inflate(0.3), eye, end);
			if (along >= 0.0 && along < nearest) {
				nearest = along;
				first = foe;
			}
		}
		Vec3 reached = first == null ? end : first.getBoundingBox().getCenter();
		double length = eye.distanceTo(reached);
		for (double d = 0.8; d < length; d += 0.5) {
			Vec3 point = eye.add(reached.subtract(eye).scale(d / length));
			level.sendParticles(ParticleTypes.CRIT, point.x, point.y - 0.2, point.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
		sound(player, SoundEvents.FISHING_BOBBER_THROW, 0.7F);
		if (first == null || !TwoHanded.allowed(player, level, first) || !hit(player, first, JugcraftArms.LASH_SHARE, false, true)) {
			return;
		}
		active.caught = first;
		Vec3 back = new Vec3(player.getX() - first.getX(), 0.0, player.getZ() - first.getZ());
		double distance = back.length();
		double resist = resist(first);
		double speed = Math.min(JugcraftArms.LASH_PULL_MAX, JugcraftArms.LASH_PULL * Math.max(0.0, distance - 1.5)) * resist;
		Vec3 pull = distance > 1.0E-4 ? back.scale(speed / distance) : Vec3.ZERO;
		push(first, new Vec3(pull.x, 0.3 * resist, pull.z));
		if (first.isPassenger() && !first.is(EntityTypeTags.CANNOT_BE_DISMOUNTED_BY_ITEM_USAGE)) {
			first.stopRiding();
		}
		sound(player, SoundEvents.CHAIN_HIT, 0.8F);
	}

	/** The sickle reaps what the chain caught, if it is now within LASH_REAP_REACH and in sight. */
	private static void reap(Active active) {
		ServerPlayer player = active.player;
		ServerLevel level = (ServerLevel) player.level();
		LivingEntity foe = active.caught;
		if (foe == null || !foe.isAlive() || foe.distanceTo(player) > JugcraftArms.LASH_REAP_REACH || !player.hasLineOfSight(foe)
				|| !TwoHanded.allowed(player, level, foe)) {
			return;
		}
		if (hit(player, foe, JugcraftArms.LASH_REAP_SHARE, true, false)) {
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, foe.getX(), foe.getY(0.5), foe.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
			sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F);
		}
	}

	/**
	 * One of the seven cuts: every foe within the katana's reach and CUTS_ARC ahead, held where it is but by the last cut,
	 * and an arc of the metal's colour drawn across the air where the blade went (alternately falling left and right, the
	 * last level and wide).
	 */
	private static void cut(Active active, int index) {
		ServerPlayer player = active.player;
		ServerLevel level = (ServerLevel) player.level();
		boolean last = index == JugcraftArms.CUTS_COUNT - 1;
		for (LivingEntity foe : TwoHanded.foes(player, active.stack, new JugcraftArms.Heavy(0, JugcraftArms.CUTS_ARC,
				JugcraftArms.CUTS_TARGETS, 1))) {
			Vec3 motion = foe.getDeltaMovement();
			if (hit(player, foe, JugcraftArms.CUTS_SHARE, last, false) && !last) {
				push(foe, motion);
			}
		}
		boolean steel = BuiltInRegistries.ITEM.getKey(active.stack.getItem()).getPath().startsWith("steel_");
		DustParticleOptions dust = new DustParticleOptions(steel ? CUT_STEEL : CUT_BRONZE, last ? 1.6F : 1.2F);
		Vec3 ahead = active.heading;
		Vec3 right = new Vec3(-ahead.z, 0.0, ahead.x);
		double tilt = last ? 0.0 : (index % 2 == 0 ? 0.7 : -0.7);
		double radius = last ? 2.6 : 2.0;
		double reach = last ? 1.2 : 0.95;
		Vec3 centre = player.position().add(0.0, player.getBbHeight() * 0.65, 0.0);
		for (int i = 0; i <= 12; i++) {
			double angle = (i / 12.0 - 0.5) * Math.PI * reach;
			Vec3 point = centre.add(ahead.scale(radius * Math.cos(angle) * 0.8)).add(right.scale(radius * Math.sin(angle)))
					.add(0.0, tilt * Math.sin(angle), 0.0);
			level.sendParticles(dust, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
		sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.25F + index * 0.06F);
	}

	// ---------------------------------------------------------------- shared

	/**
	 * Strikes one foe for the arm's attack damage times share, at a full charge, through Player.stabAttack; the foe's
	 * damage cooldown is let go first, so a quick run of an art's hits all land. Returns whether it was hurt.
	 */
	static boolean hit(ServerPlayer player, LivingEntity foe, float share, boolean knockback, boolean dismount) {
		float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		foe.damageCooldownTime = 0;
		AttackStrengthAccessor strength = (AttackStrengthAccessor) player;
		int ticker = strength.jugcraft$attackStrengthTicker();
		strength.jugcraft$setAttackStrengthTicker(FULL_CHARGE);
		try {
			return player.stabAttack(EquipmentSlot.MAINHAND, foe, base * share, true, knockback, dismount);
		} finally {
			strength.jugcraft$setAttackStrengthTicker(ticker);
		}
	}

	/**
	 * The foes about a point a blow from this player may strike: within radius across (and the foe's half width), from
	 * a block below the point to 2.5 above it, in the wielder's sight and not protected; nearest first, at most targets.
	 */
	static List<LivingEntity> around(ServerPlayer player, Vec3 centre, double radius, int targets) {
		ServerLevel level = (ServerLevel) player.level();
		List<LivingEntity> found = new ArrayList<>();
		AABB box = new AABB(centre.x - radius - 1.0, centre.y - 1.0, centre.z - radius - 1.0, centre.x + radius + 1.0, centre.y + 2.5,
				centre.z + radius + 1.0);
		for (LivingEntity foe : level.getEntitiesOfClass(LivingEntity.class, box)) {
			double dx = foe.getX() - centre.x;
			double dz = foe.getZ() - centre.z;
			double reach = radius + foe.getBbWidth() / 2.0;
			if (dx * dx + dz * dz > reach * reach || !TwoHanded.target(player, foe) || !player.hasLineOfSight(foe)
					|| !TwoHanded.allowed(player, level, foe)) {
				continue;
			}
			found.add(foe);
		}
		found.sort(Comparator.comparingDouble(foe -> foe.distanceToSqr(centre)));
		return found.size() > targets ? new ArrayList<>(found.subList(0, targets)) : found;
	}

	/** How far a point is, across, from the segment from-to (the segment's ends count as its own). */
	static double flatDistance(Vec3 point, Vec3 from, Vec3 to) {
		double sx = to.x - from.x;
		double sz = to.z - from.z;
		double length = sx * sx + sz * sz;
		double u = length < 1.0E-8 ? 0.0 : Math.max(0.0, Math.min(1.0, ((point.x - from.x) * sx + (point.z - from.z) * sz) / length));
		double dx = point.x - (from.x + sx * u);
		double dz = point.z - (from.z + sz * u);
		return Math.sqrt(dx * dx + dz * dz);
	}

	/** Where a segment from-to first enters a box, as a share of the way along it; -1 if it misses (the slab test). */
	static double enters(AABB box, Vec3 from, Vec3 to) {
		double low = 0.0;
		double high = 1.0;
		double[] start = {from.x, from.y, from.z};
		double[] delta = {to.x - from.x, to.y - from.y, to.z - from.z};
		double[] min = {box.minX, box.minY, box.minZ};
		double[] max = {box.maxX, box.maxY, box.maxZ};
		for (int axis = 0; axis < 3; axis++) {
			if (Math.abs(delta[axis]) < 1.0E-9) {
				if (start[axis] < min[axis] || start[axis] > max[axis]) {
					return -1.0;
				}
				continue;
			}
			double a = (min[axis] - start[axis]) / delta[axis];
			double b = (max[axis] - start[axis]) / delta[axis];
			low = Math.max(low, Math.min(a, b));
			high = Math.min(high, Math.max(a, b));
			if (low > high) {
				return -1.0;
			}
		}
		return low;
	}

	/** Whether the player stands on something: on the ground as the server knows it, or a block just under their feet. */
	static boolean grounded(ServerPlayer player) {
		return player.onGround() || !player.level().noCollision(player, player.getBoundingBox().move(0.0, -0.06, 0.0));
	}

	/** The player's view, level and of length one (straight up or down: the way their body faces). */
	private static Vec3 level(ServerPlayer player) {
		Vec3 look = player.getViewVector(1.0F);
		Vec3 flat = new Vec3(look.x, 0.0, look.z);
		if (flat.lengthSqr() < 1.0E-6) {
			double yaw = Math.toRadians(player.getYRot());
			return new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
		}
		return flat.normalize();
	}

	/** One less the foe's knockback resistance: how much of a pull or a throw it takes. */
	private static double resist(LivingEntity foe) {
		return Math.max(0.0, 1.0 - foe.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
	}

	/** Sets an entity's motion; a player's client is told, since a player moves themselves. */
	private static void push(Entity entity, Vec3 motion) {
		entity.setDeltaMovement(motion);
		if (entity instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
	}

	private static void sound(ServerPlayer player, SoundEvent sound, float pitch) {
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, pitch);
	}

	/** Tells every client that sees the wielder, and the wielder's own, to play this phase of the art. */
	private static void announce(ServerPlayer player, int phase) {
		WeaponArtPayload payload = new WeaponArtPayload(player.getId(), phase);
		for (ServerPlayer watcher : PlayerLookup.tracking(player)) {
			if (ServerPlayNetworking.canSend(watcher, WeaponArtPayload.TYPE)) {
				ServerPlayNetworking.send(watcher, payload);
			}
		}
		if (ServerPlayNetworking.canSend(player, WeaponArtPayload.TYPE)) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	private static void slow(ServerPlayer player, float share) {
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) {
			return;
		}
		speed.removeModifier(SLOW);
		if (share > 0.0F) {
			speed.addTransientModifier(new AttributeModifier(SLOW, -share, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}

	private static void forget(ServerPlayer player) {
		if (ACTIVE.remove(player.getUUID()) != null) {
			slow(player, 0.0F);
		}
	}
}
