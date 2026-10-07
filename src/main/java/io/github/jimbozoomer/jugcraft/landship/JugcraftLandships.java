package io.github.jimbozoomer.jugcraft.landship;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.function.Consumer;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Batch 49: the Landship (docs/features/landship.md). While a player drives one, their own attack and use are
 * switched off (on both sides): those buttons fire the landship's cannon and side guns instead.
 */
public final class JugcraftLandships {
	public static EntityType<Landship> LANDSHIP;
	public static EntityType<LandshipShell> SHELL;
	public static Item LANDSHIP_ITEM;
	public static Item CANNON_SHELL;

	private JugcraftLandships() {
	}

	public static void register() {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id("landship"));
		LANDSHIP = Registry.register(BuiltInRegistries.ENTITY_TYPE, key, EntityType.Builder
				.<Landship>of(Landship::new, MobCategory.MISC).sized(Landship.WIDTH, Landship.HEIGHT).noLootTable()
				.clientTrackingRange(10).updateInterval(1).build(key));
		ResourceKey<EntityType<?>> shellKey = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id("cannon_shell"));
		SHELL = Registry.register(BuiltInRegistries.ENTITY_TYPE, shellKey, EntityType.Builder
				.<LandshipShell>of(LandshipShell::new, MobCategory.MISC).sized(0.3F, 0.3F)
				.clientTrackingRange(8).updateInterval(2).build(shellKey));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("landship"));
		LANDSHIP_ITEM = Registry.register(BuiltInRegistries.ITEM, itemKey,
				new LandshipItem(new Item.Properties().setId(itemKey).stacksTo(1)));
		ResourceKey<Item> shellItemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("cannon_shell"));
		CANNON_SHELL = Registry.register(BuiltInRegistries.ITEM, shellItemKey, new Item(new Item.Properties().setId(shellItemKey)) {
			@Override
			public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
					Consumer<Component> tooltip, TooltipFlag flag) {
				tooltip.accept(Component.translatable("tooltip.jugcraft.cannon_shell").withStyle(ChatFormatting.GRAY));
			}
		});

		PayloadTypeRegistry.serverboundPlay().register(LandshipInputPayload.TYPE, LandshipInputPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(LandshipInputPayload.TYPE, (payload, context) -> {
			if (context.player().getVehicle() instanceof Landship landship) {
				landship.steer(context.player(), payload.forward(), payload.turn(), payload.cannon(), payload.guns());
			}
		});
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> driving(player) ? InteractionResult.FAIL
				: entity instanceof Landship landship ? landship.use(player, hand) : InteractionResult.PASS);
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> driving(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> driving(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> driving(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		UseItemCallback.EVENT.register((player, level, hand) -> driving(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.accept(LANDSHIP_ITEM));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> output.accept(CANNON_SHELL));
	}

	/** Whether the player is at the controls of a landship (its first rider). */
	public static boolean driving(Player player) {
		return player.getVehicle() instanceof Landship landship && landship.getFirstPassenger() == player;
	}
}
