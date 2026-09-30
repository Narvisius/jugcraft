package io.github.jimbozoomer.jugcraft.fluid;

import io.github.jimbozoomer.jugcraft.energy.EnergyConnectable;
import io.github.jimbozoomer.jugcraft.energy.EnergyNetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** The electric pump block: cables power it, pipes carry its output. Right-click shows its status. */
public class ElectricPumpBlock extends BaseEntityBlock implements EnergyConnectable, FluidConnectable {
	public ElectricPumpBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ElectricPumpBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel)) {
			return null;
		}
		return createTickerHelper(type, JugcraftFluids.PUMP_ENTITY,
				(tickLevel, pos, tickState, pump) -> pump.serverTick((ServerLevel) tickLevel, pos));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ElectricPumpBlockEntity pump) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.pump",
					pump.energy().getAmount(), pump.energy().getCapacity(), pump.bufferedMb()));
		}
		return InteractionResult.SUCCESS;
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
	}
}
