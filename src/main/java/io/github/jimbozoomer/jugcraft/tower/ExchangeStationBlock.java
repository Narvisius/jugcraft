package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.energy.EnergyConnectable;
import io.github.jimbozoomer.jugcraft.logistics.ItemConnectable;
import io.github.jimbozoomer.jugcraft.machine.Footprint;
import io.github.jimbozoomer.jugcraft.placement.StructureGhost;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** One inventory item, one bounded master entity, unified sliced art and a single marked external port. */
public final class ExchangeStationBlock extends HorizontalDirectionalBlock implements EntityBlock, EnergyConnectable, ItemConnectable, StructureGhost.Source {
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, 269);
	/** Missing saved properties resolve to false, preserving the original part numbering. */
	public static final BooleanProperty LARGE = BooleanProperty.create("large");
	public static final Footprint POWER_FOOTPRINT = powerFootprint();
	public static final Footprint CARGO_FOOTPRINT = cargoFootprint();
	public static final Footprint LARGE_FOOTPRINT = largeFootprint();
	public final boolean energy;
	private final Footprint footprint;
	private final int portPart;

	public ExchangeStationBlock(Properties properties, boolean energy) {
		super(properties);
		this.energy = energy;
		footprint = energy ? POWER_FOOTPRINT : CARGO_FOOTPRINT;
		portPart = footprint.offsets().indexOf(new Vec3i(2, 1, 0));
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, 0).setValue(LARGE, false));
	}

	private static Footprint powerFootprint() {
		List<Vec3i> out = new ArrayList<>();
		for (int y = 0; y < 4; y++) for (int z = 0; z < 5; z++) for (int x = 0; x < 5; x++) {
			if (y == 0 || y <= 2 && (z >= 1 && z <= 3 || z == 0 && x >= 1 && x <= 3)
					|| y == 3 && z == 2 && x >= 1 && x <= 3) out.add(new Vec3i(x, y, z));
		}
		return new Footprint(List.copyOf(out));
	}

	private static Footprint cargoFootprint() {
		List<Vec3i> out = new ArrayList<>();
		for (int y = 0; y < 3; y++) for (int z = 0; z < 4; z++) for (int x = 0; x < 5; x++) out.add(new Vec3i(x, y, z));
		return new Footprint(List.copyOf(out));
	}

	private static Footprint largeFootprint() {
		List<Vec3i> out = new ArrayList<>();
		for (int y = 0; y < 5; y++) for (int z = 0; z < 6; z++) for (int x = 0; x < 9; x++) out.add(new Vec3i(x, y, z));
		return new Footprint(List.copyOf(out));
	}

	public Footprint footprint() { return footprint; }
	public Footprint footprint(BlockState state) { return state.getValue(LARGE) ? LARGE_FOOTPRINT : footprint; }
	public int longestSide() { return 9; }
	public int portPart() { return portPart; }
	public int portPart(BlockState state) { return state.getValue(LARGE) ? 58 : portPart; }
	public int height() { return energy ? 4 : 3; }
	public int depth() { return energy ? 5 : 4; }

	public BlockPos masterPos(BlockPos pos, BlockState state) {
		Footprint footprint = footprint(state);
		int part = state.getValue(PART);
		return part < footprint.size() ? footprint.masterPos(pos, state.getValue(FACING), part) : pos;
	}

	public @Nullable ExchangeStationBlockEntity controller(Level level, BlockPos pos, BlockState state) {
		BlockPos master = masterPos(pos, state);
		if (!level.isLoaded(master)) return null;
		BlockState anchor = level.getBlockState(master);
		if (!anchor.is(this) || anchor.getValue(PART) != 0 || anchor.getValue(FACING) != state.getValue(FACING)
				|| anchor.getValue(LARGE) != state.getValue(LARGE)) return null;
		return level.getBlockEntity(master) instanceof ExchangeStationBlockEntity station ? station : null;
	}

	public boolean isPort(BlockState state, @Nullable Direction side) {
		return state.is(this) && state.getValue(PART) == portPart(state) && (side == null || side == state.getValue(FACING));
	}

	/** Reads at most 270 loaded cells on access; there is no station ticker. */
	public boolean complete(Level level, BlockPos master, BlockState state) {
		if (!state.is(this) || state.getValue(PART) != 0) return false;
		Footprint footprint = footprint(state);
		for (int part = 0; part < footprint.size(); part++) {
			BlockPos at = footprint.partPos(master, state.getValue(FACING), part);
			if (!level.isLoaded(at)) return false;
			BlockState found = level.getBlockState(at);
			if (!found.is(this) || found.getValue(PART) != part || found.getValue(FACING) != state.getValue(FACING)
					|| found.getValue(LARGE) != state.getValue(LARGE)) return false;
		}
		return true;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, PART, LARGE); }

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = placementState(context);
		return blockedPlacement(context, state).isEmpty() ? state : null;
	}

	private BlockState placementState(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(LARGE, true);
	}
	private List<BlockPos> blockedPlacement(BlockPlaceContext context, BlockState state) {
		List<BlockPos> blocked = new ArrayList<>();
		Level level = context.getLevel();
		Footprint footprint = footprint(state);
		for (int part = 0; part < footprint.size(); part++) {
			BlockPos at = footprint.partPos(context.getClickedPos(), state.getValue(FACING), part);
			if (!level.isLoaded(at) || level.isOutsideBuildHeight(at) || !level.getWorldBorder().isWithinBounds(at)
					|| context.getPlayer() != null && !level.mayInteract(context.getPlayer(), at)
					|| !level.getBlockState(at).canBeReplaced(BlockPlaceContext.at(context, at, Direction.UP))) blocked.add(at.immutable());
		}
		return List.copyOf(blocked);
	}
	@Override public StructureGhost ghost(BlockPlaceContext context) {
		BlockState state = placementState(context);
		List<StructureGhost.Cell> cells = new ArrayList<>(LARGE_FOOTPRINT.size());
		for (int part = 0; part < LARGE_FOOTPRINT.size(); part++) cells.add(new StructureGhost.Cell(
				LARGE_FOOTPRINT.partPos(context.getClickedPos(), state.getValue(FACING), part), state.setValue(PART, part)));
		return new StructureGhost(List.copyOf(cells), blockedPlacement(context, state));
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.isClientSide()) return;
		Footprint footprint = footprint(state);
		for (int part = 1; part < footprint.size(); part++) level.setBlock(footprint.partPos(pos, state.getValue(FACING), part),
				state.setValue(PART, part), Block.UPDATE_ALL);
		if (placer instanceof Player player && level.getBlockEntity(pos) instanceof ExchangeStationBlockEntity station) {
			station.placedBy(player.getUUID());
			station.bindOwned(player);
		}
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(PART) == 0 ? new ExchangeStationBlockEntity(pos, state) : null;
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(this)) return;
		Footprint footprint = footprint(state);
		BlockPos master = masterPos(pos, state);
		for (int part = 0; part < footprint.size(); part++) {
			BlockPos at = footprint.partPos(master, state.getValue(FACING), part);
			if (!level.isLoaded(at) || at.equals(pos)) continue;
			BlockState other = level.getBlockState(at);
			if (other.is(this) && other.getValue(PART) == part && other.getValue(FACING) == state.getValue(FACING)
					&& other.getValue(LARGE) == state.getValue(LARGE)) level.removeBlock(at, false);
		}
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			ExchangeStationBlockEntity station = controller(level, pos, state);
			if (station != null && station.terminal() == null) station.bindOwned(player);
			if (station == null || !station.mayUse(player)) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.exchange.not_linked"));
			} else {
				var terminal = station.terminal();
				var packager = terminal == null ? null : terminal.packagerEntity();
				if (!energy && packager != null) player.openMenu(new SimpleMenuProvider(
						(id, inventory, viewer) -> station.cargoMenu(id, inventory, viewer, pos), packager.getDisplayName()));
				else player.sendOverlayMessage(Component.translatable(terminal == null ? "message.jugcraft.exchange.not_linked"
						: "message.jugcraft.exchange.energy", terminal == null ? 0 : terminal.energy().getAmount(),
						terminal == null ? 0 : terminal.energy().getCapacity()));
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		Footprint footprint = footprint(state);
		int part = state.getValue(PART);
		if (part >= footprint.size()) return Shapes.empty();
		if (state.getValue(LARGE)) return LargeExchangeShapes.shape(energy, part, state.getValue(FACING));
		Vec3i cell = footprint.offsets().get(part);
		if (!energy) return CargoExchangeShapes.shape(part, state.getValue(FACING));
		if (cell.getY() == 0) return Block.box(0, 0, 0, 16, 4, 16);
		if (energy && cell.getY() == 3) return FurnitureBlock.turned(
				new double[] {3.6, 0, 1.5, 12.4, 3.6, 13.5}, state.getValue(FACING));
		return Block.box(1, 0, 1, 15, 16, 15);
	}

	@Override protected BlockState rotate(BlockState state, Rotation rotation) { return state; }
	@Override protected BlockState mirror(BlockState state, Mirror mirror) { return state; }
}
