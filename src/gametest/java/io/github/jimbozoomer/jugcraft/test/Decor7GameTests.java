package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CreepyDollBlock;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.DustSheetBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.HauntedChandelierBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PipeOrganBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PipeOrganBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.SpiritMirrorBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SuitOfArmorBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TatteredCurtainsBlock;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the haunted house inside: the Haunted Chandelier (hanging, lighting, a gust and the relighting,
 * snuffing), the Phantom Pipe Organ (placed and broken as one prop, playing by hand and by redstone), the Suit of
 * Armor (two halves, where its helmet looks), the Dust Sheet (over a chest and its contents, pulled off, broken; not
 * over a plain block), the Spirit Mirror (its face by night only), Tattered Curtains (one drape that opens and closes
 * together, its sway), the Creepy Doll (where it looks when seen again), and that their data loads.
 */
public class Decor7GameTests {
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

	// ---------------------------------------------------------------- the haunted chandelier

	/**
	 * It hangs under a ceiling, not from the open air; flint and steel lights all eight candles (light 12); a gust
	 * blows them out and they relight one at a time; an empty hand snuffs them and they stay out; it falls with its
	 * ceiling, dropping once.
	 */
	@GameTest(maxTicks = 300)
	public void theChandelierBlowsOutAndRelights(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos ceiling = new BlockPos(3, 6, 3);
		helper.setBlock(ceiling, Blocks.STONE);
		BlockPos pos = ceiling.below();
		ServerPlayer player = player(helper, new BlockPos(3, 2, 5), new ItemStack(item("haunted_chandelier"), 2));
		place(helper, player, ceiling, Direction.DOWN);
		helper.assertBlockPresent(block("haunted_chandelier"), pos);
		place(helper, player, new BlockPos(6, 1, 6), Direction.UP);
		helper.assertBlockNotPresent(block("haunted_chandelier"), new BlockPos(6, 2, 6));
		helper.assertTrue(helper.getBlockState(pos).getLightEmission() == 0, "It is placed unlit");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
		use(helper, player, pos, Direction.DOWN);
		BlockState lit = helper.getBlockState(pos);
		helper.assertTrue(lit.getValue(HauntedChandelierBlock.LIT) && lit.getValue(HauntedChandelierBlock.BURNING) == HauntedChandelierBlock.CANDLES
				&& lit.getLightEmission() == 12, "Flint and steel lights all eight candles, light 12: " + lit);

		BlockPos at = helper.absolutePos(pos);
		HauntedChandelierBlock.gust(level, at, lit);
		helper.assertTrue(helper.getBlockState(pos).getValue(HauntedChandelierBlock.BURNING) == 0, "A gust blows every candle out");
		helper.runAfterDelay(HauntedChandelierBlock.RELIGHT_TICKS * 3 + 2, () -> {
			int burning = helper.getBlockState(pos).getValue(HauntedChandelierBlock.BURNING);
			helper.assertTrue(burning > 0 && burning < HauntedChandelierBlock.CANDLES, "They relight one at a time: " + burning);
		});
		helper.runAfterDelay(HauntedChandelierBlock.RELIGHT_TICKS * (HauntedChandelierBlock.CANDLES + 1) + 5, () -> {
			helper.assertTrue(helper.getBlockState(pos).getValue(HauntedChandelierBlock.BURNING) == HauntedChandelierBlock.CANDLES,
					"until all burn again");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			use(helper, player, pos, Direction.DOWN);
			BlockState out = helper.getBlockState(pos);
			helper.assertTrue(!out.getValue(HauntedChandelierBlock.LIT) && out.getValue(HauntedChandelierBlock.BURNING) == 0, "An empty hand snuffs them");
			for (int t = 0; t < HauntedChandelierBlock.SWAY_PERIOD * 2; t += 5) {
				float[] sway = HauntedChandelierBlock.sway(at, t);
				helper.assertTrue(Math.abs(sway[0]) <= HauntedChandelierBlock.SWAY_DEGREES && Math.abs(sway[1]) <= HauntedChandelierBlock.SWAY_DEGREES,
						"It sways no more than its sway");
			}
			helper.runAfterDelay(HauntedChandelierBlock.RELIGHT_TICKS * 2, () -> {
				helper.assertTrue(helper.getBlockState(pos).getValue(HauntedChandelierBlock.BURNING) == 0, "Snuffed by hand, they stay out");
				level.destroyBlock(helper.absolutePos(ceiling), false);
				helper.assertBlockNotPresent(block("haunted_chandelier"), pos);
				helper.assertTrue(dropped(helper, item("haunted_chandelier")) == 1, "It falls with its ceiling, dropping once");
				helper.succeed();
			});
		});
	}

	// ---------------------------------------------------------------- the phantom pipe organ

	/**
	 * Placed, it fills three blocks across and two up with its keyboard toward the player; it won't go where a block
	 * is in the way; breaking a corner breaks it all and drops one organ.
	 */
	@GameTest(maxTicks = 40)
	public void thePipeOrganIsOneProp(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos ground = new BlockPos(3, 1, 3);
		BlockPos master = ground.above();
		ServerPlayer player = player(helper, new BlockPos(3, 2, 0), new ItemStack(item("phantom_pipe_organ"), 2));
		player.setYRot(0.0F); // looking south, at the organ
		helper.setBlock(new BlockPos(4, 3, 6), Blocks.STONE);
		place(helper, player, new BlockPos(3, 1, 6), Direction.UP);
		helper.assertBlockNotPresent(block("phantom_pipe_organ"), new BlockPos(3, 2, 6));
		helper.assertTrue(player.getMainHandItem().getCount() == 2, "Where a block is in the way, it isn't placed");

		place(helper, player, ground, Direction.UP);
		BlockPos absMaster = helper.absolutePos(master);
		for (int part = 0; part < PipeOrganBlock.WIDTH * PipeOrganBlock.HEIGHT; part++) {
			BlockPos at = PipeOrganBlock.partPos(absMaster, Direction.NORTH, part);
			BlockState state = level.getBlockState(at);
			helper.assertTrue(state.is(block("phantom_pipe_organ")) && state.getValue(PipeOrganBlock.PART) == part
					&& state.getValue(PipeOrganBlock.FACING) == Direction.NORTH, "Part " + part + " is in place, facing the player: " + state);
			helper.assertTrue(PipeOrganBlock.masterPos(at, state).equals(absMaster), "and knows its master");
		}
		helper.assertTrue(helper.getBlockEntity(master, PipeOrganBlockEntity.class) != null, "The master holds the tune");

		level.destroyBlock(PipeOrganBlock.partPos(absMaster, Direction.NORTH, 5), true);
		helper.runAfterDelay(3, () -> {
			for (int part = 0; part < PipeOrganBlock.WIDTH * PipeOrganBlock.HEIGHT; part++) {
				helper.assertFalse(level.getBlockState(PipeOrganBlock.partPos(absMaster, Direction.NORTH, part)).is(block("phantom_pipe_organ")),
						"Breaking a corner breaks it all");
			}
			helper.assertTrue(dropped(helper, item("phantom_pipe_organ")) == 1, "and drops one organ");
			helper.succeed();
		});
	}

	/**
	 * Using any block of it plays the tune from the top, and using it again stops it; a redstone signal at a corner
	 * plays it; it falls quiet at the end of the tune. Every note is a note block's, and every key on the keyboard.
	 */
	@GameTest(maxTicks = 400)
	public void thePipeOrganPlaysItsTune(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos master = new BlockPos(3, 2, 3);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 0), new ItemStack(item("phantom_pipe_organ")));
		player.setYRot(0.0F);
		place(helper, player, master.below(), Direction.UP);
		BlockPos absMaster = helper.absolutePos(master);
		BlockPos corner = PipeOrganBlock.partPos(absMaster, Direction.NORTH, 3);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(corner), Direction.NORTH, corner, false));
		PipeOrganBlockEntity organ = helper.getBlockEntity(master, PipeOrganBlockEntity.class);
		helper.assertTrue(helper.getBlockState(master).getValue(PipeOrganBlock.PLAYING) && organ.tick() == 0
				&& organ.startTime() == level.getGameTime(), "Using any block of it plays the tune from the top");
		use(helper, player, master, Direction.NORTH);
		helper.assertFalse(helper.getBlockState(master).getValue(PipeOrganBlock.PLAYING), "Using it again stops it");
		for (int[] note : PipeOrganBlockEntity.TUNE) {
			helper.assertTrue(note[0] >= 0 && note[0] < PipeOrganBlockEntity.TUNE_TICKS && note[1] >= 0 && note[1] <= 24,
					"Every note is in the tune and a note block's range");
			int key = PipeOrganBlockEntity.key(note[1], note[2]);
			helper.assertTrue(key >= 0 && key < 28, "and on the keyboard");
		}
		helper.assertTrue(PipeOrganBlockEntity.key(15, PipeOrganBlockEntity.FLUTE) > PipeOrganBlockEntity.key(15, PipeOrganBlockEntity.HARP)
				&& PipeOrganBlockEntity.key(15, PipeOrganBlockEntity.HARP) > PipeOrganBlockEntity.key(15, PipeOrganBlockEntity.BASS),
				"The flute plays above the harp, and the harp above the bass");

		BlockPos power = new BlockPos(1, 2, 3);
		helper.setBlock(power, Blocks.REDSTONE_BLOCK);
		helper.assertTrue(helper.getBlockState(master).getValue(PipeOrganBlock.PLAYING) && helper.getBlockState(master).getValue(PipeOrganBlock.POWERED),
				"A redstone signal at a corner plays it");
		helper.runAfterDelay(PipeOrganBlockEntity.TUNE_TICKS / 2, () -> helper.assertTrue(helper.getBlockEntity(master, PipeOrganBlockEntity.class).tick() > 0,
				"It is part way through"));
		helper.runAfterDelay(PipeOrganBlockEntity.TUNE_TICKS + 5, () -> {
			helper.assertFalse(helper.getBlockState(master).getValue(PipeOrganBlock.PLAYING), "At the end of the tune it falls quiet");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- the suit of armor

	/**
	 * It stands two blocks tall facing the player, its helmet's block entity on the upper half; its helmet looks at a
	 * player in front, turns at most 75 degrees, and looks ahead with nobody near; it turns a few degrees a tick; broken,
	 * it drops once.
	 */
	@GameTest(maxTicks = 40)
	public void theSuitOfArmorWatches(GameTestHelper helper) {
		floor(helper);
		BlockPos ground = new BlockPos(3, 1, 3);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("suit_of_armor")));
		player.setYRot(180.0F);
		place(helper, player, ground, Direction.UP);
		BlockState lower = helper.getBlockState(ground.above());
		BlockState upper = helper.getBlockState(ground.above(2));
		helper.assertTrue(lower.is(block("suit_of_armor")) && lower.getValue(SuitOfArmorBlock.HALF) == DoubleBlockHalf.LOWER
				&& upper.is(block("suit_of_armor")) && upper.getValue(SuitOfArmorBlock.FACING) == Direction.SOUTH, "It stands two blocks tall, facing the player");
		helper.assertTrue(helper.getBlockEntity(ground.above(2), DecorationBlockEntity.class) != null, "Its helmet is drawn from the upper half");

		Vec3 head = new Vec3(0.5, 0.0, 0.5);
		helper.assertTrue(Math.abs(SuitOfArmorBlock.watchYaw(head, Direction.NORTH, new Vec3(0.5, 0.0, -4.5))) < 1e-3, "It looks straight at a player ahead");
		helper.assertTrue(Math.abs(SuitOfArmorBlock.watchYaw(head, Direction.NORTH, new Vec3(-4.5, 0.0, -4.5)) + 45.0F) < 1e-3,
				"and turns to one ahead and to its left");
		helper.assertTrue(SuitOfArmorBlock.watchYaw(head, Direction.NORTH, new Vec3(5.5, 0.0, 0.5)) == SuitOfArmorBlock.MAX_TURN,
				"but no more than " + SuitOfArmorBlock.MAX_TURN + " degrees");
		helper.assertTrue(Math.abs(SuitOfArmorBlock.watchYaw(head, Direction.EAST, new Vec3(5.5, 0.0, 0.5))) < 1e-3,
				"Facing east, a player to the east is straight ahead");
		helper.assertTrue(SuitOfArmorBlock.watchYaw(head, Direction.NORTH, null) == 0.0F, "With nobody near it looks ahead");
		helper.assertTrue(SuitOfArmorBlock.turnToward(0.0F, 60.0F, SuitOfArmorBlock.TURN_SPEED) == SuitOfArmorBlock.TURN_SPEED
				&& SuitOfArmorBlock.turnToward(58.0F, 60.0F, SuitOfArmorBlock.TURN_SPEED) == 60.0F, "It turns slowly, and stops where it looks");

		helper.getLevel().destroyBlock(helper.absolutePos(ground.above(2)), true);
		helper.runAfterDelay(3, () -> {
			helper.assertBlockNotPresent(block("suit_of_armor"), ground.above());
			helper.assertTrue(dropped(helper, item("suit_of_armor")) == 1, "Broken, it drops once");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- the dust sheet

	/**
	 * A sheet goes over a chest (not opening it), keeps the chest and its contents and the chest's shape; an empty hand
	 * pulls it off, giving the chest back as it was and the sheet back to the player; broken, a sheeted chest drops the
	 * sheet, the chest and its contents. A plain stone block can't be covered.
	 */
	@GameTest(maxTicks = 40)
	public void aDustSheetKeepsTheChestUnderIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, Blocks.CHEST);
		ChestBlockEntity chest = helper.getBlockEntity(pos, ChestBlockEntity.class);
		chest.setItem(0, new ItemStack(Items.DIAMOND, 5));
		chest.setItem(13, new ItemStack(Items.APPLE, 3));
		ServerPlayer player = player(helper, new BlockPos(3, 2, 1), new ItemStack(item("dust_sheet"), 3));
		BlockPos stone = new BlockPos(5, 2, 3);
		helper.setBlock(stone, Blocks.STONE);
		use(helper, player, stone, Direction.UP);
		helper.assertTrue(helper.getBlockState(stone).is(Blocks.STONE) && player.getMainHandItem().getCount() == 3, "A plain stone block can't be covered");

		helper.assertTrue(use(helper, player, pos, Direction.UP).consumesAction(), "Using a sheet on a chest covers it");
		helper.assertTrue(helper.getBlockState(pos).is(block("dust_sheet")) && player.containerMenu == player.inventoryMenu,
				"instead of opening it");
		helper.assertTrue(player.getMainHandItem().getCount() == 2, "It takes one sheet");
		DustSheetBlockEntity sheet = helper.getBlockEntity(pos, DustSheetBlockEntity.class);
		helper.assertTrue(sheet.covered().is(Blocks.CHEST) && sheet.coveredData() != null, "The chest is kept under the sheet");
		helper.assertTrue(dropped(helper, Items.DIAMOND) == 0, "Nothing spilled");
		BlockPos at = helper.absolutePos(pos);
		helper.assertTrue(helper.getBlockState(pos).getShape(level, at).bounds().equals(Blocks.CHEST.defaultBlockState().getShape(level, at).bounds()),
				"The sheet takes the chest's shape");

		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, pos, Direction.UP);
		helper.assertBlockPresent(Blocks.CHEST, pos);
		ChestBlockEntity back = helper.getBlockEntity(pos, ChestBlockEntity.class);
		helper.assertTrue(back.getItem(0).is(Items.DIAMOND) && back.getItem(0).getCount() == 5 && back.getItem(13).is(Items.APPLE)
				&& back.getItem(13).getCount() == 3, "Pulled off, the chest is back with all it held");
		helper.assertTrue(player.getInventory().countItem(item("dust_sheet")) == 1, "and the sheet goes back to the player");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("dust_sheet")));
		use(helper, player, pos, Direction.UP);
		helper.assertBlockPresent(block("dust_sheet"), pos);
		level.destroyBlock(at, true);
		helper.runAfterDelay(3, () -> {
			helper.assertTrue(dropped(helper, item("dust_sheet")) == 1 && dropped(helper, Items.CHEST) == 1, "Broken, it drops the sheet and the chest");
			helper.assertTrue(dropped(helper, Items.DIAMOND) == 5 && dropped(helper, Items.APPLE) == 3, "and spills what was in the chest");
			helper.succeed();
		});
	}

	/** A sheet goes over a Rocking Chair (rather than sitting in it) and comes off it again, leaving the chair. */
	@GameTest
	public void aDustSheetCoversTheRockingChair(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, block("rocking_chair"));
		ServerPlayer player = player(helper, new BlockPos(3, 2, 1), new ItemStack(item("dust_sheet")));
		use(helper, player, pos, Direction.UP);
		helper.assertTrue(helper.getBlockState(pos).is(block("dust_sheet")) && !player.isPassenger(), "The sheet covers the chair; nobody sits");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, pos, Direction.UP);
		helper.assertTrue(helper.getBlockState(pos).is(block("rocking_chair")) && helper.getBlockEntity(pos, DecorationBlockEntity.class) != null,
				"Pulled off, the chair is back");
		helper.succeed();
	}

	// ---------------------------------------------------------------- the spirit mirror

	/**
	 * It hangs on a wall and falls when the wall goes; by day it shows no face; at night the face shows for its time in
	 * each period, fading in and out.
	 */
	@GameTest
	public void theSpiritMirrorShowsAFaceAtNight(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos wall = new BlockPos(3, 2, 4);
		helper.setBlock(wall, Blocks.STONE);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 1), new ItemStack(item("spirit_mirror")));
		place(helper, player, wall, Direction.NORTH);
		BlockPos pos = wall.north();
		helper.assertTrue(helper.getBlockState(pos).is(block("spirit_mirror")) && helper.getBlockState(pos).getValue(SpiritMirrorBlock.FACING) == Direction.NORTH,
				"It hangs on the wall, facing out");
		BlockPos at = helper.absolutePos(pos);
		int showing = 0;
		float most = 0.0F;
		for (int t = 0; t < SpiritMirrorBlock.PERIOD; t++) {
			helper.assertTrue(SpiritMirrorBlock.face(at, false, t) == 0.0F, "By day there is no face");
			float face = SpiritMirrorBlock.face(at, true, t);
			showing += face > 0.0F ? 1 : 0;
			most = Math.max(most, face);
		}
		helper.assertTrue(showing >= SpiritMirrorBlock.VISIBLE - 1 && showing <= SpiritMirrorBlock.VISIBLE,
				"At night the face shows " + SpiritMirrorBlock.VISIBLE + " ticks a period: " + showing);
		helper.assertTrue(most == 1.0F, "fully, between fading in and out");
		level.destroyBlock(helper.absolutePos(wall), false);
		helper.assertBlockNotPresent(block("spirit_mirror"), pos);
		helper.assertTrue(dropped(helper, item("spirit_mirror")) == 1, "It falls with its wall, dropping once");
		helper.succeed();
	}

	// ---------------------------------------------------------------- tattered curtains

	/**
	 * Three curtains placed down a wall make one drape (rod at the top, hem at the bottom); using any of them opens the
	 * whole drape, and using it again closes it; the cloth doesn't move at the rod, sways no more than its sway, more at
	 * night.
	 */
	@GameTest
	public void tatteredCurtainsMakeOneDrape(GameTestHelper helper) {
		floor(helper);
		for (int y = 2; y <= 6; y++) {
			helper.setBlock(new BlockPos(3, y, 4), Blocks.STONE);
		}
		ServerPlayer player = player(helper, new BlockPos(3, 2, 1), new ItemStack(item("tattered_curtains"), 3));
		for (int y = 5; y >= 3; y--) {
			place(helper, player, new BlockPos(3, y, 4), Direction.NORTH);
		}
		TatteredCurtainsBlock.Part[] parts = {TatteredCurtainsBlock.Part.BOTTOM, TatteredCurtainsBlock.Part.MIDDLE, TatteredCurtainsBlock.Part.TOP};
		for (int y = 3; y <= 5; y++) {
			BlockState state = helper.getBlockState(new BlockPos(3, y, 3));
			helper.assertTrue(state.is(block("tattered_curtains")) && state.getValue(TatteredCurtainsBlock.PART) == parts[y - 3]
					&& state.getValue(TatteredCurtainsBlock.FACING) == Direction.NORTH, "One drape, rod to hem: " + state);
		}
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, new BlockPos(3, 4, 3), Direction.NORTH);
		for (int y = 3; y <= 5; y++) {
			helper.assertTrue(helper.getBlockState(new BlockPos(3, y, 3)).getValue(TatteredCurtainsBlock.OPEN), "Using one opens the whole drape");
		}
		use(helper, player, new BlockPos(3, 3, 3), Direction.NORTH);
		for (int y = 3; y <= 5; y++) {
			helper.assertFalse(helper.getBlockState(new BlockPos(3, y, 3)).getValue(TatteredCurtainsBlock.OPEN), "and using one again closes it");
		}
		BlockPos at = helper.absolutePos(new BlockPos(3, 5, 3));
		float day = 0.0F;
		float night = 0.0F;
		for (int t = 0; t < TatteredCurtainsBlock.SWAY_PERIOD * 3; t += 3) {
			helper.assertTrue(TatteredCurtainsBlock.sway(at, true, t, 0.0F, 0.5F) == 0.0F, "At the rod it doesn't move");
			day = Math.max(day, Math.abs(TatteredCurtainsBlock.sway(at, false, t, 4.0F, 0.3F)));
			night = Math.max(night, Math.abs(TatteredCurtainsBlock.sway(at, true, t, 4.0F, 0.3F)));
		}
		helper.assertTrue(day > 0.0F && day <= TatteredCurtainsBlock.SWAY && night > day && night <= TatteredCurtainsBlock.NIGHT_SWAY,
				"It sways at the hem, more at night: " + day + ", " + night);
		helper.succeed();
	}

	// ---------------------------------------------------------------- the creepy doll

	/**
	 * It sits on a fence post and falls when the post goes; looked at again, it mostly looks at the viewer and now and
	 * then far off to one side, never turning its head more than it can.
	 */
	@GameTest
	public void theCreepyDollLooksBack(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos post = new BlockPos(3, 2, 3);
		helper.setBlock(post, Blocks.OAK_FENCE);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("creepy_doll")));
		place(helper, player, post, Direction.UP);
		helper.assertBlockPresent(block("creepy_doll"), post.above());
		BlockPos at = helper.absolutePos(post.above());
		int elsewhere = 0;
		int glances = 300;
		for (int glance = 1; glance <= glances; glance++) {
			float yaw = CreepyDollBlock.glance(at, glance, 20.0F);
			helper.assertTrue(Math.abs(yaw) <= CreepyDollBlock.MAX_TURN, "It never turns its head more than it can");
			if (Math.abs(yaw - 20.0F) > 45.0F) {
				elsewhere++;
			} else {
				helper.assertTrue(yaw == 20.0F, "Otherwise it looks right at the viewer");
			}
		}
		int expected = glances / CreepyDollBlock.ELSEWHERE_CHANCE;
		helper.assertTrue(elsewhere > expected / 2 && elsewhere < expected * 3 / 2,
				"About one glance in " + CreepyDollBlock.ELSEWHERE_CHANCE + " finds it looking elsewhere: " + elsewhere);
		level.destroyBlock(helper.absolutePos(post), false);
		helper.assertBlockNotPresent(block("creepy_doll"), post.above());
		helper.assertTrue(dropped(helper, item("creepy_doll")) == 1, "It falls with its post, dropping once");
		helper.succeed();
	}

	/** The recipes and loot tables load, and the sheet's tag holds chests and slabs. */
	@GameTest
	public void hauntedHouseInsideDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("haunted_chandelier", "phantom_pipe_organ", "suit_of_armor", "dust_sheet", "spirit_mirror", "tattered_curtains",
				"creepy_doll")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), "Recipe " + id + " loads");
			LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("blocks/" + id)));
			helper.assertTrue(table != LootTable.EMPTY, "The " + id + " drops itself");
		}
		helper.assertTrue(Blocks.CHEST.defaultBlockState().is(JugcraftAgriculture.DUST_SHEET_COVERABLE)
				&& Blocks.OAK_SLAB.defaultBlockState().is(JugcraftAgriculture.DUST_SHEET_COVERABLE)
				&& !Blocks.STONE.defaultBlockState().is(JugcraftAgriculture.DUST_SHEET_COVERABLE), "A sheet goes over chests and slabs, not stone");
		helper.succeed();
	}
}
