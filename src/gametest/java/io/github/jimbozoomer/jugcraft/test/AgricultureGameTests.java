package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.CropGrowth;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.TallCrop;
import io.github.jimbozoomer.jugcraft.agriculture.TallCropBlock;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** In-game tests for the Agriculture branch: tall crops, picking, breaking, sickles, legumes and loot. */
public class AgricultureGameTests {
	private static final BlockPos SOIL = new BlockPos(2, 1, 2);
	private static final BlockPos CROP = SOIL.above();

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static TallCropBlock corn() {
		return JugcraftAgriculture.TALL_CROPS.get(TallCrop.CORN);
	}

	private static void farmland(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
	}

	/** Plants corn on moist farmland at {@link #CROP} and grows it to {@code age}. */
	private static void plantCorn(GameTestHelper helper, int age) {
		farmland(helper, SOIL);
		helper.setBlock(CROP, corn().defaultBlockState());
		if (age > 0) {
			helper.assertTrue(corn().growTo(helper.getLevel(), helper.absolutePos(CROP), age), "Corn could not grow to age " + age);
		}
	}

	private static void assertCorn(GameTestHelper helper, int age, int height) {
		for (int section = 0; section < height; section++) {
			BlockState state = helper.getBlockState(CROP.above(section));
			helper.assertTrue(state.is(corn()) && state.getValue(TallCropBlock.SECTION) == section
					&& state.getValue(TallCropBlock.AGE) == age, "Expected corn section " + section + " at age " + age + ", found " + state);
		}
		helper.assertBlockNotPresent(corn(), CROP.above(height));
	}

	private static void setCrop(GameTestHelper helper, BlockPos pos, String crop, int age) {
		farmland(helper, pos.below());
		helper.setBlock(pos, ((CropBlock) JugcraftAgriculture.block(crop)).getStateForAge(age));
	}

	/** Ripe corn stands three blocks tall, every block showing its own section at the same age. */
	@GameTest
	public void cornGrowsThreeBlocksTall(GameTestHelper helper) {
		plantCorn(helper, 0);
		assertCorn(helper, 0, 1);
		plantCorn(helper, 4);
		assertCorn(helper, 4, 2);
		helper.assertTrue(corn().growTo(helper.getLevel(), helper.absolutePos(CROP), 7), "Corn should reach age 7");
		assertCorn(helper, 7, 3);
		helper.succeed();
	}

	/** Corn only grows into air: a block two above stops it at two blocks tall. */
	@GameTest
	public void blockedCornWaits(GameTestHelper helper) {
		plantCorn(helper, 4);
		helper.setBlock(CROP.above(2), Blocks.STONE);
		helper.assertTrue(!corn().growTo(helper.getLevel(), helper.absolutePos(CROP), 5), "Corn grew into stone");
		assertCorn(helper, 4, 2);
		helper.succeed();
	}

	/** Right-clicking ripe corn drops ears and keeps the stalk standing, three blocks tall. */
	@GameTest
	public void pickingKeepsCornStanding(GameTestHelper helper) {
		plantCorn(helper, 7);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.useBlock(CROP.above(), player);
		assertCorn(helper, TallCrop.CORN.pickReset, 3);
		helper.assertItemEntityPresent(item("corn"), CROP.above(), 3.0);
		helper.succeed();
	}

	/** Breaking the top of a plant removes all of it; the bottom's loot drops once. */
	@GameTest(maxTicks = 40)
	public void breakingTopBreaksWholePlant(GameTestHelper helper) {
		plantCorn(helper, 7);
		helper.destroyBlock(CROP.above(2));
		helper.succeedWhen(() -> {
			for (int section = 0; section < 3; section++) {
				helper.assertBlockNotPresent(corn(), CROP.above(section));
			}
			helper.assertItemEntityPresent(item("corn_kernels"), CROP, 3.0);
			helper.assertItemEntityPresent(item("corn"), CROP, 3.0);
		});
	}

	/** From two blocks tall, corn blocks movement (maze walls); seedlings can be walked through. */
	@GameTest
	public void tallCornIsAWall(GameTestHelper helper) {
		plantCorn(helper, 2);
		helper.assertTrue(helper.getBlockState(CROP).getCollisionShape(helper.getLevel(), helper.absolutePos(CROP)).isEmpty(),
				"Knee-high corn should not block movement");
		helper.assertTrue(corn().growTo(helper.getLevel(), helper.absolutePos(CROP), 3), "Corn should grow to two blocks");
		for (int section = 0; section < 2; section++) {
			BlockPos pos = CROP.above(section);
			helper.assertTrue(!helper.getBlockState(pos).getCollisionShape(helper.getLevel(), helper.absolutePos(pos)).isEmpty(),
					"Two-block corn should block movement at section " + section);
		}
		// The same collision query entity movement uses: a player-sized box cannot stand inside the wall.
		AABB player = new AABB(helper.absolutePos(CROP)).inflate(-0.2, 0.0, -0.2).expandTowards(0.0, 0.8, 0.0);
		helper.assertTrue(!helper.getLevel().noCollision(player), "An entity could stand inside a corn wall");
		helper.succeed();
	}

	/** A two- or three-block plant blocks movement, but it is still a plant: the farmland under it stays farmland. */
	@GameTest(maxTicks = 60)
	public void tallCornKeepsItsFarmland(GameTestHelper helper) {
		plantCorn(helper, 7);
		helper.runAtTickTime(40, () -> {
			helper.assertBlockPresent(Blocks.FARMLAND, SOIL);
			assertCorn(helper, 7, 3);
			helper.succeed();
		});
	}

	/** Corn kernels plant corn on farmland. */
	@GameTest
	public void kernelsPlantCorn(GameTestHelper helper) {
		farmland(helper, SOIL);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("corn_kernels")));
		BlockPos soil = helper.absolutePos(SOIL);
		helper.useBlock(SOIL, player, new BlockHitResult(Vec3.atCenterOf(soil).add(0.0, 0.5, 0.0), Direction.UP, soil, false));
		helper.assertBlockPresent(corn(), CROP);
		helper.succeed();
	}

	/** A sickle harvests every ripe crop around the clicked one, replants low crops and leaves unripe ones. */
	@GameTest
	public void sickleHarvestsAndReplants(GameTestHelper helper) {
		for (int x = 1; x <= 3; x++) {
			setCrop(helper, new BlockPos(x, 2, 2), "bean_crop", 7);
		}
		setCrop(helper, new BlockPos(2, 2, 3), "bean_crop", 3);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack sickle = new ItemStack(item("flint_sickle"));
		player.setItemInHand(InteractionHand.MAIN_HAND, sickle);
		helper.useBlock(new BlockPos(2, 2, 2), player);
		Block beans = JugcraftAgriculture.block("bean_crop");
		for (int x = 1; x <= 3; x++) {
			BlockState state = helper.getBlockState(new BlockPos(x, 2, 2));
			helper.assertTrue(state.is(beans) && state.getValue(CropBlock.AGE) == 0, "Bean crop at x=" + x + " should be replanted, found " + state);
		}
		helper.assertTrue(helper.getBlockState(new BlockPos(2, 2, 3)).getValue(CropBlock.AGE) == 3, "Unripe beans should be left alone");
		helper.assertItemEntityPresent(item("beans"), new BlockPos(2, 2, 2), 3.0);
		helper.assertTrue(player.getMainHandItem().getDamageValue() == 1, "The sickle should lose 1 durability per use");
		helper.succeed();
	}

	/** Beans next to a crop make it grow 1.5 times as fast; legumes do not boost each other. */
	@GameTest
	public void legumesFeedTheirNeighbours(GameTestHelper helper) {
		BlockPos potato = new BlockPos(2, 2, 2);
		setCrop(helper, potato, "sweet_potato_crop", 0);
		// The same soil either way: only the bean crop itself changes.
		farmland(helper, new BlockPos(3, 1, 2));
		float alone = CropGrowth.speed(helper.getLevel(), helper.absolutePos(potato), false);
		setCrop(helper, new BlockPos(3, 2, 2), "bean_crop", 0);
		float withBeans = CropGrowth.speed(helper.getLevel(), helper.absolutePos(potato), false);
		helper.assertTrue(Math.abs(withBeans - alone * CropGrowth.LEGUME_BONUS) < 1.0E-4F,
				"Expected " + alone * CropGrowth.LEGUME_BONUS + " next to beans, got " + withBeans);
		float beanSpeed = CropGrowth.speed(helper.getLevel(), helper.absolutePos(new BlockPos(3, 2, 2)), true);
		helper.assertTrue(beanSpeed < withBeans, "Legumes should not get the legume bonus themselves");
		helper.succeed();
	}

	/** The 26.x-format loot tables load: ripe crops and wild plants drop their harvest. */
	@GameTest(maxTicks = 40)
	public void cropsDropTheirHarvest(GameTestHelper helper) {
		setCrop(helper, new BlockPos(1, 2, 1), "flax_crop", 7);
		setCrop(helper, new BlockPos(4, 2, 1), "sweet_potato_crop", 7);
		helper.setBlock(new BlockPos(1, 1, 4), Blocks.GRASS_BLOCK);
		helper.setBlock(new BlockPos(1, 2, 4), JugcraftAgriculture.block("wild_beans"));
		// GameTestHelper.destroyBlock breaks without drops; break them as a player would.
		for (BlockPos pos : new BlockPos[] {new BlockPos(1, 2, 1), new BlockPos(4, 2, 1), new BlockPos(1, 2, 4)}) {
			helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
		}
		helper.succeedWhen(() -> {
			helper.assertItemEntityPresent(item("flax"), new BlockPos(1, 2, 1), 2.0);
			helper.assertItemEntityPresent(item("flax_seeds"), new BlockPos(1, 2, 1), 2.0);
			helper.assertItemEntityPresent(item("sweet_potato"), new BlockPos(4, 2, 1), 2.0);
			helper.assertItemEntityPresent(item("beans"), new BlockPos(1, 2, 4), 2.0);
		});
	}

	/** Crop items fill a composter (corn composts 65% of the time, like a carrot). */
	@GameTest
	public void cropsCompost(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 1, 2);
		helper.setBlock(pos, Blocks.COMPOSTER);
		BlockState composter = helper.getBlockState(pos);
		for (int i = 0; i < 40 && composter.getValue(ComposterBlock.LEVEL) == 0; i++) {
			composter = ComposterBlock.insertItem(null, composter, helper.getLevel(), new ItemStack(item("corn")), helper.absolutePos(pos));
		}
		helper.assertTrue(composter.getValue(ComposterBlock.LEVEL) > 0, "40 corn never raised the composter's level");
		helper.succeed();
	}

	/** Foods carry the values in docs/branches/AGRICULTURE.md; the stew returns its bowl and does not stack. */
	@GameTest
	public void foodsHaveTheirValues(GameTestHelper helper) {
		String[] foods = {"corn", "roasted_corn", "popcorn", "sweet_potato", "baked_sweet_potato", "roasted_sunflower_seeds", "three_sisters_stew"};
		int[] nutrition = {3, 5, 2, 2, 5, 2, 10};
		for (int i = 0; i < foods.length; i++) {
			FoodProperties food = new ItemStack(item(foods[i])).get(DataComponents.FOOD);
			helper.assertTrue(food != null && food.nutrition() == nutrition[i], foods[i] + " should restore " + nutrition[i] + ", has " + food);
		}
		helper.assertTrue(new ItemStack(item("beans")).get(DataComponents.FOOD) == null, "Raw beans should not be edible");
		helper.assertTrue(new ItemStack(item("three_sisters_stew")).getMaxStackSize() == 1, "Stew should not stack");
		helper.succeed();
	}

	/**
	 * Short grass drops one Jugcraft seed about one time in eight, like vanilla wheat seeds, and every
	 * crop's seed turns up, so every crop is reachable in any world.
	 */
	@GameTest
	public void grassDropsJugcraftSeeds(GameTestHelper helper) {
		helper.setBlock(SOIL, Blocks.GRASS_BLOCK);
		helper.setBlock(CROP, Blocks.SHORT_GRASS);
		BlockState grass = helper.getBlockState(CROP);
		// Every seed in JugcraftAgriculture.GRASS_SEEDS (tools/agriculture.py), so the total is the whole chance.
		String[] seeds = {"corn_kernels", "sunflower_seeds", "beans", "sweet_potato", "flax_seeds", "tomato_seeds", "pepper_seeds",
				"onion", "garlic", "cabbage_seeds", "oat_seeds", "barley_seeds", "butternut_squash_seeds", "acorn_squash_seeds",
				"warty_gourd_seeds", "turnip", "cranberries", "chestnut", "giant_pumpkin_seeds", "white_pumpkin_seeds", "jarrahdale_pumpkin_seeds",
				"cinderella_pumpkin_seeds", "red_kuri_pumpkin_seeds", "kabocha_pumpkin_seeds", "bottle_gourd_seeds", "ornamental_corn_kernels",
				"mandrake_root"};
		int[] found = new int[seeds.length];
		int breaks = 4000;
		int total = 0;
		for (int i = 0; i < breaks; i++) {
			int here = 0;
			for (ItemStack drop : Block.getDrops(grass, helper.getLevel(), helper.absolutePos(CROP), null)) {
				for (int s = 0; s < seeds.length; s++) {
					if (drop.is(item(seeds[s]))) {
						found[s]++;
						here++;
					}
				}
			}
			helper.assertTrue(here <= 1, "One broken short grass dropped " + here + " Jugcraft seeds");
			total += here;
		}
		// Expected 500 in 4,000 (0.125); the bounds are over 6 standard deviations wide.
		helper.assertTrue(total > 380 && total < 620, total + " Jugcraft seeds from " + breaks + " short grass; expected about 500");
		for (int s = 0; s < seeds.length; s++) {
			helper.assertTrue(found[s] > 0, seeds[s] + " never dropped from " + breaks + " short grass");
		}
		helper.succeed();
	}
}
