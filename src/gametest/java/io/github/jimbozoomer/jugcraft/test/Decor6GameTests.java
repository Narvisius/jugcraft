package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.GiantFakeSpiderBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.LurkingEyesBlock;
import io.github.jimbozoomer.jugcraft.agriculture.MusicBoxBlock;
import io.github.jimbozoomer.jugcraft.agriculture.MusicBoxBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.RockingChairBlock;
import io.github.jimbozoomer.jugcraft.agriculture.Seat;
import io.github.jimbozoomer.jugcraft.agriculture.SilhouetteWindowBlock;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the haunted house and yard: the Rocking Chair (sitting, and how far it rocks when), the Lurking
 * Eyes (what holds them, when they show and blink), the Silhouette Window (glowing only on the side away from a lamp,
 * cycling its designs, keeping its design when broken), the Spooky Music Box (winding, powering, playing on while
 * powered and finishing when the power goes) and the Giant Fake Spider (what it hangs from, letting out its thread,
 * swaying), and that their data loads.
 */
public class Decor6GameTests {
	private static final int TUNE_TICKS = MusicBoxBlockEntity.BEATS * MusicBoxBlockEntity.TICKS_PER_BEAT;

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

	// ---------------------------------------------------------------- the rocking chair

	/**
	 * The chair faces whoever placed it and seats one player at its seat; it is still by day with nobody in it, rocks
	 * gently under a sitter and further on its own at night; breaking it stands its sitter up.
	 */
	@GameTest(maxTicks = 40)
	public void theRockingChairRocks(GameTestHelper helper) {
		floor(helper);
		BlockPos ground = new BlockPos(3, 1, 3);
		BlockPos pos = ground.above();
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("rocking_chair")));
		player.setYRot(180.0F);
		place(helper, player, ground, Direction.UP);
		helper.assertTrue(helper.getBlockState(pos).is(block("rocking_chair"))
				&& helper.getBlockState(pos).getValue(RockingChairBlock.FACING) == Direction.SOUTH, "It faces whoever placed it");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		helper.assertTrue(use(helper, player, pos, Direction.UP).consumesAction() && player.getVehicle() instanceof Seat, "Using it sits the player in it");
		double seat = player.getVehicle().getY() - helper.absolutePos(pos).getY();
		helper.assertTrue(Math.abs(seat - RockingChairBlock.SEAT_HEIGHT) < 0.01, "on its seat (" + seat + ")");

		BlockPos at = helper.absolutePos(pos);
		float still = 0;
		float sitter = 0;
		float haunted = 0;
		for (int t = 0; t < RockingChairBlock.ROCK_PERIOD; t++) {
			still = Math.max(still, Math.abs(RockingChairBlock.rock(at, false, false, t)));
			sitter = Math.max(sitter, Math.abs(RockingChairBlock.rock(at, true, false, t)));
			haunted = Math.max(haunted, Math.abs(RockingChairBlock.rock(at, false, true, t)));
		}
		helper.assertTrue(still == 0, "By day an empty chair is still");
		helper.assertTrue(sitter > RockingChairBlock.SITTER_ROCK * 0.9F && sitter <= RockingChairBlock.SITTER_ROCK, "It rocks gently under a sitter: " + sitter);
		helper.assertTrue(haunted > sitter && haunted <= RockingChairBlock.HAUNTED_ROCK, "and further on its own at night: " + haunted);

		helper.getLevel().destroyBlock(at, true);
		helper.runAfterDelay(3, () -> {
			helper.assertTrue(!player.isPassenger() && dropped(helper, item("rocking_chair")) == 1, "Broken, it stands its sitter up and drops once");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- the lurking eyes

	/**
	 * Eyes peer out of leaves or a solid side, not from a fence post's side; they fall when their leaves go; they show
	 * only at night and only from four blocks away; they blink for a few ticks in each period.
	 */
	@GameTest
	public void lurkingEyesPeerFromTheLeaves(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos leaves = new BlockPos(3, 2, 4);
		helper.setBlock(leaves, Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
		ServerPlayer player = player(helper, new BlockPos(3, 2, 1), new ItemStack(item("lurking_eyes"), 3));
		place(helper, player, leaves, Direction.NORTH);
		helper.assertTrue(helper.getBlockState(leaves.north()).is(block("lurking_eyes"))
				&& helper.getBlockState(leaves.north()).getValue(LurkingEyesBlock.FACING) == Direction.NORTH, "They peer out of the leaves");
		BlockPos post = new BlockPos(6, 2, 4);
		helper.setBlock(post, Blocks.OAK_FENCE);
		place(helper, player, post, Direction.NORTH);
		helper.assertBlockNotPresent(block("lurking_eyes"), post.north());
		level.destroyBlock(helper.absolutePos(leaves), false);
		helper.assertBlockNotPresent(block("lurking_eyes"), leaves.north());

		helper.assertFalse(LurkingEyesBlock.showing(false, 10.0), "No eyes by day");
		helper.assertFalse(LurkingEyesBlock.showing(true, LurkingEyesBlock.HIDE_DISTANCE - 0.5), "They vanish up close");
		helper.assertTrue(LurkingEyesBlock.showing(true, 10.0), "At night, from afar, they show");
		BlockPos at = helper.absolutePos(leaves);
		int shut = 0;
		for (int t = 0; t < LurkingEyesBlock.BLINK_PERIOD; t++) {
			shut += LurkingEyesBlock.blinking(at, t) ? 1 : 0;
		}
		helper.assertTrue(shut == LurkingEyesBlock.BLINK_TICKS, "They blink for " + LurkingEyesBlock.BLINK_TICKS + " ticks a period: " + shut);
		helper.succeed();
	}

	// ---------------------------------------------------------------- the silhouette window

	/**
	 * In the dark neither side glows; with glowstone on one side, only the far side glows; sneak-use turns the design
	 * from bat to cat to witch and back; broken, it keeps its design.
	 */
	@GameTest(maxTicks = 60)
	public void theSilhouetteWindowGlowsAwayFromTheLamp(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos ground = new BlockPos(3, 1, 3);
		BlockPos pos = ground.above();
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("silhouette_window")));
		player.setYRot(180.0F);
		place(helper, player, ground, Direction.UP);
		BlockState state = helper.getBlockState(pos);
		helper.assertTrue(state.is(block("silhouette_window")) && state.getValue(SilhouetteWindowBlock.FACING) == Direction.SOUTH
				&& state.getValue(SilhouetteWindowBlock.DESIGN) == SilhouetteWindowBlock.Design.BAT, "It stands facing whoever placed it, a bat");
		BlockPos at = helper.absolutePos(pos);
		helper.assertFalse(SilhouetteWindowBlock.glows(level, at, Direction.SOUTH) || SilhouetteWindowBlock.glows(level, at, Direction.NORTH),
				"In the dark it doesn't glow");

		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.setShiftKeyDown(true);
		SilhouetteWindowBlock.Design[] order = {SilhouetteWindowBlock.Design.CAT, SilhouetteWindowBlock.Design.WITCH, SilhouetteWindowBlock.Design.BAT,
				SilhouetteWindowBlock.Design.CAT};
		for (SilhouetteWindowBlock.Design design : order) {
			use(helper, player, pos, Direction.SOUTH);
			helper.assertTrue(helper.getBlockState(pos).getValue(SilhouetteWindowBlock.DESIGN) == design, "Sneak-use turns it to the " + design);
		}
		player.setShiftKeyDown(false);

		helper.setBlock(pos.north(), Blocks.GLOWSTONE);
		helper.succeedWhen(() -> {
			helper.assertTrue(SilhouetteWindowBlock.glows(level, at, Direction.SOUTH), "With a lamp to the north, the south side glows");
			helper.assertFalse(SilhouetteWindowBlock.glows(level, at, Direction.NORTH), "and the lamp's side doesn't");
			level.destroyBlock(at, true);
			List<ItemEntity> dropped = drops(helper, item("silhouette_window"));
			helper.assertTrue(dropped.size() == 1, "Broken, it drops once");
			BlockItemStateProperties design = dropped.get(0).getItem().get(DataComponents.BLOCK_STATE);
			helper.assertTrue(design != null && design.apply(block("silhouette_window").defaultBlockState()).getValue(SilhouetteWindowBlock.DESIGN)
					== SilhouetteWindowBlock.Design.CAT, "and keeps its cat: " + design);
		});
	}

	// ---------------------------------------------------------------- the music box

	/** Winding opens it and starts the tune at the top; using it again shuts it. Every note is a note block's. */
	@GameTest
	public void theMusicBoxWindsUp(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, block("music_box"));
		ServerPlayer player = player(helper, new BlockPos(3, 2, 5), ItemStack.EMPTY);
		use(helper, player, pos, Direction.UP);
		MusicBoxBlockEntity box = helper.getBlockEntity(pos, MusicBoxBlockEntity.class);
		helper.assertTrue(helper.getBlockState(pos).getValue(MusicBoxBlock.OPEN) && box.tick() == 0, "Winding opens it at the top of the tune");
		use(helper, player, pos, Direction.UP);
		helper.assertFalse(helper.getBlockState(pos).getValue(MusicBoxBlock.OPEN), "Using it again shuts it");
		helper.assertTrue(Math.abs(MusicBoxBlockEntity.pitch(12) - 1.0F) < 1e-6 && Math.abs(MusicBoxBlockEntity.pitch(24) - 2.0F) < 1e-6,
				"Notes are pitched like a note block's");
		for (int[] note : MusicBoxBlockEntity.TUNE) {
			helper.assertTrue(note[0] >= 0 && note[0] < MusicBoxBlockEntity.BEATS && note[1] >= 0 && note[1] <= 24, "Every note is in the tune and in range");
		}
		helper.succeed();
	}

	/** A redstone signal opens it; it plays on past the end of its tune while powered; without power it finishes and shuts. */
	@GameTest(maxTicks = 400)
	public void theMusicBoxPlaysWhilePowered(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		BlockPos power = pos.east();
		helper.setBlock(pos, block("music_box"));
		helper.setBlock(power, Blocks.REDSTONE_BLOCK);
		helper.assertTrue(helper.getBlockState(pos).getValue(MusicBoxBlock.OPEN) && helper.getBlockState(pos).getValue(MusicBoxBlock.POWERED),
				"Power opens it");
		helper.runAfterDelay(TUNE_TICKS + 10, () -> {
			helper.assertTrue(helper.getBlockState(pos).getValue(MusicBoxBlock.OPEN), "While powered it plays on past the end of its tune");
			helper.assertTrue(helper.getBlockEntity(pos, MusicBoxBlockEntity.class).tick() < TUNE_TICKS, "from the top again");
			helper.setBlock(power, Blocks.AIR);
			helper.assertTrue(helper.getBlockState(pos).getValue(MusicBoxBlock.OPEN) && !helper.getBlockState(pos).getValue(MusicBoxBlock.POWERED),
					"Without power it plays to the end of the tune");
			helper.succeedWhen(() -> helper.assertFalse(helper.getBlockState(pos).getValue(MusicBoxBlock.OPEN), "and then shuts"));
		});
	}

	// ---------------------------------------------------------------- the giant fake spider

	/**
	 * The spider hangs from a ceiling, leaves or a spun cobweb but not from the open sky; sneak-use lets its thread out a
	 * block at a time and back to one; it falls when its ceiling goes; it never swings more than its sway.
	 */
	@GameTest
	public void theGiantSpiderDangles(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos ceiling = new BlockPos(3, 6, 3);
		helper.setBlock(ceiling, Blocks.STONE);
		BlockPos pos = ceiling.below();
		ServerPlayer player = player(helper, new BlockPos(3, 2, 5), new ItemStack(item("giant_fake_spider"), 4));
		place(helper, player, ceiling, Direction.DOWN);
		helper.assertBlockPresent(block("giant_fake_spider"), pos);
		place(helper, player, new BlockPos(6, 1, 6), Direction.UP);
		helper.assertBlockNotPresent(block("giant_fake_spider"), new BlockPos(6, 2, 6));
		BlockPos leaves = new BlockPos(1, 6, 1);
		helper.setBlock(leaves, Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
		place(helper, player, leaves, Direction.DOWN);
		helper.assertBlockPresent(block("giant_fake_spider"), leaves.below());
		BlockPos web = new BlockPos(6, 6, 1);
		helper.setBlock(web, block("spun_cobweb"));
		place(helper, player, web, Direction.DOWN);
		helper.assertBlockPresent(block("giant_fake_spider"), web.below());

		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.setShiftKeyDown(true);
		for (int drop : new int[] {2, 3, 4, 1}) {
			use(helper, player, pos, Direction.DOWN);
			helper.assertTrue(helper.getBlockState(pos).getValue(GiantFakeSpiderBlock.DROP) == drop, "Sneak-use lets it down to " + drop);
		}
		for (int t = 0; t < GiantFakeSpiderBlock.SWAY_PERIOD * 3; t += 7) {
			float[] sway = GiantFakeSpiderBlock.sway(helper.absolutePos(pos), t);
			helper.assertTrue(Math.abs(sway[0]) <= GiantFakeSpiderBlock.SWAY_DEGREES && Math.abs(sway[1]) <= GiantFakeSpiderBlock.SWAY_DEGREES,
					"It sways no more than its sway");
		}
		level.destroyBlock(helper.absolutePos(ceiling), false);
		helper.assertBlockNotPresent(block("giant_fake_spider"), pos);
		helper.assertTrue(dropped(helper, item("giant_fake_spider")) == 1, "It falls with its ceiling, dropping once");
		helper.succeed();
	}

	/** The recipes and loot tables load. */
	@GameTest
	public void hauntedHouseDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("rocking_chair", "lurking_eyes", "silhouette_window", "music_box", "giant_fake_spider")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), "Recipe " + id + " loads");
			LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("blocks/" + id)));
			helper.assertTrue(table != LootTable.EMPTY, "The " + id + " drops itself");
		}
		helper.succeed();
	}
}
