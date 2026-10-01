package io.github.jimbozoomer.jugcraft.chemistry;

import io.github.jimbozoomer.jugcraft.materials.JugcraftRegistry;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/** Plain items of the oil line (ITEMS in tools/petro.py). */
public final class PetroItems {
	/** Used up by the catalytic cracker, one per bucket of heavy fuel oil. */
	public static Item CRACKING_CATALYST;
	/** The residue of vacuum distillation: binds gravel into asphalt. */
	public static Item ASPHALT_BINDER;
	/** Polymerized refinery gas: the metal press flattens each into a plastic sheet. */
	public static Item PLASTIC_PELLETS;
	/** Pressed plastic, for parts of later machines (first used in the diesel engine). */
	public static Item PLASTIC_SHEET;
	/** Bauxite digested in hot lye (the Bayer process): smelted into aluminum in the electrolytic cell. */
	public static Item ALUMINA;
	/** Superphosphate: ripens the crops in a 5x5 area ({@link FertilizerItem}). */
	public static Item FERTILIZER;
	/** The Kroll process: raw titanium chlorinated with coke and reduced, a porous sponge the arc furnace melts. */
	public static Item TITANIUM_SPONGE;
	/** A lithium cell in an aluminum can: crafted from lithium carbonate, built into the lithium battery bank. */
	public static Item LITHIUM_CELL;
	/** Rare earths alloyed with iron in the alloy smelter: for the magnet dynamo and magnet motor. */
	public static Item NEODYMIUM_MAGNET;

	private PetroItems() {
	}

	public static void register() {
		CRACKING_CATALYST = JugcraftRegistry.item("cracking_catalyst");
		ASPHALT_BINDER = JugcraftRegistry.item("asphalt_binder");
		PLASTIC_PELLETS = JugcraftRegistry.item("plastic_pellets");
		PLASTIC_SHEET = JugcraftRegistry.item("plastic_sheet");
		ALUMINA = JugcraftRegistry.item("alumina");
		FERTILIZER = JugcraftRegistry.item("fertilizer", FertilizerItem::new);
		TITANIUM_SPONGE = JugcraftRegistry.item("titanium_sponge");
		LITHIUM_CELL = JugcraftRegistry.item("lithium_cell");
		NEODYMIUM_MAGNET = JugcraftRegistry.item("neodymium_magnet");

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
			output.accept(CRACKING_CATALYST);
			output.accept(ASPHALT_BINDER);
			output.accept(PLASTIC_PELLETS);
			output.accept(PLASTIC_SHEET);
			output.accept(ALUMINA);
			output.accept(FERTILIZER);
			output.accept(TITANIUM_SPONGE);
			output.accept(LITHIUM_CELL);
			output.accept(NEODYMIUM_MAGNET);
		});
	}
}
