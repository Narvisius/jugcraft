package io.github.jimbozoomer.jugcraft.lair.dreadnought;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.lair.DischargeBreakerBlock;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.weapons.DescribedItem;
import java.util.List;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The Iron Dreadnought's registrations (docs/features/iron-dreadnought.md): he, his Sparkers, his hot rivets and his
 * mortar shells; his loot (Salvaged Coil, the Grounding Strap and the Dreadnought Visor; the Dynamo Halberd and the Piston
 * Hammer are Arms VII trophies, weapons/ArmVariants.java, with their Shock boon). He is stood in the gantry of every
 * Foundry Trench as it is placed, and told when a player uses one of its breakers.
 */
public final class JugcraftDreadnought {
	public static EntityType<IronDreadnoughtEntity> IRON_DREADNOUGHT;
	public static EntityType<SparkerEntity> SPARKER;
	public static EntityType<HotRivetEntity> HOT_RIVET;
	public static EntityType<MortarShellEntity> MORTAR_SHELL;
	public static Item SALVAGED_COIL;
	public static Item GROUNDING_STRAP;
	public static Item DREADNOUGHT_VISOR;

	private JugcraftDreadnought() {
	}

	public static void register() {
		IRON_DREADNOUGHT = entity("iron_dreadnought", EntityType.Builder.<IronDreadnoughtEntity>of(IronDreadnoughtEntity::new,
				MobCategory.MONSTER).sized(3.5F, 5.5F).eyeHeight(4.9F).noLootTable().clientTrackingRange(14));
		FabricDefaultAttributeRegistry.register(IRON_DREADNOUGHT, IronDreadnoughtEntity.createAttributes());
		SPARKER = entity("sparker", EntityType.Builder.<SparkerEntity>of(SparkerEntity::new, MobCategory.MONSTER)
				.sized(0.7F, 0.8F).eyeHeight(0.4F).noLootTable().noSave().clientTrackingRange(10));
		FabricDefaultAttributeRegistry.register(SPARKER, SparkerEntity.createAttributes());
		HOT_RIVET = entity("hot_rivet", EntityType.Builder.<HotRivetEntity>of(HotRivetEntity::new, MobCategory.MISC)
				.sized(0.4F, 0.4F).noLootTable().noSave().clientTrackingRange(8).updateInterval(1));
		MORTAR_SHELL = entity("mortar_shell", EntityType.Builder.<MortarShellEntity>of(MortarShellEntity::new, MobCategory.MISC)
				.sized(0.5F, 0.5F).noLootTable().noSave().clientTrackingRange(10).updateInterval(1));

		// Each says what it is for in a grey line (tooltip.jugcraft.<id>).
		SALVAGED_COIL = item("salvaged_coil", new Item.Properties().rarity(Rarity.UNCOMMON));
		GROUNDING_STRAP = item("grounding_strap", new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
		// Worn on the head, it is drawn there as its own model (its "head" display), as the costume hats are.
		DREADNOUGHT_VISOR = item("dreadnought_visor", new Item.Properties().stacksTo(1).rarity(Rarity.RARE).component(
				DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).setEquipSound(SoundEvents.ARMOR_EQUIP_IRON).build()));

		Lairs.onPlaced(Lair.FOUNDRY_TRENCH, IronDreadnoughtEntity::summon);
		DischargeBreakerBlock.onUsed(JugcraftDreadnought::breakerUsed);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> output.accept(SALVAGED_COIL));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			output.accept(GROUNDING_STRAP);
			output.accept(DREADNOUGHT_VISOR);
		});
	}

	/** A player used a breaker: the Dreadnought whose trench it is (if he is near) answers. */
	private static boolean breakerUsed(ServerLevel level, BlockPos pos, BlockState state, Player player) {
		for (IronDreadnoughtEntity dreadnought : dreadnoughts(level, pos)) {
			if (dreadnought.breakerUsed(level, pos, state, player)) {
				return true;
			}
		}
		return false;
	}

	private static List<IronDreadnoughtEntity> dreadnoughts(ServerLevel level, BlockPos near) {
		return level.getEntitiesOfClass(IronDreadnoughtEntity.class, new AABB(near).inflate(64.0), IronDreadnoughtEntity::isAlive);
	}

	/** The Dynamo Halberd and the Piston Hammer, registered with the other Arms VII trophies (weapons/ArmVariants.java). */
	public static Item dynamoHalberd() {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id("dynamo_halberd"));
	}

	public static Item pistonHammer() {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id("piston_hammer"));
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
