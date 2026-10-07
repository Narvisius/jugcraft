package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.FerrisWheel;
import io.github.jimbozoomer.jugcraft.agriculture.FerrisWheelBlock;
import io.github.jimbozoomer.jugcraft.agriculture.FerrisWheelBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.kinetic.HandCrankBlock;
import io.github.jimbozoomer.jugcraft.kinetic.HandCrankBlockEntity;
import io.github.jimbozoomer.jugcraft.kinetic.JugcraftKinetics;
import io.github.jimbozoomer.jugcraft.kinetic.KineticNetworks;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Game tests for the Ferris wheel (fall addition 27), in a 44 by 44 empty arena so the wheel has room. Run by CI's
 * {@code runGameTest}.
 */
public class FerrisWheelGameTests {
	private static final String ARENA = "jugcraft-test:drone_tower";
	/** The booth's place in the arena, its middle. */
	private static final BlockPos BOOTH = new BlockPos(22, 1, 22);

	private static Block booth() {
		return JugcraftAgriculture.block(FerrisWheelBlock.ID);
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		// Test players start out holding the Creative Tower Guide.
		player.getInventory().clearContent();
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		return player;
	}

	private static boolean earned(ServerPlayer player, String id) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Jugcraft.id(id));
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 10; x <= 34; x++) {
			for (int z = 18; z <= 26; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
	}

	/** A booth facing north at the arena's middle, its wheel raised; returns the wheel. */
	private static FerrisWheel raised(GameTestHelper helper) {
		floor(helper);
		helper.setBlock(BOOTH, booth().defaultBlockState().setValue(FerrisWheelBlock.FACING, Direction.NORTH));
		BlockPos pos = helper.absolutePos(BOOTH);
		FerrisWheel wheel = FerrisWheel.raise(helper.getLevel(), pos, Direction.NORTH);
		helper.assertTrue(wheel != null, "The wheel is raised");
		return wheel;
	}

	/**
	 * Placed facing the player who places it, where there is room, the booth raises one wheel over itself, facing the
	 * same way. Where a block stands in the wheel's way, it can't be placed.
	 */
	@GameTest(structure = ARENA, skyAccess = true)
	public void itStandsWhereThereIsRoom(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(22, 1, 26));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(booth()));
		BlockPos ground = helper.absolutePos(new BlockPos(22, 0, 22));
		player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(ground).add(0.0, 0.5, 0.0), Direction.UP, ground, false));
		BlockPos pos = ground.above();
		helper.assertTrue(level.getBlockState(pos).is(booth()), "The booth is placed");
		Direction facing = level.getBlockState(pos).getValue(FerrisWheelBlock.FACING);
		helper.assertTrue(facing == player.getDirection().getOpposite(), "facing whoever placed it: " + facing);
		List<FerrisWheel> wheels = FerrisWheelBlock.wheels(level, pos);
		helper.assertTrue(wheels.size() == 1 && wheels.get(0).facing() == facing, "One wheel stands over it, facing the same way");
		helper.assertTrue(level.getBlockEntity(pos) instanceof FerrisWheelBlockEntity, "Its drive is there");

		// A second booth with a block in its wheel's way can't be placed.
		BlockPos second = helper.absolutePos(new BlockPos(22, 0, 34));
		helper.setBlock(new BlockPos(22, 0, 34), Blocks.STONE);
		helper.setBlock(new BlockPos(18, 8, 34), Blocks.STONE);
		player.setPos(second.getX() + 0.5, second.getY() + 1, second.getZ() + 4.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(booth()));
		player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(second).add(0.0, 0.5, 0.0), Direction.UP, second, false));
		helper.assertTrue(!level.getBlockState(second.above()).is(booth()) && player.getMainHandItem().is(booth().asItem()),
				"With a block in its way it isn't placed");
		helper.succeed();
	}

	/**
	 * The booth takes at most 12 KE a tick, from the kinetic network as any machine does, and hands it to the wheel. A
	 * hand crank beside it, cranked for 60 ticks, brings the wheel up to full speed; when the crank stops, the wheel
	 * slows to a stop.
	 */
	@GameTest(structure = ARENA, skyAccess = true, maxTicks = 200)
	public void aHandCrankTurnsIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FerrisWheel wheel = raised(helper);
		BlockPos pos = helper.absolutePos(BOOTH);
		FerrisWheelBlockEntity drive = (FerrisWheelBlockEntity) level.getBlockEntity(pos);
		helper.assertTrue(drive.acceptKinetic(Direction.EAST, 100) == FerrisWheel.NEED, "It takes " + FerrisWheel.NEED + " KE a tick");
		helper.assertTrue(drive.acceptKinetic(Direction.WEST, 100) == 0, "and no more in the same tick");
		BlockPos crank = BOOTH.east();
		helper.setBlock(crank, JugcraftKinetics.HAND_CRANK.defaultBlockState().setValue(HandCrankBlock.FACING, Direction.WEST));
		((HandCrankBlockEntity) level.getBlockEntity(helper.absolutePos(crank))).addTurns(60);
		helper.runAfterDelay(55, () -> {
			helper.assertTrue(Math.abs(wheel.speed() - FerrisWheel.FULL_SPEED) < 1.0E-5F, "Cranked, it comes up to full speed: " + wheel.speed());
			helper.assertTrue(wheel.angle() > 0.1F && wheel.lit(), "It has turned, and its lights are on: " + wheel.angle());
		});
		helper.runAfterDelay(150, () -> {
			helper.assertTrue(wheel.speed() == 0.0F, "When the crank stops, so does the wheel: " + wheel.speed());
			helper.succeed();
		});
	}

	/**
	 * Using the booth seats a player in the car at the bottom; a second sits beside them; a third finds the car full. A
	 * rider sits in their car, just over the booth. Getting off sets them down on the ground in front of the booth.
	 */
	@GameTest(structure = ARENA, skyAccess = true)
	public void ridersBoardAtTheBottomAndGetOffOnTheGround(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FerrisWheel wheel = raised(helper);
		BlockPos pos = helper.absolutePos(BOOTH);
		ServerPlayer first = player(helper, new BlockPos(22, 1, 20));
		ServerPlayer second = player(helper, new BlockPos(21, 1, 20));
		ServerPlayer third = player(helper, new BlockPos(23, 1, 20));
		ServerPlayer far = player(helper, new BlockPos(22, 1, 12));
		helper.assertTrue(!wheel.board(far), "Not from afar");
		helper.assertTrue(wheel.board(first) && first.getVehicle() == wheel, "A player boards");
		int car = FerrisWheel.bottomCar(wheel.angle());
		helper.assertTrue(wheel.seatOf(first) / FerrisWheel.SEATS == car, "into the car at the bottom");
		helper.assertTrue(wheel.board(second) && wheel.seatOf(second) / FerrisWheel.SEATS == car && wheel.seatOf(second) != wheel.seatOf(first),
				"A second sits beside them");
		helper.assertTrue(!wheel.board(third) && !third.isPassenger(), "A third finds the car full");
		wheel.positionRider(first);
		double up = first.getY() - pos.getY();
		helper.assertTrue(up > 0.0 && up < 2.5, "A rider sits in their car just over the booth: " + up + " up");
		first.stopRiding();
		BlockPos front = pos.relative(Direction.NORTH);
		helper.assertTrue(!first.isPassenger() && first.blockPosition().equals(front), "Getting off sets them down in front of the booth: "
				+ first.blockPosition() + ", not " + front);
		helper.assertTrue(wheel.seatOf(first) < 0 && wheel.board(third), "Their seat is free again");
		helper.succeed();
	}

	/** A block where a car would go next stops the wheel, jammed; cleared, it turns again. */
	@GameTest(structure = ARENA, skyAccess = true, maxTicks = 60)
	public void aBlockInTheWayJamsIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FerrisWheel wheel = raised(helper);
		helper.assertTrue(!wheel.blocked(level, 0.0F), "Nothing is in its way");
		// A block where the car on the wheel's right-hand side (car 2, at three o'clock) hangs: north-facing, its right is east.
		Vec3 seat = wheel.toWorld(FerrisWheel.pivot(2, 0.0F).x, FerrisWheel.pivot(2, 0.0F).y - FerrisWheel.SEAT_DOWN + 0.3, FerrisWheel.SEAT_BACK);
		BlockPos in = BlockPos.containing(seat);
		level.setBlockAndUpdate(in, Blocks.STONE.defaultBlockState());
		helper.assertTrue(wheel.blocked(level, 0.0F), "A block in a car's way blocks it");
		BlockPos source = helper.absolutePos(BOOTH).east();
		helper.onEachTick(() -> KineticNetworks.push(level, source, Direction.WEST, 16));
		helper.runAfterDelay(10, () -> {
			helper.assertTrue(wheel.jammed() && wheel.speed() == 0.0F, "Driven, it stays jammed");
			level.setBlockAndUpdate(in, Blocks.AIR.defaultBlockState());
		});
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(!wheel.jammed() && wheel.speed() > 0.0F, "Cleared, it turns again");
			helper.succeed();
		});
	}

	/** Riding all the way round earns Round and Round; with a friend beside you, Two to a Car too. */
	@GameTest(structure = ARENA, skyAccess = true)
	public void aWholeTurnEarnsItsAdvancements(GameTestHelper helper) {
		FerrisWheel wheel = raised(helper);
		ServerPlayer alone = player(helper, new BlockPos(22, 1, 20));
		helper.assertTrue(wheel.board(alone), "One rider boards");
		for (int i = 0; i < FerrisWheel.TURN_TICKS / 2; i++) {
			wheel.advance(FerrisWheel.FULL_SPEED);
		}
		helper.assertTrue(!earned(alone, "round_and_round"), "Half way round is not enough");
		ServerPlayer friend = player(helper, new BlockPos(21, 1, 20));
		wheel.setAngle(0.0F);
		alone.stopRiding();
		helper.assertTrue(wheel.board(alone) && wheel.board(friend), "Two board the same car");
		for (int i = 0; i <= FerrisWheel.TURN_TICKS; i++) {
			wheel.advance(FerrisWheel.FULL_SPEED);
		}
		helper.assertTrue(earned(alone, "round_and_round") && earned(friend, "round_and_round"), "All the way round earns Round and Round");
		helper.assertTrue(earned(alone, "two_to_a_car") && earned(friend, "two_to_a_car"), "and together, Two to a Car");
		helper.succeed();
	}

	/** Breaking the booth takes the wheel down: its riders are set on the ground, and the booth drops once. */
	@GameTest(structure = ARENA, skyAccess = true)
	public void breakingTheBoothTakesTheWheelDown(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FerrisWheel wheel = raised(helper);
		BlockPos pos = helper.absolutePos(BOOTH);
		ServerPlayer rider = player(helper, new BlockPos(22, 1, 20));
		helper.assertTrue(wheel.board(rider), "A rider boards");
		level.destroyBlock(pos, true);
		helper.assertTrue(wheel.isRemoved() && !rider.isPassenger(), "The wheel goes, and its rider is off");
		int dropped = 0;
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3.0))) {
			if (item.getItem().is(booth().asItem())) {
				dropped += item.getItem().getCount();
			}
		}
		helper.assertTrue(dropped == 1, "The booth drops once, not " + dropped);
		helper.succeed();
	}

	/** The recipe, the advancements and the loot table load. */
	@GameTest
	public void ferrisWheelDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(FerrisWheelBlock.ID))).isPresent(),
				"The Ferris Wheel has a recipe");
		for (String id : List.of("round_and_round", "two_to_a_car")) {
			helper.assertTrue(level.getServer().getAdvancements().get(Jugcraft.id(id)) != null, id + " loads");
		}
		helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
				Jugcraft.id("blocks/" + FerrisWheelBlock.ID))) != LootTable.EMPTY, "Its loot loads");
		helper.succeed();
	}
}
