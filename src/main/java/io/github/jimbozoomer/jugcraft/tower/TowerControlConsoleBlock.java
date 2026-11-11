package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneDepots;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.Footprint;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Tower Control Console: the Tri-Pillar's master console redone (work list item 6, approved 22 October 2026). A
 * curved three-position control-room console (FLEET + JOBS, TOWER CONTROL, POWER + SUPPLY) on a band of carpeted
 * raised floor whose curved front step follows the Holo Command Table; tools/tower_control_console.py describes it.
 *
 * <p>One item places it whole. Its parts are the cells of that floor band ({@link #FOOTPRINT}, written facing north:
 * the operators sit to the north and look south over their monitors; the arc's axis is 9 cells south of the master,
 * which is the cell under the TOWER CONTROL position). Facing points towards the operators, so a player placing it
 * from where they would sit gets it the right way round. Part 0 holds the pinned depot display; nothing is drawn by
 * block models (client/TowerControlConsoleRenderer draws its mesh and live parts). Each cell collides with what stands
 * in it on a quarter-block grid: the raised floor, the desk to its top, the riser to the cell's top. A click anywhere
 * opens the depot screen.
 */
public final class TowerControlConsoleBlock extends HorizontalDirectionalBlock implements EntityBlock, DepotDisplayBlockEntity.Display,
		io.github.jimbozoomer.jugcraft.placement.StructureGhost.Source {
	/** The arc's axis from the master cell's corner, facing north (blocks). */
	public static final double AX = 0.5, AZ = 9.5;
	public static final double A0 = Math.toRadians(240), A1 = Math.toRadians(300);
	/** The raised floor's top, the desk's top, the console's back and its operator edge (as the tool has them). */
	public static final double PLATFORM = 3 / 16.0, DESK = PLATFORM + 0.76, BACK = 8.72, EDGE = 9.86;
	/** The floor band: from the curved step out to BAND_R, HALF_W each side of the axis. */
	public static final double STEP_R = 8.25, BAND_R = 10.0, HALF_W = 5.5;
	public static final Footprint FOOTPRINT = Footprint.of(footprint());
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, FOOTPRINT.size() - 1);
	/** Parts under the desk or riser, which need clear headroom over them for the monitors. */
	private static final boolean[] UNDER_DESK = new boolean[FOOTPRINT.size()];
	private final Map<Direction, VoxelShape[]> shapes = new EnumMap<>(Direction.class);

	public TowerControlConsoleBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, 0));
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			VoxelShape[] cells = new VoxelShape[FOOTPRINT.size()];
			for (int part = 0; part < cells.length; part++) {
				Vec3i o = FOOTPRINT.offsets().get(part);
				VoxelShape shape = Shapes.empty();
				for (int i = 0; i < 4; i++) {
					for (int j = 0; j < 4; j++) {
						double h = height(o.getX() + (i + 0.5) / 4, o.getZ() + (j + 0.5) / 4);
						if (h > 0) {
							if (h > PLATFORM + 0.01) {
								UNDER_DESK[part] = true;
							}
							shape = Shapes.or(shape, FurnitureBlock.turned(new double[] {i * 4, 0, j * 4, i * 4 + 4, Math.min(16, h * 16), j * 4 + 4}, facing));
						}
					}
				}
				cells[part] = shape.isEmpty() ? Block.box(7, 0, 7, 9, 1, 9) : shape.optimize();
			}
			shapes.put(facing, cells);
		}
	}

	/** The cells of the floor band, as tools/tower_control_console.footprint() lists them: the master, then row by row. */
	private static Vec3i[] footprint() {
		List<Vec3i> out = new ArrayList<>();
		out.add(Vec3i.ZERO);
		for (int dz = -3; dz < 10; dz++) {
			for (int dx = -6; dx <= 6; dx++) {
				if (dx == 0 && dz == 0) {
					continue;
				}
				if (dx < AX - HALF_W - 1e-9 || dx + 1 > AX + HALF_W + 1e-9 || dz + 1 > AZ) {
					continue;
				}
				double x0 = dx - AX, x1 = dx + 1 - AX, z0 = dz - AZ, z1 = dz + 1 - AZ;
				double near = Math.hypot(Math.min(Math.max(0, x0), x1), Math.min(Math.max(0, z0), z1));
				double far = Math.hypot(Math.max(Math.abs(x0), Math.abs(x1)), Math.max(Math.abs(z0), Math.abs(z1)));
				if (near < BAND_R - 1e-6 && far > STEP_R + 1e-6) {
					out.add(new Vec3i(dx, 0, dz));
				}
			}
		}
		return out.toArray(Vec3i[]::new);
	}

	/** What stands at (x, z) of the master's frame facing north, as a height over the hall floor (0: nothing). */
	static double height(double x, double z) {
		double rx = x - AX, rz = z - AZ, r = Math.hypot(rx, rz);
		if (r < STEP_R) {
			return 0;
		}
		double a = Math.atan2(rz, rx);
		if (a < 0) {
			a += 2 * Math.PI;
		}
		if (a >= A0 - 0.004 && a <= A1 + 0.004 && r >= BACK - 0.03 && r <= EDGE + 0.05) {
			return r < BACK + 0.35 ? 1.0 : DESK;
		}
		return PLATFORM;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, PART);
	}

	@Override
	public boolean isAnchor(BlockState state) {
		return state.getValue(PART) == 0;
	}

	@Override
	public int linkRange() {
		return 24;
	}

	public static BlockPos partPos(BlockPos master, Direction facing, int part) {
		return FOOTPRINT.partPos(master, facing, part);
	}

	public static BlockPos masterPos(BlockPos pos, BlockState state) {
		return FOOTPRINT.masterPos(pos, state.getValue(FACING), state.getValue(PART));
	}

	/** Every part present and loaded, so a click on a stray piece does nothing. */
	public boolean complete(Level level, BlockPos master, Direction facing) {
		for (int part = 0; part < FOOTPRINT.size(); part++) {
			BlockPos at = partPos(master, facing, part);
			if (!level.isLoaded(at)) {
				return false;
			}
			BlockState found = level.getBlockState(at);
			if (!found.is(this) || found.getValue(FACING) != facing || found.getValue(PART) != part) {
				return false;
			}
		}
		return true;
	}

	public @Nullable DepotDisplayBlockEntity display(Level level, BlockPos pos, BlockState state) {
		BlockPos master = masterPos(pos, state);
		return complete(level, master, state.getValue(FACING)) && level.getBlockEntity(master) instanceof DepotDisplayBlockEntity display
				? display : null;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		if (!blockedPlacement(context).isEmpty()) {
			if (context.getPlayer() != null && !context.getLevel().isClientSide()) {
				context.getPlayer().sendOverlayMessage(Component.translatable("message.jugcraft.tower_control_console.blocked"));
			}
			return null;
		}
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** The cells in the way of placing it with {@code context}: its own cells, and the headroom over the desk. */
	public List<BlockPos> blockedPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection().getOpposite();
		Level level = context.getLevel();
		List<BlockPos> blocked = new ArrayList<>();
		for (int part = 0; part < FOOTPRINT.size(); part++) {
			BlockPos at = partPos(context.getClickedPos(), facing, part);
			if (!available(context, at) || !level.getBlockState(at).canBeReplaced(BlockPlaceContext.at(context, at, Direction.UP))) {
				blocked.add(at);
			}
			BlockPos over = at.above();
			if (UNDER_DESK[part] && (!available(context, over) || !level.getBlockState(over).getCollisionShape(level, over).isEmpty())) {
				blocked.add(over);
			}
		}
		return List.copyOf(blocked);
	}

	private static boolean available(BlockPlaceContext context, BlockPos at) {
		Level level = context.getLevel();
		return level.isLoaded(at) && !level.isOutsideBuildHeight(at) && level.getWorldBorder().isWithinBounds(at)
				&& (context.getPlayer() == null || level.mayInteract(context.getPlayer(), at));
	}

	/** The placement preview: every part as it would be set, and what is in the way. */
	@Override
	public io.github.jimbozoomer.jugcraft.placement.StructureGhost ghost(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection().getOpposite();
		List<io.github.jimbozoomer.jugcraft.placement.StructureGhost.Cell> cells = new ArrayList<>(FOOTPRINT.size());
		for (int part = 0; part < FOOTPRINT.size(); part++) {
			cells.add(new io.github.jimbozoomer.jugcraft.placement.StructureGhost.Cell(partPos(context.getClickedPos(), facing, part),
					defaultBlockState().setValue(FACING, facing).setValue(PART, part)));
		}
		return new io.github.jimbozoomer.jugcraft.placement.StructureGhost(List.copyOf(cells), blockedPlacement(context));
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.isClientSide()) {
			return;
		}
		for (int part = 1; part < FOOTPRINT.size(); part++) {
			level.setBlock(partPos(pos, state.getValue(FACING), part), state.setValue(PART, part), Block.UPDATE_ALL);
		}
		if (level.getBlockEntity(pos) instanceof DepotDisplayBlockEntity display) {
			var terminal = DroneDepots.nearest(level, pos, linkRange());
			if (terminal != null) {
				display.pin(terminal.getBlockPos());
			}
		}
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return isAnchor(state) ? new DepotDisplayBlockEntity(JugcraftTower.TOWER_CONTROL_CONSOLE_ENTITY, pos, state) : null;
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(this)) {
			return;
		}
		BlockPos master = masterPos(pos, state);
		Direction facing = state.getValue(FACING);
		for (int part = 0; part < FOOTPRINT.size(); part++) {
			BlockPos at = partPos(master, facing, part);
			if (at.equals(pos) || !level.isLoaded(at)) {
				continue;
			}
			BlockState other = level.getBlockState(at);
			if (other.is(this) && other.getValue(FACING) == facing && other.getValue(PART) == part) {
				level.removeBlock(at, false);
			}
		}
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		DepotDisplayBlockEntity display = display(level, pos, state);
		if (display != null && display.terminal() != null && display.reaches(display.terminal(), linkRange())
				&& level.isLoaded(display.terminal())
				&& level.getBlockEntity(display.terminal()) instanceof DroneTerminalBlockEntity terminal) {
			if (level.isClientSide()) {
				MasterConsoleBlock.openScreen.accept(display.terminal(), pos);
			} else {
				if (terminal.owner() == null) {
					terminal.setOwner(player.getUUID());
					player.sendOverlayMessage(Component.translatable("message.jugcraft.drone.claimed"));
				}
				terminal.rescanNow();
				terminal.refreshClients();
			}
		} else if (!level.isClientSide()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.master_console.unlinked"));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		// a model: the client bakes the fixture's parts that never move into the chunk's mesh (client/BakedFixtures); its
		// renderer draws the rest
		return RenderShape.MODEL;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapes.get(state.getValue(FACING))[state.getValue(PART)];
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapes.get(state.getValue(FACING))[state.getValue(PART)];
	}

	/** Turned with a structure, the parts keep their numbers: the footprint turns with the facing. */
	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state;
	}
}
