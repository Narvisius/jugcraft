package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.gear.ThalliteGear;
import io.github.jimbozoomer.jugcraft.gear.ThalliteGearItem;
import io.github.jimbozoomer.jugcraft.gear.TraitTooltips;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Thallite, slice 2 (docs/features/thallite.md): its tools and armor, plain and Earthbound, in their numbers; the
 * Earthbinding Template and the one-way smithing that keeps enchantments, wear and name; Regrowth (one use back a round
 * on living soil, never past 75%, on stone only with two Earthbound pieces worn); Rooted (0.075 knockback resistance a
 * piece on natural ground, none in the air); and the traits in the tooltip. The numbers are typed in here, not read
 * from JugcraftGear, so a change to them is caught. {@code ThalliteClientGameTests} shows the set in game.
 */
public class ThalliteGearGameTests {
	private static final String[] TOOLS = {"sword", "pickaxe", "axe", "shovel", "hoe"};
	private static final String[] ARMOR = {"helmet", "chestplate", "leggings", "boots"};
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	/** Iron's defense, helmet to boots, and thallite's durability (multiplier 13). */
	private static final int[] DEFENSE = {2, 6, 5, 2};
	private static final int[] DURABILITY = {143, 208, 195, 169};

	/**
	 * Every piece: 200 uses for a tool and the armor's durability; enchantability 18; iron's defense, no toughness or
	 * knockback resistance; worn from its own equipment asset; common; in #jugcraft:thallite_gear, and in
	 * #jugcraft:earthbound_armor only if Earthbound; and made as a thallite item that knows which it is. The template
	 * is an uncommon item that cannot be worn.
	 */
	@GameTest
	public void thalliteGearMatchesItsNumbers(GameTestHelper helper) {
		List<String> wrong = new ArrayList<>();
		for (String tool : TOOLS) {
			ItemStack stack = new ItemStack(item("thallite_" + tool));
			if (stack.getMaxDamage() != 200) {
				wrong.add("thallite_" + tool + " lasts " + stack.getMaxDamage() + ", not 200");
			}
			common(wrong, "thallite_" + tool, stack, false);
		}
		for (String look : List.of("thallite", "earthbound_thallite")) {
			for (int p = 0; p < ARMOR.length; p++) {
				String id = look + "_" + ARMOR[p];
				ItemStack stack = new ItemStack(item(id));
				var equippable = stack.get(DataComponents.EQUIPPABLE);
				if (equippable == null || equippable.slot() != SLOTS[p]) {
					wrong.add(id + " is not worn in " + SLOTS[p]);
				} else if (!equippable.assetId().equals(Optional.of(ResourceKey.create(EquipmentAssets.ROOT_ID, Jugcraft.id(look))))) {
					wrong.add(id + " is drawn as " + equippable.assetId() + ", not jugcraft:" + look);
				}
				near(wrong, id + "'s armor", amount(stack, "armor"), DEFENSE[p]);
				near(wrong, id + "'s toughness", amount(stack, "armor_toughness"), 0.0);
				near(wrong, id + "'s own knockback resistance", amount(stack, "knockback_resistance"), 0.0);
				if (stack.getMaxDamage() != DURABILITY[p]) {
					wrong.add(id + " lasts " + stack.getMaxDamage() + ", not " + DURABILITY[p]);
				}
				common(wrong, id, stack, look.startsWith("earthbound"));
			}
		}
		ItemStack template = new ItemStack(item("earthbinding_template"));
		if (template.getOrDefault(DataComponents.RARITY, Rarity.COMMON) != Rarity.UNCOMMON || template.has(DataComponents.EQUIPPABLE)) {
			wrong.add("The Earthbinding Template should be uncommon and not wearable");
		}
		helper.assertTrue(wrong.isEmpty(), "Thallite gear: " + String.join("; ", wrong));
		helper.succeed();
	}

	/**
	 * The template is crafted one at a time from rooted dirt, four thallite nuggets and four gold nuggets. At a smithing
	 * table it and a gold ingot bind each plain piece into its Earthbound piece, keeping enchantments, wear and name;
	 * nothing turns an Earthbound piece back, and a tool cannot be bound.
	 */
	@GameTest
	public void earthbindingIsOneWayAndKeepsThePiece(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Item gold = vanilla("gold_nugget");
		Item nugget = item("thallite_nugget");
		ItemStack crafted = craft(level, gold, nugget, gold, nugget, vanilla("rooted_dirt"), nugget, gold, nugget, gold);
		helper.assertTrue(crafted.is(item("earthbinding_template")) && crafted.getCount() == 1,
				"Rooted dirt, thallite and gold nuggets should craft one Earthbinding Template, not " + crafted);
		var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		var protection = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, Identifier.withDefaultNamespace("protection")));
		ItemStack template = new ItemStack(item("earthbinding_template"));
		for (String piece : ARMOR) {
			ItemStack plain = new ItemStack(item("thallite_" + piece));
			plain.enchant(protection, 3);
			plain.setDamageValue(40);
			plain.set(DataComponents.CUSTOM_NAME, Component.literal("Rootward " + piece));
			ItemStack bound = smith(level, template.copy(), plain.copy(), new ItemStack(vanilla("gold_ingot")));
			helper.assertTrue(bound.is(item("earthbound_thallite_" + piece)),
					"The template, a thallite " + piece + " and a gold ingot should make the Earthbound piece, not " + bound);
			helper.assertTrue(Objects.equals(bound.get(DataComponents.ENCHANTMENTS), plain.get(DataComponents.ENCHANTMENTS))
					&& bound.getDamageValue() == 40 && Objects.equals(bound.get(DataComponents.CUSTOM_NAME), plain.get(DataComponents.CUSTOM_NAME)),
					"Earthbinding the " + piece + " lost its enchantments, wear or name: " + bound);
			for (Item addition : List.of(vanilla("gold_ingot"), item("thallite_ingot"))) {
				ItemStack back = smith(level, template.copy(), bound.copy(), new ItemStack(addition));
				helper.assertTrue(back.isEmpty(), "An Earthbound " + piece + " with " + addition + " should make nothing, not " + back);
			}
		}
		ItemStack sword = smith(level, template.copy(), new ItemStack(item("thallite_sword")), new ItemStack(vanilla("gold_ingot")));
		helper.assertTrue(sword.isEmpty(), "A thallite sword should not be earthbound: " + sword);
		helper.succeed();
	}

	/**
	 * Regrowth on an armor stand holding a worn pickaxe and wearing a worn helmet: on grass each round gives each back
	 * one use; it stops at 75% of full; nothing on stone, in the air, or for an iron pickaxe; and on stone with two
	 * Earthbound pieces worn (one is not enough).
	 */
	@GameTest
	public void regrowthMendsOnLivingSoilUpToTheCap(GameTestHelper helper) {
		var stand = helper.spawn(EntityTypes.ARMOR_STAND, new BlockPos(1, 2, 1));
		BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
		BlockState stone = Blocks.STONE.defaultBlockState();
		ItemStack pickaxe = worn("thallite_pickaxe", 199);
		ItemStack helmet = worn("thallite_helmet", 100);
		stand.setItemSlot(EquipmentSlot.MAINHAND, pickaxe);
		stand.setItemSlot(EquipmentSlot.HEAD, helmet);
		stand.setItemSlot(EquipmentSlot.OFFHAND, worn("iron_pickaxe", 100));
		helper.assertTrue(ThalliteGear.regrow(stand, grass) == 2, "On grass, the pickaxe and helmet should each mend one use");
		helper.assertTrue(stand.getItemBySlot(EquipmentSlot.MAINHAND).getDamageValue() == 198
				&& stand.getItemBySlot(EquipmentSlot.HEAD).getDamageValue() == 99,
				"Regrowth did not take one damage off each: " + stand.getItemBySlot(EquipmentSlot.MAINHAND).getDamageValue() + ", "
						+ stand.getItemBySlot(EquipmentSlot.HEAD).getDamageValue());
		helper.assertTrue(stand.getItemBySlot(EquipmentSlot.OFFHAND).getDamageValue() == 100, "Regrowth mended an iron pickaxe");
		helper.assertTrue(ThalliteGear.regrow(stand, stone) == 0 && ThalliteGear.regrow(stand, null) == 0,
				"Regrowth mended on stone or in the air with no Earthbound armor");
		helper.assertTrue(ThalliteGear.regrow(stand, Blocks.FARMLAND.defaultBlockState()) == 2
				&& ThalliteGear.regrow(stand, Blocks.MOSS_BLOCK.defaultBlockState()) == 2,
				"Regrowth should work on farmland and moss");

		// The cap: 200 uses at 75% is 150 left, damage 50; the helmet's 143 at 75% is 107 left, damage 36.
		helper.assertTrue(ThalliteGear.capDamage(200) == 50 && ThalliteGear.capDamage(143) == 36,
				"The cap's damage should be 50 of 200 and 36 of 143: " + ThalliteGear.capDamage(200) + ", " + ThalliteGear.capDamage(143));
		ItemStack nearlyCapped = worn("thallite_shovel", 51);
		helper.assertTrue(ThalliteGear.mend(nearlyCapped) && nearlyCapped.getDamageValue() == 50 && !ThalliteGear.mend(nearlyCapped)
				&& nearlyCapped.getDamageValue() == 50, "Regrowth should stop at 75% of full: " + nearlyCapped.getDamageValue());
		helper.assertTrue(!ThalliteGear.mend(new ItemStack(item("thallite_shovel"))), "Regrowth mended an unworn shovel");

		// Earthbound: one piece is not enough for stone, two are.
		stand.setItemSlot(EquipmentSlot.FEET, new ItemStack(item("earthbound_thallite_boots")));
		helper.assertTrue(ThalliteGear.regrow(stand, stone) == 0, "One Earthbound piece let Regrowth work on stone");
		stand.setItemSlot(EquipmentSlot.LEGS, new ItemStack(item("earthbound_thallite_leggings")));
		helper.assertTrue(ThalliteGear.regrow(stand, stone) == 2 && ThalliteGear.regrow(stand, Blocks.SAND.defaultBlockState()) == 2,
				"Two Earthbound pieces should let Regrowth work on stone and sand");
		helper.assertTrue(ThalliteGear.regrow(stand, Blocks.OAK_PLANKS.defaultBlockState()) == 0,
				"Regrowth worked on planks, which are not natural ground");
		helper.succeed();
	}

	/**
	 * Rooted: each Earthbound piece worn adds 0.075 knockback resistance on natural ground, as one modifier that is
	 * replaced, not stacked, each refresh; plain thallite adds none; off the ground it is removed.
	 */
	@GameTest
	public void rootedHoldsOnNaturalGround(GameTestHelper helper) {
		var stand = helper.spawn(EntityTypes.ARMOR_STAND, new BlockPos(1, 2, 1));
		BlockState stone = Blocks.STONE.defaultBlockState();
		double base = stand.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
		stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(item("thallite_helmet")));
		ThalliteGear.root(stand, stone);
		near(helper, "Plain thallite's Rooted", stand.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) - base, 0.0);
		for (int p = 0; p < ARMOR.length; p++) {
			stand.setItemSlot(SLOTS[p], new ItemStack(item("earthbound_thallite_" + ARMOR[p])));
			ThalliteGear.root(stand, stone);
			ThalliteGear.root(stand, Blocks.GRASS_BLOCK.defaultBlockState());
			near(helper, (p + 1) + " Earthbound pieces' Rooted", stand.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) - base,
					(p + 1) * 0.075);
		}
		ThalliteGear.root(stand, Blocks.OAK_PLANKS.defaultBlockState());
		near(helper, "Rooted on planks", stand.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) - base, 0.0);
		ThalliteGear.root(stand, stone);
		ThalliteGear.root(stand, null);
		near(helper, "Rooted in the air", stand.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) - base, 0.0);
		helper.succeed();
	}

	/** An armor stand standing on grass reads grass as its footing, as the tick does for a player. */
	@GameTest
	public void footingIsTheBlockUnderFoot(GameTestHelper helper) {
		helper.setBlock(new BlockPos(1, 1, 1), Blocks.GRASS_BLOCK);
		var stand = helper.spawn(EntityTypes.ARMOR_STAND, new BlockPos(1, 2, 1));
		helper.succeedWhen(() -> {
			BlockState ground = ThalliteGear.footing(stand);
			helper.assertTrue(ground != null && ground.is(Blocks.GRASS_BLOCK), "The stand's footing is " + ground + ", not grass");
		});
	}

	/**
	 * The tooltip: Regrowth on every piece, then Rooted on Earthbound armor only, each with its description while
	 * Shift is held and "Hold Shift" otherwise.
	 */
	@GameTest
	public void theTraitsShowInTheTooltip(GameTestHelper helper) {
		String regrowth = Component.translatable("tooltip.jugcraft.thallite.regrowth.trait").getString();
		String rooted = Component.translatable("tooltip.jugcraft.thallite.rooted.trait").getString();
		String hint = Component.translatable("tooltip.jugcraft.hold_shift", Component.literal("Shift")).getString();
		for (Item item : BuiltInRegistries.ITEM) {
			ItemStack stack = new ItemStack(item);
			if (!stack.is(ThalliteGear.GEAR)) {
				continue;
			}
			helper.assertTrue(item instanceof ThalliteGearItem, item + " is in #jugcraft:thallite_gear but is not thallite gear");
			ThalliteGearItem gear = (ThalliteGearItem) item;
			helper.assertTrue(gear.earthbound() == stack.is(ThalliteGear.EARTHBOUND), item + " is Earthbound in its class but not its tag");
			List<String> folded = lines(gear, false);
			List<String> expanded = lines(gear, true);
			List<String> names = gear.earthbound() ? List.of(regrowth, rooted, hint) : List.of(regrowth, hint);
			helper.assertTrue(folded.equals(names), item + "'s folded traits are " + folded + ", not " + names);
			helper.assertTrue(expanded.get(0).equals(regrowth) && expanded.size() > folded.size() && !expanded.contains(hint)
					&& expanded.contains(rooted) == gear.earthbound(), item + "'s expanded traits are " + expanded);
		}
		helper.succeed();
	}

	private static List<String> lines(ThalliteGearItem item, boolean expanded) {
		List<Component> lines = new ArrayList<>();
		item.traits(TraitTooltips.of(lines::add, expanded));
		return lines.stream().map(Component::getString).toList();
	}

	/** Enchantability 18, common, in #jugcraft:thallite_gear, in #jugcraft:earthbound_armor only if Earthbound, and a thallite item. */
	private static void common(List<String> wrong, String id, ItemStack stack, boolean earthbound) {
		var enchantable = stack.get(DataComponents.ENCHANTABLE);
		if (enchantable == null || enchantable.value() != 18) {
			wrong.add(id + " is not enchantable at 18");
		}
		if (stack.getOrDefault(DataComponents.RARITY, Rarity.COMMON) != Rarity.COMMON) {
			wrong.add(id + " is not common");
		}
		if (!stack.is(ThalliteGear.GEAR)) {
			wrong.add(id + " is not in #jugcraft:thallite_gear");
		}
		if (stack.is(ThalliteGear.EARTHBOUND) != earthbound) {
			wrong.add(id + (earthbound ? " is not" : " is") + " in #jugcraft:earthbound_armor");
		}
		if (!(stack.getItem() instanceof ThalliteGearItem gear) || gear.earthbound() != earthbound) {
			wrong.add(id + " is not made as thallite gear" + (earthbound ? " (Earthbound)" : ""));
		}
	}

	/** A stack of this item (a Jugcraft id, or a vanilla one when no Jugcraft item has it) with this much damage. */
	private static ItemStack worn(String id, int damage) {
		Item item = id.startsWith("iron_") ? vanilla(id) : item(id);
		ItemStack stack = new ItemStack(item);
		stack.setDamageValue(damage);
		return stack;
	}

	private static Item item(String path) {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id(path));
	}

	private static Item vanilla(String path) {
		return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(path));
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

	private static void near(GameTestHelper helper, String what, double got, double expected) {
		helper.assertTrue(Math.abs(got - expected) < 1.0E-6, what + " is " + got + ", not " + expected);
	}

	/** What a smithing table makes from these three, or an empty stack if no recipe matches. */
	private static ItemStack smith(ServerLevel level, ItemStack template, ItemStack base, ItemStack addition) {
		SmithingRecipeInput input = new SmithingRecipeInput(template, base, addition);
		Optional<RecipeHolder<SmithingRecipe>> recipe = level.recipeAccess().getRecipeFor(RecipeType.SMITHING, input, level);
		return recipe.isPresent() ? recipe.get().value().assemble(input) : ItemStack.EMPTY;
	}

	/** What a crafting table makes from a 3x3 grid, or an empty stack. */
	private static ItemStack craft(ServerLevel level, Item... grid) {
		List<ItemStack> stacks = new ArrayList<>();
		for (Item slot : grid) {
			stacks.add(slot == null ? ItemStack.EMPTY : new ItemStack(slot));
		}
		CraftingInput input = CraftingInput.of(3, 3, stacks);
		Optional<RecipeHolder<CraftingRecipe>> recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
		return recipe.isPresent() ? recipe.get().value().assemble(input) : ItemStack.EMPTY;
	}
}
