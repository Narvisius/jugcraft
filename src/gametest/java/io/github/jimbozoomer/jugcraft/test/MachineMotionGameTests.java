package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.EnlargedMachineBlock;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

/**
 * In-game test for what the turning machine parts (client/MachineRotors) are drawn from: a formed giant sawmill and a
 * formed giant sieve (compact=false, the only state the rotors draw on) that are powered and given work light their
 * master block while they run and make their products, the sawmill planks and the sieve flint.
 */
public class MachineMotionGameTests {
	private static MachineBlockEntity formed(GameTestHelper helper, MachineKind kind, BlockPos master, ItemStack input) {
		EnlargedMachineBlock block = (EnlargedMachineBlock) JugcraftMachines.MACHINES.get(kind);
		helper.setBlock(master, block.formed(block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH)));
		block.setPlacedBy(helper.getLevel(), helper.absolutePos(master), helper.getBlockState(master), null, ItemStack.EMPTY);
		MachineBlockEntity machine = helper.getBlockEntity(master, MachineBlockEntity.class);
		helper.assertTrue(machine.energyFor(null) instanceof SimpleEnergyStorage, "The " + kind.id + " has no energy storage");
		SimpleEnergyStorage energy = (SimpleEnergyStorage) machine.energyFor(null);
		energy.setAmount(energy.getCapacity());
		machine.setItem(0, input);
		return machine;
	}

	/** Both machines light their master block while they work, and make their products. */
	@GameTest(maxTicks = 600)
	public void giantSawmillAndSieveLightTheirMasterWhileRunning(GameTestHelper helper) {
		BlockPos sawmillPos = new BlockPos(5, 1, 1);
		BlockPos sievePos = new BlockPos(2, 1, 1);
		MachineBlockEntity sawmill = formed(helper, MachineKind.SAWMILL, sawmillPos, new ItemStack(Items.OAK_LOG, 16));
		MachineBlockEntity sieve = formed(helper, MachineKind.SIEVE, sievePos, new ItemStack(Items.GRAVEL, 16));
		helper.succeedWhen(() -> {
			for (BlockPos pos : new BlockPos[] {sawmillPos, sievePos}) {
				BlockState state = helper.getBlockState(pos);
				helper.assertTrue(!state.getValue(EnlargedMachineBlock.COMPACT), "The machine at " + pos + " is compact: " + state);
				helper.assertTrue(state.getValue(MachineBlock.LIT), "The machine at " + pos + " is not lit while it works: " + state);
			}
			ItemStack planks = sawmill.getItem(MachineKind.SAWMILL.outputSlot());
			helper.assertTrue(planks.is(Items.OAK_PLANKS) && planks.getCount() >= 6, "Sawmill output is " + planks);
			ItemStack flint = sieve.getItem(MachineKind.SIEVE.outputSlot());
			helper.assertTrue(flint.is(Items.FLINT), "Sieve output is " + flint);
		});
	}
}
