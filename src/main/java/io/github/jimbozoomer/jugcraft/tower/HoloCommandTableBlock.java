package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneDepots;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
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
 * The Holo Command Table: the Tri-Pillar's giant round hologram table (work list item 5, approved 22 October 2026),
 * 11 blocks across at standing height, in the middle of the hologram rotunda. One item places the whole table: its
 * parts are the cells the round top stands on ({@link #FOOTPRINT}, as tools/holo_command_table.py lists them), part 0
 * the middle cell, the only one with a block entity (a pinned depot display, like the master console's).
 *
 * <p>Nothing is drawn by the block models: client/HoloCommandTableRenderer draws the table's mesh and its live parts (the
 * site plan in the glass field, the radar sweep, the drones, the five touch screens), and, over the Tower Core, the
 * tower hologram as the central holo table did. Each cell collides with the round top within it (to standing
 * height); a click anywhere on it opens the depot screen, as the touch consoles did.
 */
public final class HoloCommandTableBlock extends Block implements EntityBlock, DepotDisplayBlockEntity.Display {
	/** The top's radius, from the middle cell's centre. */
	public static final double RADIUS = 5.5;
	/** The cells under the round top, from the middle cell: the middle first, then row by row from the north-west. */
	public static final List<Vec3i> FOOTPRINT = footprint();
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, FOOTPRINT.size() - 1);
	private static final VoxelShape[] SHAPES = shapes();

	public HoloCommandTableBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(PART, 0));
	}

	private static List<Vec3i> footprint() {
		List<Vec3i> out = new ArrayList<>();
		out.add(Vec3i.ZERO);
		for (int dz = -6; dz <= 6; dz++) {
			for (int dx = -6; dx <= 6; dx++) {
				if (dx == 0 && dz == 0) {
					continue;
				}
				double nx = Math.min(Math.max(0.5, dx), dx + 1), nz = Math.min(Math.max(0.5, dz), dz + 1);
				if (Math.hypot(nx - 0.5, nz - 0.5) < RADIUS - 1e-6) {
					out.add(new Vec3i(dx, 0, dz));
				}
			}
		}
		return List.copyOf(out);
	}

	/** Each part's shape: the round top within its cell, on a quarter-block grid, standing height. */
	private static VoxelShape[] shapes() {
		VoxelShape[] out = new VoxelShape[FOOTPRINT.size()];
		for (int part = 0; part < out.length; part++) {
			Vec3i o = FOOTPRINT.get(part);
			VoxelShape shape = Shapes.empty();
			for (int i = 0; i < 4; i++) {
				for (int j = 0; j < 4; j++) {
					double x = o.getX() + (i + 0.5) / 4, z = o.getZ() + (j + 0.5) / 4;
					if (Math.hypot(x - 0.5, z - 0.5) < RADIUS) {
						shape = Shapes.or(shape, Block.box(i * 4, 0, j * 4, i * 4 + 4, 16, j * 4 + 4));
					}
				}
			}
			out[part] = shape.isEmpty() ? Block.box(6, 0, 6, 10, 1, 10) : shape.optimize();
		}
		return out;
	}

	public static BlockPos partPos(BlockPos master, int part) {
		return master.offset(FOOTPRINT.get(part));
	}

	public static BlockPos masterPos(BlockPos pos, BlockState state) {
		return pos.subtract(FOOTPRINT.get(state.getValue(PART)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PART);
	}

	@Override
	public boolean isAnchor(BlockState state) {
		return state.getValue(PART) == 0;
	}

	@Override
	public int linkRange() {
		return 24;
	}

	/** Every part present and loaded, so a click on a stray piece does nothing. */
	public boolean complete(Level level, BlockPos master) {
		for (int part = 0; part < FOOTPRINT.size(); part++) {
			BlockPos at = partPos(master, part);
			if (!level.isLoaded(at)) {
				return false;
			}
			BlockState found = level.getBlockState(at);
			if (!found.is(this) || found.getValue(PART) != part) {
				return false;
			}
		}
		return true;
	}

	public @Nullable DepotDisplayBlockEntity display(Level level, BlockPos pos, BlockState state) {
		BlockPos master = masterPos(pos, state);
		return complete(level, master) && level.getBlockEntity(master) instanceof DepotDisplayBlockEntity display ? display : null;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		for (Vec3i offset : FOOTPRINT) {
			BlockPos at = context.getClickedPos().offset(offset);
			Level level = context.getLevel();
			if (!level.isLoaded(at) || level.isOutsideBuildHeight(at) || !level.getWorldBorder().isWithinBounds(at)
					|| context.getPlayer() != null && !level.mayInteract(context.getPlayer(), at)
					|| !level.getBlockState(at).canBeReplaced(BlockPlaceContext.at(context, at, Direction.UP))) {
				if (context.getPlayer() != null && !level.isClientSide()) {
					context.getPlayer().sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
							"message.jugcraft.holo_command_table.blocked"));
				}
				return null;
			}
		}
		return defaultBlockState();
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.isClientSide()) {
			return;
		}
		for (int part = 1; part < FOOTPRINT.size(); part++) {
			level.setBlock(partPos(pos, part), state.setValue(PART, part), Block.UPDATE_ALL);
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
		return isAnchor(state) ? new DepotDisplayBlockEntity(JugcraftTower.HOLO_COMMAND_TABLE_ENTITY, pos, state) : null;
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(this)) {
			return;
		}
		BlockPos master = masterPos(pos, state);
		for (int part = 0; part < FOOTPRINT.size(); part++) {
			BlockPos at = partPos(master, part);
			if (at.equals(pos) || !level.isLoaded(at)) {
				continue;
			}
			BlockState other = level.getBlockState(at);
			if (other.is(this) && other.getValue(PART) == part) {
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
					player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.jugcraft.drone.claimed"));
				}
				terminal.rescanNow();
				terminal.refreshClients();
			}
		} else if (!level.isClientSide()) {
			player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.jugcraft.master_console.unlinked"));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[state.getValue(PART)];
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[state.getValue(PART)];
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state;
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state;
	}
}
