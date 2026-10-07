package io.github.jimbozoomer.jugcraft.raiders;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * The raider faction (batch 57, docs/features/raiders.md): raider infantry ({@link RaiderInfantry}), the
 * {@link RaiderWalker}, the {@link RaiderBlimp}, their bombs ({@link RaiderBomb}), the officer's insignia and the raids
 * that bring them ({@link RaiderRaids}). Keep the numbers in sync with tools/raiders.py; tools/check_mod_data.py checks
 * them. The feature switch ({@value #FEATURE}) and {@code raiders.raids=off} stop raids; raiders already in the world
 * stay until their raid ends.
 */
public final class JugcraftRaiders {
	public static final String FEATURE = "raiders";
	/** Grenades and bombs: blast radius and damage at the centre. Smaller than a player's grenade (radius 4, 16). */
	public static final double GRENADE_RADIUS = 2.5;
	public static final float GRENADE_DAMAGE = 6.0F;
	public static final double BOMB_RADIUS = 3.5;
	public static final float BOMB_DAMAGE = 10.0F;
	/** The grenadier: ticks between throws, and the range it throws from. */
	public static final int GRENADE_COOLDOWN = 70;
	public static final int GRENADE_MIN_RANGE = 6;
	public static final int GRENADE_MAX_RANGE = 18;
	/** The officer's rally (every so many ticks, how far, how long it lasts) and the rout when they fall. */
	public static final int RALLY_TICKS = 40;
	public static final int RALLY_RADIUS = 12;
	public static final int RALLY_EFFECT = 60;
	public static final int ROUT_TICKS = 200;
	/** The walker's grenade launcher: ticks between shots and its range. (It punches as often as any brawler: once a second.) */
	public static final int WALKER_PUNCH_COOLDOWN = 20;
	public static final int WALKER_LAUNCH_COOLDOWN = 100;
	public static final int WALKER_LAUNCH_MIN = 8;
	public static final int WALKER_LAUNCH_MAX = 24;
	/** The blimp: cruising height over its target, ticks between bombs and how near overhead it must be. */
	public static final int BLIMP_CRUISE = 16;
	public static final int BLIMP_BOMB_COOLDOWN = 50;
	public static final int BLIMP_BOMB_REACH = 3;
	/** Siege ladders: how long a grunt must be stuck against a wall (ticks), the tallest it climbs, and how long a ladder lasts. */
	public static final int LADDER_STUCK = 40;
	public static final int LADDER_MAX = 8;
	public static final int LADDER_TTL = 1200;

	public static EntityType<RaiderInfantry> GRUNT;
	public static EntityType<RaiderInfantry> GRENADIER;
	public static EntityType<RaiderInfantry> OFFICER;
	public static EntityType<RaiderWalker> WALKER;
	public static EntityType<RaiderBlimp> BLIMP;
	public static EntityType<RaiderBomb> BOMB;
	public static Item INSIGNIA;
	public static Item RAID_HORN;
	public static net.minecraft.world.level.block.Block SIEGE_LADDER;

	private JugcraftRaiders() {
	}

	public static void register() {
		GRUNT = infantry("raider_grunt", 24, 5, 4, 0.30);
		GRENADIER = infantry("raider_grenadier", 20, 3, 2, 0.28);
		OFFICER = infantry("raider_officer", 32, 6, 6, 0.30);
		WALKER = entity("raider_walker", EntityType.Builder.<RaiderWalker>of(RaiderWalker::new, MobCategory.MONSTER)
				.sized(2.6F, 4.6F).eyeHeight(3.5F).notInPeaceful().clientTrackingRange(10));
		FabricDefaultAttributeRegistry.register(WALKER, machine("raider_walker", 120, 14, 14, 0.22, RaiderWalker::attributes));
		BLIMP = entity("raider_blimp", EntityType.Builder.<RaiderBlimp>of(RaiderBlimp::new, MobCategory.MONSTER)
				.sized(2.8F, 4.4F).eyeHeight(1.0F).notInPeaceful().clientTrackingRange(12));
		FabricDefaultAttributeRegistry.register(BLIMP, machine("raider_blimp", 50, 0, 2, 0.12, RaiderBlimp::attributes));
		BOMB = entity("raider_bomb", EntityType.Builder.<RaiderBomb>of(RaiderBomb::new, MobCategory.MISC)
				.sized(0.25F, 0.25F).clientTrackingRange(8).updateInterval(10));
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id("raider_insignia"));
		INSIGNIA = Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key).rarity(Rarity.UNCOMMON)) {
			@Override
			public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
					Consumer<Component> tooltip, TooltipFlag flag) {
				tooltip.accept(Component.translatable("tooltip.jugcraft.raider_insignia").withStyle(ChatFormatting.GRAY));
			}
		});
		ResourceKey<Item> hornKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("raid_horn"));
		RAID_HORN = Registry.register(BuiltInRegistries.ITEM, hornKey, new RaidHornItem(new Item.Properties().setId(hornKey).stacksTo(1)
				.rarity(Rarity.RARE)));
		ResourceKey<net.minecraft.world.level.block.Block> ladderKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("siege_ladder"));
		SIEGE_LADDER = Registry.register(BuiltInRegistries.BLOCK, ladderKey, SiegeLadderBlock.create(
				net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.LADDER)
						.strength(0.2F).noLootTable().setId(ladderKey)));
		// Raider camps (raider extras): rare, out in the plains, savanna and badlands.
		RaiderCamps.register();
		RaiderRaids.register();
	}

	/** An infantry kind: (max health, attack damage, armour, movement speed). */
	private static EntityType<RaiderInfantry> infantry(String id, double health, double damage, double armour, double speed) {
		EntityType<RaiderInfantry> type = entity(id, EntityType.Builder.<RaiderInfantry>of(RaiderInfantry::new, MobCategory.MONSTER)
				.sized(0.6F, 1.95F).eyeHeight(1.62F).notInPeaceful().clientTrackingRange(8));
		FabricDefaultAttributeRegistry.register(type, RaiderInfantry.attributes(health, damage, armour, speed));
		return type;
	}

	private interface Attributes {
		AttributeSupplier.Builder of(double health, double damage, double armour, double speed);
	}

	/** A machine's attributes: (max health, attack damage, armour, movement speed). */
	private static AttributeSupplier.Builder machine(String id, double health, double damage, double armour, double speed,
			Attributes attributes) {
		return attributes.of(health, damage, armour, speed);
	}

	private static <T extends Entity> EntityType<T> entity(String id, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id(id));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}
}
