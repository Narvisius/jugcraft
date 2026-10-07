package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A spooky firework in flight: it climbs (faster each tick, as a vanilla rocket does) trailing sparks, and after its
 * flight time, or where it hits something, bursts into its {@link FireworkShape}. The burst is sent to every player who
 * can see the rocket ({@link SpookyBurstPayload}); each client draws the picture facing them. It hurts nothing and
 * breaks nothing. Flight time: {@value #LIFETIME_BASE} ticks for each flight level plus one, and up to
 * {@value #LIFETIME_SPREAD} more. A rocket fired sideways (from a dispenser) flies straight instead of climbing.
 */
public class SpookyRocket extends ThrowableItemProjectile {
	public static final int LIFETIME_BASE = 10;
	public static final int LIFETIME_SPREAD = 12;
	/** How much faster it climbs each tick (blocks per tick). */
	public static final double CLIMB = 0.04;

	private int life;
	private int lifetime = LIFETIME_BASE * 2;
	private boolean angled;

	public SpookyRocket(EntityType<? extends SpookyRocket> type, Level level) {
		super(type, level);
	}

	public SpookyRocket(Level level, double x, double y, double z, ItemStack stack, Vec3 velocity, boolean angled) {
		super(JugcraftAgriculture.SPOOKY_ROCKET, x, y, z, level, stack.copyWithCount(1));
		setDeltaMovement(velocity);
		this.angled = angled;
		this.lifetime = lifetime(stack, level.getRandom());
	}

	/** How long a rocket made from {@code stack} flies, in ticks: by its flight level, with a little chance. */
	public static int lifetime(ItemStack stack, RandomSource random) {
		Fireworks fireworks = stack.getOrDefault(DataComponents.FIREWORKS, new Fireworks(1, List.of()));
		int half = LIFETIME_SPREAD / 2;
		return LIFETIME_BASE * (fireworks.flightDuration() + 1) + random.nextInt(half + 1) + random.nextInt(half + 1);
	}

	public int lifetime() {
		return lifetime;
	}

	public int life() {
		return life;
	}

	/** The picture it bursts into. */
	public FireworkShape shape() {
		return getItem().getItem() instanceof SpookyFireworkItem firework ? firework.shape() : FireworkShape.PUMPKIN;
	}

	public boolean twinkles() {
		return Boolean.TRUE.equals(getItem().get(JugcraftAgriculture.TWINKLE));
	}

	@Override
	protected Item getDefaultItem() {
		return JugcraftAgriculture.item(FireworkShape.PUMPKIN.item());
	}

	@Override
	protected double getDefaultGravity() {
		return 0.0;
	}

	@Override
	public void tick() {
		super.tick();
		if (isRemoved()) {
			return;
		}
		if (!angled) {
			setDeltaMovement(getDeltaMovement().add(0.0, CLIMB, 0.0));
		}
		if (level().isClientSide()) {
			Vec3 motion = getDeltaMovement();
			level().addParticle(ParticleTypes.FIREWORK, getX(), getY(), getZ(), random.nextGaussian() * 0.05, -motion.y * 0.5,
					random.nextGaussian() * 0.05);
			return;
		}
		if (life == 0 && !isSilent()) {
			level().playSound(null, getX(), getY(), getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.AMBIENT, 3.0F, 1.0F);
		}
		if (++life > lifetime && level() instanceof ServerLevel server) {
			burst(server);
		}
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (level() instanceof ServerLevel server) {
			burst(server);
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		if (level() instanceof ServerLevel server) {
			burst(server);
		}
	}

	/** Bursts: every player who can see it is sent the picture to draw; then it is gone. */
	public void burst(ServerLevel level) {
		if (isRemoved()) {
			return;
		}
		SpookyBurstPayload payload = new SpookyBurstPayload(getX(), getY(), getZ(), shape().ordinal(), twinkles());
		for (ServerPlayer player : PlayerLookup.tracking(this)) {
			ServerPlayNetworking.send(player, payload);
		}
		gameEvent(GameEvent.EXPLODE, getOwner());
		discard();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("life", life);
		output.putInt("lifetime", lifetime);
		output.putBoolean("angled", angled);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		life = input.getIntOr("life", 0);
		lifetime = input.getIntOr("lifetime", LIFETIME_BASE * 2);
		angled = input.getBooleanOr("angled", false);
	}
}
