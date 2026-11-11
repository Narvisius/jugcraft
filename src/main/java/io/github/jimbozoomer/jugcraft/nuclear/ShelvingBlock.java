package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Document control's shelving (the administration building part 2; tools/admin_rooms.py shelving_cell): a unit four
 * blocks long and two high, placed and broken as one. SEG is a block's place along it (0 the end with the handwheel and
 * the label, toward whoever placed it; on the way it faces), HALF lower or upper. The fixed units stand at a bank's ends;
 * the carriages between them roll (MobileShelvingBlock).
 */
public class ShelvingBlock extends HorizontalDirectionalBlock {
	public static final IntegerProperty SEG = IntegerProperty.create("seg", 0, 3);
	public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
	public static final int SEGS = 4;

	public ShelvingBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SEG, 0).setValue(HALF, DoubleBlockHalf.LOWER));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, SEG, HALF);
	}

	/** The unit's handwheel end, its lower block (where its seg 0 lower stands). */
	public static BlockPos base(BlockState state, BlockPos pos) {
		return pos.relative(state.getValue(FACING).getOpposite(), state.getValue(SEG)).below(state.getValue(HALF) == DoubleBlockHalf.UPPER ? 1 : 0);
	}

	/** The unit's block at seg k, half h (0 lower) from its base. */
	public static BlockPos cell(BlockPos base, Direction facing, int k, int h) {
		return base.relative(facing, k).above(h);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection();
		BlockPos at = context.getClickedPos();
		for (int k = 0; k < SEGS; k++) {
			for (int h = 0; h < 2; h++) {
				if ((k != 0 || h != 0) && !context.getLevel().getBlockState(cell(at, facing, k, h)).canBeReplaced(context)) {
					return null;
				}
			}
		}
		return defaultBlockState().setValue(FACING, facing);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		if (!level.isClientSide()) {
			Direction facing = state.getValue(FACING);
			for (int k = 0; k < SEGS; k++) {
				for (int h = 0; h < 2; h++) {
					if (k != 0 || h != 0) {
						level.setBlock(cell(pos, facing, k, h), state.setValue(SEG, k).setValue(HALF, h == 0 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER),
								Block.UPDATE_ALL);
					}
				}
			}
		}
	}

	/** Broken by a player: the rest of the unit goes with it, dropping nothing (one unit, one item). */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide()) {
			Direction facing = state.getValue(FACING);
			BlockPos base = base(state, pos);
			for (int k = 0; k < SEGS; k++) {
				for (int h = 0; h < 2; h++) {
					BlockPos at = cell(base, facing, k, h);
					if (!at.equals(pos) && level.getBlockState(at).is(this)) {
						level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
					}
				}
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	/** The unit's hitbox in a block (model pixels across, the piece facing north), shifted dx pixels across. */
	protected VoxelShape shape(BlockState state, double dx) {
		boolean upper = state.getValue(HALF) == DoubleBlockHalf.UPPER;
		double x0 = Math.max(0, 0.4 + dx), x1 = Math.min(16, 15.6 + dx);
		if (x1 - x0 < 0.5) {
			return net.minecraft.world.phys.shapes.Shapes.empty();
		}
		return MultiFurnitureBlock.turned(new double[] {x0, 0, 0, x1, upper ? 15.8 : 16, 16}, state.getValue(FACING));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape(state, 0);
	}
}
