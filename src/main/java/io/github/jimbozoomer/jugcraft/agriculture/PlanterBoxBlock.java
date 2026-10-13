package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Planter Box (garden crops, herbs and spices, part b; tools/herbs.py PLANTER): a wooden box of soil that holds its own
 * water. Crops take it as moist farmland wherever it stands (the {@code minecraft:supports_crops} and
 * {@code minecraft:grows_crops} tags, and {@link #MOISTURE} always {@value #MOISTURE_LEVEL}), on a balcony, a roof or
 * indoors. It is never trampled and never dries, so it takes the place of farmland and a water source, and no more: a
 * crop on it grows as fast as on watered farmland.
 */
public class PlanterBoxBlock extends Block {
	public static final IntegerProperty MOISTURE = BlockStateProperties.MOISTURE;
	/** The moisture crops read (CropGrowth): always wet. */
	public static final int MOISTURE_LEVEL = 7;
	/** As tall as farmland, so a crop stands in its soil as it does on a field. */
	private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 15.0, 16.0);

	public PlanterBoxBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(MOISTURE, MOISTURE_LEVEL));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(MOISTURE);
	}
}
