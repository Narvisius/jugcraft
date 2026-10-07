package io.github.jimbozoomer.jugcraft.artillery;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Batch 51: the big guns (docs/features/big-guns.md). Keep the numbers in sync with tools/artillery.py;
 * tools/check_mod_data.py checks them. While a player crews a gun (as its first rider), their own attack and use are
 * switched off: attack fires the gun instead.
 */
public final class JugcraftArtillery {
	public static final double HEAVY_SPEED = 3.0;
	public static final double HEAVY_GRAVITY = 0.05;
	public static final double HEAVY_RADIUS = 5.0;
	public static final float HEAVY_DAMAGE = 32F;
	public static final double FLAK_SPEED = 4.0;
	public static final double FLAK_GRAVITY = 0.01;
	public static final double FLAK_RADIUS = 3.0;
	public static final float FLAK_DAMAGE = 10F;
	public static final int FLAK_FUSE = 30;
	public static final double FLAK_PROXIMITY = 2.5;
	public static final int MORTAR_COOLDOWN = 100;
	public static final int HOWITZER_COOLDOWN = 80;
	public static final int FLAK_COOLDOWN = 8;
	public static final float MORTAR_TRAVERSE = 2.0F;
	public static final float HOWITZER_TRAVERSE = 3.0F;
	public static final float FLAK_TRAVERSE = 12.0F;
	public static final int HOWITZER_ARC = 30;
	public static final double HOWITZER_SPEED = 0.1;
	public static final float HOWITZER_TURN = 2.0F;
	public static final int HOWITZER_FUEL_TANK = 6000;
	public static final int FUEL_PER_BUCKET = 1000;
	public static final int HOWITZER_FUEL_PER_SECOND = 5;
	public static final int BALLOON_HEIGHT = 32;
	public static final double BALLOON_CLIMB = 0.08;
	public static final int MARK_RANGE = 256;
	public static final int MARK_TTL = 6000;
	public static final int MORTAR_HEALTH = 150;
	public static final int HOWITZER_HEALTH = 140;
	public static final int FLAK_HEALTH = 60;
	public static final int BALLOON_HEALTH = 30;
	/** The guns' pivots above their feet, in blocks (tools/artillery.py's pivots, in pixels, over 16). */
	public static final double MORTAR_PIVOT_HEIGHT = 1.875;
	public static final double HOWITZER_PIVOT_HEIGHT = 2.0;
	public static final double FLAK_PIVOT_HEIGHT = 1.125;

	public static EntityType<SiegeMortar> SIEGE_MORTAR;
	public static EntityType<SelfPropelledHowitzer> HOWITZER;
	public static EntityType<FlakGun> FLAK_GUN;
	public static EntityType<ObservationBalloon> BALLOON;
	public static EntityType<ArtilleryShell> HEAVY_SHELL;
	public static EntityType<ArtilleryShell> FLAK_SHELL;
	public static Item SIEGE_MORTAR_ITEM;
	public static Item HOWITZER_ITEM;
	public static Item FLAK_GUN_ITEM;
	public static Item BALLOON_ITEM;
	public static Item HEAVY_SHELL_ITEM;
	public static Item FLAK_SHELL_ITEM;
	public static Item RANGE_FINDER;

	private JugcraftArtillery() {
	}

	public static void register() {
		SIEGE_MORTAR = entity("siege_mortar", EntityType.Builder.<SiegeMortar>of(SiegeMortar::new, MobCategory.MISC).sized(3.5F, 2.6F)
				.noLootTable().clientTrackingRange(10).updateInterval(2));
		HOWITZER = entity("self_propelled_howitzer", EntityType.Builder.<SelfPropelledHowitzer>of(SelfPropelledHowitzer::new, MobCategory.MISC)
				.sized(3.6F, 2.75F).noLootTable().clientTrackingRange(10).updateInterval(1));
		FLAK_GUN = entity("flak_gun", EntityType.Builder.<FlakGun>of(FlakGun::new, MobCategory.MISC).sized(2.0F, 1.8F)
				.noLootTable().clientTrackingRange(10).updateInterval(1));
		BALLOON = entity("observation_balloon", EntityType.Builder.<ObservationBalloon>of(ObservationBalloon::new, MobCategory.MISC)
				.sized(1.4F, 1.0F).noLootTable().clientTrackingRange(16).updateInterval(1));
		HEAVY_SHELL = entity("heavy_shell", EntityType.Builder.<ArtilleryShell>of(ArtilleryShell::new, MobCategory.MISC)
				.sized(0.4F, 0.4F).clientTrackingRange(16).updateInterval(1));
		FLAK_SHELL = entity("flak_shell", EntityType.Builder.<ArtilleryShell>of(ArtilleryShell::new, MobCategory.MISC)
				.sized(0.25F, 0.25F).clientTrackingRange(16).updateInterval(1));

		SIEGE_MORTAR_ITEM = placer("siege_mortar", () -> SIEGE_MORTAR);
		HOWITZER_ITEM = placer("self_propelled_howitzer", () -> HOWITZER);
		FLAK_GUN_ITEM = placer("flak_gun", () -> FLAK_GUN);
		BALLOON_ITEM = placer("observation_balloon", () -> BALLOON);
		HEAVY_SHELL_ITEM = described("heavy_shell", 16);
		FLAK_SHELL_ITEM = described("flak_shell", 64);
		ResourceKey<Item> finderKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("range_finder"));
		RANGE_FINDER = Registry.register(BuiltInRegistries.ITEM, finderKey, new RangeFinderItem(new Item.Properties().setId(finderKey).stacksTo(1)));

		PayloadTypeRegistry.serverboundPlay().register(ArtilleryInputPayload.TYPE, ArtilleryInputPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ArtilleryInputPayload.TYPE, (payload, context) -> {
			if (context.player().getVehicle() instanceof CrewedGun gun) {
				gun.steer(context.player(), payload.forward(), payload.turn(), payload.fire());
			}
		});
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> crewing(player) ? InteractionResult.FAIL
				: entity instanceof CrewedGun wired && player.getItemInHand(hand).is(io.github.jimbozoomer.jugcraft.building.FireControl.WIRE)
						? io.github.jimbozoomer.jugcraft.building.FireControl.link(player, wired)
				: entity instanceof CrewedGun gun ? gun.use(player, hand)
				: entity instanceof ObservationBalloon balloon ? balloon.use(player, hand) : InteractionResult.PASS);
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> crewing(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> crewing(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> crewing(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		UseItemCallback.EVENT.register((player, level, hand) -> crewing(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			for (Item item : new Item[] {SIEGE_MORTAR_ITEM, HOWITZER_ITEM, FLAK_GUN_ITEM, BALLOON_ITEM, HEAVY_SHELL_ITEM, FLAK_SHELL_ITEM, RANGE_FINDER}) {
				output.accept(item);
			}
		});
	}

	/** Whether the player is working a gun (its first rider). */
	public static boolean crewing(Player player) {
		return player.getVehicle() instanceof CrewedGun gun && gun.getFirstPassenger() == player;
	}

	private static <T extends Entity> EntityType<T> entity(String id, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id(id));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	private static Item placer(String id, Supplier<EntityType<? extends Entity>> type) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
		return Registry.register(BuiltInRegistries.ITEM, key,
				new PlaceEntityItem(new Item.Properties().setId(key).stacksTo(1), type, "tooltip.jugcraft." + id));
	}

	private static Item described(String id, int stack) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
		return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key).stacksTo(stack)) {
			@Override
			public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
					Consumer<Component> tooltip, TooltipFlag flag) {
				tooltip.accept(Component.translatable("tooltip.jugcraft." + id).withStyle(ChatFormatting.GRAY));
			}
		});
	}
}
