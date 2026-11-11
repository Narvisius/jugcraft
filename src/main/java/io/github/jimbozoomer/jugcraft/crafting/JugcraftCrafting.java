package io.github.jimbozoomer.jugcraft.crafting;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The Advanced Crafting Table (take 2, approved 26 October 2026; tools/adv_crafting_table.py): a 3 x 2 metal workstation
 * with a 6 x 6 crafting grid for the drones and the advanced kit, its recipes bigger than 3 x 3 so none can clash with a
 * crafting table's. Its block is a room kit piece (display "crafting", tower/RoomKit registers it and its block entity);
 * here its recipe type, the recipes' serializer and its screen's menu.
 */
public final class JugcraftCrafting {
	public static RecipeType<AdvancedCraftingRecipe> ADVANCED;
	public static RecipeSerializer<AdvancedCraftingRecipe> SERIALIZER;
	public static MenuType<AdvancedCraftingMenu> MENU;
	/** The table's block entity, at its anchor (registered with the room kits, tower/RoomKit). */
	public static BlockEntityType<AdvancedCraftingTableBlockEntity> TABLE_ENTITY;

	private JugcraftCrafting() {
	}

	public static void register() {
		ADVANCED = Registry.register(BuiltInRegistries.RECIPE_TYPE, Jugcraft.id("advanced_crafting"), new RecipeType<AdvancedCraftingRecipe>() {
			@Override
			public String toString() {
				return Jugcraft.MOD_ID + ":advanced_crafting";
			}
		});
		SERIALIZER = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Jugcraft.id("advanced_crafting"), AdvancedCraftingRecipe.serializer());
		MENU = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("advanced_crafting_table"),
				new ExtendedMenuType<>(AdvancedCraftingMenu::new, BlockPos.STREAM_CODEC.cast()));
	}
}
