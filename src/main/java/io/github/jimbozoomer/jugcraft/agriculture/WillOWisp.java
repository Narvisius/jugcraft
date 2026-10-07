package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A will-o'-wisp: a small floating light that drifts a little above the ground on Halloween nights and darts away
 * from anyone who comes near ({@link #FLEE_RADIUS}; a sneaking player gets to {@link #SNEAK_FLEE_RADIUS}). Use a
 * glass bottle on it to catch it: the bottle becomes a Wisp in a Jar, a lantern that keeps its light for good.
 * It fades away at dawn, when the event ends, or when struck. It drops nothing and harms nothing; see
 * {@link Wisps} for where and when it appears.
 */
public class WillOWisp extends AmbientCreature {
	public static final double FLEE_RADIUS = 6.0;
	public static final double SNEAK_FLEE_RADIUS = 2.5;
	/** Top speed in blocks a tick, drifting and fleeing. */
	public static final double DRIFT_SPEED = 0.08;
	public static final double FLEE_SPEED = 0.35;

	private @Nullable Vec3 target;

	public WillOWisp(EntityType<? extends WillOWisp> type, Level level) {
		super(type, level);
		setNoGravity(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 1.0);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		step(level, tickCount % 20 != 0 || Wisps.night(level));
	}

	/**
	 * One tick of steering: out of the {@code night} it fades; otherwise it darts away from a player inside its flee
	 * radius (a sneaking one only from close by) or drifts. Returns whether it is fleeing.
	 */
	public boolean step(ServerLevel level, boolean night) {
		if (!night) {
			fade(level);
			return false;
		}
		boolean fleeing = false;
		Player near = level.getNearestPlayer(this, FLEE_RADIUS);
		double flee = near == null ? 0 : near.isShiftKeyDown() ? SNEAK_FLEE_RADIUS : FLEE_RADIUS;
		Vec3 here = position();
		double speed = DRIFT_SPEED;
		if (near != null && !near.isSpectator() && near.distanceTo(this) < flee) {
			// Dart away from the player, a little upward.
			Vec3 away = here.subtract(near.position()).multiply(1.0, 0.0, 1.0);
			away = away.lengthSqr() < 1.0E-4 ? new Vec3(random.nextDouble() - 0.5, 0.0, random.nextDouble() - 0.5) : away;
			target = here.add(away.normalize().scale(8.0)).add(0.0, 1.0, 0.0);
			speed = FLEE_SPEED;
			fleeing = true;
		} else if (target == null || target.distanceToSqr(here) < 1.0 || random.nextInt(80) == 0) {
			// Drift to a new spot one to three blocks above the ground nearby.
			double x = getX() + random.nextInt(9) - 4;
			double z = getZ() + random.nextInt(9) - 4;
			int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
			target = new Vec3(x, ground + 1.0 + random.nextDouble() * 2.0, z);
		}
		Vec3 wanted = target.subtract(here);
		wanted = wanted.lengthSqr() > speed * speed ? wanted.normalize().scale(speed) : wanted;
		setDeltaMovement(getDeltaMovement().add(wanted.subtract(getDeltaMovement()).scale(0.2)));
		return fleeing;
	}

	/** Vanishes in a puff of light (server side). */
	public void fade(ServerLevel level) {
		level.sendParticles(ParticleTypes.GLOW, getX(), getY() + 0.25, getZ(), 12, 0.2, 0.2, 0.2, 0.02);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.3F, 1.8F);
		discard();
	}

	/** A glass bottle catches it: the bottle becomes a Wisp in a Jar. */
	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (!held.is(Items.GLASS_BOTTLE)) {
			return super.mobInteract(player, hand);
		}
		if (level() instanceof ServerLevel level && player instanceof ServerPlayer catcher) {
			ItemStack jar = new ItemStack(JugcraftAgriculture.item("wisp_in_a_jar"));
			player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, jar));
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.4F);
			level.sendParticles(ParticleTypes.GLOW, getX(), getY() + 0.25, getZ(), 8, 0.15, 0.15, 0.15, 0.01);
			TrickOrTreat.award(catcher, "wisp_in_a_jar");
			discard();
		}
		return InteractionResult.SUCCESS;
	}

	/** Any blow puts it out; it has no loot and gives no experience. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (isInvulnerableTo(level, source)) {
			return false;
		}
		fade(level);
		return true;
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide() && random.nextInt(4) == 0) {
			level().addParticle(ParticleTypes.GLOW, getRandomX(0.4), getY() + 0.25 + random.nextDouble() * 0.2, getRandomZ(0.4), 0.0, 0.0, 0.0);
		}
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(net.minecraft.world.entity.Entity entity) {
	}

	@Override
	public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	protected void checkFallDamage(double distance, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
	}

	@Override
	public boolean isIgnoringBlockTriggers() {
		return true;
	}

	@Override
	public boolean shouldDropExperience() {
		return false;
	}
}
