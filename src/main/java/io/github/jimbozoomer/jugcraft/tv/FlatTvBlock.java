package io.github.jimbozoomer.jugcraft.tv;

import io.github.jimbozoomer.jugcraft.tower.FurnitureBlock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The flat screen TV (docs/features/flat-screen-tv.md): one block of screen, 5 cm deep. Blocks placed side by side and
 * one above another on the same wall join into one screen up to 48 x 27 ({@link TvLayout}): the bezel only round the
 * outside, the chin with the logo and the power light along the bottom. With a wall behind it the panel hangs on it;
 * with nothing behind, it stands in the middle of its block on a neck and foot. FACING is the way its back faces (the
 * way the player placing it looked); LEFT, RIGHT, UP and DOWN say the screen carries on that way (left and right as you
 * look at it), STAND that it stands. Right-click: the SCREEN window (client/tv/TvScreen.java), sent by the server.
 */
public class FlatTvBlock extends HorizontalDirectionalBlock implements EntityBlock {
	public static final BooleanProperty LEFT = BooleanProperty.create("left");
	public static final BooleanProperty RIGHT = BooleanProperty.create("right");
	public static final BooleanProperty UP = BooleanProperty.create("up");
	public static final BooleanProperty DOWN = BooleanProperty.create("down");
	public static final BooleanProperty STAND = BooleanProperty.create("stand");
	/** The panel (px, facing north, its back to the north): hung, its back on the wall; standing, mid-block, lifted on its foot. */
	public static final double WALL_FRONT = 0.92, STAND_BACK = 7.6, STAND_FRONT = 8.52, LIFT = 1.6;
	/** The most blocks one join looks at (a whole wall of TVs is split into several screens). */
	private static final int MAX_GROUP = 4096;
	/**
	 * How far (blocks, to the screen's nearest part) a player may right-click a screen and work its controls: a big
	 * screen is watched from well back (client/tv/TvFarUse opens it from there; the game's own reach is 5).
	 */
	public static final double FAR_REACH = 48;
	/** The window stays open, and its controls are taken, this much further out (walking about while it's open). */
	public static final double WINDOW_SLACK = 8;

	private final Map<Direction, VoxelShape[]> shapes = new EnumMap<>(Direction.class);

	public FlatTvBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LEFT, false).setValue(RIGHT, false)
				.setValue(UP, false).setValue(DOWN, false).setValue(STAND, false));
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			// [stand][lifted]: the bottom row of a standing screen starts on its foot.
			VoxelShape[] s = new VoxelShape[4];
			s[0] = FurnitureBlock.turned(new double[] {0, 0, 0, 16, 16, WALL_FRONT}, facing);
			s[1] = s[0];
			s[2] = FurnitureBlock.turned(new double[] {0, 0, STAND_BACK, 16, 16, STAND_FRONT}, facing);
			s[3] = FurnitureBlock.turned(new double[] {0, 0, STAND_BACK - 1.6, 16, 16, STAND_FRONT + 1.6}, facing);
			shapes.put(facing, s);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LEFT, RIGHT, UP, DOWN, STAND);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		boolean stand = state.getValue(STAND);
		return shapes.get(state.getValue(FACING))[(stand ? 2 : 0) + (stand && !state.getValue(DOWN) ? 1 : 0)];
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TvBlockEntity(pos, state);
	}

	// ------------------------------------------------------------------------------------------------- joining

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (level instanceof ServerLevel server && !oldState.is(this)) {
			rejoin(server, pos, state.getValue(FACING));
		}
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide() && placer instanceof Player && level.getBlockEntity(pos) instanceof TvBlockEntity tv) {
			tv.setPlacer(placer.getUUID());
			// The screen it joined keeps its owner; a new one is the placer's.
			BlockPos c = controllerOf(level, pos, state);
			if (level.getBlockEntity(c) instanceof TvBlockEntity ctrl && ctrl.owner() == null) {
				ctrl.setPlacer(ctrl.placer() != null ? ctrl.placer() : placer.getUUID());
			}
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		Direction facing = state.getValue(FACING);
		Set<BlockPos> done = new HashSet<>();
		for (Direction d : plane(facing)) {
			BlockPos n = pos.relative(d);
			BlockState s = level.getBlockState(n);
			if (!done.contains(n) && s.is(this) && s.getValue(FACING) == facing) {
				done.addAll(rejoin(level, n, facing));
			}
		}
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		// What's behind it decides whether the screen hangs or stands: look again next tick.
		if (direction == state.getValue(FACING)) {
			ticks.scheduleTick(pos, this, 1);
		}
		return state;
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		rejoin(level, pos, state.getValue(FACING));
	}

	private static Direction[] plane(Direction facing) {
		return new Direction[] {facing.getClockWise(), facing.getCounterClockWise(), Direction.UP, Direction.DOWN};
	}

	/** Whether a hung panel has something to hang on behind the block at {@code pos}. */
	private static boolean wallBehind(Level level, BlockPos pos, Direction facing) {
		BlockPos b = pos.relative(facing);
		BlockState s = level.getBlockState(b);
		return !s.isAir() && !s.getCollisionShape(level, b).isEmpty();
	}

	/**
	 * Joins up the TV blocks on the wall around {@code start} (same facing, side by side and stacked) into screens, sets
	 * each block's edges and stand, and carries what the old screens played over to the new ones (the biggest one
	 * playing something wins). Returns the blocks it looked at.
	 */
	public static Set<BlockPos> rejoin(ServerLevel level, BlockPos start, Direction facing) {
		Block tv = level.getBlockState(start).getBlock();
		Direction right = facing.getClockWise();
		Map<TvLayout.Cell, BlockPos> cells = new HashMap<>();
		Deque<BlockPos> queue = new ArrayDeque<>();
		queue.add(start);
		Set<BlockPos> seen = new HashSet<>();
		seen.add(start);
		while (!queue.isEmpty() && cells.size() < MAX_GROUP) {
			BlockPos p = queue.poll();
			BlockState s = level.getBlockState(p);
			if (!s.is(tv) || s.getValue(FACING) != facing) {
				continue;
			}
			BlockPos d = p.subtract(start);
			cells.put(new TvLayout.Cell(d.getX() * right.getStepX() + d.getZ() * right.getStepZ(), d.getY()), p);
			for (Direction dir : plane(facing)) {
				BlockPos n = p.relative(dir);
				if (seen.add(n)) {
					queue.add(n);
				}
			}
		}
		if (cells.isEmpty()) {
			return Set.of();
		}
		// What the screens there played, and where.
		Map<BlockPos, TvBlockEntity.Snapshot> before = new HashMap<>();
		List<TvBlockEntity> blocks = new ArrayList<>();
		for (BlockPos p : cells.values()) {
			if (level.getBlockEntity(p) instanceof TvBlockEntity be) {
				blocks.add(be);
				TvBlockEntity.Snapshot s = be.takeSnapshot();
				if (s != null) {
					before.put(p, s);
				}
			}
		}
		Set<TvBlockEntity> controllers = new HashSet<>();
		for (TvLayout.Screen screen : TvLayout.tile(cells.keySet())) {
			boolean wall = false;
			List<BlockPos> mine = new ArrayList<>();
			for (int dc = 0; dc < screen.w(); dc++) {
				for (int dr = 0; dr < screen.h(); dr++) {
					BlockPos p = cells.get(new TvLayout.Cell(screen.c0() + dc, screen.r0() + dr));
					mine.add(p);
					wall |= wallBehind(level, p, facing);
				}
			}
			for (BlockPos p : mine) {
				BlockPos d = p.subtract(start);
				int c = d.getX() * right.getStepX() + d.getZ() * right.getStepZ() - screen.c0(), r = d.getY() - screen.r0();
				BlockState old = level.getBlockState(p);
				BlockState now = old.setValue(LEFT, c > 0).setValue(RIGHT, c < screen.w() - 1).setValue(DOWN, r > 0)
						.setValue(UP, r < screen.h() - 1).setValue(STAND, !wall);
				if (now != old) {
					level.setBlock(p, now, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
				}
			}
			TvBlockEntity.Snapshot best = null;
			for (Map.Entry<BlockPos, TvBlockEntity.Snapshot> e : before.entrySet()) {
				BlockPos d = e.getKey().subtract(start);
				TvLayout.Cell at = new TvLayout.Cell(d.getX() * right.getStepX() + d.getZ() * right.getStepZ(), d.getY());
				if (screen.contains(at) && better(e.getValue(), best)) {
					best = e.getValue();
				}
			}
			if (level.getBlockEntity(cells.get(screen.controller())) instanceof TvBlockEntity ctrl) {
				ctrl.adopt(best, screen.w(), screen.h());
				controllers.add(ctrl);
			}
		}
		for (TvBlockEntity be : blocks) {
			if (!controllers.contains(be)) {
				be.release();
			}
			be.sync();
		}
		return new HashSet<>(cells.values());
	}

	private static boolean better(TvBlockEntity.Snapshot a, TvBlockEntity.@Nullable Snapshot b) {
		if (b == null) {
			return true;
		}
		if (a.url().isEmpty() != b.url().isEmpty()) {
			return !a.url().isEmpty();
		}
		return a.area() > b.area();
	}

	/** The bottom-left block of the screen the block at {@code pos} is part of (by its edges). */
	public static BlockPos controllerOf(BlockGetter level, BlockPos pos, BlockState state) {
		Direction left = state.getValue(FACING).getCounterClockWise();
		BlockPos p = pos;
		BlockState s = state;
		for (int k = 0; k < TvLayout.MAX_W && s.getBlock() instanceof FlatTvBlock && s.getValue(LEFT); k++) {
			p = p.relative(left);
			s = level.getBlockState(p);
		}
		for (int k = 0; k < TvLayout.MAX_H && s.getBlock() instanceof FlatTvBlock && s.getValue(DOWN); k++) {
			p = p.below();
			s = level.getBlockState(p);
		}
		return p;
	}

	/** The screen's size in blocks from its controller (by the blocks' edges): {width, height}. */
	public static int[] sizeFrom(BlockGetter level, BlockPos controller, BlockState state) {
		Direction right = state.getValue(FACING).getClockWise();
		int w = 1, h = 1;
		BlockState s = state;
		while (w < TvLayout.MAX_W && s.getValue(RIGHT)) {
			s = level.getBlockState(controller.relative(right, w));
			if (!(s.getBlock() instanceof FlatTvBlock)) {
				break;
			}
			w++;
		}
		s = state;
		while (h < TvLayout.MAX_H && s.getValue(UP)) {
			s = level.getBlockState(controller.above(h));
			if (!(s.getBlock() instanceof FlatTvBlock)) {
				break;
			}
			h++;
		}
		return new int[] {w, h};
	}

	// ------------------------------------------------------------------------------------------------- using it

	/** A Sync Key is used on the screen (it sets the key to it: tv/SyncKeyItem), not the screen's window opened. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
		return stack.getItem() instanceof SyncKeyItem ? InteractionResult.PASS : super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
			openFor(server, sp, pos, state);
		}
		return InteractionResult.SUCCESS;
	}

	/** Sends {@code player} the SCREEN window of the screen the block at {@code pos} is part of (joining it up first if need be). */
	static void openFor(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
		BlockPos c = controllerOf(level, pos, state);
		if (!(level.getBlockEntity(c) instanceof TvBlockEntity ctrl) || !ctrl.isController()) {
			// Not joined up yet (a screen from before TVs joined, or set down without its neighbours told).
			rejoin(level, pos, state.getValue(FACING));
			c = controllerOf(level, pos, level.getBlockState(pos));
		}
		if (level.getBlockEntity(c) instanceof TvBlockEntity ctrl && ctrl.isController()) {
			JugcraftTv.open(player, ctrl);
		}
	}

	/**
	 * How far {@code p} is from the nearest part of the w x h screen whose bottom-left block is {@code controller} (0
	 * inside its blocks): the screen runs right (as you look at it) and up from there.
	 */
	public static double screenDistance(BlockPos controller, Direction facing, int w, int h, Vec3 p) {
		Direction right = facing.getClockWise();
		BlockPos far = controller.relative(right, Math.max(0, w - 1)).above(Math.max(0, h - 1));
		double x0 = Math.min(controller.getX(), far.getX()), x1 = Math.max(controller.getX(), far.getX()) + 1;
		double y0 = controller.getY(), y1 = far.getY() + 1;
		double z0 = Math.min(controller.getZ(), far.getZ()), z1 = Math.max(controller.getZ(), far.getZ()) + 1;
		double dx = Math.max(0, Math.max(x0 - p.x, p.x - x1));
		double dy = Math.max(0, Math.max(y0 - p.y, p.y - y1));
		double dz = Math.max(0, Math.max(z0 - p.z, p.z - z1));
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}
}
