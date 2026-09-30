package io.github.jimbozoomer.jugcraft.machine;

import io.github.jimbozoomer.jugcraft.energy.EnergyConnectable;
import io.github.jimbozoomer.jugcraft.fluid.FluidTankBlock;
import io.github.jimbozoomer.jugcraft.energy.EnergyNetworks;
import io.github.jimbozoomer.jugcraft.fluid.FluidNetworks;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** A front-facing machine block (generator, battery box or processor). Behavior lives in {@link MachineBlockEntity}. */
public class MachineBlock extends BaseEntityBlock implements EnergyConnectable {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	private final MachineKind kind;

	public MachineBlock(Properties properties, MachineKind kind) {
		super(properties);
		this.kind = kind;
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
	}

	public MachineKind kind() {
		return kind;
	}

	/** The block holding this machine's block entity; only multi-block machines differ from {@code pos}. */
	public BlockPos masterPos(BlockPos pos, BlockState state) {
		return pos;
	}

	/** Which block of a multi-block machine this is; 0 (the master) for one-block machines. */
	public int part(BlockState state) {
		return 0;
	}

	/**
	 * Whether a cable on {@code side} of this block may power the machine: any side, unless the
	 * machine has a {@link PowerPort}, in which case only that face of that part.
	 */
	public boolean acceptsPower(BlockState state, @Nullable Direction side) {
		if (!kind.usesPower()) {
			return false;
		}
		PowerPort port = kind.powerPort();
		return port == null || port.allows(part(state), state.getValue(FACING), side);
	}

	/**
	 * Running machines with a fire smoke from their top and crackle now and then, like a furnace.
	 * Client-side only: {@link #LIT} is set on the master block, so the effects come from there.
	 */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!kind.burnsFuel() || !state.getValue(LIT) || part(state) != 0) {
			return;
		}
		double x = pos.getX() + 0.5;
		double y = pos.getY() + kind.height();
		double z = pos.getZ() + 0.5;
		if (random.nextInt(3) == 0) {
			level.addParticle(ParticleTypes.SMOKE, x + (random.nextDouble() - 0.5) * 0.3, y + 0.1,
					z + (random.nextDouble() - 0.5) * 0.3, 0.0, 0.04, 0.0);
		}
		if (random.nextInt(40) == 0) {
			level.playLocalSound(x, pos.getY() + 0.5, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.8F, 1.0F, false);
		}
	}

	/** The machine a block belongs to, from any of its parts, or null. */
	public static @Nullable MachineBlockEntity machineAt(Level level, BlockPos pos, BlockState state) {
		if (!(state.getBlock() instanceof MachineBlock machine)) {
			return null;
		}
		return level.getBlockEntity(machine.masterPos(pos, state)) instanceof MachineBlockEntity entity ? entity : null;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new MachineBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel)) {
			return null;
		}
		return createTickerHelper(type, JugcraftMachines.MACHINE_ENTITY,
				(tickLevel, pos, tickState, machine) -> machine.serverTick((ServerLevel) tickLevel, pos, tickState));
	}

	/** Buckets and other fluid containers fill (or drain) a machine's tank instead of opening its screen. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		MachineBlockEntity machine = machineAt(level, pos, state);
		Storage<FluidVariant> tank = machine == null ? null : machine.fluidFor(null);
		if (tank != null && FluidStorageUtil.interactWithFluidStorage(tank, player, hand)) {
			return InteractionResult.SUCCESS;
		}
		return super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (kind == MachineKind.STEEL_TANK) {
			// A tank has no screen: show what it holds, like the tinplate tank.
			// LargeMachineBlock passes the master's position here.
			if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MachineBlockEntity tank && tank.reservoir() != null) {
				player.sendOverlayMessage(FluidTankBlock.describe(tank.reservoir()));
			}
			return InteractionResult.SUCCESS;
		}
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MenuProvider provider) {
			player.openMenu(provider);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		EnergyNetworks.invalidate(level);
		FluidNetworks.invalidate(level);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		EnergyNetworks.invalidate(level);
		FluidNetworks.invalidate(level);
		Containers.updateNeighboursAfterDestroy(state, level, pos);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		MachineBlockEntity machine = machineAt(level, pos, state);
		return machine == null ? 0 : machine.comparatorSignal();
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
		builder.add(FACING, LIT);
	}
}
