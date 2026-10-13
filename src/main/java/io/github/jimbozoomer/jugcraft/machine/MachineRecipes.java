package io.github.jimbozoomer.jugcraft.machine;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;

/**
 * Looks up machine recipes in the server's recipe manager (see {@link MachineRecipeTypes}).
 * Recipes come from data packs, so they reload with /reload; the per-machine lists used for slot
 * filtering are rebuilt lazily after each reload.
 */
public final class MachineRecipes {
	/** A multi-input recipe matched against the input slots, with how many items to take from each slot. */
	public record MultiMatch(MultiMachineRecipe recipe, int[] take) {
	}

	private static final Map<MachineKind, List<MultiMachineRecipe>> MULTI_CACHE = new EnumMap<>(MachineKind.class);

	private MachineRecipes() {
	}

	public static void register() {
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> clearCache());
		ServerLifecycleEvents.SERVER_STARTED.register(server -> clearCache());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> clearCache());
	}

	private static synchronized void clearCache() {
		MULTI_CACHE.clear();
		MachineCompanionPort.clearCatalog();
	}

	public static Optional<MachineRecipe> find(ServerLevel level, MachineKind kind, ItemStack input) {
		if (input.isEmpty() || MachineRecipeTypes.single(kind) == null) {
			return Optional.empty();
		}
		return level.getServer().getRecipeManager()
				.getRecipeFor(MachineRecipeTypes.single(kind), new SingleRecipeInput(input), level)
				.map(RecipeHolder::value);
	}

	/** Finds a multi-input recipe whose every ingredient sits (in enough quantity) in its own input slot; other slots must be empty. */
	public static Optional<MultiMatch> findMulti(ServerLevel level, MachineKind kind, List<ItemStack> inputs) {
		MachineInput input = new MachineInput(inputs);
		for (MultiMachineRecipe recipe : multiRecipes(level.getServer(), kind)) {
			int[] take = recipe.take(input);
			if (take != null) {
				return Optional.of(new MultiMatch(recipe, take));
			}
		}
		return Optional.empty();
	}

	/** Whether an item appears in any of this machine's multi-input recipes (for slot and hopper filtering). */
	public static boolean isMultiIngredient(MinecraftServer server, MachineKind kind, ItemStack stack) {
		for (MultiMachineRecipe recipe : multiRecipes(server, kind)) {
			if (recipe.uses(stack)) {
				return true;
			}
		}
		return false;
	}

	private static synchronized List<MultiMachineRecipe> multiRecipes(MinecraftServer server, MachineKind kind) {
		return MULTI_CACHE.computeIfAbsent(kind, k -> {
			List<MultiMachineRecipe> list = new ArrayList<>();
			for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
				if (holder.value() instanceof MultiMachineRecipe recipe && recipe.getType() == MachineRecipeTypes.multi(k)) {
					list.add(recipe);
				}
			}
			// The recipe with more ingredients wins: magnets with borax before magnets without it, whatever order the
			// recipes loaded in.
			list.sort(java.util.Comparator.comparingInt((MultiMachineRecipe recipe) -> recipe.parts().size()).reversed());
			return List.copyOf(list);
		});
	}
}
