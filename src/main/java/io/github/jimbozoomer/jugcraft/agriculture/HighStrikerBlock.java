package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The High Striker (fall addition 26): a fairground strength test five blocks tall, placed as one. Part 0 is the strike
 * pad on its painted base, parts 1 to 4 the tower with a brass rail up its front, two lamps a part, and a bell on top.
 * Struck with a Carnival Mallet ({@link Midway#strike}), the puck climbs the rail, lighting a lamp at a time to the
 * blow's {@link #LEVEL}; at the top ({@value #RUNG}) it rings the bell. {@code level} is the same on all five parts, so
 * each draws its own lamps and the puck where it is, and lit lamps give light.
 *
 * <p>The animation runs on the base's scheduled ticks with {@link HighStrikerBlockEntity}: up a lamp every
 * {@value #RISE_TICKS} ticks, a rest of {@value #HOLD_TICKS} at the top of the climb, then down a lamp a tick.
 *
 * <p>Breaking any part breaks the whole striker, which drops once (from part 0); in creative nothing drops.
 */
public class HighStrikerBlock extends HorizontalDirectionalBlock implements EntityBlock {
	public static final int PARTS = 5;
	/** Lamps up the tower, two to each of parts 1 to 4. */
	public static final int LEVELS = 8;
	/** The puck at the bell. */
	public static final int RUNG = LEVELS + 1;
	public static final int RISE_TICKS = 2;
	public static final int HOLD_TICKS = 30;
	public static final int LIGHT = 10;
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, PARTS - 1);
	public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, RUNG);
	private static final VoxelShape BASE = Shapes.or(Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0), Block.box(4.0, 4.0, 4.0, 12.0, 16.0, 12.0));
	private static final VoxelShape POST = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
	private static final VoxelShape TOP = Block.box(3.0, 0.0, 3.0, 13.0, 15.0, 13.0);

	public HighStrikerBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, 0).setValue(LEVEL, 0));
	}

	/** How many of the part's two lamps are lit at {@code level} (none on the base). */
	public static int lampsLit(int part, int level) {
		if (part == 0) {
			return 0;
		}
		return Math.clamp(level - 2 * (part - 1), 0, 2);
	}

	/** The light a part gives: {@value #LIGHT} with a lamp lit, more with the bell rung. */
	public static int light(BlockState state) {
		int part = state.getValue(PART);
		int level = state.getValue(LEVEL);
		if (part == PARTS - 1 && level == RUNG) {
			return LIGHT + 3;
		}
		return lampsLit(part, level) > 0 ? LIGHT - 2 + lampsLit(part, level) : 0;
	}

	/** Where its base (part 0) is. */
	public static BlockPos base(BlockPos pos, BlockState state) {
		return pos.below(state.getValue(PART));
	}

	/** Placed facing the player, the four blocks above free and the player allowed to build in each. */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		Level level = context.getLevel();
		BlockPos base = context.getClickedPos();
		Player player = context.getPlayer();
		for (int part = 1; part < PARTS; part++) {
			BlockPos pos = base.above(part);
			if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
					|| !level.getBlockState(pos).canBeReplaced(BlockPlaceContext.at(context, pos, Direction.UP))
					|| (player != null && !level.mayInteract(player, pos))) {
				return null;
			}
		}
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** Raises the tower and the bell above the base. */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.isClientSide()) {
			return;
		}
		for (int part = 1; part < PARTS; part++) {
			level.setBlock(pos.above(part), state.setValue(PART, part), Block.UPDATE_ALL);
		}
	}

	private boolean isPart(Level level, BlockPos pos, int part, Direction facing) {
		BlockState state = level.getBlockState(pos);
		return state.is(this) && state.getValue(PART) == part && state.getValue(FACING) == facing;
	}

	/** Breaking any part breaks the striker: part 0 drops it once; the rest go without drops. */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(this)) {
			return; // The puck moved, not removed.
		}
		BlockPos base = base(pos, state);
		Direction facing = state.getValue(FACING);
		if (state.getValue(PART) != 0) {
			if (isPart(level, base, 0, facing)) {
				level.destroyBlock(base, true);
			}
			return;
		}
		for (int part = 1; part < PARTS; part++) {
			if (isPart(level, base.above(part), part, facing)) {
				level.removeBlock(base.above(part), false);
			}
		}
	}

	/** In creative, breaking any part takes the whole striker away without dropping it. */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && player.getAbilities().instabuild && state.getValue(PART) != 0) {
			BlockPos base = base(pos, state);
			if (level.getBlockState(base).is(this)) {
				level.removeBlock(base, false);
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	/** Sets the puck's level on all five parts. */
	public void setLevel(Level level, BlockPos base, int puck) {
		BlockState baseState = level.getBlockState(base);
		if (!baseState.is(this)) {
			return;
		}
		Direction facing = baseState.getValue(FACING);
		for (int part = 0; part < PARTS; part++) {
			BlockPos pos = base.above(part);
			if (isPart(level, pos, part, facing)) {
				level.setBlock(pos, level.getBlockState(pos).setValue(LEVEL, puck), Block.UPDATE_CLIENTS);
			}
		}
	}

	/** The base's animation step: the puck climbs, rests, rings the bell at the top, and falls back. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(PART) == 0 && level.getBlockEntity(pos) instanceof HighStrikerBlockEntity striker) {
			striker.step(level, state);
		}
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(PART) == 0 ? new HighStrikerBlockEntity(pos, state) : null;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		int part = state.getValue(PART);
		return part == 0 ? BASE : part == PARTS - 1 ? TOP : POST;
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
		builder.add(FACING, PART, LEVEL);
	}
}
