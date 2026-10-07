package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.HighStrikerBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HighStrikerBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.Midway;
import io.github.jimbozoomer.jugcraft.agriculture.RingTossBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TossRing;
import io.github.jimbozoomer.jugcraft.agriculture.TossRingItem;
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
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the fall fair midway (fall addition 26): the High Striker is placed whole and breaks whole, dropping
 * once; a blow's strength decides how far the puck climbs; struck to the top, the puck climbs lamp by lamp, rings the bell
 * (a prize and Ring the Bell) and falls back, and can't be struck again until it has; only the mallet strikes it, from
 * near enough; Ring Toss judges ringers by the neck and the distance thrown, and lets the ring go after a while; a Toss
 * Ring is thrown; the plushes squeak and the prize table gives only plushes; and the data loads.
 */
public class MidwayGameTests {
	private static final TagKey<Item> PLUSHES = TagKey.create(Registries.ITEM, Jugcraft.id("plushes"));

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static Block striker() {
		return JugcraftAgriculture.block("high_striker");
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

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	/** A High Striker standing at {@code base} (relative), facing south, built part by part. */
	private static BlockPos build(GameTestHelper helper, BlockPos base) {
		BlockState state = striker().defaultBlockState().setValue(HighStrikerBlock.FACING, Direction.SOUTH);
		for (int part = 0; part < HighStrikerBlock.PARTS; part++) {
			helper.setBlock(base.above(part), state.setValue(HighStrikerBlock.PART, part));
		}
		return helper.absolutePos(base);
	}

	private static int level(GameTestHelper helper, BlockPos absolute) {
		BlockState state = helper.getLevel().getBlockState(absolute);
		return state.getBlock() instanceof HighStrikerBlock ? state.getValue(HighStrikerBlock.LEVEL) : -1;
	}

	private static int carried(ServerPlayer player, TagKey<Item> tag) {
		int count = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			if (player.getInventory().getItem(slot).is(tag)) {
				count += player.getInventory().getItem(slot).getCount();
			}
		}
		return count;
	}

	private static int lying(GameTestHelper helper, Item item) {
		int count = 0;
		AABB area = new AABB(helper.absolutePos(new BlockPos(0, 0, 0))).expandTowards(8, 8, 8);
		for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, area)) {
			if (entity.getItem().is(item)) {
				count += entity.getItem().getCount();
			}
		}
		return count;
	}

	/** Placed from its item, the striker stands five blocks tall facing the player; breaking its top drops it once. */
	@GameTest
	public void highStrikerStandsAndFallsWhole(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("high_striker")));
		BlockPos ground = helper.absolutePos(new BlockPos(3, 1, 3));
		player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(ground).add(0.0, 0.5, 0.0), Direction.UP, ground, false));
		BlockPos base = ground.above();
		for (int part = 0; part < HighStrikerBlock.PARTS; part++) {
			BlockState state = level.getBlockState(base.above(part));
			helper.assertTrue(state.is(striker()) && state.getValue(HighStrikerBlock.PART) == part, "Part " + part + " stands");
		}
		helper.assertTrue(player.getMainHandItem().isEmpty(), "Placing it uses the item");
		helper.assertTrue(level.getBlockEntity(base) instanceof HighStrikerBlockEntity, "Its base keeps the puck");
		level.destroyBlock(base.above(3), true);
		for (int part = 0; part < HighStrikerBlock.PARTS; part++) {
			helper.assertTrue(!level.getBlockState(base.above(part)).is(striker()), "Breaking a part breaks it all: part " + part);
		}
		helper.assertTrue(lying(helper, item("high_striker")) == 1, "It drops once, not " + lying(helper, item("high_striker")));
		helper.succeed();
	}

	/**
	 * A full swing climbs most of the way, the strongest rings the bell; a weak one climbs a lamp; a critical swing
	 * rings it from a middling roll.
	 */
	@GameTest
	public void strengthDecidesTheClimb(GameTestHelper helper) {
		helper.assertTrue(Midway.levelFor(1.0F, false, 1.0F) == HighStrikerBlock.RUNG, "The strongest full swing rings the bell");
		int weakest = Midway.levelFor(1.0F, false, 0.0F);
		helper.assertTrue(weakest > HighStrikerBlock.LEVELS / 2 && weakest < HighStrikerBlock.RUNG, "The weakest full swing climbs most of the way: " + weakest);
		helper.assertTrue(Midway.levelFor(0.1F, false, 1.0F) == 1, "A swing with no charge climbs a lamp");
		helper.assertTrue(Midway.levelFor(1.0F, true, 0.5F) == HighStrikerBlock.RUNG, "A critical swing rings it from a middling roll");
		helper.assertTrue(Midway.levelFor(1.0F, false, 0.5F) < HighStrikerBlock.RUNG, "where a plain one doesn't");
		helper.succeed();
	}

	/**
	 * Struck to the top, the puck climbs a lamp at a time (lighting the tower), rings the bell for a prize and Ring the
	 * Bell, rests, and falls back; struck again while in flight, nothing happens.
	 */
	@GameTest(maxTicks = 120)
	public void puckRingsTheBellAndFallsBack(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos base = build(helper, new BlockPos(3, 2, 3));
		ServerPlayer player = player(helper, new BlockPos(3, 2, 5));
		helper.assertTrue(Midway.strike(level, base, player, HighStrikerBlock.RUNG) == HighStrikerBlock.RUNG, "The puck goes up");
		helper.assertTrue(Midway.strike(level, base, player, 3) == 0, "and can't be struck again in flight");
		int climb = HighStrikerBlock.RISE_TICKS * HighStrikerBlock.RUNG;
		helper.runAfterDelay(HighStrikerBlock.RISE_TICKS * 3 + 1, () -> {
			int now = level(helper, base);
			helper.assertTrue(now >= 2 && now < HighStrikerBlock.RUNG, "It climbs a lamp at a time: " + now);
		});
		helper.runAfterDelay(climb + 3, () -> {
			helper.assertTrue(level(helper, base) == HighStrikerBlock.RUNG, "It reaches the bell");
			helper.assertTrue(HighStrikerBlock.light(level.getBlockState(base.above(HighStrikerBlock.PARTS - 1))) > 0, "and the tower is lit");
			helper.assertTrue(carried(player, PLUSHES) == 1, "The bell wins a plush");
			helper.assertTrue(earned(player, "ring_the_bell") && earned(player, "step_right_up"), "Ring the Bell and Step Right Up are earned");
		});
		helper.runAfterDelay(climb + HighStrikerBlock.HOLD_TICKS + HighStrikerBlock.RUNG + 6, () -> {
			helper.assertTrue(level(helper, base) == 0, "It falls back to the bottom");
			helper.assertTrue(level.getBlockEntity(base) instanceof HighStrikerBlockEntity striker && !striker.busy(), "and may be struck again");
			helper.succeed();
		});
	}

	/** Only the Carnival Mallet strikes it, and only from near enough; with anything else a left click passes. */
	@GameTest
	public void onlyTheMalletStrikes(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos base = build(helper, new BlockPos(1, 2, 1));
		ServerPlayer player = player(helper, new BlockPos(1, 2, 3));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.DIRT));
		helper.assertTrue(Midway.attack(player, level, InteractionHand.MAIN_HAND, base) == InteractionResult.PASS, "A bare left click passes");
		helper.assertTrue(level(helper, base) == 0 && !((HighStrikerBlockEntity) level.getBlockEntity(base)).busy(), "and strikes nothing");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Midway.mallet()));
		helper.assertTrue(Midway.attack(player, level, InteractionHand.MAIN_HAND, base) != InteractionResult.PASS, "The mallet strikes");
		helper.assertTrue(((HighStrikerBlockEntity) level.getBlockEntity(base)).busy(), "and the puck goes up");
		helper.assertTrue(Midway.attack(player, level, InteractionHand.MAIN_HAND, base.above(2)) == InteractionResult.PASS,
				"Only the base is struck, not the tower");

		BlockPos far = build(helper, new BlockPos(6, 2, 1));
		player.setPos(far.getX() + 0.5 - 8.0, far.getY(), far.getZ() + 0.5);
		Midway.attack(player, level, InteractionHand.MAIN_HAND, far);
		helper.assertTrue(!((HighStrikerBlockEntity) level.getBlockEntity(far)).busy(), "From too far off, nothing");
		helper.succeed();
	}

	/**
	 * A ring coming down over a neck from three or more blocks off is a ringer: it settles over that bottle, wins a prize
	 * and Ringer!; between the necks, or tossed from close by, it isn't; the ring comes off after a while.
	 */
	@GameTest(maxTicks = 100)
	public void ringTossJudgesRingers(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		helper.assertTrue(RingTossBlock.neckAt(3.5, 3.5) == 0 && RingTossBlock.neckAt(8.0, 8.0) == 4 && RingTossBlock.neckAt(12.5, 12.5) == 8,
				"Necks are numbered row by row");
		helper.assertTrue(RingTossBlock.neckAt(5.75, 8.0) == -1, "Between two necks is no neck");
		helper.setBlock(new BlockPos(1, 2, 1), JugcraftAgriculture.block("ring_toss"));
		helper.setBlock(new BlockPos(6, 2, 1), JugcraftAgriculture.block("ring_toss"));
		BlockPos crate = helper.absolutePos(new BlockPos(1, 2, 1));
		BlockPos other = helper.absolutePos(new BlockPos(6, 2, 1));
		ServerPlayer player = player(helper, new BlockPos(1, 2, 6));
		Vec3 from = player.position();
		Vec3 overMiddle = new Vec3(crate.getX() + 0.5, crate.getY() + RingTossBlock.TOP / 16.0, crate.getZ() + 0.5);
		Vec3 between = new Vec3(crate.getX() + 5.75 / 16.0, crate.getY() + RingTossBlock.TOP / 16.0, crate.getZ() + 0.5);
		helper.assertTrue(!RingTossBlock.land(level, crate, between, from, player), "Between the necks, no ringer");
		helper.assertTrue(!RingTossBlock.land(level, other, new Vec3(other.getX() + 0.5, overMiddle.y, other.getZ() + 0.5),
				Vec3.atCenterOf(other).add(1.0, 0.0, 0.0), player), "Tossed from close by, no ringer");
		helper.assertTrue(RingTossBlock.land(level, crate, overMiddle, from, player), "Over the middle neck from five blocks off: a ringer");
		helper.assertTrue(level.getBlockState(crate).getValue(RingTossBlock.RINGED) == 5, "It settles over the fifth bottle");
		helper.assertTrue(carried(player, PLUSHES) == 1 && earned(player, "ringer") && earned(player, "step_right_up"), "It wins a plush and Ringer!");
		helper.runAfterDelay(RingTossBlock.RINGER_TICKS + 5, () -> {
			helper.assertTrue(level.getBlockState(crate).getValue(RingTossBlock.RINGED) == 0, "The ring comes off after a while");
			helper.succeed();
		});
	}

	/** A Toss Ring is thrown from the hand: a ring in flight, and one fewer in the stack. */
	@GameTest
	public void tossRingsAreThrown(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 3));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(TossRingItem.ID), 4));
		player.getMainHandItem().use(level, player, InteractionHand.MAIN_HAND);
		helper.assertTrue(player.getMainHandItem().getCount() == 3, "One ring is thrown");
		List<TossRing> rings = level.getEntitiesOfClass(TossRing.class, player.getBoundingBox().inflate(3.0));
		helper.assertTrue(rings.size() == 1 && rings.get(0).getOwner() == player, "It flies, thrown by the player");
		helper.succeed();
	}

	/** Every plush stands and squeaks when squeezed; the prize table gives only plushes, one a roll. */
	@GameTest
	public void plushesAndPrizes(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(4, 2, 6));
		int x = 0;
		for (Midway.Plush plush : Midway.PLUSHES) {
			BlockPos pos = new BlockPos(x++, 2, 3);
			helper.setBlock(pos, JugcraftAgriculture.block(plush.id()));
			BlockPos absolute = helper.absolutePos(pos);
			InteractionResult squeezed = player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(absolute), Direction.NORTH, absolute, false));
			helper.assertTrue(squeezed.consumesAction(), plush.id() + " squeaks when squeezed");
			helper.assertTrue(new ItemStack(item(plush.id())).is(PLUSHES), plush.id() + " is a plush");
		}
		for (int roll = 0; roll < 20; roll++) {
			List<ItemStack> won = Midway.prize(level, player, player.position());
			helper.assertTrue(won.size() == 1 && won.get(0).is(PLUSHES) && won.get(0).getCount() == 1, "Each prize is one plush: " + won);
		}
		helper.assertTrue(carried(player, PLUSHES) == 20, "and goes to the winner");
		helper.succeed();
	}

	/** The recipes, advancements and prize table load. */
	@GameTest
	public void midwayDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("high_striker", "carnival_mallet", "ring_toss", "toss_ring")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), id + " has a recipe");
		}
		for (String id : List.of("step_right_up", "ring_the_bell", "ringer", "jackpot")) {
			helper.assertTrue(level.getServer().getAdvancements().get(Jugcraft.id(id)) != null, id + " loads");
		}
		helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(Midway.PRIZES) != LootTable.EMPTY, "The prize table loads");
		helper.succeed();
	}
}
