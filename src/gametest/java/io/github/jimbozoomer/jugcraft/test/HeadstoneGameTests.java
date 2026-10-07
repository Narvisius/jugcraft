package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.Epitaph;
import io.github.jimbozoomer.jugcraft.agriculture.Epitaphs;
import io.github.jimbozoomer.jugcraft.agriculture.HeadstoneBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HeadstoneBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
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
 * In-game tests for the graveyard pack's headstones: tall and long ones place whole or not at all and break as one,
 * dropping once with their epitaph; the epitaph screen's lines are checked on the server (session, chisel, reach, build
 * rights, the headstone, the lines' size) before they are cut, and a named Name Tag cuts the first line; headstones
 * weather with time and bone meal, a brush scrubs them, honeycomb waxes them and an axe takes the wax off, every part
 * alike; and the pack's data loads.
 */
public class HeadstoneGameTests {
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

	private static List<ItemEntity> itemsAround(GameTestHelper helper, BlockPos pos) {
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(3.0));
	}

	private static int dropped(GameTestHelper helper, BlockPos pos, Item item) {
		return itemsAround(helper, pos).stream().filter(entity -> entity.getItem().is(item)).mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	private static boolean earned(ServerPlayer player, String id) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Jugcraft.id(id));
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	private static boolean isPart(GameTestHelper helper, BlockPos pos, String id, int part, Direction facing) {
		BlockState state = helper.getBlockState(pos);
		return state.is(block(id)) && state.getValue(HeadstoneBlock.PART) == part && state.getValue(HeadstoneBlock.FACING) == facing;
	}

	/**
	 * A Celtic cross goes up three blocks and a table tomb back two, both facing whoever placed them; with a block in
	 * the way nothing is placed; breaking any part breaks the whole and drops it once.
	 */
	@GameTest
	public void headstonesPlaceAndBreakAsOne(GameTestHelper helper) {
		floor(helper);
		// A mock player looks south, so what they place faces north and runs back to the south.
		ServerPlayer mason = player(helper, new BlockPos(2, 2, 0), new ItemStack(item("celtic_cross")));
		helper.assertTrue(use(helper, mason, new BlockPos(2, 1, 2), Direction.UP).consumesAction(), "The cross is placed");
		for (int part = 0; part < 3; part++) {
			helper.assertTrue(isPart(helper, new BlockPos(2, 2 + part, 2), "celtic_cross", part, Direction.NORTH), "Cross part " + part + " stands");
		}
		mason.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("table_tomb")));
		use(helper, mason, new BlockPos(5, 1, 2), Direction.UP);
		helper.assertTrue(isPart(helper, new BlockPos(5, 2, 2), "table_tomb", 0, Direction.NORTH)
				&& isPart(helper, new BlockPos(5, 2, 3), "table_tomb", 1, Direction.NORTH), "The tomb runs back a block");
		helper.assertTrue(helper.getBlockEntity(new BlockPos(5, 2, 2), HeadstoneBlockEntity.class) != null, "Its foot keeps the epitaph");

		helper.setBlock(new BlockPos(2, 3, 5), Blocks.STONE);
		mason.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("celtic_cross")));
		use(helper, mason, new BlockPos(2, 1, 5), Direction.UP);
		helper.assertBlockNotPresent(block("celtic_cross"), new BlockPos(2, 2, 5));

		ServerLevel level = helper.getLevel();
		level.destroyBlock(helper.absolutePos(new BlockPos(2, 4, 2)), true);
		for (int part = 0; part < 3; part++) {
			helper.assertBlockNotPresent(block("celtic_cross"), new BlockPos(2, 2 + part, 2));
		}
		helper.assertTrue(dropped(helper, new BlockPos(2, 2, 2), item("celtic_cross")) == 1, "Breaking its top drops one cross");
		level.destroyBlock(helper.absolutePos(new BlockPos(5, 2, 3)), true);
		helper.assertBlockNotPresent(block("table_tomb"), new BlockPos(5, 2, 2));
		helper.assertTrue(dropped(helper, new BlockPos(5, 2, 2), item("table_tomb")) == 1, "Breaking its head drops one tomb");
		helper.succeed();
	}

	/**
	 * The epitaph screen's lines are cut only with a session for that headstone, a chisel in hand, in reach, with build
	 * rights, on a headstone, and only if they fit; the chisel wears; a named Name Tag cuts the first line; the epitaph
	 * goes with the broken headstone and comes back when it is placed.
	 */
	@GameTest
	public void epitaphsAreCheckedThenCut(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(2, 2, 2);
		helper.setBlock(pos, block("gothic_headstone"));
		BlockPos at = helper.absolutePos(pos);
		HeadstoneBlockEntity stone = helper.getBlockEntity(pos, HeadstoneBlockEntity.class);
		ServerPlayer mason = player(helper, pos.south(), new ItemStack(item(Epitaphs.CHISEL)));
		List<String> lines = List.of("ADA LOVELACE", "1815 - 1852", "", "Poetical science");

		helper.assertTrue(Epitaphs.engrave(mason, at, lines) == Epitaphs.Result.NO_SESSION, "Not without a session");
		Epitaphs.startSession(mason, at.east());
		helper.assertTrue(Epitaphs.engrave(mason, at, lines) == Epitaphs.Result.NO_SESSION, "Not with one for another block");
		Epitaphs.startSession(mason, at);
		helper.assertTrue(Epitaphs.engrave(mason, at, List.of("a", "b", "c", "d", "e")) == Epitaphs.Result.INVALID, "Not five lines");
		Epitaphs.startSession(mason, at);
		helper.assertTrue(Epitaphs.engrave(mason, at, List.of("x".repeat(Epitaph.LINE_LENGTH + 1))) == Epitaphs.Result.INVALID,
				"Not a line too long");
		helper.assertTrue(stone.epitaph().isBlank(), "Nothing has been cut yet");

		ServerPlayer visitor = player(helper, pos.south(), new ItemStack(item(Epitaphs.CHISEL)));
		visitor.setGameMode(GameType.ADVENTURE);
		Epitaphs.startSession(visitor, at);
		helper.assertTrue(Epitaphs.engrave(visitor, at, lines) == Epitaphs.Result.NOT_ALLOWED, "Not without build rights");
		ServerPlayer bare = player(helper, pos.south(), ItemStack.EMPTY);
		Epitaphs.startSession(bare, at);
		helper.assertTrue(Epitaphs.engrave(bare, at, lines) == Epitaphs.Result.NO_CHISEL, "Not without a chisel");
		ServerPlayer far = player(helper, pos.south(), new ItemStack(item(Epitaphs.CHISEL)));
		far.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 20.5);
		Epitaphs.startSession(far, at);
		helper.assertTrue(Epitaphs.engrave(far, at, lines) == Epitaphs.Result.TOO_FAR, "Not from far off");

		// Using the chisel on the stone opens the session itself.
		helper.assertTrue(use(helper, mason, pos, Direction.SOUTH).consumesAction(), "The chisel opens the epitaph screen");
		helper.assertTrue(Epitaphs.engrave(mason, at, lines) == Epitaphs.Result.ENGRAVED, "Then the lines are cut");
		helper.assertTrue(stone.epitaph().lines().equals(List.of("ADA LOVELACE", "1815 - 1852", "", "Poetical science")), "as written");
		helper.assertTrue(mason.getMainHandItem().getDamageValue() == 1 && earned(mason, "here_lies"), "The chisel wears; Here Lies… is earned");
		helper.assertTrue(Epitaphs.engrave(mason, at, lines) == Epitaphs.Result.NO_SESSION, "One session, one cut");
		helper.assertTrue(Epitaph.of(List.of("  Jack\u0007  ", "", "")).lines().equals(List.of("Jack")),
				"Control characters, spaces and blank lines at the end are dropped");

		ItemStack tag = new ItemStack(Items.NAME_TAG);
		tag.set(DataComponents.CUSTOM_NAME, Component.literal("AUGUSTA ADA KING"));
		mason.setItemInHand(InteractionHand.MAIN_HAND, tag);
		use(helper, mason, pos, Direction.SOUTH);
		helper.assertTrue(stone.epitaph().lines().get(0).equals("AUGUSTA ADA KING") && stone.epitaph().lines().size() == 4,
				"A named tag cuts the first line and keeps the rest");

		helper.getLevel().destroyBlock(at, true);
		ItemStack broken = itemsAround(helper, pos).stream().map(ItemEntity::getItem).filter(s -> s.is(item("gothic_headstone")))
				.findFirst().orElse(ItemStack.EMPTY);
		Epitaph carried = broken.get(JugcraftAgriculture.EPITAPH);
		helper.assertTrue(carried != null && carried.lines().get(0).equals("AUGUSTA ADA KING"), "The broken headstone keeps its epitaph: " + broken);
		mason.setItemInHand(InteractionHand.MAIN_HAND, broken.copy());
		helper.assertTrue(use(helper, mason, pos.below(), Direction.UP).consumesAction(), "It goes back down");
		HeadstoneBlockEntity again = helper.getBlockEntity(pos, HeadstoneBlockEntity.class);
		helper.assertTrue(again.epitaph().equals(carried), "with its epitaph");
		helper.succeed();
	}

	/**
	 * Bone meal ages a headstone a stage (and no further than overgrown); a brush scrubs a stage off (Groundskeeper,
	 * from overgrown) and wears; honeycomb waxes it, after which it neither ages nor takes bone meal; an axe scrapes the
	 * wax off. Every block of a tall headstone changes together, and random ticks weather an unwaxed stone in time.
	 */
	@GameTest
	public void headstonesWeatherAndAreKept(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(2, 2, 2);
		helper.setBlock(pos, block("willow_urn_headstone"));
		ServerPlayer keeper = player(helper, pos.south(), new ItemStack(Items.BONE_MEAL, 8));
		for (int i = 0; i < 4; i++) {
			use(helper, keeper, pos, Direction.SOUTH);
		}
		helper.assertTrue(HeadstoneBlock.stage(helper.getBlockState(pos)) == HeadstoneBlock.OVERGROWN && keeper.getMainHandItem().getCount() == 5,
				"Three bone meal age it to overgrown; a fourth is not used");
		keeper.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BRUSH));
		use(helper, keeper, pos, Direction.SOUTH);
		helper.assertTrue(HeadstoneBlock.stage(helper.getBlockState(pos)) == 2 && keeper.getMainHandItem().getDamageValue() == 1,
				"A brush scrubs a stage off and wears");
		helper.assertTrue(earned(keeper, "groundskeeper"), "Scrubbing an overgrown stone earns Groundskeeper");
		keeper.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HONEYCOMB, 2));
		use(helper, keeper, pos, Direction.SOUTH);
		helper.assertTrue(helper.getBlockState(pos).getValue(HeadstoneBlock.WAXED) && keeper.getMainHandItem().getCount() == 1, "Honeycomb waxes it");
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(pos);
		RandomSource random = RandomSource.create(4);
		for (int i = 0; i < 2000; i++) {
			BlockState state = level.getBlockState(at);
			state.randomTick(level, at, random);
		}
		helper.assertTrue(HeadstoneBlock.stage(helper.getBlockState(pos)) == 2, "Waxed, it does not weather");
		keeper.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE_MEAL));
		use(helper, keeper, pos, Direction.SOUTH);
		helper.assertTrue(HeadstoneBlock.stage(helper.getBlockState(pos)) == 2 && keeper.getMainHandItem().getCount() == 1, "nor takes bone meal");
		keeper.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
		use(helper, keeper, pos, Direction.SOUTH);
		helper.assertFalse(helper.getBlockState(pos).getValue(HeadstoneBlock.WAXED), "An axe scrapes the wax off");
		for (int i = 0; i < 2000; i++) {
			level.getBlockState(at).randomTick(level, at, random);
		}
		helper.assertTrue(HeadstoneBlock.stage(helper.getBlockState(pos)) == HeadstoneBlock.OVERGROWN, "Unwaxed, it weathers in time");

		ServerPlayer mason = player(helper, new BlockPos(5, 2, 0), new ItemStack(item("broken_column")));
		use(helper, mason, new BlockPos(5, 1, 2), Direction.UP);
		mason.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE_MEAL));
		use(helper, mason, new BlockPos(5, 3, 2), Direction.SOUTH);
		helper.assertTrue(HeadstoneBlock.stage(helper.getBlockState(new BlockPos(5, 2, 2))) == 1
				&& HeadstoneBlock.stage(helper.getBlockState(new BlockPos(5, 3, 2))) == 1, "Both blocks of a column age together");
		ServerPlayer visitor = player(helper, new BlockPos(5, 2, 0), new ItemStack(Items.BONE_MEAL));
		visitor.setGameMode(GameType.ADVENTURE);
		use(helper, visitor, new BlockPos(5, 2, 2), Direction.SOUTH);
		helper.assertTrue(HeadstoneBlock.stage(helper.getBlockState(new BlockPos(5, 2, 2))) == 1, "Without build rights nothing changes");
		for (int i = 1; i < HeadstoneBlock.STIR.length; i++) {
			helper.assertTrue(HeadstoneBlock.STIR[i] > HeadstoneBlock.STIR[i - 1], "The more neglected, the more often a grave stirs");
		}
		helper.succeed();
	}

	/** Every headstone's and the chisel's recipe, every loot table and both advancements load. */
	@GameTest
	public void graveyardDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (HeadstoneBlock.Style style : HeadstoneBlock.Style.values()) {
			String recipe = level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(style.id))).isPresent() ? style.id
					: style.id + "_from_stonecutting";
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(recipe))).isPresent(), style.id + " has a recipe");
			helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
					Jugcraft.id("blocks/" + style.id))) != LootTable.EMPTY, style.id + "'s loot loads");
		}
		helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(Epitaphs.CHISEL))).isPresent(),
				"The chisel's recipe loads");
		for (String id : List.of("here_lies", "groundskeeper")) {
			helper.assertTrue(level.getServer().getAdvancements().get(Jugcraft.id(id)) != null, id + " loads");
		}
		helper.succeed();
	}
}
