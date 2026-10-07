package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.RestlessSpirit;
import io.github.jimbozoomer.jugcraft.agriculture.SpiritBoard;
import io.github.jimbozoomer.jugcraft.agriculture.SpiritBoardBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SpiritBoardBlockEntity;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;

/**
 * In-game tests for the Spirit Board: a séance needs a lit candle near and fingers close enough on the planchette, and
 * takes no more than four; with no spirit near the planchette goes to NO and GOODBYE; with one it goes to YES (Is Anybody
 * There?, and a comparator reads 15), spells the spirit's name and wish and GOODBYE, faster with two pairs of hands, then
 * rests; a hand that wanders off lifts, and with none left the séance breaks off; a revealed spirit turns from the wrong
 * gift and is laid to rest by the one it wished for (experience, Luck and Unfinished Business); and the data loads.
 */
public class SpiritBoardGameTests {
	private static final BlockPos BOARD = new BlockPos(3, 2, 3);
	private static final BlockPos CANDLE = new BlockPos(5, 2, 3);

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static Item vanilla(String id) {
		return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(id));
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		return player;
	}

	private static boolean earned(ServerPlayer player, String id) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Jugcraft.id(id));
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	private static SpiritBoardBlockEntity board(GameTestHelper helper, boolean candle) {
		helper.setBlock(BOARD, JugcraftAgriculture.block("spirit_board").defaultBlockState().setValue(SpiritBoardBlock.FACING, Direction.NORTH));
		if (candle) {
			helper.setBlock(CANDLE, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true));
		}
		return helper.getBlockEntity(BOARD, SpiritBoardBlockEntity.class);
	}

	/** A revealed spirit at {@code pos} with no AI of its own, named {@code name} and wishing for {@code wish}. */
	private static RestlessSpirit spirit(GameTestHelper helper, BlockPos pos, int name, SpiritBoard.Wish wish) {
		RestlessSpirit spirit = helper.spawnWithNoFreeWill(JugcraftAgriculture.RESTLESS_SPIRIT, pos);
		spirit.setNoAi(true);
		spirit.setPersistenceRequired();
		spirit.setHome(helper.absolutePos(pos.below()));
		spirit.promise(name, wish);
		spirit.reveal(helper.getLevel().getGameTime() + 1000);
		return spirit;
	}

	/** Whether a restless spirit (perhaps another test's) is near enough the board to answer it. */
	private static boolean spiritNear(GameTestHelper helper) {
		return !helper.getLevel().getEntitiesOfClass(RestlessSpirit.class, new AABB(helper.absolutePos(BOARD)).inflate(SpiritBoard.SPIRIT_RANGE))
				.isEmpty();
	}

	/**
	 * Without candlelight the planchette won't move; a lit candle two blocks off lets it. A player too far off can't touch
	 * it; four pairs of hands can, not a fifth.
	 */
	@GameTest(maxTicks = 20)
	public void aSeanceNeedsCandlelight(GameTestHelper helper) {
		SpiritBoardBlockEntity board = board(helper, false);
		ServerPlayer first = player(helper, new BlockPos(3, 2, 1));
		helper.assertTrue(!board.touch(first) && !board.active(), "No candle, no séance");
		helper.setBlock(CANDLE, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, false));
		helper.assertTrue(!board.touch(first) && !board.active(), "An unlit candle isn't candlelight");
		helper.setBlock(CANDLE, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true));
		ServerPlayer far = player(helper, new BlockPos(3, 2, 3 + SpiritBoard.HAND_RANGE + 3));
		helper.assertTrue(!board.touch(far), "Too far off to touch the planchette");
		helper.assertTrue(board.touch(first) && board.active() && board.hands().equals(List.of(first.getUUID())), "Candlelit, the séance starts");
		for (int i = 0; i < SpiritBoard.MAX_HANDS - 1; i++) {
			helper.assertTrue(board.touch(player(helper, new BlockPos(1 + i, 2, 5))), "Another pair of hands joins");
		}
		helper.assertTrue(!board.touch(player(helper, new BlockPos(5, 2, 5))) && board.hands().size() == SpiritBoard.MAX_HANDS,
				"No room for a fifth");
		helper.succeed();
	}

	/**
	 * With no spirit near, the planchette goes to NO (a comparator reads 1) and then GOODBYE, a stop every
	 * {@value SpiritBoard#LETTER_TICKS} ticks under one pair of hands, and comes back to rest. (If another test's spirit
	 * happens to be near, it answers instead, and the test checks it said YES.)
	 */
	@GameTest(maxTicks = 100)
	public void noSpiritSaysNo(GameTestHelper helper) {
		SpiritBoardBlockEntity board = board(helper, true);
		ServerPlayer sitter = player(helper, new BlockPos(3, 2, 1));
		boolean answered = spiritNear(helper);
		helper.assertTrue(board.touch(sitter), "The séance starts");
		helper.runAfterDelay(14, () -> {
			helper.assertTrue(board.to() == (answered ? SpiritBoard.YES : SpiritBoard.NO), "First stop: " + board.to());
			if (!answered) {
				helper.assertTrue(board.signal() == 1, "A comparator reads 1 on NO: " + board.signal());
			}
		});
		helper.runAfterDelay(14 + SpiritBoard.LETTER_TICKS, () -> {
			if (!answered) {
				helper.assertTrue(board.to() == SpiritBoard.GOODBYE, "Then GOODBYE: " + board.to());
			}
		});
		helper.runAfterDelay(14 + 3 * SpiritBoard.LETTER_TICKS, () -> {
			if (!answered) {
				helper.assertTrue(!board.active() && board.to() == SpiritBoard.REST && board.hands().isEmpty()
						&& board.spelled().equals("NO … GOODBYE"), "Then it rests: " + board.spelled());
				helper.assertTrue(!earned(sitter, "is_anybody_there"), "No one answered");
			}
			helper.succeed();
		});
	}

	/**
	 * A spirit near: YES (a comparator reads 15; Is Anybody There? for every pair of hands), then its name and its wish,
	 * then GOODBYE: twelve stops, a stop every {@value SpiritBoard#FAST_LETTER_TICKS} ticks under two pairs of hands. The
	 * board then rests, and won't start again at once.
	 */
	@GameTest(maxTicks = 240)
	public void aSpiritSpellsItsNameAndWish(GameTestHelper helper) {
		SpiritBoardBlockEntity board = board(helper, true);
		RestlessSpirit spirit = spirit(helper, new BlockPos(3, 4, 6), 0, SpiritBoard.Wish.PIE);
		ServerPlayer one = player(helper, new BlockPos(3, 2, 1));
		ServerPlayer two = player(helper, new BlockPos(1, 2, 3));
		helper.assertTrue(board.touch(one) && board.touch(two) && board.hands().size() == 2, "Two pairs of hands on the planchette");
		spirit.discard();
		String message = SpiritBoard.message(0, SpiritBoard.Wish.PIE);
		helper.assertTrue(message.equals("+ MABEL PIE."), "Mabel wishes for a pie: " + message);
		helper.runAfterDelay(14, () -> helper.assertTrue(board.to() == SpiritBoard.YES && board.signal() == 15
				&& earned(one, "is_anybody_there") && earned(two, "is_anybody_there"), "YES, and both earn Is Anybody There?"));
		int done = 10 + message.length() * SpiritBoard.FAST_LETTER_TICKS;
		helper.runAfterDelay(done - 4, () -> helper.assertTrue(board.active() && board.to() == SpiritBoard.GOODBYE
				&& board.spelled().equals("YES MABEL PIE … GOODBYE"), "It spelled it all, twice as fast: " + board.spelled()));
		helper.runAfterDelay(done + 4, () -> {
			helper.assertTrue(!board.active() && board.hands().isEmpty(), "Then it rests");
			helper.assertTrue(!board.touch(one), "Resting, it won't start again at once");
			helper.succeed();
		});
	}

	/** A pair of hands that wanders off lifts from the planchette; with none left, the séance breaks off. */
	@GameTest(maxTicks = 40)
	public void wanderingHandsBreakItOff(GameTestHelper helper) {
		SpiritBoardBlockEntity board = board(helper, true);
		ServerPlayer sitter = player(helper, new BlockPos(3, 2, 1));
		helper.assertTrue(board.touch(sitter), "The séance starts");
		BlockPos away = helper.absolutePos(new BlockPos(3, 2, 3 + SpiritBoard.HAND_RANGE + 4));
		sitter.setPos(away.getX() + 0.5, away.getY(), away.getZ() + 0.5);
		helper.runAfterDelay(3, () -> {
			helper.assertTrue(!board.active() && board.hands().isEmpty(), "Walking away breaks the séance off");
			helper.succeed();
		});
	}

	/**
	 * Revealed, a spirit that wishes for an apple turns from a poppy; given an apple it takes one and rises away at rest:
	 * {@value SpiritBoard#REST_XP} experience, Luck, and Unfinished Business.
	 */
	@GameTest(maxTicks = 20)
	public void aSpiritIsLaidToRest(GameTestHelper helper) {
		RestlessSpirit spirit = spirit(helper, new BlockPos(3, 3, 4), 2, SpiritBoard.Wish.APPLE);
		ServerPlayer giver = player(helper, new BlockPos(3, 2, 2));
		helper.assertTrue(spirit.wish() == SpiritBoard.Wish.APPLE && spirit.spiritName() == 2, "It wishes for an apple");
		giver.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(vanilla("poppy")));
		giver.interactOn(spirit, InteractionHand.MAIN_HAND, spirit.position());
		helper.assertTrue(!spirit.isRemoved() && giver.getMainHandItem().getCount() == 1, "It turns from a poppy");
		int before = giver.totalExperience;
		giver.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(vanilla("apple"), 2));
		giver.interactOn(spirit, InteractionHand.MAIN_HAND, spirit.position());
		helper.assertTrue(spirit.isRemoved() && giver.getMainHandItem().getCount() == 1, "Given an apple, it takes one and is at rest");
		helper.assertTrue(giver.totalExperience - before == SpiritBoard.REST_XP && giver.hasEffect(MobEffects.LUCK)
				&& earned(giver, "unfinished_business"), "Experience, Luck and Unfinished Business");
		helper.succeed();
	}

	/** The wishes' tags hold what they should; the candles count; the recipe, loot table and advancements load. */
	@GameTest
	public void spiritBoardDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(new ItemStack(item("apple_pie_slice")).is(SpiritBoard.Wish.PIE.items) && new ItemStack(item("knit_sweater"))
				.is(SpiritBoard.Wish.SWEATER.items) && new ItemStack(vanilla("candle")).is(SpiritBoard.Wish.CANDLE.items)
				&& new ItemStack(item("aged_cider")).is(SpiritBoard.Wish.CIDER.items) && new ItemStack(item("fudge")).is(SpiritBoard.Wish.CANDY.items)
				&& new ItemStack(vanilla("jack_o_lantern")).is(SpiritBoard.Wish.PUMPKIN.items)
				&& !new ItemStack(item("wool_socks")).is(SpiritBoard.Wish.SWEATER.items), "The wishes' items");
		helper.assertTrue(Blocks.CANDLE.defaultBlockState().is(SpiritBoard.CANDLES) && JugcraftAgriculture.block("aura_candle").defaultBlockState()
				.is(SpiritBoard.CANDLES), "Candles and aura candles light a séance");
		helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id("spirit_board"))).isPresent(),
				"The recipe loads");
		helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
				Jugcraft.id("blocks/spirit_board"))) != LootTable.EMPTY, "The loot table loads");
		for (String id : List.of("is_anybody_there", "unfinished_business")) {
			helper.assertTrue(level.getServer().getAdvancements().get(Jugcraft.id(id)) != null, id + " loads");
		}
		helper.succeed();
	}
}
