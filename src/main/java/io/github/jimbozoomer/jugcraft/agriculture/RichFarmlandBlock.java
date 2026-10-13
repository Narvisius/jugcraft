package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Rich Soil Farmland (the kitchen and cooking expansion's slice 5; tools/soil.py RICH_FARMLAND): Rich Soil tilled with a
 * hoe. Crops take it as farmland (the {@code minecraft:supports_crops} and {@code minecraft:grows_crops} tags), it keeps
 * moist within {@value #WATER_REACH} blocks of water or in the rain as farmland does, and the plant on it gets
 * {@link RichSoilBlock#BOOST} extra random tick for each of its own. Unlike farmland it is never trampled; dry with
 * nothing growing on it, or with a solid block on it, it turns back into Rich Soil, not dirt.
 */
public class RichFarmlandBlock extends Block {
	public static final IntegerProperty MOISTURE = BlockStateProperties.MOISTURE;
	public static final int MAX_MOISTURE = 7;
	/** How far water keeps it moist (and one block up), as vanilla farmland (tools/soil.py WATER_REACH). */
	public static final int WATER_REACH = 4;
	/** Blocks that keep dry farmland from drying out (crops, stems), vanilla's own tag. */
	private static final TagKey<Block> MAINTAINS_FARMLAND = TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace("maintains_farmland"));
	private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 15.0, 16.0);

	public RichFarmlandBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(MOISTURE, 0));
	}

	private static BlockState richSoil() {
		return JugcraftAgriculture.block("rich_soil").defaultBlockState();
	}

	/** Whether water lies within reach, from its own level to one above, as vanilla farmland checks. */
	private static boolean nearWater(LevelReader level, BlockPos pos) {
		for (BlockPos near : BlockPos.betweenClosed(pos.offset(-WATER_REACH, 0, -WATER_REACH), pos.offset(WATER_REACH, 1, WATER_REACH))) {
			if (level.getFluidState(near).is(FluidTags.WATER)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/**
	 * A solid block on it presses it back into Rich Soil, but a fence gate, or a crop that keeps farmland (Jugcraft's
	 * corn stands solid once it is two blocks tall, and is in {@code minecraft:maintains_farmland}), does not.
	 */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockState above = level.getBlockState(pos.above());
		return !above.isSolid() || above.getBlock() instanceof FenceGateBlock || above.is(MAINTAINS_FARMLAND);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		return direction == Direction.UP && !state.canSurvive(level, pos) ? richSoil()
				: super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return true;
	}

	/** Moistens near water or in the rain, else dries a step; dry and bare, it is Rich Soil again. Then boosts its plant. */
	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		int moisture = state.getValue(MOISTURE);
		if (nearWater(level, pos) || level.isRainingAt(pos.above())) {
			if (moisture < MAX_MOISTURE) {
				level.setBlock(pos, state.setValue(MOISTURE, MAX_MOISTURE), Block.UPDATE_CLIENTS);
			}
		} else if (moisture > 0) {
			level.setBlock(pos, state.setValue(MOISTURE, moisture - 1), Block.UPDATE_CLIENTS);
		} else if (!level.getBlockState(pos.above()).is(MAINTAINS_FARMLAND)) {
			level.setBlockAndUpdate(pos, richSoil());
			return;
		}
		RichSoilBlock.boost(level, pos, random);
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(JugcraftAgriculture.item("rich_soil"));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(MOISTURE);
	}
}
