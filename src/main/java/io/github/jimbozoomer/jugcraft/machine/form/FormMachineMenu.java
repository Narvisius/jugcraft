package io.github.jimbozoomer.jugcraft.machine.form;

import io.github.jimbozoomer.jugcraft.chemistry.FluidMachineSpec;
import io.github.jimbozoomer.jugcraft.machine.MachineMenu;
import io.github.jimbozoomer.jugcraft.machine.MachineUpgrades;
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
 * The screen of every formed machine, laid out from its form: the energy bar, input tanks and slots on the left,
 * output slots and tanks on the right with the progress arrow between, and the tool sockets and upgrade slots in the
 * terminal beside the bay. Everything shown arrives through slot syncing and {@link FormMachineBlockEntity}'s fixed
 * data slots; the client never decides anything.
 */
public class FormMachineMenu extends AbstractContainerMenu {
	/** Tool sockets and upgrade slots: rows of four in the terminal's lower panel. */
	public static final int TOOL_X = 186;
	public static final int TOOL_Y = 106;
	public static final int TOOLS_PER_ROW = 4;

	public static int toolX(int index) {
		return TOOL_X + (index % TOOLS_PER_ROW) * 18;
	}

	public static int toolY(int index) {
		return TOOL_Y + (index / TOOLS_PER_ROW) * 18;
	}

	private final MachineForm form;
	private final Container container;
	private final ContainerData data;
    public static final int BUTTON_AUTO = 10;
    public static final int BUTTON_RECIPE = 100;
    private final java.util.List<io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes.Entry> catalog;
    private long lastSelectionTick = Long.MIN_VALUE;

    public java.util.List<io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes.Entry> catalog() { return catalog; }
    /** 0 automatic, 1-based catalog entry, 65535 unavailable. */
    public int selectedRecipe() { return data(FormMachineBlockEntity.DATA_COUNT); }
    public boolean catalogStale() { return data(FormMachineBlockEntity.DATA_COUNT + 1) != 0; }
    public MachineUpgrades.Effect upgrades() {
        var cards = new java.util.ArrayList<ItemStack>();
        for (int i = form.firstUpgradeSlot(); i < form.containerSize(); i++) cards.add(container.getItem(i));
        return MachineUpgrades.effect(cards);
    }
	/** The machine itself, on the server only. */
	private final @Nullable FormMachineBlockEntity machine;

	/** Client side: contents arrive through slot and data syncing. */
	public FormMachineMenu(int containerId, Inventory inventory, MachineForm form) {
        this(containerId, inventory, form, java.util.List.of());
    }

    public FormMachineMenu(int containerId, Inventory inventory, MachineForm form,
            java.util.List<io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes.Entry> catalog) {
        this(containerId, inventory, form, new SimpleContainer(form.containerSize()),
                new SimpleContainerData(FormMachineBlockEntity.DATA_COUNT + 2), null, catalog);
	}

	public FormMachineMenu(int containerId, Inventory inventory, FormMachineBlockEntity machine, ContainerData data) {
		this(containerId, inventory, machine.form(), machine.container(), data, machine, machine.form().generator() ? java.util.List.of() : machine.recipes(inventory.player.level().getServer()));
	}

	private FormMachineMenu(int containerId, Inventory inventory, MachineForm form, Container container, ContainerData data,
			@Nullable FormMachineBlockEntity machine, java.util.List<io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes.Entry> catalog) {
		super(JugcraftForms.MENU, containerId);
		checkContainerSize(container, form.containerSize());
		checkContainerDataCount(data, machine == null ? FormMachineBlockEntity.DATA_COUNT + 2 : FormMachineBlockEntity.DATA_COUNT);
		this.form = form;
		this.container = container;
		this.catalog = catalog;
        this.data = machine == null ? data : new ContainerData() {
            public int get(int index) {
                if (index == FormMachineBlockEntity.DATA_COUNT) {
                    if (machine.recipeLock() == null) return 0;
                    for (int i = 0; i < catalog.size(); i++) if (catalog.get(i).id().equals(machine.recipeLock())) return i + 1;
                    return 65535;
                }
                if (index == FormMachineBlockEntity.DATA_COUNT + 1)
                    return !form.generator() && machine.recipes(inventory.player.level().getServer()) != catalog ? 1 : 0;
                return data.get(index);
            }
            public void set(int index, int value) { }
            public int getCount() { return FormMachineBlockEntity.DATA_COUNT + 2; }
        };
		this.machine = machine;
		FluidMachineSpec spec = form.tanks();

		for (int slot = 0; slot < form.itemInputs(); slot++) {
			addSlot(new Slot(container, slot, MachineMenu.fluidItemInputX(spec, slot), MachineMenu.SLOT_Y) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return container.canPlaceItem(getContainerSlot(), stack);
				}
			});
		}
		for (int slot = 0; slot < form.itemOutputs(); slot++) {
			addSlot(new Slot(container, form.firstOutputSlot() + slot, MachineMenu.fluidItemOutputX(spec, slot), MachineMenu.SLOT_Y) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return false;
				}
			});
		}
		for (int socket = 0; socket < form.sockets().size(); socket++) {
			// Not "index": inside the slot that name is Slot's own field (its place in the menu), not the socket.
			int number = socket;
			addSlot(new Slot(container, form.firstSocketSlot() + socket, toolX(socket), toolY(socket)) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return form.sockets().get(number).accepts(stack) && !locked(number);
				}

				@Override
				public boolean mayPickup(Player player) {
					return !locked(number);
				}

				@Override
				public int getMaxStackSize() {
					return 1;
				}
			});
		}
		for (int upgrade = 0; upgrade < form.upgradeSlots(); upgrade++) {
			int place = form.sockets().size() + upgrade;
			addSlot(new Slot(container, form.firstUpgradeSlot() + upgrade, toolX(place), toolY(place)) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return MachineUpgrades.isUpgrade(stack);
				}
			});
		}
		addStandardInventorySlots(inventory, 8, 84);
		addDataSlots(this.data);
	}

	public MachineForm form() {
		return form;
	}

	public int data(int index) {
		return data.get(index) & 0xFFFF;
	}

	/** Whether socket {@code socket} holds a running batch's tool. */
	public boolean locked(int socket) {
		return (data(FormMachineBlockEntity.DATA_LOCKS) & (1 << socket)) != 0;
	}

	public MachineStatus status() {
		int base = FormMachineBlockEntity.DATA_STATUS;
		return MachineStatus.fromData(data.get(base), data.get(base + 1), data.get(base + 2), data.get(base + 3), data.get(base + 4));
	}

	public boolean paused() {
		return data(FormMachineBlockEntity.DATA_PAUSED) != 0;
	}

	/** Whether the vented tanks hold their excess (stop instead) rather than release it. */
	public boolean holdsVented() {
		return data(FormMachineBlockEntity.DATA_HOLD_VENTED) != 0;
	}

	/** JE a tick one running lane pays; for a generator, the JE it made last tick. */
	public int laneUse() {
		return data(FormMachineBlockEntity.DATA_LANE_USE);
	}

	/** Paid ticks of lane {@code lane}, and the ticks its batch needs (0 when the lane is free). */
	public int laneProgress(int lane) {
		return data(FormMachineBlockEntity.DATA_LANES + 2 * lane);
	}

	public int laneDuration(int lane) {
		return data(FormMachineBlockEntity.DATA_LANES + 2 * lane + 1);
	}

	/** The registry id of the fluid in tank {@code tank} (0 when empty), its millibuckets and what is reserved there. */
	public int tankFluid(int tank) {
		return data(FormMachineBlockEntity.DATA_TANKS + 2 * tank);
	}

	public int tankAmount(int tank) {
		return data(FormMachineBlockEntity.DATA_TANKS + 2 * tank + 1);
	}

	public int tankReserved(int tank) {
		return data(FormMachineBlockEntity.DATA_RESERVED + tank);
	}

	public long energy() {
		return ((long) data(FormMachineBlockEntity.DATA_ENERGY_HIGH) << 16) | data(FormMachineBlockEntity.DATA_ENERGY_LOW);
	}

	public long capacity() {
		return ((long) data(FormMachineBlockEntity.DATA_CAPACITY_HIGH) << 16) | data(FormMachineBlockEntity.DATA_CAPACITY_LOW);
	}

	/** Buttons: pause or resume, and cancel the running batches. Only the server applies them. */
	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (machine == null || !(player instanceof ServerPlayer server) || server.containerMenu != this
                || !stillValid(player) || player.isSpectator()
                || !server.level().mayInteract(player, machine.getBlockPos())
                || !player.mayUseItemAt(machine.getBlockPos(), net.minecraft.core.Direction.UP, ItemStack.EMPTY)
                || io.github.jimbozoomer.jugcraft.town.TownProtection.denies(player, server.level(), machine.getBlockPos())) return false;
        if (id == BUTTON_AUTO || id >= BUTTON_RECIPE) {
            if (catalogStale() || machine.form().generator()) return false;
            long now = server.level().getGameTime();
            if (lastSelectionTick == now) return false;
            int index = id - BUTTON_RECIPE;
            if (id != BUTTON_AUTO && (index < 0 || index >= catalog.size())) return false;
            boolean accepted = machine.selectRecipe(id == BUTTON_AUTO ? null : catalog.get(index).id());
            if (accepted) { lastSelectionTick = now; broadcastChanges(); }
            return accepted;
        }
        return machine.clickButton(server, id);
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}

	/** Shift-click: upgrades into the upgrade slots, tools into their sockets, everything else into the inputs. */
	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		int machineSlots = form.containerSize();
		int playerEnd = machineSlots + 36;
		Slot slot = slots.get(slotIndex);
		if (slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (slotIndex < machineSlots) {
			if (!moveItemStackTo(stack, machineSlots, playerEnd, true)) {
				return ItemStack.EMPTY;
			}
		} else if (!moveIntoMachine(stack)) {
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

	private boolean moveIntoMachine(ItemStack stack) {
		if (MachineUpgrades.isUpgrade(stack) && form.upgradeSlots() > 0) {
			return moveItemStackTo(stack, form.firstUpgradeSlot(), form.containerSize(), false);
		}
		for (int socket = 0; socket < form.sockets().size(); socket++) {
			int index = form.firstSocketSlot() + socket;
			if (slots.get(index).mayPlace(stack) && !slots.get(index).hasItem()) {
				return moveItemStackTo(stack, index, index + 1, false);
			}
		}
		for (int input = 0; input < form.itemInputs(); input++) {
			if (slots.get(input).mayPlace(stack) && moveItemStackTo(stack, input, input + 1, false)) {
				return true;
			}
		}
		return false;
	}
}
