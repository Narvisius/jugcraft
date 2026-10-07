package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.BarmbrackBlock;
import io.github.jimbozoomer.jugcraft.agriculture.BarmbrackBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CandyBagItem;
import io.github.jimbozoomer.jugcraft.agriculture.GiantCandyBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PunchBowlBlock;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for treats: the Witch's Brew Punch Bowl (berries brew punch, a bottle ladles it out), the Barmbrack
 * (eaten a slice at a time by a hungry player, one slice hiding the ring, whole loaves only dropping), Giant Candy
 * (an empty hand changes its design), the treats themselves (the drinks' effects and bottles, which treats go in a
 * candy bag), and that their data loads.
 */
public class Decor13GameTests {
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

	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos pos, Direction side) {
		BlockPos absolute = helper.absolutePos(pos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(side, 0.5), side, absolute, false);
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	private static int count(ServerPlayer player, Item item) {
		int count = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			count += stack.is(item) ? stack.getCount() : 0;
		}
		return count;
	}

	private static int dropped(GameTestHelper helper, Item item) {
		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 8, 8).inflate(2.0);
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item)).stream()
				.mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	// ---------------------------------------------------------------- the punch bowl

	/**
	 * Each berry brews two servings until the bowl is full (then berries are refused and kept); a glass bottle ladles
	 * one Witch's Brew Punch; the bowl glows only while there is punch in it.
	 */
	@GameTest(maxTicks = 40)
	public void punchBowlsBrewAndLadle(GameTestHelper helper) {
		floor(helper);
		BlockPos bowl = new BlockPos(2, 2, 2);
		helper.setBlock(bowl, block("witchs_brew_punch_bowl"));
		helper.assertTrue(helper.getBlockState(bowl).getLightEmission() == 0, "An empty bowl doesn't glow");
		ServerPlayer player = player(helper, new BlockPos(2, 2, 4), 180.0F, new ItemStack(Items.GLOW_BERRIES, 8));
		helper.assertTrue(use(helper, player, bowl, Direction.SOUTH).consumesAction(), "A berry brews punch");
		helper.assertTrue(helper.getBlockState(bowl).getValue(PunchBowlBlock.SERVINGS_LEFT) == PunchBowlBlock.PER_BERRY
				&& player.getMainHandItem().getCount() == 7, "One berry, two servings");
		helper.assertTrue(helper.getBlockState(bowl).getLightEmission() == PunchBowlBlock.LIGHT, "Punch glows");
		for (int i = 1; i < PunchBowlBlock.SERVINGS / PunchBowlBlock.PER_BERRY; i++) {
			use(helper, player, bowl, Direction.SOUTH);
		}
		helper.assertTrue(helper.getBlockState(bowl).getValue(PunchBowlBlock.SERVINGS_LEFT) == PunchBowlBlock.SERVINGS
				&& player.getMainHandItem().getCount() == 8 - PunchBowlBlock.SERVINGS / PunchBowlBlock.PER_BERRY, "Six berries fill it");
		use(helper, player, bowl, Direction.SOUTH);
		helper.assertTrue(player.getMainHandItem().getCount() == 8 - PunchBowlBlock.SERVINGS / PunchBowlBlock.PER_BERRY,
				"A full bowl takes no more berries");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 2));
		use(helper, player, bowl, Direction.SOUTH);
		helper.assertTrue(helper.getBlockState(bowl).getValue(PunchBowlBlock.SERVINGS_LEFT) == PunchBowlBlock.SERVINGS - 1
				&& count(player, item("witchs_brew_punch")) == 1 && count(player, Items.GLASS_BOTTLE) == 1, "A bottle ladles a serving");
		helper.setBlock(bowl, block("witchs_brew_punch_bowl").defaultBlockState().setValue(PunchBowlBlock.SERVINGS_LEFT, 1));
		use(helper, player, bowl, Direction.SOUTH);
		use(helper, player, bowl, Direction.SOUTH);
		helper.assertTrue(helper.getBlockState(bowl).getValue(PunchBowlBlock.SERVINGS_LEFT) == 0
				&& count(player, item("witchs_brew_punch")) == 2 && helper.getBlockState(bowl).getLightEmission() == 0,
				"The last serving empties it, and an empty bowl gives nothing");
		helper.succeed();
	}

	// ---------------------------------------------------------------- the barmbrack

	/**
	 * A placed loaf hides its ring in one of its six slices; a full player can't eat; a hungry one eats a slice a use,
	 * finding the ring in exactly that slice and nowhere else, and the last slice finishes the loaf.
	 */
	@GameTest(maxTicks = 40)
	public void barmbracksHideOneRing(GameTestHelper helper) {
		floor(helper);
		BlockPos loaf = new BlockPos(2, 2, 2);
		ServerPlayer player = player(helper, new BlockPos(2, 2, 4), 180.0F, new ItemStack(item("barmbrack")));
		use(helper, player, loaf.below(), Direction.UP);
		helper.assertTrue(helper.getBlockState(loaf).is(block("barmbrack")), "The loaf is placed");
		int ring = helper.getBlockEntity(loaf, BarmbrackBlockEntity.class).ring();
		helper.assertTrue(ring >= 0 && ring < BarmbrackBlock.SLICES, "Placing it hides the ring in a slice");
		player.getFoodData().setFoodLevel(20);
		use(helper, player, loaf, Direction.UP);
		helper.assertTrue(helper.getBlockState(loaf).getValue(BarmbrackBlock.BITES) == 0, "A full player can't eat");
		player.getFoodData().setFoodLevel(0);
		for (int slice = 0; slice < BarmbrackBlock.SLICES; slice++) {
			use(helper, player, loaf, Direction.UP);
			helper.assertTrue(count(player, item("barmbrack_ring")) == (slice >= ring ? 1 : 0), "The ring is in slice " + ring + " only");
			if (slice + 1 < BarmbrackBlock.SLICES) {
				helper.assertTrue(helper.getBlockState(loaf).getValue(BarmbrackBlock.BITES) == slice + 1, "One slice a use");
			}
		}
		helper.assertTrue(helper.getBlockState(loaf).isAir(), "The last slice finishes the loaf");
		helper.assertTrue(player.getFoodData().getFoodLevel() == BarmbrackBlock.SLICES * BarmbrackBlock.NUTRITION, "Each slice feeds");
		helper.succeed();
	}

	/** A whole loaf drops itself when broken; a cut one drops nothing (no second ring from it). */
	@GameTest(maxTicks = 40)
	public void onlyWholeBarmbracksDrop(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos whole = new BlockPos(2, 2, 2);
		BlockPos cut = new BlockPos(5, 2, 5);
		helper.setBlock(whole, block("barmbrack"));
		helper.setBlock(cut, block("barmbrack").defaultBlockState().setValue(BarmbrackBlock.BITES, 2));
		level.destroyBlock(helper.absolutePos(whole), true);
		level.destroyBlock(helper.absolutePos(cut), true);
		helper.assertTrue(dropped(helper, item("barmbrack")) == 1, "Only the whole loaf drops");
		helper.setBlock(whole, block("barmbrack"));
		helper.setBlock(whole.below(), Blocks.AIR);
		helper.assertTrue(helper.getBlockState(whole).isAir(), "A loaf needs something under it");
		helper.succeed();
	}

	// ---------------------------------------------------------------- giant candy

	/** An empty hand goes through the four designs and back to the first. */
	@GameTest(maxTicks = 40)
	public void giantCandyChangesDesign(GameTestHelper helper) {
		floor(helper);
		BlockPos candy = new BlockPos(2, 2, 2);
		helper.setBlock(candy, block("giant_candy"));
		ServerPlayer player = player(helper, new BlockPos(2, 2, 4), 180.0F, ItemStack.EMPTY);
		GiantCandyBlock.Design design = helper.getBlockState(candy).getValue(GiantCandyBlock.DESIGN);
		for (int i = 0; i < GiantCandyBlock.Design.values().length; i++) {
			use(helper, player, candy, Direction.SOUTH);
			BlockState state = helper.getBlockState(candy);
			helper.assertTrue(state.getValue(GiantCandyBlock.DESIGN) == design.next(), "Each use shows the next design");
			design = state.getValue(GiantCandyBlock.DESIGN);
		}
		helper.assertTrue(design == GiantCandyBlock.Design.CANDY_CORN, "Four uses come back round");
		helper.succeed();
	}

	// ---------------------------------------------------------------- the treats

	/**
	 * The latte and the punch are drunk even when full, give their effect and leave a glass bottle; soul cakes,
	 * cupcakes and cookies go in a candy bag (and candy bowls), the bread doesn't.
	 */
	@GameTest(maxTicks = 40)
	public void treatsFeedAndFill(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(2, 2, 2), 0.0F, ItemStack.EMPTY);
		for (String drink : List.of("pumpkin_spice_latte", "witchs_brew_punch")) {
			FoodProperties food = new ItemStack(item(drink)).get(DataComponents.FOOD);
			helper.assertTrue(food != null && food.canAlwaysEat(), drink + " can be drunk on a full stomach");
		}
		ItemStack latte = new ItemStack(item("pumpkin_spice_latte"));
		ItemStack left = latte.finishUsingItem(level, player);
		helper.assertTrue(left.is(Items.GLASS_BOTTLE) && player.hasEffect(MobEffects.SPEED), "The latte gives Speed and leaves its bottle");
		ItemStack brew = new ItemStack(item("witchs_brew_punch"));
		helper.assertTrue(brew.finishUsingItem(level, player).is(Items.GLASS_BOTTLE) && player.hasEffect(MobEffects.GLOWING),
				"The punch makes you glow and leaves its bottle");
		for (String treat : List.of("soul_cake", "spiderweb_cupcake", "bat_wing_cookie")) {
			helper.assertTrue(new ItemStack(item(treat)).is(CandyBagItem.TREATS), treat + " goes in a candy bag");
		}
		helper.assertTrue(!new ItemStack(item("pumpkin_bread")).is(CandyBagItem.TREATS), "Bread doesn't");
		TagKey<Item> candy = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "foods/candy"));
		helper.assertTrue(new ItemStack(item("spiderweb_cupcake")).is(candy) && new ItemStack(item("bat_wing_cookie")).is(candy),
				"Cupcakes and cookies count as candy");
		helper.succeed();
	}

	// ---------------------------------------------------------------- data

	/** The recipes and the blocks' loot tables load. */
	@GameTest
	public void treatsDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("soul_cake", "pumpkin_bread", "spiderweb_cupcake", "bat_wing_cookie", "pumpkin_spice_latte",
				"witchs_brew_punch_bowl", "barmbrack", "giant_candy")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), "Recipe " + id + " loads");
		}
		for (String table : List.of("blocks/witchs_brew_punch_bowl", "blocks/barmbrack", "blocks/giant_candy")) {
			helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id(table)))
					!= LootTable.EMPTY, "Loot table " + table + " loads");
		}
		helper.succeed();
	}
}
