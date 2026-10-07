package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.nuclear.JugcraftNuclear;
import io.github.jimbozoomer.jugcraft.nuclear.LeadDoorBlock;
import io.github.jimbozoomer.jugcraft.nuclear.Radiation;
import io.github.jimbozoomer.jugcraft.nuclear.RadiationField;
import io.github.jimbozoomer.jugcraft.nuclear.ShieldedStorageBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.StorageBlocks;
import io.github.jimbozoomer.jugcraft.nuclear.WarningBeaconBlock;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Round 14's storage for radioactive material (the radwaste building): what the shielded stores let out, the lead
 * pig keeping its contents, and the airlock's lead-lined doors (interlock, closer, beacons).
 */
public class StorageGameTests {
	/** A waste drum lets 0.2% of what is in it out, a chest 80%; a lead pig carried with a source in it, 0.1%. */
	@GameTest(maxTicks = 20)
	public void shieldedStoresLetLittleOut(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		helper.setBlock(new BlockPos(1, 1, 1), StorageBlocks.BLOCKS.get("waste_drum"));
		helper.setBlock(new BlockPos(3, 1, 1), Blocks.CHEST);
		if (!(level.getBlockEntity(helper.absolutePos(new BlockPos(1, 1, 1))) instanceof ShieldedStorageBlockEntity drum)
				|| !(level.getBlockEntity(helper.absolutePos(new BlockPos(3, 1, 1))) instanceof net.minecraft.world.Container chest)) {
			throw new AssertionError("the drum and the chest have their stores");
		}
		helper.assertTrue(drum.getContainerSize() == 18, "a drum holds 18: " + drum.getContainerSize());
		drum.setItem(0, new ItemStack(JugcraftNuclear.CHECK_SOURCE));
		chest.setItem(0, new ItemStack(JugcraftNuclear.CHECK_SOURCE));
		double inDrum = RadiationField.held(drum), inChest = RadiationField.held((net.minecraft.world.level.block.entity.BlockEntity) chest);
		drum.clearContent();
		chest.clearContent();
		helper.assertTrue(Math.abs(inDrum - 2 * 0.002) < 1e-9 && Math.abs(inChest - 2 * 0.8) < 1e-9, "drum " + inDrum + ", chest " + inChest);
		ItemStack pig = new ItemStack(StorageBlocks.BLOCKS.get("lead_pig"));
		pig.set(DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.fromItems(java.util.List.of(new ItemStack(JugcraftNuclear.CHECK_SOURCE))));
		helper.assertTrue(Math.abs(Radiation.strength(pig) - 2 * 0.001) < 1e-9, "a lead pig carried with a check source in it: " + Radiation.strength(pig));
		helper.succeed();
	}

	/** Broken, a lead pig drops itself with what was in it (like a shulker box); a locker spills its contents. */
	@GameTest(maxTicks = 20)
	public void leadPigKeepsItsContents(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(2, 1, 2));
		level.setBlock(at, StorageBlocks.BLOCKS.get("lead_pig").defaultBlockState(), 3);
		((ShieldedStorageBlockEntity) level.getBlockEntity(at)).setItem(4, new ItemStack(JugcraftNuclear.CHECK_SOURCE, 3));
		level.destroyBlock(at, true);
		var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(2));
		boolean found = false;
		for (ItemEntity drop : drops) {
			ItemStack stack = drop.getItem();
			var contents = stack.get(DataComponents.CONTAINER);
			if (stack.is(StorageBlocks.BLOCKS.get("lead_pig").asItem()) && contents != null
					&& contents.nonEmptyItemCopyStream().anyMatch(s -> s.is(JugcraftNuclear.CHECK_SOURCE) && s.getCount() == 3)) {
				found = true;
			}
			if (stack.is(JugcraftNuclear.CHECK_SOURCE)) {
				throw new AssertionError("the pig's contents spilled");
			}
			drop.discard();
		}
		helper.assertTrue(found, "the pig dropped with its check sources in it (" + drops.size() + " drops)");
		helper.succeed();
	}

	/**
	 * Justin: an airlock with the doors. Two lead-lined doors: with one open the other won't open, the beacons by the
	 * shut one turn red; the open one closes itself, and the beacons go green again.
	 */
	@GameTest(maxTicks = LeadDoorBlock.CLOSE_AFTER + 40)
	public void airlockDoorsInterlockAndClose(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		BlockPos a = helper.absolutePos(new BlockPos(3, 1, 1)), b = helper.absolutePos(new BlockPos(3, 1, 5));
		for (BlockPos door : new BlockPos[] {a, b}) {
			BlockState lower = StorageBlocks.LEAD_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
			level.setBlock(door, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 3);
			level.setBlock(door.above(), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 3);
		}
		BlockPos lampA = a.offset(0, 2, 1), lampB = b.offset(0, 2, 1);
		BlockState green = StorageBlocks.WARNING_BEACON.defaultBlockState().setValue(WarningBeaconBlock.FACING, Direction.SOUTH)
				.setValue(WarningBeaconBlock.COLOUR, WarningBeaconBlock.Colour.GREEN);
		level.setBlock(lampA, green, 3);
		level.setBlock(lampB, green, 3);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(0, 1, 3))));
		use(level, player, a);
		helper.assertTrue(level.getBlockState(a).getValue(DoorBlock.OPEN), "the first door opens");
		helper.assertTrue(level.getBlockState(lampB).getValue(WarningBeaconBlock.COLOUR) == WarningBeaconBlock.Colour.RED, "the other door's beacon is red");
		helper.assertTrue(level.getBlockState(lampA).getValue(WarningBeaconBlock.COLOUR) == WarningBeaconBlock.Colour.GREEN, "the open door's beacon stays green");
		use(level, player, b);
		helper.assertTrue(!level.getBlockState(b).getValue(DoorBlock.OPEN), "the second door won't open while the first is open");
		helper.runAfterDelay(LeadDoorBlock.CLOSE_AFTER + 10, () -> {
			helper.assertTrue(!level.getBlockState(a).getValue(DoorBlock.OPEN), "the first door has closed itself");
			helper.assertTrue(level.getBlockState(lampB).getValue(WarningBeaconBlock.COLOUR) == WarningBeaconBlock.Colour.GREEN, "the beacon is green again");
			use(level, player, b);
			helper.assertTrue(level.getBlockState(b).getValue(DoorBlock.OPEN), "now the second door opens");
			helper.succeed();
		});
	}

	private static void use(net.minecraft.world.level.Level level, Player player, BlockPos door) {
		level.getBlockState(door).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(door), Direction.SOUTH, door, false));
	}
}
