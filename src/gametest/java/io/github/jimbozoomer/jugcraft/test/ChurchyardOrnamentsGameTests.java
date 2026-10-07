package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.BonePileBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GiantBoneHandBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HeadstoneBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.OssuaryWallBlock;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for Halloween decorations batch 15, the churchyard's ornaments: a bone pile heaps to four layers as more
 * are placed, gives one pile a layer and goes with its ground; an ossuary wall faces whoever placed it; the giant bone hand
 * stands two blocks tall, clenches both halves while powered and opens again, and breaks as one; the witch's lantern
 * stands or hangs and gives its light; the gargoyle is a two-block headstone that weathers; and their data loads.
 */
public class ChurchyardOrnamentsGameTests {
	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, ItemStack held) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos on, Direction face) {
		BlockPos absolute = helper.absolutePos(on);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(face, 0.5), face, absolute, false);
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	/** A bone pile heaps a layer each time another is placed on it, up to four; it gives one pile a layer; it needs ground. */
	@GameTest
	public void bonePilesHeapUp(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos at = new BlockPos(3, 2, 3);
		ServerPlayer keeper = player(helper, new BlockPos(3, 2, 0), new ItemStack(item("bone_pile"), 8));
		use(helper, keeper, new BlockPos(3, 1, 3), Direction.UP);
		helper.assertTrue(helper.getBlockState(at).is(block("bone_pile")) && helper.getBlockState(at).getValue(BonePileBlock.LAYERS) == 1,
				"A bone pile is placed");
		for (int i = 0; i < 5; i++) {
			use(helper, keeper, at, Direction.UP);
		}
		helper.assertTrue(helper.getBlockState(at).getValue(BonePileBlock.LAYERS) == BonePileBlock.MAX_LAYERS, "It heaps to four layers and no more");
		List<ItemStack> drops = Block.getDrops(helper.getBlockState(at), level, helper.absolutePos(at), null);
		helper.assertTrue(drops.stream().filter(s -> s.is(item("bone_pile"))).mapToInt(ItemStack::getCount).sum() == BonePileBlock.MAX_LAYERS,
				"It gives back one pile a layer");
		helper.assertTrue(!block("bone_pile").defaultBlockState().canSurvive(level, helper.absolutePos(new BlockPos(6, 4, 6))), "Not in the air");
		helper.setBlock(new BlockPos(3, 1, 3), Blocks.AIR);
		helper.assertTrue(!helper.getBlockState(at).is(block("bone_pile")), "It goes with its ground");
		helper.succeed();
	}

	/** An ossuary wall faces whoever placed it, and drops itself. */
	@GameTest
	public void ossuaryWallsFaceTheirBuilder(GameTestHelper helper) {
		floor(helper);
		ServerPlayer builder = player(helper, new BlockPos(3, 2, 0), new ItemStack(item("ossuary_wall")));
		use(helper, builder, new BlockPos(3, 1, 4), Direction.UP);
		BlockState wall = helper.getBlockState(new BlockPos(3, 2, 4));
		helper.assertTrue(wall.is(block("ossuary_wall")) && wall.getValue(OssuaryWallBlock.FACING) == builder.getDirection().getOpposite(),
				"The wall faces its builder: " + wall);
		helper.assertTrue(Block.getDrops(wall, helper.getLevel(), helper.absolutePos(new BlockPos(3, 2, 4)), null).stream()
				.anyMatch(s -> s.is(item("ossuary_wall"))), "It drops itself");
		helper.succeed();
	}

	/** The giant bone hand stands two tall; power clenches both halves and its loss opens them; breaking either breaks both. */
	@GameTest
	public void giantBoneHandsClench(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer builder = player(helper, new BlockPos(3, 2, 0), new ItemStack(item("giant_bone_hand")));
		use(helper, builder, new BlockPos(3, 1, 3), Direction.UP);
		BlockPos lower = new BlockPos(3, 2, 3);
		helper.assertTrue(helper.getBlockState(lower).is(block("giant_bone_hand"))
				&& helper.getBlockState(lower).getValue(GiantBoneHandBlock.HALF) == DoubleBlockHalf.LOWER
				&& helper.getBlockState(lower.above()).getValue(GiantBoneHandBlock.HALF) == DoubleBlockHalf.UPPER, "The hand stands two tall");
		helper.assertTrue(!helper.getBlockState(lower).getValue(GiantBoneHandBlock.POWERED), "It starts open");
		helper.setBlock(lower.east(), Blocks.REDSTONE_BLOCK);
		helper.assertTrue(helper.getBlockState(lower).getValue(GiantBoneHandBlock.POWERED)
				&& helper.getBlockState(lower.above()).getValue(GiantBoneHandBlock.POWERED), "Powered, both halves clench");
		helper.setBlock(lower.east(), Blocks.AIR);
		helper.assertTrue(!helper.getBlockState(lower).getValue(GiantBoneHandBlock.POWERED)
				&& !helper.getBlockState(lower.above()).getValue(GiantBoneHandBlock.POWERED), "Unpowered, it opens");
		level.destroyBlock(helper.absolutePos(lower.above()), true);
		helper.assertTrue(!helper.getBlockState(lower).is(block("giant_bone_hand")), "Breaking the top breaks the whole hand");
		helper.succeed();
	}

	/** The witch's lantern stands on a floor and hangs under a ceiling, alight either way. */
	@GameTest
	public void witchsLanternsStandAndHang(GameTestHelper helper) {
		floor(helper);
		helper.setBlock(new BlockPos(5, 5, 5), Blocks.STONE);
		ServerPlayer builder = player(helper, new BlockPos(3, 2, 0), new ItemStack(item("witchs_lantern"), 2));
		use(helper, builder, new BlockPos(2, 1, 2), Direction.UP);
		use(helper, builder, new BlockPos(5, 5, 5), Direction.DOWN);
		BlockState standing = helper.getBlockState(new BlockPos(2, 2, 2));
		BlockState hanging = helper.getBlockState(new BlockPos(5, 4, 5));
		helper.assertTrue(standing.is(block("witchs_lantern")) && !standing.getValue(LanternBlock.HANGING), "It stands on the floor");
		helper.assertTrue(hanging.is(block("witchs_lantern")) && hanging.getValue(LanternBlock.HANGING), "It hangs under the ceiling");
		helper.assertTrue(standing.getLightEmission() == JugcraftAgriculture.WITCHS_LANTERN_LIGHT
				&& hanging.getLightEmission() == JugcraftAgriculture.WITCHS_LANTERN_LIGHT, "It gives its light");
		helper.succeed();
	}

	/** The gargoyle is a two-block headstone: placed whole, facing its builder, and bone meal weathers it. */
	@GameTest
	public void gargoylesAreHeadstones(GameTestHelper helper) {
		floor(helper);
		ServerPlayer mason = player(helper, new BlockPos(3, 2, 0), new ItemStack(item("gargoyle")));
		use(helper, mason, new BlockPos(3, 1, 3), Direction.UP);
		BlockState foot = helper.getBlockState(new BlockPos(3, 2, 3));
		helper.assertTrue(foot.is(block("gargoyle")) && foot.getValue(HeadstoneBlock.PART) == 0
				&& helper.getBlockState(new BlockPos(3, 3, 3)).getValue(HeadstoneBlock.PART) == 1, "The gargoyle stands on its plinth, two blocks");
		mason.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE_MEAL));
		use(helper, mason, new BlockPos(3, 2, 3), Direction.NORTH);
		helper.assertTrue(helper.getBlockState(new BlockPos(3, 2, 3)).getValue(HeadstoneBlock.WEATHERING) == 1
				&& helper.getBlockState(new BlockPos(3, 3, 3)).getValue(HeadstoneBlock.WEATHERING) == 1, "Bone meal weathers it, all of it");
		helper.succeed();
	}

	/** Recipes and loot tables load. */
	@GameTest
	public void ornamentDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("bone_pile", "ossuary_wall", "giant_bone_hand", "witchs_lantern")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), id + " has a recipe");
			helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
					Jugcraft.id("blocks/" + id))) != LootTable.EMPTY, id + "'s loot loads");
		}
		helper.succeed();
	}
}
