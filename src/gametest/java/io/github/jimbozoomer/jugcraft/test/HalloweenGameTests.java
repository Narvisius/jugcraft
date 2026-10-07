package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.AttachedGiantPumpkinVineBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CarvingFace;
import io.github.jimbozoomer.jugcraft.agriculture.CarvingTemplates;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotRecipe;
import io.github.jimbozoomer.jugcraft.agriculture.GiantPumpkinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GiantPumpkinBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.GiantPumpkinData;
import io.github.jimbozoomer.jugcraft.agriculture.GiantPumpkinVineBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GourdCanteenItem;
import io.github.jimbozoomer.jugcraft.agriculture.HarvestScaleBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCarving;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCarvings;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCarvings.Result;
import io.github.jimbozoomer.jugcraft.agriculture.ScarecrowBlock;
import io.github.jimbozoomer.jugcraft.agriculture.ScarecrowBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.TallCrop;
import io.github.jimbozoomer.jugcraft.agriculture.TallCropBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TallDecorationBlock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the Halloween harvest: the giant pumpkin and its 48x48 carving, scooping, the Harvest
 * Scale, stencils, heirloom pumpkins, the scarecrow, ornamental corn and corn shocks, treats, the gourd
 * canteen and birdhouse, and mums.
 */
public class HalloweenGameTests {
	/** The vine stands on farmland here; its fruit grows east, onto grass from x 2 to 4 and z 1 to 3. */
	private static final BlockPos VINE = new BlockPos(1, 2, 2);
	private static final BlockPos PUMPKIN = new BlockPos(2, 1, 2);
	private static final int[] CLASSIC = CarvingTemplates.ALL.get(0).face();

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static BlockHitResult hit(GameTestHelper helper, BlockPos pos, Direction side) {
		BlockPos absolute = helper.absolutePos(pos);
		return new BlockHitResult(Vec3.atCenterOf(absolute).relative(side, 0.5), side, absolute, false);
	}

	private static Player holding(GameTestHelper helper, ItemStack stack) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		return player;
	}

	private static ServerPlayer serverPlayer(GameTestHelper helper, BlockPos standAt, ItemStack held) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	private static int itemsAround(GameTestHelper helper, BlockPos pos, Item item) {
		int count = 0;
		for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(4.0))) {
			if (entity.getItem().is(item)) {
				count += entity.getItem().getCount();
			}
		}
		return count;
	}

	private static int count(List<ItemStack> drops, Item item) {
		return drops.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
	}

	private static boolean recipeLoads(GameTestHelper helper, String id) {
		return helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent();
	}

	// ---------------------------------------------------------------- the giant pumpkin

	/** A vine on moist farmland that has set its fruit east of it, on grass with room to grow; returns the fruit's master. */
	private static GiantPumpkinBlockEntity seedling(GameTestHelper helper) {
		helper.setBlock(VINE.below(), Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
		for (int x = 2; x <= 4; x++) {
			for (int z = 1; z <= 3; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
			}
		}
		GiantPumpkinVineBlock vine = (GiantPumpkinVineBlock) block("giant_pumpkin_vine");
		helper.setBlock(VINE, vine.defaultBlockState().setValue(GiantPumpkinVineBlock.AGE, GiantPumpkinVineBlock.MAX_AGE));
		helper.assertTrue(vine.growFruit(helper.getLevel(), helper.absolutePos(VINE), Direction.EAST), "A grown vine should set a fruit");
		return helper.getBlockEntity(VINE.east(), GiantPumpkinBlockEntity.class);
	}

	/** The master of the giant pumpkin covering {@code pos}. */
	private static GiantPumpkinBlockEntity master(GameTestHelper helper, BlockPos pos) {
		GiantPumpkinBlockEntity master = GiantPumpkinBlock.master(helper.getLevel(), helper.absolutePos(pos), helper.getBlockState(pos));
		if (master == null) {
			throw helper.assertionException("No giant pumpkin at " + pos + ": " + helper.getBlockState(pos));
		}
		return master;
	}

	/** A full-grown giant pumpkin (x 2-4, y 2-4, z 1-3) still held by its vine. */
	private static GiantPumpkinBlockEntity fullGrown(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		seedling(helper).feed(level, GiantPumpkinBlockEntity.GROW_TO_TWO, level.getRandom());
		master(helper, VINE.east()).feed(level, GiantPumpkinBlockEntity.GROW_TO_THREE - GiantPumpkinBlockEntity.GROW_TO_TWO, level.getRandom());
		GiantPumpkinBlockEntity master = master(helper, VINE.east());
		helper.assertTrue(master.fullGrown(), "The pumpkin should be full grown");
		return master;
	}

	/** Giant Pumpkin Seeds plant a vine on farmland, not on grass. */
	@GameTest
	public void giantSeedsPlantAVine(GameTestHelper helper) {
		BlockPos soil = new BlockPos(2, 1, 2);
		BlockPos grass = new BlockPos(4, 1, 2);
		helper.setBlock(soil, Blocks.FARMLAND);
		helper.setBlock(grass, Blocks.GRASS_BLOCK);
		Player player = holding(helper, new ItemStack(item("giant_pumpkin_seeds"), 2));
		helper.useBlock(soil, player, hit(helper, soil, Direction.UP));
		helper.useBlock(grass, player, hit(helper, grass, Direction.UP));
		helper.assertBlockPresent(block("giant_pumpkin_vine"), soil.above());
		helper.assertBlockNotPresent(block("giant_pumpkin_vine"), grass.above());
		helper.succeed();
	}

	/**
	 * A grown vine sets one small fruit and bends to it; ticks grow it 1 point plus 1 for moist farmland plus
	 * 1 when watered; it swells to 2x2x2 and then 3x3x3 away from the vine, centred on it, with its master
	 * moving to the lowest north-west corner, and a starting weight when full grown.
	 */
	@GameTest
	public void giantPumpkinGrowsToThreeBlocksWide(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		GiantPumpkinBlockEntity fruit = seedling(helper);
		BlockState attached = helper.getBlockState(VINE);
		helper.assertTrue(attached.is(block("attached_giant_pumpkin_vine")) && attached.getValue(AttachedGiantPumpkinVineBlock.FACING) == Direction.EAST,
				"The vine should bend east to its fruit: " + attached);
		helper.assertTrue(fruit.size() == 1 && fruit.weight() == 0, "A new fruit is one block and not weighed yet");
		fruit.tick(level, level.getRandom());
		helper.assertTrue(fruit.points() == 2, "Moist farmland: 2 points a tick, got " + fruit.points());
		fruit.water(level);
		fruit.tick(level, level.getRandom());
		helper.assertTrue(fruit.points() == 5, "Watered too: 3 points a tick, got " + fruit.points());

		fruit.feed(level, GiantPumpkinBlockEntity.GROW_TO_TWO, level.getRandom());
		for (int x = 2; x <= 3; x++) {
			for (int y = 2; y <= 3; y++) {
				for (int z = 2; z <= 3; z++) {
					BlockState state = helper.getBlockState(new BlockPos(x, y, z));
					helper.assertTrue(state.is(block("giant_pumpkin")) && state.getValue(GiantPumpkinBlock.SIZE) == 2,
							"Expected a 2x2x2 pumpkin at " + x + "," + y + "," + z + ": " + state);
				}
			}
		}
		master(helper, VINE.east()).feed(level, GiantPumpkinBlockEntity.GROW_TO_THREE - GiantPumpkinBlockEntity.GROW_TO_TWO, level.getRandom());
		for (int x = 2; x <= 4; x++) {
			for (int y = 2; y <= 4; y++) {
				for (int z = 1; z <= 3; z++) {
					BlockState state = helper.getBlockState(new BlockPos(x, y, z));
					helper.assertTrue(state.is(block("giant_pumpkin")) && state.getValue(GiantPumpkinBlock.SIZE) == 3,
							"Expected a 3x3x3 pumpkin at " + x + "," + y + "," + z + ": " + state);
				}
			}
		}
		GiantPumpkinBlockEntity master = helper.getBlockEntity(new BlockPos(2, 2, 1), GiantPumpkinBlockEntity.class);
		helper.assertTrue(master.fullGrown() && master.weight() >= GiantPumpkinBlockEntity.START_WEIGHT
				&& master.weight() <= GiantPumpkinBlockEntity.START_WEIGHT + 20, "Full grown it should weigh 100-120 kg: " + master.weight());
		helper.assertTrue(master.attached(level), "The vine should still hold it after it grew");
		int before = master.weight();
		master.feed(level, 10, level.getRandom());
		helper.assertTrue(master.weight() == before + 10 * GiantPumpkinBlockEntity.WEIGHT_PER_POINT, "Full grown, points become weight");
		helper.succeed();
	}

	/** A giant pumpkin waits when something is in the way, and stops growing when cut from its vine. */
	@GameTest
	public void giantPumpkinNeedsRoomAndItsVine(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		GiantPumpkinBlockEntity fruit = seedling(helper);
		helper.setBlock(new BlockPos(3, 3, 3), Blocks.STONE);
		helper.setBlock(new BlockPos(3, 3, 1), Blocks.STONE);
		fruit.feed(level, GiantPumpkinBlockEntity.GROW_TO_TWO, level.getRandom());
		helper.assertTrue(helper.getBlockState(VINE.east()).getValue(GiantPumpkinBlock.SIZE) == 1, "Stone in the way on both sides: no room");
		helper.setBlock(new BlockPos(3, 3, 1), Blocks.AIR);
		fruit.feed(level, 1, level.getRandom());
		GiantPumpkinBlockEntity grown = master(helper, VINE.east());
		helper.assertTrue(grown.size() == 2 && helper.getBlockState(new BlockPos(2, 2, 1)).is(block("giant_pumpkin")),
				"With room on the north side, it grows that way");

		helper.setBlock(VINE, Blocks.AIR);
		int points = grown.points();
		grown.tick(level, level.getRandom());
		helper.assertTrue(grown.points() == points && !grown.attached(level), "Cut from its vine, it no longer grows");
		helper.succeed();
	}

	/**
	 * Bone meal alone grows a giant pumpkin all the way: the vine to full growth, then its fruit (bone meal on a full-grown
	 * vine sets it), then, given to the vine or to the pumpkin, from one block to two and three wide, and full grown it
	 * keeps putting on weight.
	 */
	@GameTest
	public void boneMealGrowsAGiantPumpkin(GameTestHelper helper) {
		helper.setBlock(VINE.below(), Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
		for (int x = 2; x <= 4; x++) {
			for (int z = 1; z <= 3; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
			}
		}
		// Only the east side is free, so the fruit sets there.
		for (Direction side : List.of(Direction.NORTH, Direction.SOUTH, Direction.WEST)) {
			helper.setBlock(VINE.relative(side), Blocks.STONE);
		}
		helper.setBlock(VINE, block("giant_pumpkin_vine"));
		Player player = holding(helper, new ItemStack(Items.BONE_MEAL, 64));
		for (int i = 0; i < 12 && !helper.getBlockState(VINE).is(block("attached_giant_pumpkin_vine")); i++) {
			helper.useBlock(VINE, player, hit(helper, VINE, Direction.UP));
		}
		BlockState attached = helper.getBlockState(VINE);
		helper.assertTrue(attached.is(block("attached_giant_pumpkin_vine")) && attached.getValue(AttachedGiantPumpkinVineBlock.FACING) == Direction.EAST,
				"Bone meal should grow the vine and set its fruit east: " + attached);
		helper.assertTrue(helper.getBlockState(VINE.east()).is(block("giant_pumpkin")), "The fruit should be set");

		int need = GiantPumpkinBlockEntity.GROW_TO_TWO / GiantPumpkinBlockEntity.BONE_MEAL_POINTS;
		for (int i = 0; i < need; i++) {
			helper.useBlock(VINE, player, hit(helper, VINE, Direction.UP));
		}
		helper.assertTrue(master(helper, VINE.east()).size() == 2, "Bone meal on the vine feeds its pumpkin to two blocks wide");
		need = (GiantPumpkinBlockEntity.GROW_TO_THREE - GiantPumpkinBlockEntity.GROW_TO_TWO) / GiantPumpkinBlockEntity.BONE_MEAL_POINTS;
		for (int i = 0; i < need; i++) {
			helper.useBlock(VINE.east(), player, hit(helper, VINE.east(), Direction.UP));
		}
		GiantPumpkinBlockEntity master = master(helper, VINE.east());
		helper.assertTrue(master.fullGrown(), "Bone meal on the pumpkin grows it to three blocks wide, got " + master.size());
		int before = master.weight();
		helper.useBlock(VINE.east(), player, hit(helper, VINE.east(), Direction.UP));
		helper.assertTrue(master.weight() == before + GiantPumpkinBlockEntity.BONE_MEAL_POINTS * GiantPumpkinBlockEntity.WEIGHT_PER_POINT,
				"Full grown, bone meal puts on weight: " + before + " -> " + master.weight());
		helper.succeed();
	}

	/** A full-grown vine boxed in has nowhere to set its fruit: bone meal does nothing and is not used up. */
	@GameTest
	public void boxedInVineWaitsForRoom(GameTestHelper helper) {
		helper.setBlock(VINE.below(), Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
		for (Direction side : Direction.Plane.HORIZONTAL) {
			helper.setBlock(VINE.relative(side), Blocks.STONE);
		}
		helper.setBlock(VINE, block("giant_pumpkin_vine").defaultBlockState().setValue(GiantPumpkinVineBlock.AGE, GiantPumpkinVineBlock.MAX_AGE));
		Player player = holding(helper, new ItemStack(Items.BONE_MEAL, 3));
		helper.useBlock(VINE, player, hit(helper, VINE, Direction.UP));
		helper.assertTrue(helper.getBlockState(VINE).is(block("giant_pumpkin_vine")) && player.getMainHandItem().getCount() == 3,
				"Boxed in, the vine cannot set a fruit and keeps the bone meal");
		helper.setBlock(VINE.east(), Blocks.AIR);
		helper.setBlock(VINE.east().below(), Blocks.GRASS_BLOCK);
		helper.useBlock(VINE, player, hit(helper, VINE, Direction.UP));
		helper.assertTrue(helper.getBlockState(VINE.east()).is(block("giant_pumpkin")) && player.getMainHandItem().getCount() == 2,
				"With room, bone meal sets the fruit");
		helper.succeed();
	}

	/**
	 * Breaking any block picks the whole pumpkin up as one Giant Pumpkin item that keeps its size, weight, carving and
	 * name for the scale, and the vine straightens; placing the item puts the same pumpkin back, reaching away from the
	 * player, cut from any vine; an axe chops it up for nine pumpkins and its seeds.
	 */
	@GameTest(maxTicks = 40)
	public void giantPumpkinIsPickedUpWhole(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		GiantPumpkinBlockEntity grown = fullGrown(helper);
		grown.feed(level, 25, level.getRandom());
		int weight = grown.weight();
		UUID id = grown.id();
		int[] face = CarvingFace.scale(CLASSIC, GiantPumpkinBlockEntity.FACE_SIZE);
		grown.setFace(Direction.NORTH, face, null);
		BlockPos corner = new BlockPos(4, 4, 3);
		helper.assertTrue(Block.getDrops(helper.getBlockState(corner), level, helper.absolutePos(corner), null).isEmpty(),
				"Only the master block drops the pumpkin");
		level.destroyBlock(helper.absolutePos(corner), true);
		for (int x = 2; x <= 4; x++) {
			for (int y = 2; y <= 4; y++) {
				for (int z = 1; z <= 3; z++) {
					helper.assertBlockNotPresent(block("giant_pumpkin"), new BlockPos(x, y, z));
				}
			}
		}
		BlockState vine = helper.getBlockState(VINE);
		helper.assertTrue(vine.is(block("giant_pumpkin_vine")) && vine.getValue(GiantPumpkinVineBlock.AGE) == GiantPumpkinVineBlock.MAX_AGE,
				"The vine should straighten to set another: " + vine);
		List<ItemEntity> dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(3, 3, 2))).inflate(4.0));
		helper.assertTrue(dropped.size() == 1 && dropped.get(0).getItem().is(item("giant_pumpkin")) && dropped.get(0).getItem().getCount() == 1,
				"Breaking it should drop one Giant Pumpkin: " + dropped.stream().map(ItemEntity::getItem).toList());
		ItemStack carried = dropped.get(0).getItem().copy();
		dropped.get(0).discard();
		GiantPumpkinData data = carried.get(JugcraftAgriculture.GIANT_PUMPKIN);
		helper.assertTrue(data != null && data.size() == 3 && data.weight() == weight && data.id().equals(Optional.of(id))
				&& Arrays.equals(data.faces().get(Direction.NORTH), face), "The item keeps the pumpkin: " + data);

		// Placed back from the south-facing player aiming at the middle of the near edge, it fills x 2-4, z 1-3 again.
		Player player = holding(helper, carried);
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, new BlockPos(3, 1, 1), Direction.UP)));
		for (int x = 2; x <= 4; x++) {
			for (int y = 2; y <= 4; y++) {
				for (int z = 1; z <= 3; z++) {
					BlockState state = helper.getBlockState(new BlockPos(x, y, z));
					helper.assertTrue(state.is(block("giant_pumpkin")) && state.getValue(GiantPumpkinBlock.SIZE) == 3,
							"Expected the pumpkin placed back at " + x + "," + y + "," + z + ": " + state);
				}
			}
		}
		GiantPumpkinBlockEntity placed = helper.getBlockEntity(new BlockPos(2, 2, 1), GiantPumpkinBlockEntity.class);
		helper.assertTrue(placed.weight() == weight && placed.id().equals(id) && Arrays.equals(placed.face(Direction.NORTH), face)
				&& !placed.attached(level), "Placed back it is the same pumpkin, off the vine");
		helper.assertTrue(player.getMainHandItem().isEmpty(), "Placing uses the item");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
		helper.useBlock(new BlockPos(3, 3, 1), player, hit(helper, new BlockPos(3, 3, 1), Direction.NORTH));
		helper.assertBlockNotPresent(block("giant_pumpkin"), new BlockPos(2, 2, 1));
		helper.assertBlockNotPresent(block("giant_pumpkin"), new BlockPos(4, 4, 3));
		helper.succeedWhen(() -> helper.assertTrue(itemsAround(helper, new BlockPos(3, 3, 2), Items.PUMPKIN) == 9
				&& itemsAround(helper, new BlockPos(3, 3, 2), item("giant_pumpkin_seeds")) >= 1,
				"Chopping it should give 9 pumpkins and seeds, found " + itemsAround(helper, new BlockPos(3, 3, 2), Items.PUMPKIN)));
	}

	/**
	 * A full-grown giant pumpkin's side carves as one 48x48 face (a starter face blown up three times), through
	 * its master block; a 16x16 face does not fit it; a torch lights every block; carved, it stops putting on weight.
	 */
	@GameTest
	public void giantPumpkinCarvesAndGlows(GameTestHelper helper) {
		GiantPumpkinBlockEntity master = fullGrown(helper);
		BlockPos masterPos = master.getBlockPos();
		ServerPlayer player = serverPlayer(helper, new BlockPos(3, 2, -1), new ItemStack(item("carving_knife")));
		int[] face = CarvingFace.scale(CLASSIC, GiantPumpkinBlockEntity.FACE_SIZE);
		PumpkinCarvings.startSession(player, masterPos, Direction.NORTH, GiantPumpkinBlockEntity.FACE_SIZE);
		helper.assertTrue(PumpkinCarvings.carve(player, masterPos, Direction.NORTH, GiantPumpkinBlockEntity.FACE_SIZE, face) == Result.CARVED,
				"A 48x48 face should carve the giant's north side");
		helper.assertTrue(Arrays.equals(master.face(Direction.NORTH), face) && master.carved(), "The giant keeps its face");
		PumpkinCarvings.startSession(player, masterPos, Direction.NORTH);
		helper.assertTrue(PumpkinCarvings.carve(player, masterPos, Direction.NORTH, CLASSIC) == Result.NOT_A_PUMPKIN,
				"A 16x16 face does not fit a giant pumpkin");

		int glow = master.glow();
		helper.assertTrue(glow > 0, "A carved giant has a glow");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TORCH));
		helper.useBlock(new BlockPos(4, 4, 3), player, hit(helper, new BlockPos(4, 4, 3), Direction.UP));
		for (BlockPos pos : new BlockPos[] {new BlockPos(2, 2, 1), new BlockPos(3, 3, 2), new BlockPos(4, 4, 3)}) {
			helper.assertTrue(helper.getBlockState(pos).getLightEmission() == glow, "Every block should glow " + glow + " at " + pos);
		}
		int weight = master.weight();
		master.feed(helper.getLevel(), 10, helper.getLevel().getRandom());
		helper.assertTrue(master.weight() == weight && !master.canGrow(), "Carved, it stops putting on weight");
		helper.succeed();
	}

	// ---------------------------------------------------------------- scooping and heirloom pumpkins

	/** The first cut scoops a pumpkin: its seeds, and pumpkin guts. Heirloom pumpkins carve into their own block. */
	@GameTest(maxTicks = 40)
	public void heirloomPumpkinsAreScoopedAndCarved(GameTestHelper helper) {
		helper.setBlock(PUMPKIN, block("white_pumpkin"));
		ServerPlayer player = serverPlayer(helper, PUMPKIN.north(2), new ItemStack(item("carving_knife")));
		BlockPos pumpkin = helper.absolutePos(PUMPKIN);
		PumpkinCarvings.startSession(player, pumpkin, Direction.NORTH);
		helper.assertTrue(PumpkinCarvings.carve(player, pumpkin, Direction.NORTH, CLASSIC) == Result.CARVED, "A white pumpkin should carve");
		BlockState state = helper.getBlockState(PUMPKIN);
		helper.assertTrue(state.is(block("hand_carved_white_pumpkin")) && state.getValue(CarvedPumpkinBlock.FACING) == Direction.NORTH,
				"It should become a hand-carved white pumpkin: " + state);
		helper.assertTrue(Arrays.equals(helper.getBlockEntity(PUMPKIN, CarvedPumpkinBlockEntity.class).carving().face(0), CLASSIC),
				"It keeps the face");
		List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), pumpkin, helper.getBlockEntity(PUMPKIN, CarvedPumpkinBlockEntity.class));
		helper.assertTrue(drops.size() == 1 && drops.get(0).is(item("hand_carved_white_pumpkin")), "It drops itself: " + drops);
		helper.succeedWhen(() -> {
			helper.assertTrue(itemsAround(helper, PUMPKIN, item("white_pumpkin_seeds")) == 4, "The first cut lets out 4 white pumpkin seeds");
			int guts = itemsAround(helper, PUMPKIN, item("pumpkin_guts"));
			helper.assertTrue(guts >= 1 && guts <= 2, "and 1-2 pumpkin guts, found " + guts);
		});
	}

	// ---------------------------------------------------------------- the Harvest Scale

	/** The scale weighs the giant beside it, ranks it, gives one ribbon per pumpkin and tells comparators. */
	@GameTest
	public void harvestScaleWeighsAndAwardsARibbon(GameTestHelper helper) {
		GiantPumpkinBlockEntity giant = fullGrown(helper);
		BlockPos scale = new BlockPos(5, 2, 2);
		helper.setBlock(scale.below(), Blocks.STONE);
		helper.setBlock(scale, block("harvest_scale"));
		ServerPlayer player = serverPlayer(helper, new BlockPos(6, 2, 2), ItemStack.EMPTY);
		helper.useBlock(scale, player, hit(helper, scale, Direction.UP));
		HarvestScaleBlockEntity entity = helper.getBlockEntity(scale, HarvestScaleBlockEntity.class);
		helper.assertTrue(entity.board().size() == 1 && entity.board().get(0).weight() == giant.weight()
				&& entity.board().get(0).pumpkin().equals(giant.id()), "The board should list the pumpkin at its weight: " + entity.board());
		helper.assertTrue(player.getInventory().countItem(item("first_prize_ribbon")) == 1, "First place wins a first prize ribbon");
		int signal = entity.signal();
		helper.assertTrue(helper.getBlockState(scale).hasAnalogOutputSignal()
				&& signal == Math.max(1, giant.weight() * 15 / GiantPumpkinBlockEntity.MAX_WEIGHT), "Comparators read the weight, got " + signal);

		giant.feed(helper.getLevel(), 50, helper.getLevel().getRandom());
		helper.useBlock(scale, player, hit(helper, scale, Direction.UP));
		helper.assertTrue(entity.board().size() == 1 && entity.board().get(0).weight() == giant.weight(), "Weighed again, it is updated, not added");
		helper.assertTrue(player.getInventory().countItem(item("first_prize_ribbon")) == 1, "A pumpkin wins its ribbon once");

		BlockPos lonely = new BlockPos(7, 2, 6);
		helper.setBlock(lonely.below(), Blocks.STONE);
		helper.setBlock(lonely, block("harvest_scale"));
		helper.useBlock(lonely, player, hit(helper, lonely, Direction.UP));
		helper.assertTrue(helper.getBlockEntity(lonely, HarvestScaleBlockEntity.class).board().isEmpty(), "No pumpkin beside it, nothing weighed");
		helper.succeed();
	}

	// ---------------------------------------------------------------- stencils

	/** A blank stencil traces a carved side into a Pumpkin Stencil, which the carving screen offers from the other hand. */
	@GameTest
	public void stencilsTraceACarving(GameTestHelper helper) {
		helper.setBlock(PUMPKIN, Blocks.PUMPKIN);
		ServerPlayer player = serverPlayer(helper, PUMPKIN.north(2), new ItemStack(item("carving_knife")));
		BlockPos pumpkin = helper.absolutePos(PUMPKIN);
		PumpkinCarvings.startSession(player, pumpkin, Direction.NORTH);
		PumpkinCarvings.carve(player, pumpkin, Direction.NORTH, CLASSIC);

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("blank_stencil"), 2));
		helper.assertTrue(!player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, PUMPKIN, Direction.SOUTH)))
				.consumesAction(), "An uncarved side has nothing to trace");
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, PUMPKIN, Direction.NORTH)));
		helper.assertTrue(player.getMainHandItem().getCount() == 1, "Tracing uses one blank stencil");
		ItemStack stencil = ItemStack.EMPTY;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			if (player.getInventory().getItem(slot).is(item("pumpkin_stencil"))) {
				stencil = player.getInventory().getItem(slot);
			}
		}
		PumpkinCarving design = stencil.get(JugcraftAgriculture.STENCIL);
		helper.assertTrue(stencil.is(item("pumpkin_stencil")) && design != null && Arrays.equals(design.face(0), CLASSIC),
				"The stencil should hold the traced face: " + stencil);
		player.setItemInHand(InteractionHand.OFF_HAND, stencil);
		Optional<int[]> offered = PumpkinCarvings.stencil(player);
		helper.assertTrue(offered.isPresent() && Arrays.equals(offered.get(), CLASSIC), "The carving screen is offered the stencil in the other hand");
		helper.succeed();
	}

	// ---------------------------------------------------------------- decorations

	/** A scarecrow stands two blocks tall facing its placer; dye recolours its shirt; breaking either half drops one. */
	@GameTest(maxTicks = 40)
	public void scarecrowStandsAndIsDressed(GameTestHelper helper) {
		BlockPos ground = new BlockPos(3, 1, 3);
		helper.setBlock(ground, Blocks.STONE);
		Player player = holding(helper, new ItemStack(item("scarecrow")));
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, ground, Direction.UP)));
		BlockState lower = helper.getBlockState(ground.above());
		BlockState upper = helper.getBlockState(ground.above(2));
		helper.assertTrue(lower.is(block("scarecrow")) && lower.getValue(TallDecorationBlock.HALF) == DoubleBlockHalf.LOWER
				&& upper.is(block("scarecrow")) && upper.getValue(TallDecorationBlock.HALF) == DoubleBlockHalf.UPPER
				&& upper.getValue(ScarecrowBlock.SHIRT) == ScarecrowBlock.DEFAULT_SHIRT, "Expected a two-block scarecrow: " + lower + " / " + upper);

		Item blueDye = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", "blue_dye"));
		helper.assertTrue(ScarecrowBlock.dyeColor(new ItemStack(blueDye)) == DyeColor.BLUE, "Blue dye should be known as blue");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(blueDye, 2));
		helper.useBlock(ground.above(2), player, hit(helper, ground.above(2), Direction.NORTH));
		helper.assertTrue(helper.getBlockState(ground.above()).getValue(ScarecrowBlock.SHIRT) == DyeColor.BLUE
				&& helper.getBlockState(ground.above(2)).getValue(ScarecrowBlock.SHIRT) == DyeColor.BLUE, "Both halves wear the blue shirt");
		helper.assertTrue(player.getMainHandItem().getCount() == 1, "Dyeing uses one dye");

		helper.getLevel().destroyBlock(helper.absolutePos(ground.above(2)), true);
		helper.assertBlockNotPresent(block("scarecrow"), ground.above());
		helper.succeedWhen(() -> helper.assertTrue(itemsAround(helper, ground, item("scarecrow")) == 1,
				"Breaking it drops one scarecrow, found " + itemsAround(helper, ground, item("scarecrow"))));
	}

	/**
	 * A scarecrow wears a pumpkin for a head: a jack o'lantern lights it, another pumpkin swaps in (the old one comes
	 * back), a torch lights a hand-carved head by its carving, an empty hand takes the head off, and breaking the
	 * scarecrow drops its head.
	 */
	@GameTest(maxTicks = 40)
	public void scarecrowWearsAPumpkinHead(GameTestHelper helper) {
		BlockPos ground = new BlockPos(3, 1, 3);
		helper.setBlock(ground, Blocks.STONE);
		Player player = holding(helper, new ItemStack(item("scarecrow")));
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, ground, Direction.UP)));
		BlockPos lower = ground.above();
		BlockPos upper = ground.above(2);
		ScarecrowBlockEntity scarecrow = helper.getBlockEntity(upper, ScarecrowBlockEntity.class);

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.JACK_O_LANTERN, 2));
		helper.useBlock(lower, player, hit(helper, lower, Direction.NORTH));
		helper.assertTrue(scarecrow.head().is(Items.JACK_O_LANTERN) && scarecrow.head().getCount() == 1 && player.getMainHandItem().getCount() == 1,
				"The scarecrow should wear one jack o'lantern: " + scarecrow.head());
		helper.assertTrue(helper.getBlockState(upper).getLightEmission() == 15 && helper.getBlockState(lower).getLightEmission() == 0,
				"A jack o'lantern head lights the scarecrow's upper half");

		ItemStack carved = new ItemStack(item("hand_carved_pumpkin"));
		PumpkinCarving carving = PumpkinCarving.BLANK.withFace(0, CLASSIC);
		carved.set(JugcraftAgriculture.CARVING, carving);
		player.setItemInHand(InteractionHand.MAIN_HAND, carved);
		helper.useBlock(upper, player, hit(helper, upper, Direction.NORTH));
		helper.assertTrue(scarecrow.head().is(item("hand_carved_pumpkin")) && player.getInventory().countItem(Items.JACK_O_LANTERN) == 1
				&& helper.getBlockState(upper).getLightEmission() == 0, "A hand-carved head swaps in, unlit, and the jack o'lantern comes back");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TORCH));
		helper.useBlock(upper, player, hit(helper, upper, Direction.NORTH));
		helper.assertTrue(ScarecrowBlockEntity.lit(scarecrow.head()) && player.getMainHandItem().isEmpty()
				&& helper.getBlockState(upper).getLightEmission() == carving.glow(), "A torch lights the carved head by its carving");

		helper.useBlock(upper, player, hit(helper, upper, Direction.NORTH));
		helper.assertTrue(scarecrow.head().isEmpty() && player.getInventory().countItem(item("hand_carved_pumpkin")) == 1
				&& helper.getBlockState(upper).getLightEmission() == 0, "An empty hand takes the head off");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CARVED_PUMPKIN));
		helper.useBlock(upper, player, hit(helper, upper, Direction.NORTH));
		helper.getLevel().destroyBlock(helper.absolutePos(lower), true);
		helper.succeedWhen(() -> helper.assertTrue(itemsAround(helper, ground, item("scarecrow")) == 1
				&& itemsAround(helper, ground, Items.CARVED_PUMPKIN) == 1, "Breaking it drops the scarecrow and its head"));
	}

	/** Corn shocks stand two tall; ornamental corn bundles hang on a wall and fall when it goes; birdhouses hang or stand. */
	@GameTest
	public void harvestDecorationsHangAndStand(GameTestHelper helper) {
		BlockPos ground = new BlockPos(1, 1, 1);
		helper.setBlock(ground, Blocks.STONE);
		Player player = holding(helper, new ItemStack(item("corn_shock")));
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, ground, Direction.UP)));
		helper.assertBlockPresent(block("corn_shock"), ground.above());
		helper.assertBlockPresent(block("corn_shock"), ground.above(2));

		BlockPos wall = new BlockPos(4, 2, 4);
		helper.setBlock(wall, Blocks.OAK_PLANKS);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("ornamental_corn_bundle")));
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, wall, Direction.NORTH)));
		helper.assertBlockPresent(block("ornamental_corn_bundle"), wall.north());
		helper.setBlock(wall, Blocks.AIR);
		helper.assertBlockNotPresent(block("ornamental_corn_bundle"), wall.north());

		BlockPos beam = new BlockPos(6, 4, 2);
		helper.setBlock(beam, Blocks.OAK_PLANKS);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("gourd_birdhouse")));
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, beam, Direction.DOWN)));
		BlockState hanging = helper.getBlockState(beam.below());
		helper.assertTrue(hanging.is(block("gourd_birdhouse")) && hanging.getValue(LanternBlock.HANGING), "A birdhouse hangs under a beam: " + hanging);
		helper.succeed();
	}

	/** Mums go in flower pots (and come out with the pot), and make dye. */
	@GameTest
	public void mumsArePottedAndMakeDye(GameTestHelper helper) {
		BlockPos pot = new BlockPos(2, 2, 2);
		helper.setBlock(pot.below(), Blocks.STONE);
		helper.setBlock(pot, Blocks.FLOWER_POT);
		Player player = holding(helper, new ItemStack(item("orange_mum")));
		helper.useBlock(pot, player, hit(helper, pot, Direction.UP));
		helper.assertBlockPresent(block("potted_orange_mum"), pot);
		List<ItemStack> drops = Block.getDrops(helper.getBlockState(pot), helper.getLevel(), helper.absolutePos(pot), null);
		helper.assertTrue(count(drops, Items.FLOWER_POT) == 1 && count(drops, item("orange_mum")) == 1, "A potted mum drops its pot and mum: " + drops);
		for (String mum : new String[] {"yellow_mum", "orange_mum", "red_mum", "purple_mum"}) {
			String dye = mum.replace("_mum", "");
			helper.assertTrue(recipeLoads(helper, dye + "_dye_from_" + mum), "Missing the dye recipe for " + mum);
		}
		helper.succeed();
	}

	// ---------------------------------------------------------------- ornamental corn, treats and gourds

	/** Ornamental corn grows like corn and is picked for its ears; tall corn of either kind drops stalks for corn shocks. */
	@GameTest
	public void ornamentalCornGivesEarsAndStalks(GameTestHelper helper) {
		TallCropBlock ornamental = JugcraftAgriculture.TALL_CROPS.get(TallCrop.ORNAMENTAL_CORN);
		helper.assertTrue(ornamental == block("ornamental_corn_crop") && TallCrop.ORNAMENTAL_CORN.produceId.equals("ornamental_corn"),
				"Ornamental corn is a tall crop picked for its ears");
		for (TallCrop crop : new TallCrop[] {TallCrop.CORN, TallCrop.ORNAMENTAL_CORN}) {
			BlockState ripe = JugcraftAgriculture.TALL_CROPS.get(crop).defaultBlockState().setValue(TallCropBlock.AGE, TallCropBlock.MAX_AGE);
			List<ItemStack> drops = Block.getDrops(ripe, helper.getLevel(), helper.absolutePos(PUMPKIN), null);
			helper.assertTrue(count(drops, item("corn_stalks")) >= 1 && count(drops, item(crop.produceId)) >= 1,
					crop + " broken ripe should drop stalks and its ears: " + drops);
		}
		helper.succeed();
	}

	/** Treats and soups have their values; a caramel apple leaves its stick; every new recipe loads. */
	@GameTest
	public void treatsAndRecipesLoad(GameTestHelper helper) {
		String[] foods = {"caramel", "caramel_apple", "popcorn_ball", "pumpkin_soup"};
		int[] nutrition = {2, 6, 5, 8};
		for (int i = 0; i < foods.length; i++) {
			FoodProperties food = new ItemStack(item(foods[i])).get(DataComponents.FOOD);
			helper.assertTrue(food != null && food.nutrition() == nutrition[i], foods[i] + " should restore " + nutrition[i] + ", has " + food);
		}
		ItemStack left = new ItemStack(item("caramel_apple")).finishUsingItem(helper.getLevel(), helper.makeMockPlayer(GameType.SURVIVAL));
		helper.assertTrue(left.is(Items.STICK), "Eating a caramel apple leaves its stick, got " + left);
		for (String recipe : new String[] {"caramel", "caramel_from_smoking", "dried_bottle_gourd", "caramel_apple", "popcorn_ball",
				"gourd_canteen", "gourd_birdhouse", "blank_stencil", "harvest_scale", "scarecrow", "corn_shock", "ornamental_corn_bundle",
				"ornamental_corn_kernels", "white_pumpkin_seeds", "jarrahdale_pumpkin_seeds", "cinderella_pumpkin_seeds", "bottle_gourd_seeds",
				"pumpkin_pie_from_heirloom_pumpkins"}) {
			helper.assertTrue(recipeLoads(helper, recipe), "Recipe " + recipe + " should load");
		}
		List<ItemStack> slots = new ArrayList<>(List.of(new ItemStack(item("onion")), new ItemStack(item("pumpkin_guts")), new ItemStack(Items.BOWL),
				new ItemStack(item("pumpkin_guts"))));
		while (slots.size() < CookingPotBlockEntity.INPUTS) {
			slots.add(ItemStack.EMPTY);
		}
		Optional<CookingPotRecipe.Match> soup = CookingPotRecipe.find(helper.getLevel().getServer(), slots);
		helper.assertTrue(soup.isPresent() && soup.get().recipe().output().create().is(item("pumpkin_soup")), "Pumpkin guts cook into soup");
		var placed = helper.getLevel().registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
		for (String patch : new String[] {"white_pumpkin", "jarrahdale_pumpkin", "cinderella_pumpkin", "bottle_gourd", "mums"}) {
			ResourceKey<PlacedFeature> key = ResourceKey.create(Registries.PLACED_FEATURE, Jugcraft.id("patch_" + patch));
			helper.assertTrue(placed.get(key).isPresent(), "Missing placed feature " + key);
		}
		helper.succeed();
	}

	/** The gourd canteen fills at water and pours one sip at a time: on a giant pumpkin, farmland, a cauldron and fire. */
	@GameTest
	public void gourdCanteenCarriesWater(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos water = new BlockPos(6, 1, 6);
		helper.setBlock(water, Blocks.WATER);
		Player player = holding(helper, new ItemStack(item("gourd_canteen")));
		BlockPos above = helper.absolutePos(water.above());
		player.setPos(above.getX() + 0.5, above.getY(), above.getZ() + 0.5);
		player.setXRot(90.0F);
		player.getMainHandItem().use(level, player, InteractionHand.MAIN_HAND);
		helper.assertTrue(GourdCanteenItem.water(player.getMainHandItem()) == GourdCanteenItem.CAPACITY, "It fills at a water source");

		BlockPos farmland = new BlockPos(1, 1, 6);
		helper.setBlock(farmland, Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 0));
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, farmland, Direction.UP)));
		helper.assertTrue(helper.getBlockState(farmland).getValue(BlockStateProperties.MOISTURE) == 7, "A sip moistens farmland");

		BlockPos cauldron = new BlockPos(3, 1, 6);
		helper.setBlock(cauldron, Blocks.CAULDRON);
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, cauldron, Direction.UP)));
		BlockState filled = helper.getBlockState(cauldron);
		helper.assertTrue(filled.is(Blocks.WATER_CAULDRON) && filled.getValue(LayeredCauldronBlock.LEVEL) == 1, "A sip fills a cauldron a level: " + filled);

		BlockPos hearth = new BlockPos(5, 1, 3);
		helper.setBlock(hearth, Blocks.NETHERRACK);
		helper.setBlock(hearth.above(), Blocks.FIRE);
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, hearth, Direction.UP)));
		helper.assertBlockNotPresent(Blocks.FIRE, hearth.above());
		helper.assertTrue(GourdCanteenItem.water(player.getMainHandItem()) == 0, "Three sips empty it");
		helper.assertTrue(!player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, farmland, Direction.UP)))
				.consumesAction(), "An empty canteen pours nothing");

		GiantPumpkinBlockEntity giant = seedling(helper);
		player.getMainHandItem().set(JugcraftAgriculture.CANTEEN_WATER, 1);
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, VINE.east(), Direction.UP)));
		helper.assertTrue(giant.watered(level), "A sip waters a giant pumpkin");
		helper.succeed();
	}
}
