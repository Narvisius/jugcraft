package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.gear.JugcraftGear;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.EquipmentAssets;

/**
 * In-game tests for the armor-only tiers (docs/features/bloodthorn-armor.md): the owner's armor designs, each a tier
 * with numbers of its own, worn as a 3D model. Every piece has its tier's defense, toughness, knockback resistance,
 * durability and enchantability; is repaired from its tier's repair tag, which holds the stated item and not iron;
 * is fire resistant if the tier is; equips to its slot from the tier's equipment asset; and is in the vanilla tags
 * that make armor of its slot enchantable and wearable. The numbers are typed in here, not read from JugcraftGear, so
 * an unintended change is caught; every tier in JugcraftGear.ARMOR_TIERS must have a row.
 */
public class ArmorTiersGameTests {
	private static final String[] PIECES = {"helmet", "chestplate", "leggings", "boots"};
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	/** The vanilla slot tags' stems: #minecraft:head_armor, #minecraft:enchantable/head_armor and so on. */
	private static final String[] SLOT_TAGS = {"head", "chest", "leg", "foot"};

	/** A tier's numbers in helmet, chestplate, leggings, boots order, its repair item and whether it resists fire. */
	private record Tier(String id, int[] defense, int[] durability, double toughness, double knockback, int enchantability,
			String repair, boolean fireResistant) {
	}

	private static final List<Tier> TIERS = List.of(
			// Bloodthorn: a step above netherite (3/8/6/3, multiplier 37, 3.0, 0.1, 15).
			new Tier("bloodthorn", new int[] {3, 9, 7, 3}, new int[] {440, 640, 600, 520}, 3.5, 0.15, 15,
					"minecraft:netherite_ingot", true),
			// Reforged White Diamond: beside Bloodthorn, a heavier helm, the longest wear and the best enchanting.
			new Tier("reforged_white_diamond", new int[] {4, 8, 7, 3}, new int[] {495, 720, 675, 585}, 3.0, 0.1, 20,
					"minecraft:diamond", false),
			// Hades: the toughest and steadiest, a point less defense, the poorest enchanting; fire resistant.
			new Tier("hades", new int[] {3, 8, 7, 3}, new int[] {462, 672, 630, 546}, 4.0, 0.2, 12,
					"minecraft:netherite_ingot", true),
			// Sunset Gem: the longest wear and the best enchanting of all.
			new Tier("sunset_gem", new int[] {3, 8, 7, 3}, new int[] {528, 768, 720, 624}, 3.0, 0.1, 25,
					"minecraft:amethyst_shard", false),
			// Pharaoh: the heaviest helm and chest, leggings like netherite's, good enchanting; fire resistant.
			new Tier("pharaoh", new int[] {4, 9, 6, 3}, new int[] {451, 656, 615, 533}, 3.0, 0.1, 22,
					"minecraft:gold_ingot", true),
			// Dread Knight: the heaviest plate of all, among the steadiest, the poorest enchanting; fire resistant.
			new Tier("dread_knight", new int[] {4, 9, 7, 3}, new int[] {484, 704, 660, 572}, 3.5, 0.2, 10,
					"minecraft:netherite_ingot", true),
			// Valkyrie: a point over netherite, long wear and good enchanting; mended with phantom membranes.
			new Tier("valkyrie", new int[] {3, 8, 7, 3}, new int[] {506, 736, 690, 598}, 3.0, 0.1, 24,
					"minecraft:phantom_membrane", false),
			// Wayfarer: netherite's defense, the longest wear and the best enchanting, no knockback resistance.
			new Tier("wayfarer", new int[] {3, 8, 6, 3}, new int[] {550, 800, 750, 650}, 2.5, 0.0, 30,
					"minecraft:leather", false),
			// Spartan: the heavier helm, tough and steady, middling enchanting; mended with bronze.
			new Tier("spartan", new int[] {4, 8, 7, 3}, new int[] {473, 688, 645, 559}, 3.5, 0.15, 18,
					"jugcraft:bronze_ingot", false),
			// Berserker: a point over netherite, steady, middling enchanting; mended with quartz.
			new Tier("berserker", new int[] {3, 8, 7, 3}, new int[] {495, 720, 675, 585}, 3.0, 0.15, 16,
					"minecraft:quartz", false),
			// Paladin: the heavier helm, long wear and good enchanting; mended with amethyst shards.
			new Tier("paladin", new int[] {4, 8, 6, 3}, new int[] {517, 752, 705, 611}, 3.0, 0.1, 22,
					"minecraft:amethyst_shard", false),
			// Templar: the heavier chest, tough and steady, poorer enchanting; fire resistant.
			new Tier("templar", new int[] {3, 9, 7, 3}, new int[] {473, 688, 645, 559}, 3.5, 0.15, 14,
					"minecraft:netherite_ingot", true),
			// Sentinel: the heavier helm, tough and steady, good enchanting; mended with gold.
			new Tier("sentinel", new int[] {4, 8, 7, 3}, new int[] {506, 736, 690, 598}, 3.5, 0.15, 20,
					"minecraft:gold_ingot", false),
			// Frost Knight: a point over netherite, steady; mended with blue ice.
			new Tier("frost_knight", new int[] {3, 8, 7, 3}, new int[] {484, 704, 660, 572}, 3.0, 0.15, 18,
					"minecraft:blue_ice", false),
			// Wight King: the heavier helm, the steadiest of the cold sets, middling enchanting; mended with packed ice.
			new Tier("wight_king", new int[] {4, 8, 7, 3}, new int[] {495, 720, 675, 585}, 3.0, 0.2, 15,
					"minecraft:packed_ice", false),
			// Reaper: netherite's defense, good enchanting, less toughness, barely any knockback resistance; mended with bone.
			new Tier("reaper", new int[] {3, 8, 6, 3}, new int[] {462, 672, 630, 546}, 2.5, 0.05, 24,
					"minecraft:bone", false),
			// Banana: a costume: iron's defense, no toughness or knockback resistance, long wear, the best enchanting.
			new Tier("banana", new int[] {2, 6, 5, 2}, new int[] {275, 400, 375, 325}, 0.0, 0.0, 30,
					"minecraft:yellow_wool", false),
			// Scarab: gold and lapis: netherite's defense with gold's enchanting, less toughness; mended with lapis lazuli.
			new Tier("scarab", new int[] {3, 8, 6, 3}, new int[] {440, 640, 600, 520}, 2.0, 0.1, 25,
					"minecraft:lapis_lazuli", false));

	/** Every tier's four pieces have its numbers, repair, fire resistance, slot, look and tags. */
	@GameTest
	public void armorTiersHaveTheirNumbers(GameTestHelper helper) {
		List<String> wrong = new ArrayList<>();
		List<String> tested = TIERS.stream().map(Tier::id).toList();
		if (!tested.equals(JugcraftGear.ARMOR_TIERS)) {
			wrong.add("the tiers tested " + tested + " are not JugcraftGear.ARMOR_TIERS " + JugcraftGear.ARMOR_TIERS);
		}
		Object netheriteFire = component(new ItemStack(vanilla("netherite_helmet")), "minecraft:damage_resistant");
		for (Tier tier : TIERS) {
			String repairTag = "jugcraft:repairs_" + tier.id() + "_gear";
			TagKey<Item> repairs = TagKey.create(Registries.ITEM, Identifier.parse(repairTag));
			if (!new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(tier.repair()))).is(repairs)) {
				wrong.add("#" + repairTag + " does not hold " + tier.repair());
			}
			if (new ItemStack(vanilla("iron_ingot")).is(repairs)) {
				wrong.add("#" + repairTag + " holds the iron ingot");
			}
			for (int p = 0; p < PIECES.length; p++) {
				String id = tier.id() + "_" + PIECES[p];
				Item item = BuiltInRegistries.ITEM.getValue(Jugcraft.id(id));
				ItemStack stack = new ItemStack(item);
				if (stack.isEmpty()) {
					wrong.add(id + " is not registered");
					continue;
				}
				var equippable = stack.get(DataComponents.EQUIPPABLE);
				if (equippable == null || equippable.slot() != SLOTS[p]) {
					wrong.add(id + " is not worn in " + SLOTS[p]);
				} else if (!equippable.assetId().equals(Optional.of(ResourceKey.create(EquipmentAssets.ROOT_ID, Jugcraft.id(tier.id()))))) {
					wrong.add(id + " is drawn as " + equippable.assetId() + ", not jugcraft:" + tier.id());
				}
				// Toughness and knockback resistance are stored as floats (3.5F, 0.15F) and widened to doubles.
				near(wrong, id + "'s armor", amount(stack, "armor"), tier.defense()[p]);
				near(wrong, id + "'s toughness", amount(stack, "armor_toughness"), tier.toughness());
				near(wrong, id + "'s knockback resistance", amount(stack, "knockback_resistance"), tier.knockback());
				if (stack.getMaxDamage() != tier.durability()[p]) {
					wrong.add(id + " lasts " + stack.getMaxDamage() + ", not " + tier.durability()[p]);
				}
				var enchantable = stack.get(DataComponents.ENCHANTABLE);
				if (enchantable == null || enchantable.value() != tier.enchantability()) {
					wrong.add(id + " is not enchantable at " + tier.enchantability());
				}
				String repairable = String.valueOf(component(stack, "minecraft:repairable"));
				if (!repairable.contains(repairTag)) {
					wrong.add(id + " is not repaired from #" + repairTag + ": " + repairable);
				}
				// Fire resistance is the same damage_resistant component netherite's pieces carry.
				Object fire = component(stack, "minecraft:damage_resistant");
				if (tier.fireResistant() ? fire == null || !fire.equals(netheriteFire) : fire != null) {
					wrong.add(id + (tier.fireResistant() ? " does not resist fire as netherite does: " + fire + ", netherite "
							+ netheriteFire : " resists fire"));
				}
				for (String tag : List.of(SLOT_TAGS[p] + "_armor", "enchantable/armor", "enchantable/" + SLOT_TAGS[p] + "_armor",
						"enchantable/durability", "enchantable/equippable")) {
					if (!stack.is(TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace(tag)))) {
						wrong.add(id + " is not in #minecraft:" + tag);
					}
				}
			}
		}
		helper.assertTrue(wrong.isEmpty(), "Armor tiers: " + String.join("; ", wrong));
		helper.succeed();
	}

	private static Item vanilla(String path) {
		return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(path));
	}

	/** A data component's value on a stack, by the component's ID ("minecraft:repairable"), or null if absent or unknown. */
	private static Object component(ItemStack stack, String id) {
		var type = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.parse(id));
		return type == null ? null : stack.get(type);
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
}
