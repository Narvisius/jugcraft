package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CandyBagItem;
import io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CarvingTemplates;
import io.github.jimbozoomer.jugcraft.agriculture.CropGrowth;
import io.github.jimbozoomer.jugcraft.agriculture.FlamingPumpkin;
import io.github.jimbozoomer.jugcraft.agriculture.FlyingPumpkin;
import io.github.jimbozoomer.jugcraft.agriculture.HalloweenSeason;
import io.github.jimbozoomer.jugcraft.agriculture.HarvestMoon;
import io.github.jimbozoomer.jugcraft.agriculture.HarvestScaleBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.HeadlessHorseman;
import io.github.jimbozoomer.jugcraft.agriculture.HorsemanSummoning;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCarving;
import io.github.jimbozoomer.jugcraft.agriculture.ScarecrowBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.ThrowMarker;
import io.github.jimbozoomer.jugcraft.agriculture.TrebuchetBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TrebuchetBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.TrickOrTreat;
import io.github.jimbozoomer.jugcraft.agriculture.WillOWisp;
import io.github.jimbozoomer.jugcraft.agriculture.Wisps;
import java.time.Clock;
import java.time.Instant;
import java.time.MonthDay;
import java.time.ZoneOffset;
import java.util.List;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for Halloween nights: will-o'-wisps (when and where they come out, fleeing, fading, catching one in
 * a bottle), the Pumpkin Chunkin' Trebuchet (loading, aiming, throwing, the board and its ribbons, saving), the
 * Candy Bag (treats only, filled while trick-or-treating, tonight's count forgotten the next night), the Harvest
 * Moon (only on its night, doubling crop growth) and the Headless Horseman (every summoning rule, riding off with
 * nothing, his loot only for a player's kill, flaming pumpkins that burn but break nothing, rage, saving), and the
 * seasonal rules: activation, deactivation, a restart across the boundary, no duplicate rewards, earned things kept.
 *
 * <p>Times of day are passed in rather than set on the shared world clock, and every season or Harvest Moon change
 * is undone before the test method returns, so tests running alongside never see it. Every Horseman a test makes is
 * gone before it returns, so none hunts another test's players.
 */
public class NightGameTests {
	private static final long MIDNIGHT = HorsemanSummoning.MIDNIGHT;
	private static final long NOON = 6000;
	/** Trick-or-treating hour on a far-off night (the trick-or-treat tests' night, so their record is shared, not replaced). */
	private static final long TRICK_OR_TREAT_NIGHT = TrickOrTreat.DUSK + TrickOrTreat.DAY * 5000;
	private static final BlockPos DOOR = new BlockPos(3, 2, 3);
	private static final BlockPos BED = DOOR.north(3);

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	@SuppressWarnings("unchecked")
	private static <T extends Entity> EntityType<T> vanilla(String id) {
		return (EntityType<T>) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(id));
	}

	/** The event on a fixed server clock reading 20:00 UTC on {@code date} (yyyy-MM-dd). */
	private static void onDate(String date) {
		HalloweenSeason.setMode(HalloweenSeason.Mode.AUTO);
		HalloweenSeason.setWindow(HalloweenSeason.DEFAULT_START, HalloweenSeason.DEFAULT_END, ZoneOffset.UTC,
				Clock.fixed(Instant.parse(date + "T20:00:00Z"), ZoneOffset.UTC));
	}

	/** Back to the configured season, and the Harvest Moon worked out again for noon (down). */
	private static void restore(MinecraftServer server) {
		HalloweenSeason.reset();
		HarvestMoon.update(server, NOON);
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, ItemStack held) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	private static BlockHitResult hit(GameTestHelper helper, BlockPos pos, Direction side) {
		BlockPos absolute = helper.absolutePos(pos);
		return new BlockHitResult(Vec3.atCenterOf(absolute).relative(side, 0.5), side, absolute, false);
	}

	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos pos, Direction side) {
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(helper, pos, side));
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

	private static int dropped(GameTestHelper helper, Item item) {
		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 8, 8).inflate(2.0);
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item)).stream()
				.mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	private static <T extends Entity> List<T> around(GameTestHelper helper, Class<T> kind, BlockPos pos, double range) {
		return helper.getLevel().getEntitiesOfClass(kind, new AABB(helper.absolutePos(pos)).inflate(range));
	}

	private static boolean earned(ServerPlayer player, String id) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Jugcraft.id(id));
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	private static int bagged(ItemStack bag) {
		BundleContents contents = bag.get(DataComponents.BUNDLE_CONTENTS);
		return contents == null ? 0 : contents.itemCopies().mapToInt(ItemStack::getCount).sum();
	}

	private static boolean onlyTreats(ItemStack bag) {
		BundleContents contents = bag.get(DataComponents.BUNDLE_CONTENTS);
		return contents != null && contents.itemCopies().allMatch(stack -> stack.is(CandyBagItem.TREATS));
	}

	// ---------------------------------------------------------------- the Harvest Moon

	/**
	 * It rises only on the nights of its day (31 October by default, or the operator's day) while the event runs:
	 * not by day, not the night before, never with the event off. It keeps no saved state, so a restart works it out
	 * again from the clocks alone.
	 */
	@GameTest
	public void theHarvestMoonRisesOnlyOnHalloweenNight(GameTestHelper helper) {
		try {
			helper.assertTrue(HalloweenSeason.harvestMoon().equals(HalloweenSeason.DEFAULT_HARVEST_MOON), "By default it is Halloween's");
			onDate("2103-10-31");
			helper.assertTrue(HarvestMoon.rising(MIDNIGHT) && HarvestMoon.rising(HarvestMoon.DUSK) && HarvestMoon.rising(MIDNIGHT + TrickOrTreat.DAY * 7),
					"It rises at dusk on Halloween and is up at midnight, whatever the day count");
			helper.assertFalse(HarvestMoon.rising(NOON) || HarvestMoon.rising(HarvestMoon.DAWN), "Not by day, and it sets at dawn");
			onDate("2103-10-30");
			helper.assertFalse(HarvestMoon.rising(MIDNIGHT), "Not the night before");
			HalloweenSeason.setHarvestMoon(MonthDay.of(10, 30));
			helper.assertTrue(HarvestMoon.rising(MIDNIGHT), "The operator may move it to another night of the event");
			HalloweenSeason.setMode(HalloweenSeason.Mode.OFF);
			helper.assertFalse(HarvestMoon.rising(MIDNIGHT), "With the event off it never rises");
			onDate("2103-12-25");
			HalloweenSeason.setHarvestMoon(MonthDay.of(12, 25));
			helper.assertFalse(HarvestMoon.rising(MIDNIGHT), "nor on a day outside the event's window");
		} finally {
			restore(helper.getLevel().getServer());
		}
		helper.succeed();
	}

	/** Under the Harvest Moon a crop grows twice as fast; when it sets (dawn, or the event ending) growth is normal again. */
	@GameTest
	public void theHarvestMoonDoublesCropGrowth(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		BlockPos crop = new BlockPos(2, 2, 2);
		helper.setBlock(crop.below(), Blocks.FARMLAND);
		BlockPos at = helper.absolutePos(crop);
		try {
			onDate("2104-10-31");
			helper.assertFalse(HarvestMoon.update(server, NOON), "No moon at noon");
			float normal = CropGrowth.speed(level, at, false);
			helper.assertTrue(HarvestMoon.update(server, MIDNIGHT) && HarvestMoon.active(), "At midnight on Halloween the Harvest Moon is up");
			helper.assertTrue(CropGrowth.speed(level, at, false) == normal * HarvestMoon.GROWTH_BONUS,
					"and the crop grows " + HarvestMoon.GROWTH_BONUS + " times as fast: " + CropGrowth.speed(level, at, false) + " vs " + normal);
			helper.assertFalse(HarvestMoon.update(server, HarvestMoon.DAWN) || HarvestMoon.active(), "At dawn it sets");
			helper.assertTrue(CropGrowth.speed(level, at, false) == normal, "and growth is back to normal");
			HarvestMoon.update(server, MIDNIGHT);
			HalloweenSeason.setMode(HalloweenSeason.Mode.OFF);
			helper.assertFalse(HarvestMoon.update(server, MIDNIGHT), "Ending the event sets it at the next check");
		} finally {
			restore(server);
		}
		helper.succeed();
	}

	// ---------------------------------------------------------------- will-o'-wisps

	/**
	 * Wisps are about on event nights only; they appear under the open sky beside corn (not inside it, not over bare
	 * stone, not under a roof), and no more spawn where enough are already near.
	 */
	@GameTest(skyAccess = true)
	public void wispsGatherOverCornOnHalloweenNights(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos corn = new BlockPos(2, 2, 2);
		helper.setBlock(corn.below(), Blocks.FARMLAND);
		helper.setBlock(corn, JugcraftAgriculture.block("corn_crop"));
		try {
			HalloweenSeason.setMode(HalloweenSeason.Mode.ON);
			helper.assertTrue(Wisps.night(MIDNIGHT) && Wisps.night(HarvestMoon.DUSK), "Wisps are about on event nights");
			helper.assertFalse(Wisps.night(NOON) || Wisps.night(HarvestMoon.DAWN), "not by day");
			HalloweenSeason.setMode(HalloweenSeason.Mode.OFF);
			helper.assertFalse(Wisps.night(MIDNIGHT), "and not out of season");
		} finally {
			HalloweenSeason.reset();
		}
		helper.assertTrue(Wisps.canSpawnAt(level, helper.absolutePos(corn.above())), "Over corn, under the open sky, a wisp may appear");
		helper.assertTrue(Wisps.canSpawnAt(level, helper.absolutePos(corn.above().east(Wisps.CORN_RADIUS))), "and beside it");
		helper.assertFalse(Wisps.canSpawnAt(level, helper.absolutePos(corn)), "but not inside the plant");
		helper.assertFalse(Wisps.canSpawnAt(level, helper.absolutePos(new BlockPos(6, 2, 6))), "nor over bare stone away from corn");
		helper.setBlock(corn.above(4), Blocks.OAK_PLANKS);
		helper.assertFalse(Wisps.canSpawnAt(level, helper.absolutePos(corn.above())), "nor under a roof");

		List<WillOWisp> crowd = new java.util.ArrayList<>();
		for (int i = 0; i < Wisps.NEAR_CAP; i++) {
			crowd.add(helper.spawnWithNoFreeWill(JugcraftAgriculture.WILL_O_WISP, new BlockPos(1 + i, 4, 6)));
		}
		helper.assertFalse(Wisps.trySpawn(level, helper.absolutePos(corn), level.getRandom()), "With " + Wisps.NEAR_CAP + " near, no more come");
		crowd.forEach(Entity::discard);
		helper.succeed();
	}

	/**
	 * A wisp darts away from a player nearby, but a sneaking player can creep close; a glass bottle catches it (the
	 * bottle becomes a Wisp in a Jar, once: the wisp is gone) and earns Bottled Light; dawn or a blow puts it out,
	 * leaving nothing.
	 */
	@GameTest
	public void aWispFleesFadesAndIsCaughtInABottle(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		WillOWisp wisp = helper.spawnWithNoFreeWill(JugcraftAgriculture.WILL_O_WISP, new BlockPos(4, 3, 4));
		ServerPlayer player = player(helper, new BlockPos(1, 2, 4), ItemStack.EMPTY);
		helper.assertTrue(wisp.step(level, true), "A player three blocks off scares it");
		helper.assertTrue(wisp.getDeltaMovement().x > 0.0, "and it darts away from them: " + wisp.getDeltaMovement());
		player.setShiftKeyDown(true);
		helper.assertFalse(wisp.step(level, true), "A sneaking player three blocks off does not");
		player.setPos(wisp.getX() - 2.0, wisp.getY(), wisp.getZ());
		helper.assertTrue(wisp.step(level, true), "but closer than " + WillOWisp.SNEAK_FLEE_RADIUS + " blocks it flees too");
		player.setShiftKeyDown(false);

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 2));
		InteractionResult caught = player.interactOn(wisp, InteractionHand.MAIN_HAND, wisp.position());
		helper.assertTrue(caught.consumesAction() && wisp.isRemoved(), "A glass bottle catches it: " + caught);
		helper.assertTrue(player.getMainHandItem().is(Items.GLASS_BOTTLE) && player.getMainHandItem().getCount() == 1
				&& count(player, item("wisp_in_a_jar")) == 1, "One bottle became a Wisp in a Jar");
		helper.assertTrue(earned(player, "wisp_in_a_jar"), "Catching one earns Bottled Light");

		WillOWisp atDawn = helper.spawnWithNoFreeWill(JugcraftAgriculture.WILL_O_WISP, new BlockPos(2, 3, 2));
		atDawn.step(level, false);
		helper.assertTrue(atDawn.isRemoved(), "Out of the night a wisp fades");
		WillOWisp struck = helper.spawnWithNoFreeWill(JugcraftAgriculture.WILL_O_WISP, new BlockPos(6, 3, 6));
		struck.hurtServer(level, level.damageSources().playerAttack(player), 1.0F);
		helper.assertTrue(struck.isRemoved(), "A blow puts it out");
		helper.assertTrue(dropped(helper, item("wisp_in_a_jar")) == 0 && level.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class,
				new AABB(helper.absolutePos(new BlockPos(6, 3, 6))).inflate(3.0)).isEmpty(), "and leaves nothing");
		helper.succeed();
	}

	// ---------------------------------------------------------------- the trebuchet

	private static TrebuchetBlock.Arm arm(GameTestHelper helper, BlockPos pos) {
		return helper.getBlockState(pos).getValue(TrebuchetBlock.ARM);
	}

	/**
	 * Only pumpkins load it, one at a time; sneaking with an empty hand steps the angle (wrapping round); an empty hand
	 * lets fly (the pumpkin leaves the sling, flying the way the trebuchet faces) and the arm swings back after a while,
	 * taking no new pumpkin meanwhile. It works in any season.
	 */
	@GameTest(maxTicks = 80)
	public void aTrebuchetLoadsAimsAndLetsFly(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, block("trebuchet").defaultBlockState().setValue(TrebuchetBlock.FACING, Direction.NORTH));
		TrebuchetBlockEntity trebuchet = helper.getBlockEntity(pos, TrebuchetBlockEntity.class);
		ServerPlayer player = player(helper, pos.south(), new ItemStack(Items.DIRT, 2));
		use(helper, player, pos, Direction.SOUTH);
		helper.assertTrue(arm(helper, pos) == TrebuchetBlock.Arm.READY && trebuchet.loaded().isEmpty() && player.getMainHandItem().getCount() == 2,
				"Dirt is no ammunition");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PUMPKIN, 2));
		helper.assertTrue(use(helper, player, pos, Direction.SOUTH).consumesAction(), "A pumpkin goes in the sling");
		helper.assertTrue(arm(helper, pos) == TrebuchetBlock.Arm.LOADED && trebuchet.loaded().is(Items.PUMPKIN)
				&& player.getMainHandItem().getCount() == 1, "One pumpkin is loaded");
		use(helper, player, pos, Direction.SOUTH);
		helper.assertTrue(player.getMainHandItem().getCount() == 1 && trebuchet.loaded().getCount() == 1, "A loaded sling takes no second pumpkin");

		ItemStack spare = player.getMainHandItem().copy();
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.setShiftKeyDown(true);
		helper.assertTrue(trebuchet.angle() == TrebuchetBlockEntity.DEFAULT_ANGLE, "It starts at " + TrebuchetBlockEntity.DEFAULT_ANGLE + "°");
		for (int step = 0; step < 4; step++) {
			use(helper, player, pos, Direction.SOUTH);
		}
		helper.assertTrue(trebuchet.angle() == TrebuchetBlockEntity.MIN_ANGLE, "Stepping past " + TrebuchetBlockEntity.MAX_ANGLE
				+ "° wraps round to " + TrebuchetBlockEntity.MIN_ANGLE + "°: " + trebuchet.angle());
		helper.assertTrue(arm(helper, pos) == TrebuchetBlock.Arm.LOADED, "Aiming does not throw");
		player.setShiftKeyDown(false);

		use(helper, player, pos, Direction.SOUTH);
		List<FlyingPumpkin> flying = around(helper, FlyingPumpkin.class, pos, 4.0);
		helper.assertTrue(arm(helper, pos) == TrebuchetBlock.Arm.RELEASED && trebuchet.loaded().isEmpty(), "An empty hand lets fly");
		helper.assertTrue(flying.size() == 1 && flying.get(0).getDeltaMovement().z < 0.0 && flying.get(0).getDeltaMovement().y > 0.0
				&& flying.get(0).getOwner() == player, "One pumpkin flies up and north, thrown by the player");
		flying.forEach(Entity::discard);
		player.setItemInHand(InteractionHand.MAIN_HAND, spare);
		use(helper, player, pos, Direction.SOUTH);
		helper.assertTrue(trebuchet.loaded().isEmpty() && player.getMainHandItem().getCount() == 1, "While the arm swings back nothing loads");
		helper.succeedWhen(() -> helper.assertTrue(arm(helper, pos) == TrebuchetBlock.Arm.READY, "The arm swings back"));
	}

	/**
	 * A throw's speed follows the pumpkin (light carved ones fly farther, heavy ones less) and a gust of at most
	 * {@value TrebuchetBlockEntity#GUST} either way; its direction follows the facing and the angle.
	 */
	@GameTest
	public void throwsFollowTheAngleAndThePumpkin(GameTestHelper helper) {
		ItemStack pumpkin = new ItemStack(Items.PUMPKIN);
		Vec3 north = TrebuchetBlockEntity.velocity(Direction.NORTH, 45, pumpkin, 0.0);
		helper.assertTrue(Math.abs(north.length() - TrebuchetBlockEntity.BASE_SPEED) < 1.0E-6 && Math.abs(north.x) < 1.0E-9 && north.z < 0.0
				&& Math.abs(north.y + north.z) < 1.0E-9, "At 45° a pumpkin leaves north at the base speed: " + north);
		Vec3 east = TrebuchetBlockEntity.velocity(Direction.EAST, 60, pumpkin, 0.0);
		helper.assertTrue(east.x > 0.0 && Math.abs(east.z) < 1.0E-9 && Math.abs(east.y / east.x - Math.tan(Math.toRadians(60))) < 1.0E-6,
				"Facing east at 60° it leaves east, steeply: " + east);
		double plain = north.length();
		double carved = TrebuchetBlockEntity.velocity(Direction.NORTH, 45, new ItemStack(item("hand_carved_white_pumpkin")), 0.0).length();
		double heavy = TrebuchetBlockEntity.velocity(Direction.NORTH, 45, new ItemStack(item("cinderella_pumpkin")), 0.0).length();
		helper.assertTrue(carved > plain && heavy < plain, "Hollow carved pumpkins fly faster, squat heavy ones slower");
		double gusty = TrebuchetBlockEntity.velocity(Direction.NORTH, 45, pumpkin, 5.0).length();
		helper.assertTrue(Math.abs(gusty - plain * (1.0 + TrebuchetBlockEntity.GUST)) < 1.0E-9, "A gust adds at most " + TrebuchetBlockEntity.GUST);
		for (String id : TrebuchetBlockEntity.FACTORS.keySet()) {
			ItemStack ammo = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)));
			helper.assertTrue(ammo.is(TrebuchetBlockEntity.AMMO), id + " is trebuchet ammunition");
		}
		helper.assertFalse(new ItemStack(Items.MELON).is(TrebuchetBlockEntity.AMMO), "A melon is not");
		helper.succeed();
	}

	/**
	 * Where a pumpkin lands, the distance across the ground is measured on the server and marked; the board keeps each
	 * thrower's best of the three longest; each thrower's first place earns that place's ribbon, once per trebuchet,
	 * even after a save and load; 50 blocks earns Pumpkin Chunkin'.
	 */
	@GameTest
	public void theBoardKeepsTheLongestThrowsAndRibbonsComeOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, block("trebuchet").defaultBlockState().setValue(TrebuchetBlock.FACING, Direction.NORTH));
		TrebuchetBlockEntity board = helper.getBlockEntity(pos, TrebuchetBlockEntity.class);
		ServerPlayer first = player(helper, new BlockPos(1, 2, 6), ItemStack.EMPTY);
		ServerPlayer second = player(helper, new BlockPos(3, 2, 6), ItemStack.EMPTY);
		ServerPlayer third = player(helper, new BlockPos(5, 2, 6), ItemStack.EMPTY);
		ServerPlayer late = player(helper, new BlockPos(6, 2, 6), ItemStack.EMPTY);
		Item gold = item(HarvestScaleBlockEntity.RIBBONS.get(0));
		Item silver = item(HarvestScaleBlockEntity.RIBBONS.get(1));
		Item bronze = item(HarvestScaleBlockEntity.RIBBONS.get(2));

		Vec3 landing = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(5, 2, 1)));
		FlyingPumpkin pumpkin = new FlyingPumpkin(level, landing.add(0.0, 1.75, 52.0), new ItemStack(Items.PUMPKIN), helper.absolutePos(pos));
		pumpkin.setOwner(first);
		helper.assertTrue(Math.abs(pumpkin.distanceTo(landing) - 52.0) < 1.0E-9, "The distance runs across the ground");
		pumpkin.land(landing);
		helper.assertTrue(pumpkin.isRemoved() && board.board().size() == 1 && board.board().get(0).tenths() == 520, "52 blocks on the board");
		helper.assertTrue(count(first, gold) == 1 && earned(first, "pumpkin_chunkin"), "First place's ribbon, and Pumpkin Chunkin'");
		List<ThrowMarker> markers = level.getEntitiesOfClass(ThrowMarker.class, new AABB(landing, landing).inflate(1.0));
		helper.assertTrue(markers.size() == 1 && markers.get(0).hasCustomName(), "A named marker shows where it came down");
		markers.forEach(Entity::discard);

		helper.assertTrue(board.record(first, 30.0) == 0 && board.board().get(0).tenths() == 520, "A shorter throw keeps the best");
		helper.assertTrue(board.record(second, 40.0) == 1 && count(second, silver) == 1, "Second place, silver");
		helper.assertTrue(board.record(third, 20.0) == 2 && count(third, bronze) == 1, "Third place, bronze");
		helper.assertTrue(board.record(late, 10.0) == -1 && count(late, bronze) == 0, "Off the board, no ribbon");
		helper.assertFalse(earned(second, "pumpkin_chunkin"), "40 blocks is not enough for the advancement");
		helper.assertTrue(board.record(late, 60.0) == 0 && count(late, gold) == 1, "A new best takes first place and its ribbon");
		helper.assertTrue(board.board().size() == TrebuchetBlockEntity.BOARD && board.board().stream().noneMatch(e -> e.thrower().equals(third.getUUID())),
				"The board keeps the three longest");
		helper.assertTrue(board.record(first, 70.0) == 0 && count(first, gold) == 1, "Taking first again gives no second ribbon");

		var saved = board.saveWithoutMetadata(level.registryAccess());
		TrebuchetBlockEntity loaded = new TrebuchetBlockEntity(helper.absolutePos(pos), helper.getBlockState(pos));
		loaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
		helper.assertTrue(loaded.board().equals(board.board()) && loaded.angle() == board.angle(), "The board survives a save and load");
		helper.assertTrue(loaded.record(second, 80.0) == 0 && count(second, gold) == 0 && count(second, silver) == 1,
				"and still remembers who has a ribbon");
		helper.succeed();
	}

	/** Breaking a loaded trebuchet drops it and the pumpkin in its sling. */
	@GameTest
	public void breakingALoadedTrebuchetDropsItsPumpkin(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, block("trebuchet").defaultBlockState().setValue(TrebuchetBlock.FACING, Direction.EAST));
		ServerPlayer player = player(helper, pos.south(), new ItemStack(Items.CARVED_PUMPKIN));
		use(helper, player, pos, Direction.SOUTH);
		helper.assertTrue(player.getMainHandItem().isEmpty(), "Loaded");
		helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
		helper.succeedWhen(() -> helper.assertTrue(dropped(helper, Items.CARVED_PUMPKIN) == 1 && dropped(helper, item("trebuchet")) == 1,
				"The trebuchet and its pumpkin drop"));
	}

	// ---------------------------------------------------------------- the Candy Bag

	/** Only treats go in the bag, by hand (clicking in an inventory) or by filling; anything else stays out. */
	@GameTest
	public void theCandyBagHoldsOnlyTreats(GameTestHelper helper) {
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1), ItemStack.EMPTY);
		ItemStack bag = new ItemStack(item("candy_bag"));
		ItemStack corn = new ItemStack(item("candy_corn"), 5);
		CandyBagItem.fill(bag, corn);
		helper.assertTrue(corn.isEmpty() && bagged(bag) == 5, "Five candy corn go in: " + bagged(bag));
		ItemStack dirt = new ItemStack(Items.DIRT, 3);
		CandyBagItem.fill(bag, dirt);
		helper.assertTrue(dirt.getCount() == 3 && bagged(bag) == 5, "Dirt does not");

		SimpleContainer container = new SimpleContainer(2);
		container.setItem(0, new ItemStack(Items.DIRT, 3));
		container.setItem(1, new ItemStack(Items.COOKIE, 2));
		helper.assertFalse(bag.getItem().overrideStackedOnOther(bag, new Slot(container, 0, 0, 0), ClickAction.PRIMARY, player),
				"Clicking the bag on dirt does not take it");
		helper.assertTrue(container.getItem(0).getCount() == 3 && bagged(bag) == 5, "The dirt stays put");
		helper.assertTrue(bag.getItem().overrideStackedOnOther(bag, new Slot(container, 1, 0, 0), ClickAction.PRIMARY, player)
				&& container.getItem(1).isEmpty() && bagged(bag) == 7 && onlyTreats(bag), "Clicking it on cookies bags them");
		helper.succeed();
	}

	/**
	 * Trick-or-treating with the bag in hand puts the treats in it and writes tonight's count of homes on it (kept
	 * through a save and load); the next night the count is forgotten, the treats are not.
	 */
	@GameTest
	public void theCandyBagCountsTonightsHomes(GameTestHelper helper) {
		floor(helper);
		BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
		helper.setBlock(DOOR, door);
		helper.setBlock(DOOR.above(), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
		helper.setBlock(DOOR.east(2), Blocks.JACK_O_LANTERN);
		Villager villager = helper.spawnWithNoFreeWill(vanilla("villager"), DOOR.north(2));
		villager.getBrain().setMemory(MemoryModuleType.HOME, GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(BED)));
		ItemStack bag = new ItemStack(item("candy_bag"));
		ServerPlayer player = player(helper, DOOR.south(), bag);
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(item("witch_hat")));
		long tonight = TrickOrTreat.night(TRICK_OR_TREAT_NIGHT);
		try {
			HalloweenSeason.setMode(HalloweenSeason.Mode.ON);
			helper.assertTrue(TrickOrTreat.answer(player, helper.absolutePos(DOOR), TRICK_OR_TREAT_NIGHT) == TrickOrTreat.Result.TREAT, "A treat");
		} finally {
			HalloweenSeason.reset();
		}
		ItemStack held = player.getMainHandItem();
		helper.assertTrue(held.is(item("candy_bag")) && bagged(held) >= 1 && onlyTreats(held), "The treat went into the bag");
		CandyBagItem.Night night = held.get(JugcraftAgriculture.CANDY_BAG_NIGHT);
		helper.assertTrue(night != null && night.night() == tonight && night.homes() == 1, "One home tonight is written on it: " + night);

		var ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
		Tag saved = ItemStack.CODEC.encodeStart(ops, held).getOrThrow();
		ItemStack reloaded = ItemStack.CODEC.parse(ops, saved).getOrThrow();
		helper.assertTrue(night.equals(reloaded.get(JugcraftAgriculture.CANDY_BAG_NIGHT)) && bagged(reloaded) == bagged(held),
				"The count and the treats survive a save and load");
		helper.assertFalse(CandyBagItem.forget(held, tonight), "Tonight the count stays");
		int treats = bagged(held);
		helper.assertTrue(CandyBagItem.forget(held, tonight + 1) && !held.has(JugcraftAgriculture.CANDY_BAG_NIGHT) && bagged(held) == treats,
				"The next night the count is forgotten, the treats are kept");
		helper.succeed();
	}

	// ---------------------------------------------------------------- the Headless Horseman

	/** A scarecrow at {@code lower} (and above it) wearing {@code head}. */
	private static void scarecrow(GameTestHelper helper, BlockPos lower, ItemStack head) {
		ServerPlayer builder = player(helper, new BlockPos(0, 2, 0), new ItemStack(item("scarecrow")));
		builder.getMainHandItem().useOn(new UseOnContext(builder, InteractionHand.MAIN_HAND, hit(helper, lower.below(), Direction.UP)));
		wear(helper, lower, head);
	}

	/** Puts {@code head} on the scarecrow at {@code lower} (an empty stack takes it off). */
	private static void wear(GameTestHelper helper, BlockPos lower, ItemStack head) {
		helper.getBlockEntity(lower.above(), ScarecrowBlockEntity.class).setHead(head);
	}

	private static ItemStack worn(GameTestHelper helper, BlockPos lower) {
		return helper.getBlockEntity(lower.above(), ScarecrowBlockEntity.class).head();
	}

	/** A hand-carved pumpkin with the Classic face, with or without a torch inside. */
	private static ItemStack litCarving(boolean lit) {
		ItemStack head = new ItemStack(item("hand_carved_pumpkin"));
		head.set(JugcraftAgriculture.CARVING, PumpkinCarving.BLANK.withFace(0, CarvingTemplates.ALL.get(0).face()));
		head.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(CarvedPumpkinBlock.LIT, lit));
		return head;
	}

	/**
	 * Every rule of the summoning, in order: the event, the hour (within the window of midnight), not in peaceful, a
	 * lit head (a jack o'lantern or lit hand-carved pumpkin, not a dark one), open sky; then he comes, taking the
	 * head, 12 to 16 blocks off, hunting the summoner, his arena the scarecrow; and only one rides at a time.
	 */
	@GameTest(skyAccess = true)
	public void theHorsemanComesAtMidnightForALitHead(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		floor(helper);
		BlockPos lower = new BlockPos(3, 2, 3);
		scarecrow(helper, lower, new ItemStack(Items.JACK_O_LANTERN));
		BlockPos scarecrow = helper.absolutePos(lower);
		ServerPlayer caller = player(helper, lower.south(2), ItemStack.EMPTY);
		Difficulty difficulty = level.getDifficulty();
		try {
			HalloweenSeason.setMode(HalloweenSeason.Mode.OFF);
			helper.assertTrue(HorsemanSummoning.summon(caller, scarecrow, MIDNIGHT) == HorsemanSummoning.Result.OUT_OF_SEASON, "Not out of season");
			caller.setShiftKeyDown(true);
			helper.assertTrue(use(helper, caller, lower.above(), Direction.SOUTH).consumesAction(), "Sneak-using the scarecrow tries");
			helper.assertTrue(worn(helper, lower).is(Items.JACK_O_LANTERN), "and out of season nothing happens to it");
			caller.setShiftKeyDown(false);

			HalloweenSeason.setMode(HalloweenSeason.Mode.ON);
			helper.assertTrue(HorsemanSummoning.summon(caller, scarecrow, NOON) == HorsemanSummoning.Result.WRONG_HOUR, "Not at noon");
			helper.assertTrue(HorsemanSummoning.summon(caller, scarecrow, MIDNIGHT + HorsemanSummoning.HOUR_WINDOW + 1) == HorsemanSummoning.Result.WRONG_HOUR,
					"Not well after midnight");
			server.setDifficulty(Difficulty.PEACEFUL, true);
			helper.assertTrue(HorsemanSummoning.summon(caller, scarecrow, MIDNIGHT) == HorsemanSummoning.Result.PEACEFUL, "Not in peaceful");
			server.setDifficulty(difficulty, true);
			wear(helper, lower, new ItemStack(Items.CARVED_PUMPKIN));
			helper.assertTrue(HorsemanSummoning.summon(caller, scarecrow, MIDNIGHT) == HorsemanSummoning.Result.NO_HEAD, "Not for a dark carved pumpkin");
			wear(helper, lower, litCarving(false));
			helper.assertTrue(HorsemanSummoning.summon(caller, scarecrow, MIDNIGHT) == HorsemanSummoning.Result.NO_HEAD, "nor an unlit carving");
			wear(helper, lower, litCarving(true));
			helper.setBlock(lower.above(5), Blocks.OAK_PLANKS);
			helper.assertTrue(HorsemanSummoning.summon(caller, scarecrow, MIDNIGHT) == HorsemanSummoning.Result.ROOFED, "Not under a roof");
			helper.setBlock(lower.above(5), Blocks.AIR);

			HorsemanSummoning.Result result = HorsemanSummoning.summon(caller, scarecrow, MIDNIGHT - HorsemanSummoning.HOUR_WINDOW);
			helper.assertTrue(result == HorsemanSummoning.Result.SUMMONED, "Within the hour of midnight, with a lit head, he comes: " + result);
			helper.assertTrue(worn(helper, lower).isEmpty(), "He takes the head");
			List<HeadlessHorseman> riders = level.getEntitiesOfClass(HeadlessHorseman.class, new AABB(scarecrow).inflate(HorsemanSummoning.MAX_DISTANCE + 2));
			helper.assertTrue(riders.size() == 1, "One Horseman rides in: " + riders.size());
			HeadlessHorseman horseman = riders.get(0);
			double away = Math.hypot(horseman.getX() - scarecrow.getX() - 0.5, horseman.getZ() - scarecrow.getZ() - 0.5);
			helper.assertTrue(away <= HorsemanSummoning.MAX_DISTANCE + 1.0, "within " + HorsemanSummoning.MAX_DISTANCE + " blocks: " + away);
			helper.assertTrue(horseman.home().equals(scarecrow), "His arena is the scarecrow: " + horseman.home() + " vs " + scarecrow);
			helper.assertTrue(horseman.quarry().map(caller.getUUID()::equals).orElse(false), "He hunts the summoner: " + horseman.quarry());
			helper.assertTrue(horseman.isPersistenceRequired(), "and he stays");
			wear(helper, lower, new ItemStack(Items.JACK_O_LANTERN));
			helper.assertTrue(HorsemanSummoning.summon(caller, scarecrow, MIDNIGHT) == HorsemanSummoning.Result.ALREADY_RIDING, "Only one rides at a time");
			helper.assertTrue(worn(helper, lower).is(Items.JACK_O_LANTERN), "and the second head is kept");
		} finally {
			server.setDifficulty(difficulty, true);
			HalloweenSeason.reset();
			level.getEntitiesOfClass(HeadlessHorseman.class, new AABB(scarecrow).inflate(HorsemanSummoning.ONE_AT_A_TIME), h -> true)
					.forEach(Entity::discard);
		}
		helper.succeed();
	}

	/** He rides off at dawn, when the event ends, or after a long while alone, and leaves nothing behind. */
	@GameTest
	public void theHorsemanRidesOffWithNothing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		HeadlessHorseman horseman = helper.spawnWithNoFreeWill(JugcraftAgriculture.HEADLESS_HORSEMAN, new BlockPos(3, 2, 3));
		try {
			HalloweenSeason.setMode(HalloweenSeason.Mode.ON);
			helper.assertFalse(horseman.shouldLeave(MIDNIGHT), "At midnight in the event he stays");
			helper.assertTrue(horseman.shouldLeave(HarvestMoon.DAWN), "At dawn he leaves");
			horseman.setLonely(HeadlessHorseman.LONELY_TICKS);
			helper.assertTrue(horseman.shouldLeave(MIDNIGHT), "after " + HeadlessHorseman.LONELY_TICKS + " ticks with nobody near too");
			horseman.setLonely(0);
			HalloweenSeason.setMode(HalloweenSeason.Mode.OFF);
			helper.assertTrue(horseman.shouldLeave(MIDNIGHT), "and when the event ends");
		} finally {
			HalloweenSeason.reset();
		}
		horseman.rideOff(level);
		helper.assertTrue(horseman.isRemoved(), "He is gone");
		helper.assertTrue(dropped(helper, item("horseman_lantern")) == 0 && dropped(helper, item("horseman_cloak")) == 0, "leaving nothing");
		helper.succeed();
	}

	/** Killed by a player he drops his lantern and cloak (once) and earns Lost His Head; killed otherwise, nothing. */
	@GameTest
	public void defeatingTheHorsemanEarnsHisLantern(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer hunter = player(helper, new BlockPos(6, 2, 6), ItemStack.EMPTY);
		HeadlessHorseman unlucky = helper.spawnWithNoFreeWill(JugcraftAgriculture.HEADLESS_HORSEMAN, new BlockPos(2, 2, 2));
		unlucky.hurtServer(level, level.damageSources().generic(), 10000.0F);
		helper.assertTrue(unlucky.isDeadOrDying(), "The first Horseman falls to no one");
		helper.assertTrue(dropped(helper, item("horseman_lantern")) == 0, "and drops no lantern");
		unlucky.discard();

		HeadlessHorseman horseman = helper.spawnWithNoFreeWill(JugcraftAgriculture.HEADLESS_HORSEMAN, new BlockPos(3, 2, 3));
		horseman.hurtServer(level, level.damageSources().playerAttack(hunter), 10000.0F);
		helper.assertTrue(horseman.isDeadOrDying(), "The second falls to the hunter");
		helper.assertTrue(dropped(helper, item("horseman_lantern")) == 1 && dropped(helper, item("horseman_cloak")) == 1,
				"dropping his lantern and cloak, one each");
		helper.assertTrue(earned(hunter, "headless_horseman"), "Lost His Head");
		horseman.discard();
		LootTable table = level.getServer().reloadableRegistries().getLootTable(
				ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("entities/headless_horseman")));
		helper.assertTrue(table != LootTable.EMPTY, "His loot table loads");
		helper.succeed();
	}

	/** A flaming pumpkin hurts and sets alight everything living where it bursts but the Horseman, and breaks no block. */
	@GameTest
	public void flamingPumpkinsBurnButBreakNothing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		helper.setBlock(new BlockPos(4, 2, 2), Blocks.OAK_PLANKS);
		helper.setBlock(new BlockPos(2, 2, 4), Blocks.HAY_BLOCK);
		Mob cow = helper.spawnWithNoFreeWill(vanilla("cow"), new BlockPos(3, 2, 3));
		HeadlessHorseman horseman = helper.spawnWithNoFreeWill(JugcraftAgriculture.HEADLESS_HORSEMAN, new BlockPos(4, 2, 4));
		float health = cow.getHealth();
		FlamingPumpkin pumpkin = new FlamingPumpkin(level, horseman);
		pumpkin.burst(level, cow.position());
		helper.assertTrue(cow.getHealth() == health - FlamingPumpkin.DAMAGE && cow.isOnFire(), "The cow is hurt and alight: " + cow.getHealth());
		helper.assertTrue(horseman.getHealth() == horseman.getMaxHealth() && !horseman.isOnFire(), "The Horseman is not");
		helper.assertTrue(helper.getBlockState(new BlockPos(4, 2, 2)).is(Blocks.OAK_PLANKS) && helper.getBlockState(new BlockPos(2, 2, 4)).is(Blocks.HAY_BLOCK),
				"No block breaks");
		for (BlockPos pos : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 1, 0)), helper.absolutePos(new BlockPos(7, 4, 7)))) {
			helper.assertFalse(level.getBlockState(pos).is(Blocks.FIRE), "and nothing is set alight: fire at " + pos);
		}
		pumpkin.discard();
		horseman.discard();
		helper.succeed();
	}

	/** At half health he is enraged, once: faster, and throwing three pumpkins at a time instead of one. */
	@GameTest
	public void theHorsemanEnragesAtHalfHealth(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer target = player(helper, new BlockPos(0, 2, 0), ItemStack.EMPTY);
		HeadlessHorseman horseman = helper.spawnWithNoFreeWill(JugcraftAgriculture.HEADLESS_HORSEMAN, new BlockPos(5, 2, 5));
		try {
			helper.assertFalse(horseman.updateRage(level), "At full health he is calm");
			horseman.performRangedAttack(target, 1.0F);
			List<FlamingPumpkin> thrown = around(helper, FlamingPumpkin.class, new BlockPos(5, 2, 5), 4.0);
			helper.assertTrue(thrown.size() == 1, "Calm, he throws one: " + thrown.size());
			thrown.forEach(Entity::discard);

			horseman.setHealth(horseman.getMaxHealth() / 2.0F);
			helper.assertTrue(horseman.updateRage(level) && horseman.enraged(), "At half health he is enraged");
			helper.assertTrue(horseman.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(Jugcraft.id("horseman_rage")), "and faster");
			horseman.performRangedAttack(target, 1.0F);
			thrown = around(helper, FlamingPumpkin.class, new BlockPos(5, 2, 5), 4.0);
			helper.assertTrue(thrown.size() == 3, "Enraged, he throws three: " + thrown.size());
			thrown.forEach(Entity::discard);
		} finally {
			horseman.discard();
		}
		helper.succeed();
	}

	/**
	 * He is saved with the world: his arena, whom he hunts, his rage, loneliness and health. Loaded after the event has ended (a restart
	 * across the boundary) he leaves at once.
	 */
	@GameTest
	public void theHorsemanIsSavedWithHisArena(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		HeadlessHorseman horseman = helper.spawnWithNoFreeWill(JugcraftAgriculture.HEADLESS_HORSEMAN, new BlockPos(3, 2, 3));
		BlockPos home = helper.absolutePos(new BlockPos(1, 2, 1));
		ServerPlayer summoner = player(helper, new BlockPos(6, 2, 6), ItemStack.EMPTY);
		try {
			horseman.setHome(home);
			horseman.hunt(summoner);
			horseman.setHealth(horseman.getMaxHealth() / 2.0F);
			horseman.updateRage(level);
			horseman.setLonely(200);
			TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
			horseman.saveWithoutId(out);
			HeadlessHorseman copy = JugcraftAgriculture.HEADLESS_HORSEMAN.create(level, EntitySpawnReason.LOAD);
			copy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
			helper.assertTrue(copy.home().equals(home) && copy.enraged() && copy.lonely() == 200 && copy.getHealth() == horseman.getMaxHealth() / 2.0F
					&& copy.quarry().map(summoner.getUUID()::equals).orElse(false), "Arena, quarry, rage, loneliness and health are kept");
			try {
				HalloweenSeason.setMode(HalloweenSeason.Mode.ON);
				helper.assertFalse(copy.shouldLeave(MIDNIGHT), "Loaded during the event, at midnight, he rides on");
				HalloweenSeason.setMode(HalloweenSeason.Mode.OFF);
				helper.assertTrue(copy.shouldLeave(MIDNIGHT), "Loaded after it, he leaves");
			} finally {
				HalloweenSeason.reset();
			}
			copy.discard();
		} finally {
			horseman.discard();
		}
		helper.succeed();
	}

	// ---------------------------------------------------------------- what is earned, and the data

	/**
	 * Out of season everything earned stays and works: a Wisp in a Jar and the Horseman's Lantern stand and hang and
	 * give light, the cloak is worn. The recipe, loot, tags and advancements load.
	 */
	@GameTest
	public void nightTrophiesStayAndTheirDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		try {
			HalloweenSeason.setMode(HalloweenSeason.Mode.OFF);
			helper.setBlock(new BlockPos(2, 2, 2), block("wisp_in_a_jar"));
			helper.setBlock(new BlockPos(4, 2, 2), block("horseman_lantern"));
			helper.assertTrue(helper.getBlockState(new BlockPos(2, 2, 2)).getLightEmission() == JugcraftAgriculture.WISP_JAR_LIGHT
					&& helper.getBlockState(new BlockPos(4, 2, 2)).getLightEmission() == 15, "The jar and the lantern glow out of season");
			ItemStack cloak = new ItemStack(item("horseman_cloak"));
			helper.assertTrue(cloak.get(DataComponents.EQUIPPABLE) != null && cloak.get(DataComponents.EQUIPPABLE).slot() == EquipmentSlot.CHEST,
					"The cloak is worn on the chest");
		} finally {
			HalloweenSeason.reset();
		}
		helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id("trebuchet"))).isPresent(), "The trebuchet recipe loads");
		for (String id : List.of("wisp_in_a_jar", "pumpkin_chunkin", "headless_horseman")) {
			helper.assertTrue(level.getServer().getAdvancements().get(Jugcraft.id(id)) != null, "Advancement " + id + " loads");
		}
		for (String id : List.of("wisp_in_a_jar", "trebuchet", "horseman_lantern")) {
			LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("blocks/" + id)));
			helper.assertTrue(table != LootTable.EMPTY, "The " + id + " drops itself");
		}
		helper.assertTrue(new ItemStack(item("candy_corn")).is(CandyBagItem.TREATS) && new ItemStack(Items.COOKIE).is(CandyBagItem.TREATS)
				&& !new ItemStack(Items.DIRT).is(CandyBagItem.TREATS), "Candy and cookies are treats, dirt is not");
		helper.succeed();
	}
}
