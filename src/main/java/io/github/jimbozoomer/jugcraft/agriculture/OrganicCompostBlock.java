package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Organic Compost (the kitchen and cooking expansion's slice 5; tools/soil.py COMPOST): dirt, straw, bone meal and
 * rotten flesh left to rot, in the owner's four stages. Each random tick turns it a stage with a 1 in
 * {@value #TURN_CHANCE} chance, or surely while water touches it; after its last stage it is {@link RichSoilBlock}. A
 * comparator reads how far it has gone.
 */
public class OrganicCompostBlock extends Block {
	public static final int LAST_STAGE = 3;
	public static final IntegerProperty COMPOSTING = IntegerProperty.create("composting", 0, LAST_STAGE);
	/** A random tick turns dry compost a stage one time in this many (tools/soil.py COMPOST turn_chance). */
	public static final int TURN_CHANCE = 2;

	public OrganicCompostBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(COMPOSTING, 0));
	}

	/** Whether water touches any side of it. */
	private static boolean wet(ServerLevel level, BlockPos pos) {
		for (Direction side : Direction.values()) {
			if (level.getFluidState(pos.relative(side)).is(FluidTags.WATER)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return true;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!wet(level, pos) && random.nextInt(TURN_CHANCE) != 0) {
			return;
		}
		int stage = state.getValue(COMPOSTING);
		level.setBlockAndUpdate(pos, stage < LAST_STAGE ? state.setValue(COMPOSTING, stage + 1)
				: JugcraftAgriculture.block("rich_soil").defaultBlockState());
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	/** The stage it has reached: 4, 8, 12 or 15. */
	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return Math.min(15, (state.getValue(COMPOSTING) + 1) * 4);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(COMPOSTING);
	}
}
