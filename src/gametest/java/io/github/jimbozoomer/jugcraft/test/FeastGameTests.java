package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.FeastTableBlock;
import io.github.jimbozoomer.jugcraft.agriculture.FeastTableBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.Feasts;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import java.util.ArrayList;
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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
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
 * In-game tests for the harvest feast: lengths joining into one table (legs only at its ends), serving dishes (eight
 * servings, one food a dish, food only), eating a serving (the food, the bowl back from a stew, the feast's tier), a
 * grand feast of five diners at a varied table (every diner blessed, Harvest Home), taking a dish back, spilling dishes
 * when broken, and data.
 */
public class FeastGameTests {
	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	/** A table along x from (x0, 2, z) for {@code lengths} blocks, each length shaped by its neighbours. */
	private static void table(GameTestHelper helper, int x0, int z, int lengths) {
		BlockState state = JugcraftAgriculture.block("feast_table").defaultBlockState().setValue(FeastTableBlock.AXIS, Direction.Axis.X);
		for (int i = 0; i < lengths; i++) {
			helper.setBlock(new BlockPos(x0 + i, 2, z), state);
		}
		ServerLevel level = helper.getLevel();
		for (int i = 0; i < lengths; i++) {
			BlockPos at = helper.absolutePos(new BlockPos(x0 + i, 2, z));
			level.setBlock(at, Block.updateFromNeighbourShapes(level.getBlockState(at), level, at), Block.UPDATE_ALL);
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

	/** {@code player} uses the table length at {@code pos}, pointing at dish {@code dish}. */
	private static void use(GameTestHelper helper, ServerPlayer player, BlockPos pos, int dish) {
		BlockPos absolute = helper.absolutePos(pos);
		Vec3 at = new Vec3(absolute.getX() + (dish == 0 ? 0.25 : 0.75), absolute.getY() + 13.0 / 16.0, absolute.getZ() + 0.5);
		player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
				new BlockHitResult(at, Direction.UP, absolute, false));
	}

	private static FeastTableBlockEntity length(GameTestHelper helper, BlockPos pos) {
		return helper.getBlockEntity(pos, FeastTableBlockEntity.class);
	}

	/** Lengths end to end along one axis join into a table with legs at its ends; a length across them stands alone. */
	@GameTest(maxTicks = 20)
	public void lengthsJoinIntoATable(GameTestHelper helper) {
		floor(helper);
		table(helper, 1, 3, 3);
		helper.setBlock(new BlockPos(4, 2, 4), JugcraftAgriculture.block("feast_table").defaultBlockState().setValue(FeastTableBlock.AXIS, Direction.Axis.Z));
		helper.assertTrue(helper.getBlockState(new BlockPos(1, 2, 3)).getValue(FeastTableBlock.PART) == FeastTableBlock.Part.START
				&& helper.getBlockState(new BlockPos(2, 2, 3)).getValue(FeastTableBlock.PART) == FeastTableBlock.Part.MIDDLE
				&& helper.getBlockState(new BlockPos(3, 2, 3)).getValue(FeastTableBlock.PART) == FeastTableBlock.Part.END, "Start, middle and end");
		helper.assertTrue(helper.getBlockState(new BlockPos(4, 2, 4)).getValue(FeastTableBlock.PART) == FeastTableBlock.Part.SINGLE,
				"A length across the table stands alone");
		helper.assertTrue(FeastTableBlockEntity.table(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 3))).size() == 3
				&& FeastTableBlockEntity.table(helper.getLevel(), helper.absolutePos(new BlockPos(4, 2, 4))).size() == 1, "The table is three long");
		helper.succeed();
	}

	/**
	 * Food is served eight to a dish, one food a dish; a stick isn't served. Eating a serving feeds the eater and, at a
	 * table of two dishes with one diner, makes a good meal (Regeneration, not yet Absorption). A stew leaves its bowl.
	 * Sneaking takes a dish back; broken, the table spills its dishes.
	 */
	@GameTest(maxTicks = 40)
	public void servingAndEating(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		table(helper, 3, 3, 1);
		FeastTableBlockEntity table = length(helper, pos);
		ServerPlayer host = player(helper, new BlockPos(3, 2, 1), new ItemStack(Items.BREAD, 10));
		use(helper, host, pos, 0);
		helper.assertTrue(table.dish(0).is(Items.BREAD) && table.dish(0).getCount() == FeastTableBlockEntity.SERVINGS && host.getMainHandItem().getCount() == 2,
				"Eight loaves go on the first dish");
		host.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CARROT, 3));
		use(helper, host, pos, 0);
		helper.assertTrue(table.dish(0).is(Items.BREAD) && host.getMainHandItem().getCount() == 3, "Carrots don't go on the bread");
		use(helper, host, pos, 1);
		helper.assertTrue(table.dish(1).is(Items.CARROT) && table.dish(1).getCount() == 3, "but on the other dish");
		host.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
		use(helper, host, pos, 1);
		helper.assertTrue(table.dish(1).getCount() == 3 && host.getMainHandItem().is(Items.STICK), "A stick isn't served");

		ServerPlayer diner = player(helper, new BlockPos(3, 2, 5), ItemStack.EMPTY);
		diner.getFoodData().setFoodLevel(5);
		use(helper, diner, pos, 0);
		helper.assertTrue(table.dish(0).getCount() == FeastTableBlockEntity.SERVINGS - 1 && diner.getFoodData().getFoodLevel() == 10,
				"A serving of bread feeds the diner: " + diner.getFoodData().getFoodLevel());
		helper.assertTrue(diner.hasEffect(MobEffects.REGENERATION) && !diner.hasEffect(MobEffects.ABSORPTION),
				"Two dishes and one diner make a good meal");

		host.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		host.setShiftKeyDown(true);
		use(helper, host, pos, 1);
		host.setShiftKeyDown(false);
		helper.assertTrue(table.dish(1).isEmpty() && host.getInventory().countItem(Items.CARROT) == 3, "Sneaking takes the carrots back");
		host.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MUSHROOM_STEW));
		use(helper, host, pos, 1);
		use(helper, diner, pos, 1);
		helper.assertTrue(table.dish(1).isEmpty() && diner.getInventory().countItem(Items.BOWL) == 1, "A stew leaves its bowl");

		helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
		helper.succeedWhen(() -> {
			int bread = 0;
			for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(3.0))) {
				bread += item.getItem().is(Items.BREAD) ? item.getItem().getCount() : 0;
			}
			helper.assertTrue(bread == FeastTableBlockEntity.SERVINGS - 1, "Broken, it spills the bread: " + bread);
		});
	}

	/**
	 * At a table of six foods, the first diner has a feast (Absorption), the second makes it a harvest feast for both
	 * (Haste and Luck), and the fifth a grand feast for all five (Health Boost and Harvest Home).
	 */
	@GameTest(maxTicks = 20)
	public void aGrandFeast(GameTestHelper helper) {
		floor(helper);
		table(helper, 1, 3, 3);
		ItemStack[] foods = {new ItemStack(Items.BREAD, 8), new ItemStack(Items.CARROT, 8), new ItemStack(Items.BAKED_POTATO, 8),
				new ItemStack(Items.COOKED_BEEF, 8), new ItemStack(Items.APPLE, 8), new ItemStack(Items.PUMPKIN_PIE, 8)};
		for (int i = 0; i < foods.length; i++) {
			length(helper, new BlockPos(1 + i / 2, 2, 3)).serve(i % 2, foods[i]);
		}
		helper.assertTrue(FeastTableBlockEntity.variety(FeastTableBlockEntity.table(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 3)))) == 6,
				"Six foods on the table");
		List<ServerPlayer> diners = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			diners.add(player(helper, new BlockPos(1 + i % 3, 2, i < 3 ? 2 : 4), ItemStack.EMPTY));
		}
		use(helper, diners.get(0), new BlockPos(1, 2, 3), 0);
		helper.assertTrue(diners.get(0).hasEffect(MobEffects.ABSORPTION) && !diners.get(0).hasEffect(MobEffects.HASTE),
				"Six foods and one diner: a feast");
		use(helper, diners.get(1), new BlockPos(2, 2, 3), 1);
		helper.assertTrue(diners.get(0).hasEffect(MobEffects.HASTE) && diners.get(0).hasEffect(MobEffects.LUCK) && diners.get(1).hasEffect(MobEffects.HASTE),
				"A second diner makes it a harvest feast, for both");
		helper.assertFalse(diners.get(0).hasEffect(MobEffects.HEALTH_BOOST), "not yet a grand feast");
		for (int i = 2; i < 5; i++) {
			use(helper, diners.get(i), new BlockPos(1 + i % 3, 2, 3), i % 2);
		}
		AdvancementHolder home = helper.getLevel().getServer().getAdvancements().get(Jugcraft.id("harvest_home"));
		for (ServerPlayer diner : diners) {
			helper.assertTrue(diner.hasEffect(MobEffects.HEALTH_BOOST) && home != null && diner.getAdvancements().getOrStartProgress(home).isDone(),
					"Five diners at six foods: a grand feast for every one of them");
		}
		helper.assertTrue(Feasts.tier(Feasts.GRAND_FEAST) == 4 && Feasts.tier(Feasts.GOOD_MEAL - 1) == 0, "The tiers");
		helper.succeed();
	}

	/** The table's recipe and loot table load. */
	@GameTest(maxTicks = 20)
	public void feastData(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id("feast_table"))).isPresent(), "The recipe loads");
		helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("blocks/feast_table")))
				!= LootTable.EMPTY, "The loot table loads");
		helper.succeed();
	}
}
