package io.github.jimbozoomer.jugcraft.guide;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.Encyclopedia;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/** The in-game guides: the Engineer's Handbook item, and the Jugcraft Encyclopedia's server side. */
public final class JugcraftGuide {
	public static Item ENGINEERS_HANDBOOK;

	private JugcraftGuide() {
	}

	public static void register() {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id("engineers_handbook"));
		ENGINEERS_HANDBOOK = Registry.register(BuiltInRegistries.ITEM, key,
				new EngineersHandbookItem(new Item.Properties().setId(key).stacksTo(1)));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.accept(ENGINEERS_HANDBOOK));
		Encyclopedia.register();
	}
}
