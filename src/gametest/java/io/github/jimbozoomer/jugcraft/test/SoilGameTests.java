package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.BasketBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.OrganicCompostBlock;
import io.github.jimbozoomer.jugcraft.agriculture.RichFarmlandBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TallCrop;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for soil, compost and storage (the kitchen and cooking expansion's slice 5, tools/soil.py): a hoe tilling
 * Rich Soil into its farmland; vanilla wheat and Jugcraft's corn planted on that farmland; the extra growth Rich Soil and
 * its farmland give the plant on them; the farmland keeping moist by water and drying back into Rich Soil; Organic
 * Compost rotting into Rich Soil, faster when wet; the storage recipes; the basket taking in items dropped into it,
 * opening its screen, reading on a comparator and spilling when broken; and the Bag of Corn Kernels turned to its placer.
 */
public class SoilGameTests {
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

	private static int dropped(GameTestHelper helper, Item item) {
		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 8, 8).inflate(2.0);
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item)).stream()
				.mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	/** Random-ticks the block at {@code pos} {@code times} times. */
	private static void tick(GameTestHelper helper, BlockPos pos, int times) {
		ServerLevel level = helper.getLevel();
		for (int i = 0; i < times; i++) {
			helper.getBlockState(pos).randomTick(level, helper.absolutePos(pos), level.getRandom());
		}
	}

	// ---------------------------------------------------------------- rich soil and its farmland

	/** A hoe tills Rich Soil with air above into dry Rich Soil Farmland and wears a use; with a block on it, it does not. */
	@GameTest(maxTicks = 20)
	public void hoeTillsRichSoil(GameTestHelper helper) {
		floor(helper);
		BlockPos open = new BlockPos(2, 1, 2);
		BlockPos covered = new BlockPos(5, 1, 2);
		helper.setBlock(open, block("rich_soil"));
		helper.setBlock(covered, block("rich_soil"));
		helper.setBlock(covered.above(), Blocks.STONE);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(Items.IRON_HOE));
		useTop(helper, player, open);
		useTop(helper, player, covered);
		BlockState tilled = helper.getBlockState(open);
		helper.assertTrue(tilled.is(block("rich_soil_farmland")) && tilled.getValue(RichFarmlandBlock.MOISTURE) == 0,
				"The hoe tills Rich Soil into dry farmland: " + tilled);
		helper.assertTrue(helper.getBlockState(covered).is(block("rich_soil")), "Covered, it stays Rich Soil");
		helper.assertTrue(player.getMainHandItem().getDamageValue() == 1, "Tilling wears the hoe a use");
		helper.succeed();
	}

	/** Vanilla wheat seeds and Jugcraft's corn kernels both plant on Rich Soil Farmland, as on farmland. */
	@GameTest(maxTicks = 20)
	public void cropsPlantOnRichFarmland(GameTestHelper helper) {
		floor(helper);
		BlockPos wheat = new BlockPos(2, 1, 2);
		BlockPos corn = new BlockPos(5, 1, 2);
		helper.setBlock(wheat, block("rich_soil_farmland"));
		helper.setBlock(corn, block("rich_soil_farmland"));
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(Items.WHEAT_SEEDS));
		useTop(helper, player, wheat);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("corn_kernels")));
		useTop(helper, player, corn);
		helper.assertBlockPresent(Blocks.WHEAT, wheat.above());
		helper.assertBlockPresent(JugcraftAgriculture.TALL_CROPS.get(TallCrop.CORN), corn.above());
		helper.succeed();
	}

	/**
	 * The plant on Rich Soil or its farmland gets an extra random tick for each of the soil's own: random ticks of the
	 * soil alone ripen wheat on moist rich farmland and a sweet berry bush on rich soil.
	 */
	@GameTest(maxTicks = 60)
	public void richSoilSpeedsWhatGrowsOnIt(GameTestHelper helper) {
		BlockPos farmland = new BlockPos(2, 1, 2);
		BlockPos soil = new BlockPos(5, 1, 2);
		// The light and water first, so setting them down never asks the plants whether they can stay.
		helper.setBlock(farmland, block("rich_soil_farmland").defaultBlockState().setValue(RichFarmlandBlock.MOISTURE, 7));
		helper.setBlock(farmland.west(), Blocks.WATER);
		helper.setBlock(soil, block("rich_soil"));
		helper.setBlock(farmland.above(2), Blocks.GLOWSTONE);
		helper.setBlock(soil.above(2), Blocks.GLOWSTONE);
		helper.setBlock(farmland.above(), Blocks.WHEAT);
		helper.setBlock(soil.above(), Blocks.SWEET_BERRY_BUSH);
		// Give the light engine time to light the plants, then tick only the soil.
		helper.runAtTickTime(40, () -> {
			tick(helper, farmland, 400);
			tick(helper, soil, 400);
			BlockState wheat = helper.getBlockState(farmland.above());
			BlockState bush = helper.getBlockState(soil.above());
			helper.assertTrue(wheat.is(Blocks.WHEAT) && ((CropBlock) Blocks.WHEAT).isMaxAge(wheat), "The farmland's ticks ripen its wheat: " + wheat);
			helper.assertTrue(bush.getValue(SweetBerryBushBlock.AGE) == SweetBerryBushBlock.MAX_AGE, "The soil's ticks ripen its bush: " + bush);
			helper.succeed();
		});
	}

	/**
	 * Rich Soil Farmland keeps moist by water; away from it, it dries a step at a time and then, bare, turns back into Rich
	 * Soil (not dirt); a solid block set on it presses it back into Rich Soil too.
	 */
	@GameTest(maxTicks = 20)
	public void richFarmlandDriesBackToRichSoil(GameTestHelper helper) {
		floor(helper);
		BlockPos wet = new BlockPos(1, 1, 1);
		BlockPos dry = new BlockPos(6, 1, 6);
		BlockPos pressed = new BlockPos(6, 1, 1);
		helper.setBlock(wet, block("rich_soil_farmland"));
		helper.setBlock(wet.east(), Blocks.WATER);
		tick(helper, wet, 1);
		helper.assertTrue(helper.getBlockState(wet).getValue(RichFarmlandBlock.MOISTURE) == RichFarmlandBlock.MAX_MOISTURE, "Water moistens it");
		// A roof two blocks up keeps any rain off without pressing on it.
		helper.setBlock(dry.above(2), Blocks.STONE);
		helper.setBlock(dry, block("rich_soil_farmland").defaultBlockState().setValue(RichFarmlandBlock.MOISTURE, 1));
		tick(helper, dry, 1);
		helper.assertTrue(helper.getBlockState(dry).getValue(RichFarmlandBlock.MOISTURE) == 0, "Away from water it dries a step");
		tick(helper, dry, 1);
		helper.assertTrue(helper.getBlockState(dry).is(block("rich_soil")), "Dry and bare, it is Rich Soil again");
		helper.setBlock(pressed, block("rich_soil_farmland"));
		helper.setBlock(pressed.above(), Blocks.STONE);
		helper.assertTrue(helper.getBlockState(pressed).is(block("rich_soil")), "A block set on it presses it back into Rich Soil");
		helper.succeed();
	}

	/** Corn grown three blocks tall (a solid wall) on Rich Soil Farmland keeps its farmland, as on farmland. */
	@GameTest(maxTicks = 60)
	public void tallCornKeepsRichFarmland(GameTestHelper helper) {
		BlockPos soil = new BlockPos(2, 1, 2);
		helper.setBlock(soil, block("rich_soil_farmland"));
		helper.setBlock(soil.above(), JugcraftAgriculture.TALL_CROPS.get(TallCrop.CORN));
		helper.assertTrue(JugcraftAgriculture.TALL_CROPS.get(TallCrop.CORN).growTo(helper.getLevel(), helper.absolutePos(soil.above()), 7),
				"Corn grows to ripe on rich farmland");
		helper.runAtTickTime(40, () -> {
			helper.assertBlockPresent(block("rich_soil_farmland"), soil);
			for (int section = 1; section <= 3; section++) {
				helper.assertBlockPresent(JugcraftAgriculture.TALL_CROPS.get(TallCrop.CORN), soil.above(section));
			}
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- compost

	/** Wet Organic Compost turns a stage at every random tick and after its last is Rich Soil; a comparator reads its stage. */
	@GameTest(maxTicks = 20)
	public void compostRotsIntoRichSoil(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos wet = new BlockPos(2, 2, 2);
		BlockPos dry = new BlockPos(5, 2, 5);
		helper.setBlock(wet, block("organic_compost"));
		helper.setBlock(wet.east(), Blocks.WATER);
		helper.setBlock(dry, block("organic_compost"));
		BlockPos absolute = helper.absolutePos(wet);
		helper.assertTrue(level.getBlockState(absolute).getAnalogOutputSignal(level, absolute, Direction.NORTH) == 4, "Fresh compost reads 4");
		for (int stage = 1; stage <= OrganicCompostBlock.LAST_STAGE; stage++) {
			tick(helper, wet, 1);
			helper.assertTrue(helper.getBlockState(wet).getValue(OrganicCompostBlock.COMPOSTING) == stage, "Wet, it turns a stage a tick");
		}
		tick(helper, wet, 1);
		helper.assertTrue(helper.getBlockState(wet).is(block("rich_soil")), "After its last stage it is Rich Soil");
		tick(helper, dry, 100);
		helper.assertTrue(helper.getBlockState(dry).is(block("rich_soil")), "Dry, it gets there too, more slowly");
		helper.succeed();
	}

	// ---------------------------------------------------------------- storage

	/** Every crate, the kernel bag, the compost and the baskets have their recipes, and each crate and bag its unpacking. */
	@GameTest(maxTicks = 20)
	public void storageRecipesExist(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("beetroot_crate", "cabbage_crate", "carrot_crate", "corn_crate", "onion_crate", "potato_crate", "tomato_crate",
				"corn_kernel_bag", "beetroot_from_crate", "cabbage_from_crate", "carrot_from_crate", "corn_from_crate", "onion_from_crate",
				"potato_from_crate", "tomato_from_crate", "corn_kernels_from_bag", "organic_compost", "wooden_basket", "bamboo_basket")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), id + " has a recipe");
		}
		helper.succeed();
	}

	/**
	 * A basket takes in items dropped into it, a stack at a time; a comparator then reads it; using it opens its 3 by 3
	 * screen; broken, it spills what it holds.
	 */
	@GameTest(maxTicks = 60)
	public void basketTakesInWhatFallsIntoIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, block("wooden_basket"));
		BlockPos absolute = helper.absolutePos(pos);
		ItemEntity carrots = new ItemEntity(level, absolute.getX() + 0.5, absolute.getY() + 0.5, absolute.getZ() + 0.5, new ItemStack(Items.CARROT, 5));
		carrots.setDeltaMovement(Vec3.ZERO);
		level.addFreshEntity(carrots);
		helper.runAtTickTime(20, () -> {
			BasketBlockEntity basket = helper.getBlockEntity(pos, BasketBlockEntity.class);
			helper.assertTrue(basket.countItem(Items.CARROT) == 5 && !carrots.isAlive(), "The basket takes in the carrots: " + basket.countItem(Items.CARROT));
			helper.assertTrue(level.getBlockState(absolute).getAnalogOutputSignal(level, absolute, Direction.NORTH) > 0, "A comparator reads it");
			ServerPlayer player = player(helper, new BlockPos(3, 2, 6), ItemStack.EMPTY);
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
			player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
			helper.assertTrue(player.containerMenu instanceof DispenserMenu, "Using it opens its 3 by 3 screen");
			player.closeContainer();
			level.destroyBlock(absolute, true);
		});
		helper.runAtTickTime(24, () -> {
			helper.assertTrue(dropped(helper, Items.CARROT) == 5 && dropped(helper, item("wooden_basket")) == 1, "Broken, it spills its carrots");
			helper.succeed();
		});
	}

	/** The Bag of Corn Kernels turns its tied side to the player who sets it down, as the Bag of Rice does. */
	@GameTest(maxTicks = 20)
	public void kernelBagFacesItsPlacer(GameTestHelper helper) {
		floor(helper);
		BlockPos ground = new BlockPos(3, 1, 3);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("corn_kernel_bag")));
		useTop(helper, player, ground);
		BlockState bag = helper.getBlockState(ground.above());
		helper.assertTrue(bag.is(block("corn_kernel_bag")) && bag.getValue(HorizontalDirectionalBlock.FACING) == Direction.SOUTH,
				"The bag faces the player: " + bag);
		helper.succeed();
	}
}
