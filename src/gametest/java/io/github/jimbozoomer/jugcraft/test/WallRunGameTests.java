package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids;
import io.github.jimbozoomer.jugcraft.fluid.FluidPipeBlock;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.wallrun.WallRun;
import io.github.jimbozoomer.jugcraft.wallrun.WallRunBlockEntity;
import io.github.jimbozoomer.jugcraft.wallrun.WallRuns;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pipes and cables in walls (wallrun/WallRuns): only made blocks take them; a pipe put into a wall keeps the wall's
 * look, joins the pipes either side (sleeves showing only where it comes out into the open), breaks into the wall's
 * block and the pipe, and the wrench pulls the pipe out leaving the wall; one built from a blueprint takes the look of
 * the wall round it.
 */
public class WallRunGameTests {
	@GameTest(maxTicks = 20)
	public void pipeRunsThroughAWall(GameTestHelper helper) {
		if (TestFocus.skip("wallruns")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockState bricks = Blocks.BRICKS.defaultBlockState();
		BlockPos probe = helper.absolutePos(new BlockPos(6, 1, 6));
		for (BlockState natural : List.of(Blocks.DIRT.defaultBlockState(), Blocks.STONE.defaultBlockState(), Blocks.SAND.defaultBlockState(),
				Blocks.GLASS.defaultBlockState(), Blocks.CHEST.defaultBlockState(), Blocks.IRON_ORE.defaultBlockState())) {
			helper.assertTrue(!WallRuns.isWall(natural, level, probe), "not a wall for a pipe: " + natural);
		}
		for (BlockState made : List.of(bricks, Blocks.STONE_BRICKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(),
				Blocks.IRON_BLOCK.defaultBlockState(), Blocks.POLISHED_ANDESITE.defaultBlockState())) {
			helper.assertTrue(WallRuns.isWall(made, level, probe), "a wall for a pipe: " + made);
		}
		// A wall two blocks thick (x 2..3), a pipe each side of it, a run through it.
		for (int x = 2; x <= 3; x++) {
			for (int y = 1; y <= 3; y++) {
				for (int z = 1; z <= 3; z++) {
					helper.setBlock(new BlockPos(x, y, z), bricks);
				}
			}
		}
		helper.setBlock(new BlockPos(1, 2, 2), JugcraftFluids.STEEL_FLUID_PIPE);
		helper.setBlock(new BlockPos(4, 2, 2), JugcraftFluids.STEEL_FLUID_PIPE);
		Block run = WallRuns.WALL_OF.get(JugcraftFluids.STEEL_FLUID_PIPE);
		for (int x = 2; x <= 3; x++) {
			WallRuns.put(level, helper.absolutePos(new BlockPos(x, 2, 2)), run, bricks);
		}
		for (int x = 1; x <= 4; x += 3) {
			BlockPos abs = helper.absolutePos(new BlockPos(x, 2, 2));
			level.setBlock(abs, Block.updateFromNeighbourShapes(level.getBlockState(abs), level, abs), Block.UPDATE_ALL);
		}
		BlockPos a = new BlockPos(2, 2, 2), b = new BlockPos(3, 2, 2);
		helper.assertBlockPresent(run, a);
		helper.assertTrue(helper.getBlockEntity(a, WallRunBlockEntity.class).disguise() == bricks, "it looks like the wall");
		helper.assertTrue(helper.getBlockState(a).isCollisionShapeFullBlock(level, helper.absolutePos(a)), "it is as solid as the wall");
		// Sleeves where it comes out (west of a, east of b), none between the two runs or into the wall round them.
		helper.assertBlockProperty(a, PipeBlock.WEST, true);
		helper.assertBlockProperty(a, PipeBlock.EAST, false);
		helper.assertBlockProperty(a, PipeBlock.NORTH, false);
		helper.assertBlockProperty(b, PipeBlock.EAST, true);
		helper.assertBlockProperty(b, PipeBlock.WEST, false);
		// The pipes either side join it; it is a pipe of the same network as them.
		helper.assertBlockProperty(new BlockPos(1, 2, 2), PipeBlock.EAST, true);
		helper.assertBlockProperty(new BlockPos(4, 2, 2), PipeBlock.WEST, true);
		helper.assertTrue(helper.getBlockState(a).getBlock() instanceof FluidPipeBlock pipe && pipe.transferRate()
				== ((FluidPipeBlock) JugcraftFluids.STEEL_FLUID_PIPE).transferRate(), "at the steel pipe's rate");
		// Broken with a pickaxe: the bricks and the pipe; by hand, just the pipe (bricks need a pickaxe).
		BlockPos absA = helper.absolutePos(a);
		List<ItemStack> picked = Block.getDrops(helper.getBlockState(a), level, absA, level.getBlockEntity(absA), null, new ItemStack(Items.IRON_PICKAXE));
		helper.assertTrue(picked.stream().anyMatch(s -> s.is(Items.BRICKS)) && picked.stream().anyMatch(s -> s.is(JugcraftFluids.STEEL_FLUID_PIPE.asItem())),
				"a pickaxe gets the bricks and the pipe: " + picked);
		List<ItemStack> hand = Block.getDrops(helper.getBlockState(a), level, absA, level.getBlockEntity(absA), null, ItemStack.EMPTY);
		helper.assertTrue(hand.stream().noneMatch(s -> s.is(Items.BRICKS)) && hand.stream().anyMatch(s -> s.is(JugcraftFluids.STEEL_FLUID_PIPE.asItem())),
				"a hand gets only the pipe: " + hand);
		// Ctrl + right-click pulls it out holding a block (the wall's, a pipe, a cable), not holding anything else.
		helper.assertTrue(WallRuns.canPull(new ItemStack(Items.BRICKS)) && WallRuns.canPull(new ItemStack(JugcraftMachines.COPPER_CABLE))
				&& !WallRuns.canPull(new ItemStack(Items.STICK)) && !WallRuns.canPull(ItemStack.EMPTY), "what Ctrl + right-click pulls it out with");
		// The wrench pulls the pipe out of b: the wall is plain bricks again.
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		WallRuns.pullOut(level, helper.absolutePos(b), player);
		helper.assertBlockPresent(Blocks.BRICKS, b);
		helper.assertBlockProperty(a, PipeBlock.EAST, false);
		// A block put round a laid cable.
		helper.setBlock(new BlockPos(5, 1, 5), JugcraftMachines.COPPER_CABLE);
		WallRuns.put(level, helper.absolutePos(new BlockPos(5, 1, 5)), WallRuns.WALL_OF.get(JugcraftMachines.COPPER_CABLE), Blocks.STONE_BRICKS.defaultBlockState());
		helper.assertTrue(helper.getBlockState(new BlockPos(5, 1, 5)).getBlock() instanceof WallRun, "the cable is in the wall");
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void builtRunTakesTheWallsLook(GameTestHelper helper) {
		if (TestFocus.skip("wallruns")) {
			helper.succeed();
			return;
		}
		for (int x = 1; x <= 3; x++) {
			for (int y = 1; y <= 3; y++) {
				helper.setBlock(new BlockPos(x, y, 2), Blocks.STONE_BRICKS);
			}
		}
		// As a blueprint build places it: the block alone, no look given.
		helper.setBlock(new BlockPos(2, 2, 2), WallRuns.WALL_OF.get(JugcraftFluids.BRONZE_FLUID_PIPE));
		helper.succeedWhen(() -> {
			WallRunBlockEntity run = helper.getBlockEntity(new BlockPos(2, 2, 2), WallRunBlockEntity.class);
			helper.assertTrue(run.hasDisguise() && run.disguise().is(Blocks.STONE_BRICKS), "it takes the stone bricks' look");
		});
	}
}
