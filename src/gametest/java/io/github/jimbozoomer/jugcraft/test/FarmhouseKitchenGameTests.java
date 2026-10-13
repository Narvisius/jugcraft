package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CuttingBoardBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.KitchenCabinetBlock;
import io.github.jimbozoomer.jugcraft.agriculture.KitchenCabinetBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.KitchenStoveBlock;
import io.github.jimbozoomer.jugcraft.agriculture.KitchenStoveBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.PieBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SkilletBlockEntity;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the Farmhouse Kitchen (tools/kitchen.py): the Kitchen Stove, the Skillet, the Cutting Board, the
 * kitchen knives and the kitchen cabinets.
 */
public class FarmhouseKitchenGameTests {
	private static final List<String> KNIVES = List.of("flint_knife", "iron_knife", "bronze_knife", "golden_knife", "steel_knife",
			"diamond_knife", "netherite_knife");

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

	/** A survival cook standing at {@code standAt}, holding {@code held}. */
	private static ServerPlayer cook(GameTestHelper helper, BlockPos standAt, ItemStack held) {
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

	private static int dropped(GameTestHelper helper, Item item) {
		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 8, 8).inflate(2.0);
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item)).stream()
				.mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	/** What there is of {@code item} on the ground and in the cook's pockets. */
	private static int found(GameTestHelper helper, ServerPlayer player, Item item) {
		return dropped(helper, item) + player.getInventory().countItem(item);
	}

	private static BlockState litStove() {
		return block("kitchen_stove").defaultBlockState().setValue(KitchenStoveBlock.LIT, true);
	}

	private static boolean heated(GameTestHelper helper, BlockPos pos) {
		return CookingPotBlockEntity.isHeated(helper.getLevel(), helper.absolutePos(pos));
	}

	private static int onHob(KitchenStoveBlockEntity stove) {
		return (int) stove.items().stream().filter(stack -> !stack.isEmpty()).count();
	}

	// ---------------------------------------------------------------- the Kitchen Stove

	/**
	 * Placed, the stove is out: dark and cold. Flint and steel lights it (light 13, and it heats the block on top) and is
	 * worn; a shovel puts it out; a fire charge lights it again and is used up.
	 */
	@GameTest(maxTicks = 40)
	public void stoveLightsAndGoesOut(GameTestHelper helper) {
		floor(helper);
		BlockPos stove = new BlockPos(3, 2, 3);
		helper.setBlock(stove, block("kitchen_stove"));
		ServerPlayer player = cook(helper, new BlockPos(3, 2, 6), new ItemStack(Items.FLINT_AND_STEEL));
		helper.assertTrue(helper.getBlockState(stove).getLightEmission() == 0 && !heated(helper, stove.above()), "Placed, the stove is out");
		use(helper, player, stove, Direction.SOUTH);
		helper.assertTrue(helper.getBlockState(stove).getValue(KitchenStoveBlock.LIT) && helper.getBlockState(stove).getLightEmission() == KitchenStoveBlock.LIGHT
				&& heated(helper, stove.above()) && player.getMainHandItem().getDamageValue() == 1,
				"Flint and steel lights it, so it heats what is on top, and is worn");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SHOVEL));
		use(helper, player, stove, Direction.SOUTH);
		helper.assertTrue(!helper.getBlockState(stove).getValue(KitchenStoveBlock.LIT) && helper.getBlockState(stove).getLightEmission() == 0
				&& !heated(helper, stove.above()), "A shovel puts it out");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FIRE_CHARGE, 2));
		use(helper, player, stove, Direction.SOUTH);
		helper.assertTrue(helper.getBlockState(stove).getValue(KitchenStoveBlock.LIT) && player.getMainHandItem().getCount() == 1,
				"A fire charge lights it too and is used up");
		helper.succeed();
	}

	/** A lit stove heats a Cooking Pot on it, which cooks a tomato soup. */
	@GameTest(maxTicks = 400)
	public void litStoveHeatsTheCookingPot(GameTestHelper helper) {
		floor(helper);
		BlockPos pot = new BlockPos(2, 3, 2);
		helper.setBlock(pot.below(), litStove());
		helper.setBlock(pot, block("cooking_pot"));
		CookingPotBlockEntity entity = helper.getBlockEntity(pot, CookingPotBlockEntity.class);
		entity.setItem(0, new ItemStack(Items.BOWL));
		entity.setItem(1, new ItemStack(item("tomato"), 2));
		entity.setItem(2, new ItemStack(item("onion")));
		helper.succeedWhen(() -> {
			int soups = 0;
			for (int slot = CookingPotBlockEntity.RESULT; slot < CookingPotBlockEntity.SLOTS; slot++) {
				soups += entity.getItem(slot).is(item("tomato_soup")) ? entity.getItem(slot).getCount() : 0;
			}
			helper.assertTrue(soups == 1, "Expected a tomato soup, found " + soups);
		});
	}

	/**
	 * Food used on the top of a lit stove goes on the hob, one at a time, six at most; a stick is not cooked and stays in
	 * hand. In half a campfire's time the food pops off cooked. A stove with a block on top does not cook on its hob.
	 */
	@GameTest(maxTicks = 400)
	public void hobCooksFoodAndPopsItOff(GameTestHelper helper) {
		floor(helper);
		BlockPos stove = new BlockPos(2, 2, 3);
		BlockPos covered = new BlockPos(5, 2, 3);
		helper.setBlock(stove, litStove());
		helper.setBlock(covered, litStove());
		ServerPlayer player = cook(helper, new BlockPos(3, 2, 6), new ItemStack(Items.BEEF, 8));
		for (int i = 0; i <= KitchenStoveBlockEntity.SLOTS; i++) {
			use(helper, player, stove, Direction.UP);
		}
		KitchenStoveBlockEntity hob = helper.getBlockEntity(stove, KitchenStoveBlockEntity.class);
		helper.assertTrue(onHob(hob) == KitchenStoveBlockEntity.SLOTS && player.getMainHandItem().getCount() == 8 - KitchenStoveBlockEntity.SLOTS,
				"The hob takes " + KitchenStoveBlockEntity.SLOTS + " foods one at a time, then is full: " + onHob(hob));
		use(helper, player, covered, Direction.UP);
		helper.setBlock(covered.above(), Blocks.STONE);
		KitchenStoveBlockEntity under = helper.getBlockEntity(covered, KitchenStoveBlockEntity.class);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
		use(helper, player, stove, Direction.UP);
		helper.assertTrue(player.getMainHandItem().is(Items.STICK) && player.getMainHandItem().getCount() == 1 && onHob(hob) == KitchenStoveBlockEntity.SLOTS,
				"A stick is not cooked and stays in hand");
		helper.succeedWhen(() -> {
			helper.assertTrue(dropped(helper, Items.COOKED_BEEF) == KitchenStoveBlockEntity.SLOTS && onHob(hob) == 0,
					"The beef should pop off cooked: " + dropped(helper, Items.COOKED_BEEF));
			helper.assertTrue(onHob(under) == 1, "A stove with a block on top does not cook on its hob");
		});
	}

	/** A pot or a skillet used on the top of the stove is placed there as a block, and is heated. */
	@GameTest(maxTicks = 40)
	public void blocksStillPlaceOnTheStove(GameTestHelper helper) {
		floor(helper);
		BlockPos stove = new BlockPos(2, 2, 3);
		BlockPos other = new BlockPos(5, 2, 3);
		helper.setBlock(stove, litStove());
		helper.setBlock(other, litStove());
		ServerPlayer player = cook(helper, new BlockPos(3, 2, 6), new ItemStack(item("cooking_pot")));
		use(helper, player, stove, Direction.UP);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("skillet")));
		use(helper, player, other, Direction.UP);
		helper.assertTrue(helper.getBlockState(stove.above()).is(block("cooking_pot")) && helper.getBlockState(other.above()).is(block("skillet"))
				&& player.getMainHandItem().isEmpty(), "The pot and the skillet are placed on the stoves");
		helper.assertTrue(heated(helper, stove.above()) && heated(helper, other.above()), "and are heated");
		helper.succeed();
	}

	/** A lit stove burns what stands on it; one that is out does not. */
	@GameTest(maxTicks = 60)
	public void litStoveBurnsWhatStandsOnIt(GameTestHelper helper) {
		floor(helper);
		BlockPos hot = new BlockPos(2, 2, 3);
		BlockPos cold = new BlockPos(5, 2, 3);
		helper.setBlock(hot, litStove());
		helper.setBlock(cold, block("kitchen_stove"));
		Mob burnt = helper.spawn(EntityTypes.PIG, hot.above());
		Mob fine = helper.spawn(EntityTypes.PIG, cold.above());
		helper.succeedWhen(() -> {
			helper.assertTrue(burnt.getHealth() < burnt.getMaxHealth(), "The pig on the lit stove should be burnt");
			helper.assertTrue(fine.getHealth() == fine.getMaxHealth(), "The pig on the cold stove should be unhurt");
		});
	}

	// ---------------------------------------------------------------- the Skillet

	/**
	 * Raw beef goes in a skillet on a campfire, two at once; porkchop does not join it and a stick is refused. It fries a
	 * beef every third of a campfire's time, and an empty hand takes the steaks out. A skillet on stone does not fry.
	 */
	@GameTest(maxTicks = 500)
	public void skilletFriesOnHeat(GameTestHelper helper) {
		floor(helper);
		BlockPos hot = new BlockPos(2, 3, 3);
		BlockPos cold = new BlockPos(5, 3, 3);
		helper.setBlock(hot.below(), Blocks.CAMPFIRE);
		helper.setBlock(cold.below(), Blocks.STONE);
		helper.setBlock(hot, block("skillet"));
		helper.setBlock(cold, block("skillet"));
		ServerPlayer player = cook(helper, new BlockPos(3, 2, 6), new ItemStack(Items.BEEF, 2));
		use(helper, player, hot, Direction.UP);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BEEF, 2));
		use(helper, player, cold, Direction.UP);
		helper.assertTrue(player.getMainHandItem().isEmpty(), "Both beef go in at once");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PORKCHOP));
		use(helper, player, hot, Direction.UP);
		helper.assertTrue(player.getMainHandItem().getCount() == 1, "A porkchop does not join the beef");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
		use(helper, player, hot, Direction.UP);
		helper.assertTrue(player.getMainHandItem().getCount() == 1, "A stick is not fried");
		SkilletBlockEntity pan = helper.getBlockEntity(hot, SkilletBlockEntity.class);
		SkilletBlockEntity coldPan = helper.getBlockEntity(cold, SkilletBlockEntity.class);
		helper.assertTrue(pan.raw().is(Items.BEEF) && pan.raw().getCount() == 2, "The pan holds the beef: " + pan.raw());
		int perBeef = 600 / SkilletBlockEntity.SPEED;
		helper.runAfterDelay(2 * perBeef + 40, () -> {
			helper.assertTrue(pan.raw().isEmpty() && pan.fried().is(Items.COOKED_BEEF) && pan.fried().getCount() == 2,
					"The beef should be fried: " + pan.raw() + " / " + pan.fried());
			helper.assertTrue(coldPan.raw().getCount() == 2 && coldPan.fried().isEmpty(), "A cold skillet does not fry");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			use(helper, player, hot, Direction.UP);
			helper.assertTrue(pan.isEmpty() && player.getInventory().countItem(Items.COOKED_BEEF) == 2, "An empty hand takes the steaks out");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- the Cutting Board and the knives

	/**
	 * Anything is set on an empty board, one at a time. A knife cuts a porkchop into two bacon and a cod into two slices
	 * and bone meal, a use of the knife each; dirt is not cut and is taken back by hand.
	 */
	@GameTest(maxTicks = 40)
	public void cuttingBoardCutsWithAKnife(GameTestHelper helper) {
		floor(helper);
		BlockPos board = new BlockPos(3, 2, 3);
		helper.setBlock(board, block("cutting_board"));
		CuttingBoardBlockEntity entity = helper.getBlockEntity(board, CuttingBoardBlockEntity.class);
		ServerPlayer player = cook(helper, new BlockPos(3, 2, 6), new ItemStack(Items.PORKCHOP, 2));
		use(helper, player, board, Direction.UP);
		helper.assertTrue(entity.item().is(Items.PORKCHOP) && entity.item().getCount() == 1 && player.getMainHandItem().getCount() == 1,
				"One porkchop is set on the board");
		use(helper, player, board, Direction.UP);
		helper.assertTrue(entity.item().getCount() == 1 && player.getMainHandItem().getCount() == 1, "The board holds one thing");
		ItemStack knife = new ItemStack(item("iron_knife"));
		player.setItemInHand(InteractionHand.MAIN_HAND, knife);
		use(helper, player, board, Direction.UP);
		helper.assertTrue(entity.item().isEmpty() && found(helper, player, item("bacon")) == 2 && knife.getDamageValue() == 1,
				"The knife cuts the porkchop into two bacon and is used once");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COD));
		use(helper, player, board, Direction.UP);
		player.setItemInHand(InteractionHand.MAIN_HAND, knife);
		use(helper, player, board, Direction.UP);
		helper.assertTrue(found(helper, player, item("cod_slice")) == 2 && found(helper, player, Items.BONE_MEAL) == 1 && knife.getDamageValue() == 2,
				"A cod is cut into two slices and bone meal");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT));
		use(helper, player, board, Direction.UP);
		player.setItemInHand(InteractionHand.MAIN_HAND, knife);
		use(helper, player, board, Direction.UP);
		helper.assertTrue(entity.item().is(Items.DIRT) && knife.getDamageValue() == 2, "Dirt is not cut, and the knife is not worn");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, board, Direction.UP);
		helper.assertTrue(entity.item().isEmpty() && player.getInventory().countItem(Items.DIRT) == 1, "An empty hand takes it back");
		helper.succeed();
	}

	/**
	 * Every knife cuts on the board. A knife cuts a Slice of Cake from a cake, even for a hungry cook (who would
	 * otherwise eat it), the Carving Knife too; the seventh slice takes the cake. A knife cuts a slice from a pie.
	 */
	@GameTest(maxTicks = 40)
	public void knivesSliceCakesAndPies(GameTestHelper helper) {
		floor(helper);
		BlockPos board = new BlockPos(1, 2, 1);
		helper.setBlock(board, block("cutting_board"));
		ServerPlayer player = cook(helper, new BlockPos(3, 2, 6), ItemStack.EMPTY);
		for (String knife : KNIVES) {
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SALMON));
			use(helper, player, board, Direction.UP);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(knife)));
			use(helper, player, board, Direction.UP);
			helper.assertTrue(player.getMainHandItem().getDamageValue() == 1, "The " + knife + " cuts on the board");
		}
		helper.assertTrue(found(helper, player, item("salmon_slice")) == 2 * KNIVES.size(), "Every knife cut a salmon into two slices");

		BlockPos cake = new BlockPos(3, 2, 3);
		helper.setBlock(cake, Blocks.CAKE);
		player.getFoodData().setFoodLevel(4);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("flint_knife")));
		use(helper, player, cake, Direction.UP);
		helper.assertTrue(helper.getBlockState(cake).getValue(BlockStateProperties.BITES) == 1 && player.getInventory().countItem(item("cake_slice")) == 1
				&& player.getFoodData().getFoodLevel() == 4 && player.getMainHandItem().getDamageValue() == 1,
				"A hungry cook slices the cake rather than eating it");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("carving_knife")));
		use(helper, player, cake, Direction.UP);
		helper.assertTrue(helper.getBlockState(cake).getValue(BlockStateProperties.BITES) == 2, "The Carving Knife slices a cake too");
		for (int i = 0; i < 5; i++) {
			use(helper, player, cake, Direction.UP);
		}
		helper.assertTrue(helper.getBlockState(cake).isAir() && player.getInventory().countItem(item("cake_slice")) == 7,
				"Seven slices take the cake");

		BlockPos pie = new BlockPos(5, 2, 3);
		helper.setBlock(pie, block("apple_pie"));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("golden_knife")));
		use(helper, player, pie, Direction.UP);
		helper.assertTrue(helper.getBlockState(pie).getValue(PieBlock.BITES) == 1 && player.getInventory().countItem(item("apple_pie_slice")) == 1,
				"A kitchen knife cuts a slice of pie");
		helper.succeed();
	}

	/**
	 * The cuts never add up to more than the whole, raw or cooked, in food or saturation; the cooked cut comes from the
	 * furnace, the smoker and the campfire.
	 */
	@GameTest
	public void cutsAddUpToNoMoreThanTheWhole(GameTestHelper helper) {
		Object[][] cuts = {
				{Items.PORKCHOP, Items.COOKED_PORKCHOP, "bacon", "cooked_bacon", 2},
				{Items.BEEF, Items.COOKED_BEEF, "minced_beef", "beef_patty", 2},
				{Items.CHICKEN, Items.COOKED_CHICKEN, "chicken_cuts", "cooked_chicken_cuts", 2},
				{Items.MUTTON, Items.COOKED_MUTTON, "mutton_chops", "cooked_mutton_chops", 2},
				{Items.COD, Items.COOKED_COD, "cod_slice", "cooked_cod_slice", 2},
				{Items.SALMON, Items.COOKED_SALMON, "salmon_slice", "cooked_salmon_slice", 2},
				{item("cabbage"), null, "cabbage_leaf", null, 2},
		};
		ServerLevel level = helper.getLevel();
		for (Object[] cut : cuts) {
			int pieces = (Integer) cut[4];
			for (int cooked = 0; cooked <= 1; cooked++) {
				Item whole = (Item) cut[cooked];
				String part = (String) cut[2 + cooked];
				if (whole == null) {
					continue;
				}
				FoodProperties wholeFood = new ItemStack(whole).get(DataComponents.FOOD);
				FoodProperties partFood = new ItemStack(item(part)).get(DataComponents.FOOD);
				helper.assertTrue(wholeFood != null && partFood != null, part + " and its whole are food");
				helper.assertTrue(pieces * partFood.nutrition() <= wholeFood.nutrition()
						&& pieces * partFood.saturation() <= wholeFood.saturation() + 1.0E-4F,
						pieces + " " + part + " should not outweigh the whole: " + partFood + " vs " + wholeFood);
			}
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id("cutting/" + cut[2]))).isPresent(),
					"cutting/" + cut[2] + " loads");
			if (cut[3] != null) {
				for (String suffix : List.of("", "_from_smoking", "_from_campfire_cooking")) {
					helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(cut[3] + suffix))).isPresent(),
							cut[3] + suffix + " loads");
				}
			}
		}
		FoodProperties slice = new ItemStack(item("cake_slice")).get(DataComponents.FOOD);
		helper.assertTrue(slice != null && slice.nutrition() == 2, "A Slice of Cake is one bite of cake");
		for (String id : List.of("kitchen_stove", "skillet", "cutting_board", "oak_cabinet", "netherite_knife_smithing", "fried_egg")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), id + " has a recipe");
		}
		for (String knife : KNIVES) {
			if (!knife.equals("netherite_knife")) {
				helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(knife))).isPresent(), knife + " has a recipe");
			}
			helper.assertTrue(new ItemStack(item(knife)).is(JugcraftAgriculture.KNIVES), knife + " is a knife");
		}
		helper.succeed();
	}

	// ---------------------------------------------------------------- the cabinets

	/**
	 * Every wood's cabinet holds a chestful; its doors open while it is open and shut after, and broken it drops itself
	 * and what is inside.
	 */
	@GameTest(maxTicks = 40)
	public void cabinetsHoldAChestful(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = cook(helper, new BlockPos(1, 2, 3), ItemStack.EMPTY);
		List<BlockPos> placed = new ArrayList<>();
		for (int i = 0; i < JugcraftAgriculture.CABINET_WOODS.size(); i++) {
			String wood = JugcraftAgriculture.CABINET_WOODS.get(i);
			BlockPos pos = new BlockPos(1 + i % 6, 2, 1 + 4 * (i / 6));
			helper.setBlock(pos, block(wood + "_cabinet"));
			KitchenCabinetBlockEntity cabinet = helper.getBlockEntity(pos, KitchenCabinetBlockEntity.class);
			helper.assertTrue(cabinet.getContainerSize() == KitchenCabinetBlockEntity.SLOTS, wood + " cabinet holds " + KitchenCabinetBlockEntity.SLOTS);
			cabinet.setItem(KitchenCabinetBlockEntity.SLOTS - 1, new ItemStack(Items.APPLE, 3));
			placed.add(pos);
		}
		BlockPos first = placed.get(0);
		use(helper, player, first, Direction.SOUTH);
		helper.assertTrue(player.containerMenu instanceof ChestMenu menu && menu.getContainer().getContainerSize() == KitchenCabinetBlockEntity.SLOTS
				&& helper.getBlockState(first).getValue(KitchenCabinetBlock.OPEN), "Used, it opens as a chest and its doors open");
		player.closeContainer();
		helper.assertTrue(!helper.getBlockState(first).getValue(KitchenCabinetBlock.OPEN), "Closed, its doors shut");
		for (BlockPos pos : placed) {
			helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
		}
		helper.runAfterDelay(3, () -> {
			helper.assertTrue(dropped(helper, Items.APPLE) == 3 * placed.size(), "Broken, they spill what is inside: " + dropped(helper, Items.APPLE));
			for (String wood : JugcraftAgriculture.CABINET_WOODS) {
				helper.assertTrue(dropped(helper, item(wood + "_cabinet")) == 1, "The " + wood + " cabinet drops itself");
			}
			helper.succeed();
		});
	}
}
