package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CarvingFace;
import io.github.jimbozoomer.jugcraft.agriculture.CarvingTemplates;
import io.github.jimbozoomer.jugcraft.agriculture.GiantPumpkinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GiantPumpkinBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.GiantPumpkinVineBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HalloweenSeason;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinBoat;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinBoatData;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCarvings;
import io.github.jimbozoomer.jugcraft.agriculture.RegattaBuoyBlock;
import io.github.jimbozoomer.jugcraft.agriculture.RegattaFlagBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.TrickOrTreat;
import io.github.jimbozoomer.jugcraft.agriculture.TrickOrTreat.Result;
import java.time.Clock;
import java.time.Instant;
import java.time.MonthDay;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the pumpkin regatta and trick-or-treating: hollowing giant pumpkins into boats, boat
 * speed and seats, keeping a boat's data, buoys, a timed run on the server, the Halloween window, and every
 * seasonal rule (activation, deactivation, a restart across the boundary, no duplicate treats, earned
 * content kept).
 */
public class RegattaGameTests {
	private static final BlockPos VINE = new BlockPos(1, 2, 2);
	/** Dusk of a far-off night, so no other test's record shares it. */
	private static final long NIGHT = TrickOrTreat.DUSK + TrickOrTreat.DAY * 5000;
	private static final BlockPos DOOR = new BlockPos(3, 2, 3);
	private static final BlockPos BED = DOOR.north(3);
	private static final Set<Item> TREATS = new HashSet<>();

	private static final EntityType<Chicken> CHICKEN = vanilla("chicken");
	private static final EntityType<Villager> VILLAGER = vanilla("villager");

	@SuppressWarnings("unchecked")
	private static <T extends Entity> EntityType<T> vanilla(String id) {
		return (EntityType<T>) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(id));
	}

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static ServerPlayer serverPlayer(GameTestHelper helper, BlockPos standAt, ItemStack held) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	private static int count(ServerPlayer player, Item item) {
		int count = 0;
		for (int slot = 0; slot < 36; slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static ItemStack find(ServerPlayer player, Item item) {
		for (int slot = 0; slot < 36; slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(item)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static int itemsAround(GameTestHelper helper, BlockPos pos, Item item) {
		int count = 0;
		for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(4.0))) {
			if (entity.getItem().is(item)) {
				count += entity.getItem().getCount();
			}
		}
		return count;
	}

	// ---------------------------------------------------------------- giant pumpkins to boats

	private static GiantPumpkinBlockEntity seedling(GameTestHelper helper) {
		helper.setBlock(VINE.below(), Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
		for (int x = 2; x <= 4; x++) {
			for (int z = 1; z <= 3; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
			}
		}
		GiantPumpkinVineBlock vine = (GiantPumpkinVineBlock) block("giant_pumpkin_vine");
		helper.setBlock(VINE, vine.defaultBlockState().setValue(GiantPumpkinVineBlock.AGE, GiantPumpkinVineBlock.MAX_AGE));
		helper.assertTrue(vine.growFruit(helper.getLevel(), helper.absolutePos(VINE), Direction.EAST), "A grown vine should set a fruit");
		return helper.getBlockEntity(VINE.east(), GiantPumpkinBlockEntity.class);
	}

	private static GiantPumpkinBlockEntity master(GameTestHelper helper, BlockPos pos) {
		GiantPumpkinBlockEntity master = GiantPumpkinBlock.master(helper.getLevel(), helper.absolutePos(pos), helper.getBlockState(pos));
		if (master == null) {
			throw helper.assertionException("No giant pumpkin at " + pos + ": " + helper.getBlockState(pos));
		}
		return master;
	}

	/**
	 * Hollowing a full-grown, carved and lit giant pumpkin from the top makes a Pumpkin Barge that keeps its
	 * weight, carving and torch; the pumpkin is gone, its guts and giant seeds come out, the knife wears.
	 */
	@GameTest
	public void hollowingAFullGrownGiantMakesABarge(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		seedling(helper).feed(level, GiantPumpkinBlockEntity.GROW_TO_TWO, level.getRandom());
		master(helper, VINE.east()).feed(level, GiantPumpkinBlockEntity.GROW_TO_THREE - GiantPumpkinBlockEntity.GROW_TO_TWO, level.getRandom());
		GiantPumpkinBlockEntity giant = master(helper, VINE.east());
		helper.assertTrue(giant.fullGrown(), "The pumpkin should be full grown");
		int[] face = CarvingFace.scale(CarvingTemplates.ALL.get(0).face(), GiantPumpkinBlockEntity.FACE_SIZE);
		giant.setFace(Direction.SOUTH, face, null);
		giant.setLit(true);
		int weight = giant.weight();

		BlockPos top = new BlockPos(3, 4, 2);
		ServerPlayer player = serverPlayer(helper, top.above(), new ItemStack(item("carving_knife")));
		PumpkinCarvings.HollowResult result = PumpkinCarvings.hollow(player, helper.absolutePos(top), InteractionHand.MAIN_HAND);
		helper.assertTrue(result == PumpkinCarvings.HollowResult.HOLLOWED, "Expected the giant hollowed, got " + result);
		for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(2, 2, 1), new BlockPos(4, 4, 3))) {
			helper.assertBlockNotPresent(block("giant_pumpkin"), pos.immutable());
		}
		PumpkinBoatData data = find(player, item("pumpkin_barge")).get(JugcraftAgriculture.PUMPKIN_BOAT);
		helper.assertTrue(data != null && data.weight() == weight && data.carved() && data.lit()
				&& Arrays.equals(data.face(Direction.SOUTH.get2DDataValue()), face), "The barge should keep weight, carving and torch: " + data);
		helper.assertTrue(itemsAround(helper, top, item("pumpkin_guts")) >= 4, "Hollowing a full-grown giant gives 4-8 guts");
		helper.assertTrue(itemsAround(helper, top, item("giant_pumpkin_seeds")) >= 1, "and 1-3 giant pumpkin seeds");
		helper.assertTrue(itemsAround(helper, top, Items.PUMPKIN) == 0, "but none of the pumpkins breaking it gives");
		helper.assertTrue(player.getMainHandItem().getDamageValue() == 1, "Hollowing costs the knife one use");
		helper.succeed();
	}

	/**
	 * The knife on top of a 2x2x2 giant only says how until the player sneaks; then it hollows it into a
	 * Pumpkin Racer weighing its base plus its growth, with guts but no giant seeds.
	 */
	@GameTest
	public void sneakingWithTheKnifeHollowsATwoBlockGiantIntoARacer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		seedling(helper).feed(level, GiantPumpkinBlockEntity.GROW_TO_TWO, level.getRandom());
		GiantPumpkinBlockEntity giant = master(helper, VINE.east());
		helper.assertTrue(giant.size() == 2, "The pumpkin should be 2 blocks wide");
		int points = giant.points();
		BlockPos top = new BlockPos(2, 3, 2);
		ItemStack knife = new ItemStack(item("carving_knife"));
		ServerPlayer player = serverPlayer(helper, top.above(), knife);
		BlockPos absolute = helper.absolutePos(top);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).add(0, 0.5, 0), Direction.UP, absolute, false);
		knife.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
		helper.assertBlockPresent(block("giant_pumpkin"), top);
		player.setShiftKeyDown(true);
		knife.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
		helper.assertBlockNotPresent(block("giant_pumpkin"), top);
		PumpkinBoatData data = find(player, item("pumpkin_racer")).get(JugcraftAgriculture.PUMPKIN_BOAT);
		int expected = PumpkinBoat.RACER_BASE_WEIGHT + points * GiantPumpkinBlockEntity.WEIGHT_PER_POINT;
		helper.assertTrue(data != null && data.weight() == expected && !data.carved(), "Expected a " + expected + " kg racer: " + data);
		helper.assertTrue(itemsAround(helper, top, item("pumpkin_guts")) >= 2, "Hollowing a 2x2x2 giant gives 2-4 guts");
		helper.assertTrue(itemsAround(helper, top, item("giant_pumpkin_seeds")) == 0, "but no giant seeds");
		helper.succeed();
	}

	private static void close(GameTestHelper helper, double actual, double expected, String what) {
		helper.assertTrue(Math.abs(actual - expected) < 1e-9, what + ": expected " + expected + ", got " + actual);
	}

	/** Lighter boats are faster: each kind's top speed runs from its lightest to its heaviest, and the tick factor reaches it. */
	@GameTest
	public void boatSpeedFollowsWeight(GameTestHelper helper) {
		PumpkinBoat.Kind barge = PumpkinBoat.Kind.BARGE;
		PumpkinBoat.Kind racer = PumpkinBoat.Kind.RACER;
		close(helper, barge.speedRatio(GiantPumpkinBlockEntity.START_WEIGHT), 0.95, "lightest barge");
		close(helper, barge.speedRatio(GiantPumpkinBlockEntity.MAX_WEIGHT), 0.70, "heaviest barge");
		close(helper, racer.speedRatio(PumpkinBoat.RACER_LIGHTEST), 1.30, "lightest racer");
		close(helper, racer.speedRatio(PumpkinBoat.RACER_HEAVIEST), 1.15, "heaviest racer");
		helper.assertTrue(PumpkinBoat.RACER_LIGHTEST == 62 && PumpkinBoat.RACER_HEAVIEST == 126, "A racer weighs 62 to 126 kg");
		for (PumpkinBoat.Kind kind : PumpkinBoat.Kind.values()) {
			for (int weight = kind.lightest; weight <= kind.heaviest; weight += 8) {
				double friction = PumpkinBoat.WATER_FRICTION;
				double topSpeed = (1 - friction) / (1 - friction * kind.tickFactor(weight));
				close(helper, topSpeed, kind.speedRatio(weight), kind + " top speed at " + weight + " kg");
			}
		}
		helper.assertTrue(racer.tickFactor(PumpkinBoat.RACER_HEAVIEST) > 1.0 && barge.tickFactor(GiantPumpkinBlockEntity.START_WEIGHT) < 1.0,
				"Racers outpace a boat and barges lag behind");
		helper.succeed();
	}

	private static PumpkinBoat boat(GameTestHelper helper, PumpkinBoat.Kind kind, BlockPos pos, PumpkinBoatData data) {
		PumpkinBoat boat = helper.spawn(JugcraftAgriculture.boatType(kind), pos);
		ItemStack stack = new ItemStack(item(kind.id));
		stack.set(JugcraftAgriculture.PUMPKIN_BOAT, data);
		boat.setItem(stack);
		return boat;
	}

	/** The barge seats four and the racer one; a broken boat drops its item with its weight, carving and torch. */
	@GameTest
	public void boatsSeatTheirCrewAndGiveBackWhatTheyKeep(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
		int[][] faces = new int[4][];
		faces[0] = CarvingFace.scale(CarvingTemplates.ALL.get(1).face(), GiantPumpkinBlockEntity.FACE_SIZE);
		PumpkinBoatData kept = PumpkinBoatData.of(345, true, faces);
		PumpkinBoat barge = boat(helper, PumpkinBoat.Kind.BARGE, new BlockPos(2, 1, 2), kept);
		for (int seat = 0; seat < 4; seat++) {
			Chicken chicken = helper.spawnWithNoFreeWill(CHICKEN, new BlockPos(6, 1, 6));
			helper.assertTrue(chicken.startRiding(barge), "The barge should seat crew member " + (seat + 1));
		}
		Chicken fifth = helper.spawnWithNoFreeWill(CHICKEN, new BlockPos(6, 1, 6));
		helper.assertFalse(fifth.startRiding(barge), "A barge seats four");
		PumpkinBoat racer = boat(helper, PumpkinBoat.Kind.RACER, new BlockPos(6, 1, 2), PumpkinBoat.Kind.RACER.defaultData());
		helper.assertTrue(fifth.startRiding(racer), "A racer seats one");
		helper.assertFalse(helper.spawnWithNoFreeWill(CHICKEN, new BlockPos(6, 1, 6)).startRiding(racer), "and only one");

		barge.ejectPassengers();
		barge.hurtServer(level, level.damageSources().generic(), 10.0F);
		helper.assertTrue(barge.isRemoved(), "Enough damage breaks the barge");
		boolean dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(2, 1, 2))).inflate(3.0)).stream()
				.anyMatch(entity -> entity.getItem().is(item("pumpkin_barge")) && kept.equals(entity.getItem().get(JugcraftAgriculture.PUMPKIN_BOAT)));
		helper.assertTrue(dropped, "A broken barge drops its item with everything it kept");
		helper.succeed();
	}

	// ---------------------------------------------------------------- the regatta

	/** A buoy floats on still water only; using it counts its number up, sneaking down. */
	@GameTest
	public void buoysFloatAndCountUp(GameTestHelper helper) {
		BlockPos water = new BlockPos(2, 1, 2);
		BlockPos buoy = water.above();
		helper.setBlock(water.below(), Blocks.STONE);
		helper.setBlock(water, Blocks.WATER);
		helper.setBlock(buoy, block("regatta_buoy"));
		helper.assertTrue(helper.getBlockState(buoy).canSurvive(helper.getLevel(), helper.absolutePos(buoy)), "A buoy floats on water");
		helper.assertFalse(block("regatta_buoy").defaultBlockState().canSurvive(helper.getLevel(), helper.absolutePos(new BlockPos(5, 2, 5))),
				"but not on air or land");
		ServerPlayer player = serverPlayer(helper, new BlockPos(2, 1, 4), ItemStack.EMPTY);
		BlockPos absolute = helper.absolutePos(buoy);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.SOUTH, absolute, false);
		helper.useBlock(buoy, player, hit);
		helper.assertTrue(helper.getBlockState(buoy).getValue(RegattaBuoyBlock.NUMBER) == 2, "Using a buoy counts up");
		player.setShiftKeyDown(true);
		helper.useBlock(buoy, player, hit);
		helper.useBlock(buoy, player, hit);
		helper.assertTrue(helper.getBlockState(buoy).getValue(RegattaBuoyBlock.NUMBER) == RegattaBuoyBlock.MAX_NUMBER,
				"Sneaking counts down, past 1 to " + RegattaBuoyBlock.MAX_NUMBER);
		helper.setBlock(water, Blocks.STONE);
		helper.assertBlockNotPresent(block("regatta_buoy"), buoy);
		helper.succeed();
	}

	/**
	 * A run, all on the server: the flag surveys its two buoys; the driver's racer counts down without taking
	 * any mark, then (sitting within reach of everything) passes buoy 1, buoy 2 and the finish on three ticks.
	 * The flag records the time and gives a first-prize ribbon once; leaving the boat voids a run.
	 */
	@GameTest(maxTicks = 200)
	public void aRegattaRunIsTimedOnTheServer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos flagPos = new BlockPos(1, 1, 1);
		helper.setBlock(flagPos.below(), Blocks.STONE);
		helper.setBlock(flagPos, block("regatta_flag"));
		BlockPos[] buoys = {new BlockPos(4, 2, 2), new BlockPos(4, 2, 4)};
		for (int i = 0; i < buoys.length; i++) {
			helper.setBlock(buoys[i].below(2), Blocks.STONE);
			helper.setBlock(buoys[i].below(), Blocks.WATER);
			helper.setBlock(buoys[i], block("regatta_buoy").defaultBlockState().setValue(RegattaBuoyBlock.NUMBER, i + 1));
		}
		RegattaFlagBlockEntity flag = helper.getBlockEntity(flagPos, RegattaFlagBlockEntity.class);
		List<BlockPos> course = flag.survey(level);
		helper.assertTrue(course.size() >= 2 && course.get(0).equals(helper.absolutePos(buoys[0])) && course.get(1).equals(helper.absolutePos(buoys[1])),
				"The flag should find buoys 1 and 2 in order: " + course);

		helper.setBlock(new BlockPos(2, 0, 3), Blocks.STONE);
		PumpkinBoat boat = boat(helper, PumpkinBoat.Kind.RACER, new BlockPos(2, 1, 3), PumpkinBoat.Kind.RACER.defaultData());
		ServerPlayer driver = serverPlayer(helper, new BlockPos(2, 1, 3), ItemStack.EMPTY);
		helper.assertTrue(driver.startRiding(boat), "The driver should board the racer");
		helper.assertTrue(boat.startRace(driver, helper.absolutePos(flagPos), List.of(helper.absolutePos(buoys[0]), helper.absolutePos(buoys[1]))),
				"The driver should start a run");
		ServerPlayer passenger = serverPlayer(helper, new BlockPos(5, 1, 5), ItemStack.EMPTY);
		helper.assertFalse(boat.startRace(passenger, helper.absolutePos(flagPos), course), "Only the driver starts a run");

		helper.runAtTickTime(PumpkinBoat.COUNTDOWN_TICKS / 2, () -> helper.assertTrue(boat.racing() && boat.nextMark() == 0,
				"No mark counts during the countdown"));
		helper.succeedWhen(() -> {
			helper.assertFalse(boat.racing(), "The run should be over");
			List<RegattaFlagBlockEntity.Entry> board = flag.board();
			helper.assertTrue(board.size() == 1 && board.get(0).racer().equals(driver.getUUID()) && board.get(0).ticks() == 2,
					"Buoy 1 at Go, buoy 2 a tick later, the finish the next: 2 ticks, got " + board);
			helper.assertTrue(count(driver, item("first_prize_ribbon")) == 1, "The winner gets a first-prize ribbon");
			flag.finish(driver, 1);
			helper.assertTrue(count(driver, item("first_prize_ribbon")) == 1 && flag.board().get(0).ticks() == 1,
					"A better time replaces the old one, but no second ribbon");
			helper.assertTrue(boat.startRace(driver, helper.absolutePos(flagPos), course), "A new run starts");
			driver.stopRiding();
			boat.tick();
			helper.assertFalse(boat.racing(), "Leaving the boat voids the run");
		});
	}

	// ---------------------------------------------------------------- the Halloween window

	/** The window follows the operator's dates (both days in, wrapping past New Year) in their time zone; on and off override it. */
	@GameTest
	public void theHalloweenWindowFollowsTheConfiguredDates(GameTestHelper helper) {
		MonthDay start = MonthDay.of(10, 20);
		MonthDay end = MonthDay.of(11, 3);
		helper.assertTrue(HalloweenSeason.inWindow(MonthDay.of(10, 20), start, end) && HalloweenSeason.inWindow(MonthDay.of(11, 3), start, end)
				&& !HalloweenSeason.inWindow(MonthDay.of(11, 4), start, end) && !HalloweenSeason.inWindow(MonthDay.of(10, 19), start, end),
				"Both ends of the window are in it");
		helper.assertTrue(HalloweenSeason.inWindow(MonthDay.of(1, 2), MonthDay.of(12, 20), MonthDay.of(1, 5))
				&& !HalloweenSeason.inWindow(MonthDay.of(6, 1), MonthDay.of(12, 20), MonthDay.of(1, 5)), "A window may wrap past New Year");
		try {
			// 02:00 UTC on 4 November is still 3 November in New York.
			Clock lateNight = Clock.fixed(Instant.parse("2026-11-04T02:00:00Z"), ZoneOffset.UTC);
			HalloweenSeason.setMode(HalloweenSeason.Mode.AUTO);
			HalloweenSeason.setWindow(start, end, ZoneOffset.UTC, lateNight);
			helper.assertFalse(HalloweenSeason.active(), "In UTC the event is over");
			HalloweenSeason.setWindow(start, end, ZoneId.of("America/New_York"), lateNight);
			helper.assertTrue(HalloweenSeason.active(), "In New York it is still on");
			HalloweenSeason.setWindow(start, end, ZoneOffset.UTC, Clock.fixed(Instant.parse("2026-10-31T20:00:00Z"), ZoneOffset.UTC));
			helper.assertTrue(HalloweenSeason.active(), "Halloween itself is in the window");
			HalloweenSeason.setMode(HalloweenSeason.Mode.OFF);
			helper.assertFalse(HalloweenSeason.active(), "off overrides the dates");
			HalloweenSeason.setWindow(start, end, ZoneOffset.UTC, Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC));
			HalloweenSeason.setMode(HalloweenSeason.Mode.ON);
			helper.assertTrue(HalloweenSeason.active(), "on overrides them too (testing and off-season worlds)");
		} finally {
			HalloweenSeason.reset();
		}
		helper.succeed();
	}

	// ---------------------------------------------------------------- trick-or-treating

	/** A house: an oak door on stone, a jack o'lantern by it if lit, and a villager whose bed is inside. */
	private static Villager house(GameTestHelper helper, boolean porchLight) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
		BlockState lower = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
		helper.setBlock(DOOR, lower);
		helper.setBlock(DOOR.above(), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
		if (porchLight) {
			helper.setBlock(DOOR.east(2), Blocks.JACK_O_LANTERN);
		}
		return resident(helper, BED);
	}

	private static Villager resident(GameTestHelper helper, BlockPos bed) {
		Villager villager = helper.spawnWithNoFreeWill(VILLAGER, DOOR.north(2));
		villager.getBrain().setMemory(MemoryModuleType.HOME, GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(bed)));
		return villager;
	}

	private static int treats(ServerPlayer player) {
		int count = 0;
		for (int slot = 0; slot < 36; slot++) {
			count += player.getInventory().getItem(slot).getCount();
		}
		return count;
	}

	/** In season, at dusk, in costume, under a porch light: one treat from each home a night, a trick for knocking again. */
	@GameTest
	public void trickOrTreatGivesOneTreatPerHomeANight(GameTestHelper helper) {
		house(helper, true);
		ServerPlayer player = serverPlayer(helper, DOOR.south(), ItemStack.EMPTY);
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
		BlockPos door = helper.absolutePos(DOOR);
		try {
			HalloweenSeason.setMode(HalloweenSeason.Mode.ON);
			Result first = TrickOrTreat.answer(player, door, NIGHT);
			int treats = treats(player);
			helper.assertTrue(first == Result.TREAT && treats >= 1, "The first knock tonight gets a treat: " + first);
			Result again = TrickOrTreat.answer(player, door, NIGHT + 1000);
			helper.assertTrue(again == Result.TRICK && treats(player) == treats, "Knocking again tonight gets a trick, not a treat: " + again);
			Result tomorrow = TrickOrTreat.answer(player, door, NIGHT + TrickOrTreat.DAY);
			helper.assertTrue(tomorrow == Result.TREAT && treats(player) > treats, "Tomorrow night the home gives again: " + tomorrow);
		} finally {
			HalloweenSeason.reset();
		}
		helper.succeed();
	}

	/** No treats out of season, outside dusk to midnight, without a costume, without a porch light, or with nobody home. */
	@GameTest
	public void trickOrTreatNeedsTheSeasonTheHourACostumeAndALight(GameTestHelper helper) {
		Villager villager = house(helper, false);
		ServerPlayer player = serverPlayer(helper, DOOR.south(), ItemStack.EMPTY);
		BlockPos door = helper.absolutePos(DOOR);
		try {
			HalloweenSeason.setMode(HalloweenSeason.Mode.OFF);
			helper.assertTrue(TrickOrTreat.answer(player, door, NIGHT) == Result.OUT_OF_SEASON, "Nothing out of season");
			HalloweenSeason.setMode(HalloweenSeason.Mode.ON);
			helper.assertTrue(TrickOrTreat.answer(player, door, NIGHT - TrickOrTreat.DUSK + 6000) == Result.WRONG_HOUR, "Nothing at noon");
			helper.assertTrue(TrickOrTreat.answer(player, door, NIGHT + 7000) == Result.WRONG_HOUR, "Nothing after midnight");
			helper.assertTrue(TrickOrTreat.answer(player, door, NIGHT) == Result.NO_COSTUME, "Nothing without a costume");
			player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(item("witch_hat")));
			helper.assertTrue(TrickOrTreat.answer(player, door, NIGHT) == Result.NO_PORCH_LIGHT, "Nothing without a porch light");
			helper.setBlock(DOOR.west(2), block("hand_carved_pumpkin").defaultBlockState().setValue(CarvedPumpkinBlock.LIT, true));
			villager.getBrain().setMemory(MemoryModuleType.HOME, GlobalPos.of(helper.getLevel().dimension(), door.north(TrickOrTreat.HOME_RADIUS + 4)));
			helper.assertTrue(TrickOrTreat.answer(player, door, NIGHT) == Result.NOBODY_HOME, "Nothing when no one's bed is near");
			villager.getBrain().setMemory(MemoryModuleType.HOME, GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(BED)));
			helper.assertTrue(TrickOrTreat.answer(player, door, NIGHT) == Result.TREAT, "A lit hand-carved pumpkin is a porch light too");
		} finally {
			HalloweenSeason.reset();
		}
		helper.succeed();
	}

	/**
	 * Tonight's record survives a save and load (a restart); when the event ends nothing is answered but every
	 * treat and costume stays; when it starts again the same night, that home still gives no second treat.
	 */
	@GameTest
	public void treatsSurviveARestartAndTheEventEnding(GameTestHelper helper) {
		house(helper, true);
		ServerPlayer player = serverPlayer(helper, DOOR.south(), ItemStack.EMPTY);
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(item("scarecrow_hat")));
		BlockPos door = helper.absolutePos(DOOR);
		try {
			HalloweenSeason.setMode(HalloweenSeason.Mode.ON);
			helper.assertTrue(TrickOrTreat.answer(player, door, NIGHT) == Result.TREAT, "A treat while the event runs");
			int earned = treats(player);
			Tag saved = TrickOrTreat.Data.CODEC.encodeStart(NbtOps.INSTANCE, TrickOrTreat.data(helper.getLevel())).getOrThrow();
			TrickOrTreat.Data loaded = TrickOrTreat.Data.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
			long tonight = TrickOrTreat.night(NIGHT);
			helper.assertTrue(loaded.visited(tonight, player.getUUID(), helper.absolutePos(BED)) && loaded.count(tonight, player.getUUID()) == 1,
					"The saved record keeps tonight's treat");
			helper.assertFalse(loaded.record(tonight, player.getUUID(), helper.absolutePos(BED)), "and refuses the same home again after loading");

			HalloweenSeason.setMode(HalloweenSeason.Mode.OFF);
			helper.assertTrue(TrickOrTreat.answer(player, door, NIGHT) == Result.OUT_OF_SEASON, "The event has ended");
			helper.assertTrue(treats(player) == earned && player.getItemBySlot(EquipmentSlot.HEAD).is(item("scarecrow_hat")),
					"Treats and costumes stay after the event");
			HalloweenSeason.setMode(HalloweenSeason.Mode.ON);
			helper.assertTrue(TrickOrTreat.answer(player, door, NIGHT) == Result.TRICK && treats(player) == earned,
					"Restarting the event the same night gives no second treat");
		} finally {
			HalloweenSeason.reset();
		}
		helper.succeed();
	}

	/** Ten homes in one night earn the Full Bag advancement; an eleventh knock at the same door is a trick. */
	@GameTest
	public void tenHomesInOneNightFillTheBag(GameTestHelper helper) {
		house(helper, true);
		for (int i = 1; i < TrickOrTreat.FULL_BAG; i++) {
			resident(helper, BED.east(i % 5).north(i / 5 * 2));
		}
		ServerPlayer player = serverPlayer(helper, DOOR.south(), ItemStack.EMPTY);
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(item("ghost_sheet")));
		BlockPos door = helper.absolutePos(DOOR);
		try {
			HalloweenSeason.setMode(HalloweenSeason.Mode.ON);
			for (int i = 0; i < TrickOrTreat.FULL_BAG; i++) {
				Result result = TrickOrTreat.answer(player, door, NIGHT);
				helper.assertTrue(result == Result.TREAT, "Home " + (i + 1) + " should give a treat: " + result);
			}
			helper.assertTrue(TrickOrTreat.answer(player, door, NIGHT) == Result.TRICK, "Every home behind this door has given");
		} finally {
			HalloweenSeason.reset();
		}
		AdvancementHolder fullBag = helper.getLevel().getServer().getAdvancements().get(Jugcraft.id("full_bag"));
		helper.assertTrue(fullBag != null && player.getAdvancements().getOrStartProgress(fullBag).isDone(), "Ten homes earn Full Bag");
		helper.succeed();
	}

	/**
	 * Using a Candy Bag on a door knocks instead of opening it (through the game's own block-use path); one knock
	 * waits per player, only wooden doors take one, and the answer comes a moment later.
	 */
	@GameTest(maxTicks = 100)
	public void aKnockIsAnsweredALittleLater(GameTestHelper helper) {
		house(helper, true);
		ItemStack bag = new ItemStack(item("candy_bag"));
		ServerPlayer player = serverPlayer(helper, DOOR.south(), bag);
		BlockPos upper = helper.absolutePos(DOOR.above());
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(upper).relative(Direction.SOUTH, 0.5), Direction.SOUTH, upper, false);
		InteractionResult used = player.gameMode.useItemOn(player, helper.getLevel(), bag, InteractionHand.MAIN_HAND, hit);
		helper.assertTrue(used.consumesAction(), "The bag knocks: " + used);
		helper.assertFalse(helper.getBlockState(DOOR).getValue(DoorBlock.OPEN), "Knocking does not open the door");
		helper.assertTrue(TrickOrTreat.knock(player, helper.absolutePos(DOOR)) == Result.BUSY, "One knock at a time");
		helper.assertTrue(TrickOrTreat.knock(player, helper.absolutePos(DOOR.east())) == Result.NOT_A_DOOR, "Only doors take a knock");
		helper.runAtTickTime(TrickOrTreat.ANSWER_TICKS + 10, () -> {
			helper.assertTrue(TrickOrTreat.knock(player, helper.absolutePos(DOOR)) == Result.KNOCKED, "After the answer the player may knock again");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- costumes and data

	/** The costume hats and hand-carved pumpkins go on the head; carved pumpkins disguise a gaze like vanilla's. */
	@GameTest
	public void costumesAndCarvedPumpkinsAreWornOnTheHead(GameTestHelper helper) {
		for (String id : JugcraftAgriculture.COSTUMES) {
			ItemStack hat = new ItemStack(item(id));
			Equippable worn = hat.get(DataComponents.EQUIPPABLE);
			helper.assertTrue(worn != null && worn.slot() == EquipmentSlot.HEAD, id + " is worn on the head");
			helper.assertTrue(hat.is(TrickOrTreat.COSTUMES) && hat.is(TrickOrTreat.COSTUME_HATS), id + " is a costume hat");
		}
		helper.assertTrue(new ItemStack(item("ghost_sheet")).get(DataComponents.EQUIPPABLE).cameraOverlay().isPresent(),
				"Looking out from under a ghost sheet");
		for (String id : List.of("hand_carved_pumpkin", "hand_carved_white_pumpkin", "hand_carved_jarrahdale_pumpkin", "hand_carved_cinderella_pumpkin")) {
			ItemStack pumpkin = new ItemStack(item(id));
			Equippable worn = pumpkin.get(DataComponents.EQUIPPABLE);
			helper.assertTrue(worn != null && worn.slot() == EquipmentSlot.HEAD && !worn.swappable() && worn.cameraOverlay().isPresent(),
					id + " is worn like a carved pumpkin");
			helper.assertTrue(pumpkin.is(ItemTags.GAZE_DISGUISE_EQUIPMENT) && pumpkin.is(TrickOrTreat.COSTUMES), id + " disguises and counts as a costume");
		}
		helper.assertTrue(new ItemStack(Items.CARVED_PUMPKIN).is(TrickOrTreat.COSTUMES), "A vanilla carved pumpkin counts as a costume");
		helper.succeed();
	}

	/** The recipes, the treat and hollowing tables and both advancements load; treats are only candy and sweets. */
	@GameTest
	public void regattaAndTrickOrTreatDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("candy_bag", "witch_hat", "ghost_sheet", "scarecrow_hat", "regatta_flag", "regatta_buoy")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), "Recipe " + id + " loads");
		}
		LootTable hollow = level.getServer().reloadableRegistries().getLootTable(PumpkinCarvings.HOLLOW);
		helper.assertTrue(hollow != LootTable.EMPTY, "The hollowing table loads");
		LootTable treats = level.getServer().reloadableRegistries().getLootTable(TrickOrTreat.TREATS);
		helper.assertTrue(treats != LootTable.EMPTY, "The treat table loads");
		if (TREATS.isEmpty()) {
			for (String id : List.of("candy_corn", "caramel", "popcorn_ball", "caramel_apple", "king_size_candy_bar")) {
				TREATS.add(item(id));
			}
			TREATS.add(Items.COOKIE);
		}
		ServerPlayer player = serverPlayer(helper, new BlockPos(1, 1, 1), ItemStack.EMPTY);
		LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
				.withParameter(LootContextParams.THIS_ENTITY, player).create(LootContextParamSets.GIFT);
		for (int roll = 0; roll < 50; roll++) {
			List<ItemStack> given = treats.getRandomItems(params);
			helper.assertTrue(!given.isEmpty() && given.stream().allMatch(stack -> TREATS.contains(stack.getItem())), "Treats only: " + given);
		}
		for (String id : List.of("full_bag", "pumpkin_regatta")) {
			helper.assertTrue(level.getServer().getAdvancements().get(Jugcraft.id(id)) != null, "Advancement " + id + " loads");
		}
		helper.succeed();
	}
}
