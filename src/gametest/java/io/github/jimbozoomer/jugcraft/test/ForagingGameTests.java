package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.FairyRings;
import io.github.jimbozoomer.jugcraft.agriculture.ForagingBasketItem;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.MooncakeItem;
import io.github.jimbozoomer.jugcraft.agriculture.WildMushroomBlock;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * In-game tests for autumn foraging: wild mushrooms grow on soil, not stone, and spread in the shade up to a cap (bone meal
 * in any light); the jack o'lantern mushroom glows; fairy rings are counted, sprout from a mushroom, and bless the one at
 * their centre with Luck; the Foraging Basket takes only forage, fills when a mushroom is picked with it in hand, and
 * earns Forager full of every mushroom; and the cooking and worldgen data load.
 */
public class ForagingGameTests {
	private static final int[][] RING = {{3, 0}, {2, 2}, {0, 3}, {-2, 2}, {-3, 0}, {-2, -2}, {0, -3}, {2, -2}};

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static BlockState mushroom(String id) {
		return JugcraftAgriculture.block(id).defaultBlockState();
	}

	/** A grass floor from (0, 1, 0) to ({@code size}, 1, {@code size}), air above. */
	private static void meadow(GameTestHelper helper, int size) {
		for (int x = 0; x <= size; x++) {
			for (int z = 0; z <= size; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
				for (int y = 2; y <= 4; y++) {
					helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
				}
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

	private static int count(GameTestHelper helper, Block block) {
		int found = 0;
		for (BlockPos pos : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 2, 0)), helper.absolutePos(new BlockPos(8, 3, 8)))) {
			found += helper.getLevel().getBlockState(pos).is(block) ? 1 : 0;
		}
		return found;
	}

	/**
	 * A mushroom stands on grass, not stone. Spreading (as bone meal makes it, in any light) it puts out another of its
	 * kind, one at a time, until five are near, and no more. The jack o'lantern mushroom glows.
	 */
	@GameTest(maxTicks = 20)
	public void mushroomsGrowOnSoilAndSpread(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		meadow(helper, 8);
		BlockState porcini = mushroom("porcini");
		BlockPos at = new BlockPos(4, 2, 4);
		helper.assertTrue(porcini.canSurvive(level, helper.absolutePos(at)), "A porcini stands on grass");
		helper.setBlock(new BlockPos(0, 1, 0), Blocks.STONE);
		helper.assertTrue(!porcini.canSurvive(level, helper.absolutePos(new BlockPos(0, 2, 0))), "but not on stone");
		helper.setBlock(at, porcini);
		for (int i = 0; i < 40 && count(helper, porcini.getBlock()) < WildMushroomBlock.SPREAD_CAP; i++) {
			WildMushroomBlock.spread(level, helper.absolutePos(at), porcini, level.getRandom(), false);
		}
		helper.assertTrue(count(helper, porcini.getBlock()) == WildMushroomBlock.SPREAD_CAP, "It spreads to five");
		helper.assertTrue(!WildMushroomBlock.spread(level, helper.absolutePos(at), porcini, level.getRandom(), false), "and no further");
		helper.assertTrue(mushroom("jack_o_lantern_mushroom").getLightEmission() == 9 && porcini.getLightEmission() == 0,
				"The jack o'lantern mushroom glows");
		helper.succeed();
	}

	/**
	 * Eight fly agarics round a circle make a fairy ring (seven don't). At its centre a player is blessed: Luck II, Away with
	 * the Fairies, once a night. A mushroom sprouts a whole ring of its kind round a circle it stands on, and none where one
	 * stands already.
	 */
	@GameTest(maxTicks = 20)
	public void fairyRingsSproutAndBless(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		meadow(helper, 12);
		BlockPos centre = new BlockPos(6, 2, 6);
		for (int i = 0; i < RING.length - 1; i++) {
			helper.setBlock(centre.offset(RING[i][0], 0, RING[i][1]), mushroom("fly_agaric"));
		}
		helper.assertTrue(!FairyRings.ring(level, helper.absolutePos(centre)), "Seven mushrooms are no ring");
		helper.setBlock(centre.offset(RING[7][0], 0, RING[7][1]), mushroom("fly_agaric"));
		helper.assertTrue(FairyRings.ring(level, helper.absolutePos(centre)) && FairyRings.count(level, helper.absolutePos(centre)) == 8,
				"Eight are a fairy ring");
		ServerPlayer dancer = player(helper, centre);
		FairyRings.forget(dancer);
		FairyRings.bless(level, dancer, 0L);
		MobEffectInstance luck = dancer.getEffect(MobEffects.LUCK);
		helper.assertTrue(luck != null && luck.getAmplifier() == 1 && luck.getDuration() > FairyRings.LUCK_TICKS - 20, "Luck II for five minutes");
		helper.assertTrue(FairyRings.blessedTonight(dancer) && earned(dancer, "fairy_ring"), "once tonight, and Away with the Fairies");
		helper.assertTrue(MooncakeItem.fullMoonNight(14000) && !MooncakeItem.fullMoonNight(24000 + 14000) && !MooncakeItem.fullMoonNight(6000),
				"Rings bless on full-moon nights only");

		for (int x = 0; x <= 12; x++) {
			for (int z = 0; z <= 12; z++) {
				helper.setBlock(new BlockPos(x, 2, z), Blocks.AIR);
			}
		}
		BlockPos seed = new BlockPos(6, 2, 6);
		BlockState chanterelle = mushroom("chanterelle");
		helper.setBlock(seed, chanterelle);
		int planted = FairyRings.sprout(level, helper.absolutePos(seed), chanterelle, level.getRandom());
		boolean ring = false;
		for (int[] point : RING) {
			ring |= FairyRings.ring(level, helper.absolutePos(seed.offset(-point[0], 0, -point[1])));
		}
		helper.assertTrue(planted >= FairyRings.RING_MUSHROOMS - 1 && ring, "One chanterelle sprouts a ring round it: " + planted);
		helper.succeed();
	}

	/**
	 * The basket takes forage, not stone; picking a mushroom with it in the other hand puts it straight in; full of all five
	 * mushrooms it earns Forager.
	 */
	@GameTest(maxTicks = 20)
	public void theBasketTakesForage(GameTestHelper helper) {
		meadow(helper, 8);
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1));
		ItemStack basket = new ItemStack(item("foraging_basket"));
		ItemStack stone = new ItemStack(Items.STONE);
		ForagingBasketItem.fill(basket, stone, player);
		helper.assertTrue(stone.getCount() == 1, "Stone is no forage");
		ItemStack berries = new ItemStack(Items.SWEET_BERRIES, 3);
		ForagingBasketItem.fill(basket, berries, player);
		helper.assertTrue(berries.isEmpty(), "Berries go in");
		player.setItemInHand(InteractionHand.OFF_HAND, basket);
		List<String> mushrooms = JugcraftAgriculture.WILD_MUSHROOMS;
		for (int i = 0; i < mushrooms.size(); i++) {
			BlockPos pos = new BlockPos(2 + i, 2, 4);
			helper.setBlock(pos, mushroom(mushrooms.get(i)));
			player.gameMode.destroyBlock(helper.absolutePos(pos));
		}
		BundleContents contents = player.getOffhandItem().get(DataComponents.BUNDLE_CONTENTS);
		helper.assertTrue(contents != null && contents.itemCopies().filter(stack -> stack.is(JugcraftAgriculture.item("puffball"))).count() == 1,
				"A picked puffball goes straight into the basket");
		helper.assertTrue(ForagingBasketItem.holdsEveryMushroom(player.getOffhandItem()) && earned(player, "forager"),
				"All five picked: Forager");
		helper.succeed();
	}

	/** The cooked mushrooms, the stew, the basket and every mushroom's patch load. */
	@GameTest(maxTicks = 20)
	public void foragingDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String recipe : List.of("sauteed_chanterelles", "roasted_porcini", "fried_puffball", "foraging_basket", "pot_cooking/foragers_stew")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(recipe))).isPresent(), recipe + " loads");
		}
		for (String mushroom : JugcraftAgriculture.WILD_MUSHROOMS) {
			helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE)
					.get(ResourceKey.create(Registries.PLACED_FEATURE, Jugcraft.id("patch_" + mushroom))).isPresent(), mushroom + " has its patch");
		}
		helper.succeed();
	}
}
