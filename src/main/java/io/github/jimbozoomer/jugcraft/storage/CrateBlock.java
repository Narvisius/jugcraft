package io.github.jimbozoomer.jugcraft.storage;

import io.github.jimbozoomer.jugcraft.logistics.ItemConnectable;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
 * The item crate: one item type, up to 32 stacks.
 * <ul>
 * <li>Right-click with an item: puts in as much of the held stack as fits.</li>
 * <li>Right-click with an empty hand: takes out one stack. Sneak to just read what it holds.</li>
 * <li>Item pipes, extractors and hoppers work with it; comparators read how full it is.</li>
 * </ul>
 */
public class CrateBlock extends BaseEntityBlock implements ItemConnectable {
	public CrateBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CrateBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof CrateBlockEntity crate)) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		if (!level.isClientSide()) {
			try (Transaction transaction = Transaction.openOuter()) {
				long inserted = crate.storage.insert(ItemVariant.of(stack), stack.getCount(), transaction);
				transaction.commit();
				stack.shrink((int) inserted);
			}
			player.sendOverlayMessage(describe(crate));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof CrateBlockEntity crate)) {
			return InteractionResult.SUCCESS;
		}
		if (!player.isShiftKeyDown() && !crate.storage.isResourceBlank()) {
			ItemVariant variant = crate.storage.variant;
			try (Transaction transaction = Transaction.openOuter()) {
				long taken = crate.storage.extract(variant, variant.toStack().getMaxStackSize(), transaction);
				transaction.commit();
				ItemStack stack = variant.toStack((int) taken);
				if (!player.getInventory().add(stack)) {
					popResource(level, pos.above(), stack); // Full inventory: drop it on top of the crate.
				}
			}
		}
		player.sendOverlayMessage(describe(crate));
		return InteractionResult.SUCCESS;
	}

	static Component describe(CrateBlockEntity crate) {
		if (crate.storage.isResourceBlank()) {
			return Component.translatable("message.jugcraft.crate.empty", CrateBlockEntity.STACKS);
		}
		return Component.translatable("message.jugcraft.crate", crate.storage.amount,
				crate.storage.variant.toStack().getHoverName(), crate.storage.getCapacity());
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return level.getBlockEntity(pos) instanceof CrateBlockEntity crate ? StorageUtil.getRedstoneSignal(crate.storage) : 0;
	}
}
