package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.GraveMoundBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.MourningAngelBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PopUpSkeletonBlock;
import io.github.jimbozoomer.jugcraft.agriculture.ScareProp;
import io.github.jimbozoomer.jugcraft.agriculture.ScarePropBlockEntity;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the graveyard decorations: the wrought-iron Cemetery Fence and Gate (join each other, other
 * non-wooden fences and walls; the gate opens by hand), the crypt set (the Crypt Door opens by hand and drops once,
 * pillars turn to the face they are placed on), the Grave Mound and the Pop-Up Skeleton (a passer-by sets them off, a
 * sneaking one doesn't; they come down after their time and rest before going again; redstone holds them up; their
 * timers are saved), the Mourning Angel (two blocks, drops once), and that their data loads.
 */
public class Decor3GameTests {
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
		stand(helper, player, standAt);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	private static void stand(GameTestHelper helper, ServerPlayer player, BlockPos at) {
		BlockPos absolute = helper.absolutePos(at);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
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

	// ---------------------------------------------------------------- the cemetery fence and gate

	/**
	 * Cemetery fences join each other, a gate between them and nether brick fences (non-wooden fences), but not oak
	 * fences; the gate opens by hand, and between two walls it sits lower in the wall.
	 */
	@GameTest
	public void cemeteryFencesAndGatesJoin(GameTestHelper helper) {
		floor(helper);
		BlockPos a = new BlockPos(1, 2, 3);
		helper.setBlock(a, block("cemetery_fence"));
		helper.setBlock(a.east(), block("cemetery_fence"));
		helper.setBlock(a.east(2), block("cemetery_gate").defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH));
		// Set from its neighbours, as placing it by hand would (setBlock alone keeps the state it is given).
		helper.setBlock(a.east(3), Block.updateFromNeighbourShapes(block("cemetery_fence").defaultBlockState(), helper.getLevel(),
				helper.absolutePos(a.east(3))));
		helper.setBlock(a.north(), Blocks.NETHER_BRICK_FENCE);
		helper.setBlock(a.south(), Blocks.OAK_FENCE);
		BlockState fence = helper.getBlockState(a);
		helper.assertTrue(fence.getValue(FenceBlock.EAST) && fence.getValue(FenceBlock.NORTH) && !fence.getValue(FenceBlock.SOUTH),
				"A cemetery fence joins its neighbour and a nether brick fence, not an oak fence: " + fence);
		helper.assertTrue(helper.getBlockState(a.east()).getValue(FenceBlock.EAST) && helper.getBlockState(a.east(3)).getValue(FenceBlock.WEST),
				"Fences on both sides join the gate: " + helper.getBlockState(a.east()) + " | " + helper.getBlockState(a.east(2)) + " | "
						+ helper.getBlockState(a.east(3)));
		helper.assertTrue(fence.is(BlockTags.FENCES) && !fence.is(BlockTags.WOODEN_FENCES)
				&& helper.getBlockState(a.east(2)).is(BlockTags.FENCE_GATES), "The fence and gate are in vanilla's tags");

		ServerPlayer player = player(helper, new BlockPos(3, 2, 1), ItemStack.EMPTY);
		use(helper, player, a.east(2), Direction.NORTH);
		helper.assertTrue(helper.getBlockState(a.east(2)).getValue(FenceGateBlock.OPEN), "The gate opens by hand");

		BlockPos walled = new BlockPos(3, 2, 6);
		helper.setBlock(walled, block("cemetery_gate").defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH));
		helper.setBlock(walled.west(), Blocks.COBBLESTONE_WALL);
		helper.setBlock(walled.east(), Blocks.COBBLESTONE_WALL);
		helper.assertTrue(helper.getBlockState(walled).getValue(FenceGateBlock.IN_WALL), "Between walls the gate sits in the wall");
		helper.succeed();
	}

	// ---------------------------------------------------------------- the crypt set

	/**
	 * The Crypt Door is placed two tall, opens and shuts by hand, and drops once when its top is broken; a crypt stone
	 * pillar turns to lie along the face it is placed against.
	 */
	@GameTest
	public void theCryptDoorOpensByHand(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos stand = new BlockPos(3, 1, 3);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("crypt_door")));
		player.setYRot(180.0F);
		place(helper, player, stand, Direction.UP);
		BlockPos lower = stand.above();
		helper.assertTrue(helper.getBlockState(lower).is(block("crypt_door")) && helper.getBlockState(lower).getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
				&& helper.getBlockState(lower.above()).is(block("crypt_door")), "The door stands two tall");

		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, lower, Direction.SOUTH);
		helper.assertTrue(helper.getBlockState(lower).getValue(DoorBlock.OPEN) && helper.getBlockState(lower.above()).getValue(DoorBlock.OPEN),
				"It opens by hand, both halves");
		use(helper, player, lower.above(), Direction.SOUTH);
		helper.assertTrue(!helper.getBlockState(lower).getValue(DoorBlock.OPEN), "and shuts again");

		BlockPos wall = new BlockPos(6, 2, 1);
		helper.setBlock(wall, block("crypt_stone"));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("crypt_stone_pillar")));
		place(helper, player, wall, Direction.WEST);
		helper.assertTrue(helper.getBlockState(wall.west()).getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X,
				"A pillar placed against the side of a block lies along it");

		level.destroyBlock(helper.absolutePos(lower.above()), true);
		helper.succeedWhen(() -> {
			helper.assertBlockNotPresent(block("crypt_door"), lower);
			helper.assertTrue(dropped(helper, item("crypt_door")) == 1, "Breaking its top drops one door");
		});
	}

	// ---------------------------------------------------------------- the scare props

	/**
	 * A sneaking player doesn't set a grave mound off; walking past does (the hand rises). Its timers survive a save
	 * and load. Once everyone has gone it sinks after its time, and stays down through its rest even with someone near.
	 */
	@GameTest(maxTicks = 200)
	public void aPasserBySetsOffTheGraveMound(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, block("grave_mound"));
		ScarePropBlockEntity mound = helper.getBlockEntity(pos, ScarePropBlockEntity.class);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 5), ItemStack.EMPTY);
		player.setShiftKeyDown(true);
		helper.assertFalse(mound.update(level), "A sneaking player creeps past");
		player.setShiftKeyDown(false);
		helper.assertTrue(mound.update(level) && helper.getBlockState(pos).getValue(ScareProp.RAISED), "Walking past, a hand claws up");

		CompoundTag saved = mound.saveWithoutMetadata(level.registryAccess());
		ScarePropBlockEntity copy = new ScarePropBlockEntity(mound.getBlockPos(), mound.getBlockState());
		copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
		helper.assertTrue(copy.saveWithoutMetadata(level.registryAccess()).equals(saved), "Its timers are saved");

		stand(helper, player, new BlockPos(3, 2, 7).offset(0, 0, 20));
		helper.runAfterDelay(GraveMoundBlock.UP_TICKS + 2 * ScarePropBlockEntity.PERIOD, () -> {
			helper.assertFalse(helper.getBlockState(pos).getValue(ScareProp.RAISED), "After its time the hand sinks");
			stand(helper, player, new BlockPos(3, 2, 5));
			helper.assertFalse(mound.update(level), "and it rests before it can go off again");
			helper.succeed();
		});
	}

	/** A redstone signal holds a Pop-Up Skeleton up with nobody near; it comes down when the signal goes. */
	@GameTest(maxTicks = 100)
	public void redstoneHoldsThePopUpSkeletonUp(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, block("pop_up_skeleton"));
		ScarePropBlockEntity skeleton = helper.getBlockEntity(pos, ScarePropBlockEntity.class);
		helper.assertFalse(skeleton.update(level), "Nobody near, it stays in its crate");
		helper.setBlock(pos.east(), Blocks.REDSTONE_BLOCK);
		helper.assertTrue(helper.getBlockState(pos).getValue(ScareProp.RAISED), "A redstone signal springs it up at once");
		helper.runAfterDelay(PopUpSkeletonBlock.UP_TICKS + 2 * ScarePropBlockEntity.PERIOD, () -> {
			helper.assertTrue(helper.getBlockState(pos).getValue(ScareProp.RAISED), "and holds it up");
			helper.setBlock(pos.east(), Blocks.AIR);
			helper.succeedWhen(() -> helper.assertFalse(helper.getBlockState(pos).getValue(ScareProp.RAISED), "Without the signal it drops back"));
		});
	}

	/** A Pop-Up Skeleton springs up for a passer-by (2.5 blocks) but not for one farther off. */
	@GameTest
	public void thePopUpSkeletonSpringsForAPasserBy(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, block("pop_up_skeleton").defaultBlockState().setValue(PopUpSkeletonBlock.FACING, Direction.SOUTH));
		ScarePropBlockEntity skeleton = helper.getBlockEntity(pos, ScarePropBlockEntity.class);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 7), ItemStack.EMPTY);
		helper.assertFalse(skeleton.update(level), "Four blocks off, nothing happens");
		stand(helper, player, new BlockPos(3, 2, 5));
		helper.assertTrue(skeleton.update(level) && helper.getBlockState(pos).getValue(ScareProp.RAISED)
				&& helper.getBlockState(pos).getValue(PopUpSkeletonBlock.FACING) == Direction.SOUTH, "Two blocks off, it springs up");
		helper.succeed();
	}

	// ---------------------------------------------------------------- the mourning angel

	/** The angel is placed two tall facing the player, needs room above, and drops once when its head is broken. */
	@GameTest
	public void theMourningAngelStandsTwoTall(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos stand = new BlockPos(3, 1, 3);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("mourning_angel"), 2));
		player.setYRot(180.0F);
		place(helper, player, stand, Direction.UP);
		BlockState lower = helper.getBlockState(stand.above());
		helper.assertTrue(lower.is(block("mourning_angel")) && lower.getValue(MourningAngelBlock.HALF) == DoubleBlockHalf.LOWER
				&& helper.getBlockState(stand.above(2)).is(block("mourning_angel")), "The angel stands two tall: " + lower);
		helper.assertTrue(lower.getValue(MourningAngelBlock.FACING) == Direction.SOUTH, "facing whoever placed it");

		BlockPos low = new BlockPos(6, 1, 6);
		helper.setBlock(low.above(2), Blocks.STONE);
		place(helper, player, low, Direction.UP);
		helper.assertBlockNotPresent(block("mourning_angel"), low.above());

		level.destroyBlock(helper.absolutePos(stand.above(2)), true);
		helper.succeedWhen(() -> {
			helper.assertBlockNotPresent(block("mourning_angel"), stand.above());
			helper.assertTrue(dropped(helper, item("mourning_angel")) == 1, "Breaking its head drops one angel");
		});
	}

	/** The recipes (with the stonecutter's), loot tables and tool tags load. */
	@GameTest
	public void graveyardDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("cemetery_fence", "cemetery_gate", "crypt_stone", "crypt_stone_pillar", "crypt_door", "grave_mound", "mourning_angel",
				"pop_up_skeleton", "chiseled_crypt_stone_from_stonecutting", "crypt_stone_pillar_from_stonecutting")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), "Recipe " + id + " loads");
		}
		for (String id : List.of("cemetery_fence", "cemetery_gate", "crypt_stone", "chiseled_crypt_stone", "crypt_stone_pillar", "crypt_door",
				"grave_mound", "mourning_angel", "pop_up_skeleton")) {
			LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("blocks/" + id)));
			helper.assertTrue(table != LootTable.EMPTY, "The " + id + " drops itself");
		}
		helper.assertTrue(block("crypt_stone").defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)
				&& block("grave_mound").defaultBlockState().is(BlockTags.MINEABLE_WITH_SHOVEL)
				&& block("pop_up_skeleton").defaultBlockState().is(BlockTags.MINEABLE_WITH_AXE)
				&& block("crypt_door").defaultBlockState().is(BlockTags.DOORS), "Tools and tags fit");
		helper.succeed();
	}
}
