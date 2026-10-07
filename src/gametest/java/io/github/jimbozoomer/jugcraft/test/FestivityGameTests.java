package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CandleSkullBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CarvingContest;
import io.github.jimbozoomer.jugcraft.agriculture.CarvingContest.Result;
import io.github.jimbozoomer.jugcraft.agriculture.CarvingTemplates;
import io.github.jimbozoomer.jugcraft.agriculture.CostumedMobs;
import io.github.jimbozoomer.jugcraft.agriculture.GravestoneBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.HalloweenPeddler;
import io.github.jimbozoomer.jugcraft.agriculture.HalloweenSeason;
import io.github.jimbozoomer.jugcraft.agriculture.HarvestScaleBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JudgingStandBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCarving;
import java.time.Clock;
import java.time.Instant;
import java.time.MonthDay;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the Halloween festivities: the carving contest (entries, one vote each, ribbons once the
 * event ends, and every seasonal rule: activation, deactivation, a restart across the boundary, no duplicate
 * prizes, earned ribbons kept), costumed mobs and their candy, the Halloween Peddler, gravestone engraving,
 * the Candle Skull, Hanging Ghost and Spun Cobweb, the spooky sweets, and that their data loads.
 *
 * <p>Each contest test runs in its own far-off Halloween (a fixed server clock), so no two share a contest.
 */
public class FestivityGameTests {
	private static final BlockPos STAND = new BlockPos(2, 2, 2);
	private static final BlockPos OTHER_STAND = new BlockPos(5, 2, 2);
	private static final MonthDay START = HalloweenSeason.DEFAULT_START;
	private static final MonthDay END = HalloweenSeason.DEFAULT_END;
	/** The Peddler's trade set's amount (PEDDLER in tools/agriculture.py). */
	private static final int PEDDLER_WARES = 4;

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

	/** The Halloween of {@code year}, on a fixed server clock: Halloween night itself. */
	private static void halloween(int year) {
		HalloweenSeason.setMode(HalloweenSeason.Mode.AUTO);
		HalloweenSeason.setWindow(START, END, ZoneOffset.UTC, Clock.fixed(Instant.parse(year + "-10-31T20:00:00Z"), ZoneOffset.UTC));
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

	/** A Judging Stand at {@code stand} with a hand-carved pumpkin on it, carved by {@code carver} (or nobody). */
	private static void entry(GameTestHelper helper, BlockPos stand, ServerPlayer carver) {
		helper.setBlock(stand, block("judging_stand"));
		helper.setBlock(stand.above(), block("hand_carved_pumpkin"));
		CarvedPumpkinBlockEntity pumpkin = (CarvedPumpkinBlockEntity) helper.getBlockEntity(stand.above(), CarvedPumpkinBlockEntity.class);
		pumpkin.setCarving(PumpkinCarving.BLANK.withFace(0, CarvingTemplates.ALL.get(0).face()), carver);
	}

	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos pos, Direction side) {
		BlockPos absolute = helper.absolutePos(pos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(side, 0.5), side, absolute, false);
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
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

	private static List<ItemEntity> itemsAround(GameTestHelper helper, BlockPos pos) {
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(3.0));
	}

	// ---------------------------------------------------------------- the carving contest

	/**
	 * Through the stand itself: only the carver enters their pumpkin, nobody votes for themselves, each player has one
	 * vote (voting again moves it), votes are counted per entrant, taking the pumpkin off withdraws the entry, and
	 * out of season the stand takes no votes.
	 */
	@GameTest
	public void judgingStandEntriesAndOneVoteEach(GameTestHelper helper) {
		floor(helper);
		ServerPlayer carver = player(helper, STAND.south(), ItemStack.EMPTY);
		ServerPlayer rival = player(helper, OTHER_STAND.south(), ItemStack.EMPTY);
		ServerPlayer judge = player(helper, STAND.south(2), ItemStack.EMPTY);
		entry(helper, STAND, carver);
		entry(helper, OTHER_STAND, rival);
		BlockPos stand = helper.absolutePos(STAND);
		BlockPos other = helper.absolutePos(OTHER_STAND);
		try {
			halloween(2101);
			helper.assertTrue(CarvingContest.enter(judge, stand) == Result.NOT_YOURS, "Only the carver may enter a carving");
			helper.assertTrue(use(helper, carver, STAND, Direction.SOUTH).consumesAction(), "Using the stand with an empty hand enters it");
			JudgingStandBlockEntity entered = (JudgingStandBlockEntity) helper.getBlockEntity(STAND, JudgingStandBlockEntity.class);
			helper.assertTrue(entered.entrant().map(carver.getUUID()::equals).orElse(false), "The carver's entry is on the stand");
			helper.assertTrue(CarvingContest.enter(carver, stand) == Result.ALREADY_ENTERED, "A stand holds one entry");
			helper.assertTrue(CarvingContest.enter(rival, other) == Result.ENTERED, "The rival enters their own");
			helper.assertTrue(CarvingContest.vote(carver, stand) == Result.OWN_ENTRY, "Nobody votes for themselves");

			helper.assertTrue(use(helper, judge, STAND, Direction.SOUTH).consumesAction(), "Using an entered stand votes");
			helper.assertTrue(CarvingContest.vote(judge, stand) == Result.SAME, "The vote is already there");
			helper.assertTrue(CarvingContest.vote(judge, other) == Result.MOVED, "Voting elsewhere moves the one vote");
			helper.assertTrue(CarvingContest.vote(carver, other) == Result.VOTED, "The carver may vote for the rival");
			List<CarvingContest.Standing> standings = CarvingContest.standings(helper.getLevel().getServer(), 2101);
			helper.assertTrue(standings.size() == 1 && standings.get(0).entrant().equals(rival.getUUID()) && standings.get(0).votes() == 2,
					"Two votes for the rival, none for the carver: " + standings);

			helper.setBlock(STAND.above(), Blocks.AIR);
			helper.assertTrue(entered.entrant().isEmpty(), "Taking the pumpkin off withdraws the entry");
			helper.assertTrue(CarvingContest.vote(judge, stand) == Result.NO_PUMPKIN, "An empty stand takes no votes");

			HalloweenSeason.setMode(HalloweenSeason.Mode.OFF);
			helper.assertTrue(CarvingContest.vote(judge, other) == Result.CLOSED, "Out of season the stand takes no votes");
			helper.assertTrue(CarvingContest.standings(helper.getLevel().getServer(), 2101).get(0).votes() == 2, "and the votes cast stay");
		} finally {
			HalloweenSeason.reset();
		}
		helper.succeed();
	}

	/**
	 * The seasonal rules: votes count while the event runs (activation) and survive a save and load (a restart);
	 * when the event ends (deactivation) the leaders get the Harvest Scale's ribbons once, however often the server
	 * checks (no duplicates); switching the event on again that Halloween takes no more votes; the ribbons stay.
	 */
	@GameTest
	public void contestRibbonsComeOnceWhenTheEventEnds(GameTestHelper helper) {
		floor(helper);
		ServerPlayer first = player(helper, STAND.south(), ItemStack.EMPTY);
		ServerPlayer second = player(helper, OTHER_STAND.south(), ItemStack.EMPTY);
		ServerPlayer judgeA = player(helper, STAND.south(2), ItemStack.EMPTY);
		ServerPlayer judgeB = player(helper, STAND.south(3), ItemStack.EMPTY);
		entry(helper, STAND, first);
		entry(helper, OTHER_STAND, second);
		BlockPos stand = helper.absolutePos(STAND);
		BlockPos other = helper.absolutePos(OTHER_STAND);
		Item gold = item(HarvestScaleBlockEntity.RIBBONS.get(0));
		Item silver = item(HarvestScaleBlockEntity.RIBBONS.get(1));
		try {
			HalloweenSeason.setMode(HalloweenSeason.Mode.OFF);
			helper.assertTrue(CarvingContest.enter(first, stand) == Result.ENTERED && CarvingContest.enter(second, other) == Result.ENTERED,
					"Entering works any time of year");
			helper.assertTrue(CarvingContest.vote(judgeA, stand) == Result.CLOSED, "but voting waits for the event");

			halloween(2102);
			helper.assertTrue(CarvingContest.vote(judgeA, stand) == Result.VOTED && CarvingContest.vote(judgeB, stand) == Result.VOTED
					&& CarvingContest.vote(first, other) == Result.VOTED, "Votes count once the event runs");
			CarvingContest.check(helper.getLevel().getServer());
			helper.assertFalse(CarvingContest.data(helper.getLevel().getServer()).get(2102).awarded(), "Nothing is awarded while it runs");

			CarvingContest.Data data = CarvingContest.data(helper.getLevel().getServer());
			Tag saved = CarvingContest.Data.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
			CarvingContest.Data loaded = CarvingContest.Data.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
			List<CarvingContest.Standing> kept = loaded.get(2102).standings();
			helper.assertTrue(kept.size() == 2 && kept.get(0).entrant().equals(first.getUUID()) && kept.get(0).votes() == 2
					&& kept.get(1).votes() == 1 && !loaded.get(2102).awarded(), "The saved contest keeps every vote: " + kept);

			HalloweenSeason.setMode(HalloweenSeason.Mode.OFF);
			CarvingContest.check(helper.getLevel().getServer());
			helper.assertTrue(data.get(2102).awarded(), "The contest is decided when the event ends");
			helper.assertTrue(count(first, gold) == 1 && count(second, silver) == 1, "First and second place get their ribbons");
			CarvingContest.check(helper.getLevel().getServer());
			CarvingContest.check(helper.getLevel().getServer());
			helper.assertTrue(count(first, gold) == 1 && count(second, silver) == 1 && data.owed(first.getUUID()).isEmpty(),
					"Checking again gives nothing more");

			halloween(2102);
			helper.assertTrue(CarvingContest.vote(judgeA, other) == Result.CLOSED, "This Halloween's contest is over even if the event comes back");
			helper.assertTrue(count(first, gold) == 1 && count(second, silver) == 1, "The ribbons stay");
		} finally {
			HalloweenSeason.reset();
		}
		helper.succeed();
	}

	// ---------------------------------------------------------------- costumed mobs and the Peddler

	/** A random source whose first float is below the costume chance, so a roll dresses the mob. */
	private static RandomSource lucky() {
		for (long seed = 0; ; seed++) {
			if (RandomSource.create(seed).nextFloat() < CostumedMobs.CHANCE) {
				return RandomSource.create(seed);
			}
		}
	}

	/**
	 * Out of season no mob dresses up; in season a mob rolls once (a lucky roll dresses it in one of the costumes),
	 * never over a helmet and never twice; mobs not on the list never dress; a costume stays after the event.
	 */
	@GameTest
	public void costumedMobsRollOnceInSeason(GameTestHelper helper) {
		floor(helper);
		try {
			HalloweenSeason.setMode(HalloweenSeason.Mode.OFF);
			Mob zombie = helper.spawnWithNoFreeWill(vanilla("zombie"), new BlockPos(2, 2, 2));
			Mob helmeted = helper.spawnWithNoFreeWill(vanilla("skeleton"), new BlockPos(4, 2, 2));
			helmeted.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
			Mob cow = helper.spawnWithNoFreeWill(vanilla("cow"), new BlockPos(6, 2, 2));
			helper.assertFalse(CostumedMobs.dress(zombie, lucky()), "Out of season nobody dresses up");
			helper.assertFalse(zombie.entityTags().contains(CostumedMobs.ROLLED), "and the roll waits for the event");

			HalloweenSeason.setMode(HalloweenSeason.Mode.ON);
			helper.assertTrue(CostumedMobs.dress(zombie, lucky()), "A lucky roll dresses the zombie");
			ItemStack costume = zombie.getItemBySlot(EquipmentSlot.HEAD).copy();
			helper.assertTrue(CostumedMobs.COSTUMES.contains(BuiltInRegistries.ITEM.getKey(costume.getItem()).toString())
					&& zombie.entityTags().contains(CostumedMobs.COSTUMED), "in one of the costumes: " + costume);
			helper.assertFalse(CostumedMobs.dress(zombie, lucky()), "A mob rolls only once");
			helper.assertFalse(CostumedMobs.dress(helmeted, lucky()), "A helmet stays on");
			helper.assertTrue(helmeted.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "and is not swapped");
			helper.assertFalse(CostumedMobs.wears(cow), "Cows don't dress up");

			HalloweenSeason.setMode(HalloweenSeason.Mode.OFF);
			helper.assertTrue(ItemStack.matches(zombie.getItemBySlot(EquipmentSlot.HEAD), costume), "The costume stays after the event");
		} finally {
			HalloweenSeason.reset();
		}
		helper.succeed();
	}

	/** Killed by a player, a costumed zombie drops a sweet; an undressed one doesn't; the candy table gives only candy. */
	@GameTest
	public void costumedMobsDropCandyForTheirKiller(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		Set<Item> candy = new HashSet<>();
		for (String id : List.of("candy_corn", "caramel", "glow_gum", "ghost_taffy", "fizz_rocks", "witchs_licorice")) {
			candy.add(item(id));
		}
		ServerPlayer hunter = player(helper, new BlockPos(4, 2, 4), ItemStack.EMPTY);
		Mob dressed = helper.spawnWithNoFreeWill(vanilla("zombie"), new BlockPos(1, 2, 1));
		Mob plain = helper.spawnWithNoFreeWill(vanilla("zombie"), new BlockPos(6, 2, 6));
		CostumedMobs.dressNow(dressed, "jugcraft:witch_hat");
		plain.addTag(CostumedMobs.ROLLED);
		dressed.hurtServer(level, level.damageSources().playerAttack(hunter), 1000.0F);
		plain.hurtServer(level, level.damageSources().playerAttack(hunter), 1000.0F);
		helper.assertTrue(dressed.isDeadOrDying() && plain.isDeadOrDying(), "Both zombies fell");
		helper.assertTrue(itemsAround(helper, new BlockPos(1, 2, 1)).stream().anyMatch(e -> candy.contains(e.getItem().getItem())),
				"The costumed zombie dropped candy");
		helper.assertFalse(itemsAround(helper, new BlockPos(6, 2, 6)).stream().anyMatch(e -> candy.contains(e.getItem().getItem())),
				"The plain one didn't");
		LootTable table = level.getServer().reloadableRegistries().getLootTable(CostumedMobs.CANDY);
		helper.assertTrue(table != LootTable.EMPTY, "The candy table loads");
		LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, hunter.position())
				.withParameter(LootContextParams.THIS_ENTITY, hunter).create(LootContextParamSets.GIFT);
		for (int roll = 0; roll < 50; roll++) {
			List<ItemStack> given = table.getRandomItems(params);
			helper.assertTrue(!given.isEmpty() && given.stream().allMatch(stack -> candy.contains(stack.getItem())), "Candy only: " + given);
		}
		helper.succeed();
	}

	/**
	 * In season a wandering trader arrives as the Peddler: in a Witch Hat, named, with its usual wares plus the trade
	 * set's number of Halloween offers, once; out of season a trader stays itself.
	 */
	@GameTest
	public void thePeddlerSellsHalloweenWaresInSeason(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		TradeSet set = level.registryAccess().lookupOrThrow(Registries.TRADE_SET).getOptional(HalloweenPeddler.TRADES).orElse(null);
		helper.assertTrue(set != null, "The Peddler's trade set loads");
		try {
			HalloweenSeason.setMode(HalloweenSeason.Mode.ON);
			WanderingTrader peddler = helper.spawnWithNoFreeWill(vanilla("wandering_trader"), new BlockPos(2, 2, 2));
			helper.assertTrue(peddler.entityTags().contains(HalloweenPeddler.DRESSED) && peddler.getItemBySlot(EquipmentSlot.HEAD).is(item("witch_hat")),
					"The trader arrives in a Witch Hat");
			helper.assertTrue(Component.translatable("entity.jugcraft.halloween_peddler").equals(peddler.getCustomName()), "named the Halloween Peddler");
			long halloween = peddler.getOffers().stream().map(MerchantOffer::getResult)
					.filter(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(Jugcraft.MOD_ID)).count();
			helper.assertTrue(halloween == PEDDLER_WARES, "It brings the trade set's number of Halloween wares: " + halloween);
			int offers = peddler.getOffers().size();
			helper.assertTrue(HalloweenPeddler.visit(peddler, level) == 0 && peddler.getOffers().size() == offers, "and only once");
			for (MerchantOffer offer : peddler.getOffers()) {
				if (BuiltInRegistries.ITEM.getKey(offer.getResult().getItem()).getNamespace().equals(Jugcraft.MOD_ID)) {
					helper.assertTrue(offer.getBaseCostA().is(Items.EMERALD), "Halloween wares cost emeralds: " + offer);
				}
			}

			HalloweenSeason.setMode(HalloweenSeason.Mode.OFF);
			WanderingTrader trader = helper.spawnWithNoFreeWill(vanilla("wandering_trader"), new BlockPos(5, 2, 5));
			helper.assertFalse(trader.entityTags().contains(HalloweenPeddler.DRESSED) || trader.getItemBySlot(EquipmentSlot.HEAD).is(item("witch_hat")),
					"Out of season a trader is just a trader");
			helper.assertTrue(peddler.getItemBySlot(EquipmentSlot.HEAD).is(item("witch_hat")), "The Peddler keeps its hat until it leaves");
		} finally {
			HalloweenSeason.reset();
		}
		helper.succeed();
	}

	// ---------------------------------------------------------------- decorations and sweets

	/**
	 * A named Name Tag engraves a gravestone (and is kept); an unnamed one, or a player without build rights, does
	 * nothing; long names are cut short; breaking the stone keeps the engraving as its name, and placing it again
	 * brings the engraving back.
	 */
	@GameTest
	public void gravestonesTakeANameTagsName(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(2, 2, 2);
		helper.setBlock(pos, block("rounded_gravestone"));
		GravestoneBlockEntity stone = (GravestoneBlockEntity) helper.getBlockEntity(pos, GravestoneBlockEntity.class);
		ItemStack unnamed = new ItemStack(Items.NAME_TAG);
		ServerPlayer mason = player(helper, pos.south(), unnamed);
		use(helper, mason, pos, Direction.SOUTH);
		helper.assertTrue(stone.text().isEmpty(), "An unnamed tag engraves nothing");

		ItemStack tag = new ItemStack(Items.NAME_TAG);
		tag.set(DataComponents.CUSTOM_NAME, Component.literal("Here lies Jack"));
		ServerPlayer visitor = player(helper, pos.south(), tag.copy());
		visitor.setGameMode(GameType.ADVENTURE);
		use(helper, visitor, pos, Direction.SOUTH);
		helper.assertTrue(stone.text().isEmpty(), "Without build rights nothing is engraved");

		mason.setItemInHand(InteractionHand.MAIN_HAND, tag);
		helper.assertTrue(use(helper, mason, pos, Direction.SOUTH).consumesAction(), "A named tag engraves");
		helper.assertTrue(stone.text().equals("Here lies Jack") && mason.getMainHandItem().is(Items.NAME_TAG), "its name, and the tag is kept");
		stone.engrave("x".repeat(80) + "\n");
		helper.assertTrue(stone.text().length() == GravestoneBlockEntity.MAX_LENGTH, "Engravings are cut to " + GravestoneBlockEntity.MAX_LENGTH);

		stone.engrave("Rest in pieces");
		helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
		ItemStack dropped = itemsAround(helper, pos).stream().map(ItemEntity::getItem).filter(s -> s.is(item("rounded_gravestone")))
				.findFirst().orElse(ItemStack.EMPTY);
		helper.assertTrue(dropped.has(DataComponents.CUSTOM_NAME) && dropped.getHoverName().getString().equals("Rest in pieces"),
				"The broken gravestone keeps its engraving: " + dropped);
		mason.setItemInHand(InteractionHand.MAIN_HAND, dropped.copy());
		helper.assertTrue(use(helper, mason, pos.below(), Direction.UP).consumesAction(), "The gravestone goes back down");
		GravestoneBlockEntity again = (GravestoneBlockEntity) helper.getBlockEntity(pos, GravestoneBlockEntity.class);
		helper.assertTrue(again.text().equals("Rest in pieces"), "with its engraving");
		helper.succeed();
	}

	/** Flint and steel lights a Candle Skull (light 12) and wears; an empty hand snuffs it; a fire charge is used up. */
	@GameTest
	public void candleSkullsLightAndSnuff(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(2, 2, 2);
		helper.setBlock(pos, block("candle_skull"));
		ServerPlayer player = player(helper, pos.south(), new ItemStack(Items.FLINT_AND_STEEL));
		use(helper, player, pos, Direction.SOUTH);
		helper.assertTrue(helper.getBlockState(pos).getValue(CandleSkullBlock.LIT) && helper.getBlockState(pos).getLightEmission() == CandleSkullBlock.LIGHT,
				"Flint and steel lights it");
		helper.assertTrue(player.getMainHandItem().getDamageValue() == 1, "and wears");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, pos, Direction.SOUTH);
		helper.assertFalse(helper.getBlockState(pos).getValue(CandleSkullBlock.LIT), "An empty hand snuffs it");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FIRE_CHARGE, 2));
		use(helper, player, pos, Direction.SOUTH);
		helper.assertTrue(helper.getBlockState(pos).getValue(CandleSkullBlock.LIT) && player.getMainHandItem().getCount() == 1,
				"A fire charge lights it and is used up");
		helper.succeed();
	}

	/** A Hanging Ghost hangs under a block and falls when it goes; a Spun Cobweb has nothing to stick to. */
	@GameTest
	public void hangingGhostsAndSpunCobwebs(GameTestHelper helper) {
		floor(helper);
		BlockPos ceiling = new BlockPos(2, 4, 2);
		helper.setBlock(ceiling, Blocks.OAK_PLANKS);
		helper.setBlock(ceiling.below(), block("hanging_ghost"));
		helper.assertTrue(helper.getBlockState(ceiling.below()).canSurvive(helper.getLevel(), helper.absolutePos(ceiling.below())), "It hangs under planks");
		helper.setBlock(ceiling, Blocks.AIR);
		helper.assertBlockNotPresent(block("hanging_ghost"), ceiling.below());
		BlockPos web = new BlockPos(4, 2, 4);
		helper.setBlock(web, block("spun_cobweb"));
		helper.assertTrue(helper.getBlockState(web).getCollisionShape(helper.getLevel(), helper.absolutePos(web)).isEmpty()
				&& !(block("spun_cobweb") instanceof net.minecraft.world.level.block.WebBlock), "A Spun Cobweb neither blocks nor sticks");
		helper.succeed();
	}

	/** Each sweet can be eaten on a full stomach and gives its effect for its time. */
	@GameTest
	public void spookySweetsGiveTheirEffects(GameTestHelper helper) {
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1), ItemStack.EMPTY);
		record Sweet(String id, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int seconds) {
		}
		for (Sweet sweet : List.of(new Sweet("glow_gum", MobEffects.GLOWING, 30), new Sweet("ghost_taffy", MobEffects.INVISIBILITY, 3),
				new Sweet("fizz_rocks", MobEffects.JUMP_BOOST, 20), new Sweet("witchs_licorice", MobEffects.NIGHT_VISION, 45))) {
			ItemStack stack = new ItemStack(item(sweet.id()));
			helper.assertTrue(stack.get(DataComponents.FOOD) != null && stack.get(DataComponents.FOOD).canAlwaysEat(), sweet.id() + " is eaten when full");
			stack.finishUsingItem(helper.getLevel(), player);
			helper.assertTrue(player.hasEffect(sweet.effect()) && player.getEffect(sweet.effect()).getDuration() == sweet.seconds() * 20,
					sweet.id() + " gives " + sweet.seconds() + " s of its effect");
		}
		helper.succeed();
	}

	/** Every recipe of the festivities loads, the Peddler's trades and their tag load, and nothing sells emeralds. */
	@GameTest
	public void festivityDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("judging_stand", "spun_cobweb", "hanging_ghost", "candle_skull", "rounded_gravestone_from_stonecutting",
				"cross_gravestone_from_stonecutting", "obelisk_gravestone_from_stonecutting", "pot_cooking/glow_gum", "pot_cooking/ghost_taffy",
				"pot_cooking/fizz_rocks", "pot_cooking/witchs_licorice")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), "Recipe " + id + " loads");
		}
		TradeSet set = level.registryAccess().lookupOrThrow(Registries.TRADE_SET).getOptional(HalloweenPeddler.TRADES).orElse(null);
		helper.assertTrue(set != null && set.trades().size() == 15, "All fifteen Peddler trades load");
		helper.succeed();
	}
}
