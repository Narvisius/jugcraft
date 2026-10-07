package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Pillar Candles, ivory or black (the haunted house's props): vanilla's candle, one to four in a block, lit and put out
 * as vanilla's are, but tall church candles of different heights clustered together and dripping wax. {@link #LAYOUT}
 * gives each candle's centre, width and height in pixels for each count, and its flame stands {@value #FLAME_ABOVE}
 * pixels over its top.
 */
public class PillarCandleBlock extends CandleBlock {
	public static final double FLAME_ABOVE = 2.0;
	/** For one to four candles: each candle's {centre x, centre z, width, height} in pixels (tools/decor16.py CANDLES). */
	public static final double[][][] LAYOUT = {
			{{8.0, 8.0, 5.0, 12.0}},
			{{6.0, 7.0, 5.0, 13.0}, {10.5, 10.0, 4.0, 8.0}},
			{{5.5, 6.5, 5.0, 13.0}, {10.5, 7.0, 4.0, 10.0}, {8.0, 11.0, 4.0, 6.0}},
			{{5.0, 6.0, 5.0, 14.0}, {10.5, 6.0, 4.0, 10.0}, {5.5, 11.0, 4.0, 8.0}, {10.5, 11.0, 4.0, 5.0}}};
	private static final List<List<Vec3>> FLAMES = new ArrayList<>();
	private static final VoxelShape[] SHAPES = new VoxelShape[LAYOUT.length];

	static {
		for (int i = 0; i < LAYOUT.length; i++) {
			List<Vec3> flames = new ArrayList<>();
			VoxelShape shape = Shapes.empty();
			for (double[] c : LAYOUT[i]) {
				double r = c[2] / 2;
				flames.add(new Vec3(c[0] / 16, (c[3] + FLAME_ABOVE) / 16, c[1] / 16));
				shape = Shapes.or(shape, Block.box(c[0] - r, 0.0, c[1] - r, c[0] + r, c[3], c[1] + r));
			}
			FLAMES.add(List.copyOf(flames));
			SHAPES[i] = shape.optimize();
		}
	}

	public PillarCandleBlock(Properties properties) {
		super(properties);
	}

	/** Where the flames of {@code count} candles stand in the block (fractions of a block). */
	public static List<Vec3> flames(int count) {
		return FLAMES.get(count - 1);
	}

	@Override
	protected Iterable<Vec3> getParticleOffsets(BlockState state) {
		return flames(state.getValue(CANDLES));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[state.getValue(CANDLES) - 1];
	}
}
