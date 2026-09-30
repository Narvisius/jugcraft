package io.github.jimbozoomer.jugcraft.machine;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Coal generator fuels (GENERATOR_FUELS in tools/machines.py). Coal burns as long as in a vanilla furnace. */
public final class GeneratorFuels {
	private GeneratorFuels() {
	}

	public static int burnTicks(ItemStack stack) {
		if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
			return 1600;
		}
		if (stack.is(Items.COAL_BLOCK)) {
			return 16000;
		}
		return 0;
	}
}
