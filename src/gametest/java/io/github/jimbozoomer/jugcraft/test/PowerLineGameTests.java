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
		LineWires.Wire span = LineWires.between(pole1, a.kind(), a.connections(), pole2, b.kind(), b.connections()).get(0);
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
		for (BlockPos frame : LineKind.LATTICE_TOWER.frames()) {
			helper.assertTrue(level.getBlockState(tower.offset(frame)).is(JugcraftPowerLines.FRAME), "frame at " + frame);
		}
		BlockPos leg = tower.offset(5, 0, 5);
		helper.assertTrue(tower.equals(LineRegistry.of(level).anchorOf(leg)), "the leg belongs to the tower");
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		level.getBlockState(leg).getBlock().playerWillDestroy(level, leg, level.getBlockState(leg), player);
		level.removeBlock(leg, false);
		helper.succeedWhen(() -> {
			helper.assertTrue(level.getBlockState(tower).isAir(), "the anchor went");
			for (BlockPos frame : LineKind.LATTICE_TOWER.frames()) {
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
}
