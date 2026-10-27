package io.github.jimbozoomer.jugcraft.concordance.rime;

import eu.pb4.trinkets.api.TrinketsApi;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.effect.Stacking;
import io.github.jimbozoomer.jugcraft.concordance.ember.EmberArmorItem;
import io.github.jimbozoomer.jugcraft.concordance.ember.EmberGear;
import io.github.jimbozoomer.jugcraft.concordance.strata.Brace;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import org.jspecify.annotations.Nullable;

/**
 * Rime, part 2: the Rimekeeper's travel kit (docs/features/arcane-concordance-winter.md, tools/concordance_rime_kit.py;
 * Concordance Chunk B, B-10), made from the owner's water art:
 * <ul>
 * <li>the Lesser Focus of Water and the Focus of Water, worn in the Spell Focus slot: Preserve holds a crucible
 * {@value #LESSER_FOCUS_HOLD_TICKS} or {@value #FOCUS_HOLD_TICKS} ticks instead of {@link Rime#HOLD_TICKS}
 * ({@link #holdTicks});</li>
 * <li>the Water Bangle, worn in a Bracelet slot: a Rimekeeper who has gone {@value #BANGLE_RECHARGE_TICKS} ticks without a
 * Brace is braced ({@link #bangle}) through the shared effect boundary, as nobody's cast (no Strata practice, no deed);</li>
 * <li>three water sets, Nereid's (light), Aquamancer's (medium) and Winterguard's (heavy), worn as the owner's GeckoLib
 * models through Ember's renderer ({@link EmberArmorItem}): no Spell Power, toughness or knockback resistance, protection
 * no more than the Pyromaniac's, leather's and iron's. Every piece is in #minecraft:freeze_immune_wearables (data), so
 * vanilla's freezing passes its wearer by; the Yeti King's frost (YetiKingEntity.chill) does not ask, and still slows.</li>
 * </ul>
 * Only the bangle looks at anything over time: each player once every {@value #BANGLE_CHECK_TICKS} ticks.
 */
public final class RimeGear {
	/** Preserve's hold with a focus of water worn (tools/concordance_rime.py; B-P24, B-P50), within Rime.MAX_HOLD_TICKS. */
	public static final int LESSER_FOCUS_HOLD_TICKS = 1800;
	public static final int FOCUS_HOLD_TICKS = 2400;
	/** The bangle: a Brace after this long with none (B-P25, B-P49). */
	public static final int BANGLE_RECHARGE_TICKS = 600;
	/** ...lasting Brace's own time (tools/concordance_strata.py BRACE_TICKS). */
	public static final int BANGLE_BRACE_TICKS = 100;
	public static final int BANGLE_CHECK_TICKS = 20;
	/** Vanilla's per-slot base durability times these. */
	public static final int NEREIDS_DURABILITY = 7;
	public static final int AQUAMANCERS_DURABILITY = 10;
	public static final int WINTERGUARDS_DURABILITY = 15;
	/** Leather's for the cloth sets, iron's for the heavy one. */
	public static final int NEREIDS_ENCHANTABILITY = 15;
	public static final int AQUAMANCERS_ENCHANTABILITY = 15;
	public static final int WINTERGUARDS_ENCHANTABILITY = 9;
	/** The owner's medium model, Ember's import (the owner's light sheets are drawn for it), and the heavy set's own. */
	public static final String MEDIUM_MODEL = EmberGear.ARMOR_MODEL;
	public static final String HEAVY_MODEL = "winterguards";
	public static final ArmorMaterial NEREIDS_ARMOR = material("nereids", NEREIDS_DURABILITY, NEREIDS_ENCHANTABILITY,
			SoundEvents.ARMOR_EQUIP_LEATHER, 1, 2, 1, 1);
	public static final ArmorMaterial AQUAMANCERS_ARMOR = material("aquamancers", AQUAMANCERS_DURABILITY, AQUAMANCERS_ENCHANTABILITY,
			SoundEvents.ARMOR_EQUIP_LEATHER, 1, 3, 2, 1);
	public static final ArmorMaterial WINTERGUARDS_ARMOR = material("winterguards", WINTERGUARDS_DURABILITY, WINTERGUARDS_ENCHANTABILITY,
			SoundEvents.ARMOR_EQUIP_IRON, 2, 6, 5, 2);
	/** The bangle's Brace: Brace's own Braced, helpful, exclusive (a second changes nothing while one holds). */
	static final EffectSpec BANGLE_BRACE = new EffectSpec(EffectKind.STATUS, Intent.HELPFUL, 0, BANGLE_BRACE_TICKS, Brace.STATUS,
			Stacking.EXCLUSIVE, null);

	public static Item LESSER_WATER_FOCUS;
	public static Item WATER_FOCUS;
	public static Item WATER_BANGLE;
	/** The three sets' pieces by id, light to heavy, head to feet. */
	public static final Map<String, Item> ARMOR = new LinkedHashMap<>();
	/** When (server ticks) a bangle wearer was last seen braced, or first seen wearing it. Not saved. */
	public static AttachmentType<Long> BANGLE_LAST;

	private RimeGear() {
	}

	/** Registered with Rime, whatever the Concordance switch: saved items keep their ids (their recipes are switch-gated). */
	static void register() {
		LESSER_WATER_FOCUS = item("lesser_water_focus", RimeTrinketItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
		WATER_FOCUS = item("water_focus", RimeTrinketItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
		WATER_BANGLE = item("water_bangle", RimeTrinketItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
		armor("nereids_hood", "nereids", NEREIDS_ARMOR, ArmorType.HELMET, MEDIUM_MODEL);
		armor("nereids_tunic", "nereids", NEREIDS_ARMOR, ArmorType.CHESTPLATE, MEDIUM_MODEL);
		armor("nereids_pants", "nereids", NEREIDS_ARMOR, ArmorType.LEGGINGS, MEDIUM_MODEL);
		armor("nereids_shoes", "nereids", NEREIDS_ARMOR, ArmorType.BOOTS, MEDIUM_MODEL);
		armor("aquamancers_hat", "aquamancers", AQUAMANCERS_ARMOR, ArmorType.HELMET, MEDIUM_MODEL);
		armor("aquamancers_robes", "aquamancers", AQUAMANCERS_ARMOR, ArmorType.CHESTPLATE, MEDIUM_MODEL);
		armor("aquamancers_leggings", "aquamancers", AQUAMANCERS_ARMOR, ArmorType.LEGGINGS, MEDIUM_MODEL);
		armor("aquamancers_boots", "aquamancers", AQUAMANCERS_ARMOR, ArmorType.BOOTS, MEDIUM_MODEL);
		armor("winterguards_helmet", "winterguards", WINTERGUARDS_ARMOR, ArmorType.HELMET, HEAVY_MODEL);
		armor("winterguards_chestplate", "winterguards", WINTERGUARDS_ARMOR, ArmorType.CHESTPLATE, HEAVY_MODEL);
		armor("winterguards_leggings", "winterguards", WINTERGUARDS_ARMOR, ArmorType.LEGGINGS, HEAVY_MODEL);
		armor("winterguards_boots", "winterguards", WINTERGUARDS_ARMOR, ArmorType.BOOTS, HEAVY_MODEL);
		BANGLE_LAST = AttachmentRegistry.<Long>builder().buildAndRegister(Jugcraft.id("water_bangle_braced"));
		ServerTickEvents.END_SERVER_TICK.register(RimeGear::tick);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			output.accept(LESSER_WATER_FOCUS);
			output.accept(WATER_FOCUS);
			output.accept(WATER_BANGLE);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> ARMOR.values().forEach(output::accept));
	}

	/** A set's material: durability, enchantability, equip sound and protection (helmet, chestplate, leggings, boots). */
	private static ArmorMaterial material(String set, int durability, int enchantability, Holder<SoundEvent> sound, int helmet,
			int chestplate, int leggings, int boots) {
		return new ArmorMaterial(durability, Map.of(ArmorType.BOOTS, boots, ArmorType.LEGGINGS, leggings, ArmorType.CHESTPLATE,
				chestplate, ArmorType.HELMET, helmet, ArmorType.BODY, chestplate), enchantability, sound, 0.0F, 0.0F,
				TagKey.create(Registries.ITEM, Jugcraft.id("repairs_" + set + "_gear")),
				ResourceKey.create(EquipmentAssets.ROOT_ID, Jugcraft.id(set)));
	}

	private static void armor(String id, String set, ArmorMaterial material, ArmorType type, String model) {
		ARMOR.put(id, item(id, piece -> new EmberArmorItem(piece, set, model), new Item.Properties().humanoidArmor(material, type)));
	}

	private static Item item(String id, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	/** How long Preserve holds a crucible for {@code person}: longer with a focus of water worn where it works (Rime.preserve). */
	public static int holdTicks(@Nullable ServerPlayer person) {
		if (person == null) {
			return Rime.HOLD_TICKS;
		}
		AtomicInteger ticks = new AtomicInteger(Rime.HOLD_TICKS);
		TrinketsApi.getAttachment(person).forEach((access, stack) -> {
			if (!access.cosmetic() && access.canApplyEffects()) {
				if (stack.is(WATER_FOCUS)) {
					ticks.accumulateAndGet(FOCUS_HOLD_TICKS, Math::max);
				} else if (stack.is(LESSER_WATER_FOCUS)) {
					ticks.accumulateAndGet(LESSER_FOCUS_HOLD_TICKS, Math::max);
				}
			}
		});
		return Math.min(ticks.get(), Rime.MAX_HOLD_TICKS);
	}

	/** Whether {@code player} wears a Water Bangle in a Trinkets slot that applies its effects (not worn for show). */
	public static boolean wearsBangle(ServerPlayer player) {
		AtomicBoolean worn = new AtomicBoolean(false);
		TrinketsApi.getAttachment(player).forEach((access, stack) -> {
			if (!access.cosmetic() && access.canApplyEffects() && stack.is(WATER_BANGLE)) {
				worn.set(true);
			}
		});
		return worn.get();
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % BANGLE_CHECK_TICKS != 0 || !JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			bangle(player, server.getTickCount());
		}
	}

	/**
	 * One look at {@code player} at {@code now} (server ticks). Only a living Rimekeeper wearing the bangle where it works
	 * is looked at; anyone else's count is forgotten, so a bangle taken off and put back on starts again. Braced by any
	 * Brace, they have had one now. The first look only starts the count (putting it on, logging in or coming back from
	 * death gives no Brace at once). After {@value #BANGLE_RECHARGE_TICKS} ticks with none, they are braced. Returns the
	 * boundary's answer when it braced, or null when it did not try.
	 */
	public static ConcordanceEffects.@Nullable Result bangle(ServerPlayer player, long now) {
		if (!player.isAlive() || player.isSpectator()) {
			return null;
		}
		if (!wearsBangle(player) || !Rime.understands(player)) {
			if (player.hasAttached(BANGLE_LAST)) {
				player.removeAttached(BANGLE_LAST);
			}
			return null;
		}
		Long last = player.getAttached(BANGLE_LAST);
		if ((Brace.BRACED != null && player.hasEffect(Brace.BRACED)) || last == null || now < last) {
			player.setAttached(BANGLE_LAST, now);
			return null;
		}
		if (now - last < BANGLE_RECHARGE_TICKS) {
			return null;
		}
		ConcordanceEffects.Result braced = brace(player);
		if (braced.applied()) {
			player.setAttached(BANGLE_LAST, now);
		}
		return braced;
	}

	/** The bangle's Brace on its wearer, through the shared effect boundary, as nobody's cast (no actor: Alchemy.apply's way). */
	public static ConcordanceEffects.Result brace(ServerPlayer player) {
		ServerLevel level = player.level();
		Cause cause = Cause.of(null, Cause.Origin.ITEM, "jugcraft:water_bangle", ConcordanceEffects.nextSerial());
		ConcordanceEffects.Context context = new ConcordanceEffects.Context(level, cause, null,
				new Ledger(new Ledger.Limits(1, EffectKind.STATUS.work, 0)), "water_bangle", player.position());
		return ConcordanceEffects.apply(context, BANGLE_BRACE, player);
	}
}
