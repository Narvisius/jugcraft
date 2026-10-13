package io.github.jimbozoomer.jugcraft.concordance.ember;

import eu.pb4.trinkets.api.TrinketsApi;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.effect.Stacking;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.item.v1.EnchantmentEvents;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.spell_power.api.SpellPowerTags;

/**
 * Ember, part 2: the Hearthbinder's regalia (docs/features/arcane-concordance-ember-regalia.md, tools/concordance_ember.py),
 * made from the owner's own fire art:
 * <ul>
 * <li>the Lesser Focus of Fire and the Focus of Fire, worn in the Spell Focus slot: fire Spell Power
 * ({@link EmberTrinketItem}), which Cinderbolt and Hearthflare scale with;</li>
 * <li>the Fire Bangle, worn in a Bracelet slot: a Hearthbinder's melee blows leave the creature smouldering
 * ({@link #afterDamage}), through the shared effect boundary, so PvP, parties, claims and tolerance all apply;</li>
 * <li>two fire sets, the Pyromaniac's (light: hood, tunic, pants, shoes; cloth) and the Pyromancer's (medium: hat, robes,
 * leggings, boots; leather's protection), half a point of fire Spell Power a piece, worn as the owner's GeckoLib model
 * ({@link EmberArmorItem}) with each set's own texture. Spell Power's own attribute enchantments (Sunfire and its kind)
 * are refused on them, so the regalia's fire stays at most {@link #FOCUS_POWER} plus a set's.</li>
 * </ul>
 * Nothing here ticks: the Spell Power is Trinkets' and vanilla's attribute modifiers, and the blow is a damage event.
 */
public final class EmberGear {
	/** Fire Spell Power, in points above the school's base (tools/concordance_ember.py). */
	public static final double LESSER_FOCUS_POWER = 2.0;
	public static final double FOCUS_POWER = 4.0;
	/** Each piece of a fire set; four pieces give two points, one more damage. */
	public static final double ROBE_PIECE_POWER = 0.5;
	/** The Fire Bangle's blow: Smoulder for this long. */
	public static final int BANGLE_SMOULDER_TICKS = 60;
	/**
	 * Blocks beyond the reach of the weapon in hand (from the eye to the creature's hitbox, as vanilla measures a melee
	 * hit) that a blow may land and still count: a Shock arc's second foe or a weapon skill landing farther off does not.
	 */
	public static final double BANGLE_REACH_MARGIN = 1.0;
	/** Vanilla's per-slot base durability times these: the Pyromaniac's (light, cloth) and the Pyromancer's (medium). */
	public static final int PYROMANIACS_DURABILITY = 7;
	public static final int PYROMANCERS_DURABILITY = 10;
	/** Both sets' enchantability (leather's). */
	public static final int ARMOR_ENCHANTABILITY = 15;
	/**
	 * The GeckoLib model both sets are worn as: the owner's medium model. Their light fire texture is drawn for it (it is the
	 * medium sheet, byte for byte), so the light set wears it there, with its own texture.
	 */
	public static final String ARMOR_MODEL = "pyromancers";
	/** The Pyromaniac's: a little less protection than leather (1/2/1/1), repaired with wool. */
	public static final ArmorMaterial PYROMANIACS_ARMOR = material("pyromaniacs", PYROMANIACS_DURABILITY, 1, 2, 1, 1);
	/** The Pyromancer's: leather's protection (1/3/2/1), a little longer-lasting, repaired with wool. */
	public static final ArmorMaterial PYROMANCERS_ARMOR = material("pyromancers", PYROMANCERS_DURABILITY, 1, 3, 2, 1);
	/** What the bangle's blow gives: Smoulder, harmful, as Cinderbolt's word does. */
	static final EffectSpec SMOULDER_ON_HIT = new EffectSpec(EffectKind.STATUS, Intent.HARMFUL, 0, BANGLE_SMOULDER_TICKS,
			"jugcraft:smoulder", Stacking.STRONGEST, null);

	public static Item LESSER_FIRE_FOCUS;
	public static Item FIRE_FOCUS;
	public static Item FIRE_BANGLE;
	/** Both fire sets' pieces by id, the Pyromaniac's then the Pyromancer's, head to feet. */
	public static final Map<String, Item> ARMOR = new LinkedHashMap<>();

	private EmberGear() {
	}

	public static void register() {
		LESSER_FIRE_FOCUS = item("lesser_fire_focus", properties -> new EmberTrinketItem(properties, LESSER_FOCUS_POWER),
				new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant());
		FIRE_FOCUS = item("fire_focus", properties -> new EmberTrinketItem(properties, FOCUS_POWER),
				new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant());
		FIRE_BANGLE = item("fire_bangle", properties -> new EmberTrinketItem(properties, 0.0),
				new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant());
		Holder<Attribute> fire = BuiltInRegistries.ATTRIBUTE.get(Identifier.parse(Ember.SCHOOL))
				.orElseThrow(() -> new IllegalStateException("Spell Power's fire attribute is not registered: " + Ember.SCHOOL));
		armor("pyromaniacs_hood", "pyromaniacs", PYROMANIACS_ARMOR, ArmorType.HELMET, EquipmentSlotGroup.HEAD, fire);
		armor("pyromaniacs_tunic", "pyromaniacs", PYROMANIACS_ARMOR, ArmorType.CHESTPLATE, EquipmentSlotGroup.CHEST, fire);
		armor("pyromaniacs_pants", "pyromaniacs", PYROMANIACS_ARMOR, ArmorType.LEGGINGS, EquipmentSlotGroup.LEGS, fire);
		armor("pyromaniacs_shoes", "pyromaniacs", PYROMANIACS_ARMOR, ArmorType.BOOTS, EquipmentSlotGroup.FEET, fire);
		armor("pyromancers_hat", "pyromancers", PYROMANCERS_ARMOR, ArmorType.HELMET, EquipmentSlotGroup.HEAD, fire);
		armor("pyromancers_robes", "pyromancers", PYROMANCERS_ARMOR, ArmorType.CHESTPLATE, EquipmentSlotGroup.CHEST, fire);
		armor("pyromancers_leggings", "pyromancers", PYROMANCERS_ARMOR, ArmorType.LEGGINGS, EquipmentSlotGroup.LEGS, fire);
		armor("pyromancers_boots", "pyromancers", PYROMANCERS_ARMOR, ArmorType.BOOTS, EquipmentSlotGroup.FEET, fire);
		ServerLivingEntityEvents.AFTER_DAMAGE.register(EmberGear::afterDamage);
		// Spell Power's attribute enchantments (Sunfire and its kind) would multiply a set's fire, and an arcane ring's,
		// past the regalia's ceiling: refuse them on the sets. Spell Power itself only ever refuses, never forces, so this
		// holds whichever listener runs first.
		EnchantmentEvents.ALLOW_ENCHANTING.register((enchantment, target, context) -> target.getItem() instanceof EmberArmorItem
				&& enchantment.is(SpellPowerTags.Enchantments.REQUIRES_MATCHING_ATTRIBUTE) ? TriState.FALSE : TriState.DEFAULT);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			output.accept(LESSER_FIRE_FOCUS);
			output.accept(FIRE_FOCUS);
			output.accept(FIRE_BANGLE);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> ARMOR.values().forEach(output::accept));
	}

	/** A fire set's material: its durability and protection (helmet, chestplate, leggings, boots), its tag and asset. */
	private static ArmorMaterial material(String set, int durability, int helmet, int chestplate, int leggings, int boots) {
		return new ArmorMaterial(durability, Map.of(ArmorType.BOOTS, boots, ArmorType.LEGGINGS, leggings, ArmorType.CHESTPLATE,
				chestplate, ArmorType.HELMET, helmet, ArmorType.BODY, chestplate), ARMOR_ENCHANTABILITY, SoundEvents.ARMOR_EQUIP_LEATHER,
				0.0F, 0.0F, TagKey.create(Registries.ITEM, Jugcraft.id("repairs_" + set + "_gear")),
				ResourceKey.create(EquipmentAssets.ROOT_ID, Jugcraft.id(set)));
	}

	private static void armor(String id, String set, ArmorMaterial material, ArmorType type, EquipmentSlotGroup group,
			Holder<Attribute> fire) {
		Item.Properties properties = new Item.Properties().humanoidArmor(material, type);
		properties.attributes(material.createAttributes(type).withModifierAdded(fire,
				new AttributeModifier(Jugcraft.id(id + "_fire"), ROBE_PIECE_POWER, AttributeModifier.Operation.ADD_VALUE), group));
		ARMOR.put(id, item(id, piece -> new EmberArmorItem(piece, set, ARMOR_MODEL), properties.fireResistant()));
	}

	private static Item item(String id, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	/**
	 * The Fire Bangle's blow. After a player's own melee hit (vanilla's player-attack damage, dealt by the player in
	 * person, within reach of the weapon in hand: their blows, its sweep and Spell Engine's melee weapon skills; not an
	 * arrow, a spell of a school, a mount's or a machine's blow, nor a Shock arc or a skill landing beyond that reach), if
	 * they wear a bangle where it works and understand Hearthbinding, the creature smoulders, wherever the shared effect
	 * boundary lets them harm it. Cheapest checks first.
	 */
	private static void afterDamage(LivingEntity target, DamageSource source, float baseDamage, float damage, boolean blocked) {
		if (damage <= 0.0F || blocked || !(target.level() instanceof ServerLevel level) || !source.is(DamageTypes.PLAYER_ATTACK)
				|| !(source.getEntity() instanceof ServerPlayer player) || source.getDirectEntity() != player || player.isPassenger()
				|| !player.isWithinAttackRange(player.getMainHandItem(), target.getBoundingBox(), BANGLE_REACH_MARGIN)
				|| !JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE) || !wearsBangle(player)
				|| !ConcordanceProgress.knowledge(player).state(Ember.RESEARCH).atLeast(ResearchState.UNDERSTOOD)) {
			return;
		}
		smoulder(level, player, target);
	}

	/** Whether {@code player} wears a Fire Bangle in a Trinkets slot that applies its effects (not worn for show). */
	public static boolean wearsBangle(ServerPlayer player) {
		AtomicBoolean worn = new AtomicBoolean(false);
		TrinketsApi.getAttachment(player).forEach((access, stack) -> {
			if (!access.cosmetic() && access.canApplyEffects() && stack.is(FIRE_BANGLE)) {
				worn.set(true);
			}
		});
		return worn.get();
	}

	/** The bangle's Smoulder on {@code target}, as {@code player}'s, through the shared effect boundary. */
	public static ConcordanceEffects.Result smoulder(ServerLevel level, ServerPlayer player, LivingEntity target) {
		Cause cause = Cause.of(player.getUUID(), Cause.Origin.ITEM, "jugcraft:fire_bangle", ConcordanceEffects.nextSerial());
		ConcordanceEffects.Context context = new ConcordanceEffects.Context(level, cause, player,
				new Ledger(new Ledger.Limits(1, EffectKind.STATUS.work, 0)), "fire_bangle", player.position());
		return ConcordanceEffects.apply(context, SMOULDER_ON_HIT, target);
	}
}
