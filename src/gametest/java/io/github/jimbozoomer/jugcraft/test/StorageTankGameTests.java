package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.nuclear.RailingBlock;
import io.github.jimbozoomer.jugcraft.nuclear.RailingGateBlock;
import io.github.jimbozoomer.jugcraft.nuclear.SteelLadderBlock;
import io.github.jimbozoomer.jugcraft.nuclear.StorageTankPlateBlock;
import io.github.jimbozoomer.jugcraft.nuclear.StorageTankPortBlock;
import io.github.jimbozoomer.jugcraft.nuclear.StorageTankPortBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.StorageTanks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * The storage tank MBS (docs/features/nuclear.md, "Storage tanks"): a small square tank forms round its space (the
 * interior filled, the plates painted by place, the inlet linked), holds a bucket per block inside, takes fluid in at
 * the inlet and gives it out at the outlet only while their valves are open, and comes apart when a plate is broken,
 * keeping its fluid. The railing joins like a fence (and to a gate along its line); a steel ladder is caged from its
 * third length up.
 */
public class StorageTankGameTests {
	private static void tank(GameTestHelper helper) {
		BlockState plate = StorageTanks.PLATE.defaultBlockState();
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.SMOOTH_STONE.defaultBlockState());
			}
		}
		// A 5 x 5 shell (x, z 1..5), walls 1..3 high, a roof at 4: 3 x 3 x 3 inside.
		for (int x = 1; x <= 5; x++) {
			for (int z = 1; z <= 5; z++) {
				helper.setBlock(new BlockPos(x, 4, z), plate);
				for (int y = 1; y <= 3; y++) {
					if (x == 1 || x == 5 || z == 1 || z == 5) {
						helper.setBlock(new BlockPos(x, y, z), plate);
					}
				}
			}
		}
		helper.setBlock(new BlockPos(3, 1, 0), StorageTanks.OUTLET.defaultBlockState().setValue(StorageTankPortBlock.FACING, Direction.NORTH));
		helper.setBlock(new BlockPos(6, 1, 3), StorageTanks.INLET.defaultBlockState().setValue(StorageTankPortBlock.FACING, Direction.EAST));
	}

	private static long move(Storage<FluidVariant> storage, boolean insert, long amount) {
		try (Transaction t = Transaction.openOuter()) {
			long moved = insert ? storage.insert(FluidVariant.of(Fluids.WATER), amount, t) : storage.extract(FluidVariant.of(Fluids.WATER), amount, t);
			t.commit();
			return moved;
		}
	}

	@GameTest(maxTicks = 40)
	public void formsHoldsAndComesApart(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		tank(helper);
		BlockPos outletPos = helper.absolutePos(new BlockPos(3, 1, 0)), inletPos = helper.absolutePos(new BlockPos(6, 1, 3));
		StorageTankPortBlockEntity outlet = (StorageTankPortBlockEntity) helper.getLevel().getBlockEntity(outletPos);
		helper.assertTrue(outlet != null, "the outlet has its block entity");
		outlet.check(helper.getLevel());
		helper.assertTrue(outlet.formed(), "the tank forms");
		helper.assertTrue(outlet.fluid.capacity() == 27 * FluidConstants.BUCKET, "a bucket per block inside: " + outlet.fluid.capacity());
		helper.assertBlockPresent(StorageTanks.INTERIOR, new BlockPos(3, 2, 3));
		helper.assertBlockPresent(StorageTanks.INTERIOR, new BlockPos(2, 1, 2));
		helper.assertBlockProperty(new BlockPos(3, 1, 1), StorageTankPlateBlock.BAND, StorageTankPlateBlock.Band.WALL);
		helper.assertBlockProperty(new BlockPos(3, 3, 1), StorageTankPlateBlock.BAND, StorageTankPlateBlock.Band.BAND);
		helper.assertBlockProperty(new BlockPos(3, 4, 3), StorageTankPlateBlock.BAND, StorageTankPlateBlock.Band.ROOF);
		helper.assertBlockProperty(new BlockPos(1, 4, 1), StorageTankPlateBlock.BAND, StorageTankPlateBlock.Band.RIM);
		helper.assertBlockProperty(new BlockPos(1, 1, 1), StorageTankPlateBlock.FORMED, true);
		helper.assertBlockProperty(new BlockPos(6, 1, 3), StorageTankPortBlock.FORMED, true);
		// In at the inlet (from its outer face), out at the outlet; not the other way.
		Storage<FluidVariant> in = FluidStorage.SIDED.find(helper.getLevel(), inletPos, Direction.EAST);
		Storage<FluidVariant> out = FluidStorage.SIDED.find(helper.getLevel(), outletPos, Direction.NORTH);
		helper.assertTrue(in != null && out != null, "pipes find the nozzles at their flanges");
		helper.assertTrue(FluidStorage.SIDED.find(helper.getLevel(), inletPos, Direction.WEST) == null, "only at the flange");
		helper.assertTrue(move(in, true, 5 * FluidConstants.BUCKET) == 5 * FluidConstants.BUCKET, "the inlet takes water in");
		helper.assertTrue(move(out, true, FluidConstants.BUCKET) == 0, "the outlet takes nothing in");
		helper.assertTrue(move(out, false, 2 * FluidConstants.BUCKET) == 2 * FluidConstants.BUCKET, "the outlet gives water out");
		helper.assertTrue(move(in, false, FluidConstants.BUCKET) == 0, "the inlet gives nothing out");
		// A shut valve stops it.
		helper.setBlock(new BlockPos(6, 1, 3), helper.getBlockState(new BlockPos(6, 1, 3)).setValue(StorageTankPortBlock.OPEN, false));
		helper.assertTrue(move(in, true, FluidConstants.BUCKET) == 0, "a shut inlet takes nothing");
		helper.assertTrue(outlet.fluid.amount == 3 * FluidConstants.BUCKET, "three buckets in the tank: " + outlet.fluid.amount);
		// A plate broken: the tank comes apart, keeps its water; mended, it forms again.
		helper.setBlock(new BlockPos(3, 2, 1), Blocks.AIR.defaultBlockState());
		outlet.check(helper.getLevel());
		helper.assertTrue(!outlet.formed(), "a broken shell is not a tank");
		helper.assertBlockNotPresent(StorageTanks.INTERIOR, new BlockPos(3, 2, 3));
		helper.assertBlockProperty(new BlockPos(1, 1, 1), StorageTankPlateBlock.FORMED, false);
		helper.assertBlockProperty(new BlockPos(6, 1, 3), StorageTankPortBlock.FORMED, false);
		helper.assertTrue(outlet.fluid.amount == 3 * FluidConstants.BUCKET, "the outlet keeps the water");
		helper.setBlock(new BlockPos(3, 2, 1), StorageTanks.PLATE.defaultBlockState());
		outlet.check(helper.getLevel());
		helper.assertTrue(outlet.formed() && outlet.fluid.amount == 3 * FluidConstants.BUCKET, "mended, it is a tank again with its water");
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void railingJoinsAndLadderCages(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		// An L of railing with a gate in its long arm.
		helper.setBlock(new BlockPos(1, 1, 1), StorageTanks.RAILING);
		helper.setBlock(new BlockPos(1, 1, 2), StorageTanks.RAILING);
		helper.setBlock(new BlockPos(2, 1, 2), StorageTanks.RAILING);
		helper.setBlock(new BlockPos(3, 1, 2), StorageTanks.GATE.defaultBlockState().setValue(RailingGateBlock.FACING, Direction.NORTH));
		helper.setBlock(new BlockPos(4, 1, 2), StorageTanks.RAILING);
		for (BlockPos p : new BlockPos[] {new BlockPos(1, 1, 1), new BlockPos(1, 1, 2), new BlockPos(2, 1, 2), new BlockPos(4, 1, 2)}) {
			BlockPos abs = helper.absolutePos(p);
			helper.getLevel().setBlock(abs, Block.updateFromNeighbourShapes(helper.getBlockState(p), helper.getLevel(), abs), Block.UPDATE_ALL);
		}
		helper.assertBlockProperty(new BlockPos(1, 1, 2), RailingBlock.NORTH, true);
		helper.assertBlockProperty(new BlockPos(1, 1, 2), RailingBlock.EAST, true);
		helper.assertBlockProperty(new BlockPos(1, 1, 2), RailingBlock.POST, true);
		helper.assertBlockProperty(new BlockPos(2, 1, 2), RailingBlock.EAST, true);
		helper.assertBlockProperty(new BlockPos(4, 1, 2), RailingBlock.WEST, true);
		helper.assertBlockProperty(new BlockPos(1, 1, 1), RailingBlock.SOUTH, true);
		// A ladder up a wall: plain for two lengths, caged above, the top one marked.
		for (int y = 1; y <= 4; y++) {
			helper.setBlock(new BlockPos(6, y, 6), Blocks.STONE);
		}
		for (int y = 1; y <= 4; y++) {
			BlockPos p = new BlockPos(6, y, 5);
			helper.setBlock(p, StorageTanks.LADDER.defaultBlockState().setValue(SteelLadderBlock.FACING, Direction.NORTH));
		}
		for (int y = 1; y <= 4; y++) {
			BlockPos abs = helper.absolutePos(new BlockPos(6, y, 5));
			helper.getLevel().setBlock(abs, SteelLadderBlock.shaped(helper.getLevel().getBlockState(abs), helper.getLevel(), abs), Block.UPDATE_ALL);
		}
		helper.assertBlockProperty(new BlockPos(6, 2, 5), SteelLadderBlock.CAGE, false);
		helper.assertBlockProperty(new BlockPos(6, 3, 5), SteelLadderBlock.CAGE, true);
		helper.assertBlockProperty(new BlockPos(6, 4, 5), SteelLadderBlock.TOP, true);
		helper.assertBlockProperty(new BlockPos(6, 3, 5), SteelLadderBlock.TOP, false);
		helper.assertTrue(helper.getBlockState(new BlockPos(6, 2, 5)).is(net.minecraft.tags.BlockTags.CLIMBABLE), "the steel ladder climbs");
		helper.succeed();
	}
}
