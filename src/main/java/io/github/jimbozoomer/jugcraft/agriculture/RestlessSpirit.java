package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A restless spirit: a pale, hooded shape risen from a grave at night ({@link Spirits}) that drifts about it, never more
 * than {@value #HAUNT_RADIUS} blocks across the ground from it. No one sees it until it is revealed: by a player holding a
 * Spirit Lantern within {@value Spirits#REVEAL_RADIUS} blocks, or by glowing (a Revealing candle's aura gives it the
 * Glowing effect). Revealed, it shows to everyone near for {@value #REVEAL_TICKS} ticks at a time, moans now and then,
 * and shies away from anyone within {@value #SHY_RADIUS} blocks (a sneaking player gets to {@value #SNEAK_SHY_RADIUS}),
 * though never out of its haunt, so it can be cornered. A glass bottle used on a revealed spirit catches it: the bottle
 * fills with Ectoplasm. A Spirit Board's séance ({@link SpiritBoard}) learns its name and the one thing it wishes for;
 * given that, revealed, it is laid to rest. A complete ofrenda welcomes it ({@link #welcome}): it comes to the ofrenda and
 * stays there, shown and calm, until dawn. Blows pass through it; it fades at dawn or when the agriculture feature is off.
 * It harms nothing and drops nothing.
 */
public class RestlessSpirit extends AmbientCreature {
	public static final int HAUNT_RADIUS = 6;
	/** How high above its grave it drifts, in blocks, at most. */
	public static final double HAUNT_HEIGHT = 3.0;
	public static final double SHY_RADIUS = 3.0;
	public static final double SNEAK_SHY_RADIUS = 1.5;
	/** Top speed in blocks a tick, drifting and shying away. */
	public static final double DRIFT_SPEED = 0.04;
	public static final double SHY_SPEED = 0.12;
	/** How long one look by a lantern (or one glow) reveals it for, and how often it looks. */
	public static final int REVEAL_TICKS = 40;
	public static final int LOOK_TICKS = 10;
	/** How long it takes to show fully once revealed, and to fade once it isn't. */
	public static final int FADE_TICKS = 10;
	private static final EntityDataAccessor<Long> REVEALED_SINCE = SynchedEntityData.defineId(RestlessSpirit.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Long> REVEALED_UNTIL = SynchedEntityData.defineId(RestlessSpirit.class, EntityDataSerializers.LONG);

	private @Nullable BlockPos home;
	private @Nullable Vec3 target;
	/** Its name ({@link SpiritBoard#NAMES}) and wish, once a séance has asked it; -1 until then. */
	private int spiritName = -1;
	private int wish = -1;
	/** Whether an ofrenda has welcomed it (its home is then the ofrenda). */
	private boolean welcomed;

	public RestlessSpirit(EntityType<? extends RestlessSpirit> type, Level level) {
		super(type, level);
		setNoGravity(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 4.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(REVEALED_SINCE, 0L);
		builder.define(REVEALED_UNTIL, 0L);
	}

	/** The grave it haunts (where it rose, or where it was first seen). */
	public BlockPos home() {
		if (home == null) {
			home = blockPosition();
		}
		return home;
	}

	public void setHome(BlockPos grave) {
		home = grave.immutable();
	}

	/** Its name, as an index into {@link SpiritBoard#NAMES}, or -1 if no séance has asked it yet. */
	public int spiritName() {
		return spiritName;
	}

	/** What it wishes for, or null if no séance has asked it yet. */
	public SpiritBoard.@Nullable Wish wish() {
		return wish >= 0 ? SpiritBoard.Wish.values()[wish] : null;
	}

	/** Gives it a name and a wish, if it has none yet (when a séance first asks it). */
	public void promise(RandomSource random) {
		if (spiritName < 0 || wish < 0) {
			spiritName = random.nextInt(SpiritBoard.NAMES.length);
			wish = random.nextInt(SpiritBoard.Wish.values().length);
		}
	}

	/** Sets its name and wish (tests). */
	public void promise(int name, SpiritBoard.Wish wished) {
		spiritName = Math.clamp(name, 0, SpiritBoard.NAMES.length - 1);
		wish = wished.ordinal();
	}

	/** Whether an ofrenda has welcomed it. */
	public boolean welcomed() {
		return welcomed;
	}

	/**
	 * An ofrenda at {@code ofrenda} welcomes it: it makes the ofrenda its home (so it drifts there and stays near it),
	 * shows itself, and no longer shies from anyone.
	 */
	public void welcome(ServerLevel level, BlockPos ofrenda) {
		if (!welcomed || !ofrenda.equals(home)) {
			welcomed = true;
			setHome(ofrenda);
			target = null;
		}
		reveal(level.getGameTime() + REVEAL_TICKS * 2L);
	}

	/** Whether it shows now. */
	public boolean revealed() {
		return entityData.get(REVEALED_UNTIL) > level().getGameTime();
	}

	/**
	 * How clearly it shows at {@code gameTime} plus {@code partialTick}: 0 hidden to 1, fading in over {@value #FADE_TICKS}
	 * ticks once revealed and out over the last {@value #FADE_TICKS}.
	 */
	public float visibility(long gameTime, float partialTick) {
		double in = (gameTime - entityData.get(REVEALED_SINCE) + partialTick) / FADE_TICKS;
		double out = (entityData.get(REVEALED_UNTIL) - gameTime - partialTick) / FADE_TICKS;
		return (float) Mth.clamp(Math.min(in, out), 0.0, 1.0);
	}

	/** Shows it until {@code until} (game time), with a shimmer if it was hidden. */
	public void reveal(long until) {
		long now = level().getGameTime();
		if (!revealed()) {
			entityData.set(REVEALED_SINCE, now);
			level().playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 0.6F, 0.6F);
		}
		if (until > entityData.get(REVEALED_UNTIL)) {
			entityData.set(REVEALED_UNTIL, until);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		step(level, tickCount % 20 != 0 || MourningAngelBlock.night(level) && JugcraftConfig.isFeatureEnabled(JugcraftAgriculture.FEATURE));
	}

	/**
	 * One tick: out of the {@code night} it fades; otherwise it looks for a lantern (every {@value #LOOK_TICKS} ticks),
	 * shies away from a player too close if it is revealed, or drifts about its grave. Returns whether it is shying away.
	 */
	public boolean step(ServerLevel level, boolean night) {
		if (!night) {
			fade(level);
			return false;
		}
		if (tickCount % LOOK_TICKS == 0) {
			look(level);
			if (welcomed) {
				reveal(level.getGameTime() + REVEAL_TICKS);
			}
		}
		Vec3 here = position();
		Vec3 haunt = Vec3.atBottomCenterOf(home());
		boolean shying = false;
		double speed = DRIFT_SPEED;
		Player near = revealed() && !welcomed ? level.getNearestPlayer(this, SHY_RADIUS) : null;
		if (near != null && !near.isSpectator() && near.distanceTo(this) < (near.isShiftKeyDown() ? SNEAK_SHY_RADIUS : SHY_RADIUS)) {
			Vec3 away = here.subtract(near.position()).multiply(1.0, 0.0, 1.0);
			away = away.lengthSqr() < 1.0E-4 ? new Vec3(random.nextDouble() - 0.5, 0.0, random.nextDouble() - 0.5) : away;
			target = within(haunt, here.add(away.normalize().scale(3.0)));
			speed = SHY_SPEED;
			shying = true;
		} else if (target == null || target.distanceToSqr(here) < 0.25 || random.nextInt(100) == 0) {
			// Drift to a new spot about the grave, half a block to two above the ground.
			double x = haunt.x + (random.nextDouble() * 2.0 - 1.0) * HAUNT_RADIUS;
			double z = haunt.z + (random.nextDouble() * 2.0 - 1.0) * HAUNT_RADIUS;
			int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
			target = within(haunt, new Vec3(x, ground + 0.5 + random.nextDouble() * 1.5, z));
		}
		Vec3 wanted = target.subtract(here);
		wanted = wanted.lengthSqr() > speed * speed ? wanted.normalize().scale(speed) : wanted;
		setDeltaMovement(getDeltaMovement().add(wanted.subtract(getDeltaMovement()).scale(0.2)));
		// It turns the way it drifts, or, revealed and still, to stare at whoever is nearest.
		Player watcher = revealed() && !shying ? level.getNearestPlayer(this, Spirits.REVEAL_RADIUS) : null;
		Vec3 face = watcher != null && wanted.lengthSqr() < 1.0E-4 ? watcher.position().subtract(here) : wanted;
		if (face.x * face.x + face.z * face.z > 1.0E-6) {
			setYRot(Mth.approachDegrees(getYRot(), (float) (Math.atan2(face.z, face.x) * 180.0 / Math.PI) - 90.0F, 12.0F));
			yBodyRot = getYRot();
			yHeadRot = getYRot();
		}
		if (revealed() && random.nextInt(240) == 0) {
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.GHAST_AMBIENT, SoundSource.NEUTRAL, 0.3F, 1.6F);
		}
		return shying;
	}

	/**
	 * {@code wanted}, kept within its haunt: {@value #HAUNT_RADIUS} blocks across the ground from the grave at
	 * {@code haunt} (the bottom middle of its block), and from half a block to {@value #HAUNT_HEIGHT} above it.
	 */
	public static Vec3 within(Vec3 haunt, Vec3 wanted) {
		double dx = wanted.x - haunt.x;
		double dz = wanted.z - haunt.z;
		double across = Math.sqrt(dx * dx + dz * dz);
		if (across > HAUNT_RADIUS) {
			dx *= HAUNT_RADIUS / across;
			dz *= HAUNT_RADIUS / across;
		}
		return new Vec3(haunt.x + dx, Mth.clamp(wanted.y, haunt.y + 0.5, haunt.y + HAUNT_HEIGHT), haunt.z + dz);
	}

	/** Looks for a lantern: a player holding one within reach, or a glow on it, reveals it. Returns whether it shows. */
	public boolean look(ServerLevel level) {
		Player bearer = level.getNearestPlayer(getX(), getY(), getZ(), Spirits.REVEAL_RADIUS,
				entity -> entity instanceof Player player && !player.isSpectator() && Spirits.holdsLantern(player));
		if (bearer != null || hasEffect(MobEffects.GLOWING)) {
			reveal(level.getGameTime() + REVEAL_TICKS);
		}
		return revealed();
	}

	/** Fades away in a wisp of soul light (server side). */
	public void fade(ServerLevel level) {
		if (revealed()) {
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.6, getZ(), 10, 0.2, 0.4, 0.2, 0.01);
		}
		discard();
	}

	/**
	 * Once revealed: a glass bottle catches it (the bottle fills with Ectoplasm); what it wishes for, once a séance has
	 * learnt that, lays it to rest; anything else it turns from.
	 */
	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		SpiritBoard.Wish wished = wish();
		if (!revealed() || held.isEmpty() || !held.is(Items.GLASS_BOTTLE) && wished == null) {
			return super.mobInteract(player, hand);
		}
		if (level() instanceof ServerLevel level && player instanceof ServerPlayer server) {
			if (held.is(Items.GLASS_BOTTLE)) {
				ItemStack ectoplasm = new ItemStack(JugcraftAgriculture.item("ectoplasm"));
				player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, ectoplasm));
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 0.7F);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.GHAST_AMBIENT, SoundSource.NEUTRAL, 0.4F, 1.9F);
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.6, getZ(), 12, 0.2, 0.4, 0.2, 0.02);
				level.gameEvent(player, GameEvent.ENTITY_INTERACT, position());
				TrickOrTreat.award(server, "ghost_hunter");
				discard();
			} else if (held.is(wished.items)) {
				layToRest(level, server, held);
			} else {
				server.sendOverlayMessage(Component.translatable("message.jugcraft.restless_spirit.not_that"));
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.GHAST_AMBIENT, SoundSource.NEUTRAL, 0.3F, 0.7F);
			}
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * {@code giver} gives it {@code gift}, what it wished for: it takes it and rises away in a column of light, at rest. The
	 * giver gets {@value SpiritBoard#REST_XP} experience and Luck for {@value SpiritBoard#REST_LUCK_TICKS} ticks.
	 */
	public void layToRest(ServerLevel level, ServerPlayer giver, ItemStack gift) {
		gift.consume(1, giver);
		giver.giveExperiencePoints(SpiritBoard.REST_XP);
		giver.addEffect(new MobEffectInstance(MobEffects.LUCK, SpiritBoard.REST_LUCK_TICKS));
		level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.8, getZ(), 24, 0.2, 0.8, 0.2, 0.04);
		level.sendParticles(ParticleTypes.SOUL, getX(), getY() + 0.4, getZ(), 8, 0.25, 0.3, 0.25, 0.02);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0F, 0.6F);
		giver.sendOverlayMessage(Component.translatable("message.jugcraft.restless_spirit.at_rest",
				SpiritBoardBlockEntity.spiritName(Math.max(0, spiritName))));
		level.gameEvent(giver, GameEvent.ENTITY_INTERACT, position());
		TrickOrTreat.award(giver, "unfinished_business");
		discard();
	}

	/** Only a revealed spirit can be aimed at. */
	@Override
	public boolean isPickable() {
		return revealed() && super.isPickable();
	}

	/** Blows pass through it (only what ignores invulnerability, such as /kill, touches it). */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.hurtServer(level, source, amount);
		}
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide() && revealed() && random.nextInt(6) == 0) {
			level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, getRandomX(0.4), getY() + random.nextDouble() * 0.6, getRandomZ(0.4), 0.0, 0.01, 0.0);
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
	protected void checkFallDamage(double distance, boolean onGround, BlockState state, BlockPos pos) {
	}

	@Override
	public boolean isIgnoringBlockTriggers() {
		return true;
	}

	@Override
	public boolean shouldDropExperience() {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.store("home", BlockPos.CODEC, home());
		output.putInt("spirit_name", spiritName);
		output.putInt("wish", wish);
		output.putBoolean("welcomed", welcomed);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		home = input.read("home", BlockPos.CODEC).orElse(null);
		spiritName = input.getIntOr("spirit_name", -1);
		wish = input.getIntOr("wish", -1);
		welcomed = input.getBooleanOr("welcomed", false);
		if (spiritName < 0 || spiritName >= SpiritBoard.NAMES.length || wish < 0 || wish >= SpiritBoard.Wish.values().length) {
			spiritName = -1;
			wish = -1;
		}
	}
}
