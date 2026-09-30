package io.github.jimbozoomer.jugcraft.logistics;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The sorter's filter (nine sample items, shown in a dispenser-style 3x3 screen) and its inlet:
 * an insert-only item storage that forwards matching items into the inventory the outlet faces.
 * Hoppers and pipes cannot reach the filter slots themselves.
 */
public class ItemSorterBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
	private static final int[] NO_SLOTS = {};
	private NonNullList<ItemStack> filter = NonNullList.withSize(9, ItemStack.EMPTY);
	private final Storage<ItemVariant> inlet = new Inlet();

	public ItemSorterBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftLogistics.SORTER_ENTITY, pos, state);
	}

	/** The inlet for pipes on every side except the outlet; null on the outlet side. */
	public @Nullable Storage<ItemVariant> itemsFor(@Nullable Direction side) {
		return side == getBlockState().getValue(ItemSorterBlock.FACING) ? null : inlet;
	}

	/** Whether a sample of this item is in the filter. An empty filter matches nothing. */
	public boolean matches(ItemVariant resource) {
		for (ItemStack sample : filter) {
			if (!sample.isEmpty() && resource.isOf(sample.getItem())) {
				return true;
			}
		}
		return false;
	}

	private final class Inlet implements InsertionOnlyStorage<ItemVariant> {
		@Override
		public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
			StoragePreconditions.notBlankNotNegative(resource, maxAmount);
			if (level == null || !matches(resource)) {
				return 0;
			}
			Direction facing = getBlockState().getValue(ItemSorterBlock.FACING);
			Storage<ItemVariant> target = ItemStorage.SIDED.find(level, worldPosition.relative(facing), facing.getOpposite());
			return target == null ? 0 : target.insert(resource, maxAmount, transaction);
		}
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("container.jugcraft.item_sorter");
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return filter;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.filter = items;
	}

	@Override
	public int getContainerSize() {
		return filter.size();
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new DispenserMenu(containerId, inventory, this);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return NO_SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return false;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		filter = NonNullList.withSize(9, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, filter);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, filter);
	}
}
