package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Haunted Hayride: a hay wagon on rails, a minecart with {@value #SEATS} seats on its hay bales. Use it to climb on
 * (Fabric's UseEntityCallback, {@link #board}, so several can ride); it rolls like a minecart, powered rails and all.
 * At night, while it rolls with riders, something spooky is heard from the dark beside it every
 * {@value #SPOOK_MIN} to {@value #SPOOK_MAX} ticks: a shriek, a cackle, a groan, a rattle of bones or bats, with a
 * swirl of soul sparks. The jack o'lantern on its post is drawn lit by the client (client/HauntedHayrideRenderer.java).
 */
public class HauntedHayride extends Minecart {
	public static final int SEATS = 4;
	public static final int SPOOK_MIN = 100;
	public static final int SPOOK_MAX = 240;
	/** How high riders sit above the rails, on the bales. */
	public static final double SEAT_Y = 0.45;
	private static final SoundEvent[] SPOOKS = {SoundEvents.GHAST_SCREAM, SoundEvents.WITCH_CELEBRATE, SoundEvents.ZOMBIE_AMBIENT,
			SoundEvents.SKELETON_AMBIENT, SoundEvents.BAT_TAKEOFF};

	private int nextSpook = SPOOK_MIN;

	public HauntedHayride(EntityType<? extends HauntedHayride> type, Level level) {
		super(type, level);
	}

	@Override
	protected Item getDropItem() {
		return JugcraftAgriculture.item("haunted_hayride");
	}

	@Override
	public ItemStack getPickResult() {
		return new ItemStack(getDropItem());
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return getPassengers().size() < SEATS;
	}

	/** Two riders on each side, on the bales along the wagon's sides. */
	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		int index = Math.max(0, getPassengers().indexOf(passenger));
		double along = index < 2 ? 0.4 : -0.4;
		double side = index % 2 == 0 ? 0.3 : -0.3;
		return new Vec3(along, SEAT_Y, side).yRot(-getYRot() * Mth.DEG_TO_RAD);
	}

	/** A player using a hayride with a seat free climbs on (not sneaking, not riding already). */
	public static InteractionResult board(Player player, Level level, InteractionHand hand, Entity entity) {
		if (!(entity instanceof HauntedHayride ride) || player.isSecondaryUseActive() || player.isPassenger() || ride.getPassengers().size() >= SEATS) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		return player.startRiding(ride) ? InteractionResult.SUCCESS : InteractionResult.PASS;
	}

	public static boolean night(long dayTime) {
		long hour = Math.floorMod(dayTime, TrickOrTreat.DAY);
		return hour >= 13000 && hour < 23000;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level) || !isVehicle()) {
			return;
		}
		boolean rolling = getDeltaMovement().horizontalDistanceSqr() > 1.0E-4;
		if (rolling && night(level.getOverworldClockTime()) && --nextSpook <= 0) {
			spook(level);
			nextSpook = SPOOK_MIN + random.nextInt(SPOOK_MAX - SPOOK_MIN + 1);
		}
	}

	/** Something spooky from the dark a few blocks to one side, for the riders. */
	public void spook(ServerLevel level) {
		double angle = random.nextDouble() * Math.PI * 2;
		double x = getX() + Math.cos(angle) * 6;
		double z = getZ() + Math.sin(angle) * 6;
		SoundEvent sound = SPOOKS[random.nextInt(SPOOKS.length)];
		level.playSound(null, x, getY() + 1.0, z, sound, SoundSource.HOSTILE, 0.8F, 0.8F + random.nextFloat() * 0.3F);
		level.sendParticles(ParticleTypes.SOUL, x, getY() + 1.0, z, 12, 0.6, 0.6, 0.6, 0.02);
	}
}
