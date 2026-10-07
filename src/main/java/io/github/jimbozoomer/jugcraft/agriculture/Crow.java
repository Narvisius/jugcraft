package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A crow: a black bird that comes to fields by day in small flocks ({@link Crows}). It wheels a few blocks above the
 * ground, cawing, and now and then spots a ripe crop open to the sky within {@value #RAID_RADIUS} blocks, drops onto it
 * and pecks at it for {@value #PECK_TICKS} ticks, setting it back {@value #SETBACK} growth stages, then rests
 * ({@value #RAID_COOLDOWN} ticks or more) before it raids again. It won't raid a crop a scarecrow guards ({@link Scarecrows}), and takes flight if
 * one comes to guard the crop it is after (it looks every {@value #LOOK_TICKS} ticks). It flies off from a player who
 * comes within {@value #FLEE_RADIUS} blocks (a sneaking one gets to {@value #SNEAK_FLEE_RADIUS}), or who hits it, and the
 * crop is spared. Crows only peck crops while the {@code mob_griefing} game rule is on. They fly off at nightfall, and
 * when the agriculture feature is switched off. They drop feathers.
 */
public class Crow extends AmbientCreature {
	public static final double FLEE_RADIUS = 6.0;
	public static final double SNEAK_FLEE_RADIUS = 2.5;
	public static final int RAID_RADIUS = 12;
	public static final int SEARCH_TRIES = 24;
	public static final int PECK_TICKS = 40;
	public static final int SETBACK = 3;
	/** Ticks a crow rests after pecking before it may raid again: {@value #RAID_COOLDOWN} and up to {@value #RAID_COOLDOWN_SPREAD} more. */
	public static final int RAID_COOLDOWN = 600;
	public static final int RAID_COOLDOWN_SPREAD = 600;
	public static final int FLEE_TICKS = 80;
	/** How often (ticks) a crow after a crop looks round for a scarecrow that has come to guard it. */
	public static final int LOOK_TICKS = 10;
	/** At nightfall a crow climbs away and is gone once {@value #LEAVE_HEIGHT} blocks over the ground, or after {@value #LEAVE_TICKS} ticks. */
	public static final int LEAVE_HEIGHT = 24;
	public static final int LEAVE_TICKS = 200;
	public static final double FLY_SPEED = 0.18;
	public static final double FLEE_SPEED = 0.4;
	private static final EntityDataAccessor<Boolean> PECKING = SynchedEntityData.defineId(Crow.class, EntityDataSerializers.BOOLEAN);

	private @Nullable BlockPos crop;
	private @Nullable Vec3 target;
	private int peck;
	private int cooldown;
	private int fleeing;
	private int leaving;
	private int look;

	public Crow(EntityType<? extends Crow> type, Level level) {
		super(type, level);
		setNoGravity(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 4.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(PECKING, false);
	}

	/** Whether it is down on a crop, pecking (sent to clients to bob its head). */
	public boolean pecking() {
		return entityData.get(PECKING);
	}

	public @Nullable BlockPos crop() {
		return crop;
	}

	public boolean fleeing() {
		return fleeing > 0;
	}

	/** Whether {@code state} is a crop a crow goes for: a ripe single-block crop (vanilla's and Agriculture's). */
	public static boolean tempting(BlockState state) {
		return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
	}

	/** Whether crows may peck crops in {@code level}: the {@code mob_griefing} game rule. */
	public static boolean mayPeck(ServerLevel level) {
		return level.getGameRules().get(GameRules.MOB_GRIEFING);
	}

	/** Sends it after the crop at {@code pos}, if it will go: ripe, unguarded, pecking allowed. */
	public boolean raid(ServerLevel level, BlockPos pos) {
		if (!mayPeck(level) || !tempting(level.getBlockState(pos)) || Scarecrows.guarded(level, pos)) {
			return false;
		}
		crop = pos.immutable();
		return true;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		// With the feature switched off, crows leave as they do at nightfall.
		step(level, Crows.day(level) && JugcraftConfig.isFeatureEnabled(JugcraftAgriculture.FEATURE));
	}

	/** One tick of the crow's life on the server, by day or (it leaves) by night. */
	public void step(ServerLevel level, boolean day) {
		if (!day) {
			// Nightfall: off it goes, up and away.
			stopPecking();
			setDeltaMovement(getDeltaMovement().add(0.0, 0.03, 0.0));
			if (getY() > level.getHeight(Heightmap.Types.MOTION_BLOCKING, getBlockX(), getBlockZ()) + LEAVE_HEIGHT || ++leaving > LEAVE_TICKS) {
				discard();
			}
			return;
		}
		leaving = 0;
		Player near = level.getNearestPlayer(this, FLEE_RADIUS);
		if (near != null && !near.isSpectator() && near.distanceTo(this) < (near.isShiftKeyDown() ? SNEAK_FLEE_RADIUS : FLEE_RADIUS)) {
			flee(level, near.position());
		} else if (crop != null && ++look >= LOOK_TICKS) {
			look = 0;
			if (Scarecrows.guarded(level, crop)) {
				flee(level, Vec3.atCenterOf(crop));
			}
		}
		if (fleeing > 0) {
			fleeing--;
			steer(target == null ? position().add(0.0, 1.0, 0.0) : target, FLEE_SPEED);
			return;
		}
		if (crop != null) {
			BlockState state = level.getBlockState(crop);
			if (!tempting(state) || !mayPeck(level)) {
				stopPecking();
				return;
			}
			Vec3 on = Vec3.atBottomCenterOf(crop).add(0.0, 0.1, 0.0);
			if (pecking()) {
				setDeltaMovement(Vec3.ZERO);
				if (--peck % 10 == 0) {
					level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), on.x, on.y + 0.2, on.z, 4, 0.15, 0.1, 0.15, 0.02);
				}
				if (peck <= 0) {
					eat(level, state);
				}
			} else if (position().distanceToSqr(on) < 0.25) {
				peck = PECK_TICKS;
				entityData.set(PECKING, true);
				setDeltaMovement(Vec3.ZERO);
			} else {
				steer(on, FLY_SPEED);
			}
			return;
		}
		if (cooldown > 0) {
			cooldown--;
		} else if (tickCount % 20 == 0 && mayPeck(level)) {
			BlockPos found = search(level);
			if (found != null) {
				crop = found;
				caw(level);
				return;
			}
		}
		wander(level);
	}

	/** Looks for a ripe, unguarded crop within reach: a few random spots on the ground, never loading a chunk. */
	private @Nullable BlockPos search(ServerLevel level) {
		for (int i = 0; i < SEARCH_TRIES; i++) {
			int x = getBlockX() + random.nextIntBetweenInclusive(-RAID_RADIUS, RAID_RADIUS);
			int z = getBlockZ() + random.nextIntBetweenInclusive(-RAID_RADIUS, RAID_RADIUS);
			if (!level.isLoaded(new BlockPos(x, getBlockY(), z))) {
				continue;
			}
			// Crops don't block motion: the heightmap stands on the farmland, where the crop is.
			BlockPos spot = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
			if (tempting(level.getBlockState(spot)) && !Scarecrows.guarded(level, spot)) {
				return spot;
			}
		}
		return null;
	}

	/** Drifts a few blocks above the ground nearby, now and then cawing. */
	private void wander(ServerLevel level) {
		Vec3 here = position();
		if (target == null || target.distanceToSqr(here) < 1.0 || random.nextInt(100) == 0) {
			double x = getX() + random.nextInt(17) - 8;
			double z = getZ() + random.nextInt(17) - 8;
			int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
			target = new Vec3(x, ground + 3.0 + random.nextDouble() * 4.0, z);
		}
		if (random.nextInt(300) == 0) {
			caw(level);
		}
		steer(target, FLY_SPEED * 0.7);
	}

	/** Takes flight away from {@code from}, up and out, and gives up its crop. */
	public void flee(ServerLevel level, Vec3 from) {
		if (fleeing <= 0) {
			caw(level);
		}
		Vec3 away = position().subtract(from).multiply(1.0, 0.0, 1.0);
		away = away.lengthSqr() < 1.0E-4 ? new Vec3(random.nextDouble() - 0.5, 0.0, random.nextDouble() - 0.5) : away;
		target = position().add(away.normalize().scale(12.0)).add(0.0, 5.0, 0.0);
		fleeing = FLEE_TICKS;
		stopPecking();
	}

	/** Pecks the crop back {@value #SETBACK} growth stages, and rests a while before the next raid. */
	private void eat(ServerLevel level, BlockState state) {
		if (state.getBlock() instanceof CropBlock block && crop != null) {
			BlockState after = block.getStateForAge(Math.max(0, block.getMaxAge() - SETBACK));
			level.setBlock(crop, after, Block.UPDATE_ALL);
			level.gameEvent(this, GameEvent.BLOCK_CHANGE, crop);
			level.playSound(null, crop, SoundEvents.CROP_BREAK, SoundSource.NEUTRAL, 0.6F, 1.2F);
		}
		cooldown = RAID_COOLDOWN + random.nextInt(RAID_COOLDOWN_SPREAD + 1);
		caw(level);
		stopPecking();
	}

	private void stopPecking() {
		crop = null;
		peck = 0;
		entityData.set(PECKING, false);
	}

	private void caw(ServerLevel level) {
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.PARROT_AMBIENT, SoundSource.NEUTRAL, 0.8F, 0.5F + random.nextFloat() * 0.1F);
	}

	private void steer(Vec3 to, double speed) {
		Vec3 wanted = to.subtract(position());
		wanted = wanted.lengthSqr() > speed * speed ? wanted.normalize().scale(speed) : wanted;
		setDeltaMovement(getDeltaMovement().add(wanted.subtract(getDeltaMovement()).scale(0.25)));
		if (wanted.horizontalDistanceSqr() > 1.0E-4) {
			setYRot((float) (Math.atan2(wanted.z, wanted.x) * 180.0 / Math.PI) - 90.0F);
			yBodyRot = getYRot();
			yHeadRot = getYRot();
		}
	}

	/** Hit, it takes flight from whatever hit it. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt && isAlive()) {
			flee(level, source.getSourcePosition() == null ? position() : source.getSourcePosition());
		}
		return hurt;
	}

	@Override
	public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	protected void checkFallDamage(double distance, boolean onGround, BlockState state, BlockPos pos) {
	}

	@Override
	public boolean isIgnoringBlockTriggers() {
		return true;
	}
}
