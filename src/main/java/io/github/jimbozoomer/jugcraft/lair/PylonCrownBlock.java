package io.github.jimbozoomer.jugcraft.lair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The tesla crown on top of each of the Foundry Trench's pylons (docs/features/foundry-trench.md): a ribbed insulator, a
 * ring and ball of copper. A lair fixture that cannot be broken or used. {@link #LIVE} (the pylon the Dreadnought's
 * charge will strike), it burns violet, gives light {@value #LIGHT} (tools/lairs.py PYLON_CROWN) and crackles.
 */
public class PylonCrownBlock extends Block {
	public static final BooleanProperty LIVE = BooleanProperty.create("live");
	public static final int LIGHT = 12;
	private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 15, 14);

	public PylonCrownBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LIVE, false));
	}

	/** The light the crown gives in {@code state}. */
	public static int light(BlockState state) {
		return state.getValue(LIVE) ? LIGHT : 0;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** Lights or darkens the crown at {@code pos}; whether it was a crown and it changed. */
	public static boolean setLive(Level level, BlockPos pos, boolean live) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof PylonCrownBlock) || state.getValue(LIVE) == live) {
			return false;
		}
		level.setBlock(pos, state.setValue(LIVE, live), Block.UPDATE_ALL);
		return true;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(LIVE)) {
			for (int i = 0; i < 2; i++) {
				level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.15 + random.nextDouble() * 0.7, pos.getY() + 0.5
						+ random.nextDouble() * 0.6, pos.getZ() + 0.15 + random.nextDouble() * 0.7, 0.0, 0.0, 0.0);
			}
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIVE);
	}
}
