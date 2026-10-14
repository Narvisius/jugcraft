package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.fluid.FluidNetworks;
import io.github.jimbozoomer.jugcraft.fluid.FluidTankBlockEntity;
import io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids;
import io.github.jimbozoomer.jugcraft.fluid.StoredFluid;
import io.github.jimbozoomer.jugcraft.fluid.TankGroup;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

/**
 * Portable gas handling for the starter gas and acid factory (docs/features/industrial-starter-gas-and-acid-factory.md,
 * the TODO list's "filled gas tanks retain exact content"): a tank of gas carries its exact contents through breaking,
 * pickup and placing; breaking one tank of a joined group takes only that tank's share; a placed tank rejoins its group
 * and its pipe line, taking only its own gas and never more than it holds; and tanks of two fluids side by side never mix.
 */
public class GasTankHandlingGameTests {
	/** An awkward amount, three buckets and 777 droplets, so any rounding to millibuckets would show. */
	private static final long ODD = 3 * FluidConstants.BUCKET + 777;

	/**
	 * A tank of hydrogen holding an amount that is not a whole millibucket drops as one item carrying exactly that, and
	 * the tank placed from it holds exactly that again; two items of different contents never stack.
	 */
	@GameTest(maxTicks = 40)
	public void aGasTankKeepsItsExactContents(GameTestHelper helper) {
		BlockPos tank = new BlockPos(1, 1, 1);
		helper.setBlock(tank, JugcraftFluids.FLUID_TANK);
		FluidTankBlockEntity entity = helper.getBlockEntity(tank, FluidTankBlockEntity.class);
		insert(helper, entity, PetroFluids.HYDROGEN.fluid(), ODD);
		List<ItemStack> drops = Block.getDrops(helper.getBlockState(tank), helper.getLevel(), helper.absolutePos(tank), entity);
		helper.assertTrue(drops.size() == 1, "The tank dropped " + drops.size() + " stacks");
		StoredFluid stored = drops.get(0).get(JugcraftFluids.STORED_FLUID);
		helper.assertTrue(stored != null && stored.variant().isOf(PetroFluids.HYDROGEN.fluid()) && stored.amount() == ODD,
				"The item carries " + stored + ", not " + ODD + " droplets of hydrogen");

		BlockPos placed = new BlockPos(3, 1, 1);
		helper.setBlock(placed, JugcraftFluids.FLUID_TANK);
		FluidTankBlockEntity placedEntity = helper.getBlockEntity(placed, FluidTankBlockEntity.class);
		placedEntity.applyComponentsFromItemStack(drops.get(0));
		helper.assertTrue(placedEntity.storage.variant.isOf(PetroFluids.HYDROGEN.fluid()) && placedEntity.storage.amount == ODD,
				"The placed tank holds " + placedEntity.storage.amount + " droplets, not " + ODD);

		insert(helper, placedEntity, PetroFluids.HYDROGEN.fluid(), 1);
		ItemStack fuller = Block.getDrops(helper.getBlockState(placed), helper.getLevel(), helper.absolutePos(placed), placedEntity).get(0);
		helper.assertTrue(!ItemStack.isSameItemSameComponents(drops.get(0), fuller),
				"Tanks holding different amounts would stack into one");
		helper.succeed();
	}

	/**
	 * Two players reach a dropped tank of gas in the same tick: exactly one of them picks it up, carrying exactly the
	 * tank's contents, and the other gets nothing; no tank or gas is made or lost.
	 */
	@GameTest(maxTicks = 80)
	public void twoPlayersPickingUpOneTankGetOne(GameTestHelper helper) {
		BlockPos tank = new BlockPos(2, 1, 2);
		helper.setBlock(tank, JugcraftFluids.FLUID_TANK);
		insert(helper, helper.getBlockEntity(tank, FluidTankBlockEntity.class), PetroFluids.HYDROGEN.fluid(), ODD);
		helper.getLevel().destroyBlock(helper.absolutePos(tank), true);
		ServerPlayer first = helper.makeMockServerPlayerInLevel();
		ServerPlayer second = helper.makeMockServerPlayerInLevel();
		for (ServerPlayer player : List.of(first, second)) {
			player.getInventory().clearContent();
		}
		// Past the drop's pickup delay, both touch it in one tick.
		helper.runAfterDelay(20, () -> {
			List<ItemEntity> items = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(tank)).inflate(3),
					item -> item.getItem().is(JugcraftFluids.FLUID_TANK.asItem()));
			helper.assertTrue(items.size() == 1, "Breaking the tank dropped " + items.size() + " tank stacks");
			ItemEntity drop = items.get(0);
			// Each touch is measured by what it adds, so the test holds whether or not the two mock players share an
			// identity. The first takes the tank and the drop leaves the world; the game only touches entities still in
			// it, and vanilla restores a discarded drop's count, so the second player is checked against the world.
			int before = count(first);
			drop.playerTouch(first);
			int taken = count(first) - before;
			helper.assertTrue(taken == 1 && drop.isRemoved(), "The first touch took " + taken + " tanks, leaving " + drop.getItem());
			int beforeSecond = count(second);
			for (ItemEntity left : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(tank)).inflate(3),
					item -> item.isAlive() && item.getItem().is(JugcraftFluids.FLUID_TANK.asItem()))) {
				left.playerTouch(second);
			}
			helper.assertTrue(count(second) == beforeSecond, "The second touch took another tank from an empty drop");
			ServerPlayer holder = first;
			StoredFluid stored = null;
			for (int slot = 0; slot < holder.getInventory().getContainerSize(); slot++) {
				ItemStack stack = holder.getInventory().getItem(slot);
				if (stack.is(JugcraftFluids.FLUID_TANK.asItem())) {
					stored = stack.get(JugcraftFluids.STORED_FLUID);
				}
			}
			helper.assertTrue(stored != null && stored.variant().isOf(PetroFluids.HYDROGEN.fluid()) && stored.amount() == ODD,
					"The picked-up tank carries " + stored + ", not " + ODD + " droplets of hydrogen");
			helper.succeed();
		});
	}

	/**
	 * Two stacked tanks joined as one, filled with 20 buckets of hydrogen from the bottom (16 below, 4 above): breaking
	 * the top one drops a single tank carrying its 4 buckets, the bottom keeps its 16, and nothing is made or lost.
	 */
	@GameTest(maxTicks = 60)
	public void breakingOneJoinedTankTakesOnlyItsShare(GameTestHelper helper) {
		BlockPos low = new BlockPos(2, 1, 2);
		BlockPos high = low.above();
		helper.setBlock(low, JugcraftFluids.FLUID_TANK);
		helper.setBlock(high, JugcraftFluids.FLUID_TANK);
		TankGroup group = TankGroup.at(helper.getLevel(), helper.absolutePos(low));
		try (Transaction transaction = Transaction.openOuter()) {
			long moved = group.insert(FluidVariant.of(PetroFluids.HYDROGEN.fluid()), 20 * FluidConstants.BUCKET, transaction);
			helper.assertTrue(moved == 20 * FluidConstants.BUCKET, "The group took " + moved + " droplets");
			transaction.commit();
		}
		FluidTankBlockEntity lowEntity = helper.getBlockEntity(low, FluidTankBlockEntity.class);
		helper.assertTrue(lowEntity.storage.amount == FluidTankBlockEntity.CAPACITY
				&& helper.getBlockEntity(high, FluidTankBlockEntity.class).storage.amount == 4 * FluidConstants.BUCKET,
				"The group did not fill from the bottom");
		helper.getLevel().destroyBlock(helper.absolutePos(high), true);
		helper.succeedWhen(() -> {
			List<ItemEntity> items = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(high)).inflate(3),
					item -> item.getItem().is(JugcraftFluids.FLUID_TANK.asItem()));
			helper.assertTrue(items.size() == 1 && items.get(0).getItem().getCount() == 1, "Breaking it dropped " + items.size() + " tank stacks");
			StoredFluid stored = items.get(0).getItem().get(JugcraftFluids.STORED_FLUID);
			helper.assertTrue(stored != null && stored.variant().isOf(PetroFluids.HYDROGEN.fluid()) && stored.amount() == 4 * FluidConstants.BUCKET,
					"The broken tank carries " + stored + ", not its 4 buckets");
			helper.assertTrue(lowEntity.storage.amount == FluidTankBlockEntity.CAPACITY, "The tank below lost some of its 16 buckets");
		});
	}

	/**
	 * A tank carrying 4 buckets of hydrogen, placed on top of a tank with 16, joins it: the group holds all 20 and gives
	 * them back out, the top first. A tank of water placed beside it never mixes: water poured into the group reaches
	 * only the water tank, and draining hydrogen leaves the water alone.
	 */
	@GameTest(maxTicks = 40)
	public void aPlacedTankRejoinsAndFluidsNeverMerge(GameTestHelper helper) {
		BlockPos low = new BlockPos(2, 1, 2);
		helper.setBlock(low, JugcraftFluids.FLUID_TANK);
		insert(helper, helper.getBlockEntity(low, FluidTankBlockEntity.class), PetroFluids.HYDROGEN.fluid(), FluidTankBlockEntity.CAPACITY);
		BlockPos high = low.above();
		helper.setBlock(high, JugcraftFluids.FLUID_TANK);
		ItemStack carried = new ItemStack(JugcraftFluids.FLUID_TANK);
		carried.set(JugcraftFluids.STORED_FLUID, new StoredFluid(FluidVariant.of(PetroFluids.HYDROGEN.fluid()), 4 * FluidConstants.BUCKET));
		helper.getBlockEntity(high, FluidTankBlockEntity.class).applyComponentsFromItemStack(carried);

		BlockPos side = low.east();
		helper.setBlock(side, JugcraftFluids.FLUID_TANK);
		FluidTankBlockEntity water = helper.getBlockEntity(side, FluidTankBlockEntity.class);
		insert(helper, water, Fluids.WATER, 2 * FluidConstants.BUCKET);

		TankGroup group = TankGroup.at(helper.getLevel(), helper.absolutePos(low));
		FluidTankBlockEntity lowEntity = helper.getBlockEntity(low, FluidTankBlockEntity.class);
		FluidTankBlockEntity highEntity = helper.getBlockEntity(high, FluidTankBlockEntity.class);
		try (Transaction transaction = Transaction.openOuter()) {
			long hydrogen = group.extract(FluidVariant.of(PetroFluids.HYDROGEN.fluid()), 64 * FluidConstants.BUCKET, transaction);
			helper.assertTrue(hydrogen == 20 * FluidConstants.BUCKET, "The group gave " + hydrogen + " droplets of hydrogen, not 20 buckets");
			transaction.abort();
		}
		// Whichever fluid the mixed group reports, water poured in reaches only the water tank.
		try (Transaction transaction = Transaction.openOuter()) {
			group.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction);
			transaction.commit();
		}
		helper.assertTrue(lowEntity.storage.variant.isOf(PetroFluids.HYDROGEN.fluid()) && lowEntity.storage.amount == FluidTankBlockEntity.CAPACITY
				&& highEntity.storage.variant.isOf(PetroFluids.HYDROGEN.fluid()) && highEntity.storage.amount == 4 * FluidConstants.BUCKET,
				"Water reached a hydrogen tank");
		try (Transaction transaction = Transaction.openOuter()) {
			long first = group.extract(FluidVariant.of(PetroFluids.HYDROGEN.fluid()), 4 * FluidConstants.BUCKET, transaction);
			transaction.commit();
			helper.assertTrue(first == 4 * FluidConstants.BUCKET && highEntity.storage.amount == 0,
					"The placed tank on top did not drain first");
		}
		helper.assertTrue(water.storage.variant.isOf(Fluids.WATER) && (water.storage.amount == 2 * FluidConstants.BUCKET || water.storage.amount == 3 * FluidConstants.BUCKET),
				"The water tank beside it holds " + water.storage.amount + " droplets of " + water.storage.variant);
		helper.succeed();
	}

	/**
	 * A tank of hydrogen on a steel pipe line takes what is pushed down the line. Broken, it takes nothing and the push
	 * keeps its gas; placed back from its item, it rejoins the line with its contents and takes the next push. It never
	 * takes another gas, and two pushes in one tick never fill it past its capacity: the second keeps what does not fit.
	 */
	@GameTest(maxTicks = 60)
	public void aReplacedTankReconnectsThroughPipes(GameTestHelper helper) {
		BlockPos source = new BlockPos(1, 1, 2);
		BlockPos tank = new BlockPos(4, 1, 2);
		for (int x = 2; x <= 3; x++) {
			helper.setBlock(new BlockPos(x, 1, 2), JugcraftFluids.STEEL_FLUID_PIPE);
		}
		helper.setBlock(tank, JugcraftFluids.FLUID_TANK);
		insert(helper, helper.getBlockEntity(tank, FluidTankBlockEntity.class), PetroFluids.HYDROGEN.fluid(), FluidConstants.BUCKET);
		SingleFluidStorage hydrogen = held(PetroFluids.HYDROGEN.fluid(), 4 * FluidConstants.BUCKET);
		long first = push(helper, source, hydrogen, FluidConstants.BUCKET);
		helper.assertTrue(first == FluidConstants.BUCKET && helper.getBlockEntity(tank, FluidTankBlockEntity.class).storage.amount == 2 * FluidConstants.BUCKET,
				"The line moved " + first + " droplets into the tank");
		helper.getLevel().destroyBlock(helper.absolutePos(tank), true);
		helper.runAfterDelay(2, () -> {
			long none = push(helper, source, hydrogen, FluidConstants.BUCKET);
			helper.assertTrue(none == 0 && hydrogen.amount == 3 * FluidConstants.BUCKET, "With the tank gone the push moved " + none + " droplets");
			List<ItemEntity> items = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(tank)).inflate(3),
					item -> item.getItem().is(JugcraftFluids.FLUID_TANK.asItem()));
			helper.assertTrue(items.size() == 1, "Breaking the tank dropped " + items.size() + " tanks");
			ItemStack carried = items.get(0).getItem();
			items.get(0).discard();
			helper.setBlock(tank, JugcraftFluids.FLUID_TANK);
			FluidTankBlockEntity placed = helper.getBlockEntity(tank, FluidTankBlockEntity.class);
			placed.applyComponentsFromItemStack(carried);
			helper.assertTrue(placed.storage.amount == 2 * FluidConstants.BUCKET, "The replaced tank holds " + placed.storage.amount);

			long again = push(helper, source, hydrogen, FluidConstants.BUCKET / 2);
			helper.assertTrue(again == FluidConstants.BUCKET / 2 && placed.storage.amount == 5 * FluidConstants.BUCKET / 2,
					"The replaced tank took " + again + " droplets back on the line");
			SingleFluidStorage oxygen = held(PetroFluids.OXYGEN.fluid(), FluidConstants.BUCKET);
			helper.assertTrue(push(helper, source, oxygen, FluidConstants.BUCKET) == 0 && oxygen.amount == FluidConstants.BUCKET,
					"The hydrogen tank took oxygen");

			// Nearly full: two pushes in the same tick fill it exactly, and the second keeps the rest.
			try (Transaction transaction = Transaction.openOuter()) {
				placed.storage.insert(FluidVariant.of(PetroFluids.HYDROGEN.fluid()), FluidTankBlockEntity.CAPACITY - placed.storage.amount
						- FluidConstants.BUCKET / 2, transaction);
				transaction.commit();
			}
			SingleFluidStorage other = held(PetroFluids.HYDROGEN.fluid(), FluidConstants.BUCKET);
			BlockPos otherSource = new BlockPos(3, 1, 3);
			long a = push(helper, source, hydrogen, FluidConstants.BUCKET / 4);
			long b = FluidNetworks.pushToNeighbors(helper.getLevel(), helper.absolutePos(otherSource), other, FluidConstants.BUCKET,
					List.of(net.minecraft.core.Direction.NORTH));
			helper.assertTrue(placed.storage.amount == FluidTankBlockEntity.CAPACITY && a + b == FluidConstants.BUCKET / 2
					&& other.amount == FluidConstants.BUCKET - b, "Two pushes moved " + a + " and " + b + " droplets into a tank with room for half a bucket");
			helper.succeed();
		});
	}

	/** A detached storage holding {@code droplets} of {@code fluid}, standing in for a machine's outlet. */
	private static SingleFluidStorage held(net.minecraft.world.level.material.Fluid fluid, long droplets) {
		SingleFluidStorage storage = SingleFluidStorage.withFixedCapacity(16 * FluidConstants.BUCKET, () -> { });
		storage.variant = FluidVariant.of(fluid);
		storage.amount = droplets;
		return storage;
	}

	/** Pushes from {@code from} at {@code source} east into the pipe line, as a machine's outlet would. */
	private static long push(GameTestHelper helper, BlockPos source, SingleFluidStorage from, long droplets) {
		return FluidNetworks.pushToNeighbors(helper.getLevel(), helper.absolutePos(source), from, droplets,
				List.of(net.minecraft.core.Direction.EAST));
	}

	private static void insert(GameTestHelper helper, FluidTankBlockEntity tank, net.minecraft.world.level.material.Fluid fluid, long droplets) {
		try (Transaction transaction = Transaction.openOuter()) {
			long moved = tank.storage.insert(FluidVariant.of(fluid), droplets, transaction);
			helper.assertTrue(moved == droplets, "Only " + moved + " of " + droplets + " droplets went in");
			transaction.commit();
		}
	}

	private static int count(ServerPlayer player) {
		int count = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(JugcraftFluids.FLUID_TANK.asItem())) {
				count += stack.getCount();
			}
		}
		return count;
	}
}
