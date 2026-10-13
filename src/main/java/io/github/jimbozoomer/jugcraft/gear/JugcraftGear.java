package io.github.jimbozoomer.jugcraft.gear;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.tools.Chargeable;
import io.github.jimbozoomer.jugcraft.tools.JugcraftTools;
import io.github.jimbozoomer.jugcraft.weapons.DescribedItem;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;

/**
 * Bronze and steel tools and armor, and paxels for every tier (batch 25, docs/features/tools-and-armor.md), after
 * Mekanism: Tools (MIT); none of its code or art is used. Plain vanilla-style items: the 26.3 item properties do the
 * work. Keep the lists in sync with tools/gear.py; tools/check_mod_data.py checks them.
 * <p>Batch 27 (docs/features/gear-and-plastic.md) adds the scuba mask and tank, free runners, and the JE-powered
 * katana and bow, after Mekanism's scuba gear, free runners, Meka-Tana and Meka-Bow (MIT; none of its code or art).
 * <p>Steampunk and Kaiser Armor (docs/features/steampunk-and-kaiser-armor.md) are bronze and steel armor in another
 * look: their materials are the metal's with only the equipment asset changed ({@link #restyle}), and a smithing
 * template ({@link #STYLE_TEMPLATES}) turns a plain piece into a styled one and back (data-driven recipes).
 * <p>Thallite (docs/features/thallite.md) is the third metal with a full set, and Earthbound thallite a style of its
 * armor, one-way and with a perk; their traits, Regrowth and Rooted, are {@link ThalliteGear}.
 * <p>Armor-only tiers ({@link #ARMOR_TIERS}; docs/features/bloodthorn-armor.md) are the owner's own armor designs, each
 * a new tier with numbers of its own, worn as a 3D model (client/WornModelLayer) with no flat layer. They have no recipe
 * or drop yet, so for now they come only from the creative tab.
 */
public final class JugcraftGear {
	/** Metals with a full set of tools and armor (tools/gear.py: GEAR_TIERS). */
	public static final List<String> TIERS = List.of("bronze", "steel", "thallite");
	public static final List<String> PIECES = List.of("sword", "pickaxe", "axe", "shovel", "hoe", "helmet", "chestplate",
			"leggings", "boots");
	/** Paxel tiers (tools/gear.py: PAXEL_TIERS). */
	public static final List<String> PAXEL_TIERS = List.of("wood", "stone", "iron", "gold", "diamond", "netherite", "bronze",
			"steel");
	/** A paxel lasts as long as the three tools it is made from. */
	public static final int PAXEL_DURABILITY = 3;
	/** What a paxel mines fast: everything a pickaxe, axe or shovel does (data/jugcraft/tags/block/mineable/paxel.json). */
	public static final TagKey<Block> MINEABLE_WITH_PAXEL = TagKey.create(Registries.BLOCK, Jugcraft.id("mineable/paxel"));

	/** Bronze: iron-tier drops, a little more durable and quicker than iron (iron: 250, 6.0, 2.0, 14). */
	public static final ToolMaterial BRONZE = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 320, 6.5F, 2.0F, 14,
			repairs("bronze"));
	/** Steel: diamond-tier drops, between iron and diamond in everything else (diamond: 1561, 8.0, 3.0, 10). */
	public static final ToolMaterial STEEL = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 900, 7.0F, 2.5F, 12,
			repairs("steel"));
	/** Bronze armor: iron's defense with a little toughness. */
	public static final ArmorMaterial BRONZE_ARMOR = new ArmorMaterial(15, defense(2, 5, 6, 2), 12,
			SoundEvents.ARMOR_EQUIP_IRON, 0.5F, 0.0F, repairs("bronze"), asset("bronze"));
	/** Steel armor: between iron and diamond. */
	public static final ArmorMaterial STEEL_ARMOR = new ArmorMaterial(25, defense(3, 6, 7, 3), 10,
			SoundEvents.ARMOR_EQUIP_IRON, 1.5F, 0.05F, repairs("steel"), asset("steel"));
	/** Thallite: iron's drops and speed, fewer uses, and the best enchantability; its gear regrows (ThalliteGear). */
	public static final ToolMaterial THALLITE = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 200, 6.0F, 2.0F, 18,
			repairs("thallite"));
	/** Thallite armor: iron's defense, a little less durable, and the best enchantability. */
	public static final ArmorMaterial THALLITE_ARMOR = new ArmorMaterial(13, defense(2, 5, 6, 2), 18,
			SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F, repairs("thallite"), asset("thallite"));
	/** Styled armor (tools/gear.py: ARMOR_STYLES): a metal's armor in every number, in another look. */
	public static final List<String> ARMOR_STYLES = List.of("steampunk", "kaiser", "earthbound_thallite");
	/** Their smithing templates (tools/gear.py: style_templates()). */
	public static final List<String> STYLE_TEMPLATES = List.of("steampunk_pattern", "kaiser_pattern", "earthbinding_template");
	/** Steampunk armor: bronze armor in the steampunk look (goggles, gauge, boiler). */
	public static final ArmorMaterial STEAMPUNK_ARMOR = restyle(BRONZE_ARMOR, "steampunk");
	/** Kaiser armor: steel armor in the kaiserpunk parade dress of the Winged Cog. */
	public static final ArmorMaterial KAISER_ARMOR = restyle(STEEL_ARMOR, "kaiser");
	/** Earthbound thallite: thallite armor bound with gold, one-way; it adds Rooted (ThalliteGear). */
	public static final ArmorMaterial EARTHBOUND_THALLITE_ARMOR = restyle(THALLITE_ARMOR, "earthbound_thallite");
	/** The styled armor's templates, by id, for the Ingredients tab. */
	public static final Map<String, Item> TEMPLATES = new LinkedHashMap<>();
	/** Armor-only tiers with numbers of their own, each worn as a 3D model (tools/gear.py: ARMOR_TIERS). */
	public static final List<String> ARMOR_TIERS = List.of("bloodthorn", "reforged_white_diamond", "hades", "sunset_gem", "pharaoh",
			"dread_knight", "valkyrie", "wayfarer", "spartan", "berserker", "paladin", "templar", "sentinel", "frost_knight",
			"wight_king", "reaper", "banana", "scarab");
	/** Bloodthorn: a step above netherite (37, 3/6/8/3, 15, 3.0, 0.1) in durability, defense, toughness and knockback. */
	public static final ArmorMaterial BLOODTHORN_ARMOR = new ArmorMaterial(40, defense(3, 7, 9, 3), 15,
			SoundEvents.ARMOR_EQUIP_NETHERITE, 3.5F, 0.15F, repairs("bloodthorn"), asset("bloodthorn"));
	/** Reforged White Diamond: beside Bloodthorn, in other strengths: a heavier helm, the longest wear, the best
	 * enchanting; netherite's toughness and knockback resistance, and no fire resistance. */
	public static final ArmorMaterial REFORGED_WHITE_DIAMOND_ARMOR = new ArmorMaterial(45, defense(3, 7, 8, 4), 20,
			SoundEvents.ARMOR_EQUIP_DIAMOND, 3.0F, 0.1F, repairs("reforged_white_diamond"), asset("reforged_white_diamond"));
	/** Hades: beside the other two, in other strengths: the toughest and steadiest, a point less defense, the poorest
	 * enchanting; fire resistant. */
	public static final ArmorMaterial HADES_ARMOR = new ArmorMaterial(42, defense(3, 7, 8, 3), 12,
			SoundEvents.ARMOR_EQUIP_NETHERITE, 4.0F, 0.2F, repairs("hades"), asset("hades"));
	/** Sunset Gem: beside the others, in other strengths: the longest wear and the best enchanting of all; a point less
	 * defense than Bloodthorn, netherite's toughness, no fire resistance. */
	public static final ArmorMaterial SUNSET_GEM_ARMOR = new ArmorMaterial(48, defense(3, 7, 8, 3), 25,
			SoundEvents.ARMOR_EQUIP_GOLD, 3.0F, 0.1F, repairs("sunset_gem"), asset("sunset_gem"));
	/** Pharaoh: beside the others, in other strengths: the heaviest helm and chest, leggings like netherite's, good
	 * enchanting, netherite's toughness; fire resistant. */
	public static final ArmorMaterial PHARAOH_ARMOR = new ArmorMaterial(41, defense(3, 6, 9, 4), 22,
			SoundEvents.ARMOR_EQUIP_GOLD, 3.0F, 0.1F, repairs("pharaoh"), asset("pharaoh"));
	/** Dread Knight: the heaviest plate of all (23 armor for a set) and among the steadiest, the poorest enchanting; fire
	 * resistant (docs/features/four-armor-designs.md, as the three below). */
	public static final ArmorMaterial DREAD_KNIGHT_ARMOR = new ArmorMaterial(44, defense(3, 7, 9, 4), 10,
			SoundEvents.ARMOR_EQUIP_NETHERITE, 3.5F, 0.2F, repairs("dread_knight"), asset("dread_knight"));
	/** Valkyrie: a point of defense over netherite, its toughness, long wear and good enchanting; mended with phantom
	 * membranes. */
	public static final ArmorMaterial VALKYRIE_ARMOR = new ArmorMaterial(46, defense(3, 7, 8, 3), 24,
			SoundEvents.ARMOR_EQUIP_GOLD, 3.0F, 0.1F, repairs("valkyrie"), asset("valkyrie"));
	/** Wayfarer: netherite's defense, the longest wear and the best enchanting of all, less toughness and no knockback
	 * resistance; mended with leather. */
	public static final ArmorMaterial WAYFARER_ARMOR = new ArmorMaterial(50, defense(3, 6, 8, 3), 30,
			SoundEvents.ARMOR_EQUIP_LEATHER, 2.5F, 0.0F, repairs("wayfarer"), asset("wayfarer"));
	/** Spartan: the heavier helm, tough and steady, middling enchanting; mended with bronze. */
	public static final ArmorMaterial SPARTAN_ARMOR = new ArmorMaterial(43, defense(3, 7, 8, 4), 18,
			SoundEvents.ARMOR_EQUIP_GOLD, 3.5F, 0.15F, repairs("spartan"), asset("spartan"));
	/** Berserker: a point of defense over netherite, steady, middling enchanting; mended with quartz
	 * (docs/features/armor-designs-8-october.md, as the two below). */
	public static final ArmorMaterial BERSERKER_ARMOR = new ArmorMaterial(45, defense(3, 7, 8, 3), 16,
			SoundEvents.ARMOR_EQUIP_IRON, 3.0F, 0.15F, repairs("berserker"), asset("berserker"));
	/** Paladin: the heavier helm, long wear and good enchanting, netherite's toughness; mended with amethyst shards. */
	public static final ArmorMaterial PALADIN_ARMOR = new ArmorMaterial(47, defense(3, 6, 8, 4), 22,
			SoundEvents.ARMOR_EQUIP_DIAMOND, 3.0F, 0.1F, repairs("paladin"), asset("paladin"));
	/** Templar: the heavier chest, tough and steady, poorer enchanting; fire resistant. */
	public static final ArmorMaterial TEMPLAR_ARMOR = new ArmorMaterial(43, defense(3, 7, 9, 3), 14,
			SoundEvents.ARMOR_EQUIP_NETHERITE, 3.5F, 0.15F, repairs("templar"), asset("templar"));
	/** Sentinel: the heavier helm, tough and steady, good enchanting; mended with gold. */
	public static final ArmorMaterial SENTINEL_ARMOR = new ArmorMaterial(46, defense(3, 7, 8, 4), 20,
			SoundEvents.ARMOR_EQUIP_GOLD, 3.5F, 0.15F, repairs("sentinel"), asset("sentinel"));
	/** Frost Knight: a point of defense over netherite, steady; mended with blue ice. */
	public static final ArmorMaterial FROST_KNIGHT_ARMOR = new ArmorMaterial(44, defense(3, 7, 8, 3), 18,
			SoundEvents.ARMOR_EQUIP_DIAMOND, 3.0F, 0.15F, repairs("frost_knight"), asset("frost_knight"));
	/** Wight King: the heavier helm, the steadiest of the cold sets, middling enchanting; mended with packed ice. */
	public static final ArmorMaterial WIGHT_KING_ARMOR = new ArmorMaterial(45, defense(3, 7, 8, 4), 15,
			SoundEvents.ARMOR_EQUIP_IRON, 3.0F, 0.2F, repairs("wight_king"), asset("wight_king"));
	/** Reaper: netherite's defense, good enchanting, less toughness and barely any knockback resistance; mended with
	 * bone. */
	public static final ArmorMaterial REAPER_ARMOR = new ArmorMaterial(42, defense(3, 6, 8, 3), 24,
			SoundEvents.ARMOR_EQUIP_LEATHER, 2.5F, 0.05F, repairs("reaper"), asset("reaper"));
	/** Banana: a costume: iron's defense, no toughness or knockback resistance, long wear and the best enchanting of
	 * all; mended with yellow wool. */
	public static final ArmorMaterial BANANA_ARMOR = new ArmorMaterial(25, defense(2, 5, 6, 2), 30,
			SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, repairs("banana"), asset("banana"));
	/** Scarab: gold and lapis: netherite's defense with gold's enchanting, less toughness; mended with lapis lazuli. */
	public static final ArmorMaterial SCARAB_ARMOR = new ArmorMaterial(40, defense(3, 6, 8, 3), 25,
			SoundEvents.ARMOR_EQUIP_GOLD, 2.0F, 0.1F, repairs("scarab"), asset("scarab"));

	/** Scuba gear: leather-like protection, repaired with rubber. */
	public static final ArmorMaterial SCUBA_ARMOR = new ArmorMaterial(10, defense(1, 1, 2, 1), 10,
			SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, repairs("rubber"), asset("scuba"));
	/** Free runners: iron boots' defense; the attributes below are the point of them. */
	public static final ArmorMaterial RUNNERS_ARMOR = new ArmorMaterial(12, defense(2, 2, 2, 2), 12,
			SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, repairs("rubber"), asset("free_runners"));
	/** Free runners take this much off fall damage (-1: all of it) and add this much step height (half a block). */
	public static final double RUNNERS_FALL_DAMAGE = -1.0;
	public static final double RUNNERS_STEP_HEIGHT = 0.5;
	/**
	 * The katana's blade: 4 + 6 = 10 attack damage plus the hand's 1, against the netherite sword's 8, and quick
	 * (-2.2 against a sword's -2.4). Unbreakable, so its durability is unused.
	 */
	public static final ToolMaterial KATANA = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1, 8.0F, 4.0F, 10,
			repairs("steel"));

	/** Oxygen in a scuba tank, in mB. */
	public static DataComponentType<Integer> OXYGEN;
	public static Item SCUBA_MASK;
	public static Item SCUBA_TANK;
	public static Item FREE_RUNNERS;
	public static Item POWER_KATANA;
	public static Item POWER_BOW;

	/** Every item, by id, in registration order. */
	public static final Map<String, Item> ITEMS = new LinkedHashMap<>();

	private JugcraftGear() {
	}

	public static void register() {
		set("bronze", BRONZE, BRONZE_ARMOR, 6.0F, -3.1F, -2.0F, -1.0F, Item::new);
		set("steel", STEEL, STEEL_ARMOR, 5.5F, -3.0F, -2.5F, 0.0F, Item::new);
		set("thallite", THALLITE, THALLITE_ARMOR, 6.0F, -3.1F, -2.0F, -1.0F, properties -> new ThalliteGearItem(properties, false));
		styled("steampunk", STEAMPUNK_ARMOR, DescribedItem::new);
		styled("kaiser", KAISER_ARMOR, DescribedItem::new);
		styled("earthbound_thallite", EARTHBOUND_THALLITE_ARMOR, properties -> new ThalliteGearItem(properties, true));
		ThalliteGear.register();
		armorTier("bloodthorn", BLOODTHORN_ARMOR, true);
		armorTier("reforged_white_diamond", REFORGED_WHITE_DIAMOND_ARMOR, false);
		armorTier("hades", HADES_ARMOR, true);
		armorTier("sunset_gem", SUNSET_GEM_ARMOR, false);
		armorTier("pharaoh", PHARAOH_ARMOR, true);
		armorTier("dread_knight", DREAD_KNIGHT_ARMOR, true);
		armorTier("valkyrie", VALKYRIE_ARMOR, false);
		armorTier("wayfarer", WAYFARER_ARMOR, false);
		armorTier("spartan", SPARTAN_ARMOR, false);
		armorTier("berserker", BERSERKER_ARMOR, false);
		armorTier("paladin", PALADIN_ARMOR, false);
		armorTier("templar", TEMPLAR_ARMOR, true);
		armorTier("sentinel", SENTINEL_ARMOR, false);
		armorTier("frost_knight", FROST_KNIGHT_ARMOR, false);
		armorTier("wight_king", WIGHT_KING_ARMOR, false);
		armorTier("reaper", REAPER_ARMOR, false);
		armorTier("banana", BANANA_ARMOR, false);
		armorTier("scarab", SCARAB_ARMOR, false);
		for (String id : STYLE_TEMPLATES) {
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
			TEMPLATES.put(id, Registry.register(BuiltInRegistries.ITEM, key,
					new DescribedItem(new Item.Properties().setId(key).rarity(Rarity.UNCOMMON))));
		}
		for (String tier : PAXEL_TIERS) {
			ToolMaterial base = baseMaterial(tier);
			ToolMaterial paxel = new ToolMaterial(base.incorrectBlocksForDrops(), base.durability() * PAXEL_DURABILITY,
					base.speed(), base.attackDamageBonus(), base.enchantmentValue(), base.repairItems());
			item(tier + "_paxel", properties -> {
				Item.Properties tool = properties.tool(paxel, MINEABLE_WITH_PAXEL, 5.0F, -3.0F, 0.0F);
				return new Item(tier.equals("netherite") ? tool.fireResistant() : tool);
			});
		}
		extras();
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> ITEMS.forEach(
				(id, item) -> {
					if (!id.endsWith("_sword") && !isArmor(id) && !isCombatExtra(id)) {
						output.accept(item);
					}
				}));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> ITEMS.forEach((id, item) -> {
			if (isCombatExtra(id)) {
				output.accept(item);
				output.accept(full(item));
			} else if (id.endsWith("_sword") || isArmor(id)) {
				output.accept(item);
			}
		}));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> TEMPLATES.values()
				.forEach(output::accept));
	}

	/** Batch 27: scuba mask and tank, free runners, power katana and power bow. */
	private static void extras() {
		OXYGEN = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Jugcraft.id("oxygen"),
				DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());
		SCUBA_MASK = item("scuba_mask", properties -> new Item(properties.humanoidArmor(SCUBA_ARMOR, ArmorType.HELMET)));
		SCUBA_TANK = item("scuba_tank", properties -> new ScubaTankItem(properties.humanoidArmor(SCUBA_ARMOR,
				ArmorType.CHESTPLATE).component(OXYGEN, 0)));
		FREE_RUNNERS = item("free_runners", properties -> new Item(properties.humanoidArmor(RUNNERS_ARMOR, ArmorType.BOOTS)
				.attributes(RUNNERS_ARMOR.createAttributes(ArmorType.BOOTS)
						.withModifierAdded(Attributes.FALL_DAMAGE_MULTIPLIER, new AttributeModifier(Jugcraft.id("free_runners_fall"),
								RUNNERS_FALL_DAMAGE, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.FEET)
						.withModifierAdded(Attributes.STEP_HEIGHT, new AttributeModifier(Jugcraft.id("free_runners_step"),
								RUNNERS_STEP_HEIGHT, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.FEET))));
		POWER_KATANA = item("power_katana", properties -> new PowerKatanaItem(powered(properties.sword(KATANA, 6.0F, -2.2F))));
		POWER_BOW = item("power_bow", properties -> new PowerBowItem(powered(properties.enchantable(1))));
	}

	/** One of a kind, unbreakable (they run on JE, not durability), starting empty, like the powered tools. */
	static Item.Properties powered(Item.Properties properties) {
		return properties.stacksTo(1).rarity(Rarity.UNCOMMON).component(JugcraftTools.ENERGY, 0L)
				.component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
				.component(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.UNBREAKABLE, true));
	}

	/** A charged katana, bow or exosuit piece, or a full scuba tank, for the creative tab. */
	private static ItemStack full(Item item) {
		ItemStack stack = new ItemStack(item);
		if (item instanceof Chargeable) {
			Chargeable.setEnergy(stack, Chargeable.capacity(stack));
		} else if (item instanceof ScubaTankItem) {
			ScubaTankItem.setOxygen(stack, ScubaTankItem.CAPACITY);
		}
		return stack;
	}

	private static boolean isCombatExtra(String id) {
		return id.startsWith("scuba_") || id.equals("free_runners") || id.startsWith("power_") || id.contains("exosuit_")
				|| id.equals("ronin_katana");
	}

	/**
	 * One metal's sword, pickaxe, axe, shovel and hoe, then its helmet, chestplate, leggings and boots, each made by
	 * {@code make} (a plain Item, or thallite's with its traits) from properties that already carry its tool or armor.
	 */
	private static void set(String tier, ToolMaterial material, ArmorMaterial armor, float axeDamage, float axeSpeed,
			float hoeDamage, float hoeSpeed, Function<Item.Properties, Item> make) {
		item(tier + "_sword", properties -> make.apply(properties.sword(material, 3.0F, -2.4F)));
		item(tier + "_pickaxe", properties -> make.apply(properties.pickaxe(material, 1.0F, -2.8F)));
		item(tier + "_axe", properties -> make.apply(properties.axe(material, axeDamage, axeSpeed)));
		item(tier + "_shovel", properties -> make.apply(properties.shovel(material, 1.5F, -3.0F)));
		item(tier + "_hoe", properties -> make.apply(properties.hoe(material, hoeDamage, hoeSpeed)));
		item(tier + "_helmet", properties -> make.apply(properties.humanoidArmor(armor, ArmorType.HELMET)));
		item(tier + "_chestplate", properties -> make.apply(properties.humanoidArmor(armor, ArmorType.CHESTPLATE)));
		item(tier + "_leggings", properties -> make.apply(properties.humanoidArmor(armor, ArmorType.LEGGINGS)));
		item(tier + "_boots", properties -> make.apply(properties.humanoidArmor(armor, ArmorType.BOOTS)));
	}

	/**
	 * A style's helmet, chestplate, leggings and boots: its metal's armor in another look, common like the plain piece,
	 * with a grey line of lore (tooltip.jugcraft.&lt;id&gt;), made by {@code make} (Earthbound's adds its traits).
	 */
	private static void styled(String style, ArmorMaterial armor, Function<Item.Properties, Item> make) {
		item(style + "_helmet", properties -> make.apply(properties.humanoidArmor(armor, ArmorType.HELMET)));
		item(style + "_chestplate", properties -> make.apply(properties.humanoidArmor(armor, ArmorType.CHESTPLATE)));
		item(style + "_leggings", properties -> make.apply(properties.humanoidArmor(armor, ArmorType.LEGGINGS)));
		item(style + "_boots", properties -> make.apply(properties.humanoidArmor(armor, ArmorType.BOOTS)));
	}

	/**
	 * An armor-only tier's helmet, chestplate, leggings and boots: plain armor items in its own material, fire resistant
	 * as netherite is if {@code fireResistant}. How they look is their 3D model (tools/&lt;tier&gt;_armor.py).
	 */
	private static void armorTier(String tier, ArmorMaterial armor, boolean fireResistant) {
		Function<Item.Properties, Item.Properties> fire = properties -> fireResistant ? properties.fireResistant() : properties;
		item(tier + "_helmet", properties -> new Item(fire.apply(properties.humanoidArmor(armor, ArmorType.HELMET))));
		item(tier + "_chestplate", properties -> new Item(fire.apply(properties.humanoidArmor(armor, ArmorType.CHESTPLATE))));
		item(tier + "_leggings", properties -> new Item(fire.apply(properties.humanoidArmor(armor, ArmorType.LEGGINGS))));
		item(tier + "_boots", properties -> new Item(fire.apply(properties.humanoidArmor(armor, ArmorType.BOOTS))));
	}

	/** The same armor material worn in another look: every number, sound and repair tag is the base's. */
	private static ArmorMaterial restyle(ArmorMaterial base, String style) {
		return new ArmorMaterial(base.durability(), base.defense(), base.enchantmentValue(), base.equipSound(),
				base.toughness(), base.knockbackResistance(), base.repairIngredient(), asset(style));
	}

	private static ToolMaterial baseMaterial(String tier) {
		return switch (tier) {
			case "wood" -> ToolMaterial.WOOD;
			case "stone" -> ToolMaterial.STONE;
			case "iron" -> ToolMaterial.IRON;
			case "gold" -> ToolMaterial.GOLD;
			case "diamond" -> ToolMaterial.DIAMOND;
			case "netherite" -> ToolMaterial.NETHERITE;
			case "bronze" -> BRONZE;
			case "steel" -> STEEL;
			default -> throw new IllegalArgumentException(tier);
		};
	}

	private static boolean isArmor(String id) {
		return id.endsWith("_helmet") || id.endsWith("_chestplate") || id.endsWith("_leggings") || id.endsWith("_boots");
	}

	/** Defense by piece; the body value is only for animal armor and unused here. */
	private static Map<ArmorType, Integer> defense(int boots, int leggings, int chestplate, int helmet) {
		return Map.of(ArmorType.BOOTS, boots, ArmorType.LEGGINGS, leggings, ArmorType.CHESTPLATE, chestplate,
				ArmorType.HELMET, helmet, ArmorType.BODY, chestplate);
	}

	private static TagKey<Item> repairs(String metal) {
		return TagKey.create(Registries.ITEM, Jugcraft.id("repairs_" + metal + "_gear"));
	}

	private static ResourceKey<EquipmentAsset> asset(String metal) {
		return ResourceKey.create(EquipmentAssets.ROOT_ID, Jugcraft.id(metal));
	}

	/** Registers an item and lists it in ITEMS (and so in the creative tabs above); also used by JugcraftExosuit. */
	static Item item(String name, Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
		ITEMS.put(name, item);
		return item;
	}
}
