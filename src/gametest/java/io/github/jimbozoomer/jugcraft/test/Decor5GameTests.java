package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.AutumnWreathBlock;
import io.github.jimbozoomer.jugcraft.agriculture.BobbingTubBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HayBaleSeatBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.LeafPileBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCrateBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCrateBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.Seat;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the harvest party: the Bobbing for Apples Tub (apples in, ducking for them, the wait between
 * tries, apples spilled when broken), the Pumpkin Crate (produce only, four at most, taken back last first, saved,
 * spilled), the Hay Bale Seat (one sitter, getting up, the seat going with its bale), the Autumn Wreath (walls and
 * doors, mums swap its flowers, it keeps them when broken), the Leaf Piles (heaping, dropping one a layer) and the
 * softened falls onto bales and piles, and that their data loads.
 */
public class Decor5GameTests {
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

	private static List<ItemEntity> drops(GameTestHelper helper, Item item) {
		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 8, 8).inflate(2.0);
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item));
	}

	private static int dropped(GameTestHelper helper, Item item) {
		return drops(helper, item).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	private static int apples(GameTestHelper helper, BlockPos pos) {
		return helper.getBlockState(pos).getValue(BobbingTubBlock.APPLES);
	}

	private static int count(ServerPlayer player, Item item) {
		int count = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static List<Seat> seats(GameTestHelper helper) {
		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 8, 8).inflate(2.0);
		return helper.getLevel().getEntitiesOfClass(Seat.class, area);
	}

	@SuppressWarnings("unchecked")
	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> vanilla(String id) {
		return (EntityType<T>) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(id));
	}

	// ---------------------------------------------------------------- the bobbing tub

	/**
	 * Bobbing in an empty tub finds nothing; four apples go in and a fifth doesn't; ducking either catches an apple or
	 * misses, the tub splashes and refuses another try until it settles, and no apple is ever made or lost; in time
	 * every apple is caught. Broken, a tub drops itself and the apples still in it.
	 */
	@GameTest(maxTicks = 100)
	public void theTubBobsForApples(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, block("bobbing_tub"));
		ServerPlayer player = player(helper, new BlockPos(3, 2, 5), ItemStack.EMPTY);
		helper.assertTrue(use(helper, player, pos, Direction.UP).consumesAction() && apples(helper, pos) == 0
				&& !helper.getBlockState(pos).getValue(BobbingTubBlock.SPLASHING), "An empty tub has nothing to bob for");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.APPLE, BobbingTubBlock.MAX_APPLES + 1));
		for (int i = 0; i <= BobbingTubBlock.MAX_APPLES; i++) {
			use(helper, player, pos, Direction.UP);
		}
		helper.assertTrue(apples(helper, pos) == BobbingTubBlock.MAX_APPLES && player.getMainHandItem().getCount() == 1,
				"Four apples go in and the fifth stays in hand");
		player.getInventory().clearContent();

		int tries = 0;
		int caught = 0;
		while (apples(helper, pos) > 0 && tries < 80) {
			tries++;
			use(helper, player, pos, Direction.UP);
			helper.assertTrue(helper.getBlockState(pos).getValue(BobbingTubBlock.SPLASHING), "Ducking splashes the water");
			// A caught apple lands in the empty hand; put it away, so the next try is with an empty hand again.
			caught += count(player, Items.APPLE);
			player.getInventory().clearContent();
			int before = apples(helper, pos);
			helper.assertFalse(use(helper, player, pos, Direction.UP).consumesAction(), "While it splashes, nobody may try again");
			helper.assertTrue(apples(helper, pos) == before && count(player, Items.APPLE) == 0, "and the second try does nothing");
			helper.assertTrue(apples(helper, pos) + caught == BobbingTubBlock.MAX_APPLES, "Every apple is either afloat or caught");
			helper.setBlock(pos, helper.getBlockState(pos).setValue(BobbingTubBlock.SPLASHING, false));
		}
		helper.assertTrue(apples(helper, pos) == 0 && caught == BobbingTubBlock.MAX_APPLES, "In time every apple is caught (" + tries + " tries)");

		BlockPos full = new BlockPos(6, 2, 6);
		helper.setBlock(full, block("bobbing_tub").defaultBlockState().setValue(BobbingTubBlock.APPLES, 2));
		helper.getLevel().destroyBlock(helper.absolutePos(full), true);
		helper.assertTrue(dropped(helper, item("bobbing_tub")) == 1 && dropped(helper, Items.APPLE) == 2,
				"A broken tub drops itself and its two apples");

		BlockPos splash = new BlockPos(1, 2, 1);
		helper.setBlock(splash, block("bobbing_tub").defaultBlockState().setValue(BobbingTubBlock.APPLES, 1));
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, splash, Direction.UP);
		helper.runAfterDelay(BobbingTubBlock.SPLASH_TICKS + 5, () -> {
			helper.assertFalse(helper.getBlockState(splash).getValue(BobbingTubBlock.SPLASHING), "The water settles after its wait");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- the pumpkin crate

	/**
	 * The crate takes only produce, one at a time and four at most; an empty hand takes the last one back; what it
	 * holds is saved; broken, it spills what it holds and drops itself.
	 */
	@GameTest
	public void theCrateHoldsFourPumpkins(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, block("pumpkin_crate"));
		PumpkinCrateBlockEntity crate = helper.getBlockEntity(pos, PumpkinCrateBlockEntity.class);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 5), new ItemStack(Items.STICK));
		use(helper, player, pos, Direction.UP);
		helper.assertTrue(crate.isEmpty() && player.getMainHandItem().getCount() == 1, "A stick is no produce");

		List<Item> produce = List.of(Items.PUMPKIN, item("white_pumpkin"), item("butternut_squash"), item("warty_gourd"));
		for (Item piece : produce) {
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(piece, 2));
			use(helper, player, pos, Direction.UP);
			helper.assertTrue(player.getMainHandItem().getCount() == 1, "One " + piece + " goes in at a time");
		}
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MELON));
		use(helper, player, pos, Direction.UP);
		helper.assertTrue(crate.produce().size() == PumpkinCrateBlockEntity.CAPACITY && player.getMainHandItem().getCount() == 1,
				"Four fill it; a fifth stays in hand");

		player.getInventory().clearContent();
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, pos, Direction.UP);
		helper.assertTrue(crate.produce().size() == 3 && count(player, item("warty_gourd")) == 1, "An empty hand takes the last one out");

		CompoundTag saved = crate.saveWithoutMetadata(level.registryAccess());
		PumpkinCrateBlockEntity copy = new PumpkinCrateBlockEntity(crate.getBlockPos(), crate.getBlockState());
		copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
		helper.assertTrue(copy.produce().size() == 3 && copy.produce().get(1).is(item("white_pumpkin")), "What it holds is saved");

		level.destroyBlock(helper.absolutePos(pos), true);
		helper.assertTrue(dropped(helper, item("pumpkin_crate")) == 1 && dropped(helper, Items.PUMPKIN) == 1
				&& dropped(helper, item("white_pumpkin")) == 1 && dropped(helper, item("butternut_squash")) == 1,
				"Broken, it spills its three and drops itself");
		helper.succeed();
	}

	// ---------------------------------------------------------------- the hay bale seat

	/**
	 * Using a bale sits the player on it at its top; nobody else may sit there, nor may a sneaking player; getting up
	 * leaves no seat behind; breaking the bale stands its sitter up.
	 */
	@GameTest(maxTicks = 60)
	public void aHayBaleSeatsOne(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, block("hay_bale_seat"));
		ServerPlayer first = player(helper, new BlockPos(3, 2, 5), ItemStack.EMPTY);
		ServerPlayer second = player(helper, new BlockPos(4, 2, 5), ItemStack.EMPTY);
		helper.assertTrue(use(helper, first, pos, Direction.UP).consumesAction() && first.getVehicle() instanceof Seat,
				"Using the bale sits the player down");
		double seat = first.getVehicle().getY() - helper.absolutePos(pos).getY();
		helper.assertTrue(Math.abs(seat - HayBaleSeatBlock.HEIGHT) < 0.01, "on its top (" + seat + ")");
		helper.assertFalse(use(helper, second, pos, Direction.UP).consumesAction() || second.isPassenger(), "One sitter a bale");

		BlockPos other = new BlockPos(5, 2, 3);
		helper.setBlock(other, block("hay_bale_seat"));
		second.setShiftKeyDown(true);
		use(helper, second, other, Direction.UP);
		helper.assertFalse(second.isPassenger(), "A sneaking player doesn't sit");
		second.setShiftKeyDown(false);

		first.stopRiding();
		helper.runAfterDelay(3, () -> {
			helper.assertTrue(seats(helper).isEmpty(), "Getting up leaves no seat behind");
			helper.assertTrue(use(helper, second, other, Direction.UP).consumesAction() && second.isPassenger(), "The other bale seats the other player");
			helper.getLevel().destroyBlock(helper.absolutePos(other), true);
			helper.runAfterDelay(3, () -> {
				helper.assertTrue(seats(helper).isEmpty() && !second.isPassenger(), "Breaking the bale stands its sitter up");
				helper.succeed();
			});
		});
	}

	/**
	 * The same ten-block fall hurts a pig less onto a hay bale or a full leaf pile than onto stone, and a single layer
	 * of leaves softens it less than a full pile.
	 */
	@GameTest
	public void balesAndLeafPilesSoftenFalls(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos stone = new BlockPos(1, 1, 1);
		BlockPos bale = new BlockPos(3, 2, 1);
		BlockPos full = new BlockPos(5, 2, 1);
		BlockPos thin = new BlockPos(1, 2, 4);
		helper.setBlock(bale, block("hay_bale_seat"));
		helper.setBlock(full, block("red_leaf_pile").defaultBlockState().setValue(LeafPileBlock.LAYERS, LeafPileBlock.MAX_LAYERS));
		helper.setBlock(thin, block("red_leaf_pile"));
		EntityType<Mob> pigType = vanilla("pig");
		float[] hurt = new float[4];
		BlockPos[] onto = {stone, bale, full, thin};
		for (int i = 0; i < onto.length; i++) {
			Mob pig = helper.spawnWithNoFreeWill(pigType, onto[i].above());
			float before = pig.getHealth();
			BlockPos at = helper.absolutePos(onto[i]);
			BlockState state = level.getBlockState(at);
			state.getBlock().fallOn(level, state, at, pig, 10.0);
			hurt[i] = before - pig.getHealth();
		}
		helper.assertTrue(hurt[0] > 0, "Ten blocks onto stone hurts: " + hurt[0]);
		helper.assertTrue(hurt[1] < hurt[0], "The bale softens it: " + hurt[1] + " against " + hurt[0]);
		helper.assertTrue(hurt[2] < hurt[0] && hurt[2] < hurt[3], "A full leaf pile softens it most: " + hurt[2]);
		helper.assertTrue(hurt[3] < hurt[0], "A single layer of leaves softens it a little: " + hurt[3]);
		helper.succeed();
	}

	// ---------------------------------------------------------------- the autumn wreath

	/**
	 * A wreath won't stand on a floor, hangs on a wall or a door facing out with orange mums, takes another colour's
	 * mum (used) but not the same again, and falls with its wall, keeping its flowers.
	 */
	@GameTest
	public void theWreathHangsOnWallsAndDoors(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos wall = new BlockPos(3, 2, 4);
		helper.setBlock(wall, Blocks.OAK_PLANKS);
		BlockPos pos = wall.north();
		ServerPlayer player = player(helper, new BlockPos(3, 2, 1), new ItemStack(item("autumn_wreath"), 3));
		place(helper, player, new BlockPos(5, 1, 5), Direction.UP);
		helper.assertBlockNotPresent(block("autumn_wreath"), new BlockPos(5, 2, 5));
		place(helper, player, wall, Direction.NORTH);
		BlockState state = helper.getBlockState(pos);
		helper.assertTrue(state.is(block("autumn_wreath")) && state.getValue(AutumnWreathBlock.FACING) == Direction.NORTH
				&& state.getValue(AutumnWreathBlock.FLOWERS) == AutumnWreathBlock.Mums.ORANGE, "It hangs on the wall, facing out, with orange mums");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("red_mum"), 2));
		use(helper, player, pos, Direction.NORTH);
		helper.assertTrue(helper.getBlockState(pos).getValue(AutumnWreathBlock.FLOWERS) == AutumnWreathBlock.Mums.RED
				&& player.getMainHandItem().getCount() == 1, "A red mum makes its flowers red, using the mum");
		use(helper, player, pos, Direction.NORTH);
		helper.assertTrue(player.getMainHandItem().getCount() == 1, "The same colour again does nothing");

		BlockPos door = new BlockPos(6, 2, 4);
		helper.setBlock(door, Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		helper.setBlock(door.above(), Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("autumn_wreath")));
		place(helper, player, door.above(), Direction.NORTH);
		helper.assertTrue(helper.getBlockState(door.above().north()).is(block("autumn_wreath")), "It hangs on a door too");

		level.destroyBlock(helper.absolutePos(wall), false);
		helper.assertBlockNotPresent(block("autumn_wreath"), pos);
		List<ItemEntity> dropped = drops(helper, item("autumn_wreath"));
		helper.assertTrue(dropped.size() == 1, "It falls with its wall and drops once");
		BlockItemStateProperties flowers = dropped.get(0).getItem().get(DataComponents.BLOCK_STATE);
		helper.assertTrue(flowers != null && flowers.apply(block("autumn_wreath").defaultBlockState()).getValue(AutumnWreathBlock.FLOWERS)
				== AutumnWreathBlock.Mums.RED, "and keeps its red mums: " + flowers);
		helper.succeed();
	}

	// ---------------------------------------------------------------- the leaf piles

	/**
	 * A pile placed on the ground is one layer; more of the same heap it up to four; it needs solid ground; broken, a
	 * three-layer pile drops three piles.
	 */
	@GameTest
	public void leafPilesHeapUp(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos ground = new BlockPos(3, 1, 3);
		BlockPos pos = ground.above();
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("orange_leaf_pile"), 8));
		place(helper, player, ground, Direction.UP);
		helper.assertTrue(helper.getBlockState(pos).is(block("orange_leaf_pile")) && helper.getBlockState(pos).getValue(LeafPileBlock.LAYERS) == 1,
				"A pile is one layer");
		for (int layers = 2; layers <= LeafPileBlock.MAX_LAYERS; layers++) {
			place(helper, player, pos, Direction.UP);
			helper.assertTrue(helper.getBlockState(pos).getValue(LeafPileBlock.LAYERS) == layers, "More leaves heap it to " + layers);
		}
		helper.assertTrue(LeafPileBlock.fallDamageFactor(LeafPileBlock.MAX_LAYERS) < 0.25F && LeafPileBlock.fallDamageFactor(1) > 0.75F,
				"A full pile softens a fall far more than one layer");

		BlockPos air = new BlockPos(6, 4, 6);
		helper.setBlock(air.below(), Blocks.OAK_PLANKS);
		place(helper, player, air.below(), Direction.UP);
		helper.assertTrue(helper.getBlockState(air).is(block("orange_leaf_pile")), "It lies on a plank");
		level.destroyBlock(helper.absolutePos(air.below()), false);
		helper.assertBlockNotPresent(block("orange_leaf_pile"), air);

		BlockPos three = new BlockPos(1, 2, 1);
		helper.setBlock(three, block("yellow_leaf_pile").defaultBlockState().setValue(LeafPileBlock.LAYERS, 3));
		level.destroyBlock(helper.absolutePos(three), true);
		helper.assertTrue(dropped(helper, item("yellow_leaf_pile")) == 3, "A three-layer pile drops three piles");
		helper.succeed();
	}

	/** The recipes, loot tables and the crate's produce tag load. */
	@GameTest
	public void harvestPartyDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("bobbing_tub", "pumpkin_crate", "hay_bale_seat", "autumn_wreath_yellow", "autumn_wreath_orange",
				"autumn_wreath_red", "autumn_wreath_purple", "red_leaf_pile", "orange_leaf_pile", "yellow_leaf_pile")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), "Recipe " + id + " loads");
		}
		for (String id : List.of("bobbing_tub", "pumpkin_crate", "hay_bale_seat", "autumn_wreath", "red_leaf_pile", "orange_leaf_pile",
				"yellow_leaf_pile")) {
			LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("blocks/" + id)));
			helper.assertTrue(table != LootTable.EMPTY, "The " + id + " has a loot table");
		}
		for (Item piece : List.of(Items.PUMPKIN, Items.MELON, item("white_pumpkin"), item("cinderella_pumpkin"), item("acorn_squash"),
				item("bottle_gourd"))) {
			helper.assertTrue(new ItemStack(piece).is(PumpkinCrateBlock.PRODUCE), piece + " is produce for the crate");
		}
		helper.assertFalse(new ItemStack(Items.CARROT).is(PumpkinCrateBlock.PRODUCE), "A carrot isn't");
		helper.succeed();
	}
}
