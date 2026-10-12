package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.form.FormMachineBlock;
import io.github.jimbozoomer.jugcraft.machine.form.FormMachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.form.FormPort;
import io.github.jimbozoomer.jugcraft.machine.form.FormSide;
import io.github.jimbozoomer.jugcraft.machine.form.JugcraftForms;
import io.github.jimbozoomer.jugcraft.machine.form.MachineForm;
import io.github.jimbozoomer.jugcraft.machine.form.OperatingProfile;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Test-only machine forms for the industrial foundation (docs/features/industrial-machine-foundation.md), registered
 * by the test mod and never shipped: a small rig and a filled 6x6x6 hall. Both run the test mod's own
 * {@code jugcraft-test:test_split} recipes in the chemical reactor family, which the reactor itself never runs.
 */
public final class TestForms implements ModInitializer {
	public static final String NAMESPACE = "jugcraft-test";
	public static final String CAPABILITY = NAMESPACE + ":test_split";
	public static final TagKey<Item> TEST_BEDS = TagKey.create(Registries.ITEM, id("test_beds"));

	/**
	 * A 2x2x3 rig: four blocks on the ground (the controller front left), four above, and one clearance position on
	 * top of the controller for a moving part. Water in; hydrogen and oxygen out; one item input and output, one tool
	 * socket and two upgrade slots.
	 */
	public static final MachineForm RIG_FORM = MachineForm.builder(id("form_test_rig"), MachineKind.CHEMICAL_REACTOR)
			.layer("C#", "##")
			.layer("##", "##")
			.layer("~.", "..")
			.inputTank("water")
			.outputTank("hydrogen")
			.outputTank("oxygen")
			.items(1, 1)
			.socket("bed", TEST_BEDS)
			.upgrades()
			.capability(CAPABILITY)
			.warmup(5)
			.port("water_in", FormPort.Kind.FLUID_IN, 0, 0, 1, 0, FormSide.LEFT)
			.port("hydrogen_out", FormPort.Kind.FLUID_OUT, 0, 1, 1, 1, FormSide.RIGHT)
			.port("oxygen_out", FormPort.Kind.FLUID_OUT, 1, 1, 0, 1, FormSide.RIGHT)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 0, 0, 0, FormSide.LEFT)
			.port("items_in", FormPort.Kind.ITEM_IN, FormPort.ALL, 0, 1, 1, FormSide.TOP)
			.port("items_out", FormPort.Kind.ITEM_OUT, FormPort.ALL, 1, 0, 0, FormSide.FRONT)
			.build();

	/**
	 * A 6x6x6 hall in the bulk profile (four lanes, 16,000 mB tanks): a floor, hollow walls two layers high, a ring
	 * around sixteen clearance positions, a roof and a 2x2 chimney; 136 structural blocks with the controller in the
	 * front row, third from the left.
	 */
	public static final MachineForm HALL_FORM = MachineForm.builder(id("form_test_hall"), MachineKind.CHEMICAL_REACTOR)
			.layer("##C###", "######", "######", "######", "######", "######")
			.layer("######", "#....#", "#....#", "#....#", "#....#", "######")
			.layer("######", "#....#", "#....#", "#....#", "#....#", "######")
			.layer("######", "#~~~~#", "#~~~~#", "#~~~~#", "#~~~~#", "######")
			.layer("######", "######", "######", "######", "######", "######")
			.layer("..##..", "..##..", "......", "......", "......", "......")
			.profile(OperatingProfile.BULK)
			.inputTank("water")
			.outputTank("hydrogen")
			.outputTank("oxygen")
			.items(1, 1)
			.capability(CAPABILITY)
			.port("water_in", FormPort.Kind.FLUID_IN, 0, 0, 2, 0, FormSide.LEFT)
			.port("hydrogen_out", FormPort.Kind.FLUID_OUT, 0, 5, 2, 0, FormSide.RIGHT)
			.port("oxygen_out", FormPort.Kind.FLUID_OUT, 1, 5, 3, 0, FormSide.RIGHT)
			.port("power", FormPort.Kind.ENERGY_IN, 0, 2, 0, 0, FormSide.FRONT)
			.build();

	public static FormMachineBlock RIG;
	public static FormMachineBlock HALL;
	public static BlockEntityType<FormMachineBlockEntity> ENTITY;

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(NAMESPACE, path);
	}

	@Override
	public void onInitialize() {
		RIG = block("form_test_rig", RIG_FORM);
		HALL = block("form_test_hall", HALL_FORM);
		ENTITY = JugcraftForms.registerBlocks(id("form_test"), RIG, HALL);
	}

	private static FormMachineBlock block(String path, MachineForm form) {
		BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofFullCopy(Blocks.FURNACE).noOcclusion()
				.setId(ResourceKey.create(Registries.BLOCK, id(path)));
		FormMachineBlock block = Registry.register(BuiltInRegistries.BLOCK, id(path), new FormMachineBlock(properties, form));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id(path));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		return block;
	}
}
