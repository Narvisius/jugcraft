package io.github.jimbozoomer.jugcraft.lair.sentinel;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.weapons.DescribedItem;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.Equippable;

/**
 * The Cydonian Sentinel's registrations (docs/features/cydonian-sentinel.md): it, its Shard Sentries, its Rust Shards,
 * its Quake Spikes and its meteors; its loot (the Cydonian Shard, the Lowfall Core and the Sentinel's Visor; the
 * Corelance and the Redstar Cleaver are Arms VII trophies, weapons/ArmVariants.java, with their Rust boon). It is set on
 * the plinth of every Cydonian Sanctum as it is placed.
 */
public final class JugcraftSentinel {
	public static EntityType<CydonianSentinelEntity> CYDONIAN_SENTINEL;
	public static EntityType<ShardSentryEntity> SHARD_SENTRY;
	public static EntityType<RustShardEntity> RUST_SHARD;
	public static EntityType<QuakeSpikeEntity> QUAKE_SPIKE;
	public static EntityType<SentinelMeteorEntity> SENTINEL_METEOR;
	public static Item CYDONIAN_SHARD;
	public static Item LOWFALL_CORE;
	public static Item SENTINELS_VISOR;

	private JugcraftSentinel() {
	}

	public static void register() {
		CYDONIAN_SENTINEL = entity("cydonian_sentinel", EntityType.Builder.<CydonianSentinelEntity>of(CydonianSentinelEntity::new, MobCategory.MONSTER)
				.sized(2.0F, 4.0F).eyeHeight(3.4F).fireImmune().noLootTable().clientTrackingRange(12));
		FabricDefaultAttributeRegistry.register(CYDONIAN_SENTINEL, CydonianSentinelEntity.createAttributes());
		SHARD_SENTRY = entity("shard_sentry", EntityType.Builder.<ShardSentryEntity>of(ShardSentryEntity::new, MobCategory.MONSTER)
				.sized(0.8F, 0.8F).eyeHeight(0.5F).fireImmune().noLootTable().noSave().clientTrackingRange(10));
		FabricDefaultAttributeRegistry.register(SHARD_SENTRY, ShardSentryEntity.createAttributes());
		RUST_SHARD = entity("rust_shard", EntityType.Builder.<RustShardEntity>of(RustShardEntity::new, MobCategory.MISC)
				.sized(0.4F, 0.4F).noLootTable().noSave().clientTrackingRange(8).updateInterval(1));
		QUAKE_SPIKE = entity("quake_spike", EntityType.Builder.<QuakeSpikeEntity>of(QuakeSpikeEntity::new, MobCategory.MISC)
				.sized(1.0F, 1.2F).noLootTable().noSave().clientTrackingRange(8));
		SENTINEL_METEOR = entity("sentinel_meteor", EntityType.Builder.<SentinelMeteorEntity>of(SentinelMeteorEntity::new, MobCategory.MISC)
				.sized(0.8F, 0.8F).noLootTable().noSave().clientTrackingRange(10).updateInterval(1));

		// Each says what it is for in a grey line (tooltip.jugcraft.<id>).
		CYDONIAN_SHARD = item("cydonian_shard", new Item.Properties().rarity(Rarity.UNCOMMON));
		LOWFALL_CORE = item("lowfall_core", new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
		// Worn on the head, it is drawn there as its own model (its "head" display), as the costume hats are.
		SENTINELS_VISOR = item("sentinels_visor", new Item.Properties().stacksTo(1).rarity(Rarity.RARE).component(
				DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).setEquipSound(SoundEvents.ARMOR_EQUIP_IRON).build()));

		Lairs.onPlaced(Lair.CYDONIAN_SANCTUM, CydonianSentinelEntity::summon);
		LowfallCore.register();
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> output.accept(CYDONIAN_SHARD));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			output.accept(LOWFALL_CORE);
			output.accept(SENTINELS_VISOR);
		});
	}

	/** The Corelance and the Redstar Cleaver, registered with the other Arms VII trophies (weapons/ArmVariants.java). */
	public static Item corelance() {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id("corelance"));
	}

	public static Item redstarCleaver() {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id("redstar_cleaver"));
	}

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> entity(String id, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id(id));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	private static Item item(String id, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
		return Registry.register(BuiltInRegistries.ITEM, key, new DescribedItem(properties.setId(key)));
	}
}
