package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.nuclear.JugcraftNuclear;
import io.github.jimbozoomer.jugcraft.nuclear.LeadDoorBlock;
import io.github.jimbozoomer.jugcraft.nuclear.PlantDoorBlock;
import io.github.jimbozoomer.jugcraft.nuclear.PlantDoorBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.PlantInterior;
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
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
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
		((ShieldedStorageBlockEntity) level.getBlockEntity(at)).setItem(4, new ItemStack(JugcraftNuclear.CHECK_SOURCE));
		level.destroyBlock(at, true);
		var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(2));
		boolean found = false;
		for (ItemEntity drop : drops) {
			ItemStack stack = drop.getItem();
			var contents = stack.get(DataComponents.CONTAINER);
			if (stack.is(StorageBlocks.BLOCKS.get("lead_pig").asItem()) && contents != null
					&& contents.nonEmptyItemCopyStream().anyMatch(s -> s.is(JugcraftNuclear.CHECK_SOURCE))) {
				found = true;
			}
			if (stack.is(JugcraftNuclear.CHECK_SOURCE)) {
				throw new AssertionError("the pig's contents spilled");
			}
			drop.discard();
		}
		helper.assertTrue(found, "the pig dropped with its check source in it (" + drops.size() + " drops)");
		helper.succeed();
	}

	/**
	 * Justin: an airlock with the doors. Two lead-lined doors: with one open the other won't open, the beacons by the
	 * shut one turn red; the open one closes itself, and the beacons go green again.
	 */
	@GameTest(maxTicks = LeadDoorBlock.CLOSE_AFTER + PlantDoorBlock.SWING_OPEN + PlantDoorBlock.SWING_SHUT + 40)
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
		// Doors round: it swings open, stands open for the closer's time, then swings shut.
		helper.runAfterDelay(PlantDoorBlock.SWING_OPEN + LeadDoorBlock.CLOSE_AFTER + PlantDoorBlock.SWING_SHUT + 10, () -> {
			helper.assertTrue(!level.getBlockState(a).getValue(DoorBlock.OPEN), "the first door has closed itself");
			helper.assertTrue(level.getBlockState(lampB).getValue(WarningBeaconBlock.COLOUR) == WarningBeaconBlock.Colour.GREEN, "the beacon is green again");
			use(level, player, b);
			helper.assertTrue(level.getBlockState(b).getValue(DoorBlock.OPEN), "now the second door opens");
			helper.succeed();
		});
	}

	/**
	 * Doors round: a steel door swings open (MOVING for its swing, then open), its closer shuts it a few seconds later
	 * (swinging shut), and its leaf is the renderer's part model for its kind.
	 */
	@GameTest(maxTicks = PlantDoorBlock.SWING_OPEN + PlantDoorBlock.CLOSE_AFTER + PlantDoorBlock.SWING_SHUT + 40)
	public void steelDoorSwingsAndCloses(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		BlockPos door = helper.absolutePos(new BlockPos(2, 1, 2));
		placeDoor(level, door, "steel_door", Direction.NORTH, DoorHingeSide.LEFT, false);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setPos(Vec3.atCenterOf(door.south(2)));
		use(level, player, door);
		helper.assertTrue(level.getBlockState(door).getValue(DoorBlock.OPEN), "the door opens");
		helper.assertTrue(level.getBlockState(door).getValue(PlantDoorBlock.MOVING), "and swings");
		helper.assertTrue(level.getBlockState(door.above()).getValue(PlantDoorBlock.MOVING), "both halves swing");
		helper.assertTrue(level.getBlockEntity(door) instanceof PlantDoorBlockEntity, "its lower half has the door's block entity");
		helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.PlantDoors.leafPart(level.getBlockState(door)) == 0,
				"a single left-hinged steel door's leaf is part 0");
		helper.runAfterDelay(PlantDoorBlock.SWING_OPEN + 2, () -> {
			helper.assertTrue(!level.getBlockState(door).getValue(PlantDoorBlock.MOVING), "the swing is over");
			helper.assertTrue(level.getBlockState(door).getValue(DoorBlock.OPEN), "it stands open");
		});
		helper.runAfterDelay(PlantDoorBlock.SWING_OPEN + PlantDoorBlock.CLOSE_AFTER + 2, () ->
				helper.assertTrue(!level.getBlockState(door).getValue(DoorBlock.OPEN), "the closer has swung it shut"));
		helper.runAfterDelay(PlantDoorBlock.SWING_OPEN + PlantDoorBlock.CLOSE_AFTER + PlantDoorBlock.SWING_SHUT + 4, () -> {
			helper.assertTrue(!level.getBlockState(door).getValue(PlantDoorBlock.MOVING), "and it has stopped");
			helper.succeed();
		});
	}

	/**
	 * Doors round (Justin: the reader lets in the owner and the owner's party): from the reader's side a stranger is
	 * refused and the owner let in; from the other side anyone may leave. A double door's leaves know each other.
	 */
	@GameTest(maxTicks = 60)
	public void readerLetsInTheOwnerAndPairsPair(GameTestHelper helper) {
		if (TestFocus.skip("nuclear")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		BlockPos door = helper.absolutePos(new BlockPos(2, 1, 2));
		placeDoor(level, door, "steel_door", Direction.NORTH, DoorHingeSide.LEFT, true);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		// Facing north: you push it walking north, from the south (the reader's side).
		player.setPos(Vec3.atCenterOf(door.south(2)));
		((PlantDoorBlockEntity) level.getBlockEntity(door)).setOwner(java.util.UUID.randomUUID());
		use(level, player, door);
		helper.assertTrue(!level.getBlockState(door).getValue(DoorBlock.OPEN), "a stranger is refused at the reader");
		player.setPos(Vec3.atCenterOf(door.north(2)));
		use(level, player, door);
		helper.assertTrue(level.getBlockState(door).getValue(DoorBlock.OPEN), "anyone may leave from the other side");
		BlockPos second = helper.absolutePos(new BlockPos(5, 1, 2));
		placeDoor(level, second, "steel_door", Direction.NORTH, DoorHingeSide.LEFT, true);
		((PlantDoorBlockEntity) level.getBlockEntity(second)).setOwner(player.getUUID());
		player.setPos(Vec3.atCenterOf(second.south(2)));
		use(level, player, second);
		helper.assertTrue(level.getBlockState(second).getValue(DoorBlock.OPEN), "the owner is let in");
		// A pair facing north: the left-hinged leaf's latch side is east, where the right-hinged leaf stands.
		BlockPos left = helper.absolutePos(new BlockPos(8, 1, 2)), right = left.east();
		placeDoor(level, left, "steel_door", Direction.NORTH, DoorHingeSide.LEFT, false);
		placeDoor(level, right, "steel_door", Direction.NORTH, DoorHingeSide.RIGHT, false);
		helper.assertTrue(level.getBlockState(left).getValue(PlantDoorBlock.PAIRED) && level.getBlockState(right).getValue(PlantDoorBlock.PAIRED)
				&& level.getBlockState(right.above()).getValue(PlantDoorBlock.PAIRED), "the pair's leaves are paired");
		helper.succeed();
	}

	private static void placeDoor(net.minecraft.world.level.Level level, BlockPos pos, String id, Direction facing, DoorHingeSide hinge, boolean reader) {
		BlockState lower = PlantInterior.BLOCKS.get(id).defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HINGE, hinge)
				.setValue(PlantDoorBlock.READER, reader);
		level.setBlock(pos, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 3);
		level.setBlock(pos.above(), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 3);
	}

	private static void use(net.minecraft.world.level.Level level, Player player, BlockPos door) {
		level.getBlockState(door).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(door), Direction.SOUTH, door, false));
	}
}
