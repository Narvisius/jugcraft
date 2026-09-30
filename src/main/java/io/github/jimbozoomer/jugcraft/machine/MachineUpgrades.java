package io.github.jimbozoomer.jugcraft.machine;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Machine upgrades, placed in a powered processor's two upgrade slots (UPGRADES in tools/machines.py).
 * At most {@link #MAX_EFFECTIVE} of each kind count.
 * <ul>
 * <li>Speed: each card divides the time by (1 + 0.5 per card) and raises the energy per item by 25%.
 * Four cards: 3x as fast for twice the energy per item.</li>
 * <li>Efficiency: each card cuts energy use by 20% (compounding). Four cards: 41% of the energy.</li>
 * </ul>
 */
public final class MachineUpgrades {
	public static final int MAX_EFFECTIVE = 4;
	public static Item SPEED;
	public static Item EFFICIENCY;

	private MachineUpgrades() {
	}

	public static void register() {
		SPEED = item("speed_upgrade");
		EFFICIENCY = item("efficiency_upgrade");
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
			output.accept(SPEED);
			output.accept(EFFICIENCY);
		});
	}

	private static Item item(String path) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(path));
		return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key).stacksTo(MAX_EFFECTIVE)));
	}

	public static boolean isUpgrade(ItemStack stack) {
		return SPEED != null && (stack.is(SPEED) || stack.is(EFFICIENCY));
	}

	/** What the cards in a machine's upgrade slots do. */
	public record Effect(int speed, int efficiency) {
		public static final Effect NONE = new Effect(0, 0);

		/** Ticks an operation takes. */
		public int ticks(int base) {
			return Math.max(1, (int) Math.ceil(base / (1 + 0.5 * speed)));
		}

		/** JE used per tick while working. */
		public long use(long base) {
			if (base <= 0) {
				return 0;
			}
			return Math.max(1, Math.round(base * (1 + 0.5 * speed) * (1 + 0.25 * speed) * Math.pow(0.8, efficiency)));
		}
	}

	public static Effect effect(List<ItemStack> slots) {
		int speed = 0;
		int efficiency = 0;
		for (ItemStack stack : slots) {
			if (SPEED != null && stack.is(SPEED)) {
				speed += stack.getCount();
			} else if (EFFICIENCY != null && stack.is(EFFICIENCY)) {
				efficiency += stack.getCount();
			}
		}
		return new Effect(Math.min(MAX_EFFECTIVE, speed), Math.min(MAX_EFFECTIVE, efficiency));
	}
}
