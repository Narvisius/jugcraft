package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The Silk Cocoon's {@value #SLOTS} slots, and on clients when it last wriggled. */
public class SilkCocoonBlockEntity extends BaseContainerBlockEntity {
	public static final int SLOTS = 9;
	private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	private long wriggled = Long.MIN_VALUE / 2;

	public SilkCocoonBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.SILK_COCOON_ENTITY, pos, state);
	}

	/** Records when it wriggled (both sides; the renderer reads it on clients). */
	public void mark(long time) {
		wriggled = time;
	}

	public long wriggled() {
		return wriggled;
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("container.jugcraft.silk_cocoon");
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	public int getContainerSize() {
		return SLOTS;
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new ChestMenu(MenuType.GENERIC_9x1, containerId, inventory, this, 1);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
	}
}
