package io.github.jimbozoomer.jugcraft.machine;

import io.github.jimbozoomer.jugcraft.materials.JugcraftMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Generator fuels (GENERATOR_FUELS and STEAM_FUELS in tools/machines.py). Coal burns as long as
 * in a vanilla furnace; charcoal three quarters as long, so tree farm charcoal is a little weaker wood
 * power (docs/BALANCE.md). The steam generator also accepts bitumen.
 */
public final class GeneratorFuels {
	private GeneratorFuels() {
	}

	public static int burnTicks(ItemStack stack) {
		if (stack.is(Items.COAL)) {
			return 1600;
		}
		if (stack.is(Items.CHARCOAL)) {
			return 1200;
		}
		if (stack.is(Items.COAL_BLOCK)) {
			return 16000;
		}
		if (JugcraftMaterials.COKE != null && stack.is(JugcraftMaterials.COKE)) {
			return 3200;
		}
		return 0;
	}

	public static int steamBurnTicks(ItemStack stack) {
		if (JugcraftMaterials.BITUMEN != null && stack.is(JugcraftMaterials.BITUMEN)) {
			return 800;
		}
		return burnTicks(stack);
	}
}
