package io.github.jimbozoomer.jugcraft.logistics;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A conveyor slope: carries items one block up (ascending: they leave from the top of the block in
 * front) or down (they arrive from a conveyor one block higher behind it). Use it with an empty hand
 * to switch between up and down. Driven and joined like any conveyor (see {@link ConveyorBlockEntity}).
 */
public class ConveyorSlopeBlock extends ConveyorBlock {
	public static final BooleanProperty ASCENDING = BooleanProperty.create("ascending");
	/** A step: the lower half, plus the upper half on the high side. */
	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	static {
		VoxelShape lower = Block.box(0, 0, 0, 16, 8, 16);
		SHAPES.put(Direction.NORTH, Shapes.or(lower, Block.box(0, 8, 0, 16, 16, 8)));
		SHAPES.put(Direction.SOUTH, Shapes.or(lower, Block.box(0, 8, 8, 16, 16, 16)));
		SHAPES.put(Direction.WEST, Shapes.or(lower, Block.box(0, 8, 0, 8, 16, 16)));
		SHAPES.put(Direction.EAST, Shapes.or(lower, Block.box(8, 8, 0, 16, 16, 16)));
	}

	public ConveyorSlopeBlock(Properties properties) {
		super(properties, false);
		registerDefaultState(defaultBlockState().setValue(ASCENDING, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(ASCENDING);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		Direction facing = state.getValue(FACING);
		return SHAPES.get(state.getValue(ASCENDING) ? facing : facing.getOpposite());
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			boolean ascending = !state.getValue(ASCENDING);
			level.setBlock(pos, state.setValue(ASCENDING, ascending), Block.UPDATE_ALL);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.conveyor_slope",
					Component.translatable("message.jugcraft.conveyor_slope." + (ascending ? "up" : "down"))));
		}
		return InteractionResult.SUCCESS;
	}
}
