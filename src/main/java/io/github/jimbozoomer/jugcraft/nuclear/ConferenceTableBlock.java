package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.placement.StructureGhost;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
public class ConferenceTableBlock extends Block implements StructureGhost.Source {
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, 8);
	/** Set while the table removes its other parts, so they don't each try to remove the rest again. */
	private static final ThreadLocal<Boolean> CLEARING = ThreadLocal.withInitial(() -> false);
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
			clearOthers(level, pos, state);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	/** The table's other parts go, dropping nothing (the part that was broken drops the table). */
	private void clearOthers(Level level, BlockPos pos, BlockState state) {
		int part = state.getValue(PART);
		BlockPos centre = pos.offset(1 - part % 3, 0, 1 - part / 3);
		CLEARING.set(true);
		try {
			for (int p = 0; p < 9; p++) {
				BlockPos at = centre.offset(p % 3 - 1, 0, p / 3 - 1);
				BlockState there = level.getBlockState(at);
				if (!at.equals(pos) && there.is(this) && there.getValue(PART) == p) {
					level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
				}
			}
		} finally {
			CLEARING.set(false);
		}
	}

	/** However a part goes (an explosion, a command), the rest of the table goes with it: one table, one item. */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (!CLEARING.get() && !level.getBlockState(pos).is(this)) {
			clearOthers(level, pos, state);
		}
	}

	/** The whole table where its item would go (client/MbsPreview.java), the cells in the way marked. */
	@Override
	public StructureGhost ghost(BlockPlaceContext context) {
		BlockPos centre = context.getClickedPos();
		List<StructureGhost.Cell> cells = new ArrayList<>();
		List<BlockPos> blocked = new ArrayList<>();
		for (int part = 0; part < 9; part++) {
			BlockPos cell = centre.offset(part % 3 - 1, 0, part / 3 - 1);
			cells.add(new StructureGhost.Cell(cell, defaultBlockState().setValue(PART, part)));
			boolean free = part == 4 ? context.canPlace() : context.getLevel().getBlockState(cell).canBeReplaced(context);
			if (!free || context.getLevel().isOutsideBuildHeight(cell)) {
				blocked.add(cell);
			}
		}
		return new StructureGhost(cells, blocked);
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
