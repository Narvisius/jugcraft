package io.github.jimbozoomer.jugcraft.lair.alpha;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.Werewolf;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.WardenBellBlock;
import io.github.jimbozoomer.jugcraft.weapons.DescribedItem;
import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The Alpha Werewolf's registrations (docs/features/alpha-werewolf.md): he and his Packmates; his loot (the Alpha Pelt,
 * the Packfinder Charm and the Alpha's Hood; Moonfang and the Howler are Arms VII trophies, weapons/ArmVariants.java, with
 * their Howl boon). He is crouched on the Howling Stone of every Moonlit Ring as it is placed, and told when a player
 * rings one of its Warden Bells. His trail, raked turf, is one of the ring's fixtures (lair/JugcraftLairs.java).
 *
 * <p>The Packfinder Charm: held in the offhand, every {@value #CHARM_EVERY} ticks it makes every werewolf within
 * {@value #CHARM_REACH} blocks of its holder (the werewolves of the woods, his Packmates and he) glow for
 * {@value #CHARM_GLOW} ticks, for everyone to see. Decided on the server.
 */
public final class JugcraftAlpha {
	public static final double CHARM_REACH = 32.0;
	public static final int CHARM_EVERY = 40;
	public static final int CHARM_GLOW = 60;
	public static EntityType<AlphaWerewolfEntity> ALPHA_WEREWOLF;
	public static EntityType<PackmateEntity> PACKMATE;
	public static Item ALPHA_PELT;
	public static Item PACKFINDER_CHARM;
	public static Item ALPHAS_HOOD;

	private JugcraftAlpha() {
	}

	public static void register() {
		ALPHA_WEREWOLF = entity("alpha_werewolf", EntityType.Builder.<AlphaWerewolfEntity>of(AlphaWerewolfEntity::new, MobCategory.MONSTER)
				.sized(1.6F, 3.6F).eyeHeight(3.1F).noLootTable().clientTrackingRange(14));
		FabricDefaultAttributeRegistry.register(ALPHA_WEREWOLF, AlphaWerewolfEntity.createAttributes());
		PACKMATE = entity("packmate", EntityType.Builder.<PackmateEntity>of(PackmateEntity::new, MobCategory.MONSTER)
				.sized(0.9F, 2.4F).eyeHeight(2.1F).noLootTable().noSave().clientTrackingRange(10));
		FabricDefaultAttributeRegistry.register(PACKMATE, PackmateEntity.createAttributes());

		// Each says what it is for in a grey line (tooltip.jugcraft.<id>).
		ALPHA_PELT = item("alpha_pelt", new Item.Properties().rarity(Rarity.UNCOMMON));
		PACKFINDER_CHARM = item("packfinder_charm", new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
		// Worn on the head, it is drawn there as its own model (its "head" display), as the costume hats are.
		ALPHAS_HOOD = item("alphas_hood", new Item.Properties().stacksTo(1).rarity(Rarity.RARE).component(
				DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER).build()));

		Lairs.onPlaced(Lair.MOONLIT_RING, AlphaWerewolfEntity::summon);
		WardenBellBlock.onRung(JugcraftAlpha::bellRung);
		ServerTickEvents.END_LEVEL_TICK.register(JugcraftAlpha::charms);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> output.accept(ALPHA_PELT));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			output.accept(PACKFINDER_CHARM);
			output.accept(ALPHAS_HOOD);
		});
	}

	/** A player rang a Warden Bell: the Alpha whose ring it is (if he is near) answers. */
	private static void bellRung(ServerLevel level, BlockPos pos, BlockState state, Player player) {
		for (AlphaWerewolfEntity alpha : level.getEntitiesOfClass(AlphaWerewolfEntity.class, new AABB(pos).inflate(64.0), AlphaWerewolfEntity::isAlive)) {
			if (alpha.bellRung(level, pos, player)) {
				return;
			}
		}
	}

	/** Every {@value #CHARM_EVERY} ticks, the werewolves near each player holding a Packfinder Charm in their offhand glow. */
	private static void charms(ServerLevel level) {
		if (level.getGameTime() % CHARM_EVERY != 0) {
			return;
		}
		for (ServerPlayer player : level.players()) {
			if (holdsCharm(player)) {
				glow(level, player);
			}
		}
	}

	/** Whether {@code living} holds a Packfinder Charm in their offhand. */
	public static boolean holdsCharm(LivingEntity living) {
		return PACKFINDER_CHARM != null && living.getOffhandItem().is(PACKFINDER_CHARM);
	}

	/** Every werewolf within {@value #CHARM_REACH} blocks of {@code holder} glows; returns how many. */
	public static int glow(ServerLevel level, LivingEntity holder) {
		List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class, holder.getBoundingBox().inflate(CHARM_REACH),
				entity -> werewolf(entity) && entity.isAlive() && entity.distanceTo(holder) <= CHARM_REACH);
		for (LivingEntity werewolf : found) {
			werewolf.addEffect(new MobEffectInstance(MobEffects.GLOWING, CHARM_GLOW, 0, false, false));
		}
		return found.size();
	}

	/** Whether the Packfinder Charm finds {@code entity}: a werewolf of the woods, a Packmate or the Alpha. */
	public static boolean werewolf(LivingEntity entity) {
		return entity instanceof Werewolf || entity instanceof PackmateEntity || entity instanceof AlphaWerewolfEntity;
	}

	/** Moonfang and the Howler, registered with the other Arms VII trophies (weapons/ArmVariants.java). */
	public static Item moonfang() {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id("moonfang"));
	}

	public static Item howler() {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id("howler"));
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
