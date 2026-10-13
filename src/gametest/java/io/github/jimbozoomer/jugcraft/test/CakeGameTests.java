package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CakeBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HearthOvenBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HearthOvenBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PieBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PieFilling;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
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
 * In-game tests for the cakes the owner drew (tools/cakes.py): baked in the Hearth Oven from a raw cake, burnt into a Burnt
 * Cake when left in; set down with their front to whoever set them down; cut a quarter at a time, the front right
 * quarter first, whichever way the cake faces; eaten or cut with a knife into slices to take away, gone with the last,
 * dropping themselves only while whole; a Burnt Cake's poor slices; and the recipes.
 */
public class CakeGameTests {
	private static final List<PieFilling> CAKES = java.util.Arrays.stream(PieFilling.values()).filter(f -> f.cake).toList();

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
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

	/** Whether the shape covers the middle of the texel column (x, z), a texel above the cake's bottom. */
	private static boolean solid(VoxelShape shape, int x, int z) {
		return shape.toAabbs().stream().anyMatch(box -> box.contains((x + 0.5) / 16.0, 1.0 / 16.0, (z + 0.5) / 16.0));
	}

	/**
	 * Every raw cake goes into the Hearth Oven as its cake; at full heat one bakes in time and comes out the cake (its
	 * block's item), and one left in too long comes out a Burnt Cake.
	 */
	@GameTest(maxTicks = 40)
	public void cakesBakeAndBurn(GameTestHelper helper) {
		for (PieFilling cake : CAKES) {
			helper.assertTrue(HearthOvenBlockEntity.rawFilling(new ItemStack(item(cake.rawPie()))) == cake, cake.rawPie() + " goes in as " + cake);
			helper.assertTrue(JugcraftAgriculture.block(cake.pie()) instanceof CakeBlock, cake.pie() + " is set down as a cake");
		}
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, JugcraftAgriculture.block("hearth_oven").defaultBlockState().setValue(HearthOvenBlock.FACING, Direction.NORTH));
		HearthOvenBlockEntity oven = helper.getBlockEntity(pos, HearthOvenBlockEntity.class);
		oven.set(1000, HearthOvenBlockEntity.MAX_HEAT, PieFilling.CARROT_CAKE, HearthOvenBlockEntity.BAKED - 6);
		BlockPos burnPos = new BlockPos(6, 2, 3);
		helper.setBlock(burnPos, JugcraftAgriculture.block("hearth_oven").defaultBlockState().setValue(HearthOvenBlock.FACING, Direction.NORTH));
		HearthOvenBlockEntity burning = helper.getBlockEntity(burnPos, HearthOvenBlockEntity.class);
		burning.set(1000, HearthOvenBlockEntity.MAX_HEAT, PieFilling.BIRTHDAY_CAKE, HearthOvenBlockEntity.BURNT - 6);
		ServerPlayer baker = player(helper, new BlockPos(3, 2, 1));
		helper.runAfterDelay(5, () -> {
			use(helper, baker, pos, ItemStack.EMPTY, Direction.NORTH);
			helper.assertTrue(baker.getInventory().countItem(item("carrot_cake")) == 1, "Out comes a carrot cake");
			use(helper, baker, burnPos, ItemStack.EMPTY, Direction.NORTH);
			helper.assertTrue(baker.getInventory().countItem(item("burnt_cake")) == 1 && baker.getInventory().countItem(item("burnt_pie")) == 0,
					"Left in, the birthday cake comes out a Burnt Cake");
			helper.succeed();
		});
	}

	/** Set down by a player looking north, a cake faces south, towards them. */
	@GameTest(maxTicks = 20)
	public void aCakeFacesWhoeverSetsItDown(GameTestHelper helper) {
		BlockPos floor = new BlockPos(3, 1, 3);
		helper.setBlock(floor, Blocks.STONE);
		ServerPlayer baker = player(helper, new BlockPos(3, 2, 5));
		baker.setYRot(180.0F);
		baker.setYHeadRot(180.0F);
		use(helper, baker, floor, new ItemStack(item("red_velvet_cake")), Direction.UP);
		BlockState state = helper.getBlockState(floor.above());
		helper.assertTrue(state.is(JugcraftAgriculture.block("red_velvet_cake")) && state.getValue(CakeBlock.FACING) == Direction.SOUTH,
				"The cake is set down facing the player: " + state);
		helper.succeed();
	}

	/**
	 * Whichever way a cake faces, the first slice takes its front right quarter (the north-west of a cake facing north, as
	 * the owner's INTERIOR drawing cuts it), the next its front left, then its back left; a whole cake fills its block's
	 * footprint, {@link CakeBlock#HEIGHT} texels tall.
	 */
	@GameTest(maxTicks = 20)
	public void cakesAreCutFrontRightFirst(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// The texel column in the middle of each quarter of a cake facing north: front right, front left, back left, back right.
		int[][] middles = {{4, 4}, {12, 4}, {12, 12}, {4, 12}};
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos.below(), Blocks.STONE);
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			for (int bites = 0; bites < PieBlock.SLICES; bites++) {
				helper.setBlock(pos, JugcraftAgriculture.block("cheesecake").defaultBlockState().setValue(CakeBlock.FACING, facing)
						.setValue(PieBlock.BITES, bites));
				VoxelShape shape = helper.getBlockState(pos).getShape(level, helper.absolutePos(pos));
				for (int quarter = 0; quarter < PieBlock.SLICES; quarter++) {
					// Where this quarter of the north-facing cake is once the cake is turned to face `facing`.
					int[] at = turned(middles[quarter], facing);
					helper.assertTrue(solid(shape, at[0], at[1]) == (quarter >= bites),
							"Facing " + facing + " with " + bites + " slices gone, quarter " + quarter + " is " + (quarter >= bites ? "there" : "gone"));
				}
				if (bites == 0) {
					helper.assertTrue(Math.abs(shape.bounds().maxY - CakeBlock.HEIGHT / 16.0) < 1e-6 && shape.bounds().minX == 0
							&& shape.bounds().maxX == 1 && shape.bounds().minZ == 0 && shape.bounds().maxZ == 1, "A whole cake fills its footprint");
				}
			}
		}
		helper.succeed();
	}

	/** A texel column of a cake facing north, turned clockwise (seen from above) to {@code facing}, as the blockstate turns the model. */
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
	 * A hungry player eats a quarter (four food for the carrot cake), a kitchen knife cuts one to take away as a slice, the
	 * cake keeps its facing, and the last quarter takes the cake. A whole cake drops itself; a cut one doesn't. A Burnt Cake's
	 * slice is one food.
	 */
	@GameTest(maxTicks = 20)
	public void cakesAreEatenAndCut(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos.below(), Blocks.STONE);
		helper.setBlock(pos, JugcraftAgriculture.block("carrot_cake").defaultBlockState().setValue(CakeBlock.FACING, Direction.EAST));
		ServerPlayer eater = player(helper, new BlockPos(3, 2, 1));
		eater.getFoodData().setFoodLevel(4);
		use(helper, eater, pos, ItemStack.EMPTY, Direction.NORTH);
		helper.assertTrue(eater.getFoodData().getFoodLevel() == 4 + PieFilling.CARROT_CAKE.nutrition && helper.getBlockState(pos).getValue(PieBlock.BITES) == 1,
				"A quarter eaten: " + eater.getFoodData().getFoodLevel());
		use(helper, eater, pos, new ItemStack(item("carving_knife")), Direction.NORTH);
		helper.assertTrue(eater.getInventory().countItem(item(PieFilling.CARROT_CAKE.slice())) == 1 && helper.getBlockState(pos).getValue(PieBlock.BITES) == 2
				&& helper.getBlockState(pos).getValue(CakeBlock.FACING) == Direction.EAST, "A knife cuts a slice to take away; the cake still faces east");
		use(helper, eater, pos, ItemStack.EMPTY, Direction.NORTH);
		use(helper, eater, pos, ItemStack.EMPTY, Direction.NORTH);
		helper.assertTrue(helper.getBlockState(pos).isAir(), "The last quarter takes the cake");

		BlockPos whole = new BlockPos(5, 2, 3);
		BlockPos cut = new BlockPos(6, 2, 3);
		for (BlockPos at : List.of(whole, cut)) {
			helper.setBlock(at.below(), Blocks.STONE);
			helper.setBlock(at, JugcraftAgriculture.block("apple_cake"));
		}
		helper.setBlock(cut, helper.getBlockState(cut).setValue(PieBlock.BITES, 1));
		level.destroyBlock(helper.absolutePos(whole), true);
		level.destroyBlock(helper.absolutePos(cut), true);
		int dropped = 0;
		for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(whole)).inflate(3.0))) {
			dropped += entity.getItem().is(item("apple_cake")) ? entity.getItem().getCount() : 0;
			entity.discard();
		}
		helper.assertTrue(dropped == 1, "Only the whole cake drops itself: " + dropped);

		BlockPos burnt = new BlockPos(3, 2, 6);
		helper.setBlock(burnt.below(), Blocks.STONE);
		helper.setBlock(burnt, JugcraftAgriculture.block("burnt_cake"));
		eater.getFoodData().setFoodLevel(4);
		use(helper, eater, burnt, ItemStack.EMPTY, Direction.NORTH);
		helper.assertTrue(eater.getFoodData().getFoodLevel() == 4 + PieBlock.BURNT_NUTRITION, "A burnt slice is one food");
		helper.succeed();
	}

	/** Cake Batter's and every raw cake's recipes load; every cake's slice is a food. */
	@GameTest(maxTicks = 20)
	public void cakeDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<String> recipes = new ArrayList<>(List.of("cake_batter"));
		TagKey<Item> foods = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "foods"));
		for (PieFilling cake : CAKES) {
			recipes.add(cake.rawPie());
			helper.assertTrue(new ItemStack(item(cake.slice())).is(foods), cake.slice() + " is a food");
		}
		helper.assertTrue(CAKES.size() == 7, "Seven cakes: " + CAKES);
		for (String recipe : recipes) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(recipe))).isPresent(), recipe + " loads");
		}
		helper.succeed();
	}
}
