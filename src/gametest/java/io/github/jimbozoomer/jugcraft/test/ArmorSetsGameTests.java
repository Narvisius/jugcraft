package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.gear.JugcraftGear;
import io.github.jimbozoomer.jugcraft.machine.Electroplating;
import io.github.jimbozoomer.jugcraft.weapons.DescribedItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.equipment.EquipmentAssets;

/**
 * In-game tests for Steampunk and Kaiser Armor (docs/features/steampunk-and-kaiser-armor.md): each styled set is its
 * metal's armor in every number, only drawn from another equipment asset; a smithing pattern turns a plain piece into
 * a styled one and the same pattern with the metal's ingot turns it back, keeping its enchantments, wear, name, anvil
 * cost, trim and plating; wrong inputs make nothing; and the recipes load. The numbers are typed in here, not read
 * from JugcraftGear, so an unintended change to the plain sets is caught too.
 * <p>Vanilla items, enchantments, attributes and the data components no Jugcraft code has used yet (trim, repair
 * cost, enchantments, repairable) are looked up by ID, so the test needs no 26.3 field name this mod has not compiled
 * against (the dyes, for one, have no {@code Items} constant in 26.3).
 */
public class ArmorSetsGameTests {
	private static final String[] PIECES = {"helmet", "chestplate", "leggings", "boots"};
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	/** The vanilla slot tags' stems: #minecraft:head_armor, #minecraft:enchantable/head_armor and so on. */
	private static final String[] SLOT_TAGS = {"head", "chest", "leg", "foot"};

	/**
	 * A set and the metal it protects as, with that metal's numbers in helmet, chestplate, leggings, boots order
	 * (docs/features/tools-and-armor.md; JugcraftGear.BRONZE_ARMOR and STEEL_ARMOR).
	 */
	private record ArmorSet(String id, String metal, int[] defense, int[] durability, double toughness, double knockback,
			int enchantability) {
	}

	private static final List<ArmorSet> SETS = List.of(
			new ArmorSet("bronze", "bronze", new int[] {2, 6, 5, 2}, new int[] {165, 240, 225, 195}, 0.5, 0.0, 12),
			new ArmorSet("steampunk", "bronze", new int[] {2, 6, 5, 2}, new int[] {165, 240, 225, 195}, 0.5, 0.0, 12),
			new ArmorSet("steel", "steel", new int[] {3, 7, 6, 3}, new int[] {275, 400, 375, 325}, 1.5, 0.05, 10),
			new ArmorSet("kaiser", "steel", new int[] {3, 7, 6, 3}, new int[] {275, 400, 375, 325}, 1.5, 0.05, 10));

	/** A styled set, the metal it dresses, its pattern and the vanilla addition that dresses a plain piece (tools/gear.py: ARMOR_STYLES). */
	private record Style(String id, String metal, String pattern, String addition) {
	}

	private static final List<Style> STYLES = List.of(new Style("steampunk", "bronze", "steampunk_pattern", "copper_ingot"),
			new Style("kaiser", "steel", "kaiser_pattern", "gold_ingot"));

	/** What smithing must carry over, both ways: enchantments, trim, wear, durability (raised by nickel), name, anvil cost, plating. */
	private static final List<String> KEPT = List.of("minecraft:enchantments", "minecraft:trim", "minecraft:damage",
			"minecraft:max_damage", "minecraft:custom_name", "minecraft:repair_cost", "jugcraft:plating");

	/**
	 * All 16 pieces (bronze, Steampunk, steel and Kaiser): the right slot and equipment asset; the metal's armor,
	 * toughness and knockback resistance, durability and enchantability; repaired with the metal's ingot and not iron;
	 * common; and in every vanilla tag that makes armor enchantable, trimmable and wearable. Each styled piece has the
	 * plain piece's modifiers, durability, enchantability and slot, and a line of lore. The two patterns are uncommon
	 * templates that cannot be worn.
	 */
	@GameTest
	public void armorSetsMatchTheirMetal(GameTestHelper helper) {
		List<String> wrong = new ArrayList<>();
		for (ArmorSet set : SETS) {
			String repairTag = "jugcraft:repairs_" + set.metal() + "_gear";
			TagKey<Item> repairs = TagKey.create(Registries.ITEM, Identifier.parse(repairTag));
			if (!new ItemStack(item(set.metal() + "_ingot")).is(repairs)) {
				wrong.add("#" + repairTag + " does not hold the " + set.metal() + " ingot");
			}
			if (new ItemStack(vanilla("iron_ingot")).is(repairs)) {
				wrong.add("#" + repairTag + " holds the iron ingot");
			}
			for (int p = 0; p < PIECES.length; p++) {
				String id = set.id() + "_" + PIECES[p];
				ItemStack stack = new ItemStack(item(id));
				var equippable = stack.get(DataComponents.EQUIPPABLE);
				if (equippable == null || equippable.slot() != SLOTS[p]) {
					wrong.add(id + " is not worn in " + SLOTS[p]);
				} else if (!equippable.assetId().equals(Optional.of(ResourceKey.create(EquipmentAssets.ROOT_ID, Jugcraft.id(set.id()))))) {
					wrong.add(id + " is drawn as " + equippable.assetId() + ", not jugcraft:" + set.id());
				}
				// The knockback resistance is stored as a float (0.05F) and widened to a double, so all three compare
				// within a millionth.
				near(wrong, id + "'s armor", amount(stack, "armor"), set.defense()[p]);
				near(wrong, id + "'s toughness", amount(stack, "armor_toughness"), set.toughness());
				near(wrong, id + "'s knockback resistance", amount(stack, "knockback_resistance"), set.knockback());
				if (stack.getMaxDamage() != set.durability()[p]) {
					wrong.add(id + " lasts " + stack.getMaxDamage() + ", not " + set.durability()[p]);
				}
				var enchantable = stack.get(DataComponents.ENCHANTABLE);
				if (enchantable == null || enchantable.value() != set.enchantability()) {
					wrong.add(id + " is not enchantable at " + set.enchantability());
				}
				// Repair: an anvil takes an ingot if it is in the set its minecraft:repairable names. That set is the
				// metal's repair tag, checked above to hold the metal's ingot and not iron. It is read from the
				// component's text: a named tag set compares by identity, so the record is not compared whole.
				String repairable = String.valueOf(component(stack, "minecraft:repairable"));
				if (!repairable.contains(repairTag)) {
					wrong.add(id + " is not repaired from #" + repairTag + ": " + repairable);
				}
				if (stack.getOrDefault(DataComponents.RARITY, Rarity.COMMON) != Rarity.COMMON) {
					wrong.add(id + " is " + stack.get(DataComponents.RARITY) + ", not common");
				}
				for (String tag : List.of(SLOT_TAGS[p] + "_armor", "enchantable/armor", "enchantable/" + SLOT_TAGS[p] + "_armor",
						"enchantable/durability", "enchantable/equippable", "trimmable_armor")) {
					if (!stack.is(TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace(tag)))) {
						wrong.add(id + " is not in #minecraft:" + tag);
					}
				}
				if (set.id().equals(set.metal())) {
					continue;
				}
				// A styled piece against its plain piece: everything but the look is the same (repair is checked above).
				ItemStack base = new ItemStack(item(set.metal() + "_" + PIECES[p]));
				if (!(stack.getItem() instanceof DescribedItem)) {
					wrong.add(id + " has no line of lore");
				}
				for (String same : List.of("minecraft:attribute_modifiers", "minecraft:max_damage", "minecraft:enchantable")) {
					if (!Objects.equals(component(stack, same), component(base, same))) {
						wrong.add(id + "'s " + same + " is " + component(stack, same) + ", not the plain piece's " + component(base, same));
					}
				}
				var baseEquippable = base.get(DataComponents.EQUIPPABLE);
				if (equippable == null || baseEquippable == null || equippable.slot() != baseEquippable.slot()) {
					wrong.add(id + " is not worn where " + set.metal() + "_" + PIECES[p] + " is");
				}
			}
		}
		for (Style style : STYLES) {
			Item template = JugcraftGear.TEMPLATES.get(style.pattern());
			if (template == null || template != item(style.pattern())) {
				wrong.add(style.pattern() + " is not registered as a template");
				continue;
			}
			ItemStack stack = new ItemStack(template);
			if (stack.getOrDefault(DataComponents.RARITY, Rarity.COMMON) != Rarity.UNCOMMON) {
				wrong.add(style.pattern() + " is " + stack.get(DataComponents.RARITY) + ", not uncommon");
			}
			if (!(template instanceof DescribedItem)) {
				wrong.add(style.pattern() + " has no line of lore");
			}
			if (stack.has(DataComponents.EQUIPPABLE)) {
				wrong.add(style.pattern() + " can be worn");
			}
		}
		helper.assertTrue(wrong.isEmpty(), "The armor sets do not match their metal: " + wrong);
		helper.succeed();
	}

	/**
	 * For each style and piece: a plain piece with Protection IV and Unbreaking III, nickel plating (which raises its
	 * durability), a gold sentry trim (put on by vanilla's own trim recipe), 37 damage, a name and an anvil cost of 3
	 * goes in with its pattern and the addition and comes out styled with all of it; the same pattern and the metal's
	 * ingot turn it back into the very same plain piece. Wrong bases, patterns and additions match no recipe.
	 */
	@GameTest
	public void patternsRestyleAndBackKeepEverything(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		var protection = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, Identifier.withDefaultNamespace("protection")));
		var unbreaking = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, Identifier.withDefaultNamespace("unbreaking")));
		for (Style style : STYLES) {
			ItemStack pattern = new ItemStack(item(style.pattern()));
			for (String piece : PIECES) {
				String plainId = style.metal() + "_" + piece;
				String styledId = style.id() + "_" + piece;
				Item plain = item(plainId);
				Item styled = item(styledId);
				// The worn, enchanted, plated, trimmed, named plain piece, built in this order because plating repairs.
				ItemStack base = new ItemStack(plain);
				base.enchant(protection, 4);
				base.enchant(unbreaking, 3);
				base = Electroplating.plate(base, "nickel", level.registryAccess());
				int plated = base.getMaxDamage();
				helper.assertTrue(base.is(plain) && "nickel".equals(base.get(Electroplating.PLATING))
						&& plated != new ItemStack(plain).getMaxDamage(),
						"Nickel plating did not take on " + plainId + " or raise its durability: " + base + ", lasts " + plated);
				base = smith(level, new ItemStack(vanilla("sentry_armor_trim_smithing_template")), base, new ItemStack(vanilla("gold_ingot")));
				helper.assertTrue(base.is(plain) && component(base, "minecraft:trim") != null,
						"A gold sentry trim did not go on " + plainId + ": " + base);
				base.setDamageValue(37);
				base.set(DataComponents.CUSTOM_NAME, Component.literal("Old Faithful"));
				base.set(repairCost(), 3);

				// Forward: pattern, plain piece and the addition give the styled piece, with everything kept.
				ItemStack dressed = smith(level, pattern.copy(), base, new ItemStack(vanilla(style.addition())));
				helper.assertTrue(dressed.is(styled) && dressed.getCount() == 1,
						style.pattern() + " with " + style.addition() + " made " + dressed + " from " + plainId);
				helper.assertTrue(lost(base, dressed).isEmpty(), styledId + " lost " + lost(base, dressed));
				helper.assertTrue(net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(protection, dressed) == 4
						&& net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(unbreaking, dressed) == 3,
						styledId + " lost its enchantments");
				helper.assertTrue(dressed.getDamageValue() == 37 && dressed.getHoverName().getString().equals("Old Faithful")
						&& Objects.equals(component(dressed, "minecraft:repair_cost"), 3), styledId + " has damage "
						+ dressed.getDamageValue() + ", name " + dressed.getHoverName().getString() + ", anvil cost "
						+ component(dressed, "minecraft:repair_cost"));
				helper.assertTrue("nickel".equals(dressed.get(Electroplating.PLATING)) && dressed.getMaxDamage() == plated
						&& dressed.getMaxDamage() != new ItemStack(styled).getMaxDamage(),
						styledId + " is plated " + dressed.get(Electroplating.PLATING) + " and lasts " + dressed.getMaxDamage());
				helper.assertTrue(component(dressed, "minecraft:trim") != null
						&& Objects.equals(component(dressed, "minecraft:trim"), component(base, "minecraft:trim")),
						styledId + " lost its trim");

				// Back: the same pattern and the metal's ingot give the plain piece, the same as it went in.
				ItemStack back = smith(level, pattern.copy(), dressed, new ItemStack(item(style.metal() + "_ingot")));
				helper.assertTrue(back.is(plain) && back.getCount() == 1 && ItemStack.isSameItemSameComponents(back, base),
						style.pattern() + " with a " + style.metal() + " ingot turned " + styledId + " into " + back
								+ ", not the plain piece it came from (lost " + lost(base, back) + ")");
			}
		}

		// Wrong inputs: no recipe, so the table shows nothing and uses nothing.
		ItemStack steampunk = new ItemStack(item("steampunk_pattern"));
		ItemStack kaiser = new ItemStack(item("kaiser_pattern"));
		ItemStack copper = new ItemStack(vanilla("copper_ingot"));
		ItemStack gold = new ItemStack(vanilla("gold_ingot"));
		ItemStack bronzeIngot = new ItemStack(item("bronze_ingot"));
		ItemStack steelIngot = new ItemStack(item("steel_ingot"));
		ItemStack ironHelmet = new ItemStack(vanilla("iron_helmet"));
		List<String> made = new ArrayList<>();
		none(level, made, "an iron helmet with the Steampunk Pattern and copper", steampunk, ironHelmet, copper);
		none(level, made, "an iron helmet with the Kaiser Pattern and gold", kaiser, ironHelmet, gold);
		none(level, made, "a bronze helmet with the Kaiser Pattern and gold", kaiser, new ItemStack(item("bronze_helmet")), gold);
		none(level, made, "a steel helmet with the Steampunk Pattern and copper", steampunk, new ItemStack(item("steel_helmet")), copper);
		none(level, made, "a Steampunk helmet with the Kaiser Pattern and gold", kaiser, new ItemStack(item("steampunk_helmet")), gold);
		none(level, made, "a Steampunk helmet with the Kaiser Pattern and steel", kaiser, new ItemStack(item("steampunk_helmet")), steelIngot);
		none(level, made, "a Kaiser helmet with the Steampunk Pattern and copper", steampunk, new ItemStack(item("kaiser_helmet")), copper);
		none(level, made, "a Kaiser helmet with the Steampunk Pattern and bronze", steampunk, new ItemStack(item("kaiser_helmet")), bronzeIngot);
		none(level, made, "a steel helmet with the Kaiser Pattern and copper", kaiser, new ItemStack(item("steel_helmet")), copper);
		none(level, made, "a bronze helmet with the Steampunk Pattern and gold", steampunk, new ItemStack(item("bronze_helmet")), gold);
		none(level, made, "a Steampunk helmet with its pattern and copper, not bronze", steampunk, new ItemStack(item("steampunk_helmet")), copper);
		none(level, made, "a Kaiser helmet with its pattern and gold, not steel", kaiser, new ItemStack(item("kaiser_helmet")), gold);
		helper.assertTrue(made.isEmpty(), "Wrong inputs matched a smithing recipe: " + made);
		helper.succeed();
	}

	/**
	 * The recipes load: 8 smithing recipes each way, the two patterns and the plain pieces. The Steampunk grid makes 4
	 * Steampunk Patterns, the Kaiser grid (with an Imperial Crest) 4 Kaiser Patterns, and five bronze ingots in a
	 * helmet's shape still make a bronze helmet.
	 */
	@GameTest
	public void armorSetRecipesLoad(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<String> missing = new ArrayList<>();
		for (Style style : STYLES) {
			List<String> names = new ArrayList<>(List.of(style.pattern()));
			for (String piece : PIECES) {
				names.add(style.id() + "_" + piece);
				names.add(style.metal() + "_" + piece + "_from_" + style.id() + "_" + piece);
				names.add(style.metal() + "_" + piece);
			}
			for (String name : names) {
				if (level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(name))).isEmpty()) {
					missing.add(name);
				}
			}
		}
		helper.assertTrue(missing.isEmpty(), "These recipes did not load: " + missing);

		RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting = RecipeManager.createCheck(RecipeType.CRAFTING);
		Item c = vanilla("copper_ingot");
		Item l = vanilla("leather");
		Item g = vanilla("glass_pane");
		Item p = vanilla("paper");
		ItemStack steampunk = craft(level, crafting, c, l, c, g, p, g, c, l, c);
		helper.assertTrue(steampunk.is(item("steampunk_pattern")) && steampunk.getCount() == 4,
				"Copper, leather, glass panes and paper made " + steampunk + ", not 4 Steampunk Patterns");
		Item n = vanilla("gold_nugget");
		Item b = vanilla("black_dye");
		Item r = vanilla("red_dye");
		ItemStack kaiser = craft(level, crafting, n, item("imperial_crest"), n, b, p, b, n, r, n);
		helper.assertTrue(kaiser.is(item("kaiser_pattern")) && kaiser.getCount() == 4,
				"Gold nuggets, an Imperial Crest, dyes and paper made " + kaiser + ", not 4 Kaiser Patterns");
		Item bronze = item("bronze_ingot");
		ItemStack helmet = craft(level, crafting, bronze, bronze, bronze, bronze, null, bronze, null, null, null);
		helper.assertTrue(helmet.is(item("bronze_helmet")) && helmet.getCount() == 1, "Five bronze ingots made " + helmet + ", not a bronze helmet");
		helper.succeed();
	}

	private static Item item(String path) {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id(path));
	}

	/** A vanilla item by ID. */
	private static Item vanilla(String path) {
		return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(path));
	}

	/** A data component's value on a stack, by the component's ID ("minecraft:trim", "jugcraft:plating"), or null. */
	private static Object component(ItemStack stack, String id) {
		return stack.get(BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.parse(id)));
	}

	/** minecraft:repair_cost, the anvil's prior-work penalty, by ID. */
	@SuppressWarnings("unchecked")
	private static DataComponentType<Integer> repairCost() {
		return (DataComponentType<Integer>) BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.withDefaultNamespace("repair_cost"));
	}

	/** The components in {@link #KEPT} whose values differ between two stacks. */
	private static List<String> lost(ItemStack from, ItemStack to) {
		List<String> lost = new ArrayList<>();
		for (String id : KEPT) {
			if (!Objects.equals(component(from, id), component(to, id))) {
				lost.add(id);
			}
		}
		return lost;
	}

	/** The total an item's own modifiers add to an attribute ("armor", "armor_toughness", "knockback_resistance"). */
	private static double amount(ItemStack stack, String attribute) {
		return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers().stream()
				.filter(entry -> entry.attribute().getRegisteredName().equals("minecraft:" + attribute))
				.mapToDouble(entry -> entry.modifier().amount()).sum();
	}

	private static void near(List<String> wrong, String what, double got, double expected) {
		if (Math.abs(got - expected) > 1.0E-6) {
			wrong.add(what + " is " + got + ", not " + expected);
		}
	}

	/** What a smithing table makes from these three, as vanilla's smithing menu asks, or an empty stack if no recipe matches. */
	private static ItemStack smith(ServerLevel level, ItemStack template, ItemStack base, ItemStack addition) {
		SmithingRecipeInput input = new SmithingRecipeInput(template, base, addition);
		Optional<RecipeHolder<SmithingRecipe>> recipe = level.recipeAccess().getRecipeFor(RecipeType.SMITHING, input, level);
		return recipe.isPresent() ? recipe.get().value().assemble(input) : ItemStack.EMPTY;
	}

	/** Notes in {@code made} if these three match any smithing recipe; they should match none. */
	private static void none(ServerLevel level, List<String> made, String what, ItemStack template, ItemStack base, ItemStack addition) {
		SmithingRecipeInput input = new SmithingRecipeInput(template.copy(), base.copy(), addition.copy());
		Optional<RecipeHolder<SmithingRecipe>> matched = level.recipeAccess().getRecipeFor(RecipeType.SMITHING, input, level);
		if (matched.isPresent()) {
			made.add(what + " matched " + matched.get().id().identifier());
		}
	}

	/** What a crafting table makes from a 3x3 grid (null: an empty slot), or an empty stack. */
	private static ItemStack craft(ServerLevel level, RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting, Item... grid) {
		List<ItemStack> stacks = new ArrayList<>();
		for (Item slot : grid) {
			stacks.add(slot == null ? ItemStack.EMPTY : new ItemStack(slot));
		}
		CraftingInput input = CraftingInput.of(3, 3, stacks);
		Optional<RecipeHolder<CraftingRecipe>> recipe = crafting.getRecipeFor(input, level);
		return recipe.isPresent() ? recipe.get().value().assemble(input) : ItemStack.EMPTY;
	}
}
