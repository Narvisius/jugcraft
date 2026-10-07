package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.BowlingPumpkin;
import io.github.jimbozoomer.jugcraft.agriculture.BowlingScore;
import io.github.jimbozoomer.jugcraft.agriculture.BowlingScoreboardBlock;
import io.github.jimbozoomer.jugcraft.agriculture.BowlingScoreboardBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CandyBowlBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CandyBowlBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CostumeRunwayBlock;
import io.github.jimbozoomer.jugcraft.agriculture.DanceFloorBlock;
import io.github.jimbozoomer.jugcraft.agriculture.FortuneTellerTableBlock;
import io.github.jimbozoomer.jugcraft.agriculture.FortuneTellerTableBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.GhostBellBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GhostBellBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JudgesTableBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JudgesTableBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.JumpScareTrapBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SkeletonPinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TrickOrTreat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the party games: the Jump-Scare Trap (sprung by someone walking up or by redstone, then resetting),
 * the costume contest (entries on the runway, one vote each, a ribbon for the winner), Pumpkin Bowling (ten-pin
 * scoring, a pumpkin rolled down a lane of Skeleton Pins and scored), the Candy Cache (a hidden candy bowl), the
 * Monster Mash Dance Floor (lit by music, villagers dancing), Ghost Tag (tags that do no harm, no tag-backs) and the
 * Fortune Teller's Table, and that their data loads.
 *
 * Other tests' players may stand near these, so the games are checked by who is in them, not by how many.
 */
public class Decor11GameTests {
	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, float yRot, ItemStack held) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		player.setYRot(yRot);
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

	private static int dropped(GameTestHelper helper, Item item) {
		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 8, 8).inflate(2.0);
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item)).stream()
				.mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	private static int count(ServerPlayer player, Item item) {
		int count = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			count += stack.is(item) ? stack.getCount() : 0;
		}
		return count;
	}

	// ---------------------------------------------------------------- the jump-scare trap

	/**
	 * Someone sneaking up to the trap's front doesn't spring it; someone walking up does: the lid bursts open (popped),
	 * then it shuts and rests (resetting), then it is ready again; a rising redstone signal springs it too.
	 */
	@GameTest(maxTicks = 220)
	public void jumpScareTrapsSpringAndReset(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 4);
		helper.setBlock(pos, block("jump_scare_trap").defaultBlockState().setValue(JumpScareTrapBlock.FACING, Direction.NORTH));
		ServerPlayer sneak = player(helper, pos.north(), 180.0F, ItemStack.EMPTY);
		sneak.setShiftKeyDown(true);
		helper.runAfterDelay(12, () -> {
			helper.assertTrue(phase(helper, pos) == JumpScareTrapBlock.Phase.READY, "Sneaking up doesn't spring it");
			sneak.setShiftKeyDown(false);
			helper.runAfterDelay(8, () -> {
				helper.assertTrue(phase(helper, pos) == JumpScareTrapBlock.Phase.POPPED, "Walking up springs it");
				BlockPos away = helper.absolutePos(pos).above(40);
				sneak.setPos(away.getX() + 0.5, away.getY(), away.getZ() + 0.5);
				helper.runAfterDelay(JumpScareTrapBlock.POP_TICKS + 2, () -> {
					helper.assertTrue(phase(helper, pos) == JumpScareTrapBlock.Phase.RESETTING, "The ghost goes back and the lid shuts");
					helper.runAfterDelay(JumpScareTrapBlock.RESET_TICKS + 2, () -> {
						helper.assertTrue(phase(helper, pos) == JumpScareTrapBlock.Phase.READY, "and it is ready again");
						helper.setBlock(pos.east(), Blocks.REDSTONE_BLOCK);
						helper.assertTrue(phase(helper, pos) == JumpScareTrapBlock.Phase.POPPED && helper.getBlockState(pos).getValue(JumpScareTrapBlock.POWERED),
								"A redstone signal (a tripwire) springs it");
						helper.succeed();
					});
				});
			});
		});
	}

	private static JumpScareTrapBlock.Phase phase(GameTestHelper helper, BlockPos pos) {
		return helper.getBlockState(pos).getValue(JumpScareTrapBlock.PHASE);
	}

	// ---------------------------------------------------------------- the costume contest

	/**
	 * Ringing the Judges' Table opens a round; players in costume on the runway are entered, others not; each voter
	 * has one vote (using a contestant with an empty hand), moves it by voting again, never votes for themselves; the
	 * round survives a save and load; at the end the most votes win a Best Costume Ribbon and the table closes.
	 */
	@GameTest(maxTicks = 40)
	public void costumeContestsVoteForTheBest(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		for (int z = 1; z <= 5; z++) {
			helper.setBlock(new BlockPos(2, 2, z), block("costume_runway").defaultBlockState().setValue(CostumeRunwayBlock.AXIS, Direction.Axis.Z));
		}
		BlockPos tablePos = new BlockPos(2, 2, 6);
		helper.setBlock(tablePos, block("judges_table").defaultBlockState().setValue(JudgesTableBlock.FACING, Direction.NORTH));
		JudgesTableBlockEntity table = (JudgesTableBlockEntity) helper.getBlockEntity(tablePos, JudgesTableBlockEntity.class);
		ServerPlayer witch = player(helper, new BlockPos(2, 2, 2), 0.0F, ItemStack.EMPTY);
		witch.setItemSlot(EquipmentSlot.HEAD, new ItemStack(item("witch_hat")));
		ServerPlayer plain = player(helper, new BlockPos(2, 2, 4), 0.0F, ItemStack.EMPTY);
		ServerPlayer judge = player(helper, new BlockPos(5, 2, 3), 90.0F, ItemStack.EMPTY);
		ServerPlayer fan = player(helper, new BlockPos(5, 2, 4), 90.0F, ItemStack.EMPTY);

		helper.assertTrue(!table.open(), "No round is open at first");
		helper.assertTrue(use(helper, judge, tablePos, Direction.NORTH).consumesAction() && table.open()
				&& helper.getBlockState(tablePos).getValue(JudgesTableBlock.OPEN), "Ringing the bell opens a round");
		table.enterWalkers(level);
		helper.assertTrue(table.contestants().contains(witch.getUUID()) && !table.contestants().contains(plain.getUUID()),
				"The witch on the runway is entered; the player without a costume isn't");
		plain.setItemSlot(EquipmentSlot.HEAD, new ItemStack(item("ghost_sheet")));
		table.enterWalkers(level);
		helper.assertTrue(table.contestants().contains(plain.getUUID()), "In a ghost sheet, they are");

		helper.assertTrue(JudgesTableBlockEntity.onUseEntity(judge, level, InteractionHand.MAIN_HAND, witch) == InteractionResult.SUCCESS
				&& table.votesFor(witch.getUUID()) == 1, "Using a contestant votes for them");
		helper.assertTrue(table.vote(witch.getUUID(), witch.getUUID()) == JudgesTableBlockEntity.Vote.OWN, "Nobody votes for themselves");
		helper.assertTrue(table.vote(judge.getUUID(), plain.getUUID()) == JudgesTableBlockEntity.Vote.MOVED && table.votesFor(witch.getUUID()) == 0,
				"Voting again moves the one vote");
		helper.assertTrue(table.vote(judge.getUUID(), plain.getUUID()) == JudgesTableBlockEntity.Vote.SAME, "The same vote again changes nothing");
		fan.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
		helper.assertTrue(JudgesTableBlockEntity.onUseEntity(fan, level, InteractionHand.MAIN_HAND, plain) == InteractionResult.PASS,
				"Using a contestant with something in hand is no vote");
		helper.assertTrue(table.vote(fan.getUUID(), plain.getUUID()) == JudgesTableBlockEntity.Vote.VOTED
				&& table.vote(witch.getUUID(), plain.getUUID()) == JudgesTableBlockEntity.Vote.VOTED && table.votesFor(plain.getUUID()) == 3,
				"Three votes for the ghost");
		helper.assertTrue(table.vote(fan.getUUID(), judge.getUUID()) == JudgesTableBlockEntity.Vote.NOT_IN_ROUND, "Only contestants take votes");

		var saved = table.saveWithoutMetadata(level.registryAccess());
		JudgesTableBlockEntity loaded = new JudgesTableBlockEntity(helper.absolutePos(tablePos), helper.getBlockState(tablePos));
		loaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
		helper.assertTrue(loaded.open() && loaded.contestants().equals(table.contestants()) && loaded.votesFor(plain.getUUID()) == 3,
				"The round, its contestants and its votes survive a save and load");

		table.finish(level);
		helper.assertTrue(count(plain, item("best_costume_ribbon")) == 1 && count(witch, item("best_costume_ribbon")) == 0,
				"The most votes win the Best Costume Ribbon");
		helper.assertTrue(!table.open() && !helper.getBlockState(tablePos).getValue(JudgesTableBlock.OPEN) && table.contestants().isEmpty(),
				"and the table closes");
		helper.succeed();
	}

	// ---------------------------------------------------------------- pumpkin bowling

	/** Ten-pin scoring for lanes of any size: a perfect game, all spares, gutters, open frames, the tenth frame's bonus. */
	@GameTest
	public void bowlingScoresLikeTenPin(GameTestHelper helper) {
		helper.assertTrue(BowlingScore.score(Collections.nCopies(12, 10), 10) == 300 && BowlingScore.over(Collections.nCopies(12, 10), 10),
				"Twelve strikes are a perfect 300, and the game is over");
		helper.assertTrue(!BowlingScore.over(Collections.nCopies(11, 10), 10), "Eleven strikes leave one more roll");
		helper.assertTrue(BowlingScore.score(Collections.nCopies(21, 5), 10) == 150, "All spares of five score 150");
		helper.assertTrue(BowlingScore.score(Collections.nCopies(20, 0), 10) == 0 && BowlingScore.over(Collections.nCopies(20, 0), 10),
				"A gutter game scores nothing in twenty rolls");
		List<Integer> open = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			open.add(9);
			open.add(0);
		}
		helper.assertTrue(BowlingScore.score(open, 10) == 90 && BowlingScore.over(open, 10), "Nine and a miss every frame is 90");
		helper.assertTrue(BowlingScore.score(List.of(10, 3, 4), 10) == 24, "A strike counts the next two rolls");
		helper.assertTrue(BowlingScore.score(List.of(6, 4, 3), 10) == 16, "A spare counts the next roll");
		helper.assertTrue(BowlingScore.score(Collections.nCopies(12, 3), 3) == 90, "A three-pin lane can bowl a perfect game too");
		int[] next = BowlingScore.next(List.of(10, 3), 10);
		helper.assertTrue(next[0] == 1 && next[1] == 1, "After a strike and a three, the second ball of frame two comes");
		helper.assertTrue(BowlingScore.marks(List.of(10, 0, 10, 7, 2), 10).equals(List.of("X", "-/", "72")), "Marks: a strike, a spare, an open frame");
		helper.assertTrue(BowlingScore.standPins(List.of(10), 10, 0) && !BowlingScore.standPins(List.of(7), 10, 3)
				&& BowlingScore.standPins(List.of(7, 2), 10, 1), "The pins go back up after a strike or a frame, not between balls");
		helper.succeed();
	}

	/**
	 * A Bowling Pumpkin rolled down a lane runs through three Skeleton Pins, knocking them down the way it rolls, stops
	 * at the wall at the end and comes to rest as an item; the nearby Scoreboard scores the strike and stands the pins up
	 * again; a fallen pin stands up when used.
	 */
	@GameTest(maxTicks = 160)
	public void bowlingPumpkinsKnockDownPins(GameTestHelper helper) {
		floor(helper);
		BlockPos[] pins = {new BlockPos(3, 2, 3), new BlockPos(3, 2, 4), new BlockPos(3, 2, 5)};
		for (BlockPos pin : pins) {
			helper.setBlock(pin, block("skeleton_pin"));
		}
		for (int x = 2; x <= 4; x++) {
			helper.setBlock(new BlockPos(x, 2, 7), Blocks.STONE);
		}
		BlockPos boardPos = new BlockPos(6, 2, 4);
		helper.setBlock(boardPos, block("bowling_scoreboard").defaultBlockState().setValue(BowlingScoreboardBlock.FACING, Direction.WEST));
		BowlingScoreboardBlockEntity board = (BowlingScoreboardBlockEntity) helper.getBlockEntity(boardPos, BowlingScoreboardBlockEntity.class);
		ServerPlayer bowler = player(helper, new BlockPos(3, 2, 1), 0.0F, new ItemStack(item("bowling_pumpkin"), 2));
		bowler.setXRot(0.0F);
		helper.assertTrue(bowler.getMainHandItem().use(helper.getLevel(), bowler, InteractionHand.MAIN_HAND).consumesAction()
				&& bowler.getMainHandItem().getCount() == 1, "Using a Bowling Pumpkin rolls one");
		helper.runAfterDelay(40, () -> {
			AABB lane = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 4, 8);
			helper.assertTrue(helper.getLevel().getEntitiesOfClass(BowlingPumpkin.class, lane).isEmpty() && dropped(helper, item("bowling_pumpkin")) == 1,
					"It stops at the wall and comes to rest as an item");
			for (BlockPos pin : pins) {
				helper.assertTrue(helper.getBlockState(pin).getValue(SkeletonPinBlock.DOWN)
						&& helper.getBlockState(pin).getValue(SkeletonPinBlock.FACING) == Direction.SOUTH, "Every pin is knocked down the way it rolled");
			}
			helper.assertTrue(board.rolls().equals(List.of(3)) && board.pins() == 3 && BowlingScore.score(board.rolls(), board.pins()) == 3,
					"The scoreboard scores a strike on a three-pin lane: " + board.rolls());
			helper.runAfterDelay(BowlingScoreboardBlock.RESET_TICKS + 5, () -> {
				for (BlockPos pin : pins) {
					helper.assertTrue(!helper.getBlockState(pin).getValue(SkeletonPinBlock.DOWN), "The scoreboard stands the pins up again");
				}
				SkeletonPinBlock.knock(helper.getLevel(), helper.absolutePos(pins[0]), Direction.EAST);
				helper.assertTrue(helper.getBlockState(pins[0]).getValue(SkeletonPinBlock.DOWN), "A knocked pin lies down");
				ServerPlayer setter = player(helper, new BlockPos(2, 2, 3), 270.0F, ItemStack.EMPTY);
				use(helper, setter, pins[0], Direction.WEST);
				helper.assertTrue(!helper.getBlockState(pins[0]).getValue(SkeletonPinBlock.DOWN), "and stands up when used");
				helper.succeed();
			});
		});
	}

	// ---------------------------------------------------------------- the candy cache

	/** The Candy Cache keeps treats as a Candy Bowl does: filled with treats, one a night for each finder. */
	@GameTest
	public void candyCachesHideTreats(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, block("candy_cache"));
		CandyBowlBlockEntity cache = (CandyBowlBlockEntity) helper.getBlockEntity(pos, CandyBowlBlockEntity.class);
		ServerPlayer hider = player(helper, pos.south(), 180.0F, new ItemStack(item("candy_corn"), 5));
		helper.assertTrue(use(helper, hider, pos, Direction.UP).consumesAction() && cache.count() == 5, "Treats go into the cache");
		helper.assertTrue(helper.getBlockState(pos).getValue(CandyBowlBlock.FILL) == 1, "It knows how full it is");
		long tonight = TrickOrTreat.night(TrickOrTreat.DUSK + TrickOrTreat.DAY * 4000);
		ServerPlayer finder = player(helper, pos.north(), 0.0F, ItemStack.EMPTY);
		helper.assertTrue(cache.take(finder, tonight) == CandyBowlBlockEntity.Taken.TAKEN && cache.take(finder, tonight) == CandyBowlBlockEntity.Taken.HAD_ONE,
				"A finder takes one treat a night");
		helper.assertTrue(count(finder, item("candy_corn")) == 1, "and has it");
		helper.succeed();
	}

	// ---------------------------------------------------------------- the dance floor

	/**
	 * A line of dance floor tiles lights up from a jukebox playing a disc, a step further each tile, dark past its reach;
	 * without the music it goes dark; a redstone signal lights a tile and those beside it; a villager on a lit tile
	 * dances.
	 */
	@GameTest(maxTicks = 120)
	public void danceFloorsLightUpToTheMusic(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<BlockPos> tiles = new ArrayList<>();
		for (int x = 0; x <= 7; x++) {
			tiles.add(new BlockPos(x, 1, 1));
		}
		tiles.add(new BlockPos(7, 1, 2));
		tiles.add(new BlockPos(7, 1, 3));
		for (BlockPos tile : tiles) {
			helper.setBlock(tile, block("dance_floor"));
		}
		BlockPos jukebox = new BlockPos(0, 2, 1);
		helper.setBlock(jukebox, Blocks.JUKEBOX.defaultBlockState().setValue(JukeboxBlock.HAS_RECORD, true));
		helper.runAfterDelay(20, () -> {
			for (int i = 0; i < tiles.size(); i++) {
				int expected = Math.min(i, DanceFloorBlock.REACH);
				helper.assertTrue(distance(helper, tiles.get(i)) == expected, "Tile " + i + " is " + expected + " from the music: " + distance(helper, tiles.get(i)));
			}
			helper.assertTrue(helper.getBlockState(tiles.get(0)).getLightEmission() == DanceFloorBlock.LIGHT
					&& helper.getBlockState(tiles.get(9)).getLightEmission() == 0, "Lit tiles shine; past its reach, dark");
			Villager villager = (Villager) helper.spawn(BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("villager")),
					new BlockPos(1, 2, 1));
			villager.setNoAi(true);
			float before = villager.getYRot();
			BlockPos tile = helper.absolutePos(tiles.get(1));
			level.getBlockState(tile).randomTick(level, tile, level.getRandom());
			helper.assertTrue(Math.abs(villager.getYRot() - before - 90.0F) < 0.01F, "A villager on a lit tile spins");
			villager.discard();
			helper.setBlock(jukebox, Blocks.AIR);
			helper.runAfterDelay(40, () -> {
				for (int i = 0; i < tiles.size(); i++) {
					helper.assertTrue(distance(helper, tiles.get(i)) == DanceFloorBlock.REACH, "Without the music tile " + i + " goes dark");
				}
				helper.setBlock(new BlockPos(5, 0, 1), Blocks.REDSTONE_BLOCK);
				helper.runAfterDelay(5, () -> {
					helper.assertTrue(distance(helper, tiles.get(5)) == 0 && distance(helper, tiles.get(7)) == 2,
							"A redstone signal lights a tile and those beside it");
					helper.succeed();
				});
			});
		});
	}

	private static int distance(GameTestHelper helper, BlockPos tile) {
		return helper.getBlockState(tile).getValue(DanceFloorBlock.DISTANCE);
	}

	// ---------------------------------------------------------------- ghost tag

	/**
	 * Ringing the Ghost Bell starts a round with everyone near, one of them "it" and glowing; "it" hitting another
	 * player tags them (no harm done: the hit is cancelled) and they glow instead; they can't tag straight back; players
	 * outside the round are left alone; the round survives a save and load; ending it stops the bell.
	 */
	@GameTest(maxTicks = 40)
	public void ghostTagPassesTheGhostOn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos bellPos = new BlockPos(3, 2, 3);
		helper.setBlock(bellPos, block("ghost_bell").defaultBlockState().setValue(GhostBellBlock.FACING, Direction.NORTH));
		GhostBellBlockEntity bell = (GhostBellBlockEntity) helper.getBlockEntity(bellPos, GhostBellBlockEntity.class);
		ServerPlayer alice = player(helper, new BlockPos(1, 2, 1), 0.0F, ItemStack.EMPTY);
		ServerPlayer bob = player(helper, new BlockPos(5, 2, 5), 0.0F, ItemStack.EMPTY);
		helper.assertTrue(use(helper, alice, bellPos, Direction.NORTH).consumesAction() && bell.running()
				&& helper.getBlockState(bellPos).getValue(GhostBellBlock.RINGING), "Ringing the bell starts a round");
		helper.assertTrue(bell.players().contains(alice.getUUID()) && bell.players().contains(bob.getUUID()) && bell.players().contains(bell.it()),
				"Everyone near is in it, and one of them is the ghost");
		ServerPlayer ghost = level.getServer().getPlayerList().getPlayer(bell.it());
		helper.assertTrue(ghost != null && ghost.hasEffect(MobEffects.GLOWING), "The ghost glows");
		ServerPlayer target = ghost.getUUID().equals(alice.getUUID()) ? bob : alice;
		float health = target.getHealth();
		helper.assertTrue(GhostBellBlockEntity.onAttack(ghost, level, InteractionHand.MAIN_HAND, target) == InteractionResult.SUCCESS
				&& target.getUUID().equals(bell.it()) && target.hasEffect(MobEffects.GLOWING) && target.getHealth() == health,
				"Hitting another player tags them, harmlessly, and they glow");
		helper.assertTrue(!ghost.hasEffect(MobEffects.GLOWING), "The one who tagged them stops glowing");
		helper.assertTrue(GhostBellBlockEntity.onAttack(target, level, InteractionHand.MAIN_HAND, ghost) == InteractionResult.SUCCESS
				&& target.getUUID().equals(bell.it()), "No tag-backs");
		ServerPlayer outsider = player(helper, new BlockPos(6, 2, 1), 0.0F, ItemStack.EMPTY);
		helper.assertTrue(GhostBellBlockEntity.onAttack(outsider, level, InteractionHand.MAIN_HAND, target) == InteractionResult.PASS,
				"A player outside the round is left alone");

		var saved = bell.saveWithoutMetadata(level.registryAccess());
		GhostBellBlockEntity loaded = new GhostBellBlockEntity(helper.absolutePos(bellPos), helper.getBlockState(bellPos));
		loaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
		helper.assertTrue(loaded.running() && loaded.players().equals(bell.players()) && target.getUUID().equals(loaded.it()),
				"The round and who is the ghost survive a save and load");
		bell.end(level);
		helper.assertTrue(!bell.running() && !helper.getBlockState(bellPos).getValue(GhostBellBlock.RINGING) && !target.hasEffect(MobEffects.GLOWING),
				"Ending the round stops the bell and the glowing");
		helper.succeed();
	}

	// ---------------------------------------------------------------- the fortune teller's table

	/** Using the table reads a fortune (a card and an answer), at most once in its cooldown. */
	@GameTest(maxTicks = 80)
	public void fortuneTablesReadFortunes(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, block("fortune_teller_table").defaultBlockState().setValue(FortuneTellerTableBlock.FACING, Direction.NORTH));
		FortuneTellerTableBlockEntity table = (FortuneTellerTableBlockEntity) helper.getBlockEntity(pos, FortuneTellerTableBlockEntity.class);
		ServerPlayer visitor = player(helper, pos.north(), 0.0F, ItemStack.EMPTY);
		long now = helper.getLevel().getGameTime();
		helper.assertTrue(use(helper, visitor, pos, Direction.NORTH).consumesAction() && table.marked() == now, "Using it reads a fortune");
		helper.assertTrue(table.card() >= 0 && table.card() < FortuneTellerTableBlock.CARDS && table.answer() >= 0
				&& table.answer() < FortuneTellerTableBlock.ANSWERS, "A card turns and the planchette moves");
		helper.runAfterDelay(5, () -> {
			use(helper, visitor, pos, Direction.NORTH);
			helper.assertTrue(table.marked() == now, "Not again so soon");
			helper.runAfterDelay(FortuneTellerTableBlock.COOLDOWN_TICKS, () -> {
				use(helper, visitor, pos, Direction.NORTH);
				helper.assertTrue(table.marked() > now, "Later, another reading");
				helper.succeed();
			});
		});
	}

	// ---------------------------------------------------------------- data

	/** The recipes and loot tables load, and the runway lies along the way it was laid. */
	@GameTest
	public void partyGamesDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("jump_scare_trap", "costume_runway", "judges_table", "skeleton_pin", "bowling_scoreboard", "candy_cache",
				"dance_floor", "ghost_bell", "fortune_teller_table")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), "Recipe " + id + " loads");
			LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("blocks/" + id)));
			helper.assertTrue(table != LootTable.EMPTY, "The " + id + " drops itself");
		}
		helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id("bowling_pumpkin"))).isPresent(),
				"Recipe bowling_pumpkin loads");
		helper.assertTrue(block("costume_runway").defaultBlockState().rotate(Rotation.CLOCKWISE_90)
				.getValue(CostumeRunwayBlock.AXIS) == Direction.Axis.X, "A turned runway turns its edging");
		helper.succeed();
	}
}
