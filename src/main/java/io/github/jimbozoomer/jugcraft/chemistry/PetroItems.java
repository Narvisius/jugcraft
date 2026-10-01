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

	private PetroItems() {
	}

	public static void register() {
		CRACKING_CATALYST = JugcraftRegistry.item("cracking_catalyst");
		ASPHALT_BINDER = JugcraftRegistry.item("asphalt_binder");
		PLASTIC_PELLETS = JugcraftRegistry.item("plastic_pellets");
		PLASTIC_SHEET = JugcraftRegistry.item("plastic_sheet");

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
			output.accept(CRACKING_CATALYST);
			output.accept(ASPHALT_BINDER);
			output.accept(PLASTIC_PELLETS);
			output.accept(PLASTIC_SHEET);
		});
	}
}
