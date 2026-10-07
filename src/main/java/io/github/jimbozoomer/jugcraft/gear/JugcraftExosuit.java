package io.github.jimbozoomer.jugcraft.gear;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;

/**
 * The powered exosuit (batch 28, docs/features/exosuit.md), after Mekanism's MekaSuit (MIT; none of its code or art):
 * four pieces in two liveries, the Ronin katana, and the livery templates that repaint them at a smithing table. Keep
 * the lists in sync with tools/exosuit.py; tools/check_mod_data.py checks them.
 */
public final class JugcraftExosuit {
	/** Livery -> item prefix (tools/exosuit.py: STYLES). */
	public static final Map<ExosuitItem.Style, String> PREFIXES = Map.of(ExosuitItem.Style.VANGUARD, "exosuit",
			ExosuitItem.Style.RONIN, "ronin_exosuit");
	public static final List<ArmorType> PIECES = List.of(ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS,
			ArmorType.BOOTS);

	public static Item RONIN_KATANA;
	public static Item RONIN_LIVERY;
	public static Item VANGUARD_LIVERY;

	private JugcraftExosuit() {
	}

	/** Netherite's protection (3/6/8/3, toughness 3) with less knockback resistance; unbreakable, as it runs on JE. */
	private static ArmorMaterial material(String asset) {
		return new ArmorMaterial(40, Map.of(ArmorType.BOOTS, 3, ArmorType.LEGGINGS, 6, ArmorType.CHESTPLATE, 8,
				ArmorType.HELMET, 3, ArmorType.BODY, 8), 15, SoundEvents.ARMOR_EQUIP_IRON, 3.0F, 0.05F,
				TagKey.create(Registries.ITEM, Jugcraft.id("repairs_steel_gear")),
				ResourceKey.create(EquipmentAssets.ROOT_ID, Jugcraft.id(asset)));
	}

	public static void register() {
		for (ExosuitItem.Style style : ExosuitItem.Style.values()) {
			String prefix = PREFIXES.get(style);
			ArmorMaterial material = material(prefix);
			for (ArmorType type : PIECES) {
				JugcraftGear.item(prefix + "_" + ExosuitItem.piece(type), properties ->
						new ExosuitItem(JugcraftGear.powered(properties.humanoidArmor(material, type)), type, style));
			}
		}
		RONIN_KATANA = JugcraftGear.item("ronin_katana", properties ->
				new PowerKatanaItem(JugcraftGear.powered(properties.sword(JugcraftGear.KATANA, 6.0F, -2.2F))));
		RONIN_LIVERY = JugcraftGear.item("ronin_livery", Item::new);
		VANGUARD_LIVERY = JugcraftGear.item("vanguard_livery", Item::new);
		Exosuit.register();
	}

	/** The registered piece, e.g. piece(RONIN, HELMET) is ronin_exosuit_helmet. */
	public static Item piece(ExosuitItem.Style style, ArmorType type) {
		return JugcraftGear.ITEMS.get(PREFIXES.get(style) + "_" + ExosuitItem.piece(type));
	}
}
