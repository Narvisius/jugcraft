package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CakeBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HearthOvenBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HearthOvenBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PieBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PieFilling;
import java.util.Arrays;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * In-game tests for the owner's square pies and tarts (tools/pies_and_tarts.py): baked in the Hearth Oven from a raw pie or
 * tart, burnt into a Burnt Pie when left in; set down with their front to whoever set them down, a pie {@value #PIE}
 * texels tall and a tart {@value #TART}; cut a quarter at a time, the front right quarter first, as the owner's page shows
 * the strawberry pie and the blueberry tart cut, whichever way they face; eaten or cut with a knife into slices to take
 * away, gone with the last, dropping themselves only while whole; and their recipes, tags and food.
 */
public class PieTartGameTests {
	/** The pies and tarts: the fillings set down at a height of their own that are not cakes. */
	private static final List<PieFilling> BAKES = Arrays.stream(PieFilling.values()).filter(f -> f.height > 0 && !f.cake).toList();
	/** How tall a pie and a tart stand, in texels (tools/pies_and_tarts.py PIE and TART). */
	private static final int PIE = 7;
	private static final int TART = 4;

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static int height(PieFilling bake) {
		return bake.pie().endsWith("_tart") ? TART : PIE;
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		return player;
	}

	/** {@code player} uses {@code held} (or an empty hand) on the {@code face} of the block at {@code pos}. */
	private static void use(GameTestHelper helper, ServerPlayer player, BlockPos pos, ItemStack held, Direction face) {
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		BlockPos absolute = helper.absolutePos(pos);
		player.gameMode.useItemOn(player, helper.getLevel(), held, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(absolute).relative(face, 0.5), face, absolute, false));
	}

	/** Whether the shape covers the middle of the texel column (x, z), a texel above the bake's bottom. */
	private static boolean solid(VoxelShape shape, int x, int z) {
		return shape.toAabbs().stream().anyMatch(box -> box.contains((x + 0.5) / 16.0, 1.0 / 16.0, (z + 0.5) / 16.0));
	}

	/**
	 * Every raw pie and tart goes into the Hearth Oven as its own, and is set down as a {@link CakeBlock} of its height; at
	 * full heat one bakes in time and comes out baked (its block's item), and one left in too long comes out a Burnt Pie,
	 * not a Burnt Cake.
	 */
	@GameTest(maxTicks = 40)
	public void piesAndTartsBakeAndBurn(GameTestHelper helper) {
		for (PieFilling bake : BAKES) {
			helper.assertTrue(HearthOvenBlockEntity.rawFilling(new ItemStack(item(bake.rawPie()))) == bake, bake.rawPie() + " goes in as " + bake);
			helper.assertTrue(JugcraftAgriculture.block(bake.pie()) instanceof CakeBlock block && block.height == height(bake),
					bake.pie() + " is set down " + height(bake) + " texels tall");
			helper.assertTrue(bake.burnt().equals("burnt_pie"), bake + " burns into a Burnt Pie");
		}
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, JugcraftAgriculture.block("hearth_oven").defaultBlockState().setValue(HearthOvenBlock.FACING, Direction.NORTH));
		HearthOvenBlockEntity oven = helper.getBlockEntity(pos, HearthOvenBlockEntity.class);
		oven.set(1000, HearthOvenBlockEntity.MAX_HEAT, PieFilling.STRAWBERRY_PIE, HearthOvenBlockEntity.BAKED - 6);
		BlockPos burnPos = new BlockPos(6, 2, 3);
		helper.setBlock(burnPos, JugcraftAgriculture.block("hearth_oven").defaultBlockState().setValue(HearthOvenBlock.FACING, Direction.NORTH));
		HearthOvenBlockEntity burning = helper.getBlockEntity(burnPos, HearthOvenBlockEntity.class);
		burning.set(1000, HearthOvenBlockEntity.MAX_HEAT, PieFilling.LEMON_TART, HearthOvenBlockEntity.BURNT - 6);
		ServerPlayer baker = player(helper, new BlockPos(3, 2, 1));
		helper.runAfterDelay(5, () -> {
			use(helper, baker, pos, ItemStack.EMPTY, Direction.NORTH);
			helper.assertTrue(baker.getInventory().countItem(item("strawberry_pie")) == 1, "Out comes a strawberry pie");
			use(helper, baker, burnPos, ItemStack.EMPTY, Direction.NORTH);
			helper.assertTrue(baker.getInventory().countItem(item("burnt_pie")) == 1 && baker.getInventory().countItem(item("burnt_cake")) == 0,
					"Left in, the lemon tart comes out a Burnt Pie");
			helper.succeed();
		});
	}

	/** Set down by a player looking north, a pie and a tart face south, towards them. */
	@GameTest(maxTicks = 20)
	public void piesAndTartsFaceWhoeverSetsThemDown(GameTestHelper helper) {
		ServerPlayer baker = player(helper, new BlockPos(4, 2, 5));
		baker.setYRot(180.0F);
		baker.setYHeadRot(180.0F);
		for (String bake : List.of("banoffee_pie", "lemon_tart")) {
			BlockPos floor = new BlockPos(bake.equals("banoffee_pie") ? 3 : 5, 1, 3);
			helper.setBlock(floor, Blocks.STONE);
			use(helper, baker, floor, new ItemStack(item(bake)), Direction.UP);
			BlockState state = helper.getBlockState(floor.above());
			helper.assertTrue(state.is(JugcraftAgriculture.block(bake)) && state.getValue(CakeBlock.FACING) == Direction.SOUTH,
					"The " + bake + " is set down facing the player: " + state);
		}
		helper.succeed();
	}

	/**
	 * Whichever way a pie or a tart faces, the first slice takes its front right quarter, the next its front left, then its
	 * back left, as a cake's do; whole, it fills its block's footprint at its own height.
	 */
	@GameTest(maxTicks = 20)
	public void piesAndTartsAreCutFrontRightFirst(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// The texel column in the middle of each quarter of a bake facing north: front right, front left, back left, back right.
		int[][] middles = {{4, 4}, {12, 4}, {12, 12}, {4, 12}};
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos.below(), Blocks.STONE);
		for (PieFilling bake : List.of(PieFilling.STRAWBERRY_PIE, PieFilling.BLUEBERRY_TART)) {
			for (Direction facing : Direction.Plane.HORIZONTAL) {
				for (int bites = 0; bites < PieBlock.SLICES; bites++) {
					helper.setBlock(pos, JugcraftAgriculture.block(bake.pie()).defaultBlockState().setValue(CakeBlock.FACING, facing)
							.setValue(PieBlock.BITES, bites));
					VoxelShape shape = helper.getBlockState(pos).getShape(level, helper.absolutePos(pos));
					for (int quarter = 0; quarter < PieBlock.SLICES; quarter++) {
						// Where this quarter of the north-facing bake is once it is turned to face `facing`.
						int[] at = turned(middles[quarter], facing);
						helper.assertTrue(solid(shape, at[0], at[1]) == (quarter >= bites), bake.pie() + " facing " + facing + " with " + bites
								+ " slices gone: quarter " + quarter + " is " + (quarter >= bites ? "there" : "gone"));
					}
					helper.assertTrue(Math.abs(shape.bounds().maxY - height(bake) / 16.0) < 1e-6, bake.pie() + " stands " + height(bake) + " texels tall");
					if (bites == 0) {
						helper.assertTrue(shape.bounds().minX == 0 && shape.bounds().maxX == 1 && shape.bounds().minZ == 0 && shape.bounds().maxZ == 1,
								"A whole " + bake.pie() + " fills its footprint");
					}
				}
			}
		}
		helper.succeed();
	}

	/** A texel column of a bake facing north, turned clockwise (seen from above) to {@code facing}, as the blockstate turns the model. */
	private static int[] turned(int[] at, Direction facing) {
		int x = at[0], z = at[1];
		int turns = switch (facing) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
		for (int i = 0; i < turns; i++) {
			int nx = 15 - z;
			z = x;
			x = nx;
		}
		return new int[] {x, z};
	}

	/**
	 * A hungry player eats a quarter of the pork pie (five food), a kitchen knife cuts one to take away as a slice, the pie
	 * keeps its facing, and the last quarter takes the pie. A whole tart drops itself; a cut one doesn't.
	 */
	@GameTest(maxTicks = 20)
	public void piesAndTartsAreEatenAndCut(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos.below(), Blocks.STONE);
		helper.setBlock(pos, JugcraftAgriculture.block("pork_pie").defaultBlockState().setValue(CakeBlock.FACING, Direction.EAST));
		ServerPlayer eater = player(helper, new BlockPos(3, 2, 1));
		eater.getFoodData().setFoodLevel(4);
		use(helper, eater, pos, ItemStack.EMPTY, Direction.NORTH);
		helper.assertTrue(eater.getFoodData().getFoodLevel() == 4 + PieFilling.PORK_PIE.nutrition && helper.getBlockState(pos).getValue(PieBlock.BITES) == 1,
				"A quarter eaten: " + eater.getFoodData().getFoodLevel());
		use(helper, eater, pos, new ItemStack(item("carving_knife")), Direction.NORTH);
		helper.assertTrue(eater.getInventory().countItem(item(PieFilling.PORK_PIE.slice())) == 1 && helper.getBlockState(pos).getValue(PieBlock.BITES) == 2
				&& helper.getBlockState(pos).getValue(CakeBlock.FACING) == Direction.EAST, "A knife cuts a slice to take away; the pie still faces east");
		use(helper, eater, pos, ItemStack.EMPTY, Direction.NORTH);
		use(helper, eater, pos, ItemStack.EMPTY, Direction.NORTH);
		helper.assertTrue(helper.getBlockState(pos).isAir(), "The last quarter takes the pie");

		BlockPos whole = new BlockPos(5, 2, 3);
		BlockPos cut = new BlockPos(6, 2, 3);
		for (BlockPos at : List.of(whole, cut)) {
			helper.setBlock(at.below(), Blocks.STONE);
			helper.setBlock(at, JugcraftAgriculture.block("coffee_tart"));
		}
		helper.setBlock(cut, helper.getBlockState(cut).setValue(PieBlock.BITES, 1));
		level.destroyBlock(helper.absolutePos(whole), true);
		level.destroyBlock(helper.absolutePos(cut), true);
		int dropped = 0;
		for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(whole)).inflate(3.0))) {
			dropped += entity.getItem().is(item("coffee_tart")) ? entity.getItem().getCount() : 0;
			entity.discard();
		}
		helper.assertTrue(dropped == 1, "Only the whole tart drops itself: " + dropped);
		helper.succeed();
	}

	/**
	 * Every raw pie's and tart's recipe loads; there are ten; every slice is a food (and a pie, in the common tags) giving
	 * its filling's hunger, and a raw one isn't eaten as it is.
	 */
	@GameTest(maxTicks = 20)
	public void pieAndTartDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		TagKey<Item> foods = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "foods"));
		TagKey<Item> pies = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "foods/pie"));
		helper.assertTrue(BAKES.size() == 10, "Ten pies and tarts: " + BAKES);
		for (PieFilling bake : BAKES) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(bake.rawPie()))).isPresent(),
					bake.rawPie() + " loads");
			ItemStack slice = new ItemStack(item(bake.slice()));
			helper.assertTrue(slice.is(foods) && slice.is(pies), bake.slice() + " is a food and a pie");
			FoodProperties food = slice.get(DataComponents.FOOD);
			helper.assertTrue(food != null && food.nutrition() == bake.nutrition, bake.slice() + " should restore " + bake.nutrition + ", has " + food);
			helper.assertTrue(new ItemStack(item(bake.rawPie())).get(DataComponents.FOOD) == null, bake.rawPie() + " should not be eaten as it is");
		}
		helper.succeed();
	}
}
