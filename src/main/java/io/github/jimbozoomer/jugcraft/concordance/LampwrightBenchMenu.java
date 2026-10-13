package io.github.jimbozoomer.jugcraft.concordance;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The Lampwright's Bench screen: the specimen dish and the work slot at the left, the actions at the right. The data
 * carries the viewer's own status for each action ({@link BenchStatus}), so the screen shows what each needs; a button
 * press reaches {@link LampwrightBenchBlockEntity#press} on the server, which checks it again.
 */
public class LampwrightBenchMenu extends AbstractContainerMenu {
	public static final int SPECIMEN_X = 26;
	public static final int SPECIMEN_Y = 24;
	public static final int WORK_X = 26;
	public static final int WORK_Y = 52;

	private final Container container;
	private final ContainerData data;
	private final @Nullable LampwrightBenchBlockEntity bench;

	/** Client-side constructor: contents arrive through slot and data syncing. */
	public LampwrightBenchMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(LampwrightBenchBlockEntity.SLOTS), null,
				new SimpleContainerData(LampwrightBenchBlockEntity.DATA_COUNT));
	}

	public LampwrightBenchMenu(int containerId, Inventory inventory, Container container, @Nullable LampwrightBenchBlockEntity bench,
			ContainerData data) {
		super(JugcraftConcordance.BENCH_MENU, containerId);
		checkContainerSize(container, LampwrightBenchBlockEntity.SLOTS);
		checkContainerDataCount(data, LampwrightBenchBlockEntity.DATA_COUNT);
		this.container = container;
		this.data = data;
		this.bench = bench;
		addSlot(new Slot(container, LampwrightBenchBlockEntity.SPECIMEN, SPECIMEN_X, SPECIMEN_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(JugcraftConcordance.SPECIMENS);
			}
		});
		addSlot(new Slot(container, LampwrightBenchBlockEntity.WORK, WORK_X, WORK_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return container.canPlaceItem(LampwrightBenchBlockEntity.WORK, stack);
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}
		});
		addStandardInventorySlots(inventory, 8, 84);
		addDataSlots(data);
	}

	public BenchStatus status(int button) {
		return BenchStatus.fromIndex(data.get(LampwrightBenchBlockEntity.DATA_STATUS + button));
	}

	/** Study progress from 0 to {@code width}. */
	public int progress(int width) {
		return data.get(LampwrightBenchBlockEntity.DATA_PROGRESS) * width / LampwrightBenchBlockEntity.STUDY_TICKS;
	}

	/** 0 when idle, 1 while someone else's study runs, 2 while the viewer's own does. */
	public int studying() {
		return data.get(LampwrightBenchBlockEntity.DATA_STUDYING);
	}

	/** The Radiance in the lantern in the work slot, or -1 when there is no Kindled Lantern there. */
	public int charge() {
		return data.get(LampwrightBenchBlockEntity.DATA_CHARGE);
	}

	public int channelFocus() {
		return data.get(LampwrightBenchBlockEntity.DATA_CHANNEL_FOCUS);
	}

	/** A screen button (server side: the bench checks and runs it). */
	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id < 0 || id >= LampwrightBenchBlockEntity.BUTTONS) {
			return false;
		}
		// A button at most every 4 ticks a player (roadmap step 28): a held key or a modified client does no more.
		if (bench != null && player instanceof ServerPlayer server && RateGate.allow(server, "bench", 4)) {
			bench.press(server, id);
		}
		return true;
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		int benchSlots = LampwrightBenchBlockEntity.SLOTS;
		Slot slot = slots.get(slotIndex);
		if (slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (slotIndex < benchSlots) {
			if (!moveItemStackTo(stack, benchSlots, benchSlots + 36, true)) {
				return ItemStack.EMPTY;
			}
		} else if (stack.is(JugcraftConcordance.SPECIMENS)) {
			if (!moveItemStackTo(stack, LampwrightBenchBlockEntity.SPECIMEN, LampwrightBenchBlockEntity.SPECIMEN + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (!container.canPlaceItem(LampwrightBenchBlockEntity.WORK, stack)
				|| !moveItemStackTo(stack, LampwrightBenchBlockEntity.WORK, LampwrightBenchBlockEntity.WORK + 1, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		if (stack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTake(player, stack);
		return original;
	}
}
