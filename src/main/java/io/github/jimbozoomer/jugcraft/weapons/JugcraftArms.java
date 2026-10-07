package io.github.jimbozoomer.jugcraft.weapons;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.gear.JugcraftGear;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.Weapon;

/**
 * Arms (batch 42, docs/features/arms.md): longswords, greatswords, rapiers, flanged maces, war hammers, glaives,
 * halberds, spears and lances in bronze and steel; and Arms II (batch 45, docs/features/arms-ii.md): daggers, sabres,
 * estocs, battle axes, flails, scythes, quarterstaves and pikes, each with a {@link Trait} of its own; and Arms III
 * (batch 46, docs/features/arms-iii.md): two-handed swings ({@link #TWO_HANDED}) and zweihanders, mauls, executioner's
 * swords and bills; and Arms IV (batch 47, docs/features/arms-iv.md): labryses, battleblades, war forks, kamas and war
 * picks; and Arms V (batch 48, docs/features/arms-v.md): twinblades, nodachis, earthbreakers, katars, moonblades and
 * kusarigamas, each with a weapon art ({@link #ARTS}, worked by {@link WeaponArts}); and Arms VI (batch 55,
 * docs/features/arms-vi.md): katanas and brazier maces, longbows and arbalests ({@link #RANGED}) and heater and tower
 * shields ({@link #SHIELDS}); and Arms VII (batch 56, docs/features/arms-vii.md): named variants of the kinds, crafted
 * in four styles or dropped by bosses ({@link ArmVariants}); and Arms VIII (batch 59, docs/features/arms-viii.md):
 * javelins, franciscas, chakrams and harpoons, arms that are also thrown ({@link #THROWN}, {@link ThrownArmItem},
 * {@link ThrownArm}).
 *
 * <p>After studying how Epic Knights and Simply Swords make, show and animate their weapons (none of their code, models
 * or art is used): every trait here is one of 26.3's own item components, so these are plain items with no per-tick
 * code, and the game runs and checks their swings, reach, parries, thrusts and charges as it does its own weapons, on
 * the server. Their looks are data: one sprite and one model each over a shared in-hand pose per kind
 * (tools/arms.py). Keep {@link #KINDS}, {@link #CHARGES} and the numbers in sync with tools/arms.py;
 * tools/check_mod_data.py checks them.
 */
public final class JugcraftArms {
	/** The metals arms are made in (tools/arms.py: METALS; tools/gear.py: GEAR_TIERS). */
	public static final List<String> METALS = List.of("bronze", "steel");

	/**
	 * A kind of arm swung or thrust like a sword (tools/arms.py: KINDS). Damage is added to the metal's bonus as a
	 * sword's 3 is; speed is the attack-speed modifier; the swing is the attack animation and its length in ticks;
	 * reach is where a hit lands, in blocks, and margin widens the target's hitbox; disable is how long a hit stops a
	 * shield blocking (s) and wear the durability a hit costs; knockback is extra attack knockback; parry is the share
	 * of damage from in front that blocking with it stops (0: it does not block). Sword kinds sweep, mine cobwebs and
	 * take sword enchantments; pierce kinds thrust through every target in line, as the spear does.
	 */
	public record Kind(String name, float damage, float speed, SwingAnimationType swing, int swingTicks, float minReach,
			float maxReach, float margin, float disable, int wear, float knockback, float parry, boolean sword, boolean pierce) {
	}

	public static final List<Kind> KINDS = List.of(
			new Kind("longsword", 4.0F, -2.7F, SwingAnimationType.WHACK, 8, 0.0F, 3.25F, 0.0F, 0.0F, 1, 0.0F, 0.6F, true, false),
			new Kind("greatsword", 7.0F, -3.2F, SwingAnimationType.WHACK, 20, 0.0F, 3.75F, 0.0F, 2.0F, 1, 0.5F, 0.0F, true, false),
			new Kind("rapier", 1.5F, -2.0F, SwingAnimationType.STAB, 5, 0.0F, 3.5F, 0.125F, 0.0F, 1, 0.0F, 0.35F, true, false),
			new Kind("flanged_mace", 6.0F, -3.1F, SwingAnimationType.WHACK, 9, 0.0F, 3.0F, 0.0F, 3.0F, 1, 0.0F, 0.0F, false, false),
			new Kind("war_hammer", 8.0F, -3.3F, SwingAnimationType.WHACK, 22, 0.0F, 3.0F, 0.0F, 5.0F, 2, 1.0F, 0.0F, false, false),
			new Kind("glaive", 6.0F, -3.1F, SwingAnimationType.WHACK, 18, 0.0F, 4.25F, 0.0F, 0.0F, 1, 0.0F, 0.0F, true, false),
			new Kind("halberd", 7.0F, -3.2F, SwingAnimationType.STAB, 12, 1.0F, 4.5F, 0.125F, 3.0F, 1, 0.0F, 0.0F, false, true),
			// Arms II (batch 45).
			new Kind("dagger", 1.0F, -1.7F, SwingAnimationType.STAB, 4, 0.0F, 2.5F, 0.0F, 0.0F, 1, 0.0F, 0.0F, false, false),
			new Kind("sabre", 2.0F, -2.2F, SwingAnimationType.WHACK, 6, 0.0F, 3.0F, 0.0F, 0.0F, 1, 0.0F, 0.0F, true, false),
			new Kind("estoc", 3.0F, -2.6F, SwingAnimationType.STAB, 7, 0.0F, 3.5F, 0.125F, 0.0F, 1, 0.0F, 0.0F, false, false),
			new Kind("battle_axe", 8.0F, -3.3F, SwingAnimationType.WHACK, 22, 0.0F, 3.25F, 0.0F, 5.0F, 2, 0.5F, 0.0F, false, false),
			new Kind("flail", 5.0F, -3.0F, SwingAnimationType.WHACK, 10, 0.0F, 3.25F, 0.0F, 0.0F, 1, 0.0F, 0.0F, false, false),
			new Kind("scythe", 5.0F, -3.0F, SwingAnimationType.WHACK, 18, 0.0F, 4.0F, 0.0F, 0.0F, 1, 0.0F, 0.0F, true, false),
			new Kind("quarterstaff", 2.0F, -2.4F, SwingAnimationType.WHACK, 12, 0.0F, 3.5F, 0.0F, 0.0F, 1, 1.0F, 0.5F, false, false),
			new Kind("pike", 5.0F, -3.2F, SwingAnimationType.STAB, 16, 2.0F, 5.0F, 0.125F, 0.0F, 1, 0.0F, 0.0F, false, false),
			// Arms III (batch 46).
			new Kind("zweihander", 7.5F, -3.2F, SwingAnimationType.WHACK, 22, 0.0F, 4.0F, 0.0F, 2.0F, 1, 0.5F, 0.5F, true, false),
			new Kind("maul", 10.0F, -3.45F, SwingAnimationType.WHACK, 24, 0.0F, 3.25F, 0.0F, 5.0F, 2, 1.5F, 0.0F, false, false),
			new Kind("executioner", 8.0F, -3.3F, SwingAnimationType.WHACK, 22, 0.0F, 3.5F, 0.0F, 0.0F, 1, 0.5F, 0.0F, true, false),
			new Kind("bill", 6.0F, -3.1F, SwingAnimationType.WHACK, 18, 0.0F, 4.5F, 0.0F, 0.0F, 1, 0.0F, 0.0F, false, false),
			// Arms IV (batch 47).
			new Kind("labrys", 8.5F, -3.3F, SwingAnimationType.WHACK, 22, 0.0F, 3.25F, 0.0F, 3.0F, 1, 0.5F, 0.0F, false, false),
			new Kind("battleblade", 7.0F, -3.2F, SwingAnimationType.WHACK, 20, 0.0F, 3.5F, 0.0F, 2.0F, 1, 0.5F, 0.0F, true, false),
			new Kind("war_fork", 5.5F, -3.1F, SwingAnimationType.STAB, 18, 0.0F, 4.5F, 0.125F, 0.0F, 1, 0.0F, 0.0F, false, false),
			new Kind("kama", 1.5F, -2.0F, SwingAnimationType.WHACK, 5, 0.0F, 2.75F, 0.0F, 0.0F, 1, 0.0F, 0.0F, false, false),
			new Kind("war_pick", 3.0F, -2.6F, SwingAnimationType.WHACK, 7, 0.0F, 3.0F, 0.0F, 0.0F, 1, 0.0F, 0.0F, false, false),
			// Arms V (batch 48): each has a weapon art (ARTS).
			new Kind("twinblade", 4.0F, -2.8F, SwingAnimationType.WHACK, 14, 0.0F, 3.25F, 0.0F, 0.0F, 1, 0.0F, 0.0F, true, false),
			new Kind("nodachi", 6.5F, -3.2F, SwingAnimationType.WHACK, 20, 0.0F, 4.0F, 0.0F, 0.0F, 1, 0.0F, 0.0F, true, false),
			new Kind("earthbreaker", 9.5F, -3.45F, SwingAnimationType.WHACK, 24, 0.0F, 3.25F, 0.0F, 5.0F, 2, 1.0F, 0.0F, false, false),
			new Kind("katar", 1.5F, -2.0F, SwingAnimationType.STAB, 5, 0.0F, 2.75F, 0.0F, 0.0F, 1, 0.0F, 0.0F, false, false),
			new Kind("moonblade", 6.0F, -3.1F, SwingAnimationType.WHACK, 18, 0.0F, 3.75F, 0.0F, 0.0F, 1, 0.0F, 0.0F, true, false),
			new Kind("kusarigama", 2.0F, -2.3F, SwingAnimationType.WHACK, 6, 0.0F, 3.25F, 0.0F, 0.0F, 1, 0.0F, 0.0F, false, false),
			// Arms VI (batch 55); its bows, crossbows and shields are RANGED and SHIELDS.
			new Kind("katana", 3.0F, -2.5F, SwingAnimationType.WHACK, 7, 0.0F, 3.25F, 0.0F, 0.0F, 1, 0.0F, 0.0F, true, false),
			new Kind("brazier_mace", 5.0F, -3.0F, SwingAnimationType.WHACK, 10, 0.0F, 3.0F, 0.0F, 0.0F, 1, 0.0F, 0.0F, false, false),
			// Arms VIII (batch 59): thrown arms; how each flies is THROWN.
			new Kind("javelin", 2.5F, -2.6F, SwingAnimationType.STAB, 7, 0.0F, 3.25F, 0.125F, 0.0F, 1, 0.0F, 0.0F, false, false),
			new Kind("francisca", 4.0F, -2.9F, SwingAnimationType.WHACK, 9, 0.0F, 3.0F, 0.0F, 2.0F, 1, 0.0F, 0.0F, false, false),
			new Kind("chakram", 2.0F, -2.2F, SwingAnimationType.WHACK, 6, 0.0F, 2.75F, 0.0F, 0.0F, 1, 0.0F, 0.0F, false, false),
			new Kind("harpoon", 3.0F, -2.7F, SwingAnimationType.STAB, 8, 0.0F, 3.25F, 0.125F, 0.0F, 1, 0.0F, 0.0F, false, false));

	/**
	 * What an Arms II kind does besides its numbers (tools/arms.py: TRAITS), worked by {@link ArmItem} on the server:
	 * BACKSTAB, a blow from behind deals more; SADDLE, more damage while riding; ARMOR_PIERCE, more the more armor the
	 * target wears; CHOP, mines wood as its metal's axe; DAZE, a hit slows; REAP, use on ripe crops to harvest and
	 * replant them; RIDERS, more damage against anything riding or ridden. Arms III adds QUAKE, the maul's finishing blow
	 * shakes the ground; EXECUTE, more damage against a badly wounded foe; HOOK, a hit pulls the foe in and unhorses it.
	 * Arms IV adds WHIRL, the labrys's finishing blow strikes all round; SUNDER, a hit wears the foe's armor; BRACE, more
	 * damage to a foe charging in; CLEAR, the kama cuts plants and leaves 3 by 3 by 3; DELVE, mines as a pickaxe.
	 */
	public enum Trait {
		BACKSTAB, SADDLE, ARMOR_PIERCE, CHOP, DAZE, REAP, RIDERS, QUAKE, EXECUTE, HOOK, WHIRL, SUNDER, BRACE, CLEAR, DELVE, IGNITE
	}

	public static final Map<String, Trait> TRAITS = Map.ofEntries(Map.entry("dagger", Trait.BACKSTAB), Map.entry("sabre", Trait.SADDLE),
			Map.entry("estoc", Trait.ARMOR_PIERCE), Map.entry("battle_axe", Trait.CHOP), Map.entry("flail", Trait.DAZE),
			Map.entry("scythe", Trait.REAP), Map.entry("pike", Trait.RIDERS), Map.entry("maul", Trait.QUAKE),
			Map.entry("executioner", Trait.EXECUTE), Map.entry("bill", Trait.HOOK), Map.entry("labrys", Trait.WHIRL),
			Map.entry("battleblade", Trait.SUNDER), Map.entry("war_fork", Trait.BRACE), Map.entry("kama", Trait.CLEAR),
			Map.entry("war_pick", Trait.DELVE), Map.entry("brazier_mace", Trait.IGNITE), Map.entry("francisca", Trait.CHOP));
	/** A backstab, within BACKSTAB_ANGLE degrees of straight behind the target's body, deals BACKSTAB of the blow more. */
	public static final float BACKSTAB = 0.5F;
	public static final float BACKSTAB_ANGLE = 70.0F;
	/** A sabre deals SADDLE more while its wielder rides. */
	public static final float SADDLE = 3.0F;
	/** An estoc deals ARMOR_PIERCE more per point of the target's armor, at most ARMOR_PIERCE_MAX. */
	public static final float ARMOR_PIERCE = 0.3F;
	public static final float ARMOR_PIERCE_MAX = 6.0F;
	/** A flail's hit slows the target for DAZE_TICKS at Slowness amplifier DAZE_AMPLIFIER. */
	public static final int DAZE_TICKS = 40;
	public static final int DAZE_AMPLIFIER = 1;
	/** A scythe reaps every ripe crop within REAP_RADIUS of the one used on, at REAP_WEAR durability each. */
	public static final int REAP_RADIUS = 1;
	public static final int REAP_WEAR = 1;
	/** A pike deals RIDERS of the blow more against anything riding or ridden. */
	public static final float RIDERS = 0.5F;
	/**
	 * A maul's finishing blow also strikes every foe within QUAKE_RADIUS blocks (and a block of the wielder's footing)
	 * that the cleave missed, for QUAKE_SHARE of the blow, and slows every foe there for QUAKE_TICKS at Slowness amplifier
	 * QUAKE_AMPLIFIER.
	 */
	public static final float QUAKE_RADIUS = 2.5F;
	public static final float QUAKE_SHARE = 0.5F;
	public static final int QUAKE_TICKS = 40;
	public static final int QUAKE_AMPLIFIER = 1;
	/** An executioner's sword deals EXECUTE of the blow more against a foe at or below EXECUTE_HEALTH of its most health. */
	public static final float EXECUTE = 0.5F;
	public static final float EXECUTE_HEALTH = 0.3F;
	/** A bill's hit pulls the foe towards the wielder at HOOK blocks a tick, less its knockback resistance. */
	public static final float HOOK = 0.6F;
	/** A labrys's finishing blow sweeps WHIRL_ARC degrees, all round, striking up to WHIRL_TARGETS foes. */
	public static final float WHIRL_ARC = 360.0F;
	public static final int WHIRL_TARGETS = 6;
	/** A battleblade's hit wears every piece of armor the foe wears by SUNDER more. */
	public static final int SUNDER = 4;
	/** A war fork deals BRACE of the blow more to a foe closing on its wielder at BRACE_SPEED blocks a tick or more. */
	public static final float BRACE = 0.5F;
	public static final float BRACE_SPEED = 0.1F;
	/** A kama cuts every block in #jugcraft:kama_cuts within CLEAR_RADIUS of the one used on, at CLEAR_WEAR each. */
	public static final int CLEAR_RADIUS = 1;
	public static final int CLEAR_WEAR = 1;
	/** A brazier mace's hit sets the foe alight for IGNITE_SECONDS; lighting a block with it costs IGNITE_WEAR. */
	public static final int IGNITE_SECONDS = 4;
	public static final int IGNITE_WEAR = 1;

	/**
	 * How a two-handed kind swings (tools/arms.py: TWO_HANDED; worked by {@link TwoHanded}): the blow lands strike ticks
	 * after the click, sweeping arc degrees across the wielder's view and striking up to targets foes; the last of its
	 * combo attacks is the finishing blow.
	 */
	public record Heavy(int strike, float arc, int targets, int combo) {
	}

	public static final Map<String, Heavy> TWO_HANDED = Map.ofEntries(
			Map.entry("greatsword", new Heavy(7, 120.0F, 4, 2)),
			Map.entry("war_hammer", new Heavy(8, 70.0F, 2, 2)),
			Map.entry("glaive", new Heavy(6, 120.0F, 4, 2)),
			Map.entry("battle_axe", new Heavy(8, 90.0F, 3, 2)),
			Map.entry("scythe", new Heavy(6, 150.0F, 5, 2)),
			Map.entry("quarterstaff", new Heavy(4, 100.0F, 3, 3)),
			Map.entry("pike", new Heavy(5, 20.0F, 3, 2)),
			Map.entry("zweihander", new Heavy(7, 140.0F, 5, 3)),
			Map.entry("maul", new Heavy(9, 90.0F, 3, 2)),
			Map.entry("executioner", new Heavy(8, 90.0F, 2, 2)),
			Map.entry("bill", new Heavy(6, 90.0F, 3, 2)),
			Map.entry("labrys", new Heavy(8, 100.0F, 3, 2)),
			Map.entry("battleblade", new Heavy(7, 110.0F, 4, 2)),
			Map.entry("war_fork", new Heavy(6, 30.0F, 2, 2)),
			Map.entry("twinblade", new Heavy(5, 140.0F, 3, 3)),
			Map.entry("nodachi", new Heavy(7, 100.0F, 3, 2)),
			Map.entry("earthbreaker", new Heavy(9, 80.0F, 2, 2)),
			Map.entry("moonblade", new Heavy(6, 130.0F, 4, 2)));
	/**
	 * An Arms V kind's special move (tools/arms.py: ARTS; worked by {@link WeaponArts}), used with the use key: CYCLONE,
	 * three spins striking all round; IAIDO, a dash whose cut lands a moment later on every foe passed; LEAP_SLAM, a leap
	 * and a slam where the wielder lands; FLURRY, quick jabs too fast to be shrugged off; CRESCENT, a wave that runs ahead
	 * through foes until a wall stops it; CHAIN_LASH, a chain thrown at the first foe in line, which is hauled in and
	 * reaped.
	 */
	public enum Move {
		CYCLONE, IAIDO, LEAP_SLAM, FLURRY, CRESCENT, CHAIN_LASH, SEVEN_CUTS;

		/** The move's name in ids and translation keys (cyclone, iaido, leap_slam ...). */
		public String id() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/**
	 * A kind's art: its move; the ticks before it is ready again (an item cooldown); the ticks its wielder is busy with it
	 * (the animation; for the leap, its spring); and the share of their speed it takes off them meanwhile.
	 */
	public record Art(Move move, int cooldown, int ticks, float slow) {
	}

	public static final Map<String, Art> ARTS = Map.ofEntries(
			Map.entry("twinblade", new Art(Move.CYCLONE, 120, 24, 0.3F)),
			Map.entry("nodachi", new Art(Move.IAIDO, 160, 18, 0.0F)),
			Map.entry("earthbreaker", new Art(Move.LEAP_SLAM, 200, 8, 0.0F)),
			Map.entry("katar", new Art(Move.FLURRY, 100, 20, 0.5F)),
			Map.entry("moonblade", new Art(Move.CRESCENT, 140, 16, 0.5F)),
			Map.entry("kusarigama", new Art(Move.CHAIN_LASH, 120, 16, 0.5F)),
			Map.entry("katana", new Art(Move.SEVEN_CUTS, 120, 18, 0.4F)));
	/** Cyclone: CYCLONE_HITS hits from CYCLONE_FIRST, every CYCLONE_EVERY ticks, on every foe within CYCLONE_RADIUS. */
	public static final int CYCLONE_FIRST = 6;
	public static final int CYCLONE_EVERY = 6;
	public static final int CYCLONE_HITS = 3;
	public static final float CYCLONE_RADIUS = 3.0F;
	public static final float CYCLONE_SHARE = 0.5F;
	public static final float CYCLONE_PULL = 0.15F;
	public static final int CYCLONE_TARGETS = 8;
	/** Iaido: a dash from IAIDO_START for IAIDO_DASH ticks at IAIDO_SPEED; the cut lands IAIDO_DELAY ticks after it. */
	public static final int IAIDO_START = 3;
	public static final int IAIDO_DASH = 5;
	public static final float IAIDO_SPEED = 1.2F;
	public static final float IAIDO_WIDTH = 1.25F;
	public static final int IAIDO_DELAY = 4;
	public static final float IAIDO_SHARE = 1.3F;
	public static final int IAIDO_TARGETS = 6;
	public static final float IAIDO_REACH = 10.0F;
	/** Leap slam: a spring of LEAP_UP up and LEAP_FORWARD ahead; the slam where the wielder lands, within LEAP_RADIUS. */
	public static final float LEAP_UP = 0.8F;
	public static final float LEAP_FORWARD = 0.5F;
	public static final int LEAP_MIN_AIR = 4;
	public static final int LEAP_STUCK = 12;
	public static final int LEAP_MAX_AIR = 60;
	public static final float LEAP_RADIUS = 3.5F;
	public static final float LEAP_SHARE = 1.0F;
	public static final float LEAP_EDGE = 0.5F;
	public static final float LEAP_PER_BLOCK = 0.15F;
	public static final float LEAP_DROP_MAX = 6.0F;
	public static final float LEAP_LIFT = 0.45F;
	public static final int LEAP_TARGETS = 8;
	/** Flurry: FLURRY_JABS jabs from FLURRY_FIRST, every FLURRY_EVERY ticks, then the finish. */
	public static final int FLURRY_FIRST = 2;
	public static final int FLURRY_EVERY = 3;
	public static final int FLURRY_JABS = 5;
	public static final float FLURRY_SHARE = 0.28F;
	public static final float FLURRY_FINISH = 0.9F;
	public static final float FLURRY_ARC = 50.0F;
	/** Crescent: loosed at CRESCENT_RELEASE, it runs CRESCENT_SPEED blocks a tick for CRESCENT_TICKS ticks. */
	public static final int CRESCENT_RELEASE = 5;
	public static final float CRESCENT_SPEED = 1.2F;
	public static final int CRESCENT_TICKS = 10;
	public static final float CRESCENT_WIDTH = 1.5F;
	public static final float CRESCENT_SHARE = 0.9F;
	public static final float CRESCENT_FADE = 0.15F;
	public static final int CRESCENT_TARGETS = 6;
	/** Chain lash: thrown at LASH_THROW up to LASH_RANGE blocks; the reap at LASH_REAP, within LASH_REAP_REACH. */
	public static final int LASH_THROW = 4;
	public static final float LASH_RANGE = 9.0F;
	public static final float LASH_SHARE = 0.5F;
	public static final float LASH_PULL = 0.2F;
	public static final float LASH_PULL_MAX = 1.6F;
	public static final int LASH_REAP = 11;
	public static final float LASH_REAP_SHARE = 0.8F;
	public static final float LASH_REAP_REACH = 3.5F;
	/** Seven cuts: CUTS_COUNT cuts from CUTS_FIRST, every CUTS_EVERY ticks, each across every foe in reach and CUTS_ARC ahead. */
	public static final int CUTS_FIRST = 3;
	public static final int CUTS_EVERY = 2;
	public static final int CUTS_COUNT = 7;
	public static final float CUTS_SHARE = 0.22F;
	public static final float CUTS_ARC = 110.0F;
	public static final int CUTS_TARGETS = 4;

	/**
	 * A bow or crossbow in one metal (tools/arms.py: RANGED; Arms VI): a longbow draws fully in draw ticks on vanilla's
	 * curve and looses at speed blocks a tick; an arbalest loads as vanilla's crossbow and shoots at speed; their arrows'
	 * base damage is damage (vanilla's 2.0), which the game multiplies by the arrow's speed when it hits.
	 */
	public record Ranged(String name, String metal, int draw, float speed, float damage) {
	}

	public static final List<Ranged> RANGED = List.of(
			new Ranged("longbow", "bronze", 26, 3.4F, 2.0F),
			new Ranged("arbalest", "bronze", 0, 3.4F, 2.1F),
			new Ranged("longbow", "steel", 26, 3.7F, 2.0F),
			new Ranged("arbalest", "steel", 0, 3.55F, 2.1F));
	/** Vanilla's crossbow shoots its arrows at this speed; an arbalest's bolts are this much faster, as its speed is. */
	public static final float CROSSBOW_SPEED = 3.15F;

	/**
	 * A shield in one metal (tools/arms.py: SHIELDS; Arms VI): seconds to raise, degrees either side of ahead it covers,
	 * how long an axe stops it (a share of vanilla's), its durability, the share of a blocked blow it takes in wear, and,
	 * held in either hand, the knockback resistance it braces with and the share of speed it costs.
	 */
	public record Shield(String name, String metal, float delay, float angle, float disable, int durability, float wear, float brace,
			float weight) {
	}

	public static final List<Shield> SHIELDS = List.of(
			new Shield("heater_shield", "bronze", 0.15F, 90.0F, 1.0F, 400, 1.0F, 0.0F, 0.0F),
			new Shield("tower_shield", "bronze", 0.4F, 130.0F, 0.6F, 600, 0.75F, 0.4F, 0.08F),
			new Shield("heater_shield", "steel", 0.1F, 90.0F, 0.8F, 900, 1.0F, 0.0F, 0.0F),
			new Shield("tower_shield", "steel", 0.35F, 130.0F, 0.5F, 1350, 0.75F, 0.5F, 0.08F));

	/**
	 * A thrown arm in one metal (tools/arms.py: THROWN; Arms VIII, batch 59): held at least wind ticks before it is let
	 * go, it leaves at speed blocks a tick, falls gravity blocks a tick faster each tick (0: it flies flat), and strikes
	 * for damage (vanilla's trident: 8), more for the arm's damage enchantments ({@link ThrownArm}).
	 */
	public record Thrown(String name, String metal, int wind, float speed, float gravity, float damage) {
	}

	public static final List<Thrown> THROWN = List.of(
			new Thrown("javelin", "bronze", 10, 2.6F, 0.035F, 7.0F),
			new Thrown("francisca", "bronze", 6, 1.6F, 0.06F, 6.0F),
			new Thrown("chakram", "bronze", 6, 1.4F, 0.0F, 4.0F),
			new Thrown("harpoon", "bronze", 10, 2.0F, 0.04F, 5.0F),
			new Thrown("javelin", "steel", 10, 2.8F, 0.035F, 8.0F),
			new Thrown("francisca", "steel", 6, 1.7F, 0.06F, 7.0F),
			new Thrown("chakram", "steel", 6, 1.5F, 0.0F, 5.0F),
			new Thrown("harpoon", "steel", 10, 2.2F, 0.04F, 6.0F));
	/** A throw wears the arm by THROW_WEAR; another of the same arm can be thrown THROW_COOLDOWN ticks later. */
	public static final int THROW_WEAR = 1;
	public static final int THROW_COOLDOWN = 10;
	/** A thrown francisca stops a raised shield (or parrying arm) blocking for this many seconds, as an axe's blow does. */
	public static final float FRANCISCA_DISABLE = 3.0F;
	/**
	 * The chakram turns back CHAKRAM_RANGE blocks from where it was thrown (or where it meets a block) and flies back at
	 * CHAKRAM_RETURN blocks a tick, through anything; it strikes each foe once each way, up to CHAKRAM_TARGETS each way;
	 * it is caught within CHAKRAM_CATCH blocks of its thrower, and falls where it is after CHAKRAM_MAX_TICKS.
	 */
	public static final float CHAKRAM_RANGE = 12.0F;
	public static final float CHAKRAM_RETURN = 1.2F;
	public static final int CHAKRAM_TARGETS = 4;
	public static final float CHAKRAM_CATCH = 1.5F;
	public static final int CHAKRAM_MAX_TICKS = 100;
	/**
	 * Underwater the harpoon keeps HARPOON_WATER of its speed a tick, as in air; a foe it strikes is hauled towards its
	 * thrower at HARPOON_PULL blocks a tick for each block it is off (at most HARPOON_PULL_MAX), less its knockback
	 * resistance, and dragged from the saddle.
	 */
	public static final float HARPOON_WATER = 0.99F;
	public static final float HARPOON_PULL = 0.2F;
	public static final float HARPOON_PULL_MAX = 1.6F;
	/** A thrown arm in flight. */
	public static EntityType<ThrownArm> THROWN_ARM;

	/** The thrown numbers of a kind in a metal, or null if it is not thrown. */
	public static Thrown thrown(String kind, String metal) {
		for (Thrown thrown : THROWN) {
			if (thrown.name().equals(kind) && thrown.metal().equals(metal)) {
				return thrown;
			}
		}
		return null;
	}

	/** A two-handed swing slows its wielder by this share while it is in the air. */
	public static final float TWO_HANDED_SLOW = 0.6F;
	/** The finishing blow of a combo is this many times as strong. */
	public static final float FINISHER = 1.25F;
	/** A click this many ticks or fewer before a swing ends waits for it. */
	public static final int QUEUE_TICKS = 4;
	/** This many ticks without a click start the combo again. */
	public static final int COMBO_WINDOW = 30;

	/**
	 * A charging kind in one metal (tools/arms.py: CHARGE), as {@link Item.Properties#spear} takes it: jab duration (s),
	 * charge damage multiplier, charge delay (s), and for unhorsing, knockback and damage the longest a charge counts
	 * (s) and the speed it needs.
	 */
	public record Charge(String name, String metal, float jab, float multiplier, float delay, float dismountTime,
			float dismountSpeed, float knockbackTime, float knockbackSpeed, float damageTime, float damageSpeed) {
	}

	public static final List<Charge> CHARGES = List.of(
			new Charge("spear", "bronze", 0.95F, 1.0F, 0.6F, 2.5F, 11.0F, 6.75F, 5.1F, 11.25F, 4.6F),
			new Charge("lance", "bronze", 1.25F, 1.3F, 0.9F, 3.5F, 9.0F, 8.0F, 4.5F, 14.0F, 4.0F),
			new Charge("spear", "steel", 1.0F, 1.04F, 0.55F, 2.75F, 10.5F, 6.6F, 5.1F, 10.6F, 4.6F),
			new Charge("lance", "steel", 1.3F, 1.4F, 0.8F, 4.0F, 8.5F, 8.5F, 4.5F, 15.0F, 4.0F));

	/** The lance's jab adds this to the spear's, and reaches farther: from LANCE_MIN_REACH to LANCE_MAX_REACH blocks. */
	public static final float LANCE_DAMAGE = 1.0F;
	public static final float LANCE_MIN_REACH = 2.5F;
	public static final float LANCE_MAX_REACH = 5.5F;
	/** Creative players reach this much farther, as with vanilla's spears. */
	public static final float CREATIVE_REACH = 2.0F;
	/** A parry covers this many degrees either side of straight ahead (a shield: 90) and blocks after this long (s). */
	public static final float PARRY_ANGLE = 60.0F;
	public static final float PARRY_DELAY = 0.1F;
	/** A parried hit of PARRY_WEAR_THRESHOLD or more wears the arm by PARRY_WEAR_BASE plus PARRY_WEAR_FACTOR of it. */
	public static final float PARRY_WEAR_THRESHOLD = 3.0F;
	public static final float PARRY_WEAR_BASE = 1.0F;
	public static final float PARRY_WEAR_FACTOR = 0.5F;

	/** Every arm, by id, in registration order: each metal's kinds, then its spear and lance. */
	public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
	/** The Arms VI kit, by id, in registration order: each metal's longbow and arbalest, then its shields. */
	public static final Map<String, Item> KIT = new LinkedHashMap<>();

	private JugcraftArms() {
	}

	public static void register() {
		ResourceKey<EntityType<?>> thrownKey = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id("thrown_arm"));
		THROWN_ARM = Registry.register(BuiltInRegistries.ENTITY_TYPE, thrownKey,
				EntityType.Builder.<ThrownArm>of(ThrownArm::new, MobCategory.MISC)
						.sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(10).build(thrownKey));
		for (String metal : METALS) {
			ToolMaterial material = metal.equals("bronze") ? JugcraftGear.BRONZE : JugcraftGear.STEEL;
			for (Kind kind : KINDS) {
				item(metal + "_" + kind.name(), kind.name(), thrown(kind.name(), metal), properties -> arm(properties, material, kind));
			}
			for (Charge charge : CHARGES) {
				if (charge.metal().equals(metal)) {
					item(metal + "_" + charge.name(), charge.name(), null, properties -> charge(properties, material, charge));
				}
			}
			for (Ranged ranged : RANGED) {
				if (ranged.metal().equals(metal)) {
					kit(metal + "_" + ranged.name(), properties -> {
						Item.Properties base = properties.durability(material.durability() * 3 / 2).repairable(material.repairItems())
								.enchantable(material.enchantmentValue());
						return ranged.name().equals("longbow") ? new ArmBowItem(ranged, base) : new ArmCrossbowItem(ranged, base);
					});
				}
			}
			for (Shield shield : SHIELDS) {
				if (shield.metal().equals(metal)) {
					kit(metal + "_" + shield.name(), properties -> new ArmShieldItem(shield.name(), shield(properties, material, shield)));
				}
			}
		}
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			ITEMS.values().forEach(item -> output.accept(item));
			KIT.values().forEach(item -> output.accept(item));
		});
		ArmVariants.register();
		TwoHanded.register();
		WeaponArts.register();
	}

	/** A swung or thrust arm: the metal's durability, repair and enchantability, and the kind's traits. */
	static Item.Properties arm(Item.Properties properties, ToolMaterial material, Kind kind) {
		// Swords' kinds start as a sword (cobwebs, no breaking blocks in creative), a chopping kind as an axe (wood), a
		// delving kind as a pickaxe (stone and ore); the others mine nothing, like the mace.
		Item.Properties base = kind.sword() ? properties.sword(material, kind.damage(), kind.speed())
				: TRAITS.get(kind.name()) == Trait.CHOP ? properties.axe(material, kind.damage(), kind.speed())
				: TRAITS.get(kind.name()) == Trait.DELVE ? properties.pickaxe(material, kind.damage(), kind.speed())
				: properties.durability(material.durability()).repairable(material.repairItems())
						.enchantable(material.enchantmentValue()).component(DataComponents.TOOL, new Tool(List.of(), 1.0F, 2, false));
		base.attributes(attributes(material.attackDamageBonus() + kind.damage(), kind.speed(), kind.knockback()))
				.component(DataComponents.WEAPON, new Weapon(kind.wear(), kind.disable()))
				.component(DataComponents.ATTACK_ANIMATION, new SwingAnimation(kind.swing(), kind.swingTicks()))
				.component(DataComponents.ATTACK_RANGE, new AttackRange(kind.minReach(), kind.maxReach(), kind.minReach(),
						kind.maxReach() + CREATIVE_REACH, kind.margin(), 1.0F));
		if (kind.parry() > 0.0F) {
			// Blocking as a shield does, against what a shield blocks, but only part of the blow and over a narrower arc.
			// The damage types are a dynamic registry, so this is filled in once they load, as the shield's is.
			base.delayedComponent(DataComponents.BLOCKS_ATTACKS, registries -> new BlocksAttacks(PARRY_DELAY, 1.0F,
					List.of(new BlocksAttacks.DamageReduction(PARRY_ANGLE, Optional.empty(), 0.0F, kind.parry())),
					new BlocksAttacks.ItemDamageFunction(PARRY_WEAR_THRESHOLD, PARRY_WEAR_BASE, PARRY_WEAR_FACTOR),
					Optional.of(registries.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)), Optional.of(SoundEvents.SHIELD_BLOCK),
					Optional.of(SoundEvents.SHIELD_BREAK)));
		}
		if (kind.pierce()) {
			base.component(DataComponents.PIERCING_WEAPON, new PiercingWeapon(true, false, Optional.of(SoundEvents.SPEAR_ATTACK),
					Optional.of(SoundEvents.SPEAR_HIT)));
		}
		return base;
	}

	/** A charging arm: vanilla's spear with the charge's numbers; the lance jabs harder and reaches farther. */
	static Item.Properties charge(Item.Properties properties, ToolMaterial material, Charge charge) {
		Item.Properties spear = properties.spear(material, charge.jab(), charge.multiplier(), charge.delay(), charge.dismountTime(),
				charge.dismountSpeed(), charge.knockbackTime(), charge.knockbackSpeed(), charge.damageTime(), charge.damageSpeed());
		if (charge.name().equals("lance")) {
			spear.attributes(attributes(material.attackDamageBonus() + LANCE_DAMAGE, 1.0F / charge.jab() - 4.0F, 0.0F))
					.component(DataComponents.ATTACK_RANGE, new AttackRange(LANCE_MIN_REACH, LANCE_MAX_REACH, LANCE_MIN_REACH,
							LANCE_MAX_REACH + CREATIVE_REACH, 0.125F, 0.5F));
		}
		return spear;
	}

	/**
	 * A shield: blocks as vanilla's does (the blocks-attacks component, raised with the use key) with its own numbers; a
	 * heavy one braces its bearer against knockback and slows them while held in either hand.
	 */
	static Item.Properties shield(Item.Properties properties, ToolMaterial material, Shield shield) {
		Item.Properties base = properties.durability(shield.durability()).repairable(material.repairItems())
				.enchantable(material.enchantmentValue());
		base.delayedComponent(DataComponents.BLOCKS_ATTACKS, registries -> new BlocksAttacks(shield.delay(), shield.disable(),
				List.of(new BlocksAttacks.DamageReduction(shield.angle(), Optional.empty(), 0.0F, 1.0F)),
				new BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, shield.wear()),
				Optional.of(registries.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)), Optional.of(SoundEvents.SHIELD_BLOCK),
				Optional.of(SoundEvents.SHIELD_BREAK)));
		if (shield.brace() > 0.0F || shield.weight() > 0.0F) {
			base.attributes(ItemAttributeModifiers.builder()
					.add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(Jugcraft.id("shield_brace"), shield.brace(),
							AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HAND)
					.add(Attributes.MOVEMENT_SPEED, new AttributeModifier(Jugcraft.id("shield_weight"), -shield.weight(),
							AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.HAND)
					.build());
		}
		return base;
	}

	private static ItemAttributeModifiers attributes(float damage, float speed, float knockback) {
		ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damage,
						AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, speed,
						AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
		if (knockback > 0.0F) {
			builder.add(Attributes.ATTACK_KNOCKBACK, new AttributeModifier(Jugcraft.id("arms_knockback"), knockback,
					AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
		}
		return builder.build();
	}

	/** A piece of the kit (a bow, crossbow or shield), made by its own constructor. */
	private static void kit(String name, Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(name));
		KIT.put(name, Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key))));
	}

	/** An arm: an {@link ArmItem}, or a {@link ThrownArmItem} if it is thrown. */
	private static void item(String name, String kind, Thrown thrown, Function<Item.Properties, Item.Properties> traits) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(name));
		Item.Properties properties = traits.apply(new Item.Properties().setId(key));
		ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, key,
				thrown != null ? new ThrownArmItem(kind, thrown, properties) : new ArmItem(kind, properties)));
	}
}
