package io.github.jimbozoomer.jugcraft.chemistry;

import io.github.jimbozoomer.jugcraft.materials.JugcraftRegistry;
import io.github.jimbozoomer.jugcraft.weapons.GrenadeItem;
import io.github.jimbozoomer.jugcraft.weapons.GrenadeLauncherItem;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/** Plain items of the oil line (ITEMS in tools/petro.py). */
public final class PetroItems {
	/** Used up by the catalytic cracker, one per bucket of heavy fuel oil. */
	public static Item CRACKING_CATALYST;
	/** The residue of vacuum distillation (in the distillation tower): binds gravel into asphalt. */
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
	/** Electronics (batch 7): a doped silicon crystal pulled in the arc furnace (batch 24), sawn into wafers. */
	public static Item SILICON_BOULE;
	public static Item SILICON_WAFER;
	/** Wafers etched in the lithography station: four chips each. */
	public static Item MICROCHIP;
	public static Item RUBBER;
	public static Item GASKET;
	public static Item PVC_RESIN;
	public static Item SOAP;
	/** Explosive weapons (batch 18): nitrated cotton, the grenade's charge; grenades and their launcher. */
	public static Item GUNCOTTON;
	public static Item GRENADE;
	public static Item GRENADE_LAUNCHER;
	/** Power (batch 19): fitted in the advanced engine's slot for half as much power again. */
	public static Item TURBOCHARGER;
	/** The Chemical Infuser's reusable methanation catalyst: held in its bed socket, never used up. */
	public static Item NICKEL_CATALYST_BED;
	public static Item VANADIUM_PENTOXIDE;
	public static Item GASIFICATION_RESIDUE;
	public static Item AMMONIUM_NITRATE;
	/** Bone meal doses ammonium nitrate gives each crop. */
	public static final int AMMONIUM_NITRATE_DOSES = 3;
	public static Item VANADIUM_CONTACT_BED;

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
		SILICON_BOULE = JugcraftRegistry.item("silicon_boule");
		SILICON_WAFER = JugcraftRegistry.item("silicon_wafer");
		MICROCHIP = JugcraftRegistry.item("microchip");
		RUBBER = JugcraftRegistry.item("rubber");
		GASKET = JugcraftRegistry.item("gasket");
		PVC_RESIN = JugcraftRegistry.item("pvc_resin");
		SOAP = JugcraftRegistry.item("soap", SoapItem::new);
		GUNCOTTON = JugcraftRegistry.item("guncotton");
		GRENADE = JugcraftRegistry.item("grenade", GrenadeItem::new);
		GRENADE_LAUNCHER = JugcraftRegistry.item("grenade_launcher", GrenadeLauncherItem::new);
		TURBOCHARGER = JugcraftRegistry.item("turbocharger", properties -> new Item(properties.stacksTo(1)));
		NICKEL_CATALYST_BED = JugcraftRegistry.item("nickel_catalyst_bed", properties -> new Item(properties.stacksTo(1)));
		VANADIUM_PENTOXIDE = JugcraftRegistry.item("vanadium_pentoxide");
		VANADIUM_CONTACT_BED = JugcraftRegistry.item("vanadium_contact_bed", properties -> new Item(properties.stacksTo(1)));
		GASIFICATION_RESIDUE = JugcraftRegistry.item("gasification_residue");
		// A nitrogen fertilizer: superphosphate's 5x5 area, three doses a crop instead of two.
		AMMONIUM_NITRATE = JugcraftRegistry.item("ammonium_nitrate",
				properties -> new FertilizerItem(properties, FertilizerItem.RADIUS, AMMONIUM_NITRATE_DOSES));

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
			output.accept(SILICON_BOULE);
			output.accept(SILICON_WAFER);
			output.accept(MICROCHIP);
			output.accept(GUNCOTTON);
			output.accept(TURBOCHARGER);
			output.accept(NICKEL_CATALYST_BED);
			output.accept(VANADIUM_PENTOXIDE);
			output.accept(GASIFICATION_RESIDUE);
			output.accept(AMMONIUM_NITRATE);
			output.accept(VANADIUM_CONTACT_BED);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			output.accept(GRENADE);
			output.accept(GRENADE_LAUNCHER);
		});
	}
}
