package io.github.jimbozoomer.jugcraft.train;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/**
 * The train framework (docs/features/trains.md): the diesel locomotive, the passenger coach and the cargo wagon, which
 * run on any rails, and the Coupler that makes them a train. Using a car rides it (or opens the wagon); with the
 * Coupler in hand it couples or uncouples instead.
 */
public final class JugcraftTrains {
	public static EntityType<Locomotive> LOCOMOTIVE;
	public static EntityType<PassengerCar> PASSENGER_CAR;
	public static EntityType<CargoWagon> CARGO_WAGON;
	public static Item LOCOMOTIVE_ITEM;
	public static Item PASSENGER_CAR_ITEM;
	public static Item CARGO_WAGON_ITEM;
	public static Item COUPLER;

	private JugcraftTrains() {
	}

	public static void register() {
		LOCOMOTIVE = entity("locomotive", Locomotive::new, Locomotive.WIDTH, Locomotive.HEIGHT);
		PASSENGER_CAR = entity("passenger_car", PassengerCar::new, PassengerCar.WIDTH, PassengerCar.HEIGHT);
		CARGO_WAGON = entity("cargo_wagon", CargoWagon::new, CargoWagon.WIDTH, CargoWagon.HEIGHT);
		LOCOMOTIVE_ITEM = item("locomotive", properties -> new TrainItem(properties.stacksTo(1), level -> new Locomotive(LOCOMOTIVE, level),
				"tooltip.jugcraft.locomotive"));
		PASSENGER_CAR_ITEM = item("passenger_car", properties -> new TrainItem(properties.stacksTo(1),
				level -> new PassengerCar(PASSENGER_CAR, level), "tooltip.jugcraft.passenger_car"));
		CARGO_WAGON_ITEM = item("cargo_wagon", properties -> new TrainItem(properties.stacksTo(1), level -> new CargoWagon(CARGO_WAGON, level),
				"tooltip.jugcraft.cargo_wagon"));
		COUPLER = item("coupler", properties -> new CouplerItem(properties.stacksTo(1)));

		PayloadTypeRegistry.serverboundPlay().register(TrainInputPayload.TYPE, TrainInputPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TrainInputPayload.TYPE, (payload, context) -> {
			if (context.player().getVehicle() instanceof Locomotive locomotive) {
				locomotive.steer(context.player(), payload.throttle(), payload.brake());
			}
		});
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (!(entity instanceof TrainCar car)) {
				return InteractionResult.PASS;
			}
			return player.getItemInHand(hand).is(COUPLER) ? CouplerItem.use(player, car) : car.use(player, hand);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			output.accept(LOCOMOTIVE_ITEM);
			output.accept(PASSENGER_CAR_ITEM);
			output.accept(CARGO_WAGON_ITEM);
			output.accept(COUPLER);
		});
	}

	/** Whether the player is at a locomotive's controls (its first rider). */
	public static boolean driving(net.minecraft.world.entity.player.Player player) {
		return player.getVehicle() instanceof Locomotive locomotive && locomotive.getFirstPassenger() == player;
	}

	private static <T extends TrainCar> EntityType<T> entity(String name, EntityType.EntityFactory<T> factory, float width, float height) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, EntityType.Builder.of(factory, MobCategory.MISC).sized(width, height)
				.noLootTable().clientTrackingRange(10).updateInterval(1).build(key));
	}

	private static Item item(String name, Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
	}
}
