package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.Candies;
import io.github.jimbozoomer.jugcraft.agriculture.CandyBase;
import io.github.jimbozoomer.jugcraft.agriculture.CandyBatch;
import io.github.jimbozoomer.jugcraft.agriculture.CandyFlavour;
import io.github.jimbozoomer.jugcraft.agriculture.CandyKettleBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CandyKind;
import io.github.jimbozoomer.jugcraft.agriculture.CandyStage;
import io.github.jimbozoomer.jugcraft.agriculture.CandyTrayItem;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the candy kitchen: filling a Candy Kettle (a base, sugar, two flavours, dyes, nothing once it nears the
 * boil, tipping it out), heating over a fire (not without a batch) and cooling off it while keeping its stage, pouring at
 * each stage onto a Candy Tray (the base and the stage decide the candy), trays setting and breaking up (lollipops with
 * sticks, rock candy's day), pulling taffy while it is warm (or it sets hard), candy corn in coloured layers, flavoured
 * candy's names and effects, comparators, and the data.
 */
public class CandyGameTests {
	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	/** A vanilla dye (26.3 has no Items field for each). */
	private static Item dye(String color) {
		return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", color + "_dye"));
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

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, ItemStack held) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	private static void use(GameTestHelper helper, ServerPlayer player, BlockPos pos, ItemStack held) {
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		BlockPos absolute = helper.absolutePos(pos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
		player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	private static ItemStack water() {
		return PotionContents.createItemStack(Items.POTION, Potions.WATER);
	}

	private static CandyKettleBlockEntity kettle(GameTestHelper helper, BlockPos pos, boolean fire) {
		if (fire) {
			helper.setBlock(pos.below(), Blocks.CAMPFIRE);
		}
		helper.setBlock(pos, block("candy_kettle"));
		return helper.getBlockEntity(pos, CandyKettleBlockEntity.class);
	}

	/** A kettle with a base and {@code sugar} sugar, filled by {@code player}. */
	private static CandyKettleBlockEntity batch(GameTestHelper helper, ServerPlayer player, BlockPos pos, boolean cream, int sugar) {
		CandyKettleBlockEntity kettle = kettle(helper, pos, false);
		use(helper, player, pos, cream ? new ItemStack(Items.MILK_BUCKET) : water());
		use(helper, player, pos, new ItemStack(Items.SUGAR, sugar));
		for (int i = 1; i < sugar; i++) {
			use(helper, player, pos, player.getMainHandItem());
		}
		return kettle;
	}

	private static long now(GameTestHelper helper) {
		return helper.getLevel().getGameTime();
	}

	private static boolean earned(ServerPlayer player, String id) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Jugcraft.id(id));
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	private static ItemStack tray(CandyBatch batch) {
		ItemStack tray = new ItemStack(item("candy_tray"));
		CandyTrayItem.fill(tray, batch);
		return tray;
	}

	/** Uses the tray in {@code player}'s main hand (in the air). */
	private static void useTray(GameTestHelper helper, ServerPlayer player) {
		player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
	}

	// ---------------------------------------------------------------- filling

	/**
	 * A water bottle sets the base (and leaves its bottle); milk then is refused. Sugar goes in up to four; two flavours
	 * at most, each once; dyes mix. Near the boil nothing more goes in. Sneaking with an empty hand tips it all out.
	 */
	@GameTest(maxTicks = 20)
	public void aKettleTakesItsBatch(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		CandyKettleBlockEntity kettle = kettle(helper, pos, false);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 5), ItemStack.EMPTY);
		use(helper, player, pos, water());
		helper.assertTrue(kettle.base() == CandyBase.SYRUP && player.getMainHandItem().is(Items.GLASS_BOTTLE), "A water bottle sets syrup, leaving the bottle");
		use(helper, player, pos, new ItemStack(Items.MILK_BUCKET));
		helper.assertTrue(kettle.base() == CandyBase.SYRUP && player.getMainHandItem().is(Items.MILK_BUCKET), "One base a batch: the milk stays in its bucket");
		ItemStack sugar = new ItemStack(Items.SUGAR, 8);
		for (int i = 0; i < 5; i++) {
			use(helper, player, pos, sugar);
		}
		helper.assertTrue(kettle.sugar() == CandyKettleBlockEntity.MAX_SUGAR && sugar.getCount() == 8 - CandyKettleBlockEntity.MAX_SUGAR,
				"Sugar goes in four at most: " + kettle.sugar());
		use(helper, player, pos, new ItemStack(Items.COCOA_BEANS, 2));
		use(helper, player, pos, new ItemStack(Items.COCOA_BEANS, 2));
		use(helper, player, pos, new ItemStack(Items.SWEET_BERRIES));
		use(helper, player, pos, new ItemStack(Items.GLOW_BERRIES));
		helper.assertTrue(kettle.flavours().equals(List.of(CandyFlavour.CHOCOLATE, CandyFlavour.BERRY)),
				"Two flavours, each once: " + kettle.flavours());
		helper.assertTrue(kettle.color() == CandyFlavour.CHOCOLATE.color, "Undyed, it takes its first flavour's colour");
		use(helper, player, pos, new ItemStack(dye("red")));
		helper.assertTrue(kettle.dyed() && kettle.color() != CandyFlavour.CHOCOLATE.color, "A dye colours it");
		kettle.setTemperature(CandyKettleBlockEntity.ADD_BELOW);
		helper.assertTrue(!kettle.cool(), "At the boil it is no longer cool");
		ItemStack late = new ItemStack(dye("yellow"));
		int color = kettle.color();
		use(helper, player, pos, late);
		helper.assertTrue(late.getCount() == 1 && kettle.color() == color, "Nothing more goes in");
		player.setShiftKeyDown(true);
		use(helper, player, pos, ItemStack.EMPTY);
		player.setShiftKeyDown(false);
		helper.assertTrue(kettle.empty(), "Sneaking with an empty hand tips it out");
		helper.succeed();
	}

	// ---------------------------------------------------------------- heat

	/**
	 * Over a fire a batch heats, a degree every four ticks below the boil; a kettle with sugar but no base does not. Off
	 * the heat a batch cools, but keeps the stage it reached; comparators read that stage.
	 */
	@GameTest(maxTicks = 200)
	public void itHeatsOverAFireAndKeepsItsStage(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), ItemStack.EMPTY);
		BlockPos hotPos = new BlockPos(1, 2, 1);
		BlockPos dryPos = new BlockPos(3, 2, 1);
		BlockPos coolingPos = new BlockPos(5, 2, 1);
		CandyKettleBlockEntity hot = batch(helper, player, hotPos, false, 2);
		helper.setBlock(hotPos.below(), Blocks.CAMPFIRE);
		helper.setBlock(dryPos.below(), Blocks.CAMPFIRE);
		CandyKettleBlockEntity dry = kettle(helper, dryPos, false);
		use(helper, player, dryPos, new ItemStack(Items.SUGAR));
		CandyKettleBlockEntity cooling = batch(helper, player, coolingPos, true, 1);
		cooling.setTemperature(150);
		helper.assertTrue(CandyKettleBlockEntity.ticksPerDegree(50) == CandyKettleBlockEntity.HEAT_TICKS
				&& CandyKettleBlockEntity.ticksPerDegree(105) == CandyKettleBlockEntity.BOIL_TICKS
				&& CandyKettleBlockEntity.ticksPerDegree(130) == CandyKettleBlockEntity.COOK_TICKS, "Slow while its water boils off");
		helper.succeedWhen(() -> {
			helper.assertTrue(hot.temperature() >= CandyKettleBlockEntity.ROOM + 20, "Over the fire it heats: " + hot.temperature());
			helper.assertTrue(dry.temperature() == CandyKettleBlockEntity.ROOM, "Sugar without water or milk doesn't heat");
			helper.assertTrue(cooling.temperature() < 150 && cooling.cooked() == 150 && cooling.stage() == CandyStage.HARD_CRACK,
					"Off the heat it cools, keeping its stage: " + cooling.temperature());
			helper.assertTrue(cooling.makes() == CandyKind.TOFFEE, "Cream at hard crack makes toffee");
			BlockPos absolute = helper.absolutePos(coolingPos);
			helper.assertTrue(helper.getLevel().getBlockState(absolute).getAnalogOutputSignal(helper.getLevel(), absolute, Direction.NORTH)
					== CandyStage.HARD_CRACK.ordinal(), "Comparators read the stage");
		});
	}

	// ---------------------------------------------------------------- pouring

	/**
	 * The base and the stage decide the candy: syrup at soft ball pours candy corn, cream at soft ball fudge; cream at thread
	 * is too runny to pour; syrup past 175 is burnt. Pouring fills one tray (two pieces a sugar, its flavours and colour),
	 * empties the kettle and earns Sweet Science.
	 */
	@GameTest(maxTicks = 20)
	public void theStageDecidesTheCandy(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), ItemStack.EMPTY);
		helper.assertTrue(CandyStage.at(117) == CandyStage.SOFT_BALL && CandyStage.at(109) == CandyStage.SYRUP && CandyStage.at(180) == CandyStage.BURNT,
				"Stages by temperature");
		helper.assertTrue(CandyBase.SYRUP.makes(CandyStage.THREAD) == CandyKind.ROCK_CANDY && CandyBase.SYRUP.makes(CandyStage.HARD_BALL) == CandyKind.SALT_WATER_TAFFY
				&& CandyBase.SYRUP.makes(CandyStage.HARD_CRACK) == CandyKind.HARD_CANDY && CandyBase.SYRUP.makes(CandyStage.CARAMEL) == CandyKind.CARAMEL
				&& CandyBase.CREAM.makes(CandyStage.FIRM_BALL) == CandyKind.CREAM_CARAMEL && CandyBase.CREAM.makes(CandyStage.THREAD) == null,
				"Each base sets into its own candies");

		BlockPos syrupPos = new BlockPos(1, 2, 1);
		CandyKettleBlockEntity syrup = batch(helper, player, syrupPos, false, 3);
		use(helper, player, syrupPos, new ItemStack(Items.COCOA_BEANS));
		syrup.setTemperature(117);
		use(helper, player, syrupPos, new ItemStack(item("candy_tray"), 2));
		CandyBatch batch = null;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			CandyBatch found = CandyTrayItem.batch(player.getInventory().getItem(slot));
			if (found != null) {
				batch = found;
			}
		}
		helper.assertTrue(batch != null && batch.kind() == CandyKind.CANDY_CORN && batch.pieces() == 3 * CandyKettleBlockEntity.PIECES_PER_SUGAR
				&& batch.flavours().equals(List.of(CandyFlavour.CHOCOLATE)) && batch.colors().equals(List.of(CandyFlavour.CHOCOLATE.color)),
				"Syrup at soft ball pours chocolate candy corn, six pieces: " + batch);
		helper.assertTrue(player.getMainHandItem().is(item("candy_tray")) && player.getMainHandItem().getCount() == 1
				&& CandyTrayItem.batch(player.getMainHandItem()) == null, "One tray of the two is filled");
		helper.assertTrue(syrup.empty() && earned(player, "candy_maker"), "The kettle is empty, and Sweet Science is earned");

		BlockPos creamPos = new BlockPos(3, 2, 1);
		CandyKettleBlockEntity cream = batch(helper, player, creamPos, true, 1);
		cream.setTemperature(112);
		ItemStack runny = new ItemStack(item("candy_tray"));
		use(helper, player, creamPos, runny);
		helper.assertTrue(CandyTrayItem.batch(player.getMainHandItem()) == null && !cream.empty(), "Cream at thread is too runny to pour");
		cream.setTemperature(116);
		use(helper, player, creamPos, player.getMainHandItem());
		CandyBatch fudge = CandyTrayItem.batch(player.getMainHandItem());
		helper.assertTrue(fudge != null && fudge.kind() == CandyKind.FUDGE && fudge.colors().equals(List.of(Candies.NATURAL)),
				"Cream at soft ball pours plain fudge: " + fudge);

		BlockPos burntPos = new BlockPos(5, 2, 1);
		CandyKettleBlockEntity burnt = batch(helper, player, burntPos, false, 1);
		burnt.setTemperature(180);
		use(helper, player, burntPos, new ItemStack(item("candy_tray")));
		CandyBatch black = CandyTrayItem.batch(player.getMainHandItem());
		helper.assertTrue(black != null && black.kind() == CandyKind.BURNT_SUGAR, "Past 175 it burns");
		helper.succeed();
	}

	// ---------------------------------------------------------------- trays

	/**
	 * A tray breaks up once set: fudge into its pieces (named and flavoured), leaving the tray; not while still setting.
	 * Hard candy with sticks in the other hand makes a lollipop a stick. Rock candy takes a day to grow.
	 */
	@GameTest(maxTicks = 20)
	public void traysSetAndBreakUp(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 3), ItemStack.EMPTY);
		long now = now(helper);
		player.setItemInHand(InteractionHand.MAIN_HAND, tray(new CandyBatch(CandyKind.FUDGE, List.of(CandyFlavour.CHOCOLATE), List.of(Candies.NATURAL), 6,
				now, 0)));
		useTray(helper, player);
		helper.assertTrue(CandyTrayItem.batch(player.getMainHandItem()) != null, "Fresh fudge is still setting");
		player.setItemInHand(InteractionHand.MAIN_HAND, tray(new CandyBatch(CandyKind.FUDGE, List.of(CandyFlavour.CHOCOLATE), List.of(Candies.NATURAL), 6,
				now - CandyTrayItem.SET_TICKS, 0)));
		useTray(helper, player);
		int fudge = player.getInventory().countItem(item("fudge"));
		helper.assertTrue(fudge == 6 && player.getMainHandItem().is(item("candy_tray")) && CandyTrayItem.batch(player.getMainHandItem()) == null,
				"Set fudge breaks into six pieces, leaving the empty tray: " + fudge);
		player.getInventory().clearContent();

		player.setItemInHand(InteractionHand.MAIN_HAND, tray(new CandyBatch(CandyKind.HARD_CANDY, List.of(), List.of(0x3355FF), 8,
				now - CandyTrayItem.SET_TICKS, 0)));
		player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STICK, 3));
		useTray(helper, player);
		helper.assertTrue(player.getInventory().countItem(item("lollipop")) == 3 && player.getInventory().countItem(item("hard_candy")) == 5
				&& player.getOffhandItem().isEmpty(), "Three sticks make three lollipops; the rest is hard candy");
		player.getInventory().clearContent();

		CandyBatch rock = new CandyBatch(CandyKind.ROCK_CANDY, List.of(), List.of(Candies.NATURAL), 4, now - CandyTrayItem.SET_TICKS, 0);
		helper.assertTrue(!rock.ready(now) && rock.left(now) == CandyTrayItem.CRYSTAL_TICKS - CandyTrayItem.SET_TICKS, "Rock candy is still growing");
		player.setItemInHand(InteractionHand.MAIN_HAND, tray(new CandyBatch(CandyKind.ROCK_CANDY, List.of(), List.of(Candies.NATURAL), 4,
				now - CandyTrayItem.CRYSTAL_TICKS, 0)));
		useTray(helper, player);
		helper.assertTrue(player.getInventory().countItem(item("rock_candy")) == 4, "After a day it breaks up");
		helper.succeed();
	}

	/**
	 * Warm taffy is pulled by holding use: four pulls and it's done (earning Pulling Power), then it cuts into taffy. Left
	 * to go cold unpulled, it sets hard, as hard candy.
	 */
	@GameTest(maxTicks = 20)
	public void taffyMustBePulledWhileWarm(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer player = player(helper, new BlockPos(3, 2, 3), ItemStack.EMPTY);
		long now = now(helper);
		ItemStack warm = tray(new CandyBatch(CandyKind.SALT_WATER_TAFFY, List.of(CandyFlavour.BERRY), List.of(CandyFlavour.BERRY.color), 4, now, 0));
		player.setItemInHand(InteractionHand.MAIN_HAND, warm);
		helper.assertTrue(warm.getUseDuration(player) == CandyTrayItem.PULL_TICKS, "A pull takes a second");
		useTray(helper, player);
		helper.assertTrue(CandyTrayItem.batch(player.getMainHandItem()) != null && player.getInventory().countItem(item("salt_water_taffy")) == 0,
				"Unpulled, it can't be cut");
		for (int i = 0; i < CandyTrayItem.PULLS; i++) {
			warm.finishUsingItem(level, player);
		}
		helper.assertTrue(CandyTrayItem.batch(warm).pulled() && earned(player, "taffy_puller"), "Four pulls, and Pulling Power is earned");
		useTray(helper, player);
		boolean named = false;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack piece = player.getInventory().getItem(slot);
			named |= piece.is(item("salt_water_taffy"))
					&& !piece.get(DataComponents.ITEM_NAME).equals(new ItemStack(item("salt_water_taffy")).get(DataComponents.ITEM_NAME));
		}
		helper.assertTrue(player.getInventory().countItem(item("salt_water_taffy")) == 4 && named, "Pulled taffy cuts into four pieces of berry taffy");
		player.getInventory().clearContent();

		player.setItemInHand(InteractionHand.MAIN_HAND, tray(new CandyBatch(CandyKind.SALT_WATER_TAFFY, List.of(), List.of(Candies.NATURAL), 4,
				now - CandyTrayItem.WARM_TICKS, 1)));
		useTray(helper, player);
		helper.assertTrue(player.getInventory().countItem(item("hard_candy")) == 4 && player.getInventory().countItem(item("salt_water_taffy")) == 0,
				"Gone cold half-pulled, it sets hard, as hard candy");
		helper.succeed();
	}

	/**
	 * Candy corn takes up to three layers, each poured on in its own colour; three undyed layers make plain candy corn (it
	 * stacks with any other), a dyed one keeps its bands.
	 */
	@GameTest(maxTicks = 20)
	public void candyCornIsPouredInLayers(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), ItemStack.EMPTY);
		ItemStack tray = new ItemStack(item("candy_tray"));
		BlockPos[] kettles = {new BlockPos(0, 2, 1), new BlockPos(2, 2, 1), new BlockPos(4, 2, 1), new BlockPos(6, 2, 1)};
		for (int i = 0; i < kettles.length; i++) {
			CandyKettleBlockEntity kettle = batch(helper, player, kettles[i], false, 1);
			if (i == 1) {
				use(helper, player, kettles[i], new ItemStack(dye("lime")));
			}
			kettle.setTemperature(122);
			use(helper, player, kettles[i], tray);
			tray = player.getMainHandItem();
		}
		CandyBatch layered = CandyTrayItem.batch(tray);
		helper.assertTrue(layered != null && layered.colors().size() == CandyTrayItem.MAX_LAYERS && layered.pieces() == 3 * CandyKettleBlockEntity.PIECES_PER_SUGAR,
				"Three layers at most, six pieces: " + layered);
		List<Integer> bands = Candies.bands(layered.colors());
		helper.assertTrue(bands.get(0) == Candies.CORN_BANDS[0] && bands.get(1) != Candies.CORN_BANDS[1] && bands.get(2) == Candies.CORN_BANDS[2],
				"The lime layer is the middle band: " + bands);
		ItemStack dyed = Candies.make(CandyKind.CANDY_CORN, List.of(), layered.colors(), 1);
		CustomModelData model = dyed.get(DataComponents.CUSTOM_MODEL_DATA);
		helper.assertTrue(model != null && model.colors().equals(bands), "The candy corn keeps its bands");
		ItemStack plain = Candies.make(CandyKind.CANDY_CORN, List.of(), List.of(Candies.NATURAL, Candies.NATURAL, Candies.NATURAL), 1);
		helper.assertTrue(ItemStack.isSameItemSameComponents(plain, new ItemStack(item("candy_corn"))), "Three undyed layers: plain candy corn");
		helper.succeed();
	}

	// ---------------------------------------------------------------- candy

	/**
	 * Flavoured candy is named for its flavours and gives their effects when eaten (even on a full stomach); burnt sugar
	 * keeps no flavour. The candies count as candy, the flavours' items are tagged, and the recipes load.
	 */
	@GameTest(maxTicks = 20)
	public void flavouredCandyGivesItsEffects(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer player = player(helper, new BlockPos(3, 2, 3), ItemStack.EMPTY);
		ItemStack fudge = Candies.make(CandyKind.FUDGE, List.of(CandyFlavour.CHOCOLATE, CandyFlavour.HONEY), List.of(Candies.NATURAL), 2);
		ItemStack plainFudge = new ItemStack(item("fudge"));
		helper.assertTrue(!fudge.get(DataComponents.ITEM_NAME).equals(plainFudge.get(DataComponents.ITEM_NAME))
				&& fudge.get(DataComponents.LORE).lines().size() == 2, "Flavoured fudge is named and says its flavours");
		fudge.finishUsingItem(level, player);
		helper.assertTrue(player.hasEffect(MobEffects.SPEED) && player.hasEffect(MobEffects.ABSORPTION), "Chocolate and honey: Speed and Absorption");
		ItemStack burnt = Candies.make(CandyKind.BURNT_SUGAR, List.of(CandyFlavour.CHOCOLATE), List.of(Candies.NATURAL), 1);
		helper.assertTrue(ItemStack.isSameItemSameComponents(burnt, new ItemStack(item("burnt_sugar"))),
				"Burnt sugar tastes of nothing but burning");
		TagKey<Item> candy = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "foods/candy"));
		for (String id : JugcraftAgriculture.CANDIES) {
			helper.assertTrue(new ItemStack(item(id)).is(candy), id + " counts as candy");
		}
		for (CandyFlavour flavour : CandyFlavour.values()) {
			helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.ITEM).get(flavour.tag).isPresent(), flavour + " has its tag");
		}
		helper.assertTrue(CandyFlavour.of(new ItemStack(Items.COCOA_BEANS)) == CandyFlavour.CHOCOLATE
				&& CandyFlavour.of(new ItemStack(item("cranberries"))) == CandyFlavour.CRANBERRY, "Flavours come from their items");
		for (String recipe : List.of("candy_kettle", "candy_tray")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(recipe))).isPresent(), recipe + " loads");
		}
		helper.succeed();
	}
}
