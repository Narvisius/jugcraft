package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The storage rack's 54 slots. Slot i is in bay i / 6 (bays numbered along the bottom level left to right, then up a
 * level), so a rack fills from the bottom. The client gets only what each slot looks like on the pallet (its
 * {@link #looks()}, 0 for empty): the kind of load comes from the item, so a stack of one thing looks alike.
 */
public class StorageRackBlockEntity extends BaseContainerBlockEntity {
	public static final int SIZE = 54, PER_BAY = 6;
	/** Kinds of load: kraft carton, labelled carton, darker carton, white carton, labelled white, blue tote, drum, red drum, crate. */
	public static final int KINDS = 9;
	private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
	private int[] looks = new int[SIZE];

	public StorageRackBlockEntity(BlockPos pos, BlockState state) {
		super(PlantInterior.RACK_ENTITY, pos, state);
	}

	/** What each slot shows on its pallet: 0 empty, else 1 + the kind of load. */
	public int[] looks() {
		return looks;
	}

	private int[] computeLooks() {
		int[] out = new int[SIZE];
		for (int i = 0; i < SIZE; i++) {
			ItemStack stack = items.get(i);
			if (!stack.isEmpty()) {
				int h = BuiltInRegistries.ITEM.getKey(stack.getItem()).hashCode() & 0x7fffffff;
				// Mostly cartons, now and then a tote, drum or crate.
				int[] weights = {0, 0, 0, 1, 2, 3, 3, 4, 5, 5, 6, 7, 8};
				out[i] = 1 + weights[h % weights.length];
			}
		}
		return out;
	}

	@Override
	public void setChanged() {
		super.setChanged();
		if (level != null && !level.isClientSide()) {
			int[] now = computeLooks();
			if (!Arrays.equals(now, looks)) {
				looks = now;
				BlockState state = getBlockState();
				level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
			}
		}
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("container.jugcraft.storage_rack");
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
		return SIZE;
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return ChestMenu.sixRows(containerId, inventory, this);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** The client is sent the looks only, not the items. */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.putIntArray("looks", looks);
		return tag;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		int[] sent = input.getIntArray("looks").orElse(null);
		looks = sent != null && sent.length == SIZE ? sent : computeLooks();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
	}
}
