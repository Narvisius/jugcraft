package io.github.jimbozoomer.jugcraft.lair.tyrant;

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
 * The Cinder Tyrant's registrations (docs/features/cinder-tyrant.md): he, his Cinderlings, his gobs of magma and his
 * falling cinders; his loot (the Tyrant Scale, the Salamander Charm and the Tyrant's Crest; the Cinderbrand and the Magmaw
 * are Arms VII trophies, weapons/ArmVariants.java, with their Ember boon). He is sunk in the crucible of every Cinder Kiln
 * as it is placed.
 */
public final class JugcraftTyrant {
	public static EntityType<CinderTyrantEntity> CINDER_TYRANT;
	public static EntityType<CinderlingEntity> CINDERLING;
	public static EntityType<MagmaGobEntity> MAGMA_GOB;
	public static EntityType<FallingCinderEntity> FALLING_CINDER;
	public static Item TYRANT_SCALE;
	public static Item SALAMANDER_CHARM;
	public static Item TYRANT_CREST;

	private JugcraftTyrant() {
	}

	public static void register() {
		CINDER_TYRANT = entity("cinder_tyrant", EntityType.Builder.<CinderTyrantEntity>of(CinderTyrantEntity::new, MobCategory.MONSTER)
				.sized(2.6F, 2.2F).eyeHeight(1.4F).fireImmune().noLootTable().clientTrackingRange(12));
		FabricDefaultAttributeRegistry.register(CINDER_TYRANT, CinderTyrantEntity.createAttributes());
		CINDERLING = entity("cinderling", EntityType.Builder.<CinderlingEntity>of(CinderlingEntity::new, MobCategory.MONSTER)
				.sized(0.8F, 0.5F).eyeHeight(0.35F).fireImmune().noLootTable().noSave().clientTrackingRange(8));
		FabricDefaultAttributeRegistry.register(CINDERLING, CinderlingEntity.createAttributes());
		MAGMA_GOB = entity("magma_gob", EntityType.Builder.<MagmaGobEntity>of(MagmaGobEntity::new, MobCategory.MISC)
				.sized(0.6F, 0.6F).fireImmune().noLootTable().noSave().clientTrackingRange(8).updateInterval(1));
		FALLING_CINDER = entity("falling_cinder", EntityType.Builder.<FallingCinderEntity>of(FallingCinderEntity::new, MobCategory.MISC)
				.sized(0.5F, 0.8F).fireImmune().noLootTable().noSave().clientTrackingRange(8).updateInterval(1));

		// Each says what it is for in a grey line (tooltip.jugcraft.<id>).
		TYRANT_SCALE = item("tyrant_scale", new Item.Properties().rarity(Rarity.UNCOMMON));
		SALAMANDER_CHARM = item("salamander_charm", new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
		// Worn on the head, it is drawn there as its own model (its "head" display), as the costume hats are.
		TYRANT_CREST = item("tyrant_crest", new Item.Properties().stacksTo(1).rarity(Rarity.RARE).component(
				DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).setEquipSound(SoundEvents.ARMOR_EQUIP_GENERIC).build()));

		Lairs.onPlaced(Lair.CINDER_KILN, CinderTyrantEntity::summon);
		SalamanderCharm.register();
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> output.accept(TYRANT_SCALE));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			output.accept(SALAMANDER_CHARM);
			output.accept(TYRANT_CREST);
		});
	}

	/** The Cinderbrand and the Magmaw, registered with the other Arms VII trophies (weapons/ArmVariants.java). */
	public static Item cinderbrand() {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id("cinderbrand"));
	}

	public static Item magmaw() {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id("magmaw"));
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
