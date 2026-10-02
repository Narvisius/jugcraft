package io.github.jimbozoomer.jugcraft.fluid;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
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
 * The tinplate tank, and the glass tank (batch 20). Right-click with a bucket (or any fluid container item) to fill or
 * empty it, with an empty hand to read its contents. Comparators read how full it is. Tanks touching face to face join
 * into one ({@link TankGroup}). Breaking a tank keeps its contents: the item carries them ({@link StoredFluid}).
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
		if (level.getBlockEntity(pos) instanceof FluidTankBlockEntity
				&& FluidStorageUtil.interactWithFluidStorage(TankGroup.at(level, pos), player, hand)) {
			return InteractionResult.SUCCESS;
		}
		return super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FluidTankBlockEntity) {
			TankGroup group = TankGroup.at(level, pos);
			player.sendOverlayMessage(describe(group.fluid(), group.amount(), group.capacity()));
		}
		return InteractionResult.SUCCESS;
	}

	/** "Water: 3000 / 16000 mB" (or "Empty"), for any single-fluid tank; shown when right-clicked with an empty hand. */
	public static Component describe(SingleFluidStorage storage) {
		return describe(storage.variant, storage.amount, storage.getCapacity());
	}

	/** The same for any amount and capacity in droplets (a group of joined tanks, a gauge's reading). */
	public static Component describe(FluidVariant variant, long amount, long capacity) {
		long capacityMb = capacity / FluidNetworks.DROPLETS_PER_MB;
		return variant.isBlank() || amount <= 0
				? Component.translatable("message.jugcraft.tank.empty", capacityMb)
				: Component.translatable("message.jugcraft.tank", FluidVariantAttributes.getName(variant),
						amount / FluidNetworks.DROPLETS_PER_MB, capacityMb);
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
		return level.getBlockEntity(pos) instanceof FluidTankBlockEntity ? StorageUtil.getRedstoneSignal(TankGroup.at(level, pos)) : 0;
	}
}
