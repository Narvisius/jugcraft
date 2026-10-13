package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.JugcraftDrones;
import io.github.jimbozoomer.jugcraft.drone.TerminalModePayload;
import io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.MasterConsoleBlock;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.TowerUpgradePayload;
import java.util.HashSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Real placement/removal, exact terminal authority and no-drop whole-MBS editing. */
public class MasterConsoleGameTests {
	private static boolean skip(GameTestHelper h) { if (!TestFocus.skip("tower")) return false; h.succeed(); return true; }
	private static MasterConsoleBlock block() { return (MasterConsoleBlock) JugcraftTower.BLOCKS.get("tower_master_console"); }
	private static BlockState place(GameTestHelper h, BlockPos anchor, Direction facing) {
		var state = block().defaultBlockState().setValue(MasterConsoleBlock.FACING, facing);
		h.getLevel().setBlock(anchor, state, Block.UPDATE_ALL);
		block().setPlacedBy(h.getLevel(), anchor, state, null, new ItemStack(block()));
		return state;
	}
	private static void use(GameTestHelper h, ServerPlayer player, BlockPos at) {
		h.getLevel().getBlockState(at).useWithoutItem(h.getLevel(), player,
				new BlockHitResult(Vec3.atCenterOf(at), Direction.NORTH, at, false));
	}
	private static net.minecraft.world.item.context.BlockPlaceContext placement(GameTestHelper h, ServerPlayer player, BlockPos floor) {
		h.getLevel().setBlock(floor, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
		return new net.minecraft.world.item.context.BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(block()),
				new BlockHitResult(Vec3.atCenterOf(floor).relative(Direction.UP, .5), Direction.UP, floor, false));
	}

	@GameTest(structure = "jugcraft-test:drone_tower", skyAccess = true, maxTicks = 100)
	public void monitorHeadroomRejectsLowCeilingsAndTopBuildHeight(GameTestHelper h) {
		if (skip(h)) return;
		var level = h.getLevel(); var player = h.makeMockServerPlayerInLevel(); player.setGameMode(GameType.CREATIVE);
		BlockPos floor = h.absolutePos(new BlockPos(12, 2, 12));
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			player.setYRot(facing.getOpposite().toYRot());
			var context = placement(h, player, floor); BlockPos anchor = context.getClickedPos();
			h.assertTrue(block().getStateForPlacement(context) != null && block().blockedPlacement(context).isEmpty(), "open monitor space permits " + facing + " placement");
			for (int part = 3; part < 6; part++) {
				BlockPos above = MasterConsoleBlock.FOOTPRINT.partPos(anchor, facing, part).above();
				for (Block obstruction : new Block[] {Blocks.STONE, Blocks.GLASS, Blocks.OAK_LEAVES}) {
					level.setBlock(above, obstruction.defaultBlockState(), Block.UPDATE_ALL);
					h.assertTrue(block().getStateForPlacement(context) == null && block().blockedPlacement(context).contains(above), "solid, glass and leaves cannot clip rear monitor " + part + " in " + facing);
					level.setBlock(above, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
				}
			}
			level.setBlock(anchor.above(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
			h.assertTrue(block().getStateForPlacement(context) != null, "headroom remains specific to rear monitors, not the low front desktop");
			level.setBlock(anchor.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
		player.setYRot(180);
		BlockPos topFloor = new BlockPos(floor.getX(), level.getMaxY() - 1, floor.getZ());
		var top = placement(h, player, topFloor);
		h.assertTrue(top.getClickedPos().getY() == level.getMaxY() && block().getStateForPlacement(top) == null,
				"topmost base cells cannot place monitors outside the world");
		h.assertTrue(block().blockedPlacement(top).stream().filter(level::isOutsideBuildHeight).count() == 3, "all three out-of-world monitor clearances are reported");
		level.setBlock(topFloor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		var below = placement(h, player, topFloor.below());
		h.assertTrue(block().getStateForPlacement(below) != null, "one block lower leaves the required monitor space within world height");
		h.assertTrue(MasterConsoleBlock.FOOTPRINT.size() == 6, "headroom does not add dummy parts or item cost");
		h.succeed();
	}

	@GameTest(structure = "jugcraft-test:drone_tower", skyAccess = true, maxTicks = 100)
	public void kitPlacesSixPartsInEveryFacingAndBreaksAsOne(GameTestHelper h) {
		if (skip(h)) return;
		var level = h.getLevel(); BlockPos origin = h.absolutePos(new BlockPos(10, 3, 10));
		int index = 0;
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			BlockPos anchor = origin.east(6 * index++); var state = place(h, anchor, facing);
			HashSet<BlockPos> expected = new HashSet<>();
			for (int p = 0; p < MasterConsoleBlock.FOOTPRINT.size(); p++) expected.add(MasterConsoleBlock.FOOTPRINT.partPos(anchor, facing, p));
			for (int p = 0; p < MasterConsoleBlock.FOOTPRINT.size(); p++) {
				BlockPos at = MasterConsoleBlock.FOOTPRINT.partPos(anchor, facing, p); var found = level.getBlockState(at);
				h.assertTrue(found.is(block()) && found.getValue(MasterConsoleBlock.PART) == p && block().masterPos(at, found).equals(anchor), "all six real parts resolve their controller in " + facing);
				h.assertTrue((level.getBlockEntity(at) instanceof DepotDisplayBlockEntity) == (p == 0), "exactly one passive block entity per console");
				var outline = found.getShape(level, at).bounds().move(at);
				h.assertTrue(outline.getXsize() > 2.8 && outline.getZsize() > 1.8 || outline.getZsize() > 2.8 && outline.getXsize() > 1.8,
						"selection outlines the entire console, not one dummy block");
			}
			h.assertTrue(block().complete(level, anchor, facing), "placed kit is complete");
			BlockPos broken = MasterConsoleBlock.FOOTPRINT.partPos(anchor, facing, 5);
			level.destroyBlock(broken, true);
			for (BlockPos at : expected) h.assertTrue(level.getBlockState(at).isAir(), "breaking any part dismantles the complete workstation");
			h.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(anchor).inflate(3)).stream()
					.mapToInt(item -> item.getItem().is(block().asItem()) ? item.getItem().getCount() : 0).sum() == 1, "one kit drops, not six duplicated kits");
		}
		h.assertTrue(io.github.jimbozoomer.jugcraft.placement.StructureReach.longestSide(new ItemStack(block()), false) == 3, "placing reach recognizes the true MBS width");
		h.succeed();
	}

	@GameTest(structure = "jugcraft-test:drone_tower", skyAccess = true, maxTicks = 100)
	public void dummyPartClaimsUnownedTerminalAndEnforcesOwnerAndLiveLink(GameTestHelper h) {
		if (skip(h)) return;
		var level = h.getLevel(); BlockPos terminalPos = h.absolutePos(new BlockPos(9, 3, 10)), anchor = terminalPos.east(10);
		level.setBlock(terminalPos, JugcraftDrones.TERMINAL.defaultBlockState(), Block.UPDATE_ALL);
		place(h, anchor, Direction.SOUTH);
		var display = (DepotDisplayBlockEntity) level.getBlockEntity(anchor); display.pin(terminalPos);
		var terminal = (DroneTerminalBlockEntity) level.getBlockEntity(terminalPos);
		BlockPos clicked = MasterConsoleBlock.FOOTPRINT.partPos(anchor, Direction.SOUTH, 1);
		var owner = h.makeMockServerPlayerInLevel(); owner.setPos(clicked.getX() + .5, clicked.getY(), clicked.getZ() + 1.5);
		h.assertTrue(terminal.owner() == null, "terminal begins unowned"); use(h, owner, clicked);
		h.assertTrue(owner.getUUID().equals(terminal.owner()), "normal use of a dummy part claims the terminal for its first operator");
		var mode = terminal.mode(); TerminalModePayload.handle(owner, new TerminalModePayload(terminalPos, clicked));
		h.assertTrue(terminal.mode() != mode, "clicked dummy part authorizes its complete console's exact terminal");
		mode = terminal.mode(); var stranger = h.makeMockServerPlayerInLevel(); stranger.setPos(owner.position()); use(h, stranger, clicked);
		TerminalModePayload.handle(stranger, new TerminalModePayload(terminalPos, clicked));
		h.assertTrue(terminal.mode() == mode && owner.getUUID().equals(terminal.owner()), "another user cannot reclaim or change its mode");
		display.pin(terminalPos.north());
		h.assertFalse(ConsoleDeskBlock.mayAccessTerminal(owner, terminalPos, clicked), "relinking invalidates the open UI's old terminal context");
		display.pin(terminalPos);
		owner.setPos(clicked.getX() + 9.5, clicked.getY(), clicked.getZ() + 1.5);
		h.assertFalse(ConsoleDeskBlock.mayAccessTerminal(owner, terminalPos, clicked), "old master screen cannot reach from more than eight metres away");
		owner.setPos(clicked.getX() + .5, clicked.getY(), clicked.getZ() + 1.5);
		level.setBlock(anchor, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
		h.assertFalse(ConsoleDeskBlock.mayAccessTerminal(owner, terminalPos, clicked), "a stale surviving dummy cannot control a replaced controller");
		h.succeed();
	}

	@GameTest(structure = "jugcraft-test:drone_tower", skyAccess = true, maxTicks = 100)
	public void builtTowerPinsOneMasterAndUpgradeChecksTheExactCore(GameTestHelper h) {
		if (skip(h)) return;
		var level = h.getLevel(); BlockPos origin = h.absolutePos(new BlockPos(48, 1, 48));
		var player = h.makeMockServerPlayerInLevel(); player.setGameMode(GameType.CREATIVE);
		level.setBlock(origin, JugcraftTower.CORE.defaultBlockState(), Block.UPDATE_ALL);
		var core = (TowerCoreBlockEntity) level.getBlockEntity(origin); core.setOwner(player.getUUID()); core.buildInstantly(level, 1);
		int cells = 0, controllers = 0;
		for (int[] cell : core.layout().tier(1).place) {
			BlockPos at = origin.offset(cell[0], cell[1], cell[2]); var state = level.getBlockState(at);
			if (!state.is(block())) continue; cells++;
			if (state.getValue(MasterConsoleBlock.PART) == 0) {
				controllers++;
				h.assertTrue(level.getBlockEntity(at) instanceof DepotDisplayBlockEntity display && core.terminalPos().equals(display.terminal()), "the built master is pinned to its own tower");
			}
		}
		h.assertTrue(cells == 6 && controllers == 1, "actual current tower contains one complete master console");
		BlockPos remote = origin.offset(15, 1, 9); place(h, remote, Direction.NORTH);
		((DepotDisplayBlockEntity) level.getBlockEntity(remote)).pin(core.terminalPos());
		BlockPos clicked = remote.west(); player.setPos(clicked.getX() + .5, clicked.getY(), clicked.getZ() - 1.5);
		h.assertTrue(player.distanceToSqr(Vec3.atCenterOf(origin)) > TowerCoreBlockEntity.REACH_SQR, "remote console test is outside direct core reach");
		h.assertTrue(TowerUpgradePayload.mayAccessTower(player, core, clicked), "remote master authorizes only its exact linked core");
		BlockPos otherPos = origin.offset(15, 0, -12); level.setBlock(otherPos, JugcraftTower.CORE.defaultBlockState(), Block.UPDATE_ALL);
		var other = (TowerCoreBlockEntity) level.getBlockEntity(otherPos); other.setOwner(player.getUUID());
		h.assertFalse(TowerUpgradePayload.mayAccessTower(player, other, clicked), "another tower cannot borrow the master console's terminal identity");
		for (String module : JugcraftTower.MODULES) core.deposit(new ItemStack(JugcraftTower.MODULE_ITEMS.get(module), TowerCoreBlockEntity.MODULE_CAPACITY));
		var stranger = h.makeMockServerPlayerInLevel(); stranger.setPos(player.position());
		TowerUpgradePayload.handle(stranger, new TowerUpgradePayload(origin, clicked));
		h.assertTrue(core.building() == 0, "other users cannot upgrade the owner's linked tower");
		TowerUpgradePayload.handle(player, new TowerUpgradePayload(origin, clicked));
		h.assertTrue(core.building() == 2, "owner can start its next tier through the remote console");
		h.succeed();
	}
}
