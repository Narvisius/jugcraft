package io.github.jimbozoomer.jugcraft.creatures.scary;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The crook inside a costumed villain's costume (docs/features/costumed-villains-unmasking.md): a townsperson-looking
 * figure, named for who they turned out to be, who stands caught for {@value #CAUGHT_TICKS} ticks, looking at whoever
 * is near, and is then led away (a puff of smoke, and they are gone). Nothing can hurt them but {@code /kill}.
 */
public final class UnmaskedCrook extends PathfinderMob {
	/** How long the crook stands caught before being led away, in ticks. */
	public static final int CAUGHT_TICKS = 200;
	/** How far away players hear what happens to the crook, in blocks. */
	public static final double HEARING = 32.0;
	private static final EntityDataAccessor<String> SKIN = SynchedEntityData.defineId(UnmaskedCrook.class, EntityDataSerializers.STRING);

	private int caught = CAUGHT_TICKS;

	public UnmaskedCrook(EntityType<? extends UnmaskedCrook> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder attributes() {
		return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.2);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(SKIN, "townsfolk_1");
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 12.0F));
	}

	/** Which of the town's skins they wear (textures/entity/townsfolk/). */
	public String skin() {
		return entityData.get(SKIN);
	}

	public void setSkin(String skin) {
		entityData.set(SKIN, skin);
	}

	/** Ticks left before they are led away. */
	public int caught() {
		return caught;
	}

	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel level && isAlive() && --caught <= 0) {
			level.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0, getZ(), 20, 0.3, 0.5, 0.3, 0.02);
			Component led = Component.translatable("message.jugcraft.unmasked.led_away", getDisplayName());
			for (ServerPlayer player : level.getPlayers(near -> near.distanceToSqr(this) < HEARING * HEARING)) {
				player.sendSystemMessage(led);
			}
			discard();
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		// Caught, not fought: only /kill (and the void) can remove one before they are led away.
		if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return false;
		}
		return super.hurtServer(level, source, amount);
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("caught", caught);
		output.putString("skin", skin());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		caught = input.getIntOr("caught", CAUGHT_TICKS);
		setSkin(input.getStringOr("skin", "townsfolk_1"));
	}
}
