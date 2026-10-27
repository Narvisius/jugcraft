package io.github.jimbozoomer.jugcraft.lair.king;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.lair.KingsGourdBlock;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairRewards;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.weapons.DescribedItem;
import io.github.jimbozoomer.jugcraft.weapons.FrightBoon;
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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.phys.AABB;

/**
 * The Pumpkin King's registrations (docs/features/pumpkin-king.md): he, his Jack-o'-Straws, the pumpkin he tosses and the
 * head he rolls; his loot (the King's Ember, which burns as long as a block of coal and makes a King's Lantern with any
 * heirloom pumpkin; the Crowward Charm; the Pumpkin King's Crown; the Hallowreap and the Crowbill are Arms VII trophies,
 * weapons/ArmVariants.java, with their Fright boon). He is hung on the post of every Crowfield as it is placed (part 1,
 * docs/features/crowfield.md), and told when a player picks one of its King's Gourds.
 *
 * <p>The Crowward Charm: held in the offhand, crows will not come down on crops within {@value #CHARM_REACH} blocks of its
 * holder, as a lit scarecrow keeps them off. It is in {@code #jugcraft:crow_wards}, which agriculture/Scarecrows.java
 * reads; decided on the server.
 */
public final class JugcraftPumpkinKing {
	public static final int CHARM_REACH = 12;
	/** The King's Ember: as long as a block of coal burns (80 items). */
	public static final int EMBER_BURN = 16000;
	/** The King's Lantern: its ember never goes out. */
	public static final int LANTERN_LIGHT = 15;
	public static EntityType<PumpkinKingEntity> PUMPKIN_KING;
	public static EntityType<JackOStrawEntity> JACK_O_STRAW;
	public static EntityType<KingsPumpkinEntity> KINGS_PUMPKIN;
	public static EntityType<KingsHeadEntity> KINGS_HEAD;
	public static Item KINGS_EMBER;
	public static Item CROWWARD_CHARM;
	public static Item PUMPKIN_KINGS_CROWN;
	public static KingsLanternBlock KINGS_LANTERN;
	public static Item KINGS_LANTERN_ITEM;

	private JugcraftPumpkinKing() {
	}

	public static void register() {
		// His rewards may wait for a player (lair/LairRewards), as every Witching Season boss's do.
		LairRewards.payer(PumpkinKingLoot.BOSS, PumpkinKingLoot::pay);
		PUMPKIN_KING = entity("pumpkin_king", EntityType.Builder.<PumpkinKingEntity>of(PumpkinKingEntity::new, MobCategory.MONSTER)
				.sized(1.8F, 5.4F).eyeHeight(4.7F).noLootTable().clientTrackingRange(14));
		FabricDefaultAttributeRegistry.register(PUMPKIN_KING, PumpkinKingEntity.createAttributes());
		JACK_O_STRAW = entity("jack_o_straw", EntityType.Builder.<JackOStrawEntity>of(JackOStrawEntity::new, MobCategory.MONSTER)
				.sized(0.7F, 1.4F).eyeHeight(1.15F).noLootTable().noSave().clientTrackingRange(10));
		FabricDefaultAttributeRegistry.register(JACK_O_STRAW, JackOStrawEntity.createAttributes());
		KINGS_PUMPKIN = entity("kings_pumpkin", EntityType.Builder.<KingsPumpkinEntity>of(KingsPumpkinEntity::new, MobCategory.MISC)
				.sized(0.8F, 0.8F).fireImmune().noLootTable().noSave().clientTrackingRange(8).updateInterval(1));
		KINGS_HEAD = entity("kings_head", EntityType.Builder.<KingsHeadEntity>of(KingsHeadEntity::new, MobCategory.MISC)
				.sized(1.4F, 1.4F).fireImmune().noLootTable().noSave().clientTrackingRange(8).updateInterval(1));

		// Each says what it is for in a grey line (tooltip.jugcraft.<id>).
		KINGS_EMBER = item("kings_ember", new Item.Properties().rarity(Rarity.UNCOMMON).component(DataComponents.COOKING_FUEL,
				new CookingFuel(new ResolvableInt.Constant(EMBER_BURN), new ResolvableFloat.Constant(1.0F))));
		CROWWARD_CHARM = item("crowward_charm", new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
		// Worn on the head, it is drawn there as its own model (its "head" display), as the costume hats are.
		PUMPKIN_KINGS_CROWN = item("pumpkin_kings_crown", new Item.Properties().stacksTo(1).rarity(Rarity.RARE).component(
				DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).setEquipSound(SoundEvents.ARMOR_EQUIP_GOLD).build()));
		ResourceKey<net.minecraft.world.level.block.Block> lanternKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("kings_lantern"));
		KINGS_LANTERN = Registry.register(BuiltInRegistries.BLOCK, lanternKey, new KingsLanternBlock(BlockBehaviour.Properties.of()
				.mapColor(MapColor.COLOR_ORANGE).strength(1.0F).sound(SoundType.WOOD).lightLevel(state -> LANTERN_LIGHT)
				.pushReaction(PushReaction.POPPED).setId(lanternKey)));
		ResourceKey<Item> lanternItemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("kings_lantern"));
		KINGS_LANTERN_ITEM = Registry.register(BuiltInRegistries.ITEM, lanternItemKey,
				new BlockItem(KINGS_LANTERN, new Item.Properties().useBlockDescriptionPrefix().setId(lanternItemKey)));

		Lairs.onPlaced(Lair.CROWFIELD, PumpkinKingEntity::summon);
		KingsGourdBlock.onPicked(JugcraftPumpkinKing::gourdPicked);
		FrightBoon.register();
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> output.accept(KINGS_EMBER));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(KINGS_LANTERN_ITEM));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			output.accept(CROWWARD_CHARM);
			output.accept(PUMPKIN_KINGS_CROWN);
		});
	}

	/** A player picked a King's Gourd: the King whose field it is (if he is near) answers. */
	private static void gourdPicked(ServerLevel level, BlockPos pos, BlockState state, Player player) {
		for (PumpkinKingEntity king : level.getEntitiesOfClass(PumpkinKingEntity.class, new AABB(pos).inflate(64.0), PumpkinKingEntity::isAlive)) {
			if (king.gourdPicked(level, pos, player)) {
				return;
			}
		}
	}

	/** The Hallowreap and the Crowbill, registered with the other Arms VII trophies (weapons/ArmVariants.java). */
	public static Item hallowreap() {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id("hallowreap"));
	}

	public static Item crowbill() {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id("crowbill"));
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
