package io.github.jimbozoomer.jugcraft.crafting;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** The Advanced Crafting Table's 6 x 6 grid for recipe matching: slot {@code y * 6 + x}, row 0 the top (far) row. */
public record AdvancedCraftingInput(List<ItemStack> stacks) implements RecipeInput {
	public static final int SIDE = 6;

	@Override
	public ItemStack getItem(int slot) {
		return stacks.get(slot);
	}

	@Override
	public int size() {
		return stacks.size();
	}

	/** The stack at column {@code x}, row {@code y}. */
	public ItemStack at(int x, int y) {
		return stacks.get(y * SIDE + x);
	}
}
