package io.github.jimbozoomer.jugcraft.lair.yeti;

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
 * The Yeti King's registrations (docs/features/yeti-king.md): he, his whelps, his hurled boulders, falling icicles and
 * glacial spikes; his loot (Yeti Fur, the Yeti Mitten and the Yeti King's Crown; the Glacier Maul and the Rimeclaw are
 * Arms VII trophies, weapons/ArmVariants.java, with their Frost boon). He is seated on the throne of every Glacier Hall as
 * it is placed.
 */
public final class JugcraftYeti {
	public static EntityType<YetiKingEntity> YETI_KING;
	public static EntityType<YetiWhelpEntity> YETI_WHELP;
	public static EntityType<HurledBoulderEntity> HURLED_BOULDER;
	public static EntityType<FallingIcicleEntity> FALLING_ICICLE;
	public static EntityType<GlacialSpikeEntity> GLACIAL_SPIKE;
	public static Item YETI_FUR;
	public static Item YETI_MITTEN;
	public static Item YETI_KING_CROWN;

	private JugcraftYeti() {
	}

	public static void register() {
		YETI_KING = entity("yeti_king", EntityType.Builder.<YetiKingEntity>of(YetiKingEntity::new, MobCategory.MONSTER)
				.sized(2.0F, 3.4F).eyeHeight(2.9F).fireImmune().noLootTable().clientTrackingRange(12));
		FabricDefaultAttributeRegistry.register(YETI_KING, YetiKingEntity.createAttributes());
		YETI_WHELP = entity("yeti_whelp", EntityType.Builder.<YetiWhelpEntity>of(YetiWhelpEntity::new, MobCategory.MONSTER)
				.sized(0.9F, 1.3F).eyeHeight(1.1F).noLootTable().noSave().clientTrackingRange(8));
		FabricDefaultAttributeRegistry.register(YETI_WHELP, YetiWhelpEntity.createAttributes());
		HURLED_BOULDER = entity("hurled_boulder", EntityType.Builder.<HurledBoulderEntity>of(HurledBoulderEntity::new, MobCategory.MISC)
				.sized(1.0F, 1.0F).fireImmune().noLootTable().noSave().clientTrackingRange(8).updateInterval(1));
		FALLING_ICICLE = entity("falling_icicle", EntityType.Builder.<FallingIcicleEntity>of(FallingIcicleEntity::new, MobCategory.MISC)
				.sized(0.5F, 1.5F).fireImmune().noLootTable().noSave().clientTrackingRange(8).updateInterval(1));
		GLACIAL_SPIKE = entity("glacial_spike", EntityType.Builder.<GlacialSpikeEntity>of(GlacialSpikeEntity::new, MobCategory.MISC)
				.sized(0.8F, 1.5F).fireImmune().noLootTable().noSave().clientTrackingRange(8).updateInterval(2));

		// Each says what it is for in a grey line (tooltip.jugcraft.<id>).
		YETI_FUR = item("yeti_fur", new Item.Properties().rarity(Rarity.UNCOMMON));
		YETI_MITTEN = item("yeti_mitten", new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
		// Worn on the head, it is drawn there as its own model (its "head" display), as the costume hats are.
		YETI_KING_CROWN = item("yeti_king_crown", new Item.Properties().stacksTo(1).rarity(Rarity.RARE).component(
				DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).setEquipSound(SoundEvents.ARMOR_EQUIP_GENERIC).build()));

		Lairs.onPlaced(Lair.GLACIER_HALL, YetiKingEntity::summon);
		YetiMitten.register();
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> output.accept(YETI_FUR));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			output.accept(YETI_MITTEN);
			output.accept(YETI_KING_CROWN);
		});
	}

	/** The Glacier Maul and the Rimeclaw, registered with the other Arms VII trophies (weapons/ArmVariants.java). */
	public static Item glacierMaul() {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id("glacier_maul"));
	}

	public static Item rimeclaw() {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id("rimeclaw"));
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
