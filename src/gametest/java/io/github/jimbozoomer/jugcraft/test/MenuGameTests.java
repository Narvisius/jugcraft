package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotRecipe;
import io.github.jimbozoomer.jugcraft.agriculture.CuttingBoardBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.FeastBlock;
import io.github.jimbozoomer.jugcraft.agriculture.FeastDish;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.MenuDishes;
import io.github.jimbozoomer.jugcraft.agriculture.PetFoodItem;
import io.github.jimbozoomer.jugcraft.agriculture.PlacedDishBlock;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the menu (the kitchen and cooking expansion's slice 3, tools/menu.py): every dish set down by a
 * sneaking player as its own model, facing them, and taken back with an empty hand; the Cooking Pot cooking the new
 * dishes; the Cutting Board making pasta and tortilla chips; corn on the cob giving its cob back; milk in a bottle
 * clearing effects; dog food and horse feed for their owner's tamed pets only; and the nachos served like a feast.
 */
public class MenuGameTests {
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

	/** A survival player standing at {@code standAt}, looking north, holding {@code held}. */
	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, ItemStack held) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		player.setYRot(180.0F);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	/** Uses what the player holds on the top of {@code pos}. */
	private static InteractionResult useTop(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
		BlockPos absolute = helper.absolutePos(pos);
		BlockHitResult hit = new BlockHitResult(new Vec3(absolute.getX() + 0.5, absolute.getY() + 1.0, absolute.getZ() + 0.5), Direction.UP,
				absolute, false);
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	/** Uses what the player holds on {@code pos} itself (a set-down dish, a cutting board). */
	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
		BlockPos absolute = helper.absolutePos(pos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	private static int dropped(GameTestHelper helper, Item item) {
		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 8, 8).inflate(2.0);
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item)).stream()
				.mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	private static int found(GameTestHelper helper, ServerPlayer player, Item item) {
		return dropped(helper, item) + player.getInventory().countItem(item);
	}

	// ---------------------------------------------------------------- setting dishes down

	/**
	 * Every dish can be set down, a block named as its food. Not sneaking, a player eats rather than sets down; sneaking,
	 * they set a Beef Stew down facing them, and it is used up. Holding something, using the dish leaves it; an empty
	 * hand takes it back. Popcorn set down is the popcorn box; broken, a set-down dish drops its food.
	 */
	@GameTest(maxTicks = 20)
	public void dishesSetDownAndTakenBack(GameTestHelper helper) {
		floor(helper);
		for (MenuDishes.Dish dish : MenuDishes.PLACED) {
			helper.assertTrue(block(dish.id()) instanceof PlacedDishBlock placed && placed.dish() == item(dish.id())
					&& placed.dishShape() == dish.shape(), dish.id() + " sets down as itself");
		}
		BlockPos ground = new BlockPos(3, 1, 3);
		BlockPos dish = ground.above();
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("beef_stew")));
		useTop(helper, player, ground);
		helper.assertTrue(helper.getBlockState(dish).isAir() && player.getMainHandItem().is(item("beef_stew")),
				"Not sneaking, the stew is not set down");
		player.setShiftKeyDown(true);
		useTop(helper, player, ground);
		player.setShiftKeyDown(false);
		helper.assertTrue(helper.getBlockState(dish).is(block("beef_stew")) && player.getMainHandItem().isEmpty()
				&& helper.getBlockState(dish).getValue(PlacedDishBlock.FACING) == Direction.SOUTH,
				"Sneaking, the stew is set down facing the player, and used up");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD));
		use(helper, player, dish);
		helper.assertTrue(helper.getBlockState(dish).is(block("beef_stew")), "Holding bread, the stew stays");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, dish);
		helper.assertTrue(helper.getBlockState(dish).isAir() && player.getMainHandItem().is(item("beef_stew")), "An empty hand takes it back");

		BlockPos popcornGround = new BlockPos(5, 1, 3);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("popcorn"), 3));
		player.setShiftKeyDown(true);
		useTop(helper, player, popcornGround);
		player.setShiftKeyDown(false);
		helper.assertTrue(helper.getBlockState(popcornGround.above()).is(block("popcorn")) && player.getMainHandItem().getCount() == 2
				&& ((PlacedDishBlock) block("popcorn")).dishShape() == PlacedDishBlock.DishShape.BOX,
				"Popcorn set down is the popcorn box, one popcorn used");
		helper.getLevel().destroyBlock(helper.absolutePos(popcornGround.above()), true);
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(dropped(helper, item("popcorn")) == 1, "Broken, the box drops its popcorn");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- cooking

	/** The Cooking Pot knows the menu's dishes: a beef stew, hot cocoa, dumplings and boiled corn each from their ingredients. */
	@GameTest(maxTicks = 20)
	public void potKnowsTheMenu(GameTestHelper helper) {
		Object[][] recipes = {
				{"beef_stew", new Item[] {Items.BOWL, item("minced_beef"), item("minced_beef"), Items.POTATO, Items.CARROT, item("onion")}},
				{"hot_cocoa", new Item[] {Items.GLASS_BOTTLE, Items.COCOA_BEANS, Items.COCOA_BEANS, Items.MILK_BUCKET, Items.SUGAR}},
				{"dumplings", new Item[] {item("wheat_dough"), item("minced_beef"), item("cabbage_leaf"), item("onion")}},
				{"boiled_corn", new Item[] {item("corn")}}};
		for (Object[] recipe : recipes) {
			List<ItemStack> slots = new ArrayList<>();
			for (Item ingredient : (Item[]) recipe[1]) {
				slots.add(new ItemStack(ingredient));
			}
			while (slots.size() < CookingPotBlockEntity.INPUTS) {
				slots.add(ItemStack.EMPTY);
			}
			Optional<CookingPotRecipe.Match> match = CookingPotRecipe.find(helper.getLevel().getServer(), slots);
			helper.assertTrue(match.isPresent() && match.get().recipe().output().create().is(item((String) recipe[0])),
					"Ingredients for " + recipe[0] + " found " + match.map(m -> m.recipe().output().create()));
		}
		helper.succeed();
	}

	/** On a lit campfire the pot boils corn on the cob, an ear at a time. */
	@GameTest(maxTicks = 600)
	public void potBoilsCorn(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 2, 2);
		helper.setBlock(pos.below(), Blocks.CAMPFIRE);
		helper.setBlock(pos, block("cooking_pot"));
		CookingPotBlockEntity pot = helper.getBlockEntity(pos, CookingPotBlockEntity.class);
		pot.setItem(0, new ItemStack(item("corn"), 2));
		helper.succeedWhen(() -> {
			int boiled = 0;
			for (int slot = CookingPotBlockEntity.RESULT; slot < CookingPotBlockEntity.SLOTS; slot++) {
				boiled += pot.getItem(slot).is(item("boiled_corn")) ? pot.getItem(slot).getCount() : 0;
			}
			helper.assertTrue(boiled == 2 && pot.getItem(0).isEmpty(), "Expected 2 boiled corn, found " + boiled);
		});
	}

	/** On the Cutting Board a knife cuts a tortilla into two chips and wheat dough into two raw pasta. */
	@GameTest(maxTicks = 20)
	public void boardCutsPastaAndChips(GameTestHelper helper) {
		floor(helper);
		BlockPos board = new BlockPos(3, 2, 3);
		helper.setBlock(board, block("cutting_board"));
		CuttingBoardBlockEntity entity = helper.getBlockEntity(board, CuttingBoardBlockEntity.class);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("tortilla")));
		use(helper, player, board);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("iron_knife")));
		use(helper, player, board);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("wheat_dough")));
		use(helper, player, board);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("iron_knife")));
		use(helper, player, board);
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(entity.item().isEmpty() && found(helper, player, item("tortilla_chip")) == 2,
					"A tortilla cuts into two chips: " + found(helper, player, item("tortilla_chip")));
			helper.assertTrue(found(helper, player, item("raw_pasta")) == 2, "Wheat dough cuts into two raw pasta");
			helper.succeed();
		});
	}

	/** The menu's crafted, smoked and baked dishes have their recipes, and nachos theirs. */
	@GameTest(maxTicks = 20)
	public void menuRecipesExist(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("hamburger", "milk_bottle", "wheat_dough", "tortilla_raw", "cornbread_batter", "dog_food", "horse_feed",
				"melon_juice", "nachos")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), id + " has a recipe");
		}
		helper.succeed();
	}

	// ---------------------------------------------------------------- eating

	/** Boiled and roasted corn leave their corncob when eaten; milk in a bottle clears effects and leaves the bottle. */
	@GameTest(maxTicks = 20)
	public void cobsAndMilk(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(2, 2, 2), ItemStack.EMPTY);
		for (String corn : List.of("boiled_corn", "roasted_corn")) {
			player.getFoodData().setFoodLevel(4);
			ItemStack left = new ItemStack(item(corn)).finishUsingItem(level, player);
			FoodProperties food = new ItemStack(item(corn)).get(DataComponents.FOOD);
			helper.assertTrue(left.is(item("corncob")) && food != null && player.getFoodData().getFoodLevel() == 4 + food.nutrition(),
					corn + " is eaten, leaving its corncob: " + left);
		}
		player.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
		ItemStack bottle = new ItemStack(item("milk_bottle")).finishUsingItem(level, player);
		helper.assertTrue(!player.hasEffect(MobEffects.POISON) && bottle.is(Items.GLASS_BOTTLE), "Milk clears the poison, leaving the bottle");
		helper.assertTrue(new ItemStack(item("milk_bottle")).getMaxStackSize() == 16, "Milk bottles stack to 16");
		helper.succeed();
	}

	/**
	 * Dog Food heals its owner's tamed wolf by 20 and gives it Strength and Speed, leaving the bowl; a stranger, or an
	 * untamed wolf, is left to vanilla. Horse Feed gives its owner's horse Speed and Jump Boost.
	 */
	@GameTest(maxTicks = 20)
	public void petFoodForOwnersPets(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer owner = player(helper, new BlockPos(2, 2, 5), new ItemStack(item("dog_food")));
		ServerPlayer stranger = player(helper, new BlockPos(4, 2, 5), new ItemStack(item("dog_food")));
		var wolf = helper.spawnWithNoFreeWill(EntityTypes.WOLF, new Vec3(2.5, 2, 2.5));
		var wild = helper.spawnWithNoFreeWill(EntityTypes.WOLF, new Vec3(5.5, 2, 2.5));
		wolf.tame(owner);
		wolf.setHealth(10.0F);
		helper.assertTrue(PetFoodItem.feed(stranger, level, InteractionHand.MAIN_HAND, wolf) == InteractionResult.PASS
				&& stranger.getMainHandItem().is(item("dog_food")), "A stranger cannot feed someone else's wolf");
		helper.assertTrue(PetFoodItem.feed(owner, level, InteractionHand.MAIN_HAND, wild) == InteractionResult.PASS,
				"An untamed wolf is left to vanilla");
		InteractionResult fed = PetFoodItem.feed(owner, level, InteractionHand.MAIN_HAND, wolf);
		helper.assertTrue(fed.consumesAction() && Math.abs(wolf.getHealth() - 30.0F) < 0.01F && wolf.hasEffect(MobEffects.STRENGTH)
				&& wolf.hasEffect(MobEffects.SPEED) && owner.getMainHandItem().is(Items.BOWL),
				"The owner's wolf eats: health " + wolf.getHealth() + ", the bowl given back");

		var horse = helper.spawnWithNoFreeWill(EntityTypes.HORSE, new Vec3(2.5, 2, 0.5));
		horse.tameWithName(owner);
		owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("horse_feed"), 2));
		fed = PetFoodItem.feed(owner, level, InteractionHand.MAIN_HAND, horse);
		helper.assertTrue(fed.consumesAction() && horse.hasEffect(MobEffects.SPEED) && horse.hasEffect(MobEffects.JUMP_BOOST)
				&& owner.getMainHandItem().getCount() == 1 && owner.getInventory().countItem(Items.BOWL) == 0,
				"The owner's horse eats its feed, which leaves no bowl");
		helper.succeed();
	}

	// ---------------------------------------------------------------- nachos

	/** Nachos are a feast: a bowl takes a serving away (a Bowl of Nachos, three food), down to the last chip, which a use clears. */
	@GameTest(maxTicks = 20)
	public void nachosServeLikeAFeast(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, block("nachos"));
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(Items.BOWL, FeastBlock.SERVINGS));
		for (int i = 0; i < FeastBlock.SERVINGS; i++) {
			use(helper, player, pos);
		}
		FoodProperties food = new ItemStack(item("bowl_of_nachos")).get(DataComponents.FOOD);
		helper.assertTrue(player.getInventory().countItem(item("bowl_of_nachos")) == FeastBlock.SERVINGS && food != null
				&& food.nutrition() == FeastDish.NACHOS.nutrition && helper.getBlockState(pos).getValue(FeastBlock.SERVINGS_LEFT) == 0,
				"Four bowls take the nachos down to the last chip");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, pos);
		helper.assertTrue(helper.getBlockState(pos).isAir(), "A use clears the last chip");
		helper.succeed();
	}
}
