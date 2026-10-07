package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A lantern-lit prop of several blocks, placed and broken as one, like the Phantom Pipe Organ: the Haunted Archway and
 * the Dead Hollow Tree. Every block of it is this block, its part ({@link #partProperty}) saying which {@link #cells}
 * it fills, counted to the right of the player who placed it and up from the block they aimed at. Part 0 is the master,
 * which alone drops the prop. Breaking any block breaks it all. Use any block to light or put out its lanterns
 * ({@link #LIT}, on every part).
 */
public abstract class MultiDecorationBlock extends Block {
	public static final int MASTER = 0;
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	protected MultiDecorationBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, true).setValue(partProperty(), MASTER));
	}

	/**
	 * Each part's cell: {blocks to the right of the player who placed it, blocks up}, and for a prop deeper than one
	 * block a third, blocks away from that player; part 0 at {0, 0}.
	 */
	public abstract int[][] cells();

	/** The part property, 0 to the number of cells less one. */
	public abstract IntegerProperty partProperty();

	/**
	 * The cells of a solid prop {@code across} blocks wide, {@code up} tall and {@code deep} deep, in part order: right
	 * first, then up, then away (part = right + across * (up + up-count * away)), as tools/decor17_data.py cells() counts
	 * them for the models.
	 */
	protected static int[][] box(int across, int up, int deep) {
		int[][] cells = new int[across * up * deep][];
		int part = 0;
		for (int away = 0; away < deep; away++) {
			for (int high = 0; high < up; high++) {
				for (int right = 0; right < across; right++) {
					cells[part++] = new int[] {right, high, away};
				}
			}
		}
		return cells;
	}

	public int part(BlockState state) {
		return state.getValue(partProperty());
	}

	/** How many blocks across the prop is. */
	public int across() {
		int across = 1;
		for (int[] cell : cells()) {
			across = Math.max(across, cell[0] + 1);
		}
		return across;
	}

	/** How many blocks deep (away from the player who placed it) the prop is. */
	public int deep() {
		int deep = 1;
		for (int[] cell : cells()) {
			deep = Math.max(deep, cell.length > 2 ? cell[2] + 1 : 1);
		}
		return deep;
	}

	/**
	 * A small prop's box {x0, y0, z0, x1, y1, z1} (pixels in its one block) {@code scale} times bigger in the frame of a prop
	 * {@code across} blocks across and {@code deep} deep, about the small prop's bottom middle (8, 0, 8) set at the middle
	 * of the frame's floor, as tools/decor17_data.py grown() makes its model.
	 */
	protected static double[] grown(double[] box, int scale, int across, int deep) {
		double[] middle = {8.0 * across, 0.0, 8.0 * deep};
		double[] out = new double[6];
		for (int k = 0; k < 6; k++) {
			out[k] = middle[k % 3] + (box[k] - (k % 3 == 1 ? 0.0 : 8.0)) * scale;
		}
		return out;
	}

	/**
	 * Each part's shape, for each facing, of a prop of {@code cells} (as {@link #cells} gives them) whose whole box is
	 * {@code whole} ({x0, y0, z0, x1, y1, z1} in pixels in its frame, facing north: x across to the placer's left from
	 * its far block, y up, z away), cut at the block boundaries: empty for a part the box misses.
	 */
	protected static Map<Direction, VoxelShape[]> partShapes(double[] whole, int[][] cells) {
		int across = 1;
		for (int[] cell : cells) {
			across = Math.max(across, cell[0] + 1);
		}
		Map<Direction, VoxelShape[]> out = new EnumMap<>(Direction.class);
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			VoxelShape[] shapes = new VoxelShape[cells.length];
			for (int part = 0; part < cells.length; part++) {
				int[] cell = cells[part];
				double[] origin = {16.0 * (across - 1 - cell[0]), 16.0 * cell[1], cell.length > 2 ? 16.0 * cell[2] : 0.0};
				double[] box = new double[6];
				boolean empty = false;
				for (int k = 0; k < 3; k++) {
					box[k] = Math.max(0.0, whole[k] - origin[k]);
					box[k + 3] = Math.min(16.0, whole[k + 3] - origin[k]);
					empty |= box[k + 3] <= box[k];
				}
				shapes[part] = empty ? Shapes.empty() : LongDecorationBlock.shape(new double[][] {box}, facing);
			}
			out.put(facing, shapes);
		}
		return out;
	}

	/** Where part {@code part} of the prop with its master at {@code master}, facing {@code facing}, is. */
	public BlockPos partPos(BlockPos master, Direction facing, int part) {
		int[] cell = cells()[part];
		BlockPos at = master.relative(facing.getCounterClockWise(), cell[0]).above(cell[1]);
		return cell.length > 2 ? at.relative(facing.getOpposite(), cell[2]) : at;
	}

	/** Where the master of the prop this block belongs to is. */
	public BlockPos masterPos(BlockPos pos, BlockState state) {
		int[] cell = cells()[part(state)];
		BlockPos at = pos.relative(state.getValue(FACING).getCounterClockWise(), -cell[0]).below(cell[1]);
		return cell.length > 2 ? at.relative(state.getValue(FACING), cell[2]) : at;
	}

	/** Placed facing the player, from the block they aimed at to their right and up: every block it needs must be free. */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection().getOpposite();
		Level level = context.getLevel();
		BlockPos master = context.getClickedPos();
		for (int part = 1; part < cells().length; part++) {
			BlockPos pos = partPos(master, facing, part);
			if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
					|| !level.getBlockState(pos).canBeReplaced(BlockPlaceContext.at(context, pos, Direction.UP))) {
				return null;
			}
		}
		return defaultBlockState().setValue(FACING, facing).setValue(partProperty(), MASTER);
	}

	/** Fills in the rest of the prop. */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.isClientSide()) {
			return;
		}
		Direction facing = state.getValue(FACING);
		for (int part = 1; part < cells().length; part++) {
			level.setBlock(partPos(pos, facing, part), state.setValue(partProperty(), part), Block.UPDATE_ALL);
		}
	}

	/** Whether the block at {@code pos} is part {@code part} of a prop of this kind facing {@code facing}. */
	private boolean isPart(Level level, BlockPos pos, int part, Direction facing) {
		BlockState state = level.getBlockState(pos);
		return state.is(this) && part(state) == part && state.getValue(FACING) == facing;
	}

	/** Breaking any block breaks the prop: the master drops it once, the rest go without drops. */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(this)) {
			return; // Lit or put out, not removed.
		}
		BlockPos master = masterPos(pos, state);
		Direction facing = state.getValue(FACING);
		if (part(state) != MASTER) {
			if (isPart(level, master, MASTER, facing)) {
				level.destroyBlock(master, true);
			}
			return;
		}
		for (int part = 1; part < cells().length; part++) {
			BlockPos partPos = partPos(master, facing, part);
			if (isPart(level, partPos, part, facing)) {
				level.removeBlock(partPos, false);
			}
		}
	}

	/** In creative, breaking any block takes the whole prop away without dropping it. */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && player.getAbilities().instabuild && part(state) != MASTER) {
			BlockPos master = masterPos(pos, state);
			if (level.getBlockState(master).is(this)) {
				level.removeBlock(master, false);
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	/** Lights or puts out its lanterns, on every part. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			BlockPos master = masterPos(pos, state);
			Direction facing = state.getValue(FACING);
			if (!isPart(level, master, MASTER, facing)) {
				return InteractionResult.PASS;
			}
			boolean lit = !level.getBlockState(master).getValue(LIT);
			for (int part = 0; part < cells().length; part++) {
				BlockPos partPos = partPos(master, facing, part);
				if (isPart(level, partPos, part, facing)) {
					level.setBlock(partPos, level.getBlockState(partPos).setValue(LIT, lit), Block.UPDATE_ALL);
				}
			}
			level.playSound(null, pos, lit ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.0F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LIT, partProperty());
	}
}
