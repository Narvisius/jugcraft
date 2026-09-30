package io.github.jimbozoomer.jugcraft.storage;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleItemStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Holds up to {@link #STACKS} stacks of one item. Exposed to pipes and hoppers through Fabric's item storage. */
public class CrateBlockEntity extends BlockEntity {
	public static final int STACKS = 32;

	public final SingleItemStorage storage = new SingleItemStorage() {
		@Override
		protected long getCapacity(ItemVariant variant) {
			return (long) STACKS * variant.toStack().getMaxStackSize();
		}

		@Override
		protected void onFinalCommit() {
			setChanged();
		}
	};

	public CrateBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftStorage.CRATE_ENTITY, pos, state);
	}

	/** Breaking a crate drops everything in it, a stack at a time. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level == null || storage.isResourceBlank()) {
			return;
		}
		long left = storage.amount;
		while (left > 0) {
			ItemStack stack = storage.variant.toStack((int) Math.min(left, storage.variant.toStack().getMaxStackSize()));
			left -= stack.getCount();
			Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
		}
		storage.amount = 0;
		storage.variant = ItemVariant.blank();
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		storage.readValue(input);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		storage.writeValue(output);
	}
}
