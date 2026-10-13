package io.github.jimbozoomer.jugcraft.machine.form;

import io.github.jimbozoomer.jugcraft.machine.Footprint;
import io.github.jimbozoomer.jugcraft.machine.LargeMachineBlock;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * An industrial machine built to a {@link MachineForm}: placing the item checks every structural and clearance position
 * of the envelope and fills the structural ones, or names the first obstruction and places nothing. Only the
 * controller (part 0) holds a {@link FormMachineBlockEntity} and runs; every other part routes interaction to it.
 * Breaking any part removes the whole machine and drops its item once (see {@link LargeMachineBlock}). Pipes, cables,
 * conveyors and hoppers reach the machine only through the form's ports.
 */
public class FormMachineBlock extends LargeMachineBlock {
	/** A block's part property while its block state definition is built (see {@link #numbering}). */
	private static final ThreadLocal<IntegerProperty> BUILDING = new ThreadLocal<>();

	/**
	 * Which structural block of the form this is; 0 is the controller. Each form numbers only its own blocks, so a
	 * twelve-block machine has twelve part values, not one for every position a 6x6x6 envelope could hold.
	 */
	private final IntegerProperty part;
	private final MachineForm form;
	private @Nullable BlockEntityType<FormMachineBlockEntity> entityType;

	public FormMachineBlock(Properties properties, MachineForm form) {
		super(numbering(properties, form), form.family());
		this.part = BUILDING.get();
		BUILDING.remove();
		this.form = form;
	}

	/**
	 * The block state definition is built inside the superclass constructor, before this block's own fields are set,
	 * so the form's part property waits here until the constructor can keep it.
	 */
	private static Properties numbering(Properties properties, MachineForm form) {
		BUILDING.set(IntegerProperty.create("part", 0, form.footprint().size() - 1));
		return properties;
	}

	@Override
	public IntegerProperty partProperty() {
		return part != null ? part : BUILDING.get();
	}

	public MachineForm form() {
		return form;
	}

	/** The block entity type of this block's controller (set once, when the type is registered). */
	public BlockEntityType<FormMachineBlockEntity> entityType() {
		if (entityType == null) {
			throw new IllegalStateException("No block entity type registered for " + form.id());
		}
		return entityType;
	}

	public void setEntityType(BlockEntityType<FormMachineBlockEntity> type) {
		this.entityType = type;
	}

	@Override
	public Footprint footprint(BlockState state) {
		return form.footprint();
	}

	/** The controller a block of a formed machine belongs to, or null (its chunk is not loaded, or it is gone). */
	public static @Nullable FormMachineBlockEntity controllerAt(Level level, BlockPos pos, BlockState state) {
		if (!(state.getBlock() instanceof FormMachineBlock block)) {
			return null;
		}
		BlockPos controller = block.masterPos(pos, state);
		if (!level.isLoaded(controller)) {
			return null;
		}
		return level.getBlockEntity(controller) instanceof FormMachineBlockEntity machine ? machine : null;
	}

	/** The port on world face {@code side} of this block, or null when that face is closed. */
	public @Nullable FormPort portAt(BlockState state, @Nullable Direction side) {
		if (side == null) {
			return null;
		}
		return form.port(part(state), FormSide.of(side, state.getValue(FACING)));
	}

	/** Cables connect only to the form's power ports: its power input, or a generator's power output. */
	@Override
	public boolean acceptsPower(BlockState state, @Nullable Direction side) {
		if (!kind().usesPower()) {
			return false;
		}
		if (side == null) {
			return true;
		}
		FormPort port = portAt(state, side);
		return port != null && port.kind().energy();
	}

	/**
	 * No furnace smoke and crackle: a form shows its work in its lit model and moving parts, and a burning family's
	 * smoke would rise from the controller's column rather than from the machine's own exhaust.
	 */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Direction facing = state.getValue(FACING);
		for (int cell = 0; cell < form.positions(); cell++) {
			if (form.cell(cell) == FormCell.ACCESS || cell == form.controllerCell()) {
				continue;
			}
			BlockPos at = form.cellPos(pos, facing, cell);
			boolean outside = level.isOutsideBuildHeight(at) || !level.getWorldBorder().isWithinBounds(at);
			BlockState there = outside ? null : level.getBlockState(at);
			if (outside || !there.canBeReplaced(BlockPlaceContext.at(context, at, Direction.UP))) {
				Player player = context.getPlayer();
				if (player != null && !level.isClientSide()) {
					Component what = outside ? Component.translatable("message.jugcraft.form.outside")
							: there.getBlock().getName();
					player.sendOverlayMessage(Component.translatable("message.jugcraft.form.obstructed", getName(), what,
							at.getX(), at.getY(), at.getZ(), MachineStatus.cellLabel(form, cell)));
				}
				return null;
			}
		}
		return withPart(state, 0);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return part(state) == 0 ? new FormMachineBlockEntity(pos, state) : null;
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel) || part(state) != 0) {
			return null;
		}
		return createTickerHelper(type, entityType(),
				(tickLevel, pos, tickState, machine) -> machine.serverTick((ServerLevel) tickLevel, pos, tickState));
	}

	/** Buckets and other fluid containers fill input tanks and empty output tanks from any part. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		FormMachineBlockEntity machine = controllerAt(level, pos, state);
		if (machine != null && FluidStorageUtil.interactWithFluidStorage(machine.tanks().exposed(), player, hand)) {
			return InteractionResult.SUCCESS;
		}
		return super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	/**
	 * Something changed beside a part (a block set into a clearance, say): the controller checks its structure on its
	 * next tick instead of waiting for its periodic check.
	 */
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
			@Nullable Orientation orientation, boolean movedByPiston) {
		super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
		if (!level.isClientSide()) {
			FormMachineBlockEntity machine = controllerAt(level, pos, state);
			if (machine != null) {
				machine.requestCheck();
			}
		}
	}

	/** Hoppers go through the form's item ports (the item lookup), never straight into its slots. */
	@Override
	public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
		return null;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		FormMachineBlockEntity machine = controllerAt(level, pos, state);
		return machine == null ? 0 : machine.comparatorSignal();
	}
}
