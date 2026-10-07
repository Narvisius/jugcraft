package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.Epitaph;
import io.github.jimbozoomer.jugcraft.agriculture.HeadstoneBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HeadstoneBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
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
 * In-game tests for the graveyard pack's monuments: each places whole from its item (the Angel at the Tomb two blocks
 * wide, with her wings over its left end; the obelisk and the trumpeting angel four blocks tall; the mortsafe two long)
 * or not at all, weathers every block together, breaks as one dropping once with its epitaph, and its data loads.
 */
public class MonumentGameTests {
	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
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

	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos pos, Direction side) {
		BlockPos absolute = helper.absolutePos(pos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(side, 0.5), side, absolute, false);
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	private static int dropped(GameTestHelper helper, BlockPos pos, Item item) {
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(4.0)).stream()
				.filter(entity -> entity.getItem().is(item)).mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	/** Stands the player two blocks in front of (north of) {@code spot}, in easy reach, still looking south. */
	private static void standBefore(GameTestHelper helper, ServerPlayer player, BlockPos spot) {
		BlockPos absolute = helper.absolutePos(spot);
		player.setPos(absolute.getX() + 0.5, absolute.getY() + 1, absolute.getZ() - 1.5);
	}

	private static boolean isPart(GameTestHelper helper, BlockPos pos, String id, int part) {
		BlockState state = helper.getBlockState(pos);
		return state.is(block(id)) && state.getValue(HeadstoneBlock.PART) == part && state.getValue(HeadstoneBlock.FACING) == Direction.NORTH;
	}

	/**
	 * Placed by a player looking south (so they face north): the angel's second altar half is to the placer's right
	 * (west, -x) and her wings above it; the obelisk and trumpeting angel rise four blocks; the mortsafe runs back south.
	 * With a block where her wings go, the angel is not placed.
	 */
	@GameTest
	public void monumentsPlaceWhole(GameTestHelper helper) {
		floor(helper);
		ServerPlayer mason = player(helper, new BlockPos(4, 2, 0), new ItemStack(item("angel_at_the_tomb")));
		helper.assertTrue(use(helper, mason, new BlockPos(5, 1, 2), Direction.UP).consumesAction(), "The angel is placed");
		helper.assertTrue(isPart(helper, new BlockPos(5, 2, 2), "angel_at_the_tomb", 0) && isPart(helper, new BlockPos(4, 2, 2), "angel_at_the_tomb", 1)
				&& isPart(helper, new BlockPos(4, 3, 2), "angel_at_the_tomb", 2), "Her altar's two halves and her wings above the left one");
		helper.assertBlockNotPresent(block("angel_at_the_tomb"), new BlockPos(5, 3, 2));

		mason.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("grand_obelisk")));
		standBefore(helper, mason, new BlockPos(1, 1, 2));
		use(helper, mason, new BlockPos(1, 1, 2), Direction.UP);
		for (int part = 0; part < 4; part++) {
			helper.assertTrue(isPart(helper, new BlockPos(1, 2 + part, 2), "grand_obelisk", part), "Obelisk part " + part);
		}
		mason.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("mortsafe")));
		standBefore(helper, mason, new BlockPos(7, 1, 2));
		use(helper, mason, new BlockPos(7, 1, 2), Direction.UP);
		helper.assertTrue(isPart(helper, new BlockPos(7, 2, 2), "mortsafe", 0) && isPart(helper, new BlockPos(7, 2, 3), "mortsafe", 1),
				"The mortsafe runs back over the grave");

		helper.setBlock(new BlockPos(4, 3, 5), Blocks.STONE);
		mason.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("angel_at_the_tomb")));
		standBefore(helper, mason, new BlockPos(5, 1, 5));
		use(helper, mason, new BlockPos(5, 1, 5), Direction.UP);
		helper.assertBlockNotPresent(block("angel_at_the_tomb"), new BlockPos(5, 2, 5));
		helper.succeed();
	}

	/** Bone meal on the angel's wings ages all three of her blocks; breaking her wings breaks the whole, dropping her once with her epitaph. */
	@GameTest
	public void monumentsWeatherAndBreakAsOne(GameTestHelper helper) {
		floor(helper);
		ServerPlayer mason = player(helper, new BlockPos(4, 2, 0), new ItemStack(item("angel_at_the_tomb")));
		use(helper, mason, new BlockPos(5, 1, 2), Direction.UP);
		HeadstoneBlockEntity altar = helper.getBlockEntity(new BlockPos(5, 2, 2), HeadstoneBlockEntity.class);
		altar.engrave(Epitaph.of(List.of("BELOVED", "1850 - 1890")));
		mason.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE_MEAL));
		use(helper, mason, new BlockPos(4, 3, 2), Direction.SOUTH);
		for (BlockPos pos : List.of(new BlockPos(5, 2, 2), new BlockPos(4, 2, 2), new BlockPos(4, 3, 2))) {
			helper.assertTrue(HeadstoneBlock.stage(helper.getBlockState(pos)) == 1, "Every block of her ages together: " + pos);
		}
		ServerLevel level = helper.getLevel();
		level.destroyBlock(helper.absolutePos(new BlockPos(4, 3, 2)), true);
		helper.assertBlockNotPresent(block("angel_at_the_tomb"), new BlockPos(5, 2, 2));
		helper.assertBlockNotPresent(block("angel_at_the_tomb"), new BlockPos(4, 2, 2));
		helper.assertTrue(dropped(helper, new BlockPos(5, 2, 2), item("angel_at_the_tomb")) == 1, "Breaking her wings drops her once");
		ItemStack broken = level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(5, 2, 2))).inflate(4.0)).stream()
				.map(ItemEntity::getItem).filter(s -> s.is(item("angel_at_the_tomb"))).findFirst().orElse(ItemStack.EMPTY);
		Epitaph carried = broken.get(JugcraftAgriculture.EPITAPH);
		helper.assertTrue(carried != null && carried.lines().get(0).equals("BELOVED"), "with her epitaph");
		helper.succeed();
	}

	/** Every monument's recipe and loot table loads. */
	@GameTest
	public void monumentDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("grand_obelisk", "draped_urn", "angel_at_the_tomb", "trumpeting_angel", "mortsafe", "faithful_hound")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), id + " has a recipe");
			helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
					Jugcraft.id("blocks/" + id))) != LootTable.EMPTY, id + "'s loot loads");
		}
		helper.succeed();
	}
}
