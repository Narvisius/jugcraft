package io.github.jimbozoomer.jugcraft.airship;

import io.github.jimbozoomer.jugcraft.Jugcraft;
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

/** Batch 46: the zeppelin (docs/features/zeppelin.md). */
public final class JugcraftAirships {
	public static EntityType<Zeppelin> ZEPPELIN;
	public static Item ZEPPELIN_ITEM;

	private JugcraftAirships() {
	}

	public static void register() {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id("zeppelin"));
		ZEPPELIN = Registry.register(BuiltInRegistries.ENTITY_TYPE, key, EntityType.Builder
				.<Zeppelin>of(Zeppelin::new, MobCategory.MISC).sized(Zeppelin.WIDTH, Zeppelin.HEIGHT).noLootTable()
				.clientTrackingRange(10).updateInterval(1).build(key));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("zeppelin"));
		ZEPPELIN_ITEM = Registry.register(BuiltInRegistries.ITEM, itemKey,
				new ZeppelinItem(new Item.Properties().setId(itemKey).stacksTo(1)));

		PayloadTypeRegistry.serverboundPlay().register(ZeppelinInputPayload.TYPE, ZeppelinInputPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ZeppelinInputPayload.TYPE, (payload, context) -> {
			if (context.player().getVehicle() instanceof Zeppelin zeppelin) {
				zeppelin.steer(context.player(), payload.forward(), payload.turn(), payload.vertical());
			}
		});
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
				entity instanceof Zeppelin zeppelin ? zeppelin.use(player, hand) : InteractionResult.PASS);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.accept(ZEPPELIN_ITEM));
	}
}
