package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.SurfaceTaskLightBlock;
import io.github.jimbozoomer.jugcraft.tower.TowerCableTrayBlock;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class TowerFixturesGameTests {
	@GameTest
	public void wallFixturesPreserveSolidBackingInEveryRotation(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel();
		BlockPos backing = helper.absolutePos(new BlockPos(3, 3, 3));
		var block = JugcraftTower.BLOCKS.get("wall_task_light");
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			level.setBlock(backing, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
			BlockPos light = backing.relative(facing);
			var state = block.defaultBlockState().setValue(SurfaceTaskLightBlock.FACING, facing);
			helper.assertTrue(state.canSurvive(level, light), "wall light uses the real solid backing surface");
			level.setBlock(light, state, Block.UPDATE_ALL);
			helper.assertTrue(level.getBlockState(backing).is(Blocks.STONE), "fixture never replaces the wall with an open hole");
			helper.assertTrue(state.getLightEmission() == 14 && state.getShape(level, light).bounds().getSize() < 1,
					"small physical fixture emits useful side-facing light");
			level.removeBlock(backing, false);
			helper.assertTrue(level.getBlockState(light).isAir(), "unsupported fixture does not remain floating");
		}
		helper.succeed();
	}

	@GameTest
	public void trayJunctionsJoinAndReconfigureAfterRealNeighborChanges(GameTestHelper helper) {
		if (TestFocus.skip("tower")) { helper.succeed(); return; }
		var level = helper.getLevel();
		var block = (TowerCableTrayBlock) JugcraftTower.BLOCKS.get("cable_tray");
		BlockPos center = helper.absolutePos(new BlockPos(3, 3, 3));
		for (Direction facing : Direction.Plane.HORIZONTAL) level.setBlock(center.relative(facing), block.defaultBlockState(), Block.UPDATE_ALL);
		var player = helper.makeMockPlayer(GameType.CREATIVE);
		var context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, new ItemStack(block),
				new BlockHitResult(Vec3.atCenterOf(center), Direction.UP, center, false));
		level.setBlock(center, block.getStateForPlacement(context), Block.UPDATE_ALL);
		helper.assertTrue(level.getBlockState(center).getValue(TowerCableTrayBlock.SHAPE) == TowerCableTrayBlock.TrayShape.CROSS,
				"normal placement forms an actual four-way tray");
		for (Direction facing : Direction.Plane.HORIZONTAL) helper.assertTrue(
				TowerCableTrayBlock.connections(level.getBlockState(center.relative(facing))).contains(facing.getOpposite()),
				"each neighboring channel points back to the junction");
		level.removeBlock(center.north(), false);
		helper.assertTrue(level.getBlockState(center).getValue(TowerCableTrayBlock.SHAPE) == TowerCableTrayBlock.TrayShape.TEE, "three arms form a tee");
		level.removeBlock(center.east(), false);
		helper.assertTrue(level.getBlockState(center).getValue(TowerCableTrayBlock.SHAPE) == TowerCableTrayBlock.TrayShape.CORNER, "two adjacent arms form an elbow");
		level.removeBlock(center.south(), false);
		helper.assertTrue(level.getBlockState(center).getValue(TowerCableTrayBlock.SHAPE) == TowerCableTrayBlock.TrayShape.END, "one arm gets a closed termination");
		helper.assertTrue(TowerCableTrayBlock.connections(level.getBlockState(center)).equals(java.util.Set.of(Direction.WEST)), "termination stays pointed at its remaining tray");
		helper.succeed();
	}
}
