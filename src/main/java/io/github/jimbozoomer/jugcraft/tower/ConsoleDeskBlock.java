package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneDepots;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Console Desk (remade for the Tri-Pillar's work list item 14): placed side by side, facing the same way, desks join
 * into one long bench desk (tools/ops_desk.py: one top, a modesty panel and cable tray down the back, end panels at the
 * outer ends only), each block carrying a monitor, the end blocks' turned 22.5 degrees in toward the chair, one keyboard
 * and mouse on the middle block. {@link #LEFT} and {@link #RIGHT}: a desk of the same facing beside it on its user's
 * left or right. {@link #ROLE}: which of the {@link #ROLES} its row works; the left, middle and right monitors show that
 * role's three pages (client/ConsoleDeskRenderer), live from the tower terminal it is pinned to (tower construction pins
 * it; there is no server ticker). Using it opens the depot screen; sneaking and using it with an empty hand turns its whole
 * row to the next role.
 */
public final class ConsoleDeskBlock extends FurnitureBlock implements EntityBlock, DepotDisplayBlockEntity.Display {
	public static final BooleanProperty LEFT = BooleanProperty.create("left");
	public static final BooleanProperty RIGHT = BooleanProperty.create("right");
	/** The positions a desk can work, as the Tri-Pillar's hall seats them (tools/tower_tripillar.py DESK_ROLES). */
	public static final String[] ROLES = {"FLIGHT DIRECTOR", "AIRSPACE", "FLEET", "DISPATCH", "MISSIONS", "TOWER BUILD", "POWER", "SUPPLY",
			"DOCKS", "MAINTENANCE", "DATA HALL", "COMMS", "RECEPTION", "SECURITY", "MAP TABLE"};
	public static final IntegerProperty ROLE = IntegerProperty.create("role", 0, ROLES.length - 1);
	/** The longest row a role change walks along (each way). */
	static final int ROW = 16;
	/** The monitors' turn at the ends of a row (degrees about +y, as tools/ops_desk.py TILT), and its pivot (px, facing north). */
	public static final float TILT = 22.5F;
	public static final float[] PIVOT = {8F, 19.5F, 2.8F};
	/** The monitor's glass, the face the page is drawn on (px, facing north, before its turn): x0, y0, x1, y1, z. */
	public static final float[] GLASS = {1.2F, 15.5F, 14.8F, 23.75F, 3.7F};
	/** Client callback keeps the clicked desk alongside the target terminal. */
	public static java.util.function.BiConsumer<BlockPos, BlockPos> openScreen = (terminal, access) -> { };
	/** The hitboxes: facing x left x right. */
	private static final VoxelShape[] SHAPES = new VoxelShape[16];

	static {
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			for (int joins = 0; joins < 4; joins++) {
				boolean left = (joins & 2) != 0, right = (joins & 1) != 0;
				VoxelShape shape = Shapes.empty();
				for (double[] box : boxes(left, right)) {
					shape = Shapes.or(shape, turned(box, facing));
				}
				SHAPES[facing.get2DDataValue() * 4 + joins] = shape.optimize();
			}
		}
	}

	public ConsoleDeskBlock(Properties properties) {
		super(properties, boxes(false, false).toArray(new double[0][]));
		registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false).setValue(ROLE, 0));
	}

	/** The desk's boxes facing north (px): its top and frame, the monitor on its stand (turned at the row's ends). */
	static List<double[]> boxes(boolean left, boolean right) {
		List<double[]> out = new ArrayList<>();
		out.add(new double[] {left ? 0 : 0.4, 0, 1.2, right ? 16 : 15.6, 11.3, 15.1});
		float angle = angle(left, right);
		out.add(yawed(5.6, 10.4, 1.4, 5.0, 11.2, 11.45, angle, 1)[0]);
		out.add(yawed(7.3, 8.7, 2.0, 2.7, 11.45, 14.9, angle, 1)[0]);
		for (double[] b : yawed(0.8, 15.2, 2.2, 3.7, 14.9, 24.25, angle, 4)) {
			out.add(b);
		}
		return out;
	}

	/** The monitor's turn for a desk joined on its user's left / right: in toward the middle at the row's ends. */
	public static float angle(boolean left, boolean right) {
		return right && !left ? TILT : left && !right ? -TILT : 0;
	}

	/** Which of its role's three pages a desk's monitor shows: the left end the first, the right end the last. */
	public static int page(BlockState state) {
		boolean left = state.getValue(LEFT), right = state.getValue(RIGHT);
		return right && !left ? 0 : left && !right ? 2 : 1;
	}

	/** A box turned about the monitor's pivot, as {@code pieces} boxes along x each round its turned corners. */
	private static double[][] yawed(double x0, double x1, double z0, double z1, double y0, double y1, float angle, int pieces) {
		double a = Math.toRadians(angle), c = Math.cos(a), s = Math.sin(a);
		double[][] out = new double[pieces][];
		for (int k = 0; k < pieces; k++) {
			double px0 = x0 + (x1 - x0) * k / pieces, px1 = x0 + (x1 - x0) * (k + 1) / pieces;
			double minX = 16, maxX = 0, minZ = 16, maxZ = 0;
			for (double x : new double[] {px0, px1}) {
				for (double z : new double[] {z0, z1}) {
					double cx = x - PIVOT[0], cz = z - PIVOT[2];
					double rx = PIVOT[0] + cx * c + cz * s, rz = PIVOT[2] - cx * s + cz * c;
					minX = Math.min(minX, rx);
					maxX = Math.max(maxX, rx);
					minZ = Math.min(minZ, rz);
					maxZ = Math.max(maxZ, rz);
				}
			}
			out[k] = new double[] {Math.max(0, minX), y0, Math.max(0, minZ), Math.min(16, maxX), y1, Math.min(16, maxZ)};
		}
		return out;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(LEFT, RIGHT, ROLE);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[state.getValue(FACING).get2DDataValue() * 4 + (state.getValue(LEFT) ? 2 : 0) + (state.getValue(RIGHT) ? 1 : 0)];
	}

	/** Its user's left and right, for a desk facing {@code facing} (they sit behind it looking that way). */
	public static Direction leftOf(Direction facing) {
		return facing.getCounterClockWise();
	}

	private static boolean same(BlockState state, Direction facing) {
		return state.getBlock() instanceof ConsoleDeskBlock && state.getValue(FACING) == facing;
	}

	/** The state with its joins to the desks beside it worked out (its role kept). */
	public static BlockState joined(BlockState state, BlockGetter level, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		return state.setValue(LEFT, same(level.getBlockState(pos.relative(leftOf(facing))), facing))
				.setValue(RIGHT, same(level.getBlockState(pos.relative(leftOf(facing).getOpposite())), facing));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = joined(super.getStateForPlacement(context), context.getLevel(), context.getClickedPos());
		Direction facing = state.getValue(FACING);
		// a desk added to a row works what the row works
		for (Direction side : new Direction[] {leftOf(facing), leftOf(facing).getOpposite()}) {
			BlockState beside = context.getLevel().getBlockState(context.getClickedPos().relative(side));
			if (same(beside, facing)) {
				return state.setValue(ROLE, beside.getValue(ROLE));
			}
		}
		return state;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		return direction.getAxis() == state.getValue(FACING).getClockWise().getAxis() ? joined(state, level, pos) : state;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		// Set down with its joins given (the tower places each desk as its layout has it), it still looks at what is beside
		// it now, a tick later: the rest of its row may not be built yet, or an older layout may have given no joins at all.
		if (!level.isClientSide() && !oldState.is(this) && joined(state, level, pos) != state) {
			level.scheduleTick(pos, this, 1);
		}
	}

	@Override
	protected void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, RandomSource random) {
		BlockState joined = joined(state, level, pos);
		if (joined != state) {
			level.setBlock(pos, joined, Block.UPDATE_CLIENTS);
		}
	}

	/** "FLIGHT DIRECTOR" as "Flight Director". */
	static String title(String name) {
		StringBuilder out = new StringBuilder();
		for (String word : name.split(" ")) {
			out.append(out.isEmpty() ? "" : " ").append(word.charAt(0)).append(word.substring(1).toLowerCase(java.util.Locale.ROOT));
		}
		return out.toString();
	}

	/** The desks of the row {@code pos} is in (itself first), along its user's left and right. */
	public static List<BlockPos> row(BlockGetter level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		List<BlockPos> out = new ArrayList<>(List.of(pos));
		if (!(state.getBlock() instanceof ConsoleDeskBlock)) {
			return out;
		}
		Direction facing = state.getValue(FACING);
		for (Direction side : new Direction[] {leftOf(facing), leftOf(facing).getOpposite()}) {
			BlockPos at = pos.relative(side);
			for (int i = 0; i < ROW && same(level.getBlockState(at), facing); i++) {
				out.add(at);
				at = at.relative(side);
			}
		}
		return out;
	}

	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DepotDisplayBlockEntity(JugcraftTower.CONSOLE_ENTITY, pos, state);
	}
	@Override public boolean isAnchor(BlockState state) { return true; }
	@Override public int linkRange() { return 24; }

	@Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DepotDisplayBlockEntity display) {
			var nearest = DroneDepots.nearest(level, pos, linkRange());
			if (nearest != null) display.pin(nearest.getBlockPos());
		}
	}

	@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player.isSecondaryUseActive()) {
			// sneaking with nothing in hand: the whole row turns to the next role
			if (!level.isClientSide()) {
				int role = (state.getValue(ROLE) + 1) % ROLES.length;
				for (BlockPos at : row(level, pos)) {
					BlockState desk = level.getBlockState(at);
					level.setBlock(at, desk.setValue(ROLE, role), Block.UPDATE_CLIENTS);
				}
				player.sendOverlayMessage(Component.translatable("message.jugcraft.console_desk.role", title(ROLES[role])));
			}
			return InteractionResult.SUCCESS;
		}
		if (level.getBlockEntity(pos) instanceof DepotDisplayBlockEntity display && display.terminal() != null
				&& level.getBlockEntity(display.terminal()) instanceof DroneTerminalBlockEntity terminal) {
			if (level.isClientSide()) openScreen.accept(display.terminal(), pos);
			else {
				// Match direct terminal use: imported, unowned depots belong to their first operator.
				if (terminal.owner() == null) {
					terminal.setOwner(player.getUUID());
					player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.jugcraft.drone.claimed"));
				}
				terminal.rescanNow(); terminal.refreshClients();
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** A supplied access position grants only a nearby real console's exact loaded terminal link. */
	public static boolean mayAccessTerminal(ServerPlayer player, BlockPos terminalPos, BlockPos accessPos) {
		var level = player.level();
		if (!level.isLoaded(terminalPos) || !(level.getBlockEntity(terminalPos) instanceof DroneTerminalBlockEntity)
				|| !level.isLoaded(accessPos) || player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(accessPos)) > 8 * 8) return false;
		if (terminalPos.equals(accessPos)) return true;
		var state = level.getBlockState(accessPos);
		if (state.getBlock() instanceof MasterConsoleBlock console) {
			var display = console.display(level, accessPos, state);
			return display != null && display.reaches(terminalPos, console.linkRange()) && terminalPos.equals(display.terminal());
		}
		if (state.getBlock() instanceof TowerControlConsoleBlock console) {
			var display = console.display(level, accessPos, state);
			return display != null && display.reaches(terminalPos, console.linkRange()) && terminalPos.equals(display.terminal());
		}
		if (state.getBlock() instanceof HoloCommandTableBlock table) {
			// The giant holo table's touch stations work like the touch consoles did.
			var display = table.display(level, accessPos, state);
			return display != null && display.reaches(terminalPos, table.linkRange()) && terminalPos.equals(display.terminal());
		}
		if (state.getBlock() instanceof HallFixtureBlock fixture) {
			// A Tri-Pillar touch console works like a desk.
			return fixture.interactive() && level.getBlockEntity(accessPos) instanceof DepotDisplayBlockEntity display
					&& display.reaches(terminalPos, fixture.linkRange()) && terminalPos.equals(display.terminal());
		}
		return level.getBlockState(accessPos).getBlock() instanceof ConsoleDeskBlock desk
				&& level.getBlockEntity(accessPos) instanceof DepotDisplayBlockEntity display
				&& display.reaches(terminalPos, desk.linkRange()) && terminalPos.equals(display.terminal());
	}
}
