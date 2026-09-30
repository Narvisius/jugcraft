package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.fluid.ElectricPumpBlockEntity;
import io.github.jimbozoomer.jugcraft.fluid.FluidTankBlockEntity;
import io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.LargeMachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.MachineRecipe;
import io.github.jimbozoomer.jugcraft.machine.MachineRecipes;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * In-game tests run by `./gradlew build` on a headless server (Fabric game test API).
 * Each builds a small setup in an empty test area and waits for the expected result.
 */
public class JugcraftGameTests {
	private static Item item(String path) {
		return BuiltInRegistries.ITEM.getValue(Jugcraft.id(path));
	}

	private static void charge(GameTestHelper helper, BlockPos relative, Direction side) {
		EnergyStorage storage = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(relative), side);
		helper.assertTrue(storage instanceof SimpleEnergyStorage, "No energy storage at " + relative + " on " + side);
		((SimpleEnergyStorage) storage).setAmount(storage.getCapacity());
	}

	private static BlockState machine(MachineKind kind) {
		return JugcraftMachines.MACHINES.get(kind).defaultBlockState();
	}

	/** Recipes are data-driven: the crushing recipe for tin ore loads from data/jugcraft/recipe/crushing. */
	@GameTest
	public void crushingRecipesLoad(GameTestHelper helper) {
		MachineRecipe recipe = MachineRecipes.find(helper.getLevel(), MachineKind.CRUSHER, new ItemStack(item("tin_ore")))
				.orElseThrow(() -> helper.assertionException("No crushing recipe for tin ore"));
		ItemStack output = recipe.output().create();
		helper.assertTrue(output.is(item("raw_tin")) && output.getCount() == 2, "Tin ore should crush into 2 raw tin, got " + output);
		helper.succeed();
	}

	/** A powered crusher turns one ore into two raw ore. */
	@GameTest(maxTicks = 400)
	public void crusherDoublesOre(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 1, 2);
		helper.setBlock(pos, machine(MachineKind.CRUSHER));
		charge(helper, pos, Direction.UP);
		MachineBlockEntity crusher = helper.getBlockEntity(pos, MachineBlockEntity.class);
		crusher.setItem(0, new ItemStack(item("tin_ore")));
		helper.succeedWhen(() -> {
			ItemStack output = crusher.getItem(MachineKind.CRUSHER.outputSlot());
			helper.assertTrue(output.is(item("raw_tin")) && output.getCount() == 2, "Crusher output is " + output);
		});
	}

	/** Coal generator -> copper cables -> electric furnace smelts raw iron. */
	@GameTest(maxTicks = 600)
	public void cablesCarryPower(GameTestHelper helper) {
		BlockPos generator = new BlockPos(1, 1, 3);
		BlockPos furnace = new BlockPos(5, 1, 3);
		helper.setBlock(generator, machine(MachineKind.COAL_GENERATOR));
		for (int x = 2; x <= 4; x++) {
			helper.setBlock(new BlockPos(x, 1, 3), JugcraftMachines.COPPER_CABLE);
		}
		helper.setBlock(furnace, machine(MachineKind.ELECTRIC_FURNACE));
		helper.getBlockEntity(generator, MachineBlockEntity.class).setItem(0, new ItemStack(Items.COAL));
		MachineBlockEntity electricFurnace = helper.getBlockEntity(furnace, MachineBlockEntity.class);
		electricFurnace.setItem(0, new ItemStack(Items.RAW_IRON));
		helper.succeedWhen(() -> {
			ItemStack output = electricFurnace.getItem(MachineKind.ELECTRIC_FURNACE.outputSlot());
			helper.assertTrue(output.is(Items.IRON_INGOT), "Electric furnace output is " + output);
		});
	}

	/** An electric pump on water pushes it through bronze pipes into a tank. */
	@GameTest(maxTicks = 200)
	public void pumpFillsTankThroughPipes(GameTestHelper helper) {
		helper.setBlock(new BlockPos(1, 1, 3), Blocks.WATER);
		BlockPos pump = new BlockPos(1, 2, 3);
		helper.setBlock(pump, JugcraftFluids.ELECTRIC_PUMP);
		helper.getBlockEntity(pump, ElectricPumpBlockEntity.class).energy().setAmount(ElectricPumpBlockEntity.ENERGY_CAPACITY);
		for (int x = 2; x <= 3; x++) {
			helper.setBlock(new BlockPos(x, 2, 3), JugcraftFluids.BRONZE_FLUID_PIPE);
		}
		BlockPos tank = new BlockPos(4, 2, 3);
		helper.setBlock(tank, JugcraftFluids.FLUID_TANK);
		FluidTankBlockEntity tankEntity = helper.getBlockEntity(tank, FluidTankBlockEntity.class);
		helper.succeedWhen(() -> helper.assertTrue(tankEntity.storage.amount > 0, "Tank is still empty"));
	}

	/** The 2x2 alloy smelter: places all four blocks, takes power only at its socket, makes bronze. */
	@GameTest(maxTicks = 400)
	public void alloySmelterMakesBronze(GameTestHelper helper) {
		BlockPos master = new BlockPos(4, 1, 3);
		LargeMachineBlock block = (LargeMachineBlock) JugcraftMachines.MACHINES.get(MachineKind.ALLOY_SMELTER);
		BlockState state = block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH);
		helper.setBlock(master, state);
		block.setPlacedBy(helper.getLevel(), helper.absolutePos(master), helper.getBlockState(master), null, ItemStack.EMPTY);
		BlockPos right = master.west();
		for (BlockPos part : new BlockPos[] {master, right, master.above(), right.above()}) {
			helper.assertBlockPresent(block, part);
		}
		helper.assertTrue(EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(master), Direction.NORTH) == null,
				"The alloy smelter must not take power at its front");
		charge(helper, right, Direction.WEST);
		MachineBlockEntity smelter = helper.getBlockEntity(master, MachineBlockEntity.class);
		smelter.setItem(0, new ItemStack(Items.COPPER_INGOT, 3));
		smelter.setItem(1, new ItemStack(item("tin_ingot")));
		helper.succeedWhen(() -> {
			ItemStack output = smelter.getItem(MachineKind.ALLOY_SMELTER.outputSlot());
			helper.assertTrue(output.is(item("bronze_ingot")) && output.getCount() == 4, "Alloy smelter output is " + output);
		});
	}

	/** Breaking any block of a multi-block machine removes the whole machine. */
	@GameTest(maxTicks = 40)
	public void breakingOnePartRemovesTheMachine(GameTestHelper helper) {
		BlockPos base = new BlockPos(3, 1, 3);
		LargeMachineBlock block = (LargeMachineBlock) JugcraftMachines.MACHINES.get(MachineKind.WIND_TURBINE);
		helper.setBlock(base, block.defaultBlockState());
		block.setPlacedBy(helper.getLevel(), helper.absolutePos(base), helper.getBlockState(base), null, ItemStack.EMPTY);
		helper.assertBlockPresent(block, base.above(2));
		helper.destroyBlock(base.above(2));
		helper.succeedWhen(() -> {
			for (int y = 0; y < 3; y++) {
				helper.assertBlockNotPresent(block, base.above(y));
			}
		});
	}
}
