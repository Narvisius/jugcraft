package io.github.jimbozoomer.jugcraft.tower;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A piece of furniture with its own model (chair, console desk, equipment rack, cable tray): it is placed for
 * the player placing it (whoever uses it looks the way that player looked), and its hitbox follows the model. Boxes are given in pixels for a block facing north and turned for the other facings.
 */
public class FurnitureBlock extends HorizontalDirectionalBlock {
	private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

	/** @param boxes {x0, y0, z0, x1, y1, z1} in pixels, facing north */
	public FurnitureBlock(Properties properties, double[]... boxes) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			VoxelShape shape = Shapes.empty();
			for (double[] box : boxes) {
				shape = Shapes.or(shape, turned(box, facing));
			}
			shapes.put(facing, shape.optimize());
		}
	}

	/** A box turned from north to {@code facing}, the way the blockstate turns the model (clockwise). */
	static VoxelShape turned(double[] b, Direction facing) {
		double x0 = b[0], z0 = b[2], x1 = b[3], z1 = b[5];
		int turns = switch (facing) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
		for (int i = 0; i < turns; i++) {
			double nx0 = 16 - z1, nx1 = 16 - z0, nz0 = x0, nz1 = x1;
			x0 = nx0;
			x1 = nx1;
			z0 = nz0;
			z1 = nz1;
		}
		return Block.box(x0, b[1], z0, x1, Math.min(16, b[4]), z1);
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
	protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
			net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
		if (this != JugcraftTower.BLOCKS.get("operator_chair")) {
			return super.useWithoutItem(state, level, pos, player, hit);
		}
		if (!level.isClientSide()) {
			// The chair's model faces north with its back to the south, so the sitter looks the way the chair faces.
			SeatEntity.sit(level, pos, player, state.getValue(FACING));
		}
		return net.minecraft.world.InteractionResult.SUCCESS;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapes.get(state.getValue(FACING));
	}
}
