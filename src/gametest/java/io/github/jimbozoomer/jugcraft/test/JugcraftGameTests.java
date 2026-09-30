package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.fluid.ElectricPumpBlockEntity;
import io.github.jimbozoomer.jugcraft.fluid.FluidTankBlockEntity;
import io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids;
import io.github.jimbozoomer.jugcraft.logistics.ItemSorterBlockEntity;
import io.github.jimbozoomer.jugcraft.logistics.JugcraftLogistics;
import io.github.jimbozoomer.jugcraft.logistics.PneumaticExtractorBlock;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.LargeMachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.MachineRecipe;
import io.github.jimbozoomer.jugcraft.machine.MachineRecipes;
import io.github.jimbozoomer.jugcraft.machine.SideConfig;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
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

	private static int count(ChestBlockEntity chest, Item item) {
		int total = 0;
		for (int slot = 0; slot < chest.getContainerSize(); slot++) {
			ItemStack stack = chest.getItem(slot);
			if (stack.is(item)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	private static ChestBlockEntity chest(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, Blocks.CHEST);
		return helper.getBlockEntity(pos, ChestBlockEntity.class);
	}

	private static void extractor(GameTestHelper helper, BlockPos pos, Direction intake) {
		helper.setBlock(pos, JugcraftLogistics.PNEUMATIC_EXTRACTOR.defaultBlockState().setValue(PneumaticExtractorBlock.FACING, intake));
	}

	/** Chest -> extractor -> brass item pipes -> chest. */
	@GameTest(maxTicks = 200)
	public void extractorMovesItemsThroughPipes(GameTestHelper helper) {
		ChestBlockEntity source = chest(helper, new BlockPos(1, 1, 3));
		source.setItem(0, new ItemStack(Items.IRON_INGOT, 5));
		extractor(helper, new BlockPos(2, 1, 3), Direction.WEST);
		for (int x = 3; x <= 4; x++) {
			helper.setBlock(new BlockPos(x, 1, 3), JugcraftLogistics.BRASS_ITEM_PIPE);
		}
		ChestBlockEntity target = chest(helper, new BlockPos(5, 1, 3));
		helper.succeedWhen(() -> helper.assertTrue(count(target, Items.IRON_INGOT) == 5,
				"Target chest has " + count(target, Items.IRON_INGOT) + " iron"));
	}

	/** A sorter set to iron takes the iron; everything else goes to the plain chest on the same pipes. */
	@GameTest(maxTicks = 300)
	public void sorterRoutesMatchingItems(GameTestHelper helper) {
		ChestBlockEntity source = chest(helper, new BlockPos(1, 1, 1));
		source.setItem(0, new ItemStack(Items.IRON_INGOT, 4));
		source.setItem(1, new ItemStack(Items.GOLD_INGOT, 4));
		extractor(helper, new BlockPos(2, 1, 1), Direction.WEST);
		for (int x = 3; x <= 5; x++) {
			helper.setBlock(new BlockPos(x, 1, 1), JugcraftLogistics.BRASS_ITEM_PIPE);
		}
		BlockPos sorter = new BlockPos(4, 1, 2);
		helper.setBlock(sorter, JugcraftLogistics.ITEM_SORTER.defaultBlockState().setValue(PneumaticExtractorBlock.FACING, Direction.SOUTH));
		helper.getBlockEntity(sorter, ItemSorterBlockEntity.class).setItem(0, new ItemStack(Items.IRON_INGOT));
		ChestBlockEntity sorted = chest(helper, new BlockPos(4, 1, 3));
		ChestBlockEntity overflow = chest(helper, new BlockPos(6, 1, 1));
		helper.succeedWhen(() -> {
			helper.assertTrue(count(sorted, Items.IRON_INGOT) == 4 && count(sorted, Items.GOLD_INGOT) == 0,
					"Sorted chest: " + count(sorted, Items.IRON_INGOT) + " iron, " + count(sorted, Items.GOLD_INGOT) + " gold");
			helper.assertTrue(count(overflow, Items.GOLD_INGOT) == 4 && count(overflow, Items.IRON_INGOT) == 0,
					"Overflow chest: " + count(overflow, Items.GOLD_INGOT) + " gold, " + count(overflow, Items.IRON_INGOT) + " iron");
		});
	}

	/** A crusher set to eject pushes its results into the chest below (its default output face). */
	@GameTest(maxTicks = 400)
	public void machineEjectsIntoChest(GameTestHelper helper) {
		ChestBlockEntity below = chest(helper, new BlockPos(2, 1, 2));
		BlockPos pos = new BlockPos(2, 2, 2);
		helper.setBlock(pos, machine(MachineKind.CRUSHER));
		charge(helper, pos, Direction.UP);
		MachineBlockEntity crusher = helper.getBlockEntity(pos, MachineBlockEntity.class);
		helper.assertTrue(crusher.clickSideButton(SideConfig.EJECT_BUTTON), "Eject button was refused");
		crusher.setItem(0, new ItemStack(item("tin_ore")));
		helper.succeedWhen(() -> helper.assertTrue(count(below, item("raw_tin")) == 2,
				"Chest below has " + count(below, item("raw_tin")) + " raw tin"));
	}
}
