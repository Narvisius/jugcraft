package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.guns.CellRackBlock;
import io.github.jimbozoomer.jugcraft.guns.CellRackBlockEntity;
import io.github.jimbozoomer.jugcraft.guns.EnergyCellItem;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import io.github.jimbozoomer.jugcraft.tools.Chargeable;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the Cell Rack (slice 10E, docs/features/guns.md): it charges every Energy Cell standing in it at
 * once from its buffer, sharing the charge evenly and lighting while it does; a player stands a cell in the cradle they
 * point at and takes one out with an empty hand; hoppers put cells in from above and take only full ones from below;
 * it keeps its cells and charge when saved, and breaking it drops them.
 */
public class CellRackGameTests {
	private static final BlockPos RACK = new BlockPos(2, 2, 2);

	/**
	 * It is registered with its block entity and its recipe loads; cables reach its buffer from every side; and a point
	 * on its front picks the cradle nearest it, whichever way it faces.
	 */
	@GameTest
	public void cellRackIsRegistered(GameTestHelper helper) {
		helper.assertTrue(JugcraftGuns.CELL_RACK instanceof CellRackBlock && JugcraftGuns.CELL_RACK.asItem() != Items.AIR,
				"The Cell Rack is registered with its item");
		helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id("cell_rack"))).isPresent(),
				"The Cell Rack's recipe loads");
		CellRackBlockEntity rack = rack(helper);
		for (Direction side : Direction.values()) {
			helper.assertTrue(EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(RACK), side) == rack.energy(),
					"Cables reach the Cell Rack's buffer from the " + side.getSerializedName());
		}
		// Slots 0 to 2 are the lower shelf, 3 to 5 the upper, each left to right seen from the front.
		double left = 12.5 / 16.0;
		double right = 3.5 / 16.0;
		helper.assertTrue(CellRackBlock.cradleAt(Direction.NORTH, left, 0.3, 0.0) == 0
				&& CellRackBlock.cradleAt(Direction.NORTH, 0.5, 0.3, 0.0) == 1
				&& CellRackBlock.cradleAt(Direction.NORTH, right, 0.8, 0.0) == 5, "Facing north, the front's points pick their cradles");
		helper.assertTrue(CellRackBlock.cradleAt(Direction.SOUTH, 1.0 - left, 0.3, 1.0) == 0
				&& CellRackBlock.cradleAt(Direction.EAST, 1.0, 0.8, left) == 3
				&& CellRackBlock.cradleAt(Direction.WEST, 0.0, 0.8, 1.0 - right) == 5, "Turned, its cradles turn with it");
		helper.succeed();
	}

	/**
	 * Three empty cells charge together, each the same, and the rack lights while they do; a full cell beside them takes
	 * nothing. Once they are full it goes dark, and its buffer has given exactly what the cells took. A cell on its own
	 * takes no more a tick than the Charging Station gives one.
	 */
	@GameTest(maxTicks = 100)
	public void cellRackChargesSeveralCellsAtOnce(GameTestHelper helper) {
		CellRackBlockEntity rack = rack(helper);
		rack.energy().setAmount(CellRackBlockEntity.CAPACITY);
		for (int slot : new int[] {0, 2, 4}) {
			rack.setItem(slot, new ItemStack(JugcraftGuns.ENERGY_CELL));
		}
		rack.setItem(5, cell(EnergyCellItem.CAPACITY));
		helper.runAfterDelay(5, () -> {
			long first = Chargeable.energy(rack.getItem(0));
			helper.assertTrue(first > 0 && first < EnergyCellItem.CAPACITY, "After five ticks the first cell holds " + first + " JE");
			helper.assertTrue(Chargeable.energy(rack.getItem(2)) == first && Chargeable.energy(rack.getItem(4)) == first,
					"The cells hold " + first + ", " + Chargeable.energy(rack.getItem(2)) + " and " + Chargeable.energy(rack.getItem(4))
							+ " JE, not the same");
			helper.assertTrue(Chargeable.energy(rack.getItem(5)) == EnergyCellItem.CAPACITY, "The full cell's charge changed");
			helper.assertTrue(helper.getBlockState(RACK).getValue(CellRackBlock.LIT), "The Cell Rack is dark while it charges");
		});
		long[] start = new long[1];
		helper.runAfterDelay(60, () -> {
			for (int slot : new int[] {0, 2, 4, 5}) {
				helper.assertTrue(CellRackBlockEntity.full(rack.getItem(slot)), "The cell in cradle " + slot + " holds only "
						+ Chargeable.energy(rack.getItem(slot)) + " JE");
			}
			long left = CellRackBlockEntity.CAPACITY - 3 * EnergyCellItem.CAPACITY;
			helper.assertTrue(rack.energy().getAmount() == left, "The buffer holds " + rack.energy().getAmount() + " JE, not " + left);
			helper.assertTrue(!helper.getBlockState(RACK).getValue(CellRackBlock.LIT), "The Cell Rack stays lit with every cell full");
			for (int slot : new int[] {0, 2, 4, 5}) {
				rack.removeItemNoUpdate(slot);
			}
			rack.setItem(1, new ItemStack(JugcraftGuns.ENERGY_CELL));
			start[0] = helper.getLevel().getGameTime();
		});
		helper.runAfterDelay(65, () -> {
			long energy = Chargeable.energy(rack.getItem(1));
			long most = CellRackBlockEntity.PER_CELL * (helper.getLevel().getGameTime() - start[0] + 1);
			helper.assertTrue(energy > 0 && energy <= most, "A lone cell took " + energy + " JE, more than " + most);
			helper.succeed();
		});
	}

	/**
	 * A cell used on the rack stands in the cradle pointed at, or the nearest free one; with an empty hand the player
	 * takes the cell nearest where they point.
	 */
	@GameTest
	public void playersStandCellsInTheirCradles(GameTestHelper helper) {
		CellRackBlockEntity rack = rack(helper);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos front = helper.absolutePos(RACK.north());
		player.setPos(front.getX() + 0.5, front.getY(), front.getZ() + 0.5);
		// Energy Cells stack to one: a cell in the hand, used on the upper shelf's right-hand cradle, seen from the front.
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftGuns.ENERGY_CELL));
		helper.assertTrue(use(helper, player, 3.5, 12.0).consumesAction() && !rack.getItem(5).isEmpty(),
				"A cell used on the upper right stands in that cradle");
		helper.assertTrue(player.getMainHandItem().isEmpty(), "Standing the cell in its cradle takes it from the hand");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftGuns.ENERGY_CELL));
		helper.assertTrue(use(helper, player, 3.5, 12.0).consumesAction() && !rack.getItem(4).isEmpty(),
				"With that cradle full, the next stands beside it");
		helper.assertTrue(player.getMainHandItem().isEmpty(), "The second cell left the hand");
		// The lower shelf's left-hand cradle: the nearest cell to it is the upper middle one.
		helper.assertTrue(use(helper, player, 12.5, 4.0).consumesAction() && rack.getItem(4).isEmpty() && !rack.getItem(5).isEmpty(),
				"An empty hand takes the cell nearest where it points");
		helper.assertTrue(player.getInventory().countItem(JugcraftGuns.ENERGY_CELL) == 1, "The cell taken is in the inventory");
		helper.succeed();
	}

	/** A hopper above puts Energy Cells in, and nothing else; a hopper below takes out only the full ones. */
	@GameTest(maxTicks = 100)
	public void hoppersLoadCellsAndTakeFullOnes(GameTestHelper helper) {
		CellRackBlockEntity rack = rack(helper);
		rack.setItem(4, cell(EnergyCellItem.CAPACITY));
		rack.setItem(5, cell(EnergyCellItem.CAPACITY / 2));
		helper.setBlock(RACK.above(), Blocks.HOPPER);
		helper.setBlock(RACK.below(), Blocks.HOPPER);
		HopperBlockEntity above = (HopperBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(RACK.above()));
		HopperBlockEntity below = (HopperBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(RACK.below()));
		// Energy Cells stack to one, so each goes in a slot of its own.
		above.setItem(0, new ItemStack(Items.STICK));
		above.setItem(1, new ItemStack(JugcraftGuns.ENERGY_CELL));
		above.setItem(2, new ItemStack(JugcraftGuns.ENERGY_CELL));
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(rack.getItem(0).is(JugcraftGuns.ENERGY_CELL) && rack.getItem(1).is(JugcraftGuns.ENERGY_CELL),
					"The hopper above stood its two cells in the rack");
			helper.assertTrue(above.getItem(0).is(Items.STICK) && above.getItem(1).isEmpty() && above.getItem(2).isEmpty(),
					"The hopper above put in only its cells");
			helper.assertTrue(rack.getItem(4).isEmpty() && rack.getItem(5).is(JugcraftGuns.ENERGY_CELL),
					"The hopper below took the full cell and left the half-charged one");
			helper.assertTrue(below.countItem(JugcraftGuns.ENERGY_CELL) == 1 && Chargeable.energy(below.getItem(0)) == EnergyCellItem.CAPACITY,
					"The hopper below holds the full cell");
			helper.succeed();
		});
	}

	/** Saved and loaded, it keeps its buffer and its cells with their charge; broken, it drops itself and its cells. */
	@GameTest
	public void cellRackKeepsAndDropsItsCells(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CellRackBlockEntity rack = rack(helper);
		rack.energy().setAmount(12_345);
		rack.setItem(0, cell(EnergyCellItem.CAPACITY));
		rack.setItem(3, cell(4_321));
		CompoundTag saved = rack.saveWithoutMetadata(level.registryAccess());
		CellRackBlockEntity copy = new CellRackBlockEntity(rack.getBlockPos(), rack.getBlockState());
		copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
		helper.assertTrue(copy.energy().getAmount() == 12_345 && Chargeable.energy(copy.getItem(0)) == EnergyCellItem.CAPACITY
				&& Chargeable.energy(copy.getItem(3)) == 4_321 && copy.getItem(1).isEmpty(), "The buffer and the cells are saved");

		level.destroyBlock(helper.absolutePos(RACK), true);
		List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(RACK)).inflate(2.0));
		List<Long> cells = drops.stream().filter(drop -> drop.getItem().is(JugcraftGuns.ENERGY_CELL))
				.map(drop -> Chargeable.energy(drop.getItem())).sorted().toList();
		helper.assertTrue(cells.equals(List.of(4_321L, EnergyCellItem.CAPACITY)), "Broken, it dropped cells holding " + cells + " JE");
		helper.assertTrue(drops.stream().anyMatch(drop -> drop.getItem().is(JugcraftGuns.CELL_RACK.asItem())), "Broken, it dropped itself");
		helper.succeed();
	}

	/** A Cell Rack facing north, two blocks up so a hopper fits below it. */
	private static CellRackBlockEntity rack(GameTestHelper helper) {
		helper.setBlock(RACK, JugcraftGuns.CELL_RACK.defaultBlockState());
		return helper.getBlockEntity(RACK, CellRackBlockEntity.class);
	}

	/** Uses the player's held item on the rack's front, this many pixels across (from the west) and up. */
	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, double across, double up) {
		BlockPos absolute = helper.absolutePos(RACK);
		Vec3 at = new Vec3(absolute.getX() + across / 16.0, absolute.getY() + up / 16.0, absolute.getZ() + 1.0 / 16.0);
		BlockHitResult hit = new BlockHitResult(at, Direction.NORTH, absolute, false);
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	/** An Energy Cell holding this much charge. */
	private static ItemStack cell(long energy) {
		ItemStack cell = new ItemStack(JugcraftGuns.ENERGY_CELL);
		Chargeable.setEnergy(cell, energy);
		return cell;
	}
}
