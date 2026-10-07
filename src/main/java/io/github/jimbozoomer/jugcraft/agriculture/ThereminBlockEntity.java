package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Theremin's ear: every {@value #SENSE_TICKS} ticks it finds the nearest creature (a player or any mob; not a spectator,
 * nor an armour stand)
 * within {@value #RANGE} blocks of its pitch antenna. Comparators read how near: 15 at the antenna, one less for each
 * {@value #RANGE}/15 of a block further, 0 with no one in range, whether it is playing or not. Playing (switched on, or
 * powered), it sings for whoever is nearest: the pitch rises over two octaves as they come from {@value #RANGE} blocks
 * off to the antenna, with a slow vibrato, louder the closer they are. A player within {@value #PLAYER_RANGE} blocks of a
 * playing theremin earns Good Vibrations.
 */
public class ThereminBlockEntity extends BlockEntity {
	public static final int SENSE_TICKS = 4;
	public static final double RANGE = 8.0;
	public static final double PLAYER_RANGE = 3.0;
	/** The pitch at the edge of its range and at the antenna (a note block's are 0.5 to 2). */
	public static final float LOW = 0.5F;
	public static final float HIGH = 2.0F;
	/** How far the vibrato bends the pitch, and how fast (radians a tick). */
	public static final float VIBRATO = 0.03F;
	public static final float VIBRATO_SPEED = 0.7F;

	private int signal;
	private double distance = -1.0;

	public ThereminBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.THEREMIN_ENTITY, pos, state);
	}

	/** The comparator reading: 0 to 15. */
	public int signal() {
		return signal;
	}

	/** How far the nearest creature was at the last look, or -1 for none. */
	public double distance() {
		return distance;
	}

	/** Where the pitch antenna stands, at the cabinet's right front corner (as its player faces it). */
	public static Vec3 antenna(BlockPos pos, Direction facing) {
		Direction right = facing.getCounterClockWise();
		return Vec3.atCenterOf(pos).add(right.getStepX() * 0.3, 0.6, right.getStepZ() * 0.3);
	}

	/** The comparator reading for a creature {@code distance} blocks off (or none, for a negative distance). */
	public static int signalFor(double distance) {
		if (distance < 0.0 || distance > RANGE) {
			return 0;
		}
		return Mth.clamp(15 - (int) Math.floor(distance * 15.0 / RANGE), 0, 15);
	}

	/** The pitch for a creature {@code distance} blocks off: {@value #LOW} at the edge of its range, {@value #HIGH} at the antenna. */
	public static float pitchFor(double distance) {
		double near = 1.0 - Mth.clamp(distance / RANGE, 0.0, 1.0);
		return (float) (LOW * Math.pow(HIGH / LOW, near));
	}

	private @Nullable LivingEntity nearest(ServerLevel level, Vec3 antenna) {
		LivingEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(antenna, antenna).inflate(RANGE),
				living -> (living instanceof Mob || living instanceof Player) && living.isAlive() && !living.isSpectator())) {
			double d = entity.getBoundingBox().getCenter().distanceTo(antenna);
			if (d <= RANGE && d < bestDistance) {
				best = entity;
				bestDistance = d;
			}
		}
		return best;
	}

	void serverTick(ServerLevel level, BlockState state) {
		long time = level.getGameTime();
		if (time % SENSE_TICKS != 0) {
			return;
		}
		Direction facing = state.hasProperty(ThereminBlock.FACING) ? state.getValue(ThereminBlock.FACING) : Direction.NORTH;
		Vec3 antenna = antenna(worldPosition, facing);
		LivingEntity near = nearest(level, antenna);
		distance = near == null ? -1.0 : near.getBoundingBox().getCenter().distanceTo(antenna);
		int now = signalFor(distance);
		if (now != signal) {
			signal = now;
			setChanged();
			level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
		}
		if (near == null || !ThereminBlock.playing(state)) {
			return;
		}
		float pitch = pitchFor(distance) * (1.0F + VIBRATO * Mth.sin(time * VIBRATO_SPEED));
		float volume = 0.6F + 0.4F * (float) (1.0 - distance / RANGE);
		level.playSound(null, antenna.x, antenna.y, antenna.z, SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.RECORDS, volume, pitch);
		double note = Mth.clamp(Math.log(pitch / LOW) / Math.log(HIGH / LOW), 0.0, 1.0);
		level.sendParticles(ParticleTypes.NOTE, antenna.x, antenna.y + 0.6, antenna.z, 0, note, 0.0, 0.0, 1.0);
		if (near instanceof ServerPlayer player && distance <= PLAYER_RANGE) {
			TrickOrTreat.award(player, "good_vibrations");
		}
	}

	/** Whether {@code player} is close enough to play it (for its message). */
	public static boolean inReach(BlockPos pos, Direction facing, Player player) {
		return player.getBoundingBox().getCenter().distanceTo(antenna(pos, facing)) <= PLAYER_RANGE;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		signal = Mth.clamp(input.getIntOr("signal", 0), 0, 15);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("signal", signal);
	}
}
