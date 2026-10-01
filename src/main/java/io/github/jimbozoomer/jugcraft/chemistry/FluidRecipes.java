package io.github.jimbozoomer.jugcraft.chemistry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * One recipe type and serializer per fluid processing machine, named by {@link MachineKind#recipeType()}, and the
 * lookups the machines use. Recipes come from data packs; the per-machine lists are rebuilt after each reload.
 */
public final class FluidRecipes {
	private static final Map<MachineKind, RecipeType<FluidRecipe>> TYPES = new EnumMap<>(MachineKind.class);
	private static final Map<MachineKind, RecipeSerializer<FluidRecipe>> SERIALIZERS = new EnumMap<>(MachineKind.class);
	private static final Map<MachineKind, List<FluidRecipe>> CACHE = new EnumMap<>(MachineKind.class);

	private FluidRecipes() {
	}

	public static void register() {
		for (MachineKind kind : MachineKind.values()) {
			String name = kind.recipeType();
			if (name == null || !kind.isFluidProcessor()) {
				continue;
			}
			TYPES.put(kind, Registry.register(BuiltInRegistries.RECIPE_TYPE, Jugcraft.id(name), new RecipeType<FluidRecipe>() {
				@Override
				public String toString() {
					return Jugcraft.MOD_ID + ":" + name;
				}
			}));
			SERIALIZERS.put(kind, Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Jugcraft.id(name), FluidRecipe.serializer(kind)));
		}
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> clearCache());
		ServerLifecycleEvents.SERVER_STARTED.register(server -> clearCache());
	}

	private static synchronized void clearCache() {
		CACHE.clear();
	}

	static RecipeType<FluidRecipe> type(MachineKind kind) {
		return TYPES.get(kind);
	}

	static RecipeSerializer<FluidRecipe> serializer(MachineKind kind) {
		return SERIALIZERS.get(kind);
	}

	/** Every recipe of this machine, in load order. */
	public static synchronized List<FluidRecipe> recipes(MinecraftServer server, MachineKind kind) {
		return CACHE.computeIfAbsent(kind, k -> {
			List<FluidRecipe> list = new ArrayList<>();
			for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
				if (holder.value() instanceof FluidRecipe recipe && recipe.machine() == k) {
					list.add(recipe);
				}
			}
			return List.copyOf(list);
		});
	}

	/** The first recipe whose items and fluids are all present. */
	public static Optional<FluidRecipe> find(MinecraftServer server, MachineKind kind, List<ItemStack> items, FluidTanks tanks) {
		for (FluidRecipe recipe : recipes(server, kind)) {
			if (recipe.itemsMatch(items) && recipe.fluidsMatch(tanks)) {
				return Optional.of(recipe);
			}
		}
		return Optional.empty();
	}

	/** Whether any recipe of this machine uses {@code variant} in input tank {@code tank} (what the tank accepts). */
	public static boolean usesFluid(MinecraftServer server, MachineKind kind, int tank, FluidVariant variant) {
		for (FluidRecipe recipe : recipes(server, kind)) {
			if (tank < recipe.fluids().size() && variant.isOf(recipe.fluids().get(tank).fluid())) {
				return true;
			}
		}
		return false;
	}

	/** Whether any recipe of this machine uses {@code stack} in item input slot {@code slot}. */
	public static boolean usesItem(MinecraftServer server, MachineKind kind, int slot, ItemStack stack) {
		for (FluidRecipe recipe : recipes(server, kind)) {
			if (slot < recipe.items().size() && recipe.items().get(slot).ingredient().test(stack)) {
				return true;
			}
		}
		return false;
	}
}
