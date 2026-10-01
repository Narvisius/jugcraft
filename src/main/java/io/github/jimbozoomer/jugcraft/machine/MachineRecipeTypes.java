package io.github.jimbozoomer.jugcraft.machine;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * One recipe type and serializer per processing machine, named by {@link MachineKind#recipeType()}:
 * jugcraft:crushing, arc_smelting, pressing, wire_drawing (one input) and jugcraft:alloying,
 * circuit_assembly (several inputs). The electric furnace uses vanilla smelting recipes.
 */
public final class MachineRecipeTypes {
	private static final Map<MachineKind, RecipeType<MachineRecipe>> SINGLE = new EnumMap<>(MachineKind.class);
	private static final Map<MachineKind, RecipeType<MultiMachineRecipe>> MULTI = new EnumMap<>(MachineKind.class);
	private static final Map<MachineKind, RecipeSerializer<MachineRecipe>> SINGLE_SERIALIZERS = new EnumMap<>(MachineKind.class);
	private static final Map<MachineKind, RecipeSerializer<MultiMachineRecipe>> MULTI_SERIALIZERS = new EnumMap<>(MachineKind.class);

	private MachineRecipeTypes() {
	}

	public static void register() {
		for (MachineKind kind : MachineKind.values()) {
			String name = kind.recipeType();
			if (name == null || kind.isFluidProcessor()) {
				continue; // Fluid processors have their own recipe class (chemistry/FluidRecipes).
			}
			if (kind.isMultiInput()) {
				MULTI.put(kind, type(name));
				MULTI_SERIALIZERS.put(kind, Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Jugcraft.id(name),
						MultiMachineRecipe.serializer(kind)));
			} else {
				SINGLE.put(kind, type(name));
				SINGLE_SERIALIZERS.put(kind, Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Jugcraft.id(name),
						MachineRecipe.serializer(kind)));
			}
		}
	}

	private static <T extends Recipe<?>> RecipeType<T> type(String name) {
		return Registry.register(BuiltInRegistries.RECIPE_TYPE, Jugcraft.id(name), new RecipeType<T>() {
			@Override
			public String toString() {
				return Jugcraft.MOD_ID + ":" + name;
			}
		});
	}

	public static RecipeType<MachineRecipe> single(MachineKind kind) {
		return SINGLE.get(kind);
	}

	public static RecipeType<MultiMachineRecipe> multi(MachineKind kind) {
		return MULTI.get(kind);
	}

	static RecipeSerializer<MachineRecipe> singleSerializer(MachineKind kind) {
		return SINGLE_SERIALIZERS.get(kind);
	}

	static RecipeSerializer<MultiMachineRecipe> multiSerializer(MachineKind kind) {
		return MULTI_SERIALIZERS.get(kind);
	}
}
