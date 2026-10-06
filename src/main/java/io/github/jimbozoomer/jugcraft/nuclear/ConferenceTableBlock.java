package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A round meeting table three blocks across, placed and broken as one: PART 0..8 is the block's place in the 3 x 3
 * (row by row from the north-west corner, 4 the middle with the pedestal). Placing it needs the 3 x 3 round the
 * clicked block free.
 */
public class ConferenceTableBlock extends Block {
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, 8);
	private static final VoxelShape[] SHAPES = new VoxelShape[9];

	static {
		// The top's slice over each block (a disc of radius 23 px round the middle block's centre), the pedestal.
		for (int part = 0; part < 9; part++) {
			int bx = part % 3, bz = part / 3;
			VoxelShape shape = Shapes.empty();
			for (int z = 0; z < 16; z += 2) {
				double gz = bz * 16 + z + 1 - 24;
				double half = Math.sqrt(Math.max(0, 23 * 23 - gz * gz));
				double x0 = Math.max(0, 24 - half - bx * 16), x1 = Math.min(16, 24 + half - bx * 16);
				if (x1 - x0 > 0.5) {
					shape = Shapes.or(shape, Block.box(x0, 11.5, z, x1, 13, z + 2));
				}
			}
			if (part == 4) {
				shape = Shapes.or(shape, Block.box(5, 0, 5, 11, 11.5, 11));
			}
			SHAPES[part] = shape.isEmpty() ? Shapes.empty() : shape.optimize();
		}
	}

	public ConferenceTableBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(PART, 4));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PART);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos centre = context.getClickedPos();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if ((dx != 0 || dz != 0) && !context.getLevel().getBlockState(centre.offset(dx, 0, dz)).canBeReplaced(context)) {
					return null;
				}
			}
		}
		return defaultBlockState();
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		if (!level.isClientSide()) {
			for (int part = 0; part < 9; part++) {
				if (part != 4) {
					level.setBlock(pos.offset(part % 3 - 1, 0, part / 3 - 1), state.setValue(PART, part), Block.UPDATE_ALL);
				}
			}
		}
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide()) {
			int part = state.getValue(PART);
			BlockPos centre = pos.offset(1 - part % 3, 0, 1 - part / 3);
			for (int p = 0; p < 9; p++) {
				BlockPos at = centre.offset(p % 3 - 1, 0, p / 3 - 1);
				if (!at.equals(pos) && level.getBlockState(at).is(this)) {
					level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
				}
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	/** Turned with the structure it is part of: each block takes the place its slice turns to. */
	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		int part = state.getValue(PART), bx = part % 3, bz = part / 3;
		for (int i = 0; i < switch (rotation) {
			case CLOCKWISE_90 -> 1;
			case CLOCKWISE_180 -> 2;
			case COUNTERCLOCKWISE_90 -> 3;
			default -> 0;
		}; i++) {
			int nbx = 2 - bz, nbz = bx;
			bx = nbx;
			bz = nbz;
		}
		return state.setValue(PART, bz * 3 + bx);
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		int part = state.getValue(PART), bx = part % 3, bz = part / 3;
		return switch (mirror) {
			case FRONT_BACK -> state.setValue(PART, bz * 3 + 2 - bx);
			case LEFT_RIGHT -> state.setValue(PART, (2 - bz) * 3 + bx);
			default -> state;
		};
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[state.getValue(PART)];
	}
}
