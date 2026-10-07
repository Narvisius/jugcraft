package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * The Mourning Angel: a two-block marble statue on a plinth, wings folded, head bowed into its hands. It stands on any
 * solid top and faces whoever placed it, like the Scarecrow. At night it weeps: now and then a tear drips from its
 * hands (only drawn by clients).
 */
public class MourningAngelBlock extends TallDecorationBlock {
	/** Where the tears fall, in pixels of the upper half, for an angel facing north: x, y, z. */
	private static final double[] TEARS = {8.0, 7.0, 3.5};

	public MourningAngelBlock(Properties properties) {
		super(properties, Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0), Block.box(1.0, 0.0, 3.0, 15.0, 14.0, 13.0));
	}

	/** Whether it is night (dusk to dawn) on the overworld clock, any time of year. */
	public static boolean night(Level level) {
		long hour = Math.floorMod(level.getOverworldClockTime(), TrickOrTreat.DAY);
		return hour >= HarvestMoon.DUSK && hour < HarvestMoon.DAWN;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(HALF) != DoubleBlockHalf.UPPER || random.nextInt(5) != 0 || !night(level)) {
			return;
		}
		Direction facing = state.getValue(FACING);
		double dx = TEARS[0] / 16.0 - 0.5 + (random.nextDouble() - 0.5) * 0.15;
		double dz = TEARS[2] / 16.0 - 0.5;
		// Turn the north-facing spot round to this facing (about the block's centre), as a block model's y rotation does.
		double x = switch (facing) {
			case SOUTH -> -dx;
			case WEST -> dz;
			case EAST -> -dz;
			default -> dx;
		};
		double z = switch (facing) {
			case SOUTH -> -dz;
			case WEST -> -dx;
			case EAST -> dx;
			default -> dz;
		};
		level.addParticle(ParticleTypes.DRIPPING_WATER, pos.getX() + 0.5 + x, pos.getY() + TEARS[1] / 16.0, pos.getZ() + 0.5 + z, 0.0, 0.0, 0.0);
	}
}
