package io.github.jimbozoomer.jugcraft.tower;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The tower storage at a Drone Tower's Cargo Exchange, as a big scrolling chest (Justin, 10 November 2026: "you can't
 * physically put things into the cargo storage ... I'd like there to be a way for you to physically put things in"; his
 * pick: "Storage screen": up to 540 stacks, 10 double chests, any item, drag items in and out; client/TowerStorageScreen).
 *
 * <p>The grid shows what the tower storage (the core's store) holds, a stack to a slot, six rows at a time, scrolled.
 * Click a stack to take it (right-click: half), click with something held to put it in (right-click: one), shift-click
 * between the grid and your inventory, Q to drop. Anything goes in as long as it's a plain item (no name, enchantment or
 * damage: the store keeps items and counts) and it fits ({@link TowerCoreBlockEntity#anyRoom}). LOAD BLOCKS puts in
 * the tower blocks you carry that the tower still needs, as before. What's in counts for drop pods and the tiers, with
 * everything else the tower pays from (TowerSupply).
 *
 * <p>The grid's slots aren't a real container: the server works every click on the store itself
 * ({@link #clicked}) and sends the slots as they then are; the client only shows them.
 */
public class TowerStorageMenu extends AbstractContainerMenu {
	public static final int COLS = 9, ROWS = 6, GRID = COLS * ROWS;
	/** The screen's size and where its slots are (GUI pixels, as a large chest's, with the tower screen's header). */
	public static final int WIDTH = 196, HEIGHT = 240, GRID_X = 8, GRID_Y = 36, INV_X = 8, INV_Y = 158, HOTBAR_Y = 216;
	/** Buttons: LOAD BLOCKS; scrolled to a row ({@link #SCROLL} + the row). */
	public static final int LOAD_BLOCKS = 0, SCROLL = 1000;
	public static final double REACH = 8;
	private final Player player;
	public final BlockPos core;
	public final BlockPos access;
	private final Container grid;
	private int row;
	private int stacks;
	private @Nullable List<ItemStack> view;

	/** What the client needs to open it. */
	public record Opening(BlockPos core, BlockPos access) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Opening> STREAM_CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, Opening::core, BlockPos.STREAM_CODEC, Opening::access, Opening::new);
	}

	public TowerStorageMenu(int id, Inventory inventory, Opening opening) {
		super(JugcraftTower.STORAGE_MENU, id);
		this.player = inventory.player;
		this.core = opening.core();
		this.access = opening.access();
		this.grid = player.level().isClientSide() ? new SimpleContainer(GRID) : new View();
		for (int r = 0; r < ROWS; r++) {
			for (int c = 0; c < COLS; c++) {
				addSlot(new GridSlot(grid, r * COLS + c, GRID_X + c * 18, GRID_Y + r * 18));
			}
		}
		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 9; c++) {
				addSlot(new Slot(inventory, 9 + r * 9 + c, INV_X + c * 18, INV_Y + r * 18));
			}
		}
		for (int c = 0; c < 9; c++) {
			addSlot(new Slot(inventory, c, INV_X + c * 18, HOTBAR_Y));
		}
		addDataSlot(new DataSlot() {
			@Override
			public int get() {
				return row;
			}

			@Override
			public void set(int value) {
				row = value;
			}
		});
		addDataSlot(new DataSlot() {
			@Override
			public int get() {
				return player.level().isClientSide() ? stacks : view().size();
			}

			@Override
			public void set(int value) {
				stacks = value;
			}
		});
	}

	/** Opens the tower storage's screen at the Cargo Exchange part {@code access}. */
	public static void open(ServerPlayer player, TowerCoreBlockEntity core, BlockPos access) {
		BlockPos at = core.getBlockPos().immutable();
		BlockPos from = access.immutable();
		player.openMenu(new ExtendedMenuProvider<Opening>() {
			@Override
			public Opening getScreenOpeningData(ServerPlayer viewer) {
				return new Opening(at, from);
			}

			@Override
			public Component getDisplayName() {
				return Component.translatable("screen.jugcraft.tower_load.storage_title");
			}

			@Override
			public AbstractContainerMenu createMenu(int id, Inventory inventory, Player viewer) {
				return new TowerStorageMenu(id, inventory, new Opening(at, from));
			}
		});
	}

	/** The core this screen shows, as this side has it (null when it is gone). */
	public @Nullable TowerCoreBlockEntity tower() {
		return player.level().isLoaded(core) && player.level().getBlockEntity(core) instanceof TowerCoreBlockEntity t ? t : null;
	}

	/** The row at the grid's top. */
	public int row() {
		return row;
	}

	/** How many stacks the tower storage's contents make (the grid's whole length). */
	public int stacks() {
		return stacks;
	}

	/** The rows the grid can scroll down past its first. */
	public static int maxRow(int stacks) {
		return Math.max(0, (stacks + COLS - 1) / COLS - ROWS);
	}

	/** The store's contents as stacks, in the items' registry order (the server's side). */
	List<ItemStack> view() {
		if (view == null) {
			List<ItemStack> out = new ArrayList<>();
			TowerCoreBlockEntity t = tower();
			if (t != null) {
				List<Map.Entry<Item, Integer>> entries = new ArrayList<>(t.store().entrySet());
				entries.sort(Comparator.comparingInt(e -> BuiltInRegistries.ITEM.getId(e.getKey())));
				for (Map.Entry<Item, Integer> e : entries) {
					int max = Math.max(1, e.getKey().getDefaultMaxStackSize());
					for (int left = e.getValue(); left > 0; left -= max) {
						out.add(new ItemStack(e.getKey(), Math.min(max, left)));
					}
				}
			}
			view = out;
		}
		return view;
	}

	@Override
	public void broadcastChanges() {
		view = null;                                             // (the store may have changed since: pipes, the drones)
		row = Math.min(row, maxRow(view().size()));
		super.broadcastChanges();
	}

	@Override
	public boolean stillValid(Player viewer) {
		TowerCoreBlockEntity t = tower();
		return t != null && !viewer.isSpectator() && t.mayUse(viewer) && !t.firstLayer()
				&& viewer.distanceToSqr(Vec3.atCenterOf(access)) <= REACH * REACH;
	}

	@Override
	public void clicked(int slot, int button, ContainerInput input, Player viewer) {
		if (slot >= 0 && slot < GRID) {
			if (!viewer.level().isClientSide()) {
				gridClick(slot, button, input, viewer);
				view = null;
			}
			return;
		}
		super.clicked(slot, button, input, viewer);
	}

	/** A click on the grid (the server's side), worked on the store itself. */
	private void gridClick(int slot, int button, ContainerInput input, Player viewer) {
		TowerCoreBlockEntity t = tower();
		if (t == null || !t.mayUse(viewer)) {
			return;
		}
		ItemStack shown = grid.getItem(slot);
		ItemStack carried = getCarried();
		switch (input) {
			case PICKUP -> {
				if (carried.isEmpty()) {
					if (!shown.isEmpty()) {
						int took = t.take(shown.getItem(), button == 0 ? shown.getCount() : (shown.getCount() + 1) / 2);
						if (took > 0) {
							setCarried(new ItemStack(shown.getItem(), took));
						}
					}
				} else if (button == 0) {
					if (t.depositAny(carried) == 0) {
						refused(viewer, t, carried);
					}
				} else {
					ItemStack one = carried.copyWithCount(1);
					if (t.depositAny(one) > 0) {
						carried.shrink(1);
					} else {
						refused(viewer, t, carried);
					}
				}
			}
			case QUICK_MOVE -> {
				if (shown.isEmpty()) {
					return;
				}
				ItemStack moving = shown.copy();
				int before = moving.getCount();
				moveItemStackTo(moving, GRID, slots.size(), true);
				int moved = before - moving.getCount();
				if (moved > 0) {
					t.take(shown.getItem(), moved);
				}
			}
			case THROW -> {
				if (!shown.isEmpty() && carried.isEmpty()) {
					int took = t.take(shown.getItem(), button == 0 ? 1 : shown.getCount());
					if (took > 0) {
						viewer.drop(new ItemStack(shown.getItem(), took), true, net.minecraft.util.Prediction.SERVER_ONLY);
					}
				}
			}
			case CLONE -> {
				if (viewer.hasInfiniteMaterials() && carried.isEmpty() && !shown.isEmpty()) {
					setCarried(shown.copyWithCount(shown.getMaxStackSize()));
				}
			}
			default -> {
			}
		}
	}

	/** Why {@code stack} didn't go in: it isn't a plain item, or the tower storage is full. */
	private static void refused(Player viewer, TowerCoreBlockEntity t, ItemStack stack) {
		viewer.sendOverlayMessage(Component.translatable(!stack.getComponentsPatch().isEmpty() ? "message.jugcraft.tower.storage_plain"
				: "message.jugcraft.tower.storage_full"));
	}

	@Override
	public ItemStack quickMoveStack(Player viewer, int index) {
		if (index < GRID || index >= slots.size()) {
			return ItemStack.EMPTY;
		}
		Slot slot = slots.get(index);
		if (!slot.hasItem() || viewer.level().isClientSide()) {
			return ItemStack.EMPTY;
		}
		TowerCoreBlockEntity t = tower();
		if (t == null || !t.mayUse(viewer)) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		if (t.depositAny(stack) > 0) {
			slot.setChanged();
			view = null;
		} else {
			refused(viewer, t, stack);
		}
		return ItemStack.EMPTY;
	}

	@Override
	public boolean canDragTo(Slot slot) {
		return !(slot instanceof GridSlot);
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
		return !(slot instanceof GridSlot);
	}

	@Override
	public boolean clickMenuButton(Player viewer, int id) {
		if (!(viewer instanceof ServerPlayer server) || !stillValid(viewer)) {
			return false;
		}
		if (id >= SCROLL) {
			row = Math.max(0, Math.min(id - SCROLL, maxRow(view().size())));
			broadcastChanges();
			return true;
		}
		TowerCoreBlockEntity t = tower();
		if (id == LOAD_BLOCKS && t != null) {
			int taken = t.depositToStorageFrom(server);
			server.sendOverlayMessage(taken > 0 ? Component.translatable("message.jugcraft.tower.deposited_blocks", taken)
					: Component.translatable(t.storageStacksUsed() >= TowerCoreBlockEntity.STORAGE_STACKS
							? "message.jugcraft.tower.storage_full" : "message.jugcraft.tower.nothing_needed"));
			view = null;
			broadcastChanges();
			return true;
		}
		return false;
	}

	/** A slot of the grid: shows a stack of the tower storage (clicks are the menu's, {@link #clicked}). */
	public static final class GridSlot extends Slot {
		GridSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}
	}

	/** The server's grid: the stacks of {@link #view} from the scrolled row. Never written to: clicks change the store. */
	private final class View implements Container {
		@Override
		public int getContainerSize() {
			return GRID;
		}

		@Override
		public boolean isEmpty() {
			return view().isEmpty();
		}

		@Override
		public ItemStack getItem(int slot) {
			List<ItemStack> v = view();
			int i = row * COLS + slot;
			return i >= 0 && i < v.size() ? v.get(i) : ItemStack.EMPTY;
		}

		@Override
		public ItemStack removeItem(int slot, int count) {
			return ItemStack.EMPTY;
		}

		@Override
		public ItemStack removeItemNoUpdate(int slot) {
			return ItemStack.EMPTY;
		}

		@Override
		public void setItem(int slot, ItemStack stack) {
		}

		@Override
		public void setChanged() {
		}

		@Override
		public boolean stillValid(Player viewer) {
			return true;
		}

		@Override
		public void clearContent() {
		}
	}
}
