package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.MushroomColonyBlock;
import io.github.jimbozoomer.jugcraft.agriculture.RottenTomato;
import io.github.jimbozoomer.jugcraft.agriculture.TallCrop;
import io.github.jimbozoomer.jugcraft.agriculture.TallCropBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TomatoVineBlock;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
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
 * In-game tests for the garden crops in the owner's art (slice 7a, tools/garden.py): a ripe tomato vine going over with
 * time, giving rotten tomatoes when picked or broken and growing back fresh; a thrown Rotten Tomato bumping a cow without
 * hurting it and bursting; brown and red mushrooms planting their colonies on Rich Soil (and not on plain dirt); colonies
 * growing in the shade but not under the open sky, bone meal growing them anywhere; shears and knives picking grown
 * colonies; broken colonies giving back their mushroom; and the wild carrots, potatoes and beetroots giving vanilla's crops.
 */
public class GardenGameTests {
	private static final BlockPos SOIL = new BlockPos(2, 1, 2);
	private static final BlockPos CROP = SOIL.above();

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static TomatoVineBlock vine() {
		return (TomatoVineBlock) JugcraftAgriculture.TALL_CROPS.get(TallCrop.TOMATO);
	}

	private static MushroomColonyBlock colony(String id) {
		return (MushroomColonyBlock) block(id);
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	/** Moist farmland at {@link #SOIL}, two trellises on it, and a tomato in the lower one grown ripe. */
	private static void ripeTomato(GameTestHelper helper) {
		helper.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
		helper.setBlock(CROP, block("trellis"));
		helper.setBlock(CROP.above(), block("trellis"));
		helper.setBlock(CROP, vine().defaultBlockState());
		helper.assertTrue(vine().growTo(helper.getLevel(), helper.absolutePos(CROP), TallCropBlock.MAX_AGE), "The tomato grows ripe");
	}

	/** A survival player standing at {@code standAt}, holding {@code held}. */
	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, ItemStack held) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
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

	private static int dropped(GameTestHelper helper, Item item) {
		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 8, 8).inflate(2.0);
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item)).stream()
				.mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	private static void clearDrops(GameTestHelper helper) {
		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 8, 8).inflate(2.0);
		helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> true).forEach(ItemEntity::discard);
	}

	/** Random-ticks the block at {@code pos} {@code times} times. */
	private static void tick(GameTestHelper helper, BlockPos pos, int times) {
		ServerLevel level = helper.getLevel();
		for (int i = 0; i < times; i++) {
			helper.getBlockState(pos).randomTick(level, helper.absolutePos(pos), level.getRandom());
		}
	}

	private static boolean overripe(GameTestHelper helper, BlockPos pos) {
		BlockState state = helper.getBlockState(pos);
		return state.is(vine()) && TomatoVineBlock.isOverripe(state);
	}

	/** Stone round {@code at} and over it, so no light reaches it. */
	private static void shut(GameTestHelper helper, BlockPos at) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx != 0 || dz != 0) {
					helper.setBlock(at.offset(dx, 0, dz), Blocks.STONE);
				}
				helper.setBlock(at.offset(dx, 1, dz), Blocks.STONE);
			}
		}
	}

	// ---------------------------------------------------------------- the tomato vine that goes over

	/**
	 * A ripe vine keeps ticking until it goes over, every block of it at once; gone over, it stops. Picked, it gives rotten
	 * tomatoes (as many as tomatoes) and grows back from its regrowth age, fresh.
	 */
	@GameTest(maxTicks = 40)
	public void ripeTomatoesGoOverAndRot(GameTestHelper helper) {
		ripeTomato(helper);
		helper.assertTrue(helper.getBlockState(CROP).isRandomlyTicking(), "A ripe vine keeps ticking, to go over");
		helper.assertTrue(!overripe(helper, CROP) && !overripe(helper, CROP.above()), "A vine grows ripe fresh");
		// One in OVERRIPE_CHANCE: 300 ticks all but certainly turn it.
		tick(helper, CROP, 300);
		helper.assertTrue(overripe(helper, CROP) && overripe(helper, CROP.above()), "Left on the vine, every block of it goes over");
		helper.assertTrue(!helper.getBlockState(CROP).isRandomlyTicking(), "Gone over, it waits to be picked");
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.useBlock(CROP.above(), player);
		BlockState picked = helper.getBlockState(CROP);
		helper.assertTrue(picked.getValue(TallCropBlock.AGE) == TallCrop.TOMATO.pickReset && !overripe(helper, CROP) && !overripe(helper, CROP.above()),
				"Picked, it goes back to its regrowth age, fresh: " + picked);
		int rotten = dropped(helper, item(TomatoVineBlock.ROTTEN));
		helper.assertTrue(rotten >= TallCrop.TOMATO.pickMin && rotten <= TallCrop.TOMATO.pickMax && dropped(helper, item("tomato")) == 0,
				"Picking a vine gone over gives " + TallCrop.TOMATO.pickMin + "-" + TallCrop.TOMATO.pickMax + " rotten tomatoes and no tomato: " + rotten);
		helper.succeed();
	}

	/** Broken gone over, a vine drops rotten tomatoes (no fresh ones), its seeds and both trellises. */
	@GameTest(maxTicks = 40)
	public void brokenOverripeVinesDropRottenTomatoes(GameTestHelper helper) {
		ripeTomato(helper);
		helper.assertTrue(vine().goOver(helper.getLevel(), helper.absolutePos(CROP)), "A ripe vine can go over");
		helper.assertTrue(!vine().goOver(helper.getLevel(), helper.absolutePos(CROP.above())), "Only its bottom block turns the vine");
		helper.getLevel().destroyBlock(helper.absolutePos(CROP.above()), true);
		helper.succeedWhen(() -> {
			helper.assertBlockNotPresent(vine(), CROP);
			helper.assertTrue(dropped(helper, item(TomatoVineBlock.ROTTEN)) >= TallCrop.TOMATO.pickMin, "Rotten tomatoes drop");
			helper.assertTrue(dropped(helper, item("tomato")) == 0, "No fresh tomato drops from a vine gone over");
			helper.assertTrue(dropped(helper, item("tomato_seeds")) >= 1 && dropped(helper, item("trellis")) == 2,
					"Its seeds and both trellises come back");
		});
	}

	/** A vine that is not ripe never goes over. */
	@GameTest(maxTicks = 20)
	public void unripeVinesDoNotGoOver(GameTestHelper helper) {
		helper.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
		helper.setBlock(CROP, block("trellis"));
		helper.setBlock(CROP.above(), block("trellis"));
		helper.setBlock(CROP, vine().defaultBlockState());
		helper.assertTrue(vine().growTo(helper.getLevel(), helper.absolutePos(CROP), TallCropBlock.MAX_AGE - 1), "The tomato grows near ripe");
		helper.assertTrue(!vine().goOver(helper.getLevel(), helper.absolutePos(CROP)) && !overripe(helper, CROP), "An unripe vine cannot go over");
		helper.succeed();
	}

	// ---------------------------------------------------------------- the rotten tomato

	/** A rotten tomato dropped on a cow bursts on it: the cow is hit (by the thrower) but not hurt, and nothing is left. */
	@GameTest(maxTicks = 60)
	public void rottenTomatoesBumpWithoutHurting(GameTestHelper helper) {
		floor(helper);
		Mob cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(4, 2, 4));
		ServerPlayer thrower = player(helper, new BlockPos(1, 2, 1), ItemStack.EMPTY);
		RottenTomato tomato = new RottenTomato(helper.getLevel(), thrower, new ItemStack(item(TomatoVineBlock.ROTTEN)));
		tomato.setPos(cow.getX(), cow.getY() + 3.0, cow.getZ());
		tomato.setDeltaMovement(0.0, -1.0, 0.0);
		helper.getLevel().addFreshEntity(tomato);
		helper.succeedWhen(() -> {
			helper.assertTrue(tomato.isRemoved(), "The tomato bursts where it lands");
			helper.assertTrue(cow.isAlive() && cow.getHealth() == cow.getMaxHealth(), "It hurts nobody: " + cow.getHealth());
			helper.assertTrue(cow.getLastHurtByMob() == thrower, "It hit the cow, from its thrower");
			helper.assertTrue(dropped(helper, item(TomatoVineBlock.ROTTEN)) == 0, "Nothing is left of it");
		});
	}

	/** Thrown from the hand, a rotten tomato is used up and flies. */
	@GameTest(maxTicks = 20)
	public void rottenTomatoesAreThrown(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(2, 2, 2), new ItemStack(item(TomatoVineBlock.ROTTEN), 3));
		player.setXRot(-30.0F);
		player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
		helper.assertTrue(player.getMainHandItem().getCount() == 2, "Throwing uses one");
		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 8, 8).inflate(4.0);
		helper.assertTrue(!helper.getLevel().getEntitiesOfClass(RottenTomato.class, area).isEmpty(), "A rotten tomato flies");
		helper.succeed();
	}

	// ---------------------------------------------------------------- mushroom colonies

	/** A brown or red mushroom used on Rich Soil plants its colony there, young, using the mushroom; on dirt it plants none. */
	@GameTest(maxTicks = 20)
	public void mushroomsPlantColoniesOnRichSoil(GameTestHelper helper) {
		floor(helper);
		BlockPos brown = new BlockPos(1, 1, 1);
		BlockPos red = new BlockPos(4, 1, 1);
		BlockPos dirt = new BlockPos(6, 1, 4);
		helper.setBlock(brown, block("rich_soil"));
		helper.setBlock(red, block("rich_soil"));
		helper.setBlock(dirt, Blocks.DIRT);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 5), new ItemStack(Items.BROWN_MUSHROOM, 2));
		useTop(helper, player, brown);
		helper.assertTrue(player.getMainHandItem().getCount() == 1, "Planting uses the mushroom");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RED_MUSHROOM, 2));
		useTop(helper, player, red);
		useTop(helper, player, dirt);
		BlockState planted = helper.getBlockState(brown.above());
		helper.assertTrue(planted.is(colony("brown_mushroom_colony")) && planted.getValue(MushroomColonyBlock.AGE) == 0,
				"A brown mushroom plants a young brown colony on Rich Soil: " + planted);
		helper.assertBlockPresent(colony("red_mushroom_colony"), red.above());
		helper.assertTrue(!(helper.getBlockState(dirt.above()).getBlock() instanceof MushroomColonyBlock), "On plain dirt no colony grows");
		helper.assertTrue(JugcraftAgriculture.colony(Items.BROWN_MUSHROOM) == colony("brown_mushroom_colony")
				&& JugcraftAgriculture.colony(Items.RED_MUSHROOM) == colony("red_mushroom_colony") && JugcraftAgriculture.colony(Items.WHEAT) == null,
				"Each mushroom plants its own colony");
		helper.succeed();
	}

	/**
	 * A colony shut away from the light grows through its stages; one under the open sky does not, though bone meal grows it
	 * a stage anywhere. A grown colony stops ticking.
	 */
	@GameTest(maxTicks = 80)
	public void coloniesGrowInTheShade(GameTestHelper helper) {
		floor(helper);
		BlockPos dark = new BlockPos(2, 1, 2);
		BlockPos open = new BlockPos(6, 1, 6);
		helper.setBlock(dark, block("rich_soil"));
		helper.setBlock(open, block("rich_soil"));
		helper.setBlock(dark.above(), colony("brown_mushroom_colony"));
		helper.setBlock(open.above(), colony("red_mushroom_colony"));
		shut(helper, dark.above());
		// Give the light engine time to darken the shut one.
		helper.runAtTickTime(40, () -> {
			ServerLevel level = helper.getLevel();
			int shade = level.getRawBrightness(helper.absolutePos(dark.above()), 0);
			int sky = level.getRawBrightness(helper.absolutePos(open.above()), 0);
			helper.assertTrue(shade <= MushroomColonyBlock.MAX_LIGHT && sky > MushroomColonyBlock.MAX_LIGHT,
					"Stone shuts out the light (" + shade + "); the sky lights the other (" + sky + ")");
			tick(helper, dark.above(), 200);
			tick(helper, open.above(), 200);
			BlockState grown = helper.getBlockState(dark.above());
			helper.assertTrue(MushroomColonyBlock.isGrown(grown) && !grown.isRandomlyTicking(), "In the shade it grows, and grown it rests: " + grown);
			helper.assertTrue(helper.getBlockState(open.above()).getValue(MushroomColonyBlock.AGE) == 0, "Under the open sky it does not grow");
			ServerPlayer player = player(helper, new BlockPos(6, 2, 3), new ItemStack(Items.BONE_MEAL, 4));
			BlockPos absolute = helper.absolutePos(open.above());
			player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
			helper.assertTrue(helper.getBlockState(open.above()).getValue(MushroomColonyBlock.AGE) == 1, "Bone meal grows it a stage, light or not");
			helper.succeed();
		});
	}

	/** Shears or a knife pick a grown colony: 2-3 of its mushroom, back to its regrowth stage, a use off the tool. A bare hand does not. */
	@GameTest(maxTicks = 20)
	public void grownColoniesArePicked(GameTestHelper helper) {
		floor(helper);
		BlockPos soil = new BlockPos(3, 1, 3);
		helper.setBlock(soil, block("rich_soil"));
		BlockState grown = colony("brown_mushroom_colony").defaultBlockState().setValue(MushroomColonyBlock.AGE, MushroomColonyBlock.MAX_AGE);
		helper.setBlock(soil.above(), grown);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), ItemStack.EMPTY);
		BlockPos absolute = helper.absolutePos(soil.above());
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
		player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
		helper.assertTrue(MushroomColonyBlock.isGrown(helper.getBlockState(soil.above())) && dropped(helper, Items.BROWN_MUSHROOM) == 0,
				"A bare hand picks nothing");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
		player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
		int picked = dropped(helper, Items.BROWN_MUSHROOM);
		helper.assertTrue(picked >= MushroomColonyBlock.PICK_MIN && picked <= MushroomColonyBlock.PICK_MAX,
				"Shears pick " + MushroomColonyBlock.PICK_MIN + "-" + MushroomColonyBlock.PICK_MAX + " mushrooms: " + picked);
		helper.assertTrue(helper.getBlockState(soil.above()).getValue(MushroomColonyBlock.AGE) == MushroomColonyBlock.PICK_RESET,
				"Picked, the colony goes back to its regrowth stage");
		helper.assertTrue(player.getMainHandItem().getDamageValue() == 1, "Picking wears the shears a use");
		clearDrops(helper);
		helper.setBlock(soil.above(), grown);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("iron_knife")));
		player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
		helper.assertTrue(dropped(helper, Items.BROWN_MUSHROOM) >= MushroomColonyBlock.PICK_MIN
				&& helper.getBlockState(soil.above()).getValue(MushroomColonyBlock.AGE) == MushroomColonyBlock.PICK_RESET, "A knife picks it too");
		helper.succeed();
	}

	/** Broken young, a colony gives back its mushroom; grown, 2-3 more; and without its soil it pops off. */
	@GameTest(maxTicks = 40)
	public void brokenColoniesGiveBackTheirMushroom(GameTestHelper helper) {
		floor(helper);
		BlockPos young = new BlockPos(1, 1, 1);
		BlockPos grown = new BlockPos(5, 1, 1);
		BlockPos loose = new BlockPos(3, 1, 5);
		helper.setBlock(young, block("rich_soil"));
		helper.setBlock(grown, block("rich_soil"));
		helper.setBlock(loose, block("rich_soil"));
		helper.setBlock(young.above(), colony("red_mushroom_colony"));
		helper.setBlock(grown.above(), colony("brown_mushroom_colony").defaultBlockState().setValue(MushroomColonyBlock.AGE, MushroomColonyBlock.MAX_AGE));
		helper.setBlock(loose.above(), colony("brown_mushroom_colony"));
		helper.getLevel().destroyBlock(helper.absolutePos(young.above()), true);
		helper.getLevel().destroyBlock(helper.absolutePos(grown.above()), true);
		helper.setBlock(loose, Blocks.AIR);
		helper.succeedWhen(() -> {
			helper.assertBlockNotPresent(colony("brown_mushroom_colony"), loose.above());
			helper.assertTrue(dropped(helper, Items.RED_MUSHROOM) == 1, "A young colony gives back its mushroom");
			int brown = dropped(helper, Items.BROWN_MUSHROOM);
			helper.assertTrue(brown >= 2 + MushroomColonyBlock.PICK_MIN && brown <= 2 + MushroomColonyBlock.PICK_MAX,
					"A grown colony gives its mushroom and " + MushroomColonyBlock.PICK_MIN + "-" + MushroomColonyBlock.PICK_MAX
							+ " more, and the loose one its mushroom: " + brown);
		});
	}

	// ---------------------------------------------------------------- wild roots

	/** The wild carrots, potatoes and beetroots give vanilla's carrots, potatoes and beetroot seeds, 1-2 each. */
	@GameTest(maxTicks = 40)
	public void wildRootsGiveVanillaCrops(GameTestHelper helper) {
		String[] wild = {"wild_carrots", "wild_potatoes", "wild_beetroots"};
		Item[] gives = {Items.CARROT, Items.POTATO, Items.BEETROOT_SEEDS};
		for (int i = 0; i < wild.length; i++) {
			BlockPos pos = new BlockPos(1 + 3 * i, 1, 2);
			helper.setBlock(pos, Blocks.GRASS_BLOCK);
			helper.setBlock(pos.above(), block(wild[i]));
			helper.getLevel().destroyBlock(helper.absolutePos(pos.above()), true);
		}
		helper.succeedWhen(() -> {
			for (int i = 0; i < wild.length; i++) {
				int count = dropped(helper, gives[i]);
				helper.assertTrue(count >= 1 && count <= 2, wild[i] + " gives 1-2 " + gives[i] + ": " + count);
			}
		});
	}
}
