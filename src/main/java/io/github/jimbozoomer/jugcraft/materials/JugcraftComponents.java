package io.github.jimbozoomer.jugcraft.materials;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/**
 * Mechanical components: plates (metal press), wires (wire drawer), gears (crafted from four
 * plates) and circuits (circuit assembler), plus the ore-processing items: dusts (pulverizer),
 * washed ores (ore washer) and sawdust (sawmill). Keep these lists in sync with COMPONENTS,
 * CIRCUITS and WASHED_ORES in tools/materials.py; the checker compares them.
 */
public final class JugcraftComponents {
	public static final String[] PLATES = {"copper", "iron", "tin", "bronze", "brass", "invar", "aluminum", "nickel", "lead", "tungsten", "steel", "titanium"};
	public static final String[] GEARS = {"iron", "bronze", "brass", "invar", "steel"};
	public static final String[] WIRES = {"copper", "silver", "aluminum"};
	public static final String[] CIRCUITS = {"basic_circuit", "advanced_circuit", "processor"};
	public static final String[] DUSTS = {"copper", "iron", "gold", "tin", "zinc", "lead", "silver", "nickel", "tungsten", "uranium"};
	public static final String[] WASHED_ORES = {"copper", "iron", "gold", "tin", "zinc", "lead", "silver", "nickel", "tungsten", "uranium"};

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
		for (String metal : DUSTS) {
			items.add(JugcraftRegistry.item(metal + "_dust"));
		}
		for (String metal : WASHED_ORES) {
			items.add(JugcraftRegistry.item("washed_" + metal + "_ore"));
		}
		items.add(JugcraftRegistry.item("sawdust"));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> items.forEach(output::accept));
	}
}
