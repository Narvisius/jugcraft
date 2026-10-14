package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The storage rack's 54 slots. Slot i is in bay i / 6 (bays numbered along the bottom level left to right, then up a
 * level), so a rack fills from the bottom. The client gets only what each slot looks like on the pallet (its
 * {@link #looks()}, 0 for empty): the kind of load comes from the item ({@link #lookOf}), so a stack of one thing looks
 * alike. Only something holding a liquid (a filled bucket, a potion or bottle, any mod's fluid container) is stored as
 * a drum or jerrycans, the drum's colour by what is in it; everything else (an empty bucket too) goes in cartons, totes
 * or crates.
 */
public class StorageRackBlockEntity extends BaseContainerBlockEntity {
	public static final int SIZE = 54, PER_BAY = 6;
	/** Kinds of load (what the renderer draws). */
	public static final int KRAFT = 0, KRAFT_LABEL = 1, KRAFT_DARK = 2, WHITE = 3, WHITE_LABEL = 4, TOTE = 5, DRUM_BLUE = 6,
			DRUM_RED = 7, CRATE = 8, DRUM_BLACK = 9, DRUM_GREEN = 10, DRUM_YELLOW = 11, DRUM_WHITE = 12, JERRYCANS = 13,
			KRAFT_FRAGILE = 14, KRAFT_LABEL_C = 15;
	public static final int KINDS = 16;
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
				out[i] = 1 + lookOf(stack);
			}
		}
		return out;
	}

	/** The kind of load an item is stored as. */
	public static int lookOf(ItemStack stack) {
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		int h = id.hashCode() & 0x7fffffff;
		int liquid = liquidLook(stack);
		if (liquid >= 0) {
			return liquid;
		}
		String path = id.getPath();
		if (path.contains("glass") || path.contains("bottle") || path.contains("lantern") || path.contains("lamp")) {
			return pick(h, KRAFT_FRAGILE, KRAFT_FRAGILE, WHITE_LABEL);
		}
		if (stack.isDamageableItem() || stack.is(ConventionalItemTags.INGOTS) || stack.is(ConventionalItemTags.ORES)
				|| stack.is(ConventionalItemTags.RAW_MATERIALS) || stack.is(ConventionalItemTags.STORAGE_BLOCKS)) {
			// Heavy goods: tools, armour, metal and ore.
			return pick(h, CRATE, CRATE, TOTE);
		}
		if (stack.has(DataComponents.FOOD)) {
			return pick(h, KRAFT, KRAFT_LABEL, KRAFT_DARK, KRAFT_LABEL_C, WHITE);
		}
		return pick(h, KRAFT, KRAFT, KRAFT_LABEL, KRAFT_LABEL_C, KRAFT_DARK, WHITE, WHITE_LABEL, TOTE, TOTE, CRATE);
	}

	private static int pick(int h, int... kinds) {
		return kinds[h % kinds.length];
	}

	/** A drum or jerrycans if the item holds a liquid, else -1. */
	private static int liquidLook(ItemStack stack) {
		Item item = stack.getItem();
		if (item instanceof PotionItem || item == Items.HONEY_BOTTLE || item == Items.OMINOUS_BOTTLE) {
			return JERRYCANS;
		}
		if (item == Items.MILK_BUCKET) {
			return DRUM_WHITE;
		}
		Fluid fluid = Fluids.EMPTY;
		if (item instanceof BucketItem bucket) {
			fluid = bucket.getContent();
		} else {
			try {
				Storage<FluidVariant> held = FluidStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack));
				FluidVariant variant = held == null ? null : StorageUtil.findStoredResource(held);
				if (variant != null && !variant.isBlank()) {
					fluid = variant.getFluid();
				}
			} catch (RuntimeException e) {
				// Another mod's container that can't be asked this way: not a liquid as far as the rack can tell.
			}
		}
		if (fluid == Fluids.EMPTY) {
			return -1;
		}
		FluidState state = fluid.defaultFluidState();
		if (state.is(FluidTags.WATER)) {
			return DRUM_BLUE;
		}
		if (state.is(FluidTags.LAVA)) {
			return DRUM_RED;
		}
		String name = BuiltInRegistries.FLUID.getKey(fluid).getPath();
		if (name.matches(".*(crude|oil|lubric|tar|asphalt|bitumen|creosote).*")) {
			return DRUM_BLACK;
		}
		if (name.matches(".*(gasoline|petrol|diesel|naphtha|fuel|kerosene|ethanol|methanol|alcohol).*")) {
			return DRUM_RED;
		}
		if (name.matches(".*(acid|lye|caustic|hydroxide|bleach|chlor|milk).*")) {
			return DRUM_WHITE;
		}
		if (name.matches(".*(water|brine|fracking).*")) {
			return DRUM_BLUE;
		}
		if (name.matches(".*(electrolyte|honey|syrup).*")) {
			return DRUM_YELLOW;
		}
		return pick(BuiltInRegistries.FLUID.getKey(fluid).hashCode() & 0x7fffffff, DRUM_BLUE, DRUM_GREEN, DRUM_YELLOW, DRUM_WHITE);
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
