package io.github.jimbozoomer.jugcraft.machine;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** The input slots of a multi-input machine (alloy smelter, circuit assembler), for recipe matching. */
public record MachineInput(List<ItemStack> stacks) implements RecipeInput {
	@Override
	public ItemStack getItem(int slot) {
		return stacks.get(slot);
	}

	@Override
	public int size() {
		return stacks.size();
	}
}
