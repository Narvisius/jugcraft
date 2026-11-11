package io.github.jimbozoomer.jugcraft.tower;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Loading the Drone Tower (Justin, 28 October 2026; the approved mockup tower_core_loading_mockup): the blocks needed,
 * what is in, a slot to put blocks in, LOAD ALL, and the player's inventory under it (click or shift-click stacks in).
 * Only what is still needed goes in; the rest stays with the player. Two uses of one screen
 * (client/TowerLoadScreen):
 * <ul>
 * <li>the core's, for the first layer (the foundation and tier 1 together): opened as the core is placed (after its
 * warning, TowerPlacement) and by right-clicking it until tier 1 starts; BUILD FIRST LAYER builds the foundation and
 * goes straight on to tier 1 ({@link TowerCoreBlockEntity#buildFirstLayer});</li>
 * <li>the Cargo Exchange's, after that: the tower storage (10 double chests, {@link TowerCoreBlockEntity#STORAGE_STACKS}),
 * which takes the tower's blocks up to what the rest of the tower needs; the next tier is listed and UPGRADE is at the
 * Tower Control console.</li>
 * </ul>
 * The counts come from the core's own data on the client (its store is synced), so nothing else is sent.
 */
public class TowerLoadMenu extends AbstractContainerMenu {
	/** The screen's size and where its slots are (the mockup's layout, GUI pixels; a slot's item 1 px inside its box). */
	public static final int WIDTH = 340, HEIGHT = 262, INPUT_X = 9, INPUT_Y = 145, INV_X = 90, INV_Y = 179, HOTBAR_Y = 237;
	public static final int LOAD_ALL = 0, BUILD = 1;
	/** How far from the core (or the exchange it was opened at) the screen stays open (blocks). */
	public static final double REACH = 8;
	private final Player player;
	public final BlockPos core;
	/** The tower storage at the Cargo Exchange (rather than the core's first layer). */
	public final boolean storage;
	/** Where it was opened: the core, or the Cargo Exchange's part that was clicked. */
	public final BlockPos access;

	/** What the client needs to open it. */
	public record Opening(BlockPos core, boolean storage, BlockPos access) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Opening> STREAM_CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, Opening::core, ByteBufCodecs.BOOL, Opening::storage, BlockPos.STREAM_CODEC, Opening::access,
				Opening::new);
	}

	public TowerLoadMenu(int id, Inventory inventory, Opening opening) {
		super(JugcraftTower.LOAD_MENU, id);
		this.player = inventory.player;
		this.core = opening.core();
		this.storage = opening.storage();
		this.access = opening.access();
		addSlot(new InputSlot(new SimpleContainer(1), this));
		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 9; c++) {
				addSlot(new Slot(inventory, 9 + r * 9 + c, INV_X + c * 18, INV_Y + r * 18));
			}
		}
		for (int c = 0; c < 9; c++) {
			addSlot(new Slot(inventory, c, INV_X + c * 18, HOTBAR_Y));
		}
	}

	/** Opens the core's first-layer screen for {@code player}. */
	public static void openFirstLayer(ServerPlayer player, TowerCoreBlockEntity core) {
		open(player, core, false, core.getBlockPos());
	}

	/** Opens the tower storage's screen at the Cargo Exchange part {@code access}. */
	public static void openStorage(ServerPlayer player, TowerCoreBlockEntity core, BlockPos access) {
		open(player, core, true, access);
	}

	private static void open(ServerPlayer player, TowerCoreBlockEntity core, boolean storage, BlockPos access) {
		BlockPos at = core.getBlockPos().immutable();
		BlockPos from = access.immutable();
		player.openMenu(new ExtendedMenuProvider<Opening>() {
			@Override
			public Opening getScreenOpeningData(ServerPlayer viewer) {
				return new Opening(at, storage, from);
			}

			@Override
			public Component getDisplayName() {
				return Component.translatable(storage ? "screen.jugcraft.tower_load.storage_title" : "screen.jugcraft.tower_load.title");
			}

			@Override
			public AbstractContainerMenu createMenu(int id, Inventory inventory, Player viewer) {
				return new TowerLoadMenu(id, inventory, new Opening(at, storage, from));
			}
		});
	}

	/** The core this screen loads, as this side has it (null when it is gone). */
	public @Nullable TowerCoreBlockEntity tower() {
		return player.level().isLoaded(core) && player.level().getBlockEntity(core) instanceof TowerCoreBlockEntity t ? t : null;
	}

	/** Whether {@code stack} (or some of it) would go in. */
	boolean takes(ItemStack stack) {
		TowerCoreBlockEntity t = tower();
		return t != null && (storage ? t.storageWants(stack) : t.wants(stack));
	}

	/** How many of {@code stack} would go in. */
	int room(ItemStack stack) {
		TowerCoreBlockEntity t = tower();
		if (t == null || !takes(stack)) {
			return 0;
		}
		return storage ? t.storageRoom(stack.getItem()) : t.room(stack.getItem());
	}

	/** Puts what goes in of {@code stack} in (the server's side only). Returns how many went in. */
	int put(ItemStack stack) {
		TowerCoreBlockEntity t = tower();
		if (t == null || player.level().isClientSide() || !t.mayUse(player)) {
			return 0;
		}
		return storage ? t.depositToStorage(stack) : t.deposit(stack);
	}

	@Override
	public boolean stillValid(Player viewer) {
		TowerCoreBlockEntity t = tower();
		return t != null && !viewer.isSpectator() && t.mayUse(viewer) && t.firstLayer() != storage
				&& viewer.distanceToSqr(Vec3.atCenterOf(access)) <= REACH * REACH;
	}

	@Override
	public ItemStack quickMoveStack(Player viewer, int index) {
		Slot slot = slots.get(index);
		if (index == 0 || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		if (takes(stack)) {
			// shift-click: what is still needed goes in, the rest stays in the slot (the server decides; the client waits)
			if (!viewer.level().isClientSide()) {
				int went = put(stack);
				if (went > 0) {
					slot.setChanged();
				}
			}
			return ItemStack.EMPTY;
		}
		// anything else moves between the inventory and the hotbar, as in a chest's screen
		int hotbarStart = 1 + 27;
		boolean moved = index < hotbarStart ? moveItemStackTo(stack, hotbarStart, slots.size(), false)
				: moveItemStackTo(stack, 1, hotbarStart, false);
		if (!moved) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return ItemStack.EMPTY;
	}

	@Override
	public boolean clickMenuButton(Player viewer, int id) {
		if (!(viewer instanceof ServerPlayer server) || !stillValid(viewer)) {
			return false;
		}
		TowerCoreBlockEntity t = tower();
		if (t == null) {
			return false;
		}
		if (id == LOAD_ALL) {
			int taken;
			if (storage) {
				taken = t.depositToStorageFrom(server);
			} else if (server.hasInfiniteMaterials()) {
				int before = TowerCosts.total(t.store());
				t.fillFirstLayer();
				taken = TowerCosts.total(t.store()) - before;
			} else {
				taken = t.depositFrom(server);
			}
			server.sendOverlayMessage(taken > 0 ? Component.translatable("message.jugcraft.tower.deposited_blocks", taken)
					: Component.translatable(storage && t.storageStacksUsed() >= TowerCoreBlockEntity.STORAGE_STACKS
							? "message.jugcraft.tower.storage_full" : "message.jugcraft.tower.nothing_needed"));
			broadcastChanges();
			return true;
		}
		if (id == BUILD && !storage) {
			if (t.buildFirstLayer(server)) {
				server.closeContainer();
			}
			return true;
		}
		return false;
	}

	/**
	 * The PUT BLOCKS IN slot: holds nothing. A stack dropped on it puts in what is still needed (the rest stays on the
	 * cursor), a right-click one block; it takes only what goes in, so a stack never sits in it.
	 */
	static final class InputSlot extends Slot {
		private final TowerLoadMenu menu;

		InputSlot(Container container, TowerLoadMenu menu) {
			super(container, 0, INPUT_X, INPUT_Y);
			this.menu = menu;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return menu.takes(stack);
		}

		@Override
		public int getMaxStackSize(ItemStack stack) {
			return Math.max(0, Math.min(stack.getMaxStackSize(), menu.room(stack)));
		}

		@Override
		public void set(ItemStack stack) {
			if (!stack.isEmpty()) {
				menu.put(stack);
				if (!stack.isEmpty() && !menu.player.level().isClientSide()) {
					// the core took less than offered (it filled up meanwhile): the rest goes back to the player
					menu.player.getInventory().placeItemBackInInventory(stack, net.minecraft.util.Prediction.SERVER_ONLY);
				}
			}
			// the client keeps nothing either: its counts come from the core's data
			container.setItem(0, ItemStack.EMPTY);
			setChanged();
		}

		@Override
		public boolean mayPickup(Player player) {
			return false;
		}
	}
}
