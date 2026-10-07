package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CanningKettleBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JarContents;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PantryShelfBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.PreserveJarItem;
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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the preserves pantry: preserves cooked into Mason Jars in the Cooking Pot (stamped with when they were
 * cooked; pickling vinegar gives its bottle back), eating a jar a serving at a time down to the empty jar, spoiling (an
 * unsealed jar three days old; a sealed one never, until opened), the Canning Kettle (water first, full fresh jars only,
 * sealing in boiling water, not without heat, lifting out), the Pantry Shelf (six jars, comparators, spilling when broken),
 * sealed jars stacking, and the data.
 */
public class PantryGameTests {
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
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
		BlockPos absolute = helper.absolutePos(pos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	private static long now(GameTestHelper helper) {
		return helper.getLevel().getGameTime();
	}

	/** A fresh, full jar of {@code preserve} cooked at {@code made}. */
	private static ItemStack jar(String preserve, long made) {
		ItemStack jar = new ItemStack(item(preserve));
		PreserveJarItem.cooked(jar, made);
		return jar;
	}

	// ---------------------------------------------------------------- cooking

	/**
	 * Jam cooks into a Mason Jar in the Cooking Pot, stamped with the time it was cooked; pickles in cider vinegar leave the
	 * vinegar's bottle in a result slot, out of the way of the next batch.
	 */
	@GameTest(maxTicks = 600)
	public void preservesCookIntoJars(GameTestHelper helper) {
		BlockPos jamPot = new BlockPos(1, 2, 1);
		BlockPos picklePot = new BlockPos(4, 2, 1);
		long start = now(helper);
		for (BlockPos pos : List.of(jamPot, picklePot)) {
			helper.setBlock(pos.below(), Blocks.CAMPFIRE);
			helper.setBlock(pos, block("cooking_pot"));
		}
		CookingPotBlockEntity jam = helper.getBlockEntity(jamPot, CookingPotBlockEntity.class);
		jam.setItem(0, new ItemStack(item("mason_jar")));
		jam.setItem(1, new ItemStack(Items.SWEET_BERRIES, 6));
		jam.setItem(2, new ItemStack(Items.SUGAR, 2));
		CookingPotBlockEntity pickles = helper.getBlockEntity(picklePot, CookingPotBlockEntity.class);
		pickles.setItem(0, new ItemStack(item("mason_jar")));
		pickles.setItem(1, new ItemStack(Items.BEETROOT, 4));
		pickles.setItem(2, new ItemStack(item("cider_vinegar")));
		helper.succeedWhen(() -> {
			ItemStack made = jam.getItem(CookingPotBlockEntity.RESULT);
			helper.assertTrue(made.is(item("sweet_berry_jam")), "Expected a jar of sweet berry jam, found " + made);
			JarContents contents = made.get(JugcraftAgriculture.JAR_CONTENTS);
			helper.assertTrue(contents != null && contents.servings() == PreserveJarItem.SERVINGS && contents.made() >= start
					&& !PreserveJarItem.sealed(made), "The jar is full, unsealed, and stamped with when it was cooked");
			helper.assertTrue(pickles.getItem(CookingPotBlockEntity.RESULT).is(item("pickled_beets")), "Beets pickle in cider vinegar");
			helper.assertTrue(pickles.getItem(CookingPotBlockEntity.RESULT + 1).is(Items.GLASS_BOTTLE) && pickles.getItem(2).isEmpty(),
					"The vinegar's bottle goes to a result slot, not back among the ingredients");
		});
	}

	// ---------------------------------------------------------------- eating and spoiling

	/** A jar is eaten a serving at a time; the last serving leaves the empty Mason Jar. Glow berry jelly gives Night Vision. */
	@GameTest(maxTicks = 40)
	public void jarsAreEatenAServingAtATime(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(2, 2, 2), ItemStack.EMPTY);
		ItemStack jelly = jar("glow_berry_jelly", now(helper));
		for (int serving = PreserveJarItem.SERVINGS - 1; serving >= 0; serving--) {
			player.getFoodData().setFoodLevel(2);
			jelly = jelly.finishUsingItem(level, player);
			helper.assertTrue(player.getFoodData().getFoodLevel() == 4, "A serving of jelly is 2 food");
			if (serving > 0) {
				helper.assertTrue(PreserveJarItem.servings(jelly) == serving, "Servings left: " + serving + ", found " + PreserveJarItem.servings(jelly));
			}
		}
		helper.assertTrue(jelly.is(item("mason_jar")), "The last serving leaves the empty jar, found " + jelly);
		helper.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION), "Glow berry jelly gives Night Vision");
		helper.succeed();
	}

	/**
	 * An unsealed jar cooked more than three days ago has spoiled: a serving is 1 food with Hunger and Nausea. A sealed jar of
	 * the same age is fine; opening it starts its three days.
	 */
	@GameTest(maxTicks = 40)
	public void unsealedJarsSpoil(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(2, 2, 2), ItemStack.EMPTY);
		ItemStack old = jar("apple_butter", now(helper) - PreserveJarItem.SPOIL_TICKS - 20);
		helper.assertTrue(PreserveJarItem.spoiled(old, now(helper)), "An unsealed jar over three days old has spoiled");
		player.getFoodData().setFoodLevel(2);
		old.finishUsingItem(level, player);
		helper.assertTrue(player.getFoodData().getFoodLevel() == 3 && player.hasEffect(MobEffects.HUNGER) && player.hasEffect(MobEffects.NAUSEA),
				"A spoiled serving is 1 food and a bad stomach");
		ItemStack sealed = jar("apple_butter", now(helper) - PreserveJarItem.SPOIL_TICKS - 20);
		PreserveJarItem.seal(sealed);
		helper.assertTrue(!PreserveJarItem.spoiled(sealed, now(helper)), "A sealed jar keeps");
		player.getFoodData().setFoodLevel(2);
		ItemStack opened = sealed.finishUsingItem(level, player);
		JarContents contents = opened.get(JugcraftAgriculture.JAR_CONTENTS);
		helper.assertTrue(player.getFoodData().getFoodLevel() == 6 && !PreserveJarItem.sealed(opened) && contents != null
				&& contents.servings() == PreserveJarItem.SERVINGS - 1 && contents.made() == now(helper), "Opening a sealed jar starts its days");
		helper.succeed();
	}

	/** Sealed jars of the same preserve stack; unsealed jars cooked at different times don't. */
	@GameTest
	public void sealedJarsStack(GameTestHelper helper) {
		ItemStack a = jar("pumpkin_butter", 100);
		ItemStack b = jar("pumpkin_butter", 200);
		helper.assertTrue(!ItemStack.isSameItemSameComponents(a, b), "Unsealed jars from different batches don't stack");
		PreserveJarItem.seal(a);
		PreserveJarItem.seal(b);
		helper.assertTrue(ItemStack.isSameItemSameComponents(a, b), "Sealed jars stack");
		helper.succeed();
	}

	// ---------------------------------------------------------------- the canning kettle

	/**
	 * Jars need water in the kettle; only a full, fresh jar goes in; once the water boils, a jar seals after twenty seconds in
	 * it; an empty hand lifts out the sealed jars (comparators read them).
	 */
	@GameTest(maxTicks = 600)
	public void kettleSealsJarsInBoilingWater(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(2, 3, 2);
		helper.setBlock(pos.below(), Blocks.CAMPFIRE);
		helper.setBlock(pos, block("canning_kettle"));
		CanningKettleBlockEntity kettle = helper.getBlockEntity(pos, CanningKettleBlockEntity.class);
		ServerPlayer player = player(helper, new BlockPos(4, 2, 2), jar("corn_relish", now(helper)));
		use(helper, player, pos);
		helper.assertTrue(kettle.jars().isEmpty() && !player.getMainHandItem().isEmpty(), "No jars without water");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
		use(helper, player, pos);
		helper.assertTrue(kettle.water() && player.getMainHandItem().is(Items.BUCKET), "A water bucket fills it");
		ItemStack opened = jar("corn_relish", now(helper));
		opened.set(JugcraftAgriculture.JAR_CONTENTS, new JarContents(2, now(helper)));
		player.setItemInHand(InteractionHand.MAIN_HAND, opened);
		use(helper, player, pos);
		helper.assertTrue(kettle.jars().isEmpty(), "An opened jar can't be sealed");
		player.setItemInHand(InteractionHand.MAIN_HAND, jar("corn_relish", now(helper)));
		use(helper, player, pos);
		player.setItemInHand(InteractionHand.MAIN_HAND, jar("pickled_peppers", now(helper)));
		use(helper, player, pos);
		helper.assertTrue(kettle.jars().size() == 2, "Two fresh jars go in");
		kettle.boil();
		helper.runAfterDelay(CanningKettleBlockEntity.PROCESS_TICKS + 10, () -> {
			helper.assertTrue(kettle.sealedCount() == 2, "After twenty seconds at the boil both jars have sealed");
			BlockPos absolute = helper.absolutePos(pos);
			helper.assertTrue(helper.getLevel().getBlockState(absolute).getAnalogOutputSignal(helper.getLevel(), absolute, Direction.NORTH) > 0,
					"Comparators read the sealed jars");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			use(helper, player, pos);
			int sealed = 0;
			for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
				ItemStack stack = player.getInventory().getItem(slot);
				sealed += stack.getItem() instanceof PreserveJarItem && PreserveJarItem.sealed(stack) ? 1 : 0;
			}
			helper.assertTrue(sealed == 2 && kettle.jars().isEmpty(), "An empty hand lifts out both sealed jars");
			helper.succeed();
		});
	}

	/** Without heat the water cools off the boil and nothing seals; sneaking lifts the jars out unsealed. */
	@GameTest(maxTicks = 200)
	public void kettleNeedsHeat(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(2, 2, 2);
		helper.setBlock(pos, block("canning_kettle"));
		CanningKettleBlockEntity kettle = helper.getBlockEntity(pos, CanningKettleBlockEntity.class);
		kettle.fill();
		kettle.add(jar("sweet_berry_jam", now(helper)));
		kettle.boil();
		helper.runAfterDelay(100, () -> {
			helper.assertTrue(!kettle.boiling() && kettle.sealedCount() == 0, "Off the heat it stops boiling and nothing seals");
			ServerPlayer player = player(helper, new BlockPos(4, 2, 2), ItemStack.EMPTY);
			player.setShiftKeyDown(true);
			use(helper, player, pos);
			helper.assertTrue(kettle.jars().isEmpty() && player.getInventory().countItem(item("sweet_berry_jam")) == 1,
					"Sneaking lifts the jar out, unsealed");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- the pantry shelf

	/** A shelf holds six jars (a seventh is refused), comparators read 15, an empty hand takes the last; broken, it spills them. */
	@GameTest(maxTicks = 40)
	public void pantryShelfHoldsSixJars(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos pos = new BlockPos(2, 2, 2);
		helper.setBlock(pos, block("pantry_shelf"));
		PantryShelfBlockEntity shelf = helper.getBlockEntity(pos, PantryShelfBlockEntity.class);
		ServerPlayer player = player(helper, new BlockPos(2, 2, 4), new ItemStack(item("mason_jar"), 7));
		for (int i = 0; i < 7; i++) {
			use(helper, player, pos);
		}
		helper.assertTrue(shelf.count() == PantryShelfBlockEntity.SLOTS && player.getMainHandItem().getCount() == 1, "Six go up, not a seventh");
		BlockPos absolute = helper.absolutePos(pos);
		helper.assertTrue(level.getBlockState(absolute).getAnalogOutputSignal(level, absolute, Direction.NORTH) == 15, "A full shelf reads 15");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
		use(helper, player, pos);
		helper.assertTrue(shelf.count() == PantryShelfBlockEntity.SLOTS, "A stick doesn't go up");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, pos);
		helper.assertTrue(shelf.count() == PantryShelfBlockEntity.SLOTS - 1, "An empty hand takes one down");
		level.destroyBlock(absolute, true);
		int jars = level.getEntitiesOfClass(ItemEntity.class, new AABB(absolute).inflate(2.0), e -> e.getItem().is(item("mason_jar"))).stream()
				.mapToInt(e -> e.getItem().getCount()).sum();
		helper.assertTrue(jars == PantryShelfBlockEntity.SLOTS - 1, "Broken, it spills its jars, found " + jars);
		helper.succeed();
	}

	// ---------------------------------------------------------------- data

	/** The recipes (every preserve and the vinegar in the Cooking Pot) and the loot tables load. */
	@GameTest
	public void pantryDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<String> recipes = List.of("mason_jar", "pantry_shelf", "canning_kettle", "pot_cooking/cider_vinegar", "pot_cooking/sweet_berry_jam",
				"pot_cooking/apple_butter", "pot_cooking/pumpkin_butter", "pot_cooking/cranberry_preserves", "pot_cooking/glow_berry_jelly",
				"pot_cooking/pickled_beets", "pot_cooking/pickled_peppers", "pot_cooking/corn_relish");
		for (String id : recipes) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), "Recipe " + id + " loads");
		}
		for (String table : List.of("blocks/canning_kettle", "blocks/pantry_shelf")) {
			helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id(table)))
					!= LootTable.EMPTY, "Loot table " + table + " loads");
		}
		helper.succeed();
	}
}
