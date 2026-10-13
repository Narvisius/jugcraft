package io.github.jimbozoomer.jugcraft.lair.tatterlace;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.weapons.DescribedItem;
import io.github.jimbozoomer.jugcraft.weapons.StitchBoon;
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
 * Madame Tatterlace's registrations (docs/features/tatterlace.md): she, her tossed thimbles, Binding Threads, Lace
 * Snares, rolling spools, egg sacs and spiderlings; her loot (Gossamer Silk, the Golden Thimble and Tatterlace's
 * Headdress; the Needle Rapier is an Arms VII trophy, weapons/ArmVariants.java, with its Stitch boon). She is set on her
 * silk over the doily of every Spindle Loft as it is placed.
 */
public final class JugcraftTatterlace {
	public static EntityType<TatterlaceEntity> TATTERLACE;
	public static EntityType<TossedThimbleEntity> TOSSED_THIMBLE;
	public static EntityType<BindingThreadEntity> BINDING_THREAD;
	public static EntityType<LaceSnareEntity> LACE_SNARE;
	public static EntityType<RollingSpoolEntity> ROLLING_SPOOL;
	public static EntityType<TatterEggSacEntity> TATTER_EGG_SAC;
	public static EntityType<SpiderlingEntity> TATTER_SPIDERLING;
	public static Item GOSSAMER_SILK;
	public static Item GOLDEN_THIMBLE;
	public static Item TATTERLACE_HEADDRESS;

	private JugcraftTatterlace() {
	}

	public static void register() {
		TATTERLACE = entity("tatterlace", EntityType.Builder.<TatterlaceEntity>of(TatterlaceEntity::new, MobCategory.MONSTER)
				.sized(2.4F, 1.5F).eyeHeight(1.2F).fireImmune().noLootTable().clientTrackingRange(12));
		FabricDefaultAttributeRegistry.register(TATTERLACE, TatterlaceEntity.createAttributes());
		TOSSED_THIMBLE = entity("tossed_thimble", EntityType.Builder.<TossedThimbleEntity>of(TossedThimbleEntity::new, MobCategory.MISC)
				.sized(0.5F, 0.5F).fireImmune().noLootTable().noSave().clientTrackingRange(8).updateInterval(1));
		BINDING_THREAD = entity("binding_thread", EntityType.Builder.<BindingThreadEntity>of(BindingThreadEntity::new, MobCategory.MISC)
				.sized(0.5F, 0.5F).fireImmune().noLootTable().noSave().clientTrackingRange(8).updateInterval(1));
		LACE_SNARE = entity("lace_snare", EntityType.Builder.<LaceSnareEntity>of(LaceSnareEntity::new, MobCategory.MISC)
				.sized(1.0F, 0.25F).fireImmune().noLootTable().noSave().clientTrackingRange(8).updateInterval(2));
		ROLLING_SPOOL = entity("rolling_spool", EntityType.Builder.<RollingSpoolEntity>of(RollingSpoolEntity::new, MobCategory.MISC)
				.sized(1.5F, 1.5F).fireImmune().noLootTable().noSave().clientTrackingRange(8).updateInterval(1));
		TATTER_EGG_SAC = entity("tatter_egg_sac", EntityType.Builder.<TatterEggSacEntity>of(TatterEggSacEntity::new, MobCategory.MONSTER)
				.sized(1.0F, 0.75F).noLootTable().noSave().clientTrackingRange(8));
		FabricDefaultAttributeRegistry.register(TATTER_EGG_SAC, TatterEggSacEntity.createAttributes());
		TATTER_SPIDERLING = entity("tatter_spiderling", EntityType.Builder.<SpiderlingEntity>of(SpiderlingEntity::new, MobCategory.MONSTER)
				.sized(0.7F, 0.4F).eyeHeight(0.25F).noLootTable().noSave().clientTrackingRange(8));
		FabricDefaultAttributeRegistry.register(TATTER_SPIDERLING, SpiderlingEntity.createAttributes());

		// Each says what it is for in a grey line (tooltip.jugcraft.<id>).
		GOSSAMER_SILK = item("gossamer_silk", new Item.Properties().rarity(Rarity.UNCOMMON));
		GOLDEN_THIMBLE = item("golden_thimble", new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
		// Worn on the head, it is drawn there as its own model (its "head" display), as the costume hats are.
		TATTERLACE_HEADDRESS = item("tatterlace_headdress", new Item.Properties().stacksTo(1).rarity(Rarity.RARE).component(
				DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER).build()));

		Lairs.onPlaced(Lair.SPINDLE_LOFT, TatterlaceEntity::summon);
		StitchBoon.register();
		GoldenThimble.register();
		LaceSnareEntity.register();
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> output.accept(GOSSAMER_SILK));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			output.accept(GOLDEN_THIMBLE);
			output.accept(TATTERLACE_HEADDRESS);
		});
	}

	/** The Needle Rapier, registered with the other Arms VII trophies (weapons/ArmVariants.java). */
	public static Item needleRapier() {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id("needle_rapier"));
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
