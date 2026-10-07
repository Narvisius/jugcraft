package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.Squirrel;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for squirrels and acorns (fall addition 24): a squirrel goes for an acorn lying near and takes it; an
 * oak sapling sprouts only on open earth away from saplings and logs; buried on earth the acorn is gone from its paws,
 * on stone it keeps it; an acorn planted by a player is an oak sapling; nuts tempt and breed squirrels (Nuts About
 * Squirrels); and the data loads.
 */
public class SquirrelGameTests {
	private static void floor(GameTestHelper helper, net.minecraft.world.level.block.Block block) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), block);
			}
		}
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		return player;
	}

	private static boolean earned(ServerPlayer player, String id) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Jugcraft.id(id));
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	/** A squirrel with empty paws goes for an acorn lying a few blocks off and takes it. */
	@GameTest(maxTicks = 300)
	public void squirrelsGatherAcorns(GameTestHelper helper) {
		floor(helper, Blocks.GRASS_BLOCK);
		Squirrel squirrel = helper.spawn(JugcraftAgriculture.SQUIRREL, new BlockPos(1, 2, 1));
		ItemEntity acorn = helper.spawnItem(Squirrel.acorn(), new Vec3(5.5, 2.2, 5.5));
		helper.succeedWhen(() -> {
			helper.assertTrue(squirrel.carrying(), "The squirrel has taken the acorn");
			helper.assertTrue(acorn.isRemoved(), "and it is gone from the ground");
		});
	}

	/**
	 * An oak may sprout on open earth, not where there is no earth, nor near a log. Burying on earth empties its paws;
	 * on stone it keeps its acorn. (The room is checked first: a buried acorn may sprout a sapling, which would leave no
	 * room near it.)
	 */
	@GameTest
	public void buriedAcornsMaySproutOaks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper, Blocks.GRASS_BLOCK);
		helper.setBlock(new BlockPos(7, 1, 7), Blocks.STONE);
		helper.assertTrue(Squirrel.roomForSapling(level, helper.absolutePos(new BlockPos(4, 2, 4))), "Open earth has room for an oak");
		helper.assertTrue(!Squirrel.roomForSapling(level, helper.absolutePos(new BlockPos(7, 2, 7))), "Stone has none");
		helper.setBlock(new BlockPos(2, 2, 5), Blocks.OAK_LOG);
		helper.assertTrue(!Squirrel.roomForSapling(level, helper.absolutePos(new BlockPos(4, 2, 5))), "Nor beside a tree");

		Squirrel digger = helper.spawn(JugcraftAgriculture.SQUIRREL, new BlockPos(1, 2, 1));
		digger.setNoAi(true);
		digger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Squirrel.acorn()));
		helper.assertTrue(digger.bury(level) && !digger.carrying(), "On earth, it buries its acorn");
		Squirrel onStone = helper.spawn(JugcraftAgriculture.SQUIRREL, new BlockPos(7, 2, 7));
		onStone.setNoAi(true);
		onStone.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Squirrel.acorn()));
		helper.assertTrue(!onStone.bury(level) && onStone.carrying(), "On stone, it keeps it");
		helper.succeed();
	}

	/** An acorn used on the top of grass plants an oak sapling and is used up; on stone, nothing. */
	@GameTest
	public void acornsPlantOaks(GameTestHelper helper) {
		floor(helper, Blocks.GRASS_BLOCK);
		helper.setBlock(new BlockPos(6, 1, 6), Blocks.STONE);
		ServerPlayer planter = player(helper, new BlockPos(3, 2, 1));
		planter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Squirrel.acorn(), 2));
		for (BlockPos ground : List.of(new BlockPos(3, 1, 3), new BlockPos(6, 1, 6))) {
			BlockPos absolute = helper.absolutePos(ground);
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
			planter.getMainHandItem().useOn(new UseOnContext(planter, InteractionHand.MAIN_HAND, hit));
		}
		helper.assertBlockPresent(Blocks.OAK_SAPLING, new BlockPos(3, 2, 3));
		helper.assertBlockNotPresent(Blocks.OAK_SAPLING, new BlockPos(6, 2, 6));
		helper.assertTrue(planter.getMainHandItem().getCount() == 1, "One acorn is planted");
		helper.succeed();
	}

	/** Acorns and chestnuts are squirrel food, wheat isn't; two squirrels in love breed a kit (Nuts About Squirrels). */
	@GameTest
	public void nutsBreedSquirrels(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper, Blocks.GRASS_BLOCK);
		Squirrel squirrel = helper.spawn(JugcraftAgriculture.SQUIRREL, new BlockPos(2, 2, 2));
		Squirrel partner = helper.spawn(JugcraftAgriculture.SQUIRREL, new BlockPos(3, 2, 2));
		squirrel.setNoAi(true);
		partner.setNoAi(true);
		helper.assertTrue(squirrel.isFood(new ItemStack(Squirrel.acorn())) && squirrel.isFood(new ItemStack(JugcraftAgriculture.item("chestnut")))
				&& !squirrel.isFood(new ItemStack(Items.WHEAT)), "Nuts are squirrel food, wheat isn't");
		ServerPlayer keeper = player(helper, new BlockPos(5, 2, 5));
		squirrel.setInLove(keeper);
		helper.assertTrue(squirrel.getBreedOffspring(level, partner) instanceof Squirrel, "They breed a kit");
		helper.assertTrue(earned(keeper, "nuts_about_squirrels"), "Nuts About Squirrels is earned");
		helper.succeed();
	}

	/** Roasted acorns cook, the advancement loads, and the squirrel's food tag and home tag exist. */
	@GameTest
	public void squirrelDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("roasted_acorns", "roasted_acorns_from_smoking", "roasted_acorns_from_campfire_cooking")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), id + " loads");
		}
		helper.assertTrue(level.getServer().getAdvancements().get(Jugcraft.id("nuts_about_squirrels")) != null, "The advancement loads");
		helper.assertTrue(new ItemStack(JugcraftAgriculture.item("roasted_chestnuts")).is(Squirrel.FOOD), "Roasted chestnuts are squirrel food");
		helper.succeed();
	}
}
