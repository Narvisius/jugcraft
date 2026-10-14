package io.github.jimbozoomer.jugcraft.lair.lich;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.lair.ArchiveLecternBlock;
import io.github.jimbozoomer.jugcraft.lair.GraveSealBlock;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.NameStoneBlock.Rune;
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
 * The Crypt Lich's registrations (docs/features/crypt-lich.md): he, his Restless Dead, his Soul Bolts and his bones; his
 * loot (Grave Dust, the Emptied Phylactery and the Lich's Crown; the Soulreaver and the Gravewarden are Arms VII trophies,
 * weapons/ArmVariants.java, with their Drain and Wither boons). He is seated on the throne of every Archive of Last Names
 * as it is placed, and told when a player uses one of its grave seals or reads its lectern.
 */
public final class JugcraftLich {
	public static EntityType<CryptLichEntity> CRYPT_LICH;
	public static EntityType<RestlessDeadEntity> RESTLESS_DEAD;
	public static EntityType<SoulBoltEntity> SOUL_BOLT;
	public static EntityType<LichBonesEntity> LICH_BONES;
	public static Item GRAVE_DUST;
	public static Item EMPTIED_PHYLACTERY;
	public static Item LICHS_CROWN;

	private JugcraftLich() {
	}

	public static void register() {
		CRYPT_LICH = entity("crypt_lich", EntityType.Builder.<CryptLichEntity>of(CryptLichEntity::new, MobCategory.MONSTER)
				.sized(0.9F, 2.5F).eyeHeight(2.1F).noLootTable().clientTrackingRange(12));
		FabricDefaultAttributeRegistry.register(CRYPT_LICH, CryptLichEntity.createAttributes());
		RESTLESS_DEAD = entity("restless_dead", EntityType.Builder.<RestlessDeadEntity>of(RestlessDeadEntity::new, MobCategory.MONSTER)
				.sized(0.6F, 1.8F).eyeHeight(1.5F).noLootTable().noSave().clientTrackingRange(10));
		FabricDefaultAttributeRegistry.register(RESTLESS_DEAD, RestlessDeadEntity.createAttributes());
		SOUL_BOLT = entity("soul_bolt", EntityType.Builder.<SoulBoltEntity>of(SoulBoltEntity::new, MobCategory.MISC)
				.sized(0.5F, 0.5F).noLootTable().noSave().clientTrackingRange(8).updateInterval(1));
		LICH_BONES = entity("lich_bones", EntityType.Builder.<LichBonesEntity>of(LichBonesEntity::new, MobCategory.MISC)
				.sized(1.0F, 1.0F).noLootTable().noSave().clientTrackingRange(8));

		// Each says what it is for in a grey line (tooltip.jugcraft.<id>).
		GRAVE_DUST = item("grave_dust", new Item.Properties().rarity(Rarity.UNCOMMON));
		EMPTIED_PHYLACTERY = item("emptied_phylactery", new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
		// Worn on the head, it is drawn there as its own model (its "head" display), as the costume hats are.
		LICHS_CROWN = item("lichs_crown", new Item.Properties().stacksTo(1).rarity(Rarity.RARE).component(
				DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).setEquipSound(SoundEvents.ARMOR_EQUIP_IRON).build()));

		Lairs.onPlaced(Lair.CRYPT_ARCHIVE, CryptLichEntity::summon);
		GraveSealBlock.onUsed(JugcraftLich::sealUsed);
		ArchiveLecternBlock.onRead(JugcraftLich::read);
		EmptiedPhylactery.register();
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> output.accept(GRAVE_DUST));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			output.accept(EMPTIED_PHYLACTERY);
			output.accept(LICHS_CROWN);
		});
	}

	/** A player used a grave seal: the Lich whose archive it is (if he is near) answers. */
	private static boolean sealUsed(ServerLevel level, BlockPos pos, BlockState state, Player player) {
		for (CryptLichEntity lich : liches(level, pos)) {
			if (lich.sealUsed(level, pos, state, player)) {
				return true;
			}
		}
		return false;
	}

	/** A player read a Lectern of Last Names: the Lich whose archive it is (if he is near) is told. */
	private static void read(ServerLevel level, BlockPos lectern, Player player, List<Rune> restored) {
		for (CryptLichEntity lich : liches(level, lectern)) {
			if (lich.lectern().equals(lectern)) {
				lich.lecternRead(level, player, restored);
			}
		}
	}

	private static List<CryptLichEntity> liches(ServerLevel level, BlockPos near) {
		return level.getEntitiesOfClass(CryptLichEntity.class, new AABB(near).inflate(64.0), CryptLichEntity::isAlive);
	}

	/** The Soulreaver and the Gravewarden, registered with the other Arms VII trophies (weapons/ArmVariants.java). */
	public static Item soulreaver() {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id("soulreaver"));
	}

	public static Item gravewarden() {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id("gravewarden"));
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
