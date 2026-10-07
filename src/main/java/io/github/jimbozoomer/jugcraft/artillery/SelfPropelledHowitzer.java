package io.github.jimbozoomer.jugcraft.artillery;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * The Self-Propelled Howitzer (batch 51): a tracked gun carriage. Its driver steers with the movement keys (it climbs
 * one-block steps) and fires the long gun, which turns {@value JugcraftArtillery#HOWITZER_ARC} degrees either side of
 * the hull, flat at what they look at or on whichever arc reaches a marked target. Driving burns
 * {@value JugcraftArtillery#HOWITZER_FUEL_PER_SECOND} mB of diesel or kerosene a second.
 */
public class SelfPropelledHowitzer extends CrewedGun {
	public static final List<String> FUELS = List.of("diesel_bucket", "premium_diesel_bucket", "kerosene_bucket");
	/** Half the distance between the tracks' centres, in blocks (for the tracks' animation). */
	public static final float TRACK_HALF_SPAN = 1.44F;
	private int fuel;
	private float leftTrack;
	private float rightTrack;

	public SelfPropelledHowitzer(EntityType<? extends SelfPropelledHowitzer> type, Level level) {
		super(type, level);
	}

	public int fuel() {
		return fuel;
	}

	public void setFuel(int fuel) {
		this.fuel = Mth.clamp(fuel, 0, JugcraftArtillery.HOWITZER_FUEL_TANK);
	}

	public float leftTrack() {
		return leftTrack;
	}

	public float rightTrack() {
		return rightTrack;
	}

	@Override
	public InteractionResult use(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		Identifier id = BuiltInRegistries.ITEM.getKey(held.getItem());
		if (id.getNamespace().equals("jugcraft") && FUELS.contains(id.getPath())) {
			if (!level().isClientSide()) {
				if (fuel + JugcraftArtillery.FUEL_PER_BUCKET > JugcraftArtillery.HOWITZER_FUEL_TANK) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.artillery.full"));
				} else {
					fuel += JugcraftArtillery.FUEL_PER_BUCKET;
					if (!player.getAbilities().instabuild) {
						player.setItemInHand(hand, new ItemStack(Items.BUCKET));
					}
					level().playSound(null, getX(), getY(), getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0F, 0.8F);
					player.sendOverlayMessage(Component.translatable("message.jugcraft.artillery.refuelled",
							fuel * 100 / JugcraftArtillery.HOWITZER_FUEL_TANK));
				}
			}
			return InteractionResult.SUCCESS;
		}
		return super.use(player, hand);
	}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide()) {
			return;
		}
		double yaw = Math.toRadians(getYRot());
		double moved = (getX() - xo) * -Math.sin(yaw) + (getZ() - zo) * Math.cos(yaw);
		float turned = (float) Math.toRadians(Mth.wrapDegrees(getYRot() - yRotO)) * TRACK_HALF_SPAN;
		leftTrack += (float) moved + turned;
		rightTrack += (float) moved - turned;
		if (Math.abs(moved) + Math.abs(turned) > 0.01 && tickCount % 3 == 0) {
			double x = getX() - 0.5 * Math.cos(yaw) - 0.9 * Math.sin(yaw);
			double z = getZ() - 0.5 * Math.sin(yaw) + 0.9 * Math.cos(yaw);
			level().addParticle(ParticleTypes.LARGE_SMOKE, x, getY() + 2.6, z, 0, 0.04, 0);
		}
	}

	@Override
	protected void move(ServerLevel level, boolean listening) {
		int f = listening ? forward : 0;
		int t = listening ? turn : 0;
		boolean fuelled = fuel > 0;
		if (fuelled && t != 0) {
			setYRot(getYRot() - t * JugcraftArtillery.HOWITZER_TURN);
		}
		double yaw = Math.toRadians(getYRot());
		double speed = !fuelled ? 0 : f > 0 ? JugcraftArtillery.HOWITZER_SPEED : f < 0 ? -JugcraftArtillery.HOWITZER_SPEED / 2 : 0;
		double vy = getDeltaMovement().y - 0.08;
		if (onGround()) {
			vy = Math.max(vy, -0.1);
			if (speed != 0 && horizontalCollision) {
				vy = 0.45; // Climb a one-block step.
			}
		}
		setDeltaMovement(-Math.sin(yaw) * speed, vy * 0.98, Math.cos(yaw) * speed);
		move(MoverType.SELF, getDeltaMovement());
		for (Entity passenger : getPassengers()) {
			passenger.resetFallDistance();
		}
		boolean working = speed != 0 || t != 0;
		if (working && fuelled && tickCount % (20 / JugcraftArtillery.HOWITZER_FUEL_PER_SECOND) == 0) {
			fuel--;
		}
		if (working && tickCount % 8 == 0) {
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.MINECART_RIDING, SoundSource.NEUTRAL, 0.6F, 0.45F);
		}
		if (getFirstPassenger() instanceof Player player && tickCount % 40 == 0) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.artillery.fuel",
					fuel * 100 / JugcraftArtillery.HOWITZER_FUEL_TANK));
		}
	}

	/** The gun turns only so far either side of the hull. */
	@Override
	protected float limitYaw(float yaw) {
		float relative = Mth.clamp(Mth.wrapDegrees(yaw - getYRot()), -JugcraftArtillery.HOWITZER_ARC, JugcraftArtillery.HOWITZER_ARC);
		return getYRot() + relative;
	}

	/** The crew ride with the hull, not the gun. */
	@Override
	protected float seatYaw(int index) {
		return getYRot();
	}

	@Override
	protected EntityType<ArtilleryShell> shellType() {
		return JugcraftArtillery.HEAVY_SHELL;
	}

	@Override
	protected Item ammo() {
		return JugcraftArtillery.HEAVY_SHELL_ITEM;
	}

	@Override
	protected double shellSpeed() {
		return JugcraftArtillery.HEAVY_SPEED;
	}

	@Override
	protected double shellGravity() {
		return JugcraftArtillery.HEAVY_GRAVITY;
	}

	@Override
	protected int cooldown() {
		return JugcraftArtillery.HOWITZER_COOLDOWN;
	}

	@Override
	protected float traverse() {
		return JugcraftArtillery.HOWITZER_TRAVERSE;
	}

	@Override
	protected float minPitch() {
		return -5.0F;
	}

	@Override
	protected float maxPitch() {
		return 70.0F;
	}

	@Override
	protected boolean highArc() {
		return false;
	}

	@Override
	protected double pivotHeight() {
		return JugcraftArtillery.HOWITZER_PIVOT_HEIGHT;
	}

	@Override
	protected double barrelLength() {
		return 5.8;
	}

	@Override
	protected Item dropItem() {
		return JugcraftArtillery.HOWITZER_ITEM;
	}

	@Override
	protected int health() {
		return JugcraftArtillery.HOWITZER_HEALTH;
	}

	@Override
	protected int seats() {
		return 2;
	}

	/** The driver and gunner in the cab; a second crewman on the deck. */
	@Override
	protected Vec3 seat(int index) {
		return index == 0 ? new Vec3(-0.55, 1.45, -1.5) : new Vec3(0.6, 1.4, -1.6);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		fuel = Mth.clamp(input.getIntOr("fuel", 0), 0, JugcraftArtillery.HOWITZER_FUEL_TANK);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("fuel", fuel);
	}
}
