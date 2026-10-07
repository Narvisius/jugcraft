package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.LeafBlowerItem;
import io.github.jimbozoomer.jugcraft.agriculture.LeafPileBlock;
import io.github.jimbozoomer.jugcraft.tools.Chargeable;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * Game tests for the leaf blower (fall addition 30): its charge, the stream's push on items and mobs, herding leaf piles,
 * blowing out candles, and vacuuming. Run by CI's {@code runGameTest}. The blower faces east (+x) from the west end of
 * a stone floor.
 */
public class LeafBlowerGameTests {
	private static final String ARENA = "jugcraft-test:drone_tower";

	private static ServerPlayer blower(GameTestHelper helper, BlockPos standAt, GameType mode) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(mode);
		player.getInventory().clearContent();
		BlockPos absolute = helper.absolutePos(standAt);
		player.snapTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5, -90.0F, 15.0F);
		ItemStack stack = new ItemStack(JugcraftAgriculture.item(LeafBlowerItem.ID));
		Chargeable.setEnergy(stack, LeafBlowerItem.CAPACITY);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		return player;
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 2; x <= 20; x++) {
			for (int z = 6; z <= 14; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
	}

	private static Block pile(String colour) {
		return JugcraftAgriculture.block(colour + "_leaf_pile");
	}

	private static int layers(GameTestHelper helper, BlockPos pos, Block pile) {
		BlockState state = helper.getBlockState(pos);
		return state.is(pile) ? state.getValue(LeafPileBlock.LAYERS) : 0;
	}

	private static boolean earned(ServerPlayer player, String id) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Jugcraft.id(id));
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	/** Flat, it won't start; charged, it does, and each tick of blowing costs its JE until it runs flat and stops. */
	@GameTest(structure = ARENA, skyAccess = true)
	public void itRunsOnItsCharge(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = blower(helper, new BlockPos(4, 1, 10), GameType.SURVIVAL);
		ItemStack stack = player.getMainHandItem();
		Chargeable.setEnergy(stack, 0);
		helper.assertTrue(stack.getItem().use(level, player, InteractionHand.MAIN_HAND) == InteractionResult.FAIL, "Flat, it won't start");
		Chargeable.setEnergy(stack, 10);
		helper.assertTrue(stack.getItem().use(level, player, InteractionHand.MAIN_HAND) == InteractionResult.CONSUME, "Charged, it starts");
		LeafBlowerItem item = (LeafBlowerItem) stack.getItem();
		int duration = item.getUseDuration(stack, player);
		item.onUseTick(level, player, stack, duration - 1);
		item.onUseTick(level, player, stack, duration - 2);
		helper.assertTrue(Chargeable.energy(stack) == 10 - 2 * LeafBlowerItem.BLOW_JE, "Each tick costs " + LeafBlowerItem.BLOW_JE + " JE: " + Chargeable.energy(stack));
		item.onUseTick(level, player, stack, duration - 3);
		helper.assertTrue(Chargeable.energy(stack) == 2 && !player.isUsingItem(), "Too flat for another tick, it stops");
		helper.assertTrue(Chargeable.capacity(stack) == LeafBlowerItem.CAPACITY, "It holds " + LeafBlowerItem.CAPACITY + " JE");
		helper.succeed();
	}

	/** Items in the stream are blown along it, a pig more gently; what is behind isn't touched. */
	@GameTest(structure = ARENA, skyAccess = true)
	public void itBlowsItemsAndMobsAlong(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = blower(helper, new BlockPos(4, 1, 10), GameType.SURVIVAL);
		player.setXRot(0.0F);
		ItemEntity ahead = new ItemEntity(level, helper.absolutePos(new BlockPos(8, 2, 10)).getX() + 0.5,
				helper.absolutePos(new BlockPos(8, 2, 10)).getY(), helper.absolutePos(new BlockPos(8, 2, 10)).getZ() + 0.5, new ItemStack(Items.OAK_LEAVES));
		ItemEntity behind = new ItemEntity(level, helper.absolutePos(new BlockPos(2, 2, 10)).getX() + 0.5,
				helper.absolutePos(new BlockPos(2, 2, 10)).getY(), helper.absolutePos(new BlockPos(2, 2, 10)).getZ() + 0.5, new ItemStack(Items.OAK_LEAVES));
		ahead.setDeltaMovement(Vec3.ZERO);
		behind.setDeltaMovement(Vec3.ZERO);
		level.addFreshEntity(ahead);
		level.addFreshEntity(behind);
		Pig pig = EntityTypes.PIG.create(level, EntitySpawnReason.TRIGGERED);
		helper.assertTrue(pig != null, "A pig");
		BlockPos at = helper.absolutePos(new BlockPos(9, 1, 10));
		pig.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
		pig.setDeltaMovement(Vec3.ZERO);
		level.addFreshEntity(pig);
		LeafBlowerItem.blow(level, player, false);
		helper.assertTrue(ahead.getDeltaMovement().x > 0.05, "An item ahead is blown along: " + ahead.getDeltaMovement());
		helper.assertTrue(pig.getDeltaMovement().x > 0.0 && pig.getDeltaMovement().x < ahead.getDeltaMovement().x,
				"a pig more gently: " + pig.getDeltaMovement());
		helper.assertTrue(behind.getDeltaMovement().x == 0.0, "and one behind isn't touched");
		helper.succeed();
	}

	/**
	 * Another player is pushed only where the user could fight them: a team-mate with friendly fire off isn't. (Every
	 * mock player shares one name, so the team is made and gone within the tick, before any other test runs.)
	 */
	@GameTest(structure = ARENA, skyAccess = true)
	public void itLeavesTeamMatesAlone(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = blower(helper, new BlockPos(4, 1, 10), GameType.SURVIVAL);
		player.setXRot(0.0F);
		ServerPlayer friend = helper.makeMockServerPlayerInLevel();
		BlockPos at = helper.absolutePos(new BlockPos(8, 1, 10));
		friend.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
		friend.setDeltaMovement(Vec3.ZERO);
		Scoreboard scoreboard = level.getServer().getScoreboard();
		PlayerTeam team = scoreboard.addPlayerTeam("jugcraft_leaf_blower_test");
		team.setAllowFriendlyFire(false);
		scoreboard.addPlayerToTeam(friend.getScoreboardName(), team);
		scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
		boolean allies = !player.canHarmPlayer(friend);
		LeafBlowerItem.blow(level, player, false);
		scoreboard.removePlayerTeam(team);
		helper.assertTrue(allies, "Team-mates without friendly fire can't fight");
		helper.assertTrue(friend.getDeltaMovement().horizontalDistance() == 0.0, "so the stream leaves a team-mate be: " + friend.getDeltaMovement());
		helper.succeed();
	}

	/**
	 * A pile in the stream gives a layer to the block beyond it; a pile against a wall stays; a pile can't heap past a full
	 * one; it earns Gone with the Wind. In adventure mode nothing moves.
	 */
	@GameTest(structure = ARENA, skyAccess = true)
	public void itHerdsLeafPiles(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		Block red = pile("red");
		ServerPlayer player = blower(helper, new BlockPos(4, 1, 10), GameType.SURVIVAL);
		helper.setBlock(new BlockPos(7, 1, 10), red.defaultBlockState().setValue(LeafPileBlock.LAYERS, 2));
		helper.setBlock(new BlockPos(9, 1, 9), Blocks.STONE);
		helper.setBlock(new BlockPos(8, 1, 9), red.defaultBlockState().setValue(LeafPileBlock.LAYERS, 3));
		// A leaf tick of blowing: the leaves move, and the advancement is given for it.
		LeafBlowerItem.blow(level, player, true);
		helper.assertTrue(layers(helper, new BlockPos(7, 1, 10), red) == 1 && layers(helper, new BlockPos(8, 1, 10), red) == 1,
				"A layer is blown on to the next block: " + layers(helper, new BlockPos(7, 1, 10), red) + " and " + layers(helper, new BlockPos(8, 1, 10), red));
		helper.assertTrue(layers(helper, new BlockPos(8, 1, 9), red) == 3, "One against a wall stays");
		helper.assertTrue(earned(player, "gone_with_the_wind"), "Moving a layer earns Gone with the Wind");

		ServerPlayer visitor = blower(helper, new BlockPos(4, 1, 13), GameType.ADVENTURE);
		helper.setBlock(new BlockPos(7, 1, 13), red.defaultBlockState().setValue(LeafPileBlock.LAYERS, 2));
		helper.assertTrue(LeafBlowerItem.blowLeaves(level, visitor, visitor.getEyePosition(), visitor.getLookAngle()) == 0
				&& layers(helper, new BlockPos(7, 1, 13), red) == 2, "In adventure mode nothing moves");
		helper.succeed();
	}

	/** A pile won't heap past a full pile ahead of it, nor onto another colour. */
	@GameTest(structure = ARENA, skyAccess = true)
	public void pilesStopAtFullOrOtherPiles(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		Block red = pile("red");
		Block yellow = pile("yellow");
		ServerPlayer player = blower(helper, new BlockPos(4, 1, 10), GameType.SURVIVAL);
		helper.setBlock(new BlockPos(7, 1, 10), red.defaultBlockState().setValue(LeafPileBlock.LAYERS, 2));
		helper.setBlock(new BlockPos(8, 1, 10), yellow.defaultBlockState());
		helper.setBlock(new BlockPos(8, 1, 11), red.defaultBlockState().setValue(LeafPileBlock.LAYERS, LeafPileBlock.MAX_LAYERS));
		helper.setBlock(new BlockPos(9, 1, 10), Blocks.STONE);
		helper.setBlock(new BlockPos(9, 1, 11), Blocks.STONE);
		helper.setBlock(new BlockPos(7, 1, 11), red.defaultBlockState());
		LeafBlowerItem.blowLeaves(level, player, player.getEyePosition(), player.getLookAngle());
		helper.assertTrue(layers(helper, new BlockPos(7, 1, 10), red) == 2 && layers(helper, new BlockPos(8, 1, 10), yellow) == 1,
				"Red won't heap onto yellow");
		helper.assertTrue(layers(helper, new BlockPos(7, 1, 11), red) == 1 && layers(helper, new BlockPos(8, 1, 11), red) == LeafPileBlock.MAX_LAYERS,
				"nor past a full pile");
		helper.succeed();
	}

	/** Lit candles in the stream blow out. */
	@GameTest(structure = ARENA, skyAccess = true)
	public void itBlowsOutCandles(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = blower(helper, new BlockPos(4, 1, 10), GameType.SURVIVAL);
		helper.setBlock(new BlockPos(7, 1, 10), Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true));
		helper.setBlock(new BlockPos(2, 1, 10), Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true));
		helper.assertTrue(LeafBlowerItem.blowOutCandles(level, player, player.getEyePosition(), player.getLookAngle()) == 1, "One candle is in the stream");
		helper.assertTrue(!helper.getBlockState(new BlockPos(7, 1, 10)).getValue(CandleBlock.LIT), "It blows out");
		helper.assertTrue(helper.getBlockState(new BlockPos(2, 1, 10)).getValue(CandleBlock.LIT), "The one behind stays lit");
		helper.succeed();
	}

	/** Sneaking, it vacuums: a layer of a pile in reach comes up into the inventory, one out of reach doesn't; items are drawn in. */
	@GameTest(structure = ARENA, skyAccess = true)
	public void itVacuumsLeaves(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		Block orange = pile("orange");
		ServerPlayer player = blower(helper, new BlockPos(4, 1, 10), GameType.SURVIVAL);
		helper.setBlock(new BlockPos(7, 1, 10), orange.defaultBlockState().setValue(LeafPileBlock.LAYERS, 2));
		helper.setBlock(new BlockPos(11, 1, 10), orange.defaultBlockState());
		BlockPos itemAt = helper.absolutePos(new BlockPos(7, 2, 10));
		ItemEntity item = new ItemEntity(level, itemAt.getX() + 0.5, itemAt.getY(), itemAt.getZ() + 0.5, new ItemStack(Items.OAK_LEAVES));
		item.setDeltaMovement(Vec3.ZERO);
		level.addFreshEntity(item);
		LeafBlowerItem.vacuum(level, player, true);
		helper.assertTrue(layers(helper, new BlockPos(7, 1, 10), orange) == 1, "A layer comes up");
		helper.assertTrue(player.getInventory().countItem(orange.asItem()) == 1, "into the inventory");
		helper.assertTrue(layers(helper, new BlockPos(11, 1, 10), orange) == 1, "One out of reach stays");
		helper.assertTrue(item.getDeltaMovement().x < 0.0, "An item is drawn in: " + item.getDeltaMovement());
		helper.succeed();
	}

	/** Its recipe and advancement load. */
	@GameTest
	public void leafBlowerDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(LeafBlowerItem.ID))).isPresent(),
				"The Leaf Blower has a recipe");
		helper.assertTrue(level.getServer().getAdvancements().get(Jugcraft.id("gone_with_the_wind")) != null, "Gone with the Wind loads");
		helper.succeed();
	}
}
