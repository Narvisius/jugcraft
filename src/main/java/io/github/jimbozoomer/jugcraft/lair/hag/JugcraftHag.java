package io.github.jimbozoomer.jugcraft.lair.hag;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.lair.FenLampBlock;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.weapons.DescribedItem;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.minecraft.world.phys.AABB;

/**
 * The Mire Hag's registrations (docs/features/mire-hag.md): she, her Likenesses, her Root Knot, her roots, her grasping
 * hands and her brews; her loot (Hagmoss, the Hag's Eye and the Hag's Shawl; the Hagthorn and the Bogfang are Arms VII
 * trophies, weapons/ArmVariants.java, with their Venom boon). She is seated on the porch of every Lantern Fen as it is
 * placed, and told when a player lights one of its lamps again.
 */
public final class JugcraftHag {
	public static EntityType<MireHagEntity> MIRE_HAG;
	public static EntityType<HagLikenessEntity> HAG_LIKENESS;
	public static EntityType<RootKnotEntity> ROOT_KNOT;
	public static EntityType<HagRootsEntity> HAG_ROOTS;
	public static EntityType<MireGraspEntity> MIRE_GRASP;
	public static EntityType<PoisonBrewEntity> POISON_BREW;
	public static Item HAGMOSS;
	public static Item HAGS_EYE;
	public static Item HAGS_SHAWL;

	private JugcraftHag() {
	}

	public static void register() {
		// Her Likenesses are she to look at: her size, her name, her model and her sheet.
		MIRE_HAG = entity("mire_hag", EntityType.Builder.<MireHagEntity>of(MireHagEntity::new, MobCategory.MONSTER)
				.sized(1.0F, 2.3F).eyeHeight(2.0F).noLootTable().clientTrackingRange(12));
		FabricDefaultAttributeRegistry.register(MIRE_HAG, MireHagEntity.createAttributes());
		HAG_LIKENESS = entity("hag_likeness", EntityType.Builder.<HagLikenessEntity>of(HagLikenessEntity::new, MobCategory.MONSTER)
				.sized(1.0F, 2.3F).eyeHeight(2.0F).noLootTable().noSave().clientTrackingRange(12));
		FabricDefaultAttributeRegistry.register(HAG_LIKENESS, HagLikenessEntity.createAttributes());
		ROOT_KNOT = entity("root_knot", EntityType.Builder.<RootKnotEntity>of(RootKnotEntity::new, MobCategory.MONSTER)
				.sized(1.2F, 0.9F).eyeHeight(0.6F).noLootTable().noSave().clientTrackingRange(10));
		FabricDefaultAttributeRegistry.register(ROOT_KNOT, RootKnotEntity.createAttributes());
		HAG_ROOTS = entity("hag_roots", EntityType.Builder.<HagRootsEntity>of(HagRootsEntity::new, MobCategory.MISC)
				.sized(0.6F, 1.0F).noLootTable().noSave().clientTrackingRange(8));
		MIRE_GRASP = entity("mire_grasp", EntityType.Builder.<MireGraspEntity>of(MireGraspEntity::new, MobCategory.MISC)
				.sized(3.0F, 1.0F).noLootTable().noSave().clientTrackingRange(8));
		POISON_BREW = entity("poison_brew", EntityType.Builder.<PoisonBrewEntity>of(PoisonBrewEntity::new, MobCategory.MISC)
				.sized(0.4F, 0.4F).noLootTable().noSave().clientTrackingRange(8).updateInterval(1));

		// Each says what it is for in a grey line (tooltip.jugcraft.<id>).
		HAGMOSS = item("hagmoss", new Item.Properties().rarity(Rarity.UNCOMMON).compostable(ContextIntProviders.COMPOSTABLE_MEDIUM));
		HAGS_EYE = item("hags_eye", new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
		// Worn on the head, it is drawn there as its own model (its "head" display), as the costume hats are.
		HAGS_SHAWL = item("hags_shawl", new Item.Properties().stacksTo(1).rarity(Rarity.RARE).component(
				DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER).build()));

		Lairs.onPlaced(Lair.LANTERN_FEN, MireHagEntity::summon);
		FenLampBlock.onLitByHand(JugcraftHag::lampLit);
		HagRootsEntity.register();
		HagsEye.register();
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> output.accept(HAGMOSS));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			output.accept(HAGS_EYE);
			output.accept(HAGS_SHAWL);
		});
	}

	/** A player lit a fen lamp again: the Hag whose fen it is (if she is near) is shown, and her Likenesses in its light burst. */
	private static void lampLit(ServerLevel level, BlockPos lamp, Player player) {
		for (MireHagEntity hag : level.getEntitiesOfClass(MireHagEntity.class, new AABB(lamp).inflate(64.0),
				found -> found.isAlive() && found.lamps().contains(lamp))) {
			hag.lampLit(level, lamp, player);
		}
	}

	/** The Hagthorn and the Bogfang, registered with the other Arms VII trophies (weapons/ArmVariants.java). */
	public static Item hagthorn() {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id("hagthorn"));
	}

	public static Item bogfang() {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id("bogfang"));
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
