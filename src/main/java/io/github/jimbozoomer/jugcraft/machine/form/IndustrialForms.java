package io.github.jimbozoomer.jugcraft.machine.form;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FluidFuels;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The industrial machine forms the game registers, from the owner's factory plan (docs/features/
 * industrial-machine-foundation.md): package 2's Electrolytic Separator (docs/features/industrial-electrolytic-separator.md),
 * Gas Burning Generator (docs/features/industrial-gas-burning-generator.md), Chemical Infuser
 * (docs/features/industrial-chemical-infuser.md), Chemical Oxidizer (docs/features/industrial-chemical-oxidizer.md),
 * Industrial Fermenter (docs/features/industrial-fermenter.md), Bulk Chemical Reactor
 * (docs/features/industrial-bulk-chemical-reactor.md), Anaerobic Digester (docs/features/industrial-anaerobic-digester.md),
 * Coal Gasifier and Gas Cleanup and Separation Rack (docs/features/industrial-coal-gasification.md), and Bio
 * Generator (docs/features/industrial-bio-generator.md).
 * Each form is also described in
 * tools/industrial_forms.py for its art, recipes and data; tools/check_mod_data.py keeps the two the same. Every form
 * shares the one controller block entity type, {@code jugcraft:machine_form}, so a saved machine keeps its type when
 * more forms join this list.
 */
public final class IndustrialForms {
	/** Splitting water and ordinary brine. */
	public static final String AQUEOUS_ELECTROLYSIS = "jugcraft:aqueous_electrolysis";
	/** Joining two gases with no catalyst bed, such as hydrogen burnt in chlorine. */
	public static final String GAS_COMBINATION = "jugcraft:gas_combination";
	/** Burning a solid feed in a metered oxidant gas, such as sulfur in oxygen. */
	public static final String SOLID_OXIDATION = "jugcraft:solid_oxidation";
	/** Fermenting crops into alcohol in a closed, stirred, heated vessel, its carbon dioxide collected. */
	public static final String CONTROLLED_FERMENTATION = "jugcraft:controlled_fermentation";
	/** Dissolving a gas in a liquid, such as hydrogen chloride in water into hydrochloric acid. */
	public static final String ACID_ABSORPTION = "jugcraft:acid_absorption";
	/** Leaching ores in hydrochloric acid. */
	public static final String CHLORIDE_LEACHING = "jugcraft:chloride_leaching";
	/** Digesting organic waste in a sealed, stirred tank into raw biogas. */
	public static final String ANAEROBIC_DIGESTION = "jugcraft:anaerobic_digestion";
	/** Gasifying a carbon feed with steam into a dirty carbon monoxide and hydrogen mixture. */
	public static final String COAL_GASIFICATION = "jugcraft:coal_gasification";
	/** Cleaning and dividing a gas mixture into its gases. */
	public static final String GAS_SEPARATION = "jugcraft:gas_separation";
	/** Oxidizing sulfur dioxide into sulfur trioxide over the vanadium contact bed. */
	public static final String CONTACT_CONVERSION = "jugcraft:contact_conversion";
	/** Roasting vanadium oxide out of washed iron ore with lye. */
	public static final String VANADIUM_RECOVERY = "jugcraft:vanadium_recovery";
	/** Neutralizing nitric acid with ammonia into solid ammonium nitrate. */
	public static final String NITRATE_NEUTRALIZATION = "jugcraft:nitrate_neutralization";
	/** Recovering ruthenium from washed nickel ore with hydrochloric acid and chlorine. */
	public static final String PRECIOUS_METAL_RECOVERY = "jugcraft:precious_metal_recovery";
	/** Joining a carbon oxide and hydrogen into methane over a nickel bed. */
	public static final String NICKEL_METHANATION = "jugcraft:nickel_methanation";
	/** What the Infuser's bed socket holds: the nickel catalyst bed and the vanadium contact bed. */
	public static final TagKey<Item> CATALYST_BEDS = TagKey.create(Registries.ITEM, Jugcraft.id("catalyst_beds"));

	/**
	 * The Electrolytic Separator: the steel-entry form of the Electrolytic Cell family, two wide, two deep and three
	 * tall, every position a block, the controller (its control box) front left on the ground. Water or brine comes in
	 * at the back; oxygen or chlorine, hydrogen and lye leave at the front, with the power socket at the foot of the
	 * right tower. Water leaves the lye tank unused; a full lye tank stops brine until it is emptied.
	 */
	public static final MachineForm ELECTROLYTIC_SEPARATOR_FORM = MachineForm.builder(Jugcraft.id("electrolytic_separator"), MachineKind.ELECTROLYTIC_CELL)
			.layer("C#", "##")
			.layer("##", "##")
			.layer("##", "##")
			.profile(OperatingProfile.ENTRY)
			.inputTank("feed")
			.outputTank("anode_gas")
			.outputTank("hydrogen")
			.outputTank("lye")
			.capability("jugcraft:aqueous_electrolysis")
			.capability("jugcraft:electronics_peroxide")
			.companionAssistance()
			.upgrades()
			.warmup(40)
			.port("feed_in", FormPort.Kind.FLUID_IN, 0, 0, 1, 0, FormSide.BACK)
			.port("anode_gas_out", FormPort.Kind.FLUID_OUT, 0, 1, 0, 2, FormSide.FRONT)
			.port("hydrogen_out", FormPort.Kind.FLUID_OUT, 1, 0, 0, 2, FormSide.FRONT)
			.port("lye_out", FormPort.Kind.FLUID_OUT, 2, 1, 0, 1, FormSide.FRONT)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 1, 0, 0, FormSide.FRONT)
			.build();

	/**
	 * The Gas Burning Generator: the shared hydrogen and methane generator of the starter factory, a form of the
	 * combustion generators' family (the Gas Turbine's energy figures), four wide, five deep and four tall. The skid fills
	 * the ground layer, the engine-generator train the middle columns, the generator housing and the terminal cabinet
	 * the back rows, and the cabinet's top two blocks of the fourth layer; the corners above the skid at the front are
	 * open. The controller is the ignition box at the front left. Fuel comes in low on the right side; power leaves at
	 * the cabinet's socket at the back. It burns only its own fuels, never the turbine's.
	 */
	public static final MachineForm GAS_BURNING_GENERATOR_FORM = MachineForm.builder(Jugcraft.id("gas_burning_generator"), MachineKind.GAS_TURBINE)
			.layer("C###", "####", "####", "####", "####")
			.layer(".##.", ".##.", ".##.", "####", "####")
			.layer(".##.", ".##.", ".##.", "####", "####")
			.layer("....", "....", "....", "....", "..##")
			.profile(OperatingProfile.ENTRY)
			.inputTank("fuel")
			.fuel(Jugcraft.id("hydrogen"), 128, 128)
			.fuel(Jugcraft.id("methane"), 448, 256)
			.port("fuel_in", FormPort.Kind.FLUID_IN, 0, 3, 1, 0, FormSide.RIGHT)
			.port("power_out", FormPort.Kind.ENERGY_OUT, 0, 2, 4, 1, FormSide.BACK)
			.build();

	/**
	 * The Chemical Infuser: the starter form of the synthesis family (the Synthesis Converter's energy figures), two
	 * wide, three deep and three tall, every position a block, the controller (its control box) front left on the
	 * ground. Hydrogen comes in at the left shoulder and the other gas at the right; the product gas and the water a
	 * reaction gives off leave at the back, by the cooling jacket, with the power socket low on the right at the back.
	 * Hydrogen chloride needs no bed; methanation needs the nickel catalyst bed in its bed socket, never used up.
	 */
	public static final MachineForm CHEMICAL_INFUSER_FORM = MachineForm.builder(Jugcraft.id("chemical_infuser"), MachineKind.SYNTHESIS_CONVERTER)
			.layer("C#", "##", "##")
			.layer("##", "##", "##")
			.layer("##", "##", "##")
			.profile(OperatingProfile.ENTRY)
			.inputTank("hydrogen")
			.inputTank("reactant")
			.outputTank("product")
			.outputTank("water")
			.socket("bed", TagKey.create(Registries.ITEM, Jugcraft.id("catalyst_beds")))
			.capability("jugcraft:gas_combination")
			.capability("jugcraft:nickel_methanation")
			.capability("jugcraft:contact_conversion")
			.capability("jugcraft:starter_gas_infusing")
			.capability("jugcraft:cobalt_synthesis")
			.capability("jugcraft:syngas_balancing")
			.alias("reagent_a", "hydrogen")
			.alias("reagent_b", "reactant")
			.alias("product_gas", "product")
			.alias("process_water", "water")
			.upgrades()
			.warmup(40)
			.waterDrainOutput(1)
			.port("hydrogen_in", FormPort.Kind.FLUID_IN, 0, 0, 1, 1, FormSide.LEFT)
			.port("reactant_in", FormPort.Kind.FLUID_IN, 1, 1, 1, 1, FormSide.RIGHT)
			.port("product_out", FormPort.Kind.FLUID_OUT, 0, 1, 2, 1, FormSide.BACK)
			.port("water_out", FormPort.Kind.FLUID_OUT, 1, 0, 2, 0, FormSide.BACK)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 1, 2, 0, FormSide.RIGHT)
			.build();

	/**
	 * The Chemical Oxidizer: the starter solid-feed oxidation form of the chemical reactor family (the Chemical Reactor's
	 * energy figures), two wide, two deep and three tall, every position a block, the controller (its control panel)
	 * front left on the ground. Sulfur dust goes into the hopper on top at the back and a feed screw carries it into the
	 * lined reaction throat; oxygen comes in low on the right; the gas leaves at the top of the collector hood, and the
	 * power socket is low on the left side at the back.
	 */
	public static final MachineForm CHEMICAL_OXIDIZER_FORM = MachineForm.builder(Jugcraft.id("chemical_oxidizer"), MachineKind.CHEMICAL_REACTOR)
			.layer("C#", "##")
			.layer("##", "##")
			.layer("##", "##")
			.profile(OperatingProfile.ENTRY)
			.inputTank("oxidant")
			.outputTank("product")
			.items(1, 0)
			.capability("jugcraft:solid_oxidation")
			.capability("jugcraft:starter_gas_oxidizing")
			.alias("product_gas", "product")
			.upgrades()
			.warmup(40)
			.port("feed_in", FormPort.Kind.ITEM_IN, 0, 0, 1, 2, FormSide.TOP)
			.port("oxidant_in", FormPort.Kind.FLUID_IN, 0, 1, 0, 0, FormSide.RIGHT)
			.port("product_out", FormPort.Kind.FLUID_OUT, 0, 1, 0, 2, FormSide.TOP)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 1, 0, FormSide.LEFT)
			.build();

	/**
	 * The Industrial Fermenter: a controlled fermentation form of the chemical reactor family (the Chemical Reactor's
	 * energy figures), three wide, three deep and four tall, every position a block: a tall stepped vessel on four legs,
	 * the controller (its instrument box) front left on the ground. Crops go in at the hatch on the crown and water low at
	 * the back left; the alcohol leaves at the broth outlet low at the front and the carbon dioxide at the upper flange at
	 * the back. The carbon dioxide tank vents what it cannot hold, so fermentation never waits on it (the owner's choice).
	 */
	public static final MachineForm INDUSTRIAL_FERMENTER_FORM = MachineForm.builder(Jugcraft.id("industrial_fermenter"), MachineKind.CHEMICAL_REACTOR)
			.layer("C##", "###", "###")
			.layer("###", "###", "###")
			.layer("###", "###", "###")
			.layer("###", "###", "###")
			.profile(OperatingProfile.ENTRY)
			.inputTank("water")
			.outputTank("product")
			.outputTank("co2")
			.vent("co2")
			.items(1, 0)
			.capability("jugcraft:controlled_fermentation")
			.alias("crop_mash", "water")
			.alias("ethanol_broth", "product")
			.alias("fermentation_exhaust", "co2")
			.upgrades()
			.warmup(40)
			.port("feed_in", FormPort.Kind.ITEM_IN, 0, 0, 1, 3, FormSide.TOP)
			.port("water_in", FormPort.Kind.FLUID_IN, 0, 0, 2, 1, FormSide.BACK)
			.port("product_out", FormPort.Kind.FLUID_OUT, 0, 1, 0, 0, FormSide.FRONT)
			.port("co2_out", FormPort.Kind.FLUID_OUT, 1, 1, 2, 3, FormSide.BACK)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 2, 2, 0, FormSide.BACK)
			.build();

	/**
	 * The Bulk Fermenter: the art brief's 5x5x6 variant of the Industrial Fermenter, the same vessel enlarged and the same
	 * controlled fermentation in the bulk profile, four batches at once from 16,000 mB tanks at 80% of the draw a batch.
	 * Its carbon dioxide tank vents what it cannot hold, like the smaller one's.
	 */
	public static final MachineForm BULK_FERMENTER_FORM = MachineForm.builder(Jugcraft.id("bulk_fermenter"), MachineKind.CHEMICAL_REACTOR)
			.layer("C####", "#####", "#####", "#####", "#####")
			.layer("#####", "#####", "#####", "#####", "#####")
			.layer("#####", "#####", "#####", "#####", "#####")
			.layer("#####", "#####", "#####", "#####", "#####")
			.layer("#####", "#####", "#####", "#####", "#####")
			.layer("#####", "#####", "#####", "#####", "#####")
			.profile(OperatingProfile.BULK)
			.inputTank("water")
			.outputTank("product")
			.outputTank("co2")
			.vent("co2")
			.items(1, 0)
			.capability(CONTROLLED_FERMENTATION)
			.upgrades()
			.warmup(40)
			.port("feed_in", FormPort.Kind.ITEM_IN, 0, 0, 2, 5, FormSide.TOP)
			.port("water_in", FormPort.Kind.FLUID_IN, 0, 0, 4, 1, FormSide.BACK)
			.port("product_out", FormPort.Kind.FLUID_OUT, 0, 2, 0, 0, FormSide.FRONT)
			.port("co2_out", FormPort.Kind.FLUID_OUT, 1, 2, 4, 4, FormSide.BACK)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 4, 4, 0, FormSide.BACK)
			.alias("crop_mash", "water")
			.alias("ethanol_broth", "product")
			.alias("fermentation_exhaust", "co2")

			.build();

	/**
	 * The Bulk Chemical Reactor: the Chemical Reactor enlarged (the chemical reactor family's energy figures) into a
	 * four-block cube in the expanded profile, two batches at once from 8,000 mB tanks. Water or acid goes in at the side,
	 * gas at the short absorption inlet on the lid and ore at the hatch beside it; acid leaves at the outlet low at the
	 * front and washed ore at the chute beside it.
	 */
	public static final MachineForm BULK_CHEMICAL_REACTOR_FORM = MachineForm.builder(Jugcraft.id("bulk_chemical_reactor"), MachineKind.CHEMICAL_REACTOR)
			.layer("C###", "####", "####", "####")
			.layer("####", "####", "####", "####")
			.layer("####", "####", "####", "####")
			.layer("####", "####", "####", "####")
			.profile(OperatingProfile.EXPANDED)
			.inputTank("liquid")
			.inputTank("gas")
			.outputTank("product")
			.items(1, 1)
			.capability("jugcraft:acid_absorption")
			.capability("jugcraft:chloride_leaching")
			.capability("jugcraft:vanadium_recovery")
			.capability("jugcraft:nitrate_neutralization")
			.capability("jugcraft:precious_metal_recovery")
			.upgrades()
			.warmup(40)
			.port("feed_in", FormPort.Kind.ITEM_IN, 0, 1, 1, 3, FormSide.TOP)
			.port("liquid_in", FormPort.Kind.FLUID_IN, 0, 0, 2, 0, FormSide.LEFT)
			.port("gas_in", FormPort.Kind.FLUID_IN, 1, 2, 2, 3, FormSide.TOP)
			.port("product_out", FormPort.Kind.FLUID_OUT, 0, 2, 0, 0, FormSide.FRONT)
			.port("items_out", FormPort.Kind.ITEM_OUT, FormPort.ALL, 1, 0, 0, FormSide.FRONT)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 3, 3, 0, FormSide.RIGHT)
			.capability("jugcraft:battery_bulk_chemical_reactor")
            .capability("jugcraft:electronics_reagents")
			.capability("jugcraft:phosphate_formulation")
			.capability("jugcraft:storable_propellants")
			.build();

	/**
	 * The Anaerobic Digester: the owner's selected substantial first bulk digester, a six-block cube of the chemical
	 * reactor family (its energy figures) in the bulk profile, four batches at once from 16,000 mB tanks. Waste goes in at
	 * the feed auger and water low on the left; raw biogas leaves at the header flange high on the right and digestate at
	 * the chute low on the right; the power socket is at the back.
	 */
	public static final MachineForm ANAEROBIC_DIGESTER_FORM = MachineForm.builder(Jugcraft.id("anaerobic_digester"), MachineKind.CHEMICAL_REACTOR)
			.layer("C#####", "######", "######", "######", "######", "######")
			.layer("######", "######", "######", "######", "######", "######")
			.layer("######", "######", "######", "######", "######", "######")
			.layer("######", "######", "######", "######", "######", "######")
			.layer("######", "######", "######", "######", "######", "######")
			.layer("######", "######", "######", "######", "######", "######")
			.profile(OperatingProfile.BULK)
			.inputTank("water")
			.outputTank("biogas")
			.outputTank("digestate")
			.items(1, 1)
			.capability("jugcraft:anaerobic_digestion")
			.alias("digestion_water", "water")
			.alias("raw_biogas", "biogas")
			.upgrades()
			.warmup(40)
			.port("feed_in", FormPort.Kind.ITEM_IN, 0, 0, 2, 1, FormSide.LEFT)
			.port("water_in", FormPort.Kind.FLUID_IN, 0, 0, 4, 0, FormSide.LEFT)
			.port("biogas_out", FormPort.Kind.FLUID_OUT, 0, 5, 4, 5, FormSide.RIGHT)
			.port("digestate_out", FormPort.Kind.ITEM_OUT, FormPort.ALL, 5, 2, 0, FormSide.RIGHT)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 3, 5, 0, FormSide.BACK)
			.port("liquid_digestate_out", FormPort.Kind.FLUID_OUT, 1, 0, 5, 0, FormSide.BACK)
			.build();

	/**
	 * The Coal Gasifier: the selected earlier gas-production form of the chemical reactor family (its energy figures, the
	 * plan's paid electrical heat), four wide, four deep and six tall in the expanded profile. The carbon feed goes in at
	 * the side charging chute and water low on the left; raw syngas leaves at the header flange high on the right and the
	 * ash chamber's residue at the chute low on the right.
	 */
	public static final MachineForm COAL_GASIFIER_FORM = MachineForm.builder(Jugcraft.id("coal_gasifier"), MachineKind.CHEMICAL_REACTOR)
			.layer("C###", "####", "####", "####")
			.layer("####", "####", "####", "####")
			.layer("####", "####", "####", "####")
			.layer("####", "####", "####", "####")
			.layer("####", "####", "####", "####")
			.layer("####", "####", "####", "####")
			.profile(OperatingProfile.EXPANDED)
			.inputTank("water")
			.outputTank("product")
			.items(1, 1)
			.capability("jugcraft:coal_gasification")
			.capability("jugcraft:starter_gasification")
			.alias("feed_water", "water")
			.alias("dirty_gas", "product")
			.upgrades()
			.warmup(40)
			.port("feed_in", FormPort.Kind.ITEM_IN, 0, 0, 1, 2, FormSide.LEFT)
			.port("water_in", FormPort.Kind.FLUID_IN, 0, 0, 3, 0, FormSide.LEFT)
			.port("syngas_out", FormPort.Kind.FLUID_OUT, 0, 3, 2, 5, FormSide.RIGHT)
			.port("residue_out", FormPort.Kind.ITEM_OUT, FormPort.ALL, 3, 1, 0, FormSide.RIGHT)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 2, 3, 0, FormSide.BACK)
			.build();

	/**
	 * The Gas Cleanup and Separation Rack: the selected combined cleanup and separation form of the synthesis family (the
	 * Infuser's energy figures), three wide, five deep and four tall. Raw syngas goes in low on the left; carbon monoxide
	 * and hydrogen leave at the tops of their towers at the back.
	 */
	public static final MachineForm GAS_CLEANUP_RACK_FORM = MachineForm.builder(Jugcraft.id("gas_cleanup_rack"), MachineKind.SYNTHESIS_CONVERTER)
			.layer("C##", "###", "###", "###", "###")
			.layer("###", "###", "###", "###", "###")
			.layer("###", "###", "###", "###", "###")
			.layer("###", "###", "###", "###", "###")
			.profile(OperatingProfile.ENTRY)
			.inputTank("feed")
			.outputTank("product")
			.outputTank("hydrogen")
			.capability("jugcraft:gas_separation")
			.upgrades()
			.warmup(40)
			.port("feed_in", FormPort.Kind.FLUID_IN, 0, 0, 2, 0, FormSide.LEFT)
			.port("product_out", FormPort.Kind.FLUID_OUT, 0, 2, 4, 3, FormSide.TOP)
			.port("hydrogen_out", FormPort.Kind.FLUID_OUT, 1, 0, 4, 3, FormSide.TOP)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 1, 4, 0, FormSide.BACK)
			.build();

	/**
	 * The Bio Generator: the lower-output generator for biological fuels, a form of the diesel generator's family (its
	 * energy figures: a 60,000 JE store, 1,024 JE/t out), three wide, four deep and three tall. The skid and the
	 * engine-generator train fill the two lower layers; above them only the exhaust's run along the middle and the
	 * radiator at the back. The controller is the sheltered instrument panel at the front left. Bioethanol comes in low on
	 * the right side; power leaves at the terminal box low on the left. It also burns the Digester's biogas.
	 */
	public static final MachineForm BIO_GENERATOR_FORM = MachineForm.builder(Jugcraft.id("bio_generator"), MachineKind.DIESEL_GENERATOR)
			.layer("C##", "###", "###", "###")
			.layer("###", "###", "###", "###")
			.layer(".#.", ".#.", ".#.", "###")
			.profile(OperatingProfile.ENTRY)
			.inputTank("fuel")
			.fuel(Jugcraft.id("bioethanol"), 192, 96)
			.fuel(Jugcraft.id("biogas"), 268, 64)
			.fuel(Jugcraft.id("clean_biogas"), 268, 64)
			.port("fuel_in", FormPort.Kind.FLUID_IN, 0, 2, 2, 0, FormSide.RIGHT)
			.port("power_out", FormPort.Kind.ENERGY_OUT, 0, 0, 2, 0, FormSide.LEFT)
			.build();

	/**
	 * The Bulk Electrolytic Separator: the art brief's 4x4x6 variant of the Electrolytic Separator, the same cell
	 * housing and towers twice as large, in the Electrolytic Cell family with the Separator's tanks and capability, so
	 * it splits the same water and brine, in the bulk profile: four batches side by side from 16,000 mB tanks at 80% of
	 * the draw a batch. It takes no upgrade cards, which would put its hydrogen under the electrolysis floor.
	 */
	public static final MachineForm BULK_ELECTROLYTIC_SEPARATOR_FORM = MachineForm.builder(Jugcraft.id("bulk_electrolytic_separator"),
					MachineKind.ELECTROLYTIC_CELL)
			.layer("C###", "####", "####", "####")
			.layer(".###", "####", "####", ".##.")
			.layer(".###", "####", "####", ".##.")
			.layer(".###", "####", "####", ".##.")
			.layer("####", "####", "####", ".##.")
			.layer(".##.", "####", "####", ".##.")
			.profile(OperatingProfile.BULK)
			.inputTank("feed")
			.outputTank("anode_gas")
			.outputTank("hydrogen")
			.outputTank("lye")
			.capability(AQUEOUS_ELECTROLYSIS)
			.warmup(40)
			.port("feed_in", FormPort.Kind.FLUID_IN, 0, 1, 3, 1, FormSide.BACK)
			.port("anode_gas_out", FormPort.Kind.FLUID_OUT, 0, 3, 0, 4, FormSide.FRONT)
			.port("hydrogen_out", FormPort.Kind.FLUID_OUT, 1, 0, 0, 4, FormSide.FRONT)
			.port("lye_out", FormPort.Kind.FLUID_OUT, 2, 2, 0, 2, FormSide.FRONT)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 2, 0, 0, FormSide.FRONT)
			.capability("jugcraft:electronics_peroxide")

			.build();

	/**
	 * The Bulk Chemical Oxidizer: the art brief's 4x4x6 variant of the Chemical Oxidizer, the same throat, hood and
	 * hopper twice as large, in the chemical reactor family with the Oxidizer's tanks, feed slot and capability, so it
	 * burns the same feeds, in the bulk profile: four batches side by side from 16,000 mB tanks at 80% of the draw a batch.
	 */
	public static final MachineForm BULK_CHEMICAL_OXIDIZER_FORM = MachineForm.builder(Jugcraft.id("bulk_chemical_oxidizer"),
					MachineKind.CHEMICAL_REACTOR)
			.layer("C###", "####", "####", "####")
			.layer("####", "####", "####", "###.")
			.layer("####", "####", "####", "#.#.")
			.layer("####", "####", "####", "###.")
			.layer("####", "####", "####", "###.")
			.layer("####", "####", "####", "###.")
			.profile(OperatingProfile.BULK)
			.inputTank("oxidant")
			.outputTank("product")
			.items(1, 0)
			.capability(SOLID_OXIDATION)
			.capability("jugcraft:starter_gas_oxidizing")
			.upgrades()
			.warmup(40)
			.port("feed_in", FormPort.Kind.ITEM_IN, 0, 1, 2, 5, FormSide.TOP)
			.port("oxidant_in", FormPort.Kind.FLUID_IN, 0, 3, 0, 0, FormSide.RIGHT)
			.port("product_out", FormPort.Kind.FLUID_OUT, 0, 2, 0, 5, FormSide.TOP)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 2, 0, FormSide.LEFT)
			.alias("product_gas", "product")

			.build();

	/**
	 * The Bulk Chemical Infuser: the art brief's 4x6x6 variant of the Chemical Infuser, the same chamber, paired inlets,
	 * bed drawer and cooling jacket twice as large, in the synthesis family with the Infuser's tanks, bed socket and
	 * capabilities, so it runs the same recipes, in the bulk profile: four batches side by side from 16,000 mB tanks at
	 * 80% of the draw a batch.
	 */
	public static final MachineForm BULK_CHEMICAL_INFUSER_FORM = MachineForm.builder(Jugcraft.id("bulk_chemical_infuser"),
					MachineKind.SYNTHESIS_CONVERTER)
			.layer("C###", "####", "####", "####", "####", "####")
			.layer("####", "####", "####", "####", ".###", ".###")
			.layer("####", "####", "####", "####", ".##.", ".##.")
			.layer("####", "####", "####", "####", ".##.", ".##.")
			.layer("####", "####", "####", "####", ".##.", ".##.")
			.layer("....", ".##.", ".##.", "....", "....", "....")
			.profile(OperatingProfile.BULK)
			.inputTank("hydrogen")
			.inputTank("reactant")
			.outputTank("product")
			.outputTank("water")
			.socket("bed", CATALYST_BEDS)
			.capability(GAS_COMBINATION)
			.capability(NICKEL_METHANATION)
			.capability(CONTACT_CONVERSION)
			.capability("jugcraft:starter_gas_infusing")
			.upgrades()
			.warmup(40)
			.port("hydrogen_in", FormPort.Kind.FLUID_IN, 0, 0, 2, 2, FormSide.LEFT)
			.port("reactant_in", FormPort.Kind.FLUID_IN, 1, 3, 2, 2, FormSide.RIGHT)
			.port("product_out", FormPort.Kind.FLUID_OUT, 0, 2, 5, 2, FormSide.BACK)
			.port("water_out", FormPort.Kind.FLUID_OUT, 1, 1, 5, 0, FormSide.BACK)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 3, 4, 0, FormSide.RIGHT)
			.capability("jugcraft:cobalt_synthesis")
			.capability("jugcraft:syngas_balancing")
			.alias("reagent_a", "hydrogen")
			.alias("reagent_b", "reactant")
			.alias("product_gas", "product")
			.alias("process_water", "water")
			.waterDrainOutput(1)

			.build();

	/**
	 * The Large Gas Burning Generator: the art brief's 5x6x5 variant of the Gas Burning Generator, the same
	 * engine-generator train drawn larger, in the gas turbine's family. It burns hydrogen and methane four times as fast,
	 * methane an eighth better, from an 8,000 mB tank. Gas comes in low on the right side, the second block back; power
	 * leaves at the back of the terminal cabinet.
	 */
	public static final MachineForm LARGE_GAS_BURNING_GENERATOR_FORM = MachineForm.builder(Jugcraft.id("large_gas_burning_generator"),
					MachineKind.GAS_TURBINE)
			.layer("C####", "#####", "#####", "#####", "#####", "#####")
			.layer(".###.", ".####", ".###.", "#####", "#####", "#####")
			.layer(".###.", ".###.", ".###.", "#####", "#####", "#####")
			.layer(".###.", ".###.", ".###.", ".###.", ".###.", ".####")
			.layer(".....", ".....", ".....", ".....", ".....", "..###")
			.profile(OperatingProfile.EXPANDED)
			.inputTank("fuel")
			.fuel(Jugcraft.id("hydrogen"), FluidFuels.HYDROGEN, FluidFuels.LARGE_GAS_BURNING_HYDROGEN)
			.fuel(Jugcraft.id("methane"), FluidFuels.LARGE_GAS_METHANE, FluidFuels.LARGE_GAS_BURNING_METHANE)
			.port("fuel_in", FormPort.Kind.FLUID_IN, 0, 4, 1, 0, FormSide.RIGHT)
			.port("power_out", FormPort.Kind.ENERGY_OUT, 0, 3, 5, 1, FormSide.BACK)
			.build();

	/**
	 * The Large Bio Generator: the art brief's 5x6x5 variant of the Bio Generator, the same engine-generator drawn larger,
	 * in the diesel generator's family. It burns bioethanol and biogas a quarter better and four times as fast, from an
	 * 8,000 mB tank. Fuel comes in low on the right side and power leaves low on the left, both behind the middle.
	 */
	public static final MachineForm LARGE_BIO_GENERATOR_FORM = MachineForm.builder(Jugcraft.id("large_bio_generator"),
					MachineKind.DIESEL_GENERATOR)
			.layer("C####", "#####", "#####", "#####", "#####", "#####")
			.layer("#####", "#####", "#####", "#####", "#####", "#####")
			.layer("#####", "#####", "#####", "#####", "#####", "#####")
			.layer("#####", "#####", "#####", "#####", "#####", "#####")
			.layer("#####", "#####", "#####", "#####", "#####", "#####")
			.profile(OperatingProfile.EXPANDED)
			.inputTank("fuel")
			.fuel(Jugcraft.id("bioethanol"), FluidFuels.LARGE_BIOETHANOL, FluidFuels.LARGE_BIO_GENERATOR_BIOETHANOL)
			.fuel(Jugcraft.id("biogas"), FluidFuels.LARGE_BIOGAS, FluidFuels.LARGE_BIO_GENERATOR_BIOGAS)
			.fuel(Jugcraft.id("clean_biogas"), FluidFuels.LARGE_BIOGAS, FluidFuels.LARGE_BIO_GENERATOR_BIOGAS)
			.port("fuel_in", FormPort.Kind.FLUID_IN, 0, 4, 3, 0, FormSide.RIGHT)
			.port("power_out", FormPort.Kind.ENERGY_OUT, 0, 0, 3, 0, FormSide.LEFT)
			.build();

	/**
	 * The Combined Cycle Power Plant: the factory's big power station, six wide, four deep and five tall, the largest a form may be. A gas turbine
	 * on its skid draws air from the filter house over its generator end and blows its exhaust through a tall heat-recovery
	 * steam generator to the stack; the steam turns a second turbine on the same shaft. It burns methane (528 JE/mB) and the
	 * Coal Gasifier's raw syngas (256 JE/mB) at 2,048 JE/t and hydrogen (128 JE/mB) at 1,024 JE/t, from one inlet at the
	 * back, and sends the power out of the generator's terminal on its left side.
	 */
	public static final MachineForm COMBINED_CYCLE_PLANT_FORM = MachineForm.builder(Jugcraft.id("combined_cycle_power_plant"),
					MachineKind.GAS_TURBINE)
			.layer("C#####", "######", "######", "######")
			.layer("######", "######", "######", "######")
			.layer("##..##", "##..##", "##..##", "##..##")
			.layer("##..##", "##..##", "##..##", "##..##")
			.layer("....##", "....##", "....##", "....##")
			.profile(OperatingProfile.ENTRY)
			.inputTank("fuel")
			.fuel(Jugcraft.id("methane"), FluidFuels.COMBINED_CYCLE_METHANE, FluidFuels.COMBINED_CYCLE_METHANE_RATE)
			.fuel(Jugcraft.id("raw_syngas"), FluidFuels.RAW_SYNGAS, FluidFuels.COMBINED_CYCLE_SYNGAS_RATE)
			.fuel(Jugcraft.id("hydrogen"), FluidFuels.HYDROGEN, FluidFuels.COMBINED_CYCLE_HYDROGEN_RATE)
			.port("fuel_in", FormPort.Kind.FLUID_IN, 0, 2, 3, 0, FormSide.BACK)
			.port("power_out", FormPort.Kind.ENERGY_OUT, 0, 0, 2, 0, FormSide.LEFT)
			.build();

	public static FormMachineBlock ELECTROLYTIC_SEPARATOR;
	public static FormMachineBlock BULK_ELECTROLYTIC_SEPARATOR;
	public static FormMachineBlock GAS_BURNING_GENERATOR;
	public static FormMachineBlock CHEMICAL_INFUSER;
	public static FormMachineBlock BULK_CHEMICAL_INFUSER;
	public static FormMachineBlock CHEMICAL_OXIDIZER;
	public static FormMachineBlock BULK_CHEMICAL_OXIDIZER;
	public static FormMachineBlock INDUSTRIAL_FERMENTER;
	public static FormMachineBlock BULK_FERMENTER;
	public static FormMachineBlock BULK_CHEMICAL_REACTOR;
	public static FormMachineBlock ANAEROBIC_DIGESTER;
	public static FormMachineBlock COAL_GASIFIER;
	public static FormMachineBlock GAS_CLEANUP_RACK;
	public static FormMachineBlock BIO_GENERATOR;
	public static FormMachineBlock LARGE_BIO_GENERATOR;
	public static FormMachineBlock LARGE_GAS_BURNING_GENERATOR;
	public static FormMachineBlock COMBINED_CYCLE_PLANT;
	public static BlockEntityType<FormMachineBlockEntity> ENTITY;

	public static final MachineForm GAS_CLEANING_RACK_FORM = MachineForm.builder(Jugcraft.id("gas_cleaning_rack"), MachineKind.CHEMICAL_REACTOR)
			.layer("C##", "###", "###", "###", "###")
			.layer("###", "###", "###", "###", "###")
			.layer("###", "###", "###", "###", "###")
			.layer("###", "###", "###", "###", "###")
			.profile(OperatingProfile.ENTRY)
			.inputTank("dirty_gas")
			.inputTank("wash_water")
			.outputTank("carbon_monoxide")
			.outputTank("hydrogen")
			.items(0, 1)
			.capability("jugcraft:starter_gas_cleaning")
			.capability("jugcraft:biogas_cleaning")
			.upgrades()
			.warmup(40)
			.port("dirty_gas_in", FormPort.Kind.FLUID_IN, 0, 2, 4, 3, FormSide.BACK)
			.port("wash_water_in", FormPort.Kind.FLUID_IN, 1, 0, 0, 0, FormSide.LEFT)
			.port("co_out", FormPort.Kind.FLUID_OUT, 0, 0, 0, 2, FormSide.FRONT)
			.port("hydrogen_out", FormPort.Kind.FLUID_OUT, 1, 1, 0, 2, FormSide.FRONT)
			.port("residue_out", FormPort.Kind.ITEM_OUT, 0, 1, 0, 0, FormSide.FRONT)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 2, 0, 0, FormSide.FRONT)
			.build();
	public static FormMachineBlock GAS_CLEANING_RACK;

	public static final MachineForm BIOGAS_CLEANER_FORM = MachineForm.builder(Jugcraft.id("biogas_cleaner"), MachineKind.CHEMICAL_REACTOR)
			.layer("C#", "##", "##")
			.layer("##", "##", "##")
			.layer("##", "##", "##")
			.profile(OperatingProfile.ENTRY)
			.inputTank("raw_biogas")
			.inputTank("cleaning_water")
			.outputTank("clean_biogas")
			.outputTank("wash_digestate")
			.capability("jugcraft:biogas_cleaning")
			.upgrades()
			.warmup(40)
			.port("raw_in", FormPort.Kind.FLUID_IN, 0, 1, 2, 2, FormSide.BACK)
			.port("water_in", FormPort.Kind.FLUID_IN, 1, 0, 2, 0, FormSide.BACK)
			.port("clean_out", FormPort.Kind.FLUID_OUT, 0, 1, 0, 2, FormSide.FRONT)
			.port("wash_out", FormPort.Kind.FLUID_OUT, 1, 1, 0, 0, FormSide.FRONT)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.FRONT)
			.build();
	public static FormMachineBlock BIOGAS_CLEANER;

	public static final MachineForm DIGESTATE_SEPARATOR_FORM = MachineForm.builder(Jugcraft.id("digestate_separator"), MachineKind.CHEMICAL_REACTOR)
			.layer("C#", "##", "##")
			.layer("##", "##", "##")
			.layer("##", "##", "##")
			.profile(OperatingProfile.ENTRY)
			.inputTank("digestate_feed")
			.outputTank("recovered_water")
			.items(0, 1)
			.capability("jugcraft:digestate_separation")
			.upgrades()
			.warmup(40)
			.port("slurry_in", FormPort.Kind.FLUID_IN, 0, 0, 2, 1, FormSide.BACK)
			.port("water_out", FormPort.Kind.FLUID_OUT, 0, 1, 2, 0, FormSide.BACK)
			.port("fiber_out", FormPort.Kind.ITEM_OUT, 0, 1, 0, 0, FormSide.FRONT)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.FRONT)
			.build();
	public static FormMachineBlock DIGESTATE_SEPARATOR;

	public static final MachineForm MASHING_STATION_FORM = MachineForm.builder(Jugcraft.id("mashing_station"), MachineKind.CHEMICAL_REACTOR)
			.layer("C##", "###", "###")
			.layer("###", "###", "###")
			.layer("###", "###", "###")
			.profile(OperatingProfile.ENTRY)
			.inputTank("mash_water")
			.outputTank("crop_mash")
			.items(1, 0)
			.capability("jugcraft:crop_mashing")
			.upgrades()
			.warmup(40)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.FRONT)
			.port("feed_0", FormPort.Kind.FLUID_IN, 0, 0, 2, 0, FormSide.BACK)
			.port("product_0", FormPort.Kind.FLUID_OUT, 0, 1, 2, 0, FormSide.BACK)
			.port("solid_in_0", FormPort.Kind.ITEM_IN, 0, 2, 0, 1, FormSide.FRONT)
			.build();
	public static FormMachineBlock MASHING_STATION;

	public static final MachineForm CHEMICAL_DISTILLATION_STATION_FORM = MachineForm.builder(Jugcraft.id("chemical_distillation_station"), MachineKind.DISTILLATION_TOWER)
			.layer("C###", "####", "####", "####")
			.layer("####", "####", "####", "####")
			.layer("####", "####", "####", "####")
			.layer("####", "####", "####", "####")
			.layer("####", "####", "####", "####")
			.layer("####", "####", "####", "####")
			.profile(OperatingProfile.ENTRY)
			.inputTank("ethanol_broth")
			.outputTank("distilled_ethanol")
			.outputTank("stillage")
			.capability("jugcraft:chemical_distillation")
			.capability("jugcraft:process_distillation")
            .capability("jugcraft:cryogenic_vaporization")
			.upgrades()
			.warmup(40)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.FRONT)
			.port("feed_0", FormPort.Kind.FLUID_IN, 0, 2, 3, 1, FormSide.BACK)
			.port("product_0", FormPort.Kind.FLUID_OUT, 0, 0, 3, 1, FormSide.BACK)
			.port("product_1", FormPort.Kind.FLUID_OUT, 1, 2, 3, 0, FormSide.BACK)
			.build();
	public static FormMachineBlock CHEMICAL_DISTILLATION_STATION;

	public static final MachineForm BAUXITE_DIGESTER_FORM = MachineForm.builder(Jugcraft.id("bauxite_digester"), MachineKind.CHEMICAL_REACTOR)
			.layer("C##", "###", "###", "###")
			.layer("###", "###", "###", "###")
			.layer("###", "###", "###", "###")
			.profile(OperatingProfile.ENTRY)
			.inputTank("digestion_lye")
			.inputTank("digestion_water")
			.outputTank("aluminate_slurry")
			.items(1, 0)
			.capability("jugcraft:bauxite_digestion")
			.upgrades()
			.warmup(40)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.FRONT)
			.port("feed_0", FormPort.Kind.FLUID_IN, 0, 0, 3, 1, FormSide.BACK)
			.port("feed_1", FormPort.Kind.FLUID_IN, 1, 1, 3, 1, FormSide.BACK)
			.port("product_0", FormPort.Kind.FLUID_OUT, 0, 2, 3, 0, FormSide.BACK)
			.port("solid_in_0", FormPort.Kind.ITEM_IN, 0, 2, 0, 1, FormSide.FRONT)
			.build();
	public static FormMachineBlock BAUXITE_DIGESTER;

	public static final MachineForm ALUMINATE_FILTER_PRESS_FORM = MachineForm.builder(Jugcraft.id("aluminate_filter_press"), MachineKind.CHEMICAL_REACTOR)
			.layer("C##", "###", "###", "###")
			.layer("###", "###", "###", "###")
			.layer("###", "###", "###", "###")
			.profile(OperatingProfile.ENTRY)
			.inputTank("aluminate_slurry")
			.outputTank("recovered_lye")
			.outputTank("recovered_water")
			.items(0, 2)
			.capability("jugcraft:aluminate_recovery")
			.capability("jugcraft:digestate_separation")
			.capability("jugcraft:phosphate_filtration")
			.upgrades()
			.warmup(40)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.FRONT)
			.port("feed_0", FormPort.Kind.FLUID_IN, 0, 1, 3, 1, FormSide.BACK)
			.port("product_0", FormPort.Kind.FLUID_OUT, 0, 0, 3, 0, FormSide.BACK)
			.port("product_1", FormPort.Kind.FLUID_OUT, 1, 1, 3, 0, FormSide.BACK)
			.port("solid_out_0", FormPort.Kind.ITEM_OUT, 0, 2, 3, 1, FormSide.BACK)
			.port("solid_out_1", FormPort.Kind.ITEM_OUT, 1, 2, 3, 2, FormSide.BACK)
			.build();
	public static FormMachineBlock ALUMINATE_FILTER_PRESS;

	public static final MachineForm ALUMINA_CALCINER_FORM = MachineForm.builder(Jugcraft.id("alumina_calciner"), MachineKind.CHEMICAL_REACTOR)
			.layer("C##", "###", "###", "###")
			.layer("###", "###", "###", "###")
			.layer("###", "###", "###", "###")
			.profile(OperatingProfile.ENTRY)
			.inputTank("oven_solution")
			.outputTank("recovered_water")
			.items(1, 1)
			.capability("jugcraft:alumina_calcination")
			.capability("jugcraft:chloride_drying")
			.capability("jugcraft:ptfe_sintering")
			.upgrades()
			.warmup(40)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.FRONT)
			.port("product_0", FormPort.Kind.FLUID_OUT, 0, 0, 3, 1, FormSide.BACK)
			.port("solid_in_0", FormPort.Kind.ITEM_IN, 0, 2, 0, 1, FormSide.FRONT)
			.port("solid_out_0", FormPort.Kind.ITEM_OUT, 0, 2, 3, 1, FormSide.BACK)
			.port("solution_in", FormPort.Kind.FLUID_IN, 0, 1, 3, 0, FormSide.BACK)
			.capability("jugcraft:ceramic_firing")
			.capability("jugcraft:battery_alumina_calciner")
			.capability("jugcraft:phosphate_drying")
			.build();
	public static FormMachineBlock ALUMINA_CALCINER;

	public static final MachineForm ALUMINUM_REDUCTION_CELL_FORM = MachineForm.builder(Jugcraft.id("aluminum_reduction_cell"), MachineKind.ELECTROLYTIC_CELL)
			.layer("C##", "###", "###")
			.layer("###", "###", "###")
			.layer("###", "###", "###")
			.layer("###", "###", "###")
			.profile(OperatingProfile.ENTRY)
			.outputTank("carbon_dioxide")
			.items(2, 1)
			.capability("jugcraft:aluminum_reduction")
			.upgrades()
			.warmup(40)
			.gasVentOutput(0, Jugcraft.id("carbon_dioxide"))
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.FRONT)
			.port("product_0", FormPort.Kind.FLUID_OUT, 0, 0, 2, 1, FormSide.BACK)
			.port("solid_in_0", FormPort.Kind.ITEM_IN, 0, 2, 0, 1, FormSide.FRONT)
			.port("solid_in_1", FormPort.Kind.ITEM_IN, 1, 2, 0, 2, FormSide.FRONT)
			.port("solid_out_0", FormPort.Kind.ITEM_OUT, 0, 2, 2, 1, FormSide.BACK)
			.build();
	public static FormMachineBlock ALUMINUM_REDUCTION_CELL;

	public static final MachineForm COASTAL_SEAWATER_INTAKE_FORM = MachineForm.builder(Jugcraft.id("coastal_seawater_intake"), MachineKind.CHEMICAL_REACTOR)
			.layer("C##", "###", "###")
			.layer("###", "###", "###")
			.profile(OperatingProfile.ENTRY)
			.outputTank("seawater")
			.capability("jugcraft:coastal_pumping")
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.FRONT)
			.port("product_0", FormPort.Kind.FLUID_OUT, 0, 0, 2, 0, FormSide.BACK)
			.build();
	public static FormMachineBlock COASTAL_SEAWATER_INTAKE;

	public static final MachineForm BRINE_CONCENTRATOR_FORM = MachineForm.builder(Jugcraft.id("brine_concentrator"), MachineKind.CHEMICAL_REACTOR)
			.layer("C##", "###", "###")
			.layer("###", "###", "###")
			.layer("###", "###", "###")
			.profile(OperatingProfile.ENTRY)
			.inputTank("seawater")
			.outputTank("magnesium_brine")
			.outputTank("recovered_water")
			.capability("jugcraft:seawater_concentration")
			.capability("jugcraft:acid_concentration")
			.upgrades()
			.warmup(40)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.FRONT)
			.port("feed_0", FormPort.Kind.FLUID_IN, 0, 0, 2, 1, FormSide.BACK)
			.port("product_0", FormPort.Kind.FLUID_OUT, 0, 0, 2, 0, FormSide.BACK)
			.port("product_1", FormPort.Kind.FLUID_OUT, 1, 1, 2, 0, FormSide.BACK)
			.build();
	public static FormMachineBlock BRINE_CONCENTRATOR;

	public static final MachineForm MINERAL_CONDITIONER_FORM = MachineForm.builder(Jugcraft.id("mineral_conditioner"), MachineKind.CHEMICAL_REACTOR)
			.layer("C##", "###", "###")
			.layer("###", "###", "###")
			.layer("###", "###", "###")
			.profile(OperatingProfile.ENTRY)
			.inputTank("mineral_feed")
			.inputTank("conditioning_reagent")
			.outputTank("conditioned_liquid")
			.outputTank("recovered_water")
			.items(1, 1)
			.capability("jugcraft:mineral_conditioning")
			.upgrades()
			.warmup(40)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.FRONT)
			.port("feed_0", FormPort.Kind.FLUID_IN, 0, 0, 2, 1, FormSide.BACK)
			.port("feed_1", FormPort.Kind.FLUID_IN, 1, 1, 2, 1, FormSide.BACK)
			.port("product_0", FormPort.Kind.FLUID_OUT, 0, 0, 2, 0, FormSide.BACK)
			.port("product_1", FormPort.Kind.FLUID_OUT, 1, 1, 2, 0, FormSide.BACK)
			.port("solid_in_0", FormPort.Kind.ITEM_IN, 0, 2, 0, 1, FormSide.FRONT)
			.port("solid_out_0", FormPort.Kind.ITEM_OUT, 0, 2, 2, 1, FormSide.BACK)
			.capability("jugcraft:battery_mineral_conditioner")
			.capability("jugcraft:electronics_conditioning")
			.capability("jugcraft:phosphate_preparation")
			.capability("jugcraft:synthetic_preparation")
			.capability("jugcraft:process_recovery")
            .capability("jugcraft:propellant_recovery")
			.build();
	public static FormMachineBlock MINERAL_CONDITIONER;

	public static final MachineForm CHLORIDE_DRYER_FORM = MachineForm.builder(Jugcraft.id("chloride_dryer"), MachineKind.CHEMICAL_REACTOR)
			.layer("C##", "###", "###")
			.layer("###", "###", "###")
			.layer("###", "###", "###")
			.profile(OperatingProfile.ENTRY)
			.inputTank("chloride_solution")
			.outputTank("recovered_water")
			.items(0, 1)
			.capability("jugcraft:chloride_drying")
			.upgrades()
			.warmup(40)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.FRONT)
			.port("feed_0", FormPort.Kind.FLUID_IN, 0, 0, 2, 1, FormSide.BACK)
			.port("product_0", FormPort.Kind.FLUID_OUT, 0, 0, 2, 0, FormSide.BACK)
			.port("solid_out_0", FormPort.Kind.ITEM_OUT, 0, 2, 2, 1, FormSide.BACK)
			.build();
	public static FormMachineBlock CHLORIDE_DRYER;

	public static final MachineForm MAGNESIUM_ELECTROLYSIS_CELL_FORM = MachineForm.builder(Jugcraft.id("magnesium_electrolysis_cell"), MachineKind.ELECTROLYTIC_CELL)
			.layer("C##", "###", "###")
			.layer("###", "###", "###")
			.layer("###", "###", "###")
			.profile(OperatingProfile.ENTRY)
			.outputTank("chlorine")
			.items(1, 1)
			.capability("jugcraft:magnesium_electrolysis")
			.upgrades()
			.warmup(40)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.FRONT)
			.port("product_0", FormPort.Kind.FLUID_OUT, 0, 0, 2, 0, FormSide.BACK)
			.port("solid_in_0", FormPort.Kind.ITEM_IN, 0, 2, 0, 1, FormSide.FRONT)
			.port("solid_out_0", FormPort.Kind.ITEM_OUT, 0, 2, 2, 1, FormSide.BACK)
			.build();
	public static FormMachineBlock MAGNESIUM_ELECTROLYSIS_CELL;

	public static final MachineForm TITANIUM_CHLORINATOR_FORM = MachineForm.builder(Jugcraft.id("titanium_chlorinator"), MachineKind.CHEMICAL_REACTOR)
			.layer("C##", "###", "###", "###")
			.layer("###", "###", "###", "###")
			.layer("###", "###", "###", "###")
			.layer("###", "###", "###", "###")
			.profile(OperatingProfile.ENTRY)
			.inputTank("chlorine")
			.outputTank("crude_chloride")
			.outputTank("carbon_dioxide")
			.items(2, 0)
			.capability("jugcraft:titanium_chlorination")
			.upgrades()
			.warmup(40)
			.gasVentOutput(1, Jugcraft.id("carbon_dioxide"))
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.FRONT)
			.port("feed_0", FormPort.Kind.FLUID_IN, 0, 0, 3, 1, FormSide.BACK)
			.port("product_0", FormPort.Kind.FLUID_OUT, 0, 0, 3, 0, FormSide.BACK)
			.port("product_1", FormPort.Kind.FLUID_OUT, 1, 1, 3, 0, FormSide.BACK)
			.port("solid_in_0", FormPort.Kind.ITEM_IN, 0, 2, 0, 1, FormSide.FRONT)
			.port("solid_in_1", FormPort.Kind.ITEM_IN, 1, 2, 0, 2, FormSide.FRONT)
			.build();
	public static FormMachineBlock TITANIUM_CHLORINATOR;

	public static final MachineForm TITANIUM_REDUCTION_RETORT_FORM = MachineForm.builder(Jugcraft.id("titanium_reduction_retort"), MachineKind.ELECTROLYTIC_CELL)
			.layer("C##", "###", "###", "###")
			.layer("###", "###", "###", "###")
			.layer("###", "###", "###", "###")
			.profile(OperatingProfile.ENTRY)
			.inputTank("purified_chloride")
			.items(1, 2)
			.capability("jugcraft:titanium_reduction")
			.upgrades()
			.warmup(40)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.FRONT)
			.port("feed_0", FormPort.Kind.FLUID_IN, 0, 0, 3, 1, FormSide.BACK)
			.port("solid_in_0", FormPort.Kind.ITEM_IN, 0, 2, 0, 1, FormSide.FRONT)
			.port("solid_out_0", FormPort.Kind.ITEM_OUT, 0, 2, 3, 1, FormSide.BACK)
			.port("solid_out_1", FormPort.Kind.ITEM_OUT, 1, 2, 3, 2, FormSide.BACK)
			.build();
	public static FormMachineBlock TITANIUM_REDUCTION_RETORT;

	public static final MachineForm POLYMER_MOLDING_PRESS_FORM = MachineForm.builder(Jugcraft.id("polymer_molding_press"), MachineKind.CHEMICAL_REACTOR)
			.layer("C##", "###", "###", "###")
			.layer("###", "###", "###", "###")
			.layer("###", "###", "###", "###")
			.profile(OperatingProfile.ENTRY)
			.items(1, 1)
			.socket("mold", TagKey.create(Registries.ITEM, Jugcraft.id("polymer_molds")))
			.capability("jugcraft:polymer_molding")
			.upgrades()
			.warmup(40)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.FRONT)
			.port("solid_in_0", FormPort.Kind.ITEM_IN, 0, 2, 0, 1, FormSide.FRONT)
			.port("solid_out_0", FormPort.Kind.ITEM_OUT, 0, 2, 3, 1, FormSide.BACK)
			.build();
	public static FormMachineBlock POLYMER_MOLDING_PRESS;

	public static final MachineForm MONOMER_PREPARATION_UNIT_FORM = MachineForm.builder(Jugcraft.id("monomer_preparation_unit"), MachineKind.CHEMICAL_REACTOR)
			.layer("C##", "###", "###", "###")
			.layer("###", "###", "###", "###")
			.layer("###", "###", "###", "###")
			.profile(OperatingProfile.ENTRY)
			.inputTank("monomer_feed")
			.inputTank("monomer_reagent")
			.outputTank("prepared_monomer")
			.outputTank("monomer_recovery")
			.capability("jugcraft:monomer_preparation")
			.upgrades()
			.warmup(40)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.FRONT)
			.port("feed_0", FormPort.Kind.FLUID_IN, 0, 0, 3, 1, FormSide.BACK)
			.port("feed_1", FormPort.Kind.FLUID_IN, 1, 1, 3, 1, FormSide.BACK)
			.port("product_0", FormPort.Kind.FLUID_OUT, 0, 0, 3, 0, FormSide.BACK)
			.port("product_1", FormPort.Kind.FLUID_OUT, 1, 1, 3, 0, FormSide.BACK)
			.build();
	public static FormMachineBlock MONOMER_PREPARATION_UNIT;

	public static final MachineForm POLYMERIZATION_KETTLE_FORM = MachineForm.builder(Jugcraft.id("polymerization_kettle"), MachineKind.CHEMICAL_REACTOR)
			.layer("C##", "###", "###")
			.layer("###", "###", "###")
			.layer("###", "###", "###")
			.layer("###", "###", "###")
			.profile(OperatingProfile.ENTRY)
			.inputTank("polymer_monomer")
			.items(0, 1)
			.socket("process_insert", TagKey.create(Registries.ITEM, Jugcraft.id("polymer_process_inserts")))
			.capability("jugcraft:graded_polymerization")
			.upgrades()
			.warmup(40)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.FRONT)
			.port("feed_0", FormPort.Kind.FLUID_IN, 0, 0, 2, 1, FormSide.BACK)
			.port("solid_out_0", FormPort.Kind.ITEM_OUT, 0, 2, 2, 1, FormSide.BACK)
			.build();
	public static FormMachineBlock POLYMERIZATION_KETTLE;

	public static final MachineForm POLYMER_EXTRUDER_FORM = MachineForm.builder(Jugcraft.id("polymer_extruder"), MachineKind.CHEMICAL_REACTOR)
			.layer("C##", "###", "###", "###", "###")
			.layer("###", "###", "###", "###", "###")
			.layer("###", "###", "###", "###", "###")
			.profile(OperatingProfile.ENTRY)
			.items(1, 2)
			.socket("extrusion_die", TagKey.create(Registries.ITEM, Jugcraft.id("polymer_extrusion_dies")))
			.capability("jugcraft:polymer_extrusion")
			.upgrades()
			.warmup(40)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.FRONT)
			.port("solid_in_0", FormPort.Kind.ITEM_IN, 0, 2, 0, 1, FormSide.FRONT)
			.port("solid_out_0", FormPort.Kind.ITEM_OUT, 0, 2, 4, 1, FormSide.BACK)
			.port("solid_out_1", FormPort.Kind.ITEM_OUT, 1, 2, 4, 2, FormSide.BACK)
			.build();
	public static FormMachineBlock POLYMER_EXTRUDER;

	public static final MachineForm POLYMER_RECLAIMER_FORM = MachineForm.builder(Jugcraft.id("polymer_reclaimer"), MachineKind.CHEMICAL_REACTOR)
			.layer("C##", "###", "###")
			.layer("###", "###", "###")
			.layer("###", "###", "###")
			.profile(OperatingProfile.ENTRY)
			.items(1, 1)
			.capability("jugcraft:polymer_reclaiming")
			.upgrades()
			.warmup(40)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.FRONT)
			.port("solid_in_0", FormPort.Kind.ITEM_IN, 0, 2, 0, 1, FormSide.FRONT)
			.port("solid_out_0", FormPort.Kind.ITEM_OUT, 0, 2, 2, 1, FormSide.BACK)
			.build();
	public static FormMachineBlock POLYMER_RECLAIMER;

	public static final MachineForm ACID_CONCENTRATOR_FORM = MachineForm.builder(Jugcraft.id("acid_concentrator"), MachineKind.CHEMICAL_REACTOR)
			.layer("C##", "###", "###")
			.layer("###", "###", "###")
			.layer("###", "###", "###")
			.layer("###", "###", "###")
			.profile(OperatingProfile.ENTRY)
			.inputTank("dilute_acid")
			.outputTank("concentrated_acid")
			.outputTank("condensate")
			.capability("jugcraft:acid_concentration")
			.upgrades()
			.warmup(40)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.FRONT)
			.port("feed_0", FormPort.Kind.FLUID_IN, 0, 0, 2, 1, FormSide.BACK)
			.port("product_0", FormPort.Kind.FLUID_OUT, 0, 0, 2, 0, FormSide.BACK)
			.port("product_1", FormPort.Kind.FLUID_OUT, 1, 1, 2, 0, FormSide.BACK)
			.build();
	public static FormMachineBlock ACID_CONCENTRATOR;

	public static final MachineForm CHEMICAL_DISSOLUTION_CHAMBER_FORM = MachineForm.builder(Jugcraft.id("chemical_dissolution_chamber"), MachineKind.CHEMICAL_REACTOR)
			.layer("C##", "###", "###", "###")
			.layer("###", "###", "###", "###")
			.layer("###", "###", "###", "###")
			.profile(OperatingProfile.ENTRY)
			.inputTank("dissolution_acid")
			.outputTank("fluoride_gas")
			.items(1, 1)
			.capability("jugcraft:fluoride_dissolution")
			.capability("jugcraft:phosphate_dissolution")
			.upgrades()
			.warmup(40)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.FRONT)
			.port("feed_0", FormPort.Kind.FLUID_IN, 0, 0, 3, 1, FormSide.BACK)
			.port("product_0", FormPort.Kind.FLUID_OUT, 0, 0, 3, 0, FormSide.BACK)
			.port("solid_in_0", FormPort.Kind.ITEM_IN, 0, 2, 0, 1, FormSide.FRONT)
			.port("solid_out_0", FormPort.Kind.ITEM_OUT, 0, 2, 3, 1, FormSide.BACK)
			.build();
	public static FormMachineBlock CHEMICAL_DISSOLUTION_CHAMBER;

	public static final MachineForm FLUOROCARBON_REACTOR_FORM = MachineForm.builder(Jugcraft.id("fluorocarbon_reactor"), MachineKind.CHEMICAL_REACTOR)
			.layer("C##", "###", "###", "###")
			.layer("###", "###", "###", "###")
			.layer("###", "###", "###", "###")
			.layer("###", "###", "###", "###")
			.profile(OperatingProfile.ENTRY)
			.inputTank("carbon_feed")
			.inputTank("halogen_reagent")
			.outputTank("fluorocarbon_product")
			.outputTank("acid_gas_recovery")
			.capability("jugcraft:fluorocarbon_synthesis")
			.upgrades()
			.warmup(40)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.FRONT)
			.port("feed_0", FormPort.Kind.FLUID_IN, 0, 0, 3, 1, FormSide.BACK)
			.port("feed_1", FormPort.Kind.FLUID_IN, 1, 1, 3, 1, FormSide.BACK)
			.port("product_0", FormPort.Kind.FLUID_OUT, 0, 0, 3, 0, FormSide.BACK)
			.port("product_1", FormPort.Kind.FLUID_OUT, 1, 1, 3, 0, FormSide.BACK)
			.build();
	public static FormMachineBlock FLUOROCARBON_REACTOR;

	public static final MachineForm FLUOROMONOMER_CRACKER_FORM = MachineForm.builder(Jugcraft.id("fluoromonomer_cracker"), MachineKind.CHEMICAL_REACTOR)
			.layer("C##", "###", "###", "###")
			.layer("###", "###", "###", "###")
			.layer("###", "###", "###", "###")
			.profile(OperatingProfile.ENTRY)
			.inputTank("fluorocarbon_feed")
			.outputTank("fluoromonomer")
			.outputTank("acid_gas_recovery")
			.capability("jugcraft:fluoromonomer_cracking")
			.upgrades()
			.warmup(40)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.FRONT)
			.port("feed_0", FormPort.Kind.FLUID_IN, 0, 0, 3, 1, FormSide.BACK)
			.port("product_0", FormPort.Kind.FLUID_OUT, 0, 0, 3, 0, FormSide.BACK)
			.port("product_1", FormPort.Kind.FLUID_OUT, 1, 1, 3, 0, FormSide.BACK)
			.build();
	public static FormMachineBlock FLUOROMONOMER_CRACKER;

	public static final MachineForm PTFE_SINTERING_OVEN_FORM = MachineForm.builder(Jugcraft.id("ptfe_sintering_oven"), MachineKind.CHEMICAL_REACTOR)
			.layer("C##", "###", "###", "###")
			.layer("###", "###", "###", "###")
			.layer("###", "###", "###", "###")
			.profile(OperatingProfile.ENTRY)
			.items(1, 1)
			.capability("jugcraft:ptfe_sintering")
			.upgrades()
			.warmup(40)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.FRONT)
			.port("solid_in_0", FormPort.Kind.ITEM_IN, 0, 2, 0, 1, FormSide.FRONT)
			.port("solid_out_0", FormPort.Kind.ITEM_OUT, 0, 2, 3, 1, FormSide.BACK)
			.build();
	public static FormMachineBlock PTFE_SINTERING_OVEN;

	public static final MachineForm LINED_FLUOROCHEMICAL_REACTOR_FORM = MachineForm.builder(Jugcraft.id("lined_fluorochemical_reactor"), MachineKind.CHEMICAL_REACTOR)
			.layer("C##", "###", "###", "###", "###")
			.layer("###", "###", "###", "###", "###")
			.layer("###", "###", "###", "###", "###")
			.layer("###", "###", "###", "###", "###")
			.layer("###", "###", "###", "###", "###")
			.profile(OperatingProfile.EXPANDED)
			.inputTank("carbon_feed")
			.inputTank("halogen_reagent")
			.outputTank("fluorocarbon_product")
			.outputTank("acid_gas_recovery")
			.capability("jugcraft:fluorocarbon_synthesis")
			.upgrades()
			.warmup(40)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.FRONT)
			.port("feed_0", FormPort.Kind.FLUID_IN, 0, 0, 4, 1, FormSide.BACK)
			.port("feed_1", FormPort.Kind.FLUID_IN, 1, 1, 4, 1, FormSide.BACK)
			.port("product_0", FormPort.Kind.FLUID_OUT, 0, 0, 4, 0, FormSide.BACK)
			.port("product_1", FormPort.Kind.FLUID_OUT, 1, 1, 4, 0, FormSide.BACK)
			.build();
	public static FormMachineBlock LINED_FLUOROCHEMICAL_REACTOR;

	public static final MachineForm PRECISION_FINISHER_FORM = MachineForm.builder(Jugcraft.id("precision_finisher"), MachineKind.CHEMICAL_REACTOR)
			.layer("C##", "###", "###")
			.layer("###", "###", "###")
			.layer("###", "###", "###")
			.profile(OperatingProfile.ENTRY)
			.inputTank("finishing_water")
			.outputTank("finishing_return")
			.items(1, 1)
			.socket("finishing_head", TagKey.create(Registries.ITEM, Jugcraft.id("finishing_heads")))
			.capability("jugcraft:precision_finishing")
			.upgrades()
			.warmup(40)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.FRONT)
			.port("feed_0", FormPort.Kind.FLUID_IN, 0, 0, 2, 1, FormSide.BACK)
			.port("product_0", FormPort.Kind.FLUID_OUT, 0, 0, 2, 0, FormSide.BACK)
			.port("solid_in_0", FormPort.Kind.ITEM_IN, 0, 2, 0, 1, FormSide.FRONT)
			.port("solid_out_0", FormPort.Kind.ITEM_OUT, 0, 2, 2, 1, FormSide.BACK)
			.build();
	public static FormMachineBlock PRECISION_FINISHER;

	public static final MachineForm WET_PROCESSING_BENCH_FORM = MachineForm.builder(Jugcraft.id("wet_processing_bench"), MachineKind.CHEMICAL_REACTOR)
			.layer("C##", "###", "###")
			.layer("###", "###", "###")
			.layer("###", "###", "###")
			.profile(OperatingProfile.ENTRY)
			.inputTank("wafer_reagent")
			.inputTank("wafer_rinse")
			.outputTank("wafer_waste")
			.items(1, 1)
			.capability("jugcraft:wafer_processing")
			.upgrades()
			.warmup(40)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.FRONT)
			.port("feed_0", FormPort.Kind.FLUID_IN, 0, 0, 2, 1, FormSide.BACK)
			.port("feed_1", FormPort.Kind.FLUID_IN, 1, 1, 2, 1, FormSide.BACK)
			.port("product_0", FormPort.Kind.FLUID_OUT, 0, 0, 2, 0, FormSide.BACK)
			.port("solid_in_0", FormPort.Kind.ITEM_IN, 0, 2, 0, 1, FormSide.FRONT)
			.port("solid_out_0", FormPort.Kind.ITEM_OUT, 0, 2, 2, 1, FormSide.BACK)
			.build();
	public static FormMachineBlock WET_PROCESSING_BENCH;

	private IndustrialForms() {
	}

	public static void register() {
		ELECTROLYTIC_SEPARATOR = block("electrolytic_separator", ELECTROLYTIC_SEPARATOR_FORM);
		BULK_ELECTROLYTIC_SEPARATOR = block("bulk_electrolytic_separator", BULK_ELECTROLYTIC_SEPARATOR_FORM);
		GAS_BURNING_GENERATOR = block("gas_burning_generator", GAS_BURNING_GENERATOR_FORM);
		CHEMICAL_INFUSER = block("chemical_infuser", CHEMICAL_INFUSER_FORM);
		BULK_CHEMICAL_INFUSER = block("bulk_chemical_infuser", BULK_CHEMICAL_INFUSER_FORM);
		CHEMICAL_OXIDIZER = block("chemical_oxidizer", CHEMICAL_OXIDIZER_FORM);
		BULK_CHEMICAL_OXIDIZER = block("bulk_chemical_oxidizer", BULK_CHEMICAL_OXIDIZER_FORM);
		INDUSTRIAL_FERMENTER = block("industrial_fermenter", INDUSTRIAL_FERMENTER_FORM);
		BULK_FERMENTER = block("bulk_fermenter", BULK_FERMENTER_FORM);
		BULK_CHEMICAL_REACTOR = block("bulk_chemical_reactor", BULK_CHEMICAL_REACTOR_FORM);
		ANAEROBIC_DIGESTER = block("anaerobic_digester", ANAEROBIC_DIGESTER_FORM);
		COAL_GASIFIER = block("coal_gasifier", COAL_GASIFIER_FORM);
		GAS_CLEANUP_RACK = block("gas_cleanup_rack", GAS_CLEANUP_RACK_FORM);
		BIO_GENERATOR = block("bio_generator", BIO_GENERATOR_FORM);
		LARGE_BIO_GENERATOR = block("large_bio_generator", LARGE_BIO_GENERATOR_FORM);
		LARGE_GAS_BURNING_GENERATOR = block("large_gas_burning_generator", LARGE_GAS_BURNING_GENERATOR_FORM);
		GAS_CLEANING_RACK = block("gas_cleaning_rack", GAS_CLEANING_RACK_FORM);
		BIOGAS_CLEANER = block("biogas_cleaner", BIOGAS_CLEANER_FORM);
		DIGESTATE_SEPARATOR = block("digestate_separator", DIGESTATE_SEPARATOR_FORM);
		MASHING_STATION = block("mashing_station", MASHING_STATION_FORM);
		CHEMICAL_DISTILLATION_STATION = block("chemical_distillation_station", CHEMICAL_DISTILLATION_STATION_FORM);
		BAUXITE_DIGESTER = block("bauxite_digester", BAUXITE_DIGESTER_FORM);
		ALUMINATE_FILTER_PRESS = block("aluminate_filter_press", ALUMINATE_FILTER_PRESS_FORM);
		ALUMINA_CALCINER = block("alumina_calciner", ALUMINA_CALCINER_FORM);
		ALUMINUM_REDUCTION_CELL = block("aluminum_reduction_cell", ALUMINUM_REDUCTION_CELL_FORM);
		COASTAL_SEAWATER_INTAKE = block("coastal_seawater_intake", COASTAL_SEAWATER_INTAKE_FORM);
		BRINE_CONCENTRATOR = block("brine_concentrator", BRINE_CONCENTRATOR_FORM);
		MINERAL_CONDITIONER = block("mineral_conditioner", MINERAL_CONDITIONER_FORM);
		CHLORIDE_DRYER = block("chloride_dryer", CHLORIDE_DRYER_FORM);
		MAGNESIUM_ELECTROLYSIS_CELL = block("magnesium_electrolysis_cell", MAGNESIUM_ELECTROLYSIS_CELL_FORM);
		TITANIUM_CHLORINATOR = block("titanium_chlorinator", TITANIUM_CHLORINATOR_FORM);
		TITANIUM_REDUCTION_RETORT = block("titanium_reduction_retort", TITANIUM_REDUCTION_RETORT_FORM);
		POLYMER_MOLDING_PRESS = block("polymer_molding_press", POLYMER_MOLDING_PRESS_FORM);
		MONOMER_PREPARATION_UNIT = block("monomer_preparation_unit", MONOMER_PREPARATION_UNIT_FORM);
		POLYMERIZATION_KETTLE = block("polymerization_kettle", POLYMERIZATION_KETTLE_FORM);
		POLYMER_EXTRUDER = block("polymer_extruder", POLYMER_EXTRUDER_FORM);
		POLYMER_RECLAIMER = block("polymer_reclaimer", POLYMER_RECLAIMER_FORM);
		ACID_CONCENTRATOR = block("acid_concentrator", ACID_CONCENTRATOR_FORM);
		CHEMICAL_DISSOLUTION_CHAMBER = block("chemical_dissolution_chamber", CHEMICAL_DISSOLUTION_CHAMBER_FORM);
		FLUOROCARBON_REACTOR = block("fluorocarbon_reactor", FLUOROCARBON_REACTOR_FORM);
		FLUOROMONOMER_CRACKER = block("fluoromonomer_cracker", FLUOROMONOMER_CRACKER_FORM);
		PTFE_SINTERING_OVEN = block("ptfe_sintering_oven", PTFE_SINTERING_OVEN_FORM);
		LINED_FLUOROCHEMICAL_REACTOR = block("lined_fluorochemical_reactor", LINED_FLUOROCHEMICAL_REACTOR_FORM);
		COMBINED_CYCLE_PLANT = block("combined_cycle_power_plant", COMBINED_CYCLE_PLANT_FORM);
		PRECISION_FINISHER = block("precision_finisher", PRECISION_FINISHER_FORM);
		WET_PROCESSING_BENCH = block("wet_processing_bench", WET_PROCESSING_BENCH_FORM);
		List<FormMachineBlock> blocks = List.of(ELECTROLYTIC_SEPARATOR, GAS_BURNING_GENERATOR, CHEMICAL_INFUSER, CHEMICAL_OXIDIZER, INDUSTRIAL_FERMENTER, BULK_CHEMICAL_REACTOR, ANAEROBIC_DIGESTER, COAL_GASIFIER, GAS_CLEANUP_RACK, BIO_GENERATOR, GAS_CLEANING_RACK, BIOGAS_CLEANER, DIGESTATE_SEPARATOR, MASHING_STATION, CHEMICAL_DISTILLATION_STATION, BAUXITE_DIGESTER, ALUMINATE_FILTER_PRESS, ALUMINA_CALCINER, ALUMINUM_REDUCTION_CELL, COASTAL_SEAWATER_INTAKE, BRINE_CONCENTRATOR, MINERAL_CONDITIONER, CHLORIDE_DRYER, MAGNESIUM_ELECTROLYSIS_CELL, TITANIUM_CHLORINATOR, TITANIUM_REDUCTION_RETORT, POLYMER_MOLDING_PRESS, MONOMER_PREPARATION_UNIT, POLYMERIZATION_KETTLE, POLYMER_EXTRUDER, POLYMER_RECLAIMER, ACID_CONCENTRATOR, CHEMICAL_DISSOLUTION_CHAMBER, FLUOROCARBON_REACTOR, FLUOROMONOMER_CRACKER, PTFE_SINTERING_OVEN, LINED_FLUOROCHEMICAL_REACTOR, PRECISION_FINISHER, WET_PROCESSING_BENCH, COMBINED_CYCLE_PLANT, BULK_FERMENTER, LARGE_BIO_GENERATOR, LARGE_GAS_BURNING_GENERATOR, BULK_CHEMICAL_INFUSER, BULK_CHEMICAL_OXIDIZER, BULK_ELECTROLYTIC_SEPARATOR);
		ENTITY = JugcraftForms.registerBlocks(Jugcraft.id("machine_form"), blocks.toArray(FormMachineBlock[]::new));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> blocks.stream().filter(block -> !isSpecialist(block.form())).forEach(output::accept));
	}

	/** Old specialist blocks remain registered and craftable for existing factories. */
	public static boolean isSpecialist(MachineForm form) {
		return switch (form.id().getPath()) {
			case "acid_concentrator", "biogas_cleaner", "digestate_separator", "chloride_dryer", "ptfe_sintering_oven" -> true;
			default -> false;
		};
	}

	private static FormMachineBlock block(String path, MachineForm form) {
		// Like the machines: the furnace's light while running, given off by the controller alone although every part
		// lights its lamps and strips, and no hiding of neighbours' faces by a detailed model.
		BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofFullCopy(Blocks.FURNACE).noOcclusion()
				.lightLevel(state -> state.getValue(MachineBlock.LIT) && state.getBlock() instanceof FormMachineBlock machine
						&& machine.part(state) == 0 ? 13 : 0)
				.setId(ResourceKey.create(Registries.BLOCK, Jugcraft.id(path)));
		FormMachineBlock block = Registry.register(BuiltInRegistries.BLOCK, Jugcraft.id(path), new FormMachineBlock(properties, form));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(path));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		return block;
	}
}
