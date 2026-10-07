package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.AppleLeavesBlock;
import io.github.jimbozoomer.jugcraft.agriculture.BarrelCider;
import io.github.jimbozoomer.jugcraft.agriculture.CiderBarrelBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CiderBarrelBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CiderPressBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotRecipe;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the cider mill: the apple tree (it grows from its sapling; its leaves blossom, ripen and are picked;
 * placed leaves don't fruit), the Cider Press (the hopper fills and refuses a ninth apple; the crank grinds one apple at a
 * time, paced; the screw presses a cheese in four turns, letting out its juice and knocking out the pomace; a bottle draws
 * sweet cider), the Cider Barrel (fresh juice in, its clock restarting; sparkling after a day, aged after three; a
 * fermenting batch takes no fresh juice; broken, it keeps its cider), the drinks, and the data.
 */
public class CiderGameTests {
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

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, ItemStack held) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
		BlockPos absolute = helper.absolutePos(pos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	private static long now(GameTestHelper helper) {
		return helper.getLevel().getGameTime();
	}

	// ---------------------------------------------------------------- the apple tree

	/**
	 * An apple sapling grows into an apple tree: an oak trunk under apple leaves (worldgen/feature/apple_tree.json). A tree
	 * needs more free space above it than the test area has, so the sapling goes on top of whatever closes the area above.
	 */
	@GameTest
	public void appleSaplingsGrowTrees(GameTestHelper helper) {
		BlockPos sapling = new BlockPos(3, 2, 3);
		for (int dy = 0; dy <= 24; dy++) {
			if (!helper.getBlockState(sapling.above(dy)).isAir()) {
				sapling = sapling.above(dy + 1);
				break;
			}
		}
		helper.setBlock(sapling.below(), Blocks.DIRT);
		helper.setBlock(sapling, block("apple_sapling"));
		ServerLevel level = helper.getLevel();
		BlockPos absolute = helper.absolutePos(sapling);
		boolean grown = JugcraftAgriculture.APPLE_GROWER.growTree(level, level.getChunkSource().getGenerator(), absolute,
				level.getBlockState(absolute), level.getRandom());
		helper.assertTrue(grown, "The apple sapling at " + sapling + " should grow into a tree");
		helper.assertBlockPresent(Blocks.OAK_LOG, sapling);
		int leaves = 0;
		for (BlockPos pos : BlockPos.betweenClosed(absolute.offset(-3, 0, -3), absolute.offset(3, 10, 3))) {
			if (level.getBlockState(pos).is(block("apple_leaves"))) {
				leaves++;
			}
		}
		helper.assertTrue(leaves >= 12, "Expected a crown of apple leaves, found " + leaves);
		helper.succeed();
	}

	/** Tree-grown leaves over air blossom and ripen, and a right-click picks the apples; placed leaves never fruit. */
	@GameTest(maxTicks = 40)
	public void appleLeavesFruitAndArePicked(GameTestHelper helper) {
		BlockPos log = new BlockPos(2, 4, 2);
		BlockPos natural = log.east();
		BlockPos placed = log.west();
		helper.setBlock(log, Blocks.OAK_LOG);
		BlockState leaves = block("apple_leaves").defaultBlockState().setValue(LeavesBlock.DISTANCE, 1);
		helper.setBlock(natural, leaves.setValue(LeavesBlock.PERSISTENT, false));
		helper.setBlock(placed, leaves.setValue(LeavesBlock.PERSISTENT, true));
		ServerLevel level = helper.getLevel();
		boolean blossomed = false;
		for (int i = 0; i < 400; i++) {
			for (BlockPos pos : new BlockPos[] {natural, placed}) {
				helper.getBlockState(pos).randomTick(level, helper.absolutePos(pos), level.getRandom());
			}
			blossomed |= helper.getBlockState(natural).getValue(AppleLeavesBlock.FRUIT) == 1;
		}
		helper.assertTrue(blossomed, "Tree leaves should blossom on the way");
		helper.assertTrue(helper.getBlockState(natural).getValue(AppleLeavesBlock.FRUIT) == AppleLeavesBlock.RIPE,
				"Tree leaves should ripen apples, found " + helper.getBlockState(natural));
		helper.assertTrue(helper.getBlockState(placed).getValue(AppleLeavesBlock.FRUIT) == 0, "Placed leaves should not fruit");
		helper.useBlock(natural, helper.makeMockPlayer(GameType.SURVIVAL));
		helper.assertTrue(helper.getBlockState(natural).getValue(AppleLeavesBlock.FRUIT) == 0, "Picking should leave bare leaves");
		helper.succeedWhen(() -> helper.assertItemEntityPresent(Items.APPLE, natural, 3.0));
	}

	// ---------------------------------------------------------------- the press

	/** Apples go in one at a time up to eight; the crank grinds one apple at a time, and no faster than the press allows. */
	@GameTest(maxTicks = 40)
	public void pressGrindsApplesIntoPulp(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(2, 2, 2);
		helper.setBlock(pos, block("cider_press"));
		CiderPressBlockEntity press = helper.getBlockEntity(pos, CiderPressBlockEntity.class);
		ServerPlayer player = player(helper, new BlockPos(4, 2, 2), new ItemStack(Items.APPLE, 9));
		for (int i = 0; i < 9; i++) {
			use(helper, player, pos);
		}
		helper.assertTrue(press.apples() == CiderPressBlockEntity.CAPACITY && player.getMainHandItem().getCount() == 1,
				"Eight apples go in, not a ninth");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, pos);
		use(helper, player, pos);
		helper.assertTrue(press.apples() == 7 && press.pulp() == 1, "One turn of the crank grinds one apple; a second at once does nothing");
		helper.runAfterDelay(CiderPressBlockEntity.WORK_TICKS + 1, () -> {
			use(helper, player, pos);
			helper.assertTrue(press.apples() == 6 && press.pulp() == 2, "Once the crank is free again it grinds another");
			helper.succeed();
		});
	}

	/**
	 * Four turns of the screw press a full cheese: each lets out a quarter of the juice (a serving an apple); the last
	 * knocks out the pomace (one for two apples). No apples go in while pressing. A bottle draws Sweet Cider.
	 */
	@GameTest(maxTicks = 100)
	public void pressingLetsOutJuiceAndPomace(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(2, 2, 2);
		helper.setBlock(pos, block("cider_press"));
		CiderPressBlockEntity press = helper.getBlockEntity(pos, CiderPressBlockEntity.class);
		long start = now(helper) - 1000;
		for (int i = 0; i < CiderPressBlockEntity.CAPACITY; i++) {
			press.addApple();
			press.grind(start + i * CiderPressBlockEntity.WORK_TICKS);
		}
		ServerPlayer player = player(helper, new BlockPos(4, 2, 2), ItemStack.EMPTY);
		use(helper, player, pos);
		helper.assertTrue(press.turns() == 1 && press.juice() == 2, "The first turn lets out a quarter of the juice");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.APPLE));
		use(helper, player, pos);
		helper.assertTrue(press.apples() == 0 && player.getMainHandItem().getCount() == 1, "No apples go in while pressing");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		int step = CiderPressBlockEntity.WORK_TICKS + 1;
		helper.runAfterDelay(step, () -> use(helper, player, pos));
		helper.runAfterDelay(2 * step, () -> use(helper, player, pos));
		helper.runAfterDelay(3 * step, () -> {
			use(helper, player, pos);
			helper.assertTrue(press.juice() == CiderPressBlockEntity.CAPACITY && press.pulp() == 0 && press.turns() == 0,
					"Four turns press out all the juice and free the screw, found " + press.juice() + " juice");
			AABB around = new AABB(helper.absolutePos(pos)).inflate(2.0);
			int pomace = helper.getLevel().getEntitiesOfClass(ItemEntity.class, around, e -> e.getItem().is(item("apple_pomace"))).stream()
					.mapToInt(e -> e.getItem().getCount()).sum();
			helper.assertTrue(pomace == CiderPressBlockEntity.CAPACITY / CiderPressBlockEntity.APPLES_PER_POMACE,
					"The last turn knocks out four pomace, found " + pomace);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 2));
			use(helper, player, pos);
			helper.assertTrue(press.juice() == CiderPressBlockEntity.CAPACITY - 1 && player.getInventory().countItem(item("sweet_cider")) == 1,
					"A bottle draws a serving of sweet cider");
			helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(pos)).getAnalogOutputSignal(helper.getLevel(),
					helper.absolutePos(pos), Direction.NORTH) > 0, "Comparators read the juice");
			helper.succeed();
		});
	}

	/** A cheese of three apples lets out exactly three servings over its four turns and two pomace. */
	@GameTest
	public void smallCheesesPressEvenly(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(2, 2, 2);
		helper.setBlock(pos, block("cider_press"));
		CiderPressBlockEntity press = helper.getBlockEntity(pos, CiderPressBlockEntity.class);
		long time = now(helper) - 1000;
		for (int i = 0; i < 3; i++) {
			press.addApple();
			press.grind(time);
		}
		int pomace = 0;
		for (int turn = 0; turn < CiderPressBlockEntity.TURNS; turn++) {
			pomace += press.turn(time + turn * CiderPressBlockEntity.WORK_TICKS);
		}
		helper.assertTrue(press.juice() == 3 && pomace == 2, "Three apples give three servings and two pomace, got " + press.juice() + " and " + pomace);
		helper.succeed();
	}

	// ---------------------------------------------------------------- the barrel

	/**
	 * Sweet cider goes in by the bottle (the bottles come back); after a day the batch is sparkling and takes no fresh juice;
	 * a bottle draws sparkling cider; after three days it is aged; the chalk mark follows; broken, it keeps its cider.
	 */
	@GameTest(maxTicks = 40)
	public void barrelAgesCider(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos pos = new BlockPos(2, 2, 2);
		helper.setBlock(pos, block("cider_barrel"));
		CiderBarrelBlockEntity barrel = helper.getBlockEntity(pos, CiderBarrelBlockEntity.class);
		ServerPlayer player = player(helper, new BlockPos(4, 2, 2), new ItemStack(item("sweet_cider"), 4));
		for (int i = 0; i < 3; i++) {
			use(helper, player, pos);
		}
		helper.assertTrue(barrel.servings() == 3 && player.getInventory().countItem(Items.GLASS_BOTTLE) == 3, "Three servings go in; the bottles come back");
		helper.assertTrue(barrel.stage(now(helper)) == 0 && helper.getBlockState(pos).getValue(CiderBarrelBlock.CIDER) == 1, "It starts sweet");
		barrel.setStarted(now(helper) - CiderBarrelBlockEntity.SPARKLING_TICKS);
		helper.assertTrue(helper.getBlockState(pos).getValue(CiderBarrelBlock.CIDER) == 2, "After a day it is sparkling");
		use(helper, player, pos);
		helper.assertTrue(barrel.servings() == 3 && player.getInventory().countItem(item("sweet_cider")) == 1, "A fermenting batch takes no fresh juice");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
		use(helper, player, pos);
		helper.assertTrue(barrel.servings() == 2 && player.getInventory().countItem(item("sparkling_cider")) == 1, "A bottle draws sparkling cider");
		barrel.setStarted(now(helper) - CiderBarrelBlockEntity.AGED_TICKS);
		helper.assertTrue(barrel.stage(now(helper)) == 2 && helper.getBlockState(pos).getValue(CiderBarrelBlock.CIDER) == 3, "After three days it is aged");
		long started = barrel.started();
		level.destroyBlock(helper.absolutePos(pos), true);
		AABB area = new AABB(helper.absolutePos(pos)).inflate(2.0);
		List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, area, e -> e.getItem().is(item("cider_barrel")));
		BarrelCider kept = drops.isEmpty() ? null : drops.get(0).getItem().get(JugcraftAgriculture.BARREL_CIDER);
		helper.assertTrue(kept != null && kept.servings() == 2 && kept.started() == started, "Broken, it keeps its cider and its age");
		helper.succeed();
	}

	/** Topping up a sweet batch starts its clock again, so fresh juice can't be slipped into an older batch to age it. */
	@GameTest
	public void toppingUpRestartsTheClock(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(2, 2, 2);
		helper.setBlock(pos, block("cider_barrel"));
		CiderBarrelBlockEntity barrel = helper.getBlockEntity(pos, CiderBarrelBlockEntity.class);
		barrel.fill(now(helper));
		barrel.setStarted(now(helper) - CiderBarrelBlockEntity.SPARKLING_TICKS + 100);
		helper.assertTrue(barrel.canFill(now(helper)), "A batch still sweet takes more juice");
		barrel.fill(now(helper));
		helper.assertTrue(barrel.untilNext(now(helper)) == CiderBarrelBlockEntity.SPARKLING_TICKS, "Topping up starts the day again");
		helper.succeed();
	}

	// ---------------------------------------------------------------- drinks and data

	/** Every cider is drunk even when full, gives its effect and leaves its bottle; sweet cider gives its bottle back in a recipe. */
	@GameTest(maxTicks = 40)
	public void cidersAreDrinks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(2, 2, 2), ItemStack.EMPTY);
		List<String> ciders = List.of("sweet_cider", "sparkling_cider", "aged_cider", "mulled_cider");
		for (String cider : ciders) {
			FoodProperties food = new ItemStack(item(cider)).get(DataComponents.FOOD);
			helper.assertTrue(food != null && food.canAlwaysEat(), cider + " can be drunk on a full stomach");
			helper.assertTrue(new ItemStack(item(cider)).finishUsingItem(level, player).is(Items.GLASS_BOTTLE), cider + " leaves its bottle");
		}
		helper.assertTrue(player.hasEffect(MobEffects.HASTE) && player.hasEffect(MobEffects.JUMP_BOOST) && player.hasEffect(MobEffects.ABSORPTION)
				&& player.hasEffect(MobEffects.REGENERATION), "The four ciders give Haste, Jump Boost, Absorption and Regeneration");
		helper.assertTrue(item("sweet_cider").getCraftingRemainder() != null, "Sweet cider gives its bottle back in a recipe");
		helper.assertTrue(item("sparkling_cider").getCraftingRemainder() == null,
				"Sparkling cider keeps its bottle in the mulled cider (the Cooking Pot would hand back a second)");
		helper.succeed();
	}

	/** The recipes (mulled cider in the Cooking Pot), the loot tables and the apple tree's placed feature load. */
	@GameTest
	public void ciderDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("cider_press", "cider_barrel", "mulling_spices", "apple_seeds", "apple_cider_donut", "pot_cooking/mulled_cider")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), "Recipe " + id + " loads");
		}
		for (String table : List.of("blocks/cider_press", "blocks/cider_barrel", "blocks/apple_leaves", "blocks/apple_sapling")) {
			helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id(table)))
					!= LootTable.EMPTY, "Loot table " + table + " loads");
		}
		List<ItemStack> slots = new ArrayList<>(List.of(new ItemStack(item("sparkling_cider")), new ItemStack(item("mulling_spices"))));
		while (slots.size() < CookingPotBlockEntity.INPUTS) {
			slots.add(ItemStack.EMPTY);
		}
		Optional<CookingPotRecipe.Match> mulled = CookingPotRecipe.find(level.getServer(), slots);
		helper.assertTrue(mulled.isPresent() && mulled.get().recipe().output().create().is(item("mulled_cider")),
				"Sparkling cider and spices cook into mulled cider");
		ResourceKey<PlacedFeature> patch = ResourceKey.create(Registries.PLACED_FEATURE, Jugcraft.id("patch_apple_tree"));
		helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).get(patch).isPresent(), "Wild apple trees are placed");
		helper.succeed();
	}
}
