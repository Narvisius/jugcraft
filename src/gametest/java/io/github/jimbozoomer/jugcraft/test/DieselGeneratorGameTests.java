package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentFrameBlock;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentPlacement;
import io.github.jimbozoomer.jugcraft.nuclear.JugcraftNuclear;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The emergency diesel generator: its walkways stand on frames you walk on, the steps up to each on half-block frames
 * (so they walk like stairs), and using it starts and stops it.
 */
public class DieselGeneratorGameTests {
	@GameTest(structure = "jugcraft-test:drone_tower", maxTicks = 20)
	public void stepsWalkAndItStartsAndStops(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		BlockPos anchor = helper.absolutePos(new BlockPos(5, 1, 20));
		Direction facing = Direction.NORTH;
		helper.assertTrue(EquipmentPlacement.place(helper.getLevel(), anchor, EquipmentKind.EMERGENCY_DIESEL_GENERATOR, facing, null),
				"the generator fits");
		EquipmentKind kind = EquipmentKind.EMERGENCY_DIESEL_GENERATOR;
		// The steps up to each walkway (left x 0, right x 6), from the floor: half (z 11), whole (z 10), whole and a half
		// (z 9), the deck.
		int[][] expect = {{11, 0, 1}, {10, 0, 2}, {9, 0, 2}, {9, 1, 1}, {8, 1, 2}, {8, 0, 2}};
		for (int x : new int[] {0, 6}) {
			for (int[] e : expect) {
				BlockPos at = anchor.offset(EquipmentKind.toWorld(new BlockPos(x, e[1], e[0]), facing));
				BlockState state = helper.getLevel().getBlockState(at);
				helper.assertTrue(state.is(JugcraftNuclear.FRAME), "a frame at step x " + x + " z " + e[0] + " y " + e[1]);
				helper.assertTrue(state.getValue(EquipmentFrameBlock.SHAPE) == (e[2] == 1 ? EquipmentFrameBlock.Shape.HALF : EquipmentFrameBlock.Shape.FULL),
						"step x " + x + " z " + e[0] + " y " + e[1] + " is " + state.getValue(EquipmentFrameBlock.SHAPE));
			}
			// Above each walkway and step is clear to stand in; the floor at the foot of each flight is clear.
			for (int[] c : new int[][] {{2, 5}, {2, 9}, {1, 10}, {1, 11}, {0, 12}}) {
				helper.assertTrue(helper.getLevel().getBlockState(anchor.offset(EquipmentKind.toWorld(new BlockPos(x, c[0], c[1]), facing))).isAir(),
						"clear above x " + x + " y " + c[0] + " z " + c[1]);
			}
		}
		// Used: it starts; used again: it stops.
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		EquipmentBlockEntity generator = (EquipmentBlockEntity) helper.getLevel().getBlockEntity(anchor);
		EquipmentPlacement.use(helper.getLevel(), anchor, player, ItemStack.EMPTY, InteractionHand.MAIN_HAND);
		helper.assertTrue(generator.running, "started");
		EquipmentPlacement.use(helper.getLevel(), anchor, player, ItemStack.EMPTY, InteractionHand.MAIN_HAND);
		helper.assertTrue(!generator.running, "stopped");
		helper.succeed();
	}
}
