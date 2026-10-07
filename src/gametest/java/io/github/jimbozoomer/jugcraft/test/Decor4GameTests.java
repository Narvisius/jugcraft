package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.ApothecaryShelfBlock;
import io.github.jimbozoomer.jugcraft.agriculture.BubblingCauldronBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CrystalBallBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GrimoireStandBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.WitchsBroomBlock;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the witch's cottage: the Bubbling Cauldron (water by bucket, brews by ingredient, glowing, poured
 * back out, heated by the Cooking Pot's heat sources), the Apothecary Shelf (walls only, four arrangements, falls with
 * its wall), the Crystal Ball (gazing flares it, it rests before the next gaze), the Grimoire Stand (turns through its
 * spreads) and the Witch's Broom (needs a floor), and that their data loads.
 */
public class Decor4GameTests {
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

	private static BlockHitResult hit(GameTestHelper helper, BlockPos pos, Direction side) {
		BlockPos absolute = helper.absolutePos(pos);
		return new BlockHitResult(Vec3.atCenterOf(absolute).relative(side, 0.5), side, absolute, false);
	}

	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos pos, Direction side) {
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(helper, pos, side));
	}

	private static InteractionResult place(GameTestHelper helper, ServerPlayer player, BlockPos pos, Direction side) {
		return player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, pos, side)));
	}

	private static int dropped(GameTestHelper helper, Item item) {
		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 8, 8).inflate(2.0);
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item)).stream()
				.mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	private static BubblingCauldronBlock.Brew contents(GameTestHelper helper, BlockPos pos) {
		return helper.getBlockState(pos).getValue(BubblingCauldronBlock.CONTENTS);
	}

	// ---------------------------------------------------------------- the bubbling cauldron

	/**
	 * A brew ingredient does nothing in an empty cauldron; a water bucket fills it (the bucket comes back empty); a
	 * spider eye makes a green brew that glows, the same again does nothing, nether wart turns it purple; an empty
	 * bucket pours it out as water. A lit campfire underneath heats it, an unlit one doesn't.
	 */
	@GameTest
	public void theCauldronBrews(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, block("bubbling_cauldron"));
		ServerPlayer player = player(helper, new BlockPos(3, 2, 5), new ItemStack(Items.SPIDER_EYE, 3));
		use(helper, player, pos, Direction.UP);
		helper.assertTrue(contents(helper, pos) == BubblingCauldronBlock.Brew.EMPTY && player.getMainHandItem().getCount() == 3,
				"An ingredient does nothing without water");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
		use(helper, player, pos, Direction.UP);
		helper.assertTrue(contents(helper, pos) == BubblingCauldronBlock.Brew.WATER && player.getMainHandItem().is(Items.BUCKET),
				"A water bucket fills it and comes back empty");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SPIDER_EYE, 3));
		use(helper, player, pos, Direction.UP);
		helper.assertTrue(contents(helper, pos) == BubblingCauldronBlock.Brew.GREEN && player.getMainHandItem().getCount() == 2
				&& helper.getBlockState(pos).getLightEmission() == BubblingCauldronBlock.BREW_LIGHT, "A spider eye makes a glowing green brew");
		use(helper, player, pos, Direction.UP);
		helper.assertTrue(player.getMainHandItem().getCount() == 2, "The same ingredient again does nothing");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("nether_wart"))));
		use(helper, player, pos, Direction.UP);
		helper.assertTrue(contents(helper, pos) == BubblingCauldronBlock.Brew.PURPLE, "Nether wart turns it purple");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
		use(helper, player, pos, Direction.UP);
		helper.assertTrue(contents(helper, pos) == BubblingCauldronBlock.Brew.EMPTY && player.getMainHandItem().is(Items.WATER_BUCKET)
				&& helper.getBlockState(pos).getLightEmission() == 0, "An empty bucket pours it out as water");

		BlockPos hot = new BlockPos(6, 3, 6);
		helper.setBlock(hot.below(), Blocks.CAMPFIRE);
		helper.setBlock(hot, block("bubbling_cauldron"));
		helper.assertTrue(CookingPotBlockEntity.isHeated(level, helper.absolutePos(hot)), "A lit campfire heats it");
		helper.setBlock(hot.below(), Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false));
		helper.assertFalse(CookingPotBlockEntity.isHeated(level, helper.absolutePos(hot)), "An unlit one doesn't");
		helper.succeed();
	}

	// ---------------------------------------------------------------- the apothecary shelf

	/** The shelf hangs only on walls, sneak-use sets it out four ways and back, and it falls (dropping) with its wall. */
	@GameTest
	public void theApothecaryShelfHangsOnAWall(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos wall = new BlockPos(3, 2, 4);
		helper.setBlock(wall, Blocks.OAK_PLANKS);
		BlockPos pos = wall.north();
		ServerPlayer player = player(helper, new BlockPos(3, 2, 1), new ItemStack(item("apothecary_shelf"), 2));
		place(helper, player, new BlockPos(5, 1, 5), Direction.UP);
		helper.assertBlockNotPresent(block("apothecary_shelf"), new BlockPos(5, 2, 5));
		place(helper, player, wall, Direction.NORTH);
		helper.assertTrue(helper.getBlockState(pos).is(block("apothecary_shelf"))
				&& helper.getBlockState(pos).getValue(ApothecaryShelfBlock.FACING) == Direction.NORTH, "It hangs on the wall, facing out");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.setShiftKeyDown(true);
		for (int n = 1; n <= ApothecaryShelfBlock.ARRANGEMENTS; n++) {
			use(helper, player, pos, Direction.NORTH);
			helper.assertTrue(helper.getBlockState(pos).getValue(ApothecaryShelfBlock.ARRANGEMENT) == n % ApothecaryShelfBlock.ARRANGEMENTS,
					"Sneak-use sets it out the next way");
		}
		level.destroyBlock(helper.absolutePos(wall), false);
		helper.succeedWhen(() -> {
			helper.assertBlockNotPresent(block("apothecary_shelf"), pos);
			helper.assertTrue(dropped(helper, item("apothecary_shelf")) == 1, "It falls with its wall");
		});
	}

	// ---------------------------------------------------------------- the crystal ball

	/** Gazing flares the ball (light 12); while it rests a second gaze does nothing; it settles after its time. */
	@GameTest(maxTicks = 100)
	public void gazingIntoTheCrystalBall(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, block("crystal_ball"));
		helper.assertTrue(helper.getBlockState(pos).getLightEmission() == CrystalBallBlock.LIGHT, "It glows softly");
		ServerPlayer player = player(helper, new BlockPos(3, 2, 5), ItemStack.EMPTY);
		helper.assertTrue(use(helper, player, pos, Direction.SOUTH).consumesAction(), "Gazing works");
		helper.assertTrue(helper.getBlockState(pos).getValue(CrystalBallBlock.GAZING)
				&& helper.getBlockState(pos).getLightEmission() == CrystalBallBlock.GAZING_LIGHT, "The mist flares");
		helper.assertFalse(use(helper, player, pos, Direction.SOUTH).consumesAction(), "While it rests, another gaze does nothing");
		helper.runAfterDelay(CrystalBallBlock.GAZE_TICKS + 5, () -> {
			helper.assertFalse(helper.getBlockState(pos).getValue(CrystalBallBlock.GAZING), "The mist settles");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- the grimoire and the broom

	/** The grimoire faces whoever placed it, glows faintly and turns through its four spreads and back. */
	@GameTest
	public void theGrimoireTurnsItsPages(GameTestHelper helper) {
		floor(helper);
		BlockPos stand = new BlockPos(3, 1, 3);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("grimoire_stand")));
		player.setYRot(180.0F);
		place(helper, player, stand, Direction.UP);
		BlockPos pos = stand.above();
		BlockState state = helper.getBlockState(pos);
		helper.assertTrue(state.is(block("grimoire_stand")) && state.getValue(GrimoireStandBlock.FACING) == Direction.SOUTH
				&& state.getLightEmission() == GrimoireStandBlock.LIGHT, "It faces its reader and glows faintly: " + state);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		GrimoireStandBlock.Spread first = state.getValue(GrimoireStandBlock.PAGE);
		for (int i = 0; i < GrimoireStandBlock.Spread.values().length; i++) {
			use(helper, player, pos, Direction.UP);
			helper.assertTrue(helper.getBlockState(pos).getValue(GrimoireStandBlock.PAGE) != first || i == GrimoireStandBlock.Spread.values().length - 1,
					"Each use turns to another spread");
		}
		helper.assertTrue(helper.getBlockState(pos).getValue(GrimoireStandBlock.PAGE) == first, "and round to the first again");
		helper.succeed();
	}

	/** The broom stands on a floor facing whoever placed it, won't stand in the air, and falls when its floor goes. */
	@GameTest
	public void theBroomNeedsAFloor(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos stand = new BlockPos(3, 1, 3);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("witchs_broom"), 2));
		player.setYRot(180.0F);
		place(helper, player, stand, Direction.UP);
		helper.assertTrue(helper.getBlockState(stand.above()).is(block("witchs_broom"))
				&& helper.getBlockState(stand.above()).getValue(WitchsBroomBlock.FACING) == Direction.SOUTH, "It stands facing its owner");
		BlockPos post = new BlockPos(6, 3, 6);
		helper.setBlock(post, Blocks.OAK_PLANKS);
		place(helper, player, post, Direction.EAST);
		helper.assertBlockNotPresent(block("witchs_broom"), post.east());
		level.destroyBlock(helper.absolutePos(stand), false);
		helper.succeedWhen(() -> {
			helper.assertBlockNotPresent(block("witchs_broom"), stand.above());
			helper.assertTrue(dropped(helper, item("witchs_broom")) == 1, "It falls when its floor goes");
		});
	}

	/** The recipes, loot tables and brew tags load. */
	@GameTest
	public void witchsCottageDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("bubbling_cauldron", "apothecary_shelf", "crystal_ball", "grimoire_stand", "witchs_broom")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), "Recipe " + id + " loads");
			LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("blocks/" + id)));
			helper.assertTrue(table != LootTable.EMPTY, "The " + id + " drops itself");
		}
		helper.assertTrue(BubblingCauldronBlock.brewFor(new ItemStack(Items.SPIDER_EYE)) == BubblingCauldronBlock.Brew.GREEN
				&& BubblingCauldronBlock.brewFor(new ItemStack(Items.GLOWSTONE_DUST)) == BubblingCauldronBlock.Brew.ORANGE
				&& BubblingCauldronBlock.brewFor(new ItemStack(Items.DIRT)) == null, "The brew tags load");
		helper.succeed();
	}
}
