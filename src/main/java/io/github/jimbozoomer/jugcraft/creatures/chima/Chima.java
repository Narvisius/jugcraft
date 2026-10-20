package io.github.jimbozoomer.jugcraft.creatures.chima;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Chima (docs/features/chima.md): a lion cub with a great spiky orange mane, a round yellow body and a star-tipped tail,
 * at home on the savanna. Wild, it hunts cows: it picks out the nearest, roars ("rawr!") and pounces, and once it has
 * brought one down it rests for {@value #HUNT_REST} ticks before it hunts again. Hold out beef (item tag
 * {@code jugcraft:chima_food}) and it comes to you; fed beef, it is tamed one time in {@value #TAME_CHANCE}, as a cat is
 * with fish (hearts if it takes to you, smoke if not).
 *
 * <p>Its look changes with what it is given (each from an item tag {@code jugcraft:chima_skins/<skin>}): a jack
 * o'lantern makes it a pumpkin cub with a little jack o'lantern on its tail, rotten flesh a stitched green-and-pink
 * zombie, a bone a little skeleton with a round skull, and fish a shark, blue in front and yellow behind. Anyone may change
 * a wild one; only its owner a tame one. Its owner, sneaking with an empty hand, has it go back to its own look.
 *
 * <p>Tamed, it wears a red collar, belongs to whoever tamed it and follows them, coming to them when they are far off.
 * It no longer hunts cows on its own, so it is safe on a farm, but it goes for whatever hurts its owner. Its owner uses
 * it with an empty hand to have it sit and stay, as with a tame wolf; beef heals it. Its owner's blows don't hurt it.
 * Everything is decided on the server.
 */
public class Chima extends PathfinderMob {
	public static final float MAX_HEALTH = 16.0F;
	public static final float DAMAGE = 4.0F;
	public static final double SPEED = 0.3;
	/** How far off it notices beef held out to it (the attribute its tempt goal reads). */
	public static final double TEMPT_RANGE = 10.0;
	/** Fed beef, a wild one is tamed one time in this many. */
	public static final int TAME_CHANCE = 3;
	/** After bringing down a cow, it rests this many ticks (two minutes) before it hunts again. */
	public static final int HUNT_REST = 2400;
	public static final float BEEF_HEAL = 4.0F;
	/** Tamed, it follows its owner once they are further than this, in blocks, and stops this near. */
	public static final double FOLLOW_START = 7.0;
	public static final double FOLLOW_STOP = 2.5;
	/** Further than this from its owner, it comes to them at once. */
	public static final double TELEPORT_DISTANCE = 16.0;
	public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, Jugcraft.id("chima_food"));
	private static final EntityDataAccessor<Boolean> TAME = SynchedEntityData.defineId(Chima.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> SITTING = SynchedEntityData.defineId(Chima.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(Chima.class, EntityDataSerializers.INT);

	/** Its looks: its own, and the four it takes on from what it is given. */
	public enum Skin {
		NORMAL, PUMPKIN, ZOMBIE, SKELETON, SHARK;

		/** The items that give this look (none for its own). */
		public final @Nullable TagKey<Item> items;

		Skin() {
			String name = name().toLowerCase(java.util.Locale.ROOT);
			items = name.equals("normal") ? null : TagKey.create(Registries.ITEM, Jugcraft.id("chima_skins/" + name));
		}

		public String id() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}

		/** The look {@code stack} gives, or null if it gives none. */
		public static @Nullable Skin of(ItemStack stack) {
			for (Skin skin : values()) {
				if (skin.items != null && stack.is(skin.items)) {
					return skin;
				}
			}
			return null;
		}

		public static Skin byId(String id) {
			for (Skin skin : values()) {
				if (skin.id().equals(id)) {
					return skin;
				}
			}
			return NORMAL;
		}
	}

	private @Nullable UUID owner;
	/** Ticks left resting after a kill. */
	private int rest;

	public Chima(EntityType<? extends Chima> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, MAX_HEALTH).add(Attributes.MOVEMENT_SPEED, SPEED)
				.add(Attributes.ATTACK_DAMAGE, DAMAGE).add(Attributes.TEMPT_RANGE, TEMPT_RANGE).add(Attributes.FOLLOW_RANGE, 20.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(TAME, false);
		builder.define(SITTING, false);
		builder.define(SKIN, Skin.NORMAL.ordinal());
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new StayGoal());
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.35, true));
		goalSelector.addGoal(3, new TemptGoal(this, 1.1, stack -> stack.is(FOOD), false));
		goalSelector.addGoal(4, new FollowOwnerGoal());
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new DefendOwnerGoal());
		targetSelector.addGoal(2, new HurtByTargetGoal(this));
		targetSelector.addGoal(3, new HuntGoal());
	}

	// ---------------------------------------------------------------- what it is

	public boolean tame() {
		return entityData.get(TAME);
	}

	public Skin skin() {
		int index = entityData.get(SKIN);
		return index >= 0 && index < Skin.values().length ? Skin.values()[index] : Skin.NORMAL;
	}

	public void setSkin(Skin skin) {
		entityData.set(SKIN, skin.ordinal());
	}

	public boolean sitting() {
		return entityData.get(SITTING);
	}

	public void setSitting(boolean sitting) {
		entityData.set(SITTING, sitting);
		if (sitting) {
			getNavigation().stop();
			setTarget(null);
		}
	}

	public @Nullable UUID owner() {
		return owner;
	}

	public boolean ownedBy(Player player) {
		return tame() && player.getUUID().equals(owner);
	}

	public @Nullable Player ownerPlayer() {
		return owner == null ? null : level().getPlayerByUUID(owner);
	}

	/** Ticks left resting after a kill (0 when it would hunt). */
	public int rest() {
		return rest;
	}

	/** Tames it to {@code player}: it is theirs from now on, sits where it is, and stops hunting. */
	public void tameTo(Player player) {
		owner = player.getUUID();
		entityData.set(TAME, true);
		setPersistenceRequired();
		setTarget(null);
		setSitting(true);
	}

	// ---------------------------------------------------------------- hunting

	/** A wild one hunting picks out the nearest cow it can see, and roars as it starts after it. */
	private class HuntGoal extends NearestAttackableTargetGoal<Cow> {
		HuntGoal() {
			super(Chima.this, Cow.class, true);
		}

		@Override
		public boolean canUse() {
			return !tame() && rest == 0 && super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return !tame() && super.canContinueToUse();
		}

		@Override
		public void start() {
			super.start();
			roar();
		}
	}

	/** Its "rawr!": a cub's high-pitched roar. */
	public void roar() {
		level().playSound(null, getX(), getY(), getZ(), SoundEvents.POLAR_BEAR_WARNING, SoundSource.NEUTRAL, 0.8F, 1.7F + random.nextFloat() * 0.2F);
	}

	/** A blow that brings a cow down ends the hunt for a while. */
	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		// Retaliation also reaches this path: judging only the owner-defense goal would bypass claims/PvP.
		if (tame() && target instanceof LivingEntity living) {
			Player actor = ownerPlayer();
			if (target.getUUID().equals(owner) || target instanceof Player && actor == null
					|| !ConcordanceEffects.mayHarm(actor, living) || !Authority.mayStrike(level, actor, owner, living)) {
				setTarget(null);
				return false;
			}
		}
		boolean hit = super.doHurtTarget(level, target);
		if (hit && target instanceof Cow && !target.isAlive()) {
			rest = HUNT_REST;
			setTarget(null);
		}
		return hit;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (rest > 0) {
			rest--;
		}
	}

	// ---------------------------------------------------------------- feeding and taming

	/**
	 * Beef tames a wild one, one time in {@value #TAME_CHANCE}, and heals a tame one that is hurt. A jack o'lantern,
	 * rotten flesh, a bone or fish changes its look (a tame one's only for its owner). Its owner, with an empty hand, has
	 * it sit or get up, and sneaking, has it go back to its own look. Decided on the server, where its owner is known.
	 */
	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		boolean food = held.is(FOOD);
		Skin given = Skin.of(held);
		boolean mayChange = !tame() || ownedBy(player);
		if (!(level() instanceof ServerLevel level)) {
			return food || given != null && mayChange || held.isEmpty() && tame() ? InteractionResult.SUCCESS : super.mobInteract(player, hand);
		}
		if (given != null && mayChange && given != skin()) {
			held.consume(1, player);
			setSkin(given);
			playSound(SoundEvents.GENERIC_EAT.value(), 0.7F, 1.1F);
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 0.7, getZ(), 10, 0.35, 0.35, 0.35, 0.0);
			roar();
			return InteractionResult.SUCCESS;
		}
		if (food && !tame()) {
			held.consume(1, player);
			playSound(SoundEvents.GENERIC_EAT.value(), 0.7F, 1.3F);
			if (random.nextInt(TAME_CHANCE) == 0) {
				tameTo(player);
				level.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.9, getZ(), 7, 0.3, 0.2, 0.3, 0.0);
			} else {
				level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.9, getZ(), 7, 0.3, 0.2, 0.3, 0.02);
			}
			return InteractionResult.SUCCESS;
		}
		if (food && getHealth() < getMaxHealth()) {
			heal(BEEF_HEAL);
			held.consume(1, player);
			playSound(SoundEvents.GENERIC_EAT.value(), 0.7F, 1.3F);
			level.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.9, getZ(), 2, 0.2, 0.1, 0.2, 0.0);
			return InteractionResult.SUCCESS;
		}
		if (held.isEmpty() && ownedBy(player) && player.isSecondaryUseActive()) {
			setSkin(Skin.NORMAL);
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 0.7, getZ(), 6, 0.3, 0.3, 0.3, 0.0);
			return InteractionResult.SUCCESS;
		}
		if (held.isEmpty() && ownedBy(player)) {
			setSitting(!sitting());
			return InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	// ---------------------------------------------------------------- following

	/** Comes to its owner: to open ground two or three blocks from them. Returns whether it found a spot. */
	public boolean comeToOwner() {
		Player player = ownerPlayer();
		if (!tame() || sitting() || player == null || player.isSpectator() || player.level() != level()) {
			return false;
		}
		BlockPos center = player.blockPosition();
		for (int attempt = 0; attempt < 20; attempt++) {
			int dx = random.nextInt(7) - 3;
			int dz = random.nextInt(7) - 3;
			if (Math.abs(dx) < 2 && Math.abs(dz) < 2) {
				continue;
			}
			for (int dy = 1; dy >= -1; dy--) {
				BlockPos spot = center.offset(dx, dy, dz);
				Vec3 to = Vec3.atBottomCenterOf(spot);
				if (level().isLoaded(spot) && level().isInWorldBounds(spot) && level().isInWorldBounds(spot.above())
						&& level().getWorldBorder().isWithinBounds(spot)
						&& level().getFluidState(spot).isEmpty() && level().getFluidState(spot.above()).isEmpty()
						&& level().getBlockState(spot.below()).isFaceSturdy(level(), spot.below(), Direction.UP)
						&& level().noCollision(this, getBoundingBox().move(to.subtract(position())))) {
					snapTo(to.x, to.y, to.z, getYRot(), getXRot());
					getNavigation().stop();
					return true;
				}
			}
		}
		return false;
	}

	/** Sitting, it stays where it is. */
	private class StayGoal extends Goal {
		StayGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return sitting();
		}

		@Override
		public void start() {
			getNavigation().stop();
		}
	}

	/** Tamed, it follows its owner: walks to them past {@link #FOLLOW_START}, and comes to them at once past {@link #TELEPORT_DISTANCE}. */
	private class FollowOwnerGoal extends Goal {
		private @Nullable Player target;
		private int repath;

		FollowOwnerGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (!tame() || sitting() || getTarget() != null) {
				return false;
			}
			Player player = ownerPlayer();
			if (player == null || player.isSpectator() || player.level() != level() || distanceToSqr(player) < FOLLOW_START * FOLLOW_START) {
				return false;
			}
			target = player;
			return true;
		}

		@Override
		public boolean canContinueToUse() {
			return target != null && !sitting() && getTarget() == null && target.isAlive() && target.level() == level()
					&& distanceToSqr(target) > FOLLOW_STOP * FOLLOW_STOP;
		}

		@Override
		public void start() {
			repath = 0;
		}

		@Override
		public void stop() {
			target = null;
			getNavigation().stop();
		}

		@Override
		public void tick() {
			if (target == null) {
				return;
			}
			getLookControl().setLookAt(target, 10.0F, getMaxHeadXRot());
			if (--repath > 0) {
				return;
			}
			repath = 10;
			if (distanceToSqr(target) > TELEPORT_DISTANCE * TELEPORT_DISTANCE && comeToOwner()) {
				return;
			}
			getNavigation().moveTo(target, 1.2);
		}
	}

	/** Tamed and not sitting, it goes for whatever last hurt its owner. */
	private class DefendOwnerGoal extends Goal {
		private int seen;

		DefendOwnerGoal() {
			setFlags(EnumSet.of(Goal.Flag.TARGET));
		}

		@Override
		public boolean canUse() {
			if (!tame() || sitting()) {
				return false;
			}
			Player player = ownerPlayer();
			if (player == null) {
				return false;
			}
			LivingEntity attacker = player.getLastHurtByMob();
			return attacker != null && attacker.isAlive() && attacker != Chima.this && player.getLastHurtByMobTimestamp() != seen
					&& !(attacker instanceof Chima other && other.ownedBy(player)) && !(attacker instanceof Player);
		}

		@Override
		public void start() {
			Player player = ownerPlayer();
			if (player != null) {
				seen = player.getLastHurtByMobTimestamp();
				setTarget(player.getLastHurtByMob());
				roar();
			}
		}

		@Override
		public boolean canContinueToUse() {
			return false;
		}
	}

	// ---------------------------------------------------------------- harm and sounds

	/** Its owner's blows don't hurt it. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getEntity() instanceof Player player && ownedBy(player)) {
			return false;
		}
		return super.hurtServer(level, source, amount);
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return !tame() && super.removeWhenFarAway(distance);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.POLAR_BEAR_AMBIENT_BABY;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.POLAR_BEAR_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.POLAR_BEAR_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 1.5F + random.nextFloat() * 0.2F;
	}

	// ---------------------------------------------------------------- saving

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("tame", tame());
		if (owner != null) {
			output.store("owner", UUIDUtil.CODEC, owner);
		}
		output.putBoolean("sitting", sitting());
		output.putInt("rest", rest);
		output.putString("skin", skin().id());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(TAME, input.getBooleanOr("tame", false));
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
		setSitting(input.getBooleanOr("sitting", false));
		rest = Math.max(0, input.getIntOr("rest", 0));
		setSkin(Skin.byId(input.getStringOr("skin", "normal")));
	}
}
