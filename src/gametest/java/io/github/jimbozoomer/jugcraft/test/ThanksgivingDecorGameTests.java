package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.CandleCenterpieceBlock;
import io.github.jimbozoomer.jugcraft.agriculture.FacingDecorationBlock;
import io.github.jimbozoomer.jugcraft.agriculture.FeastTableBlock;
import io.github.jimbozoomer.jugcraft.agriculture.FeastTableBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.LeafGarlandBlock;
import io.github.jimbozoomer.jugcraft.agriculture.WallDecorationBlock;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Thanksgiving decorations (tools/thanksgiving_decor.py; docs/features/thanksgiving-decor.md): every floor prop is placed
 * from its item facing whoever placed it and, broken, drops itself once; the candle centerpiece lights with flint and
 * steel, gives its light and an empty hand snuffs it; garlands hung side by side on one wall join, only their two ends
 * left tied off, and a gap splits them; the lantern stands or hangs; and the Autumn Leaf Table Runner dresses a Feast
 * Table length without touching its dishes, comes back with shears and drops with the length.
 */
public class ThanksgivingDecorGameTests {
	/** The floor props (tools/thanksgiving_decor.py PROPS, kinds "floor" and "candles"). */
	private static final List<String> FLOOR = List.of("pumpkin_and_gourd_stack", "wicker_apple_basket", "pear_basket", "root_vegetable_hamper",
			"harvest_place_setting", "napkin_and_cutlery_caddy", "bread_basket", "gravy_boat", "covered_butter_dish", "pie_stand",
			"pumpkin_candle_centerpiece", "turkey_statuette", "squirrel_acorn_figurine", "table_cornucopia");

	@GameTest(maxTicks = 40)
	public void everyFloorPropFacesItsPlacerAndDropsItself(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos at = helper.absolutePos(new BlockPos(3, 2, 3));
		ServerPlayer player = standing(helper, new BlockPos(3, 2, 1));
		for (String id : FLOOR) {
			place(player, helper, new BlockPos(3, 2, 3), new ItemStack(item(id)));
			BlockState placed = level.getBlockState(at);
			helper.assertTrue(placed.is(block(id)) && placed.getValue(FacingDecorationBlock.FACING) == Direction.NORTH,
					id + " stands where it was placed, facing the player: " + placed);
			helper.assertTrue(player.getMainHandItem().isEmpty(), id + " is used up placing it");
			level.destroyBlock(at, true);
			helper.assertTrue(level.getBlockState(at).isAir() && dropped(helper, item(id)) == 1, id + ", broken, drops itself once");
			clearDrops(helper);
		}
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void theCandleCenterpieceLightsAndSnuffs(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos at = helper.absolutePos(new BlockPos(3, 2, 3));
		ServerPlayer player = standing(helper, new BlockPos(3, 2, 1));
		place(player, helper, new BlockPos(3, 2, 3), new ItemStack(item("pumpkin_candle_centerpiece")));
		helper.assertTrue(!level.getBlockState(at).getValue(CandleCenterpieceBlock.LIT) && level.getBlockState(at).getLightEmission() == 0,
				"It is placed unlit, giving no light");
		use(player, level, at, new ItemStack(Items.FLINT_AND_STEEL));
		helper.assertTrue(level.getBlockState(at).getValue(CandleCenterpieceBlock.LIT)
				&& level.getBlockState(at).getLightEmission() == CandleCenterpieceBlock.LIGHT, "Flint and steel lights its candles");
		use(player, level, at, ItemStack.EMPTY);
		helper.assertTrue(!level.getBlockState(at).getValue(CandleCenterpieceBlock.LIT), "An empty hand snuffs them");
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void garlandsOnOneWallJoin(GameTestHelper helper) {
		floor(helper);
		Block garland = block("autumn_leaf_wall_garland");
		// A wall to the south of a row of three garlands facing north; set from their neighbours, as placing by hand would.
		for (int x = 1; x <= 5; x++) {
			helper.setBlock(new BlockPos(x, 2, 4), Blocks.STONE);
		}
		for (int x = 2; x <= 4; x++) {
			BlockPos pos = new BlockPos(x, 2, 3);
			helper.setBlock(pos, garland.defaultBlockState().setValue(WallDecorationBlock.FACING, Direction.NORTH));
		}
		for (int x = 2; x <= 4; x++) {
			BlockPos pos = new BlockPos(x, 2, 3);
			helper.setBlock(pos, Block.updateFromNeighbourShapes(helper.getBlockState(pos), helper.getLevel(), helper.absolutePos(pos)));
		}
		// Facing north, its left (counter-clockwise) is west.
		BlockState west = helper.getBlockState(new BlockPos(2, 2, 3));
		BlockState middle = helper.getBlockState(new BlockPos(3, 2, 3));
		BlockState east = helper.getBlockState(new BlockPos(4, 2, 3));
		helper.assertTrue(!west.getValue(LeafGarlandBlock.LEFT) && west.getValue(LeafGarlandBlock.RIGHT), "The west end is tied off: " + west);
		helper.assertTrue(middle.getValue(LeafGarlandBlock.LEFT) && middle.getValue(LeafGarlandBlock.RIGHT), "The middle joins both: " + middle);
		helper.assertTrue(east.getValue(LeafGarlandBlock.LEFT) && !east.getValue(LeafGarlandBlock.RIGHT), "The east end is tied off: " + east);
		helper.setBlock(new BlockPos(3, 2, 3), Blocks.AIR);
		helper.assertTrue(!helper.getBlockState(new BlockPos(2, 2, 3)).getValue(LeafGarlandBlock.RIGHT)
				&& !helper.getBlockState(new BlockPos(4, 2, 3)).getValue(LeafGarlandBlock.LEFT), "Taking the middle one down splits them");
		helper.setBlock(new BlockPos(2, 2, 4), Blocks.AIR);
		helper.assertTrue(helper.getBlockState(new BlockPos(2, 2, 3)).isAir(), "A garland falls when its wall goes");
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void theAmberJarLanternStandsOrHangs(GameTestHelper helper) {
		floor(helper);
		Block lantern = block("amber_jar_lantern");
		helper.assertTrue(lantern instanceof LanternBlock, "It is a lantern");
		helper.setBlock(new BlockPos(3, 2, 3), lantern.defaultBlockState());
		helper.setBlock(new BlockPos(5, 4, 3), Blocks.STONE);
		helper.setBlock(new BlockPos(5, 3, 3), lantern.defaultBlockState().setValue(LanternBlock.HANGING, true));
		helper.assertTrue(helper.getBlockState(new BlockPos(3, 2, 3)).getLightEmission() == JugcraftAgriculture.AMBER_JAR_LANTERN_LIGHT
				&& helper.getBlockState(new BlockPos(5, 3, 3)).getValue(LanternBlock.HANGING), "It glows standing, and hangs under a block");
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void theLeafRunnerDressesATableAndComesBack(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = standing(helper, new BlockPos(3, 2, 1));
		helper.setBlock(new BlockPos(3, 2, 3), JugcraftAgriculture.block("feast_table"));
		BlockPos at = helper.absolutePos(new BlockPos(3, 2, 3));
		FeastTableBlockEntity table = (FeastTableBlockEntity) level.getBlockEntity(at);
		table.serve(0, new ItemStack(Items.BREAD, 2));
		use(player, level, at, new ItemStack(item(FeastTableBlock.LEAF_RUNNER), 2));
		helper.assertTrue(level.getBlockState(at).getValue(FeastTableBlock.RUNNER) == FeastTableBlock.Runner.LEAVES
				&& player.getMainHandItem().getCount() == 1, "One runner dresses one length");
		use(player, level, at, player.getMainHandItem());
		helper.assertTrue(player.getMainHandItem().getCount() == 1, "Laying it again uses nothing");
		player.getInventory().clearContent();
		use(player, level, at, new ItemStack(Blocks.CARPET.white().asItem()));
		helper.assertTrue(level.getBlockState(at).getValue(FeastTableBlock.RUNNER) == FeastTableBlock.Runner.LINEN
				&& carried(player, item(FeastTableBlock.LEAF_RUNNER)) == 1, "Swapping cloths gives the leaf runner back");
		player.getInventory().clearContent();
		use(player, level, at, new ItemStack(item(FeastTableBlock.LEAF_RUNNER)));
		player.getInventory().clearContent();
		use(player, level, at, new ItemStack(Items.SHEARS));
		helper.assertTrue(level.getBlockState(at).getValue(FeastTableBlock.RUNNER) == FeastTableBlock.Runner.HARVEST
				&& carried(player, item(FeastTableBlock.LEAF_RUNNER)) == 1 && table.dish(0).getCount() == 2,
				"Shears take it back, the food left on the table");
		player.getInventory().clearContent();
		use(player, level, at, new ItemStack(item(FeastTableBlock.LEAF_RUNNER)));
		level.destroyBlock(at, true);
		helper.assertTrue(dropped(helper, item(FeastTableBlock.LEAF_RUNNER)) == 1 && dropped(helper, item("feast_table")) == 1,
				"Breaking drops one table and its leaf runner");
		helper.succeed();
	}

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	private static ServerPlayer standing(GameTestHelper helper, BlockPos at) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(at);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		return player;
	}

	/** Uses {@code held} on the block at {@code absolute} (an absolute position) from its front. */
	private static void use(ServerPlayer player, ServerLevel level, BlockPos absolute, ItemStack held) {
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.NORTH, 0.5), Direction.NORTH, absolute, false));
	}

	/** Places {@code held} from the player's hand on the top of the block below {@code pos} (relative). */
	private static void place(ServerPlayer player, GameTestHelper helper, BlockPos pos, ItemStack held) {
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		BlockPos below = helper.absolutePos(pos.below());
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(below).relative(Direction.UP, 0.5), Direction.UP, below, false)));
	}

	private static int dropped(GameTestHelper helper, Item item) {
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(3, 3, 4))).inflate(5.0),
				entity -> entity.getItem().is(item)).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	private static void clearDrops(GameTestHelper helper) {
		helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(3, 3, 4))).inflate(5.0))
				.forEach(ItemEntity::discard);
	}

	private static int carried(ServerPlayer player, Item item) {
		int count = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			if (player.getInventory().getItem(slot).is(item)) {
				count += player.getInventory().getItem(slot).getCount();
			}
		}
		return count;
	}
}
