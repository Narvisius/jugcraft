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
	public static final MachineForm ELECTROLYTIC_SEPARATOR_FORM = MachineForm.builder(Jugcraft.id("electrolytic_separator"),
					MachineKind.ELECTROLYTIC_CELL)
			.layer("C#", "##")
			.layer("##", "##")
			.layer("##", "##")
			.profile(OperatingProfile.ENTRY)
			.inputTank("feed")
			.outputTank("anode_gas")
			.outputTank("hydrogen")
			.outputTank("lye")
			.capability(AQUEOUS_ELECTROLYSIS)
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
	public static final MachineForm GAS_BURNING_GENERATOR_FORM = MachineForm.builder(Jugcraft.id("gas_burning_generator"),
					MachineKind.GAS_TURBINE)
			.layer("C###", "####", "####", "####", "####")
			.layer(".##.", ".##.", ".##.", "####", "####")
			.layer(".##.", ".##.", ".##.", "####", "####")
			.layer("....", "....", "....", "....", "..##")
			.profile(OperatingProfile.ENTRY)
			.inputTank("fuel")
			.fuel(Jugcraft.id("hydrogen"), FluidFuels.HYDROGEN, FluidFuels.GAS_BURNING_HYDROGEN)
			.fuel(Jugcraft.id("methane"), FluidFuels.METHANE, FluidFuels.GAS_BURNING_METHANE)
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
	public static final MachineForm CHEMICAL_INFUSER_FORM = MachineForm.builder(Jugcraft.id("chemical_infuser"),
					MachineKind.SYNTHESIS_CONVERTER)
			.layer("C#", "##", "##")
			.layer("##", "##", "##")
			.layer("##", "##", "##")
			.profile(OperatingProfile.ENTRY)
			.inputTank("hydrogen")
			.inputTank("reactant")
			.outputTank("product")
			.outputTank("water")
			.socket("bed", CATALYST_BEDS)
			.capability(GAS_COMBINATION)
			.capability(NICKEL_METHANATION)
			.capability(CONTACT_CONVERSION)
			.upgrades()
			.warmup(40)
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
	public static final MachineForm CHEMICAL_OXIDIZER_FORM = MachineForm.builder(Jugcraft.id("chemical_oxidizer"),
					MachineKind.CHEMICAL_REACTOR)
			.layer("C#", "##")
			.layer("##", "##")
			.layer("##", "##")
			.profile(OperatingProfile.ENTRY)
			.inputTank("oxidant")
			.outputTank("product")
			.items(1, 0)
			.capability(SOLID_OXIDATION)
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
	public static final MachineForm INDUSTRIAL_FERMENTER_FORM = MachineForm.builder(Jugcraft.id("industrial_fermenter"),
					MachineKind.CHEMICAL_REACTOR)
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
			.capability(CONTROLLED_FERMENTATION)
			.upgrades()
			.warmup(40)
			.port("feed_in", FormPort.Kind.ITEM_IN, 0, 0, 1, 3, FormSide.TOP)
			.port("water_in", FormPort.Kind.FLUID_IN, 0, 0, 2, 1, FormSide.BACK)
			.port("product_out", FormPort.Kind.FLUID_OUT, 0, 1, 0, 0, FormSide.FRONT)
			.port("co2_out", FormPort.Kind.FLUID_OUT, 1, 1, 2, 3, FormSide.BACK)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 2, 2, 0, FormSide.BACK)
			.build();

	/**
	 * The Bulk Chemical Reactor: the Chemical Reactor enlarged (the chemical reactor family's energy figures) into a
	 * four-block cube in the expanded profile, two batches at once from 8,000 mB tanks. Water or acid goes in at the side,
	 * gas at the short absorption inlet on the lid and ore at the hatch beside it; acid leaves at the outlet low at the
	 * front and washed ore at the chute beside it.
	 */
	public static final MachineForm BULK_CHEMICAL_REACTOR_FORM = MachineForm.builder(Jugcraft.id("bulk_chemical_reactor"),
					MachineKind.CHEMICAL_REACTOR)
			.layer("C###", "####", "####", "####")
			.layer("####", "####", "####", "####")
			.layer("####", "####", "####", "####")
			.layer("####", "####", "####", "####")
			.profile(OperatingProfile.EXPANDED)
			.inputTank("liquid")
			.inputTank("gas")
			.outputTank("product")
			.items(1, 1)
			.capability(ACID_ABSORPTION)
			.capability(CHLORIDE_LEACHING)
			.capability(VANADIUM_RECOVERY)
			.capability(NITRATE_NEUTRALIZATION)
			.upgrades()
			.warmup(40)
			.port("feed_in", FormPort.Kind.ITEM_IN, 0, 1, 1, 3, FormSide.TOP)
			.port("liquid_in", FormPort.Kind.FLUID_IN, 0, 0, 2, 0, FormSide.LEFT)
			.port("gas_in", FormPort.Kind.FLUID_IN, 1, 2, 2, 3, FormSide.TOP)
			.port("product_out", FormPort.Kind.FLUID_OUT, 0, 2, 0, 0, FormSide.FRONT)
			.port("items_out", FormPort.Kind.ITEM_OUT, FormPort.ALL, 1, 0, 0, FormSide.FRONT)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 3, 3, 0, FormSide.RIGHT)
			.build();

	/**
	 * The Anaerobic Digester: the owner's selected substantial first bulk digester, a six-block cube of the chemical
	 * reactor family (its energy figures) in the bulk profile, four batches at once from 16,000 mB tanks. Waste goes in at
	 * the feed auger and water low on the left; raw biogas leaves at the header flange high on the right and digestate at
	 * the chute low on the right; the power socket is at the back.
	 */
	public static final MachineForm ANAEROBIC_DIGESTER_FORM = MachineForm.builder(Jugcraft.id("anaerobic_digester"),
					MachineKind.CHEMICAL_REACTOR)
			.layer("C#####", "######", "######", "######", "######", "######")
			.layer("######", "######", "######", "######", "######", "######")
			.layer("######", "######", "######", "######", "######", "######")
			.layer("######", "######", "######", "######", "######", "######")
			.layer("######", "######", "######", "######", "######", "######")
			.layer("######", "######", "######", "######", "######", "######")
			.profile(OperatingProfile.BULK)
			.inputTank("water")
			.outputTank("biogas")
			.items(1, 1)
			.capability(ANAEROBIC_DIGESTION)
			.upgrades()
			.warmup(40)
			.port("feed_in", FormPort.Kind.ITEM_IN, 0, 0, 2, 1, FormSide.LEFT)
			.port("water_in", FormPort.Kind.FLUID_IN, 0, 0, 4, 0, FormSide.LEFT)
			.port("biogas_out", FormPort.Kind.FLUID_OUT, 0, 5, 4, 5, FormSide.RIGHT)
			.port("digestate_out", FormPort.Kind.ITEM_OUT, FormPort.ALL, 5, 2, 0, FormSide.RIGHT)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 3, 5, 0, FormSide.BACK)
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
			.capability(COAL_GASIFICATION)
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
	public static final MachineForm GAS_CLEANUP_RACK_FORM = MachineForm.builder(Jugcraft.id("gas_cleanup_rack"),
					MachineKind.SYNTHESIS_CONVERTER)
			.layer("C##", "###", "###", "###", "###")
			.layer("###", "###", "###", "###", "###")
			.layer("###", "###", "###", "###", "###")
			.layer("###", "###", "###", "###", "###")
			.profile(OperatingProfile.ENTRY)
			.inputTank("feed")
			.outputTank("product")
			.outputTank("hydrogen")
			.capability(GAS_SEPARATION)
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
	public static final MachineForm BIO_GENERATOR_FORM = MachineForm.builder(Jugcraft.id("bio_generator"),
					MachineKind.DIESEL_GENERATOR)
			.layer("C##", "###", "###", "###")
			.layer("###", "###", "###", "###")
			.layer(".#.", ".#.", ".#.", "###")
			.profile(OperatingProfile.ENTRY)
			.inputTank("fuel")
			.fuel(Jugcraft.id("bioethanol"), FluidFuels.BIOETHANOL, FluidFuels.BIO_GENERATOR_BIOETHANOL)
			.fuel(Jugcraft.id("biogas"), FluidFuels.BIOGAS, FluidFuels.BIO_GENERATOR_BIOGAS)
			.port("fuel_in", FormPort.Kind.FLUID_IN, 0, 2, 2, 0, FormSide.RIGHT)
			.port("power_out", FormPort.Kind.ENERGY_OUT, 0, 0, 2, 0, FormSide.LEFT)
			.build();

	public static FormMachineBlock ELECTROLYTIC_SEPARATOR;
	public static FormMachineBlock GAS_BURNING_GENERATOR;
	public static FormMachineBlock CHEMICAL_INFUSER;
	public static FormMachineBlock CHEMICAL_OXIDIZER;
	public static FormMachineBlock INDUSTRIAL_FERMENTER;
	public static FormMachineBlock BULK_CHEMICAL_REACTOR;
	public static FormMachineBlock ANAEROBIC_DIGESTER;
	public static FormMachineBlock COAL_GASIFIER;
	public static FormMachineBlock GAS_CLEANUP_RACK;
	public static FormMachineBlock BIO_GENERATOR;
	public static BlockEntityType<FormMachineBlockEntity> ENTITY;

	private IndustrialForms() {
	}

	public static void register() {
		ELECTROLYTIC_SEPARATOR = block("electrolytic_separator", ELECTROLYTIC_SEPARATOR_FORM);
		GAS_BURNING_GENERATOR = block("gas_burning_generator", GAS_BURNING_GENERATOR_FORM);
		CHEMICAL_INFUSER = block("chemical_infuser", CHEMICAL_INFUSER_FORM);
		CHEMICAL_OXIDIZER = block("chemical_oxidizer", CHEMICAL_OXIDIZER_FORM);
		INDUSTRIAL_FERMENTER = block("industrial_fermenter", INDUSTRIAL_FERMENTER_FORM);
		BULK_CHEMICAL_REACTOR = block("bulk_chemical_reactor", BULK_CHEMICAL_REACTOR_FORM);
		ANAEROBIC_DIGESTER = block("anaerobic_digester", ANAEROBIC_DIGESTER_FORM);
		COAL_GASIFIER = block("coal_gasifier", COAL_GASIFIER_FORM);
		GAS_CLEANUP_RACK = block("gas_cleanup_rack", GAS_CLEANUP_RACK_FORM);
		BIO_GENERATOR = block("bio_generator", BIO_GENERATOR_FORM);
		List<FormMachineBlock> blocks = List.of(ELECTROLYTIC_SEPARATOR, GAS_BURNING_GENERATOR, CHEMICAL_INFUSER, CHEMICAL_OXIDIZER,
				INDUSTRIAL_FERMENTER, BULK_CHEMICAL_REACTOR, ANAEROBIC_DIGESTER, COAL_GASIFIER, GAS_CLEANUP_RACK, BIO_GENERATOR);
		ENTITY = JugcraftForms.registerBlocks(Jugcraft.id("machine_form"), blocks.toArray(FormMachineBlock[]::new));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> blocks.forEach(output::accept));
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
