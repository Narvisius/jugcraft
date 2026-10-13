package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.agriculture.PlacedDishBlock.DishShape;
import java.util.List;

/**
 * The menu (the kitchen and cooking expansion's slice 3, tools/menu.py), the rice slice (slice 4, tools/rice.py) and the
 * orchards (slice 6, tools/orchard.py): every dish that can be set down as a {@link PlacedDishBlock} and how it stands, in
 * the order of tools/menu.py all_placed() (the menu's dishes, the foods Jugcraft already had that now wear the owner's art,
 * the rice dishes, then the orchards' juices). Each block shares its food's ID; tools/check_mod_data.py compares this list
 * with tools/menu.py.
 */
public final class MenuDishes {
	/** A dish that can be set down: its food's (and block's) ID and the shape its model takes. */
	public record Dish(String id, DishShape shape) {
	}

	public static final List<Dish> PLACED = List.of(
			new Dish("hot_cocoa", DishShape.STAND),
			new Dish("creamy_corn_drink", DishShape.STAND),
			new Dish("melon_juice", DishShape.STAND),
			new Dish("glow_berry_custard", DishShape.STAND),
			new Dish("milk_bottle", DishShape.STAND),
			new Dish("beef_stew", DishShape.BOWL),
			new Dish("chicken_soup", DishShape.BOWL),
			new Dish("baked_cod_stew", DishShape.BOWL),
			new Dish("fish_stew", DishShape.BOWL),
			new Dish("bone_broth", DishShape.BOWL),
			new Dish("corn_soup", DishShape.BOWL),
			new Dish("noodle_soup", DishShape.BOWL),
			new Dish("tomato_sauce", DishShape.BOWL),
			new Dish("fruit_salad", DishShape.BOWL),
			new Dish("nether_salad", DishShape.BOWL),
			new Dish("creamed_corn", DishShape.FLAT),
			new Dish("bacon_and_eggs", DishShape.PLATE),
			new Dish("steak_and_potatoes", DishShape.PLATE),
			new Dish("roasted_mutton_chops", DishShape.PLATE),
			new Dish("grilled_salmon", DishShape.PLATE),
			new Dish("ratatouille", DishShape.PLATE),
			new Dish("pasta_with_meatballs", DishShape.PLATE),
			new Dish("pasta_with_mutton_chop", DishShape.PLATE),
			new Dish("squid_ink_pasta", DishShape.PLATE),
			new Dish("vegetable_noodles", DishShape.PLATE),
			new Dish("cornbread_stuffing", DishShape.PLATE),
			new Dish("hamburger", DishShape.STACK),
			new Dish("bacon_sandwich", DishShape.STACK),
			new Dish("chicken_sandwich", DishShape.STACK),
			new Dish("egg_sandwich", DishShape.STACK),
			new Dish("mutton_wrap", DishShape.FLAT),
			new Dish("taco", DishShape.FLAT),
			new Dish("stuffed_potato", DishShape.FLAT),
			new Dish("dumplings", DishShape.FLAT),
			new Dish("ham", DishShape.FLAT),
			new Dish("smoked_ham", DishShape.FLAT),
			new Dish("barbecue_stick", DishShape.FLAT),
			new Dish("corn_dog", DishShape.FLAT),
			new Dish("classic_corn_dog", DishShape.FLAT),
			new Dish("honey_cookie", DishShape.FLAT),
			new Dish("sweet_berry_cookie", DishShape.FLAT),
			new Dish("caramel_popcorn", DishShape.FLAT),
			new Dish("corn_popsicle", DishShape.FLAT),
			new Dish("melon_popsicle", DishShape.FLAT),
			new Dish("boiled_corn", DishShape.FLAT),
			new Dish("cornbread", DishShape.FLAT),
			new Dish("tortilla", DishShape.FLAT),
			new Dish("tortilla_chip", DishShape.FLAT),
			new Dish("dog_food", DishShape.BOWL),
			new Dish("horse_feed", DishShape.FLAT),
			new Dish("onion_soup", DishShape.BOWL),
			new Dish("vegetable_soup", DishShape.BOWL),
			new Dish("pumpkin_soup", DishShape.BOWL),
			new Dish("cabbage_rolls", DishShape.FLAT),
			new Dish("roasted_corn", DishShape.FLAT),
			new Dish("mulled_cider", DishShape.STAND),
			new Dish("popcorn", DishShape.BOX),
			new Dish("cooked_rice", DishShape.BOWL),
			new Dish("fried_rice", DishShape.BOWL),
			new Dish("mushroom_rice", DishShape.PLATE),
			new Dish("salmon_roll", DishShape.FLAT),
			new Dish("cod_roll", DishShape.FLAT),
			new Dish("kelp_roll", DishShape.FLAT),
			new Dish("kelp_roll_slice", DishShape.FLAT),
			new Dish("orange_juice", DishShape.STAND),
			new Dish("lemonade", DishShape.STAND),
			new Dish("strawberry_milkshake", DishShape.MILKSHAKE),
			new Dish("banana_milkshake", DishShape.MILKSHAKE),
			new Dish("plum_milkshake", DishShape.MILKSHAKE),
			new Dish("apple_milkshake", DishShape.MILKSHAKE),
			new Dish("blueberry_milkshake", DishShape.MILKSHAKE),
			new Dish("pumpkin_milkshake", DishShape.MILKSHAKE),
			new Dish("chocolate_milkshake", DishShape.MILKSHAKE));

	private MenuDishes() {
	}
}
