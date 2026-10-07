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
					"minecraft:diamond", false));

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
