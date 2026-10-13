package io.github.jimbozoomer.jugcraft.lair.vesperine;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.weapons.DescribedItem;
import io.github.jimbozoomer.jugcraft.weapons.HarvestBoon;
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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Vesperine's registrations (docs/features/vesperine.md): she and her skulls, their bolts, her thrown scythe, her
 * thralls, the harvest's souls and the Vesper Scythe's crescent; her loot (Reaper's Shade, the Reaper's Hood and the
 * Dirge and Requiem skull trophies; the Vesper Scythe is an Arms VII trophy, weapons/ArmVariants.java). She is seated
 * on the throne of every Hollow Acre as it is placed.
 */
public final class JugcraftVesperine {
	public static EntityType<VesperineEntity> VESPERINE;
	public static EntityType<ReaperSkullEntity> DIRGE;
	public static EntityType<ReaperSkullEntity> REQUIEM;
	public static EntityType<GriefBoltEntity> GRIEF_BOLT;
	public static EntityType<ThrownScytheEntity> THROWN_SCYTHE;
	public static EntityType<GraveThrallEntity> GRAVE_THRALL;
	public static EntityType<HarvestSoulEntity> HARVEST_SOUL;
	public static EntityType<ReapingCrescentEntity> REAPING_CRESCENT;
	public static Item REAPER_SHADE;
	public static Item REAPER_HOOD;
	public static Block DIRGE_SKULL;
	public static Block REQUIEM_SKULL;
	public static Item DIRGE_SKULL_ITEM;
	public static Item REQUIEM_SKULL_ITEM;

	private JugcraftVesperine() {
	}

	public static void register() {
		VESPERINE = entity("vesperine", EntityType.Builder.<VesperineEntity>of(VesperineEntity::new, MobCategory.MONSTER)
				.sized(1.0F, 3.2F).eyeHeight(2.8F).fireImmune().noLootTable().clientTrackingRange(12));
		FabricDefaultAttributeRegistry.register(VESPERINE, VesperineEntity.createAttributes());
		DIRGE = entity("dirge", EntityType.Builder.<ReaperSkullEntity>of(ReaperSkullEntity::new, MobCategory.MONSTER)
				.sized(1.25F, 1.25F).eyeHeight(0.6F).fireImmune().noLootTable().clientTrackingRange(12));
		FabricDefaultAttributeRegistry.register(DIRGE, ReaperSkullEntity.createAttributes());
		REQUIEM = entity("requiem", EntityType.Builder.<ReaperSkullEntity>of(ReaperSkullEntity::new, MobCategory.MONSTER)
				.sized(1.25F, 1.25F).eyeHeight(0.6F).fireImmune().noLootTable().clientTrackingRange(12));
		FabricDefaultAttributeRegistry.register(REQUIEM, ReaperSkullEntity.createAttributes());
		GRIEF_BOLT = entity("grief_bolt", EntityType.Builder.<GriefBoltEntity>of(GriefBoltEntity::new, MobCategory.MISC)
				.sized(0.5F, 0.5F).fireImmune().noLootTable().noSave().clientTrackingRange(8).updateInterval(2));
		THROWN_SCYTHE = entity("thrown_scythe", EntityType.Builder.<ThrownScytheEntity>of(ThrownScytheEntity::new, MobCategory.MISC)
				.sized(1.5F, 0.5F).fireImmune().noLootTable().noSave().clientTrackingRange(8).updateInterval(1));
		GRAVE_THRALL = entity("grave_thrall", EntityType.Builder.<GraveThrallEntity>of(GraveThrallEntity::new, MobCategory.MONSTER)
				.sized(0.6F, 1.99F).fireImmune().noLootTable().noSave().clientTrackingRange(8));
		FabricDefaultAttributeRegistry.register(GRAVE_THRALL, GraveThrallEntity.createAttributes());
		HARVEST_SOUL = entity("harvest_soul", EntityType.Builder.<HarvestSoulEntity>of(HarvestSoulEntity::new, MobCategory.MISC)
				.sized(0.5F, 0.5F).fireImmune().noLootTable().noSave().clientTrackingRange(8).updateInterval(2));
		REAPING_CRESCENT = entity("reaping_crescent", EntityType.Builder.<ReapingCrescentEntity>of(ReapingCrescentEntity::new,
				MobCategory.MISC).sized(1.0F, 0.5F).fireImmune().noLootTable().noSave().clientTrackingRange(8).updateInterval(1));

		REAPER_SHADE = item("reaper_shade", new Item.Properties().rarity(Rarity.UNCOMMON));
		// Each says what it is for in a grey line (tooltip.jugcraft.<id>).
		// Worn on the head, it is drawn there as its own model (its "head" display), as the costume hats are.
		REAPER_HOOD = item("reaper_hood", new Item.Properties().stacksTo(1).rarity(Rarity.RARE).component(DataComponents.EQUIPPABLE,
				Equippable.builder(EquipmentSlot.HEAD).setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER).build()));
		DIRGE_SKULL = skull("dirge_skull", true);
		REQUIEM_SKULL = skull("requiem_skull", false);
		DIRGE_SKULL_ITEM = skullItem("dirge_skull", DIRGE_SKULL);
		REQUIEM_SKULL_ITEM = skullItem("requiem_skull", REQUIEM_SKULL);

		Lairs.onPlaced(Lair.HOLLOW_ACRE, VesperineEntity::summon);
		HarvestBoon.register();
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> output.accept(REAPER_SHADE));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> output.accept(REAPER_HOOD));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> {
			output.accept(DIRGE_SKULL_ITEM);
			output.accept(REQUIEM_SKULL_ITEM);
		});
	}

	/** The Vesper Scythe, registered with the other Arms VII trophies (weapons/ArmVariants.java). */
	public static Item vesperScythe() {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id("vesper_scythe"));
	}

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> entity(String id, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id(id));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	private static Item item(String id, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
		return Registry.register(BuiltInRegistries.ITEM, key, new DescribedItem(properties.setId(key)));
	}

	/** A skull trophy: hovering and glowing (light 6), as hard as bone. */
	private static Block skull(String id, boolean dirge) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(id));
		return Registry.register(BuiltInRegistries.BLOCK, key, new TrophySkullBlock(dirge, BlockBehaviour.Properties.of()
				.mapColor(MapColor.COLOR_BLACK).strength(1.0F).sound(SoundType.BONE_BLOCK).lightLevel(state -> 6).noOcclusion()
				.pushReaction(PushReaction.POPPED).setId(key)));
	}

	/** Its item, which can also be worn on the head (a costume). */
	private static Item skullItem(String id, Block block) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
		return Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix()
				.rarity(Rarity.EPIC).component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD)
						.setEquipSound(SoundEvents.ARMOR_EQUIP_GENERIC).build()).setId(key)));
	}
}
