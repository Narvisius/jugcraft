package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PlacedDishBlock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * In-game tests for the owner's milkshakes (tools/milkshakes.py): each crafted from a Milk Bottle, a snowball, a sugar
 * and its own flavour; drunk, even on a full stomach, for {@value #FOOD} food and Haste, leaving the glass bottle; and set
 * down by a sneaking player as the owner's glass, facing them, and taken back with an empty hand.
 */
public class MilkshakeGameTests {
	/** What a milkshake gives (tools/milkshakes.py FOOD) and how long its Haste lasts, in seconds (EFFECT). */
	private static final int FOOD = 5;
	private static final int HASTE_SECONDS = 30;

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	/** The milkshakes, in the page's order, and the flavour each takes with the milk, snowball and sugar. */
	private static Map<String, List<Item>> flavours() {
		Map<String, List<Item>> out = new LinkedHashMap<>();
		out.put("strawberry_milkshake", List.of(item("strawberry"), item("strawberry")));
		out.put("banana_milkshake", List.of(item("banana")));
		out.put("plum_milkshake", List.of(item("plum")));
		out.put("apple_milkshake", List.of(Items.APPLE));
		out.put("blueberry_milkshake", List.of(item("blueberries"), item("blueberries")));
		out.put("pumpkin_milkshake", List.of(Items.PUMPKIN));
		out.put("chocolate_milkshake", List.of(Items.COCOA_BEANS, Items.SWEET_BERRIES));
		return out;
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
	private static void useTop(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
		BlockPos absolute = helper.absolutePos(pos);
		BlockHitResult hit = new BlockHitResult(new Vec3(absolute.getX() + 0.5, absolute.getY() + 1.0, absolute.getZ() + 0.5), Direction.UP,
				absolute, false);
		player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	/** Uses what the player holds on {@code pos} itself. */
	private static void use(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
		BlockPos absolute = helper.absolutePos(pos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
		player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	/** A Milk Bottle, a snowball, a sugar and the flavour, anywhere in a crafting grid, make the milkshake. */
	@GameTest(maxTicks = 20)
	public void milkshakesAreCrafted(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var recipes = level.getServer().getRecipeManager();
		for (Map.Entry<String, List<Item>> shake : flavours().entrySet()) {
			List<ItemStack> grid = new ArrayList<>();
			for (Item ingredient : List.of(Items.SNOWBALL, item("milk_bottle"), Items.SUGAR)) {
				grid.add(new ItemStack(ingredient));
			}
			shake.getValue().forEach(flavour -> grid.add(new ItemStack(flavour)));
			while (grid.size() < 9) {
				grid.add(ItemStack.EMPTY);
			}
			CraftingInput input = CraftingInput.of(3, 3, grid);
			ItemStack made = recipes.getRecipeFor(RecipeType.CRAFTING, input, level)
					.orElseThrow(() -> helper.assertionException(shake.getKey() + "'s ingredients craft nothing")).value().assemble(input);
			helper.assertTrue(made.is(item(shake.getKey())) && made.getCount() == 1, shake.getKey() + "'s ingredients made " + made);
		}
		helper.succeed();
	}

	/**
	 * Every milkshake is drunk even on a full stomach (a drink, stacking to 16), for five food and half a minute of Haste,
	 * and leaves its glass bottle.
	 */
	@GameTest(maxTicks = 20)
	public void milkshakesAreDrunk(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = player(helper, new BlockPos(2, 2, 2), ItemStack.EMPTY);
		for (String shake : flavours().keySet()) {
			ItemStack stack = new ItemStack(item(shake));
			FoodProperties food = stack.get(DataComponents.FOOD);
			helper.assertTrue(food != null && food.nutrition() == FOOD && food.canAlwaysEat() && stack.getMaxStackSize() == 16,
					shake + " is a drink of " + FOOD + " food, drunk even when full: " + food);
			player.removeAllEffects();
			player.getFoodData().setFoodLevel(4);
			ItemStack left = stack.finishUsingItem(level, player);
			var haste = player.getEffect(MobEffects.HASTE);
			helper.assertTrue(left.is(Items.GLASS_BOTTLE) && player.getFoodData().getFoodLevel() == 4 + FOOD && haste != null
					&& haste.getDuration() <= HASTE_SECONDS * 20 && haste.getDuration() > HASTE_SECONDS * 20 - 20,
					shake + " gives " + FOOD + " food and Haste, leaving its bottle: " + left + ", " + haste);
		}
		helper.succeed();
	}

	/**
	 * Every milkshake sets down as the owner's glass ({@link PlacedDishBlock.DishShape#MILKSHAKE}): a sneaking player sets
	 * the chocolate milkshake down facing them, it stands as tall as its straw, and an empty hand takes it back.
	 */
	@GameTest(maxTicks = 20)
	public void milkshakesSetDownAndTakenBack(GameTestHelper helper) {
		for (String shake : flavours().keySet()) {
			helper.assertTrue(block(shake) instanceof PlacedDishBlock placed && placed.dish() == item(shake)
					&& placed.dishShape() == PlacedDishBlock.DishShape.MILKSHAKE, shake + " sets down as the milkshake glass");
		}
		BlockPos ground = new BlockPos(3, 1, 3);
		BlockPos glass = ground.above();
		helper.setBlock(ground, Blocks.STONE);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("chocolate_milkshake")));
		player.setShiftKeyDown(true);
		useTop(helper, player, ground);
		player.setShiftKeyDown(false);
		helper.assertTrue(helper.getBlockState(glass).is(block("chocolate_milkshake")) && player.getMainHandItem().isEmpty()
				&& helper.getBlockState(glass).getValue(PlacedDishBlock.FACING) == Direction.SOUTH,
				"Sneaking, the milkshake is set down facing the player, and used up");
		VoxelShape shape = helper.getBlockState(glass).getShape(helper.getLevel(), helper.absolutePos(glass));
		helper.assertTrue(Math.abs(shape.bounds().maxY - 15.75 / 16.0) < 1e-6 && shape.bounds().minX > 0.25 && shape.bounds().maxX < 0.75,
				"The glass stands as tall as its straw, narrower than a block: " + shape.bounds());
		use(helper, player, glass);
		helper.assertTrue(helper.getBlockState(glass).isAir() && player.getMainHandItem().is(item("chocolate_milkshake")),
				"An empty hand takes it back");
		helper.succeed();
	}
}
