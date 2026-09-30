package io.github.jimbozoomer.jugcraft.fluid;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The tinplate tank. Right-click with a bucket (or any fluid container item) to fill or
 * empty it, with an empty hand to read its contents. Comparators read how full it is.
 * Breaking the tank loses its contents.
 */
public class FluidTankBlock extends BaseEntityBlock implements FluidConnectable {
	public FluidTankBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FluidTankBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank
				&& FluidStorageUtil.interactWithFluidStorage(tank.storage, player, hand)) {
			return InteractionResult.SUCCESS;
		}
		return super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank) {
			player.sendOverlayMessage(describe(tank.storage));
		}
		return InteractionResult.SUCCESS;
	}

	/** "Water: 3000 / 16000 mB" (or "Empty"), for any single-fluid tank; shown when right-clicked with an empty hand. */
	public static Component describe(SingleFluidStorage storage) {
		long capacity = storage.getCapacity() / FluidNetworks.DROPLETS_PER_MB;
		return storage.isResourceBlank()
				? Component.translatable("message.jugcraft.tank.empty", capacity)
				: Component.translatable("message.jugcraft.tank", FluidVariantAttributes.getName(storage.variant),
						storage.amount / FluidNetworks.DROPLETS_PER_MB, capacity);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		FluidNetworks.invalidate(level);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		FluidNetworks.invalidate(level);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank ? StorageUtil.getRedstoneSignal(tank.storage) : 0;
	}
}
