package io.github.jimbozoomer.jugcraft.weapons;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.gear.JugcraftGear;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * Arms VII (batch 56, docs/features/arms-vii.md): named variants of the arms. Each is an {@link ArmItem} of one of
 * {@link JugcraftArms#KINDS}, so its swing, reach, trait, two-handed blow, weapon art and motion are its kind's, made as
 * a steel arm is, with its line's perk or boon:
 *
 * <ul>
 * <li>four styles, made at a smithing table from a steel arm of the kind, the style's pattern ({@link #PATTERNS}) and a
 * material, keeping the arm's enchantments and wear: gilded (takes enchantments as gold does), ironclad (twice as
 * hard-wearing), bonecarved (strikes the undead harder) and runebound (marks what it strikes);</li>
 * <li>the trophies of eight bosses still to be made (docs/branches/BOSSES.md), with no recipe: each boss's loot table
 * (loot_table/bosses/&lt;boss&gt;.json) drops one of its two. They are twice as hard-wearing as steel and carry a boon.</li>
 * </ul>
 *
 * <p>Every boon is worked on the server, when its arm strikes ({@link ArmItem#hurtEnemy}, {@link ArmItem#boonBonus}),
 * and is bounded: an effect of at most 5 s and amplifier 1, which another hit refreshes but never stacks, or at most half
 * the blow again; no variant deals as much a second as a netherite sword, whatever its boon adds. Keep {@link #VARIANTS} and the numbers in sync with tools/arms_variants.py; tools/check_mod_data.py
 * checks them.
 */
public final class ArmVariants {
	/**
	 * What a variant's hit does besides its kind's: FROST slows; EMBER sets alight; VENOM poisons; DRAIN heals the
	 * wielder; WITHER withers; SHOCK arcs to the nearest other foe; GALE throws the foe up and back; HOWL weakens; TIDE
	 * strikes harder at a foe in water or rain; GRAVEBANE strikes the undead harder; MARK makes the foe glow.
	 */
	public enum Boon {
		FROST, EMBER, VENOM, DRAIN, WITHER, SHOCK, GALE, HOWL, TIDE, GRAVEBANE, MARK
	}

	/** A variant: its id, its kind (a {@link JugcraftArms.Kind}), its line (a style or a boss) and its boon (or null). */
	public record Variant(String name, String kind, String line, Boon boon) {
	}

	/** The crafted styles; any other line is a boss. */
	public static final List<String> STYLES = List.of("gilded", "ironclad", "bonecarved", "runebound");

	public static final List<Variant> VARIANTS = List.of(
			new Variant("gilded_longsword", "longsword", "gilded", null),
			new Variant("gilded_rapier", "rapier", "gilded", null),
			new Variant("gilded_sabre", "sabre", "gilded", null),
			new Variant("gilded_halberd", "halberd", "gilded", null),
			new Variant("ironclad_zweihander", "zweihander", "ironclad", null),
			new Variant("ironclad_maul", "maul", "ironclad", null),
			new Variant("ironclad_war_pick", "war_pick", "ironclad", null),
			new Variant("ironclad_battle_axe", "battle_axe", "ironclad", null),
			new Variant("bonecarved_dagger", "dagger", "bonecarved", Boon.GRAVEBANE),
			new Variant("bonecarved_flail", "flail", "bonecarved", Boon.GRAVEBANE),
			new Variant("bonecarved_glaive", "glaive", "bonecarved", Boon.GRAVEBANE),
			new Variant("bonecarved_labrys", "labrys", "bonecarved", Boon.GRAVEBANE),
			new Variant("runebound_nodachi", "nodachi", "runebound", Boon.MARK),
			new Variant("runebound_moonblade", "moonblade", "runebound", Boon.MARK),
			new Variant("runebound_staff", "quarterstaff", "runebound", Boon.MARK),
			new Variant("runebound_war_hammer", "war_hammer", "runebound", Boon.MARK),
			new Variant("glacier_maul", "maul", "yeti_king", Boon.FROST),
			new Variant("rimeclaw", "katar", "yeti_king", Boon.FROST),
			new Variant("cinderbrand", "greatsword", "cinder_tyrant", Boon.EMBER),
			new Variant("magmaw", "earthbreaker", "cinder_tyrant", Boon.EMBER),
			new Variant("hagthorn", "scythe", "mire_hag", Boon.VENOM),
			new Variant("bogfang", "kama", "mire_hag", Boon.VENOM),
			new Variant("soulreaver", "moonblade", "crypt_lich", Boon.DRAIN),
			new Variant("gravewarden", "executioner", "crypt_lich", Boon.WITHER),
			new Variant("dynamo_halberd", "halberd", "iron_dreadnought", Boon.SHOCK),
			new Variant("piston_hammer", "war_hammer", "iron_dreadnought", Boon.SHOCK),
			new Variant("moonfang", "sabre", "werewolf_alpha", Boon.HOWL),
			new Variant("howler", "twinblade", "werewolf_alpha", Boon.HOWL),
			new Variant("stormcaller", "glaive", "storm_roc", Boon.GALE),
			new Variant("galefeather", "estoc", "storm_roc", Boon.GALE),
			new Variant("tidebreaker", "war_fork", "abyssal_leviathan", Boon.TIDE),
			new Variant("leviathans_hook", "bill", "abyssal_leviathan", Boon.TIDE));

	/** The styles' patterns (smithing templates), in STYLES order. */
	public static final List<String> PATTERN_NAMES = List.of("gilders_pattern", "ironclad_pattern", "bonecarvers_pattern",
			"runecarvers_pattern");

	/** FROST: Slowness for FROST_TICKS at FROST_AMPLIFIER. */
	public static final int FROST_TICKS = 60;
	public static final int FROST_AMPLIFIER = 1;
	/** EMBER: alight for EMBER_SECONDS. */
	public static final int EMBER_SECONDS = 3;
	/** VENOM: Poison. */
	public static final int VENOM_TICKS = 80;
	public static final int VENOM_AMPLIFIER = 0;
	/** WITHER: Wither. */
	public static final int WITHER_TICKS = 60;
	public static final int WITHER_AMPLIFIER = 0;
	/** HOWL: Weakness. */
	public static final int HOWL_TICKS = 60;
	public static final int HOWL_AMPLIFIER = 0;
	/** MARK: Glowing. */
	public static final int MARK_TICKS = 80;
	/** DRAIN: health a hit gives back. */
	public static final float DRAIN_HEAL = 1.0F;
	/** SHOCK: the arc's share of the wielder's attack damage, and how far it reaches. */
	public static final float SHOCK_SHARE = 0.3F;
	public static final float SHOCK_RANGE = 4.0F;
	/** GALE: knockback, and the lift. */
	public static final float GALE_KNOCKBACK = 0.6F;
	public static final float GALE_LIFT = 0.35F;
	/** TIDE: the share more against a foe in water or rain. */
	public static final float TIDE = 0.25F;
	/** GRAVEBANE: the share more against the undead. */
	public static final float GRAVEBANE = 0.2F;
	/** Gilded arms' enchantability (gold tools'; steel's is 12). */
	public static final int GILDED_ENCHANTABILITY = 22;
	/** Ironclad arms and the bosses' trophies last this many times as long as steel. */
	public static final int IRONCLAD_DURABILITY = 2;
	public static final int TROPHY_DURABILITY = 2;

	/** Every variant, by id, in registration order. */
	public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
	/** The patterns, by id. */
	public static final Map<String, Item> PATTERNS = new LinkedHashMap<>();

	private ArmVariants() {
	}

	static void register() {
		for (Variant variant : VARIANTS) {
			JugcraftArms.Kind kind = JugcraftArms.KINDS.stream().filter(k -> k.name().equals(variant.kind())).findFirst()
					.orElseThrow(() -> new IllegalStateException("Arms VII: no kind " + variant.kind()));
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(variant.name()));
			Item.Properties properties = JugcraftArms.arm(new Item.Properties().setId(key), JugcraftGear.STEEL, kind);
			boolean style = STYLES.contains(variant.line());
			if (variant.line().equals("gilded")) {
				properties.enchantable(GILDED_ENCHANTABILITY);
			} else if (variant.line().equals("ironclad")) {
				properties.durability(JugcraftGear.STEEL.durability() * IRONCLAD_DURABILITY);
			} else if (!style) {
				properties.durability(JugcraftGear.STEEL.durability() * TROPHY_DURABILITY);
			}
			properties.rarity(style ? Rarity.UNCOMMON : Rarity.EPIC);
			ITEMS.put(variant.name(), Registry.register(BuiltInRegistries.ITEM, key,
					new ArmItem(variant.kind(), variant.line(), variant.boon(), properties)));
		}
		for (String name : PATTERN_NAMES) {
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(name));
			PATTERNS.put(name, Registry.register(BuiltInRegistries.ITEM, key,
					new DescribedItem(new Item.Properties().setId(key).rarity(Rarity.UNCOMMON))));
		}
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> ITEMS.values().forEach(output::accept));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> PATTERNS.values().forEach(output::accept));
	}

	/** The variant an item is, or null. */
	public static Variant of(Item item) {
		String path = BuiltInRegistries.ITEM.getKey(item).getPath();
		return VARIANTS.stream().filter(v -> v.name().equals(path)).findFirst().orElse(null);
	}
}
