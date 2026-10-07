package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Ring Toss (fall addition 26): a slatted crate of nine bottles, necks up in three rows. A Toss Ring ({@link TossRing})
 * landing on its top within {@value #RINGER_RADIUS} pixels of a neck, thrown from {@value #MIN_DISTANCE} or more blocks
 * off, is a ringer: it settles over that bottle ({@link #RINGED}, the bottle's number from 1, or 0 for none) for
 * {@value #RINGER_TICKS} ticks, and its thrower wins a prize ({@link Midway#prize}) and Ringer!. The server judges where
 * the ring came down and where it was thrown from.
 */
public class RingTossBlock extends Block {
	public static final IntegerProperty RINGED = IntegerProperty.create("ringed", 0, 9);
	public static final double RINGER_RADIUS = 1.25;
	public static final double MIN_DISTANCE = 3.0;
	public static final int RINGER_TICKS = 60;
	/** The bottles' centres across the crate, in pixels, and the height (pixels) of the crate's top, where rings land. */
	public static final double[] NECKS = {3.5, 8.0, 12.5};
	public static final double TOP = 13.0;
	private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, TOP, 16.0);

	public RingTossBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(RINGED, 0));
	}

	/** The bottle (0 to 8, row by row from the north-west) whose neck is within reach of ({@code x}, {@code z}) in pixels, or -1. */
	public static int neckAt(double x, double z) {
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 3; column++) {
				double dx = x - NECKS[column];
				double dz = z - NECKS[row];
				if (dx * dx + dz * dz <= RINGER_RADIUS * RINGER_RADIUS) {
					return row * 3 + column;
				}
			}
		}
		return -1;
	}

	/**
	 * A ring comes down on the crate at {@code pos} at {@code hit}, thrown from {@code from} by {@code thrower}: returns
	 * whether it is a ringer (and, if so, settles it and gives the prize).
	 */
	public static boolean land(ServerLevel level, BlockPos pos, Vec3 hit, Vec3 from, @Nullable Entity thrower) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof RingTossBlock)) {
			return false;
		}
		double x = (hit.x - pos.getX()) * 16.0;
		double z = (hit.z - pos.getZ()) * 16.0;
		double y = (hit.y - pos.getY()) * 16.0;
		int bottle = neckAt(x, z);
		double fromX = from.x - (pos.getX() + 0.5);
		double fromZ = from.z - (pos.getZ() + 0.5);
		if (bottle < 0 || y < TOP - 0.5 || fromX * fromX + fromZ * fromZ < MIN_DISTANCE * MIN_DISTANCE) {
			level.playSound(null, pos, SoundEvents.GLASS_HIT, SoundSource.BLOCKS, 0.6F, 1.6F);
			return false;
		}
		level.setBlock(pos, state.setValue(RINGED, bottle + 1), Block.UPDATE_ALL);
		level.scheduleTick(pos, state.getBlock(), RINGER_TICKS);
		level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.2F);
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + x / 16.0, pos.getY() + 1.0, pos.getZ() + z / 16.0, 8, 0.15, 0.1, 0.15, 0.0);
		if (thrower instanceof ServerPlayer player) {
			TrickOrTreat.award(player, "ringer");
			Midway.prize(level, player, Vec3.atCenterOf(pos.above()));
		}
		return true;
	}

	/** The ringer's time is up: the ring is taken off. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(RINGED) != 0) {
			level.setBlock(pos, state.setValue(RINGED, 0), Block.UPDATE_ALL);
		}
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(RINGED);
	}
}
