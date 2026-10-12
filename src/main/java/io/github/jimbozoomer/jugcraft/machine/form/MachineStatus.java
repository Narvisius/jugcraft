package io.github.jimbozoomer.jugcraft.machine.form;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * What a formed machine is doing and, when it is stuck, the one thing to fix, naming the tank, slot, socket or
 * position concerned: "Lye tank is full", "Bed socket needs a Nickel Catalyst Bed". Five small numbers, so it syncs to
 * the screen within the menu's fixed data slots.
 *
 * @param state the lifecycle state
 * @param reason why, when the state needs one
 * @param index the tank (inputs first), container slot, socket or envelope cell concerned, or -1
 * @param subject the raw registry id of the fluid or item concerned, or 0
 * @param amount millibuckets, items or JE per tick, as the reason says
 */
public record MachineStatus(MachineLifecycle state, Reason reason, int index, int subject, int amount) {
	public enum Reason {
		NONE, PART_MISSING, CLEARANCE_BLOCKED, PAUSED, EMPTY, MISSING_FLUID, MISSING_ITEM, UNUSED_FLUID, UNUSED_ITEM,
		TOOL_REQUIRED, OUTPUT_FULL, OUTPUT_OTHER_FLUID, OUTPUT_SLOT_FULL, NO_ENERGY,
		/** A generator's fuel tank is empty and it has nothing left in hand. */
		NO_FUEL,
		/** A generator's store is full: nothing is drawing its power. */
		POWER_FULL;

		public String translationKey() {
			return "container.jugcraft.form.reason." + name().toLowerCase();
		}

		public static Reason byOrdinal(int ordinal) {
			Reason[] values = values();
			return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NONE;
		}
	}

	public static final MachineStatus IDLE = of(MachineLifecycle.IDLE, Reason.EMPTY);
	public static final MachineStatus PROCESSING = of(MachineLifecycle.PROCESSING, Reason.NONE);
	public static final MachineStatus WARMING = of(MachineLifecycle.WARMING, Reason.NONE);
	public static final MachineStatus PAUSED = of(MachineLifecycle.IDLE, Reason.PAUSED);
	/** Values a status takes in the menu's synced data. */
	public static final int DATA_VALUES = 5;

	public static MachineStatus of(MachineLifecycle state, Reason reason) {
		return new MachineStatus(state, reason, -1, 0, 0);
	}

	public static MachineStatus fluid(MachineLifecycle state, Reason reason, int tank, Fluid fluid, int mb) {
		return new MachineStatus(state, reason, tank, BuiltInRegistries.FLUID.getId(fluid), mb);
	}

	public static MachineStatus item(MachineLifecycle state, Reason reason, int slot, Item item, int count) {
		return new MachineStatus(state, reason, slot, BuiltInRegistries.ITEM.getId(item), count);
	}

	public static MachineStatus at(MachineLifecycle state, Reason reason, int index) {
		return new MachineStatus(state, reason, index, 0, 0);
	}

	/** One of the five synced values. */
	public int data(int value) {
		return switch (value) {
			case 0 -> state.ordinal();
			case 1 -> reason.ordinal();
			case 2 -> index + 1;
			case 3 -> subject;
			default -> amount;
		};
	}

	/** Rebuilds a status from the five synced values (each arrives as 16 bits). */
	public static MachineStatus fromData(int state, int reason, int index, int subject, int amount) {
		return new MachineStatus(MachineLifecycle.byOrdinal(state & 0xFFFF), Reason.byOrdinal(reason & 0xFFFF),
				(index & 0xFFFF) - 1, subject & 0xFFFF, amount & 0xFFFF);
	}

	/** The state line, such as "Output blocked". */
	public Component title() {
		return Component.translatable(state.translationKey());
	}

	/** The state line as {@code form} words it: a generator at work is "Generating" rather than "Processing". */
	public Component title(MachineForm form) {
		return form.generator() && state == MachineLifecycle.PROCESSING
				? Component.translatable("container.jugcraft.form.state.generating") : title();
	}

	/** The reason line, such as "Lye tank is full", or empty when there is nothing to fix. */
	public Component detail(MachineForm form) {
		if (reason == Reason.NONE) {
			return Component.empty();
		}
		Component what = switch (reason) {
			case MISSING_FLUID, UNUSED_FLUID, OUTPUT_FULL, OUTPUT_OTHER_FLUID, NO_FUEL -> tankLabel(form, index);
			case MISSING_ITEM, UNUSED_ITEM, OUTPUT_SLOT_FULL -> slotLabel(form, index);
			case TOOL_REQUIRED -> socketLabel(form, index);
			case PART_MISSING, CLEARANCE_BLOCKED -> cellLabel(form, index);
			default -> Component.empty();
		};
		Component name = switch (reason) {
			case MISSING_FLUID, UNUSED_FLUID, OUTPUT_OTHER_FLUID -> fluidName(subject);
			case MISSING_ITEM, UNUSED_ITEM, TOOL_REQUIRED -> itemName(subject);
			default -> Component.empty();
		};
		return Component.translatable(reason.translationKey(), what, name, amount);
	}

	/** A tank's label from its role: "Hydrogen tank", or a translation of it when one is given. */
	public static Component tankLabel(MachineForm form, int tank) {
		if (tank < 0 || tank >= form.tankRoles().size()) {
			return Component.literal("?");
		}
		String role = form.tankRoles().get(tank);
		return Component.translatableWithFallback("container.jugcraft.form.tank." + role, capitalize(role) + " tank");
	}

	public static Component slotLabel(MachineForm form, int slot) {
		boolean input = slot < form.firstOutputSlot();
		int number = (input ? slot : slot - form.firstOutputSlot()) + 1;
		return Component.translatable(input ? "container.jugcraft.form.input_slot" : "container.jugcraft.form.output_slot", number);
	}

	public static Component socketLabel(MachineForm form, int socket) {
		if (socket < 0 || socket >= form.sockets().size()) {
			return Component.literal("?");
		}
		String name = form.sockets().get(socket).name();
		return Component.translatableWithFallback("container.jugcraft.form.socket." + name, capitalize(name) + " socket");
	}

	/** An envelope position as the player counts it: column from the left, row from the front, layer from the ground. */
	public static Component cellLabel(MachineForm form, int cell) {
		if (cell < 0 || cell >= form.positions()) {
			return Component.literal("?");
		}
		return Component.translatable("container.jugcraft.form.cell", form.column(cell) + 1, form.row(cell) + 1, form.layer(cell) + 1);
	}

	private static Component fluidName(int id) {
		Fluid fluid = BuiltInRegistries.FLUID.byId(id);
		return fluid == null ? Component.literal("?") : FluidVariantAttributes.getName(FluidVariant.of(fluid));
	}

	private static Component itemName(int id) {
		Item item = BuiltInRegistries.ITEM.byId(id);
		return item == null ? Component.literal("?") : new ItemStack(item).getHoverName();
	}

	private static String capitalize(String name) {
		String spaced = name.replace('_', ' ');
		return spaced.isEmpty() ? spaced : Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
	}
}
