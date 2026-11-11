package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A roller door's push-button station (doors round): a grey enclosure on the wall beside the opening, its OPEN,
 * CLOSE and STOP buttons (tools/plant_doors.py station_boxes). OPEN rolls the nearest roller door up, CLOSE lets it
 * down; STOP is for a door in motion (these finish their travel: it only clicks). FACING is the way you look at it
 * (the wall behind it).
 */
public class RollerDoorStationBlock extends HorizontalDirectionalBlock {
	/** How far it looks for its door (blocks, each way; up and down). */
	public static final int REACH = 8, REACH_UP = 6;
	/** The buttons' middles (model px up the block): OPEN, CLOSE, STOP. */
	public static final double[] BUTTONS = {10.6, 8.1, 5.2};
	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	static {
		// Facing north the wall is at z 0 (north), the box stands out of it to z 2.4.
		SHAPES.put(Direction.NORTH, Block.box(5, 3.6, 0, 11, 16, 2.4));
		SHAPES.put(Direction.SOUTH, Block.box(5, 3.6, 13.6, 11, 16, 16));
		SHAPES.put(Direction.WEST, Block.box(0, 3.6, 5, 2.4, 16, 11));
		SHAPES.put(Direction.EAST, Block.box(13.6, 3.6, 5, 16, 16, 11));
	}

	public RollerDoorStationBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		double y = (hit.getLocation().y - pos.getY()) * 16;
		int button = 0;
		for (int k = 1; k < BUTTONS.length; k++) {
			if (Math.abs(y - BUTTONS[k]) < Math.abs(y - BUTTONS[button])) {
				button = k;
			}
		}
		level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.5F, 1.4F);
		BlockPos door = nearestDoor(level, pos);
		if (door == null || button == 2) {
			return InteractionResult.SUCCESS;
		}
		BlockState at = level.getBlockState(door);
		boolean open = at.getValue(RollerDoorBlock.OPEN);
		if (!at.getValue(RollerDoorBlock.MOVING) && open == (button == 1)) {
			RollerDoorBlock.toggle(level, door);
		}
		return InteractionResult.SUCCESS;
	}

	/** The nearest roller door block within reach, or null. */
	public static BlockPos nearestDoor(Level level, BlockPos pos) {
		BlockPos best = null;
		for (BlockPos p : BlockPos.betweenClosed(pos.offset(-REACH, -REACH_UP, -REACH), pos.offset(REACH, REACH_UP, REACH))) {
			if (level.getBlockState(p).getBlock() instanceof RollerDoorBlock && (best == null || p.distSqr(pos) < best.distSqr(pos))) {
				best = p.immutable();
			}
		}
		return best;
	}
}
