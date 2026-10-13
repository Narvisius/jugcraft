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

/** One three-metre command console, six physical cells and one passive, pinned depot display. */
public final class MasterConsoleBlock extends HorizontalDirectionalBlock implements EntityBlock, DepotDisplayBlockEntity.Display {
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, 5);
	/** Facing points toward the operator; the front-centre cell is the sole controller. */
	public static final Footprint FOOTPRINT = Footprint.of(Vec3i.ZERO, new Vec3i(-1, 0, 0), new Vec3i(1, 0, 0),
			new Vec3i(0, 0, 1), new Vec3i(-1, 0, 1), new Vec3i(1, 0, 1));
	public static java.util.function.BiConsumer<BlockPos, BlockPos> openScreen = (terminal, access) -> { };
	private final Map<Direction, VoxelShape[]> collision = new EnumMap<>(Direction.class);
	private final Map<Direction, VoxelShape> selection = new EnumMap<>(Direction.class);
	private static final double[][] BOXES = {
			{-15.5, 10.8, 1, 31.5, 12.8, 26}, {-15, 0, 21, 31, 11, 31},
			{-14.8, 0, 3, -10.5, 11, 18}, {26.5, 0, 3, 30.8, 11, 18},
			{-15, 12.9, 23, 31, 25, 27}};

	public MasterConsoleBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, 0));
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			VoxelShape whole = Shapes.empty();
			for (double[] box : BOXES) whole = Shapes.or(whole, FurnitureBlock.turned(box, facing));
			selection.put(facing, whole.optimize());
			VoxelShape[] cells = new VoxelShape[FOOTPRINT.size()];
			for (int part = 0; part < cells.length; part++) {
				Vec3i offset = FOOTPRINT.offsets().get(part); double x = offset.getX() * 16, z = offset.getZ() * 16;
				VoxelShape local = Shapes.empty();
				for (double[] box : BOXES) {
					double x0 = Math.max(x, box[0]), z0 = Math.max(z, box[2]);
					double x1 = Math.min(x + 16, box[3]), z1 = Math.min(z + 16, box[5]);
					if (x0 < x1 && z0 < z1) local = Shapes.or(local, FurnitureBlock.turned(
							new double[] {x0 - x, box[1], z0 - z, x1 - x, box[4], z1 - z}, facing));
				}
				cells[part] = local.optimize();
			}
			collision.put(facing, cells);
		}
	}

	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, PART); }
	@Override public boolean isAnchor(BlockState state) { return state.getValue(PART) == 0; }
	@Override public int linkRange() { return 24; }
	public BlockPos masterPos(BlockPos pos, BlockState state) { return FOOTPRINT.masterPos(pos, state.getValue(FACING), state.getValue(PART)); }

	/** Every clicked piece must still belong to a complete, loaded console, not a stale dummy. */
	public boolean complete(Level level, BlockPos master, Direction facing) {
		for (int part = 0; part < FOOTPRINT.size(); part++) {
			BlockPos at = FOOTPRINT.partPos(master, facing, part);
			if (!level.isLoaded(at)) return false;
			BlockState found = level.getBlockState(at);
			if (!found.is(this) || found.getValue(FACING) != facing || found.getValue(PART) != part) return false;
		}
		return true;
	}
	public @Nullable DepotDisplayBlockEntity display(Level level, BlockPos pos, BlockState state) {
		BlockPos master = masterPos(pos, state);
		return complete(level, master, state.getValue(FACING)) && level.getBlockEntity(master) instanceof DepotDisplayBlockEntity display
				? display : null;
	}

	@Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return blockedPlacement(context).isEmpty() ? defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()) : null;
	}
	/** Exact shared placement/preview clearance: six occupied cells and air space for the rear monitor bank. */
	public List<BlockPos> blockedPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection().getOpposite();
		List<BlockPos> blocked = new ArrayList<>();
		for (int part = 0; part < FOOTPRINT.size(); part++) {
			BlockPos at = FOOTPRINT.partPos(context.getClickedPos(), facing, part);
			if (!available(context, at, false)) blocked.add(at);
			if (part >= 3 && !available(context, at.above(), true)) blocked.add(at.above());
		}
		return List.copyOf(blocked);
	}
	private static boolean available(BlockPlaceContext context, BlockPos at, boolean headroom) {
		Level level = context.getLevel();
		if (!level.isLoaded(at) || level.isOutsideBuildHeight(at) || !level.getWorldBorder().isWithinBounds(at)
				|| context.getPlayer() != null && !level.mayInteract(context.getPlayer(), at)) return false;
		BlockState found = level.getBlockState(at);
		return found.canBeReplaced(BlockPlaceContext.at(context, at, Direction.UP))
				&& (!headroom || found.getCollisionShape(level, at).isEmpty());
	}
	@Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.isClientSide()) return;
		for (int part = 1; part < FOOTPRINT.size(); part++) level.setBlock(FOOTPRINT.partPos(pos, state.getValue(FACING), part),
				state.setValue(PART, part), Block.UPDATE_ALL);
		if (level.getBlockEntity(pos) instanceof DepotDisplayBlockEntity display) {
			var terminal = DroneDepots.nearest(level, pos, linkRange());
			if (terminal != null) display.pin(terminal.getBlockPos());
		}
	}
	@Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return isAnchor(state) ? new DepotDisplayBlockEntity(JugcraftTower.MASTER_CONSOLE_ENTITY, pos, state) : null;
	}
	@Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(this)) return;
		BlockPos master = masterPos(pos, state);
		for (int part = 0; part < FOOTPRINT.size(); part++) {
			BlockPos at = FOOTPRINT.partPos(master, state.getValue(FACING), part);
			if (at.equals(pos) || !level.isLoaded(at)) continue;
			BlockState other = level.getBlockState(at);
			if (other.is(this) && other.getValue(FACING) == state.getValue(FACING) && other.getValue(PART) == part) level.removeBlock(at, false);
		}
	}
	@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		DepotDisplayBlockEntity display = display(level, pos, state);
		if (display != null && display.terminal() != null && display.getBlockPos().distSqr(display.terminal()) <= linkRange() * linkRange()
				&& level.isLoaded(display.terminal())
				&& level.getBlockEntity(display.terminal()) instanceof DroneTerminalBlockEntity terminal) {
			if (level.isClientSide()) openScreen.accept(display.terminal(), pos);
			else {
				if (terminal.owner() == null) {
					terminal.setOwner(player.getUUID());
					player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.jugcraft.drone.claimed"));
				}
				terminal.rescanNow(); terminal.refreshClients();
			}
		} else if (!level.isClientSide()) player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.jugcraft.master_console.unlinked"));
		return InteractionResult.SUCCESS;
	}

	/** The crosshair outlines the complete console; collision stays local to each physical cell. */
	@Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		BlockPos offset = FOOTPRINT.partPos(BlockPos.ZERO, state.getValue(FACING), state.getValue(PART));
		return selection.get(state.getValue(FACING)).move(-offset.getX(), 0, -offset.getZ());
	}
	@Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return collision.get(state.getValue(FACING))[state.getValue(PART)];
	}
	@Override protected BlockState rotate(BlockState state, Rotation rotation) { return state; }
	@Override protected BlockState mirror(BlockState state, Mirror mirror) { return state; }
}
