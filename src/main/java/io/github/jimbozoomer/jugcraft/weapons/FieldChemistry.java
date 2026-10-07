package io.github.jimbozoomer.jugcraft.weapons;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.gear.JugcraftGear;
import io.github.jimbozoomer.jugcraft.gear.ScubaTankItem;
import io.github.jimbozoomer.jugcraft.materials.JugcraftRegistry;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.RemoveStatusEffectsConsumeEffect;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.TagKey;
import org.jspecify.annotations.Nullable;

/**
 * Field chemistry (batch 31, docs/features/field-chemistry.md): chemical grenades for the hand and the grenade
 * launcher, the gas mask that counters them, and medicines from the chemical reactor. Nothing here breaks, moves or
 * burns a block. Keep the numbers in sync with tools/field_chemistry.py; tools/check_mod_data.py checks them.
 */
public final class FieldChemistry {
	/** Chlorine: a cloud this wide (blocks) for this long (ticks), hurting what breathes this much every second. */
	public static final double CHLORINE_RADIUS = 3.0;
	public static final int CHLORINE_TICKS = 200;
	public static final float CHLORINE_DAMAGE = 2.0F;
	/** Smoke: a wider, longer cloud that hides whoever is inside from mobs and blinds players without a mask. */
	public static final double SMOKE_RADIUS = 4.0;
	public static final int SMOKE_TICKS = 300;
	/** Thermite: a burning pool on the floor that hurts and sets alight whatever stands in it, every second. */
	public static final double THERMITE_RADIUS = 2.0;
	public static final int THERMITE_TICKS = 120;
	public static final float THERMITE_DAMAGE = 4.0F;
	public static final int THERMITE_FIRE_SECONDS = 5;
	/** Flashbang: reach (blocks), how long it blinds a player looking at it, and how long it staggers a mob. */
	public static final double FLASH_RADIUS = 10.0;
	public static final int FLASH_BLIND_TICKS = 80;
	public static final int FLASH_STUN_TICKS = 60;
	/** A gas mask's filter: one point of durability a second in gas or smoke. A helmet has 11 per multiplier. */
	public static final int GAS_MASK_DURABILITY = 20;
	/** Oxygen a sealed scuba set uses each second in gas or smoke instead (mB). */
	public static final int SCUBA_GAS_OXYGEN = 20;
	/** Medicines: the first aid kit's cooldown (seconds) and how long the stimulant lasts (ticks). */
	public static final int FIRST_AID_COOLDOWN = 10;
	public static final int STIMULANT_TICKS = 1200;

	public static final ResourceKey<DamageType> CHLORINE_DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE,
			Jugcraft.id("chlorine"));
	public static final ResourceKey<DamageType> THERMITE_DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE,
			Jugcraft.id("thermite"));
	public static final TagKey<Item> REPAIRS_GAS_MASK = TagKey.create(Registries.ITEM, Jugcraft.id("repairs_gas_mask"));
	public static final ResourceKey<EquipmentAsset> GAS_MASK_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID,
			Jugcraft.id("gas_mask"));
	/** A filter mask of rubber and glass: leather's protection, repaired with charcoal (fresh activated carbon). */
	public static final ArmorMaterial GAS_MASK_ARMOR = new ArmorMaterial(GAS_MASK_DURABILITY,
			Map.of(ArmorType.BOOTS, 1, ArmorType.LEGGINGS, 1, ArmorType.CHESTPLATE, 1, ArmorType.HELMET, 1, ArmorType.BODY, 1),
			10, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, REPAIRS_GAS_MASK, GAS_MASK_ASSET);

	public static Item CHLORINE_GRENADE;
	public static Item SMOKE_GRENADE;
	public static Item THERMITE;
	public static Item THERMITE_GRENADE;
	public static Item FLASHBANG;
	public static Item GAS_MASK;
	public static Item FIRST_AID_KIT;
	public static Item ANTIDOTE;
	public static Item STIMULANT;
	public static EntityType<ChemicalCloud> CHEMICAL_CLOUD;

	/** Every item, by id, in registration order. */
	public static final Map<String, Item> ITEMS = new LinkedHashMap<>();

	private FieldChemistry() {
	}

	public static void register() {
		CHLORINE_GRENADE = item("chlorine_grenade", properties -> new GrenadeItem(properties, Warhead.CHLORINE));
		SMOKE_GRENADE = item("smoke_grenade", properties -> new GrenadeItem(properties, Warhead.SMOKE));
		THERMITE = item("thermite", DescribedItem::new);
		THERMITE_GRENADE = item("thermite_grenade", properties -> new GrenadeItem(properties, Warhead.THERMITE));
		FLASHBANG = item("flashbang", properties -> new GrenadeItem(properties, Warhead.FLASHBANG));
		GAS_MASK = item("gas_mask", properties -> new DescribedItem(properties.humanoidArmor(GAS_MASK_ARMOR, ArmorType.HELMET)));
		// A dressing and antiseptic: four hearts at once, then a wait before the next.
		FIRST_AID_KIT = item("first_aid_kit", properties -> new DescribedItem(properties.stacksTo(16).useCooldown(FIRST_AID_COOLDOWN)
				.component(DataComponents.CONSUMABLE, Consumable.builder().consumeSeconds(1.5F).animation(ItemUseAnimation.BRUSH)
						.sound(SoundEvents.ARMOR_EQUIP_LEATHER).hasConsumeParticles(false)
						.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.INSTANT_HEALTH, 1, 1)))
						.build())));
		// Activated charcoal: clears every harmful effect and keeps the good ones (milk clears both).
		ANTIDOTE = item("antidote", properties -> new DescribedItem(properties.stacksTo(16).usingConvertsTo(Items.GLASS_BOTTLE)
				.component(DataComponents.CONSUMABLE, Consumables.defaultDrink()
						.onConsume(new RemoveStatusEffectsConsumeEffect(HolderSet.direct(List.of(MobEffects.POISON,
								MobEffects.WITHER, MobEffects.NAUSEA, MobEffects.BLINDNESS, MobEffects.HUNGER, MobEffects.WEAKNESS,
								MobEffects.SLOWNESS, MobEffects.MINING_FATIGUE, MobEffects.DARKNESS, MobEffects.LEVITATION,
								MobEffects.INFESTED, MobEffects.OOZING, MobEffects.WEAVING))))
						.build())));
		// Caffeine: quick feet and quick hands for a minute, and hungry for it.
		STIMULANT = item("stimulant", properties -> new DescribedItem(properties.stacksTo(16).usingConvertsTo(Items.GLASS_BOTTLE)
				.component(DataComponents.CONSUMABLE, Consumables.defaultDrink()
						.onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
								new MobEffectInstance(MobEffects.SPEED, STIMULANT_TICKS, 1),
								new MobEffectInstance(MobEffects.HASTE, STIMULANT_TICKS, 1),
								new MobEffectInstance(MobEffects.HUNGER, STIMULANT_TICKS, 0))))
						.build())));

		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id("chemical_cloud"));
		CHEMICAL_CLOUD = Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
				EntityType.Builder.<ChemicalCloud>of(ChemicalCloud::new, MobCategory.MISC).noSummon().noLootTable()
						.sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(20).build(key));

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			for (Entry<String, Item> entry : ITEMS.entrySet()) {
				if (entry.getValue() instanceof GrenadeItem || entry.getKey().equals("gas_mask")) {
					output.accept(entry.getValue());
				}
			}
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(output -> {
			output.accept(FIRST_AID_KIT);
			output.accept(ANTIDOTE);
			output.accept(STIMULANT);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> output.accept(THERMITE));
	}

	private static Item item(String name, java.util.function.Function<Item.Properties, Item> factory) {
		Item item = JugcraftRegistry.item(name, factory);
		ITEMS.put(name, item);
		return item;
	}

	/** Damage of {@code type} from {@code direct} (a cloud), credited to {@code owner}. */
	static DamageSource damage(ServerLevel level, ResourceKey<DamageType> type, @Nullable Entity direct, @Nullable Entity owner) {
		return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type), direct, owner);
	}

	/**
	 * Whether a grenade from {@code owner} may touch {@code target}: always its owner and non-players, and another player
	 * only where the owner may hurt them (the server's PvP setting) and they are not in the owner's party.
	 */
	static boolean mayAffect(@Nullable Entity owner, LivingEntity target) {
		if (!(target instanceof Player victim) || victim == owner) {
			return true;
		}
		if (victim.isCreative() || victim.isSpectator()) {
			return false;
		}
		return !(owner instanceof Player thrower)
				|| thrower.canHarmPlayer(victim) && !JugcraftParties.sameParty(thrower.getUUID(), victim.getUUID());
	}

	/**
	 * Whether {@code wearer} breathes filtered air: a gas mask, or the sealed scuba mask and tank with oxygen in it.
	 * {@code use} wears the filter (one point) or uses a second's oxygen.
	 */
	public static boolean breathesFiltered(LivingEntity wearer, boolean use) {
		ItemStack head = wearer.getItemBySlot(EquipmentSlot.HEAD);
		if (head.is(GAS_MASK)) {
			if (use && wearer.level() instanceof ServerLevel) {
				head.hurtAndBreak(1, wearer, EquipmentSlot.HEAD);
			}
			return true;
		}
		ItemStack chest = wearer.getItemBySlot(EquipmentSlot.CHEST);
		if (head.is(JugcraftGear.SCUBA_MASK) && chest.is(JugcraftGear.SCUBA_TANK) && ScubaTankItem.oxygen(chest) >= SCUBA_GAS_OXYGEN) {
			if (use) {
				ScubaTankItem.setOxygen(chest, ScubaTankItem.oxygen(chest) - SCUBA_GAS_OXYGEN);
			}
			return true;
		}
		return false;
	}
}
