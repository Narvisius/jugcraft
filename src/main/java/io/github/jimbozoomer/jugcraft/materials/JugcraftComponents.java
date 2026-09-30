package io.github.jimbozoomer.jugcraft.materials;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/**
 * Mechanical components: plates (metal press), wires (wire drawer), gears (crafted from four
 * plates) and circuits (circuit assembler). Keep these lists in sync with COMPONENTS and
 * CIRCUITS in tools/materials.py; the checker compares them.
 */
public final class JugcraftComponents {
	public static final String[] PLATES = {"copper", "iron", "tin", "bronze", "brass", "invar", "aluminum", "nickel", "lead", "tungsten"};
	public static final String[] GEARS = {"iron", "bronze", "brass", "invar"};
	public static final String[] WIRES = {"copper", "silver", "aluminum"};
	public static final String[] CIRCUITS = {"basic_circuit", "advanced_circuit"};

	private JugcraftComponents() {
	}

	public static void register() {
		List<Item> items = new ArrayList<>();
		for (String metal : PLATES) {
			items.add(JugcraftRegistry.item(metal + "_plate"));
		}
		for (String metal : GEARS) {
			items.add(JugcraftRegistry.item(metal + "_gear"));
		}
		for (String metal : WIRES) {
			items.add(JugcraftRegistry.item(metal + "_wire"));
		}
		for (String circuit : CIRCUITS) {
			items.add(JugcraftRegistry.item(circuit));
		}
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> items.forEach(output::accept));
	}
}
