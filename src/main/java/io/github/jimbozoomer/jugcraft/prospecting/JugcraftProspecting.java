package io.github.jimbozoomer.jugcraft.prospecting;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/** Prospecting: the Geo-Resonance Prospector and its survey packet. (The ore drill is a MachineKind.) */
public final class JugcraftProspecting {
	public static Item PROSPECTOR;

	private JugcraftProspecting() {
	}

	public static void register() {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id("prospector"));
		PROSPECTOR = Registry.register(BuiltInRegistries.ITEM, key, new ProspectorItem(new Item.Properties().setId(key).stacksTo(1)));
		PayloadTypeRegistry.clientboundPlay().register(SurveyPayload.TYPE, SurveyPayload.CODEC);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.accept(PROSPECTOR));
	}
}
