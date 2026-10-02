package io.github.jimbozoomer.jugcraft.solar;

import io.github.jimbozoomer.jugcraft.energy.EnergyConnectable;
import io.github.jimbozoomer.jugcraft.energy.EnergyNetworks;
import io.github.jimbozoomer.jugcraft.fluid.FluidNetworks;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The solar receiver (see {@link SolarReceiverBlockEntity}). Pipe or pour water into it; cables take power from any
 * side. Right-click it to read its field.
 */
public class SolarReceiverBlock extends BaseEntityBlock implements EnergyConnectable {
	public SolarReceiverBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SolarReceiverBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel)) {
			return null;
		}
		return createTickerHelper(type, JugcraftSolar.SOLAR_RECEIVER_ENTITY,
				(tickLevel, pos, tickState, receiver) -> receiver.serverTick((ServerLevel) tickLevel, pos));
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (level.getBlockEntity(pos) instanceof SolarReceiverBlockEntity receiver
				&& FluidStorageUtil.interactWithFluidStorage(receiver.water(), player, hand)) {
			return InteractionResult.SUCCESS;
		}
		return super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SolarReceiverBlockEntity receiver
				&& level instanceof ServerLevel server) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.solar_receiver",
					SolarReceiverBlockEntity.countHeliostats(server, pos), receiver.lastOutput(),
					receiver.water().amount / FluidNetworks.DROPLETS_PER_MB));
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
