package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The tomato on its trellis ({@link TallCrop#TOMATO}; garden crops, tools/garden.py OVERRIPE): a ripe vine left unpicked
 * goes over. On each of its bottom block's random ticks it turns with a 1 in {@value #OVERRIPE_CHANCE} chance, every
 * block of it then {@link #OVERRIPE} and drawn withered. Picking or breaking it then gives as many Rotten Tomatoes
 * ({@value #ROTTEN}) as a ripe vine gives tomatoes, and picking sets it back to its regrowth age, fresh again.
 */
public class TomatoVineBlock extends TallCropBlock {
	public static final BooleanProperty OVERRIPE = BooleanProperty.create("overripe");
	public static final int OVERRIPE_CHANCE = 10;
	public static final String ROTTEN = "rotten_tomato";

	public TomatoVineBlock(Properties properties, TallCrop crop) {
		super(properties, crop);
		this.registerDefaultState(this.defaultBlockState().setValue(OVERRIPE, false));
	}

	public static boolean isOverripe(BlockState state) {
		return state.getValue(OVERRIPE);
	}

	/** A ripe vine's bottom keeps ticking until it goes over. */
	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return super.isRandomlyTicking(state) || state.getValue(SECTION) == 0 && isRipe(state) && !isOverripe(state);
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!isRipe(state)) {
			super.randomTick(state, level, pos, random);
		} else if (!isOverripe(state) && random.nextInt(OVERRIPE_CHANCE) == 0) {
			goOver(level, pos);
		}
	}

	/** Turns every block of the ripe vine whose bottom is {@code bottom} over-ripe. Returns false if it is not ripe. */
	public boolean goOver(Level level, BlockPos bottom) {
		BlockState state = level.getBlockState(bottom);
		if (!state.is(this) || state.getValue(SECTION) != 0 || !isRipe(state)) {
			return false;
		}
		for (int section = crop().height(MAX_AGE) - 1; section >= 0; section--) {
			BlockPos pos = bottom.above(section);
			BlockState part = level.getBlockState(pos);
			if (part.is(this)) {
				level.setBlock(pos, part.setValue(OVERRIPE, true), Block.UPDATE_CLIENTS);
			}
		}
		return true;
	}

	@Override
	protected Item produce(BlockState bottom) {
		return isOverripe(bottom) ? JugcraftAgriculture.item(ROTTEN) : super.produce(bottom);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(OVERRIPE);
	}
}
