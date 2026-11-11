package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.powerline.JugcraftPowerLines;
import io.github.jimbozoomer.jugcraft.powerline.LineAnchorBlockEntity;
import io.github.jimbozoomer.jugcraft.powerline.LineKind;
import io.github.jimbozoomer.jugcraft.powerline.LineNetworks;
import io.github.jimbozoomer.jugcraft.powerline.LinePlacement;
import io.github.jimbozoomer.jugcraft.powerline.LineRegistry;
import io.github.jimbozoomer.jugcraft.powerline.LineWires;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Power lines (docs/features/power-lines.md): moving energy, the rate cap, live wires, placing, breaking and upgrading. */
public class PowerLineGameTests {
	private static final String ARENA = "jugcraft-test:drone_tower";

	/** A stone floor under the arena, so structures have ground to stand on. */
	private static void floor(GameTestHelper helper) {
		for (int x = 0; x < 44; x++) {
			for (int z = 0; z < 44; z++) {
				helper.getLevel().setBlockAndUpdate(helper.absolutePos(new BlockPos(x, 0, z)), Blocks.STONE.defaultBlockState());
			}
		}
	}

	private static BlockPos place(GameTestHelper helper, LineKind kind, int x, int z) {
		BlockPos anchor = LinePlacement.place(helper.getLevel(), helper.absolutePos(new BlockPos(x, 1, z)), kind, null);
		helper.assertTrue(anchor != null, kind.id + " placed at " + x + ", " + z);
		return anchor;
	}

	private static LineAnchorBlockEntity at(GameTestHelper helper, BlockPos pos) {
		return (LineAnchorBlockEntity) helper.getLevel().getBlockEntity(pos);
	}

	private static void wire(GameTestHelper helper, BlockPos a, BlockPos b) {
		helper.assertTrue(LinePlacement.canJoin(helper.getLevel(), a, b) == null,
				"can wire " + a + " to " + b + ": " + LinePlacement.canJoin(helper.getLevel(), a, b));
		at(helper, a).connect(b);
		at(helper, b).connect(a);
		LineNetworks.invalidate(helper.getLevel());
	}

	private static long feed(GameTestHelper helper, BlockPos box, long amount) {
		EnergyStorage storage = EnergyStorage.SIDED.find(helper.getLevel(), box, Direction.UP);
		helper.assertTrue(storage != null, "the box takes energy from cables");
		try (Transaction transaction = Transaction.openOuter()) {
			long accepted = storage.insert(amount, transaction);
			transaction.commit();
			return accepted;
		}
	}

	/** Pedestal - pole - pole - pedestal: energy put in at one end comes out at the other, at most the line's rate per tick. */
	@GameTest(structure = ARENA, maxTicks = 60, skyAccess = true)
	public void energyCrossesTheLineAtItsRate(GameTestHelper helper) {
		if (TestFocus.skip("powerline")) {
			helper.succeed();
			return;
		}
		floor(helper);
		BlockPos from = place(helper, LineKind.SERVICE_PEDESTAL, 4, 4);
		BlockPos pole1 = place(helper, LineKind.WOODEN_POLE, 4, 12);
		BlockPos pole2 = place(helper, LineKind.WOODEN_POLE, 4, 36);
		BlockPos to = place(helper, LineKind.SERVICE_PEDESTAL, 4, 40);
		wire(helper, from, pole1);
		wire(helper, pole1, pole2);
		wire(helper, pole2, to);
		long accepted = feed(helper, from, 1_000_000);
		helper.assertTrue(accepted == LineKind.SERVICE_PEDESTAL.capacity * 2, "the pedestal buffers two ticks of its capacity, took " + accepted);
		long[] most = {0};
		helper.onEachTick(() -> {
			LineNetworks.Network network = LineNetworks.networkOf(helper.getLevel(), from);
			if (network != null) {
				most[0] = Math.max(most[0], network.lastMoved);
			}
		});
		helper.succeedWhen(() -> {
			long received = LineKind.SERVICE_PEDESTAL.capacity * 2 - at(helper, to).roomToReceive();
			helper.assertTrue(received > 0, "energy reached the far pedestal");
			LineNetworks.Network network = LineNetworks.networkOf(helper.getLevel(), from);
			helper.assertTrue(network != null && network.size() == 4 && network.rate() == LineKind.WOODEN_POLE.capacity,
					"one network of four at the wooden pole's rate");
			helper.assertTrue(most[0] <= LineKind.WOODEN_POLE.capacity, "no more than the wooden pole's rate in one tick, moved " + most[0]);
			helper.assertTrue(received <= LineKind.WOODEN_POLE.capacity * (helper.getTick() + 1), "at most the rate per tick in all");
		});
	}

	/** Cutting a wire stops the flow. */
	@GameTest(structure = ARENA, maxTicks = 40, skyAccess = true)
	public void aCutLineCarriesNothing(GameTestHelper helper) {
		if (TestFocus.skip("powerline")) {
			helper.succeed();
			return;
		}
		floor(helper);
		BlockPos from = place(helper, LineKind.SERVICE_PEDESTAL, 4, 4);
		BlockPos pole = place(helper, LineKind.WOODEN_POLE, 4, 12);
		BlockPos to = place(helper, LineKind.SERVICE_PEDESTAL, 4, 20);
		wire(helper, from, pole);
		at(helper, pole).connect(to);
		LineNetworks.invalidate(helper.getLevel());   // only one end knows the wire: not a wire at all
		feed(helper, from, 10_000);
		helper.runAfterDelay(3, () -> {
			helper.assertTrue(at(helper, to).roomToReceive() == LineKind.SERVICE_PEDESTAL.capacity * 2, "nothing arrived over a half-made wire");
			helper.succeed();
		});
	}

	/** A pig touching a live wire is hurt; nothing happens on a dead line. */
	@GameTest(structure = ARENA, maxTicks = 100, skyAccess = true)
	public void liveWiresShock(GameTestHelper helper) {
		if (TestFocus.skip("powerline")) {
			helper.succeed();
			return;
		}
		floor(helper);
		BlockPos from = place(helper, LineKind.SERVICE_PEDESTAL, 4, 4);
		BlockPos pole1 = place(helper, LineKind.WOODEN_POLE, 4, 10);
		BlockPos pole2 = place(helper, LineKind.WOODEN_POLE, 4, 34);
		BlockPos to = place(helper, LineKind.SERVICE_PEDESTAL, 4, 40);
		wire(helper, from, pole1);
		wire(helper, pole1, pole2);
		wire(helper, pole2, to);
		LineAnchorBlockEntity a = at(helper, pole1), b = at(helper, pole2);
		LineWires.Wire span = LineWires.between(helper.getLevel(), pole1, a.kind(), a.connections(), pole2, b.kind(), b.connections()).get(0);
		Vec3 mid = span.at(0.5);
		var pig = net.minecraft.world.entity.EntityTypes.PIG.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
		helper.assertTrue(pig != null, "a pig");
		pig.setNoGravity(true);
		pig.setNoAi(true);
		pig.snapTo(mid.x, mid.y - pig.getBbHeight() / 2, mid.z, 0, 0);
		helper.getLevel().addFreshEntity(pig);
		float health = pig.getHealth();
		helper.onEachTick(() -> feed(helper, from, 4_096));
		helper.succeedWhen(() -> helper.assertTrue(pig.getHealth() < health || !pig.isAlive(), "the pig on the live wire was shocked"));
	}

	/** A lattice tower fills its legs with frame blocks; breaking a leg takes the whole tower and drops it once. */
	@GameTest(structure = ARENA, maxTicks = 40, skyAccess = true)
	public void breakingALegTakesTheTower(GameTestHelper helper) {
		if (TestFocus.skip("powerline")) {
			helper.succeed();
			return;
		}
		floor(helper);
		BlockPos tower = place(helper, LineKind.LATTICE_TOWER, 20, 20);
		ServerLevel level = helper.getLevel();
		List<BlockPos> frames = at(helper, tower).frames();
		helper.assertTrue(!frames.isEmpty(), "the tower has legs");
		for (BlockPos frame : frames) {
			helper.assertTrue(level.getBlockState(tower.offset(frame)).is(JugcraftPowerLines.FRAME), "frame at " + frame);
		}
		BlockPos leg = tower.offset(frames.get(0));
		helper.assertTrue(tower.equals(LineRegistry.of(level).anchorOf(leg)), "the leg belongs to the tower");
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		level.getBlockState(leg).getBlock().playerWillDestroy(level, leg, level.getBlockState(leg), player);
		level.removeBlock(leg, false);
		helper.succeedWhen(() -> {
			helper.assertTrue(level.getBlockState(tower).isAir(), "the anchor went");
			for (BlockPos frame : frames) {
				helper.assertTrue(!level.getBlockState(tower.offset(frame)).is(JugcraftPowerLines.FRAME), "no frame left at " + frame);
			}
			List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(tower).inflate(8),
					e -> e.getItem().is(JugcraftPowerLines.ITEMS.get(LineKind.LATTICE_TOWER)));
			helper.assertTrue(drops.stream().mapToInt(e -> e.getItem().getCount()).sum() == 1, "one tower dropped");
		});
	}

	/** Using a better pole on a wired pole swaps it in place: the wires stay, the old pole comes back. */
	@GameTest(structure = ARENA, maxTicks = 20, skyAccess = true)
	public void polesUpgradeInPlace(GameTestHelper helper) {
		if (TestFocus.skip("powerline")) {
			helper.succeed();
			return;
		}
		floor(helper);
		BlockPos box = place(helper, LineKind.SERVICE_PEDESTAL, 4, 4);
		BlockPos pole = place(helper, LineKind.WOODEN_POLE, 4, 12);
		wire(helper, box, pole);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack better = new ItemStack(JugcraftPowerLines.ITEMS.get(LineKind.DOUBLE_ARM_POLE));
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, better);
		LinePlacement.upgrade(helper.getLevel(), pole, player, better, LineKind.DOUBLE_ARM_POLE);
		LineAnchorBlockEntity upgraded = at(helper, pole);
		helper.assertTrue(upgraded.kind() == LineKind.DOUBLE_ARM_POLE, "the pole is now a double-arm pole");
		helper.assertTrue(upgraded.connections().contains(box), "it kept its wire");
		helper.assertTrue(at(helper, box).connections().contains(pole), "the pedestal still knows the wire");
		helper.assertTrue(better.isEmpty(), "the new pole was used");
		helper.assertTrue(player.getInventory().contains(new ItemStack(JugcraftPowerLines.ITEMS.get(LineKind.WOODEN_POLE))),
				"the old pole came back");
		helper.assertTrue(helper.getLevel().getBlockState(pole.above(12)).is(JugcraftPowerLines.FRAME), "the taller pole's frame was added");
		helper.succeed();
	}

	/** Families do not mix, and wires have a reach. */
	@GameTest(structure = ARENA, maxTicks = 20, skyAccess = true)
	public void wiresJoinOnlyWhatTheyShould(GameTestHelper helper) {
		if (TestFocus.skip("powerline")) {
			helper.succeed();
			return;
		}
		floor(helper);
		BlockPos wood = place(helper, LineKind.WOODEN_POLE, 4, 4);
		BlockPos steel = place(helper, LineKind.STEEL_MONOPOLE, 4, 20);
		BlockPos far = place(helper, LineKind.WOODEN_POLE, 40, 40);
		helper.assertTrue("message.jugcraft.power_line.incompatible".equals(LinePlacement.canJoin(helper.getLevel(), wood, steel)),
				"a wooden pole does not wire to a steel monopole");
		helper.assertTrue("message.jugcraft.power_line.too_far".equals(LinePlacement.canJoin(helper.getLevel(), wood, far)),
				"wooden poles reach 32 blocks");
		helper.succeed();
	}

	/**
	 * Power lines round 2: two 345 kV towers wired on a diagonal each stand as a line end (the angle body) turned to the
	 * other, their legs (frame blocks) turned with them, and the span hangs three twin-bundle phases and two shield wires.
	 */
	@GameTest(structure = ARENA, maxTicks = 20, skyAccess = true)
	public void towersTurnToTheirLine(GameTestHelper helper) {
		if (TestFocus.skip("powerline")) {
			helper.succeed();
			return;
		}
		floor(helper);
		BlockPos a = place(helper, LineKind.LATTICE_345, 8, 8);
		BlockPos b = place(helper, LineKind.LATTICE_345, 34, 34);
		wire(helper, a, b);
		ServerLevel level = helper.getLevel();
		LineAnchorBlockEntity ea = at(helper, a);
		io.github.jimbozoomer.jugcraft.powerline.LineGeometry.Layout la = ea.layout();
		helper.assertTrue(la.angle(), "a line end stands as its angle body");
		helper.assertTrue(Math.abs(Math.toDegrees(la.yaw) - 45) < 0.5, "turned to its line (yaw " + Math.toDegrees(la.yaw) + ")");
		List<BlockPos> frames = ea.frames();
		helper.assertTrue(frames.equals(io.github.jimbozoomer.jugcraft.powerline.LineGeometry.frames(la)), "its frame is its legs as it stands");
		for (BlockPos f : frames) {
			helper.assertTrue(level.getBlockState(a.offset(f)).is(JugcraftPowerLines.FRAME), "leg frame at " + f);
		}
		// a turned leg: the footing at (+x, +z) is on the diagonal, about base x sqrt(2) out
		helper.assertTrue(frames.stream().anyMatch(f -> f.getY() == 0 && f.getX() == 0 && Math.abs(f.getZ()) >= 6), "a leg on the line's axis");
		LineWires.Span span = LineWires.span(level, a, LineKind.LATTICE_345, ea.connections(), b, LineKind.LATTICE_345, at(helper, b).connections());
		helper.assertTrue(span.phases().size() == 3 && span.phases().stream().allMatch(p -> p.size() == 2), "three twin-bundle phases");
		helper.assertTrue(span.shields().size() == 2, "two shield wires");
		helper.succeed();
	}

	/** A tower on a straight line is a tangent (suspension) tower; at a right-angle corner it is an angle (dead-end) tower. */
	@GameTest(structure = ARENA, maxTicks = 20, skyAccess = true)
	public void cornersTakeAngleTowers(GameTestHelper helper) {
		if (TestFocus.skip("powerline")) {
			helper.succeed();
			return;
		}
		floor(helper);
		BlockPos a = place(helper, LineKind.STEEL_MONOPOLE, 4, 4);
		BlockPos b = place(helper, LineKind.STEEL_MONOPOLE, 4, 22);
		BlockPos c = place(helper, LineKind.STEEL_MONOPOLE, 4, 40);
		BlockPos d = place(helper, LineKind.STEEL_MONOPOLE, 40, 40);
		wire(helper, a, b);
		wire(helper, b, c);
		wire(helper, c, d);
		helper.assertTrue(!at(helper, b).layout().angle(), "on the straight: a tangent pole");
		helper.assertTrue(at(helper, c).layout().angle(), "at the corner: an angle pole");
		io.github.jimbozoomer.jugcraft.powerline.LineGeometry.Layout lc = at(helper, c).layout();
		helper.assertTrue(!lc.phasesTo(b).get(0).get(0).equals(lc.phasesTo(d).get(0).get(0)), "each span ends on its own strain assembly");
		helper.succeed();
	}

	/** The switchyard's line gantry takes a 345 kV line: three twin-bundle phases off its dead-ends. */
	@GameTest(structure = ARENA, maxTicks = 20, skyAccess = true)
	public void gantryTakesTheLine(GameTestHelper helper) {
		if (TestFocus.skip("powerline")) {
			helper.succeed();
			return;
		}
		floor(helper);
		ServerLevel level = helper.getLevel();
		BlockPos gantry = helper.absolutePos(new BlockPos(10, 1, 20));
		level.setBlockAndUpdate(gantry, JugcraftPowerLines.ANCHORS.get(LineKind.LINE_GANTRY).defaultBlockState());
		BlockPos tower = place(helper, LineKind.LATTICE_345, 38, 20);
		wire(helper, gantry, tower);
		LineWires.Span span = LineWires.span(level, gantry, LineKind.LINE_GANTRY, at(helper, gantry).connections(), tower, LineKind.LATTICE_345,
				at(helper, tower).connections());
		helper.assertTrue(span.phases().size() == 3 && span.phases().stream().allMatch(p -> p.size() == 2), "three twin-bundle phases");
		Vec3 end = span.phases().get(0).get(0).a();
		helper.assertTrue(end.x > gantry.getX() + 4 && Math.abs(end.y - gantry.getY() - 17.44) < 0.01, "off the gantry's dead-ends: " + end);
		helper.succeed();
	}
	/**
	 * Power lines part 2, the box facing rule: a Unit Substation placed alone faces +z; wired to a monopole off to its
	 * east it turns to face it (its gantry toward the line), its fence and equipment (frame blocks) turning with it, and
	 * its strain assemblies on the gantry point straight out along the line.
	 */
	@GameTest(structure = ARENA, maxTicks = 20, skyAccess = true)
	public void boxesFaceTheirFirstLine(GameTestHelper helper) {
		if (TestFocus.skip("powerline")) {
			helper.succeed();
			return;
		}
		floor(helper);
		ServerLevel level = helper.getLevel();
		BlockPos sub = place(helper, LineKind.UNIT_SUBSTATION, 6, 20);
		helper.assertTrue(level.getBlockState(sub.offset(0, 0, 10)).is(JugcraftPowerLines.FRAME), "alone it faces +z: its back fence 10 blocks south");
		BlockPos mono = place(helper, LineKind.STEEL_MONOPOLE, 38, 20);
		wire(helper, sub, mono);
		io.github.jimbozoomer.jugcraft.powerline.LineGeometry.Layout l = at(helper, sub).layout();
		helper.assertTrue(Math.abs(Math.toDegrees(l.yaw) - 90) < 0.01, "it turned to its line in a 90-degree step (yaw " + Math.toDegrees(l.yaw) + ")");
		List<BlockPos> frames = at(helper, sub).frames();
		helper.assertTrue(frames.stream().allMatch(f -> f.getX() >= 0 && f.getX() <= 10 && Math.abs(f.getZ()) <= 5), "its yard runs back toward the line");
		helper.assertTrue(level.getBlockState(sub.offset(10, 0, 0)).is(JugcraftPowerLines.FRAME), "its back fence is toward the line");
		helper.assertTrue(!level.getBlockState(sub.offset(0, 0, 10)).is(JugcraftPowerLines.FRAME), "the fence where it stood before is gone");
		helper.assertTrue(Math.abs(l.kitTurns.get(0)) < 0.01, "its strain assemblies point straight along the line");
		LineWires.Span span = LineWires.span(level, sub, LineKind.UNIT_SUBSTATION, at(helper, sub).connections(), mono, LineKind.STEEL_MONOPOLE,
				at(helper, mono).connections());
		helper.assertTrue(span.phases().size() == 3, "three phases strung off the gantry");
		helper.assertTrue(span.conductors().stream().allMatch(w -> Math.abs(w.a().y - sub.getY() - 8.95) < 0.05 || Math.abs(w.b().y - sub.getY() - 8.95) < 0.05),
				"each from a strain assembly under the gantry's beam");
		helper.succeed();
	}

	/**
	 * Power lines part 2: a superconducting line into a Unit Substation lands on its cryogenic terminations (the
	 * substation turns its front, where they stand, to the line), the cables leaving the pylon from its sleeve's mouth,
	 * with no messenger on that last span.
	 */
	@GameTest(structure = ARENA, maxTicks = 20, skyAccess = true)
	public void superconductingLinesLandOnTheTerminations(GameTestHelper helper) {
		if (TestFocus.skip("powerline")) {
			helper.succeed();
			return;
		}
		floor(helper);
		ServerLevel level = helper.getLevel();
		BlockPos sub = place(helper, LineKind.UNIT_SUBSTATION, 20, 16);
		BlockPos pylon = place(helper, LineKind.SUPERCONDUCTING_PYLON, 20, 40);
		wire(helper, sub, pylon);
		io.github.jimbozoomer.jugcraft.powerline.LineGeometry.Layout ls = at(helper, sub).layout();
		helper.assertTrue(Math.abs(Math.abs(Math.toDegrees(ls.yaw)) - 180) < 0.01, "its front faces the superconducting line (yaw " + Math.toDegrees(ls.yaw) + ")");
		helper.assertTrue(ls.vesselDir != null, "its cryogenic terminations are fitted");
		LineWires.Span span = LineWires.span(level, sub, LineKind.UNIT_SUBSTATION, at(helper, sub).connections(), pylon,
				LineKind.SUPERCONDUCTING_PYLON, at(helper, pylon).connections());
		helper.assertTrue(span.conductors().size() == 2, "two cables");
		helper.assertTrue(span.shields().isEmpty(), "no messenger into the terminations");
		for (LineWires.Wire w : span.conductors()) {
			Vec3 near = w.a().distanceToSqr(Vec3.atBottomCenterOf(sub)) < w.b().distanceToSqr(Vec3.atBottomCenterOf(sub)) ? w.a() : w.b();
			Vec3 far = near == w.a() ? w.b() : w.a();
			boolean onVessel = ls.box.vessels().stream().anyMatch(v -> ls.world(v.add(ls.vesselDir.scale(0.77))).distanceTo(near) < 0.01);
			helper.assertTrue(onVessel, "a cable lands on a termination's sleeve: " + near);
			helper.assertTrue(Math.abs(far.y - pylon.getY() - 19.98) < 0.05, "and leaves the pylon at its cradle: " + far);
		}
		helper.succeed();
	}

	/**
	 * Power lines part 2: a pole's service to a box is not part of its line. A wooden pole mid-line wired to a Service
	 * Pedestal and a Pad-Mount Transformer stays a tangent pole turned along its line; it carries the pole-mount
	 * transformer on the pedestal's side (the drop leaves its spool) and the riser on the other (no overhead wire).
	 */
	@GameTest(structure = ARENA, maxTicks = 20, skyAccess = true)
	public void polesServeBoxesFromTheSide(GameTestHelper helper) {
		if (TestFocus.skip("powerline")) {
			helper.succeed();
			return;
		}
		floor(helper);
		ServerLevel level = helper.getLevel();
		BlockPos a = place(helper, LineKind.WOODEN_POLE, 8, 4);
		BlockPos b = place(helper, LineKind.WOODEN_POLE, 8, 22);
		BlockPos c = place(helper, LineKind.WOODEN_POLE, 8, 40);
		BlockPos ped = place(helper, LineKind.SERVICE_PEDESTAL, 20, 22);
		BlockPos pmt = place(helper, LineKind.PAD_MOUNT_TRANSFORMER, 1, 22);
		wire(helper, a, b);
		wire(helper, b, c);
		wire(helper, b, ped);
		wire(helper, b, pmt);
		io.github.jimbozoomer.jugcraft.powerline.LineGeometry.Layout lb = at(helper, b).layout();
		helper.assertTrue(!lb.angle(), "still a tangent pole on its straight line");
		helper.assertTrue(Math.abs(Math.sin(lb.yaw)) < 1e-6, "turned along its line, not toward the boxes");
		helper.assertTrue(lb.xfmrSide == 1 && lb.riserSide == -1, "the transformer toward the pedestal, the riser toward the pad-mount: "
				+ lb.xfmrSide + ", " + lb.riserSide);
		LineAnchorBlockEntity eb = at(helper, b);
		List<LineWires.Wire> drop = LineWires.between(level, b, LineKind.WOODEN_POLE, eb.connections(), ped, LineKind.SERVICE_PEDESTAL,
				at(helper, ped).connections());
		helper.assertTrue(drop.size() == 1, "one service drop");
		Vec3 spool = lb.world(lb.support.xfmr().get(1).drop());
		helper.assertTrue(drop.get(0).a().distanceTo(spool) < 0.01 || drop.get(0).b().distanceTo(spool) < 0.01, "the drop leaves the transformer's spool");
		helper.assertTrue(LineWires.between(level, b, LineKind.WOODEN_POLE, eb.connections(), pmt, LineKind.PAD_MOUNT_TRANSFORMER,
				at(helper, pmt).connections()).isEmpty(), "the pad-mount's cable is buried");
		helper.succeed();
	}
}
