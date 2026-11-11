package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.JugcraftDrones;
import io.github.jimbozoomer.jugcraft.drone.TerminalModePayload;
import io.github.jimbozoomer.jugcraft.tools.JugcraftTools;
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
	private static MasterConsoleBlock block() { return (MasterConsoleBlock) io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("tower_master_console"); }
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

	/**
	 * The Tri-Pillar's hall has its master: the Tower Control Console (in the old master console's place since layout 10),
	 * whole and pinned to the tower's terminal under the cargo hall, past its own link range: through that pin it starts
	 * its exact core's next tier for the owner. A master console a player sets down 51 blocks from the terminal links to
	 * nothing; one set down 16 blocks from it pins itself.
	 */
	@GameTest(structure = TriPillarArena.ARENA, skyAccess = true, maxTicks = 100)
	public void builtTowerPinsOneMasterAndUpgradeChecksTheExactCore(GameTestHelper h) {
		if (skip(h)) return;
		var level = h.getLevel();
		var player = h.makeMockServerPlayerInLevel(); player.setGameMode(GameType.CREATIVE);
		var core = TriPillarArena.build(h, player, 1); BlockPos origin = core.getBlockPos(), terminal = core.terminalPos();
		BlockPos console = TriPillarArena.at(core, TriPillarArena.MASTER_CONSOLE);
		var tcc = (io.github.jimbozoomer.jugcraft.tower.TowerControlConsoleBlock) io.github.jimbozoomer.jugcraft.tower.JugcraftTower.TOWER_CONTROL_CONSOLE;
		int controllers = 0, old = 0;
		for (int[] cell : core.layout().foundation.place) {
			var state = level.getBlockState(origin.offset(cell[0], cell[1], cell[2]));
			if (state.is(block())) old++;
			if (state.is(tcc) && state.getValue(io.github.jimbozoomer.jugcraft.tower.TowerControlConsoleBlock.PART) == 0) controllers++;
		}
		h.assertTrue(old == 0 && controllers == 1, "the hall has one Tower Control Console and no old master console: " + controllers + "/" + old);
		h.assertTrue(level.getBlockState(console).is(tcc) && tcc.complete(level, console, Direction.NORTH),
				"the Tower Control Console stands whole north of the central table, facing north");
		h.assertTrue(level.getBlockEntity(console) instanceof DepotDisplayBlockEntity pinned && terminal.equals(pinned.terminal()),
				"the built master is pinned to its own tower");
		var display = (DepotDisplayBlockEntity) level.getBlockEntity(console);
		h.assertTrue(!console.closerThan(terminal, tcc.linkRange()) && display.reaches(terminal, tcc.linkRange()),
				"the terminal is beyond the console's own link range, but within the reach of its pin");
		// At the console's operator side, north of it: out of the core's own reach, within the console's eight metres.
		BlockPos clicked = console;
		player.setPos(origin.getX() + .5, origin.getY() + 1, origin.getZ() - 12.5);
		h.assertTrue(player.distanceToSqr(Vec3.atCenterOf(origin)) > TowerCoreBlockEntity.REACH_SQR
				&& player.distanceToSqr(Vec3.atCenterOf(clicked)) <= 64, "the operator is outside direct core reach, at the console");
		h.assertTrue(TowerUpgradePayload.mayAccessTower(player, core, clicked), "the hall's master authorizes its exact linked core across the site");
		BlockPos otherPos = origin.offset(66, 1, 40); level.setBlock(otherPos, JugcraftTower.CORE.defaultBlockState(), Block.UPDATE_ALL);
		var other = (TowerCoreBlockEntity) level.getBlockEntity(otherPos); other.setOwner(player.getUUID());
		h.assertFalse(TowerUpgradePayload.mayAccessTower(player, other, clicked), "another tower cannot borrow the master console's terminal identity");
		// Free floor (3 x 3 x 3 in the design data) in the hall's west, 51 blocks from the terminal, and by dispatch, 16 from it.
		BlockPos loose = origin.offset(-20, 1, -5), yard = origin.offset(16, 1, -13);
		place(h, loose, Direction.NORTH);
		h.assertTrue(((DepotDisplayBlockEntity) level.getBlockEntity(loose)).terminal() == null, "a console placed 51 blocks from the terminal links to nothing");
		player.setPos(loose.getX() + .5, loose.getY(), loose.getZ() - 1.5);
		h.assertFalse(TowerUpgradePayload.mayAccessTower(player, core, loose), "an unlinked console out of the core's reach cannot upgrade it");
		place(h, yard, Direction.NORTH);
		h.assertTrue(terminal.equals(((DepotDisplayBlockEntity) level.getBlockEntity(yard)).terminal()), "a console placed 16 blocks from the terminal pins itself to it");
		player.setPos(yard.getX() + .5, yard.getY(), yard.getZ() - 1.5);
		h.assertTrue(player.distanceToSqr(Vec3.atCenterOf(origin)) > TowerCoreBlockEntity.REACH_SQR
				&& TowerUpgradePayload.mayAccessTower(player, core, yard), "the console by dispatch authorizes its linked core");
		player.setPos(origin.getX() + .5, origin.getY() + 1, origin.getZ() - 12.5);
		TriPillarArena.stock(core);
		var stranger = h.makeMockServerPlayerInLevel(); stranger.setPos(player.position());
		TowerUpgradePayload.handle(stranger, new TowerUpgradePayload(origin, clicked));
		h.assertTrue(core.building() == 0, "other users cannot upgrade the owner's linked tower");
		TowerUpgradePayload.handle(player, new TowerUpgradePayload(origin, clicked));
		h.assertTrue(core.building() == 2, "owner can start its next tier through the hall's master console");
		h.succeed();
	}

}
