package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The chemistry lab's flammable liquids and corrosives safety cabinets (tools/admin_rooms.py cabinet_boxes; one block
 * wide, 1.65 m tall): a click swings both doors open (the shelves, the cans and bottles behind them) or shut.
 */
public class SafetyCabinetBlock extends MultiFurnitureBlock {
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

	public SafetyCabinetBlock(Properties properties) {
		super(properties, 1, 2, true, 1, true, false, 10.4);
		registerDefaultState(defaultBlockState().setValue(OPEN, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(OPEN);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		return state == null ? null : state.setValue(OPEN, false);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		boolean top = state.getValue(partProperty()) >= width;
		return turned(new double[] {0.4, 0, 1, 15.6, top ? 10.4 : 16, 9}, state.getValue(FACING));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		Direction facing = state.getValue(FACING);
		BlockPos origin = pos.subtract(offset(facing, state.getValue(partProperty())));
		boolean open = !state.getValue(OPEN);
		for (int p = 0; p < width * rows; p++) {
			BlockPos at = origin.offset(offset(facing, p));
			BlockState s = level.getBlockState(at);
			if (s.is(this)) {
				level.setBlock(at, s.setValue(OPEN, open), Block.UPDATE_ALL);
			}
		}
		level.playSound(null, pos, open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.6F, 1.2F);
		return InteractionResult.SUCCESS_SERVER;
	}
}
