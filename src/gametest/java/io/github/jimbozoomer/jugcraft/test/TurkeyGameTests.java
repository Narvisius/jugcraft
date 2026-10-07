package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.RoastTurkeyBlock;
import io.github.jimbozoomer.jugcraft.agriculture.Turkey;
import io.github.jimbozoomer.jugcraft.agriculture.Turkeys;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for wild turkeys: two fed on seeds breed a poult (Gobble Gobble); a tom struts for an audience (a player
 * near), stopping in time, and a hen never struts; a hen lays an egg when her time comes and a tom never does; a flock
 * comes with a tom first and only to grass in turkey country; a roast turkey is eaten a serving at a time, carved into
 * slices (Carving the Bird), drops itself only whole, and leaves a bone with its last serving; and the data loads.
 */
public class TurkeyGameTests {
	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static Item vanilla(String id) {
		return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(id));
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

	private static Turkey turkey(GameTestHelper helper, BlockPos pos, boolean tom) {
		Turkey turkey = helper.spawn(JugcraftAgriculture.TURKEY, pos);
		turkey.setTom(tom);
		turkey.setPersistenceRequired();
		return turkey;
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK.defaultBlockState());
			}
		}
	}

	/** A tom and a hen, each fed wheat seeds by the same player, breed a poult; the player earns Gobble Gobble. */
	@GameTest(maxTicks = 300)
	public void turkeysBreedOnSeeds(GameTestHelper helper) {
		floor(helper);
		Turkey tom = turkey(helper, new BlockPos(3, 2, 3), true);
		Turkey hen = turkey(helper, new BlockPos(4, 2, 3), false);
		ServerPlayer farmer = player(helper, new BlockPos(3, 2, 5));
		for (Turkey turkey : List.of(tom, hen)) {
			farmer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(vanilla("wheat_seeds"), 4));
			farmer.interactOn(turkey, InteractionHand.MAIN_HAND, turkey.position());
			helper.assertTrue(turkey.isInLove(), "Fed seeds, a turkey is in love");
		}
		helper.succeedWhen(() -> {
			List<Turkey> poults = helper.getLevel().getEntitiesOfClass(Turkey.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(10),
					Turkey::isBaby);
			helper.assertTrue(!poults.isEmpty(), "A poult hatches");
			helper.assertTrue(earned(farmer, "gobble_gobble"), "Breeding them earns Gobble Gobble");
		});
	}

	/**
	 * A tom struts for a player near him (a hen won't), for {@value Turkey#STRUT_TICKS} ticks.
	 */
	@GameTest(maxTicks = 140)
	public void aTomStrutsForAnAudience(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		Turkey tom = turkey(helper, new BlockPos(3, 2, 3), true);
		Turkey hen = turkey(helper, new BlockPos(6, 2, 6), false);
		hen.setTom(false);
		helper.assertTrue(!hen.strut() && !hen.strutting(), "A hen doesn't strut");
		hen.discard();
		ServerPlayer watcher = player(helper, new BlockPos(3, 2, 6));
		helper.assertTrue(tom.audience(level), "A player three blocks off is an audience");
		helper.assertTrue(tom.strut() && tom.strutting(), "He struts");
		BlockPos away = helper.absolutePos(new BlockPos(3, 2, 3 + Turkey.STRUT_RANGE + 40));
		watcher.setPos(away.getX() + 0.5, away.getY(), away.getZ() + 0.5);
		helper.runAfterDelay(Turkey.STRUT_TICKS + 5, () -> {
			helper.assertTrue(!tom.strutting(), "The strut ends after " + Turkey.STRUT_TICKS + " ticks");
			helper.succeed();
		});
	}

	/** When her time comes a hen lays an egg; a tom never does. */
	@GameTest(maxTicks = 40)
	public void hensLayEggs(GameTestHelper helper) {
		floor(helper);
		Turkey hen = turkey(helper, new BlockPos(2, 2, 2), false);
		Turkey tom = turkey(helper, new BlockPos(6, 2, 6), true);
		hen.setEggTime(2);
		tom.setEggTime(2);
		helper.runAfterDelay(10, () -> {
			List<ItemEntity> nearHen = helper.getLevel().getEntitiesOfClass(ItemEntity.class, hen.getBoundingBox().inflate(2),
					drop -> drop.getItem().is(vanilla("egg")));
			List<ItemEntity> nearTom = helper.getLevel().getEntitiesOfClass(ItemEntity.class, tom.getBoundingBox().inflate(2),
					drop -> drop.getItem().is(vanilla("egg")));
			helper.assertTrue(nearHen.size() == 1 && nearTom.isEmpty() && hen.eggTime() >= Turkey.EGG_MIN,
					"The hen laid an egg and started on the next; the tom laid none");
			helper.succeed();
		});
	}

	/** A flock is three to five turkeys, a tom first; turkeys come only to grass. */
	@GameTest
	public void aFlockComes(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		BlockPos spot = helper.absolutePos(new BlockPos(4, 2, 4));
		int count = Turkeys.spawnFlock(level, spot, level.getRandom());
		List<Turkey> flock = level.getEntitiesOfClass(Turkey.class, new AABB(spot).inflate(4));
		helper.assertTrue(count >= Turkeys.FLOCK_MIN && count <= Turkeys.FLOCK_MAX && flock.size() >= count
				&& flock.stream().anyMatch(Turkey::isTom), "A flock of " + count + " with a tom");
		helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE.defaultBlockState());
		BlockPos stone = helper.absolutePos(new BlockPos(1, 1, 1));
		helper.assertTrue(Turkeys.ground(level, stone.getX(), stone.getZ()) == null, "Turkeys don't come to bare stone");
		for (Turkey turkey : flock) {
			turkey.discard();
		}
		helper.succeed();
	}

	/**
	 * A hungry player eats a serving of roast turkey; a Carving Knife carves a slice (Carving the Bird); a whole turkey drops
	 * itself and a carved one nothing; the last serving takes the platter and leaves a bone.
	 */
	@GameTest(maxTicks = 20)
	public void aRoastTurkeyIsCarvedAndEaten(GameTestHelper helper) {
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos.below(), Blocks.OAK_PLANKS.defaultBlockState());
		helper.setBlock(pos, JugcraftAgriculture.block("roast_turkey").defaultBlockState().setValue(RoastTurkeyBlock.FACING, Direction.NORTH));
		ServerLevel level = helper.getLevel();
		BlockPos absolute = helper.absolutePos(pos);
		helper.assertTrue(Block.getDrops(helper.getBlockState(pos), level, absolute, null).stream().anyMatch(drop -> drop.is(item("roast_turkey"))),
				"A whole roast turkey drops itself");
		ServerPlayer diner = player(helper, new BlockPos(3, 2, 1));
		diner.getFoodData().setFoodLevel(10);
		use(helper, diner, pos, ItemStack.EMPTY);
		helper.assertTrue(helper.getBlockState(pos).getValue(RoastTurkeyBlock.BITES) == 1
				&& diner.getFoodData().getFoodLevel() == 10 + RoastTurkeyBlock.NUTRITION, "A serving eaten");
		helper.assertTrue(Block.getDrops(helper.getBlockState(pos), level, absolute, null).isEmpty(), "A carved turkey drops nothing");
		use(helper, diner, pos, new ItemStack(item("carving_knife")));
		helper.assertTrue(helper.getBlockState(pos).getValue(RoastTurkeyBlock.BITES) == 2 && diner.getInventory().countItem(item("turkey_slice")) == 1
				&& earned(diner, "carving_the_bird"), "A slice carved off: Carving the Bird");
		helper.setBlock(pos, helper.getBlockState(pos).setValue(RoastTurkeyBlock.BITES, RoastTurkeyBlock.SERVINGS - 1));
		diner.getFoodData().setFoodLevel(10);
		use(helper, diner, pos, ItemStack.EMPTY);
		List<ItemEntity> bones = level.getEntitiesOfClass(ItemEntity.class, new AABB(absolute).inflate(1), drop -> drop.getItem().is(vanilla("bone")));
		helper.assertTrue(helper.getBlockState(pos).isAir() && bones.size() == 1, "The last serving leaves a bone");
		helper.succeed();
	}

	/** {@code player} uses what they hold (or an empty hand) on the block at {@code pos}. */
	private static void use(GameTestHelper helper, ServerPlayer player, BlockPos pos, ItemStack held) {
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		BlockPos absolute = helper.absolutePos(pos);
		player.gameMode.useItemOn(player, helper.getLevel(), held, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(absolute), Direction.NORTH, absolute, false));
	}

	/** Seeds tempt turkeys; plains and forests are turkey country, deserts aren't; the cooking, loot and advancements load. */
	@GameTest
	public void turkeyDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(new ItemStack(vanilla("wheat_seeds")).is(Turkey.FOOD) && new ItemStack(item("corn_kernels")).is(Turkey.FOOD)
				&& !new ItemStack(vanilla("wheat")).is(Turkey.FOOD), "Seeds and corn kernels tempt turkeys, wheat doesn't");
		var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
		helper.assertTrue(biomes.getOrThrow(Biomes.PLAINS).is(Turkeys.HABITAT) && biomes.getOrThrow(Biomes.FOREST).is(Turkeys.HABITAT)
				&& !biomes.getOrThrow(Biomes.DESERT).is(Turkeys.HABITAT), "Plains and forests are turkey country; deserts aren't");
		for (String recipe : List.of("roast_turkey", "roast_turkey_from_smoking", "roast_turkey_from_campfire_cooking")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(recipe))).isPresent(), recipe + " loads");
		}
		for (String table : List.of("entities/turkey", "blocks/roast_turkey")) {
			helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id(table)))
					!= LootTable.EMPTY, table + " loads");
		}
		for (String id : List.of("gobble_gobble", "carving_the_bird")) {
			helper.assertTrue(level.getServer().getAdvancements().get(Jugcraft.id(id)) != null, id + " loads");
		}
		helper.succeed();
	}
}
