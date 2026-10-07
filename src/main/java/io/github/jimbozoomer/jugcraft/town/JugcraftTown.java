package io.github.jimbozoomer.jugcraft.town;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

/**
 * The walled town (docs/features/walled-town.md): a medieval town built near the start of every new world, kept as it
 * was built, with townsfolk who change its decor with the seasons and keep its shops, and Jugs, the town's credit.
 * {@code town.enabled=false} stops new towns; a town already built stays, with its protection
 * ({@code town.protection=off} lifts that), townsfolk and shops. Everything stays registered either way.
 */
public final class JugcraftTown {
	public static final String FEATURE = "town";

	public static Block ATM;
	public static EntityType<Townsfolk> TOWNSFOLK;
	public static ExtendedMenuType<ShopMenu, ShopMenu.Opening> SHOP_MENU;
	public static ExtendedMenuType<AtmMenu, AtmMenu.Opening> ATM_MENU;

	private JugcraftTown() {
	}

	public static void register() {
		ResourceKey<Block> atmKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("atm"));
		ATM = Registry.register(BuiltInRegistries.BLOCK, atmKey, new AtmBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
				.strength(5.0F, 1200.0F).noOcclusion().pushReaction(PushReaction.IMMOVEABLE).setId(atmKey)));
		ResourceKey<Item> atmItem = ResourceKey.create(Registries.ITEM, Jugcraft.id("atm"));
		Registry.register(BuiltInRegistries.ITEM, atmItem, new BlockItem(ATM, new Item.Properties().setId(atmItem).useBlockDescriptionPrefix()));

		ResourceKey<EntityType<?>> townsfolkKey = ResourceKey.create(Registries.ENTITY_TYPE, Jugcraft.id("townsfolk"));
		TOWNSFOLK = Registry.register(BuiltInRegistries.ENTITY_TYPE, townsfolkKey, EntityType.Builder.<Townsfolk>of(Townsfolk::new, MobCategory.MISC)
				.sized(0.6F, 1.8F).eyeHeight(1.62F).fireImmune().clientTrackingRange(10).build(townsfolkKey));
		FabricDefaultAttributeRegistry.register(TOWNSFOLK, Townsfolk.createAttributes());

		SHOP_MENU = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("town_shop"), new ExtendedMenuType<>(ShopMenu::new, ShopMenu.Opening.STREAM_CODEC));
		ATM_MENU = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("atm"), new ExtendedMenuType<>(AtmMenu::new, AtmMenu.Opening.STREAM_CODEC));

		TownProtection.register();
		TownBuilder.register();
		TownPlanner.register();
		TownDecor.register();
		TownsfolkCare.register();
		TownCommand.register();
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			ShopMenu.clear();
			AtmMenu.clear();
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(ATM));
	}
}
