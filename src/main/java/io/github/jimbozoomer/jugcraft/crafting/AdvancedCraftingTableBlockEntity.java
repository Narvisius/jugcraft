package io.github.jimbozoomer.jugcraft.crafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Advanced Crafting Table's grid, at its anchor: 36 slots (6 x 6, row by row from the far row) that stay on the table
 * when the screen closes. Whenever they change the server finds what they make (jugcraft:advanced_crafting) and sends
 * the table to the clients, which draw the items lying on the grid's cells, the result over it and the touchscreen;
 * nothing ticks. Taking the result takes one of every item on the grid (a recipe uses every one) and leaves any
 * container (a bucket) behind.
 */
public class AdvancedCraftingTableBlockEntity extends BlockEntity implements Container, ExtendedMenuProvider<BlockPos> {
	public static final int SLOTS = AdvancedCraftingInput.SIDE * AdvancedCraftingInput.SIDE;

	private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	/** What the grid makes (empty for nothing): found by the server, sent to the clients to draw. */
	private ItemStack result = ItemStack.EMPTY;
	/** The open screens on this table (server only), told at once when the result changes. */
	private final Set<AdvancedCraftingMenu> menus = Collections.newSetFromMap(new IdentityHashMap<>());

	public AdvancedCraftingTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** What the grid makes now (a copy is the caller's to change). */
	public ItemStack result() {
		return result;
	}

	public AdvancedCraftingInput input() {
		return new AdvancedCraftingInput(List.copyOf(items));
	}

	void listen(AdvancedCraftingMenu menu) {
		menus.add(menu);
	}

	void unlisten(AdvancedCraftingMenu menu) {
		menus.remove(menu);
	}

	/** Finds what the grid makes and sends the table to the clients (server only). */
	public void refresh() {
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		result = find(server).map(recipe -> recipe.assemble(input())).orElse(ItemStack.EMPTY);
		for (AdvancedCraftingMenu menu : new ArrayList<>(menus)) {
			menu.refreshResult();
		}
		// the clients draw the grid's items as well as the result: they get the table whenever it changes
		server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	private java.util.Optional<AdvancedCraftingRecipe> find(ServerLevel server) {
		AdvancedCraftingInput input = input();
		if (input.isEmpty() || JugcraftCrafting.ADVANCED == null) {
			return java.util.Optional.empty();
		}
		return server.getServer().getRecipeManager().getRecipeFor(JugcraftCrafting.ADVANCED, input, server).map(RecipeHolder::value);
	}

	/**
	 * Makes the result once (server only): takes one of every item on the grid, leaving a container in its slot (or
	 * giving it to the player when the slot still holds more). Returns what was made, or nothing if the grid no longer
	 * makes anything.
	 */
	public ItemStack craft(Player player) {
		if (!(level instanceof ServerLevel server)) {
			return ItemStack.EMPTY;
		}
		var recipe = find(server);
		if (recipe.isEmpty()) {
			refresh();
			return ItemStack.EMPTY;
		}
		ItemStack made = recipe.get().assemble(input());
		for (int i = 0; i < SLOTS; i++) {
			ItemStack stack = items.get(i);
			if (stack.isEmpty()) {
				continue;
			}
			ItemStackTemplate remainder = stack.getItem().getCraftingRemainder();
			stack.shrink(1);
			if (remainder != null) {
				ItemStack left = remainder.create();
				if (stack.isEmpty()) {
					items.set(i, left);
				} else {
					player.getInventory().placeItemBackInInventory(left, net.minecraft.util.Prediction.SERVER_ONLY);
				}
			} else if (stack.isEmpty()) {
				items.set(i, ItemStack.EMPTY);
			}
		}
		setChanged();
		return made;
	}

	// ------------------------------------------------------------------ the container

	@Override
	public int getContainerSize() {
		return SLOTS;
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack stack : items) {
			if (!stack.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		return items.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		ItemStack taken = ContainerHelper.removeItem(items, slot, count);
		if (!taken.isEmpty()) {
			setChanged();
		}
		return taken;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(items, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		items.set(slot, stack);
		stack.limitSize(getMaxStackSize(stack));
		setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public void clearContent() {
		items.clear();
		setChanged();
	}

	/** Every change to the grid finds its result again and tells the clients. */
	@Override
	public void setChanged() {
		super.setChanged();
		refresh();
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null) {
			Containers.dropContents(level, pos, items);
		}
	}

	// ------------------------------------------------------------------ the screen

	@Override
	public Component getDisplayName() {
		return getBlockState().getBlock().getName();
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		refresh();
		return new AdvancedCraftingMenu(id, inventory, this);
	}

	@Override
	public BlockPos getScreenOpeningData(ServerPlayer player) {
		return worldPosition;
	}

	// ------------------------------------------------------------------ saving, the clients' copy

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items.clear();
		ContainerHelper.loadAllItems(input, items);
		result = input.read("result", ItemStack.CODEC).orElse(ItemStack.EMPTY);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		if (!result.isEmpty()) {
			output.store("result", ItemStack.CODEC, result);
		}
	}

	@Override
	public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}
}
