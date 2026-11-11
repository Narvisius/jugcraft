package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.nuclear.PlantInterior;
import io.github.jimbozoomer.jugcraft.tv.FlatTvBlock;
import io.github.jimbozoomer.jugcraft.tv.JugcraftTv;
import io.github.jimbozoomer.jugcraft.tv.TvBlockEntity;
import io.github.jimbozoomer.jugcraft.tv.TvPayloads;
import io.github.jimbozoomer.jugcraft.tv.TvSpeakerBlock;
import io.github.jimbozoomer.jugcraft.tv.TvSpeakerBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The flat screen TV (docs/features/flat-screen-tv.md): blocks on a wall join into one screen (the bezel only round
 * the outside, the bottom-left block holding what it plays), split where a block is taken out and join again; with
 * nothing behind them they stand; the SCREEN window's controls are checked on the server (a web address only, the
 * owner's party or creative, from as far as a big screen is watched but not further), a seek while it plays holds it
 * there for the seeker's game, and taking out the bottom-left block keeps the link and its link count. A surround speaker
 * links itself to the nearest screen; a Sync Key links it to another (and unlinks it); breaking either half takes both.
 */
public class FlatTvGameTests {
	private static Block tv() {
		return PlantInterior.BLOCKS.get("flat_tv");
	}

	private static BlockState facing(Direction d) {
		return tv().defaultBlockState().setValue(FlatTvBlock.FACING, d);
	}

	private static void wall(GameTestHelper helper, int z) {
		for (int x = 0; x <= 5; x++) {
			for (int y = 1; y <= 5; y++) {
				helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
			}
		}
	}

	private static boolean edges(GameTestHelper helper, BlockPos pos, boolean left, boolean right, boolean up, boolean down) {
		BlockState s = helper.getBlockState(pos);
		return s.is(tv()) && s.getValue(FlatTvBlock.LEFT) == left && s.getValue(FlatTvBlock.RIGHT) == right && s.getValue(FlatTvBlock.UP) == up
				&& s.getValue(FlatTvBlock.DOWN) == down;
	}

	private static TvBlockEntity be(GameTestHelper helper, BlockPos pos) {
		TvBlockEntity tv = helper.getBlockEntity(pos, TvBlockEntity.class);
		helper.assertTrue(tv != null, "A TV block at " + pos + " has its block entity");
		return tv;
	}

	/** Six blocks on a wall make one 3 x 2 screen; the top right taken out leaves a 3 x 1 and a 2 x 1; put back, one again. */
	@GameTest
	public void tvBlocksJoinIntoOneScreen(GameTestHelper helper) {
		if (TestFocus.skip("tv")) {
			helper.succeed();
			return;
		}
		wall(helper, 1);
		// Facing north (their backs on the wall to the north): the viewer's right is east, +x.
		for (int c = 0; c < 3; c++) {
			for (int r = 0; r < 2; r++) {
				helper.setBlock(new BlockPos(1 + c, 2 + r, 2), facing(Direction.NORTH));
			}
		}
		helper.assertTrue(edges(helper, new BlockPos(1, 2, 2), false, true, true, false), "The bottom left carries on right and up");
		helper.assertTrue(edges(helper, new BlockPos(2, 3, 2), true, true, false, true), "The top middle carries on left, right and down");
		helper.assertTrue(edges(helper, new BlockPos(3, 2, 2), true, false, true, false), "The bottom right ends the screen on the right");
		TvBlockEntity ctrl = be(helper, new BlockPos(1, 2, 2));
		helper.assertTrue(ctrl.isController() && ctrl.screenWidth() == 3 && ctrl.screenHeight() == 2, "The bottom left holds a 3 x 2 screen");
		helper.assertTrue(!be(helper, new BlockPos(2, 2, 2)).isController(), "The others hold nothing");
		helper.assertTrue(!helper.getBlockState(new BlockPos(1, 2, 2)).getValue(FlatTvBlock.STAND), "With a wall behind it hangs");

		helper.setBlock(new BlockPos(3, 3, 2), Blocks.AIR);
		helper.assertTrue(be(helper, new BlockPos(1, 2, 2)).screenWidth() == 3 && be(helper, new BlockPos(1, 2, 2)).screenHeight() == 1,
				"The bottom row is a 3 x 1 screen");
		helper.assertTrue(be(helper, new BlockPos(1, 3, 2)).isController() && be(helper, new BlockPos(1, 3, 2)).screenWidth() == 2,
				"The top row's two make a 2 x 1");
		helper.assertTrue(edges(helper, new BlockPos(1, 2, 2), false, true, false, false), "The bottom left no longer carries on up");

		helper.setBlock(new BlockPos(3, 3, 2), facing(Direction.NORTH));
		helper.assertTrue(be(helper, new BlockPos(1, 2, 2)).screenWidth() == 3 && be(helper, new BlockPos(1, 2, 2)).screenHeight() == 2,
				"Put back, it is one 3 x 2 again");
		helper.assertTrue(!be(helper, new BlockPos(1, 3, 2)).isController(), "The top left gives up its screen");
		helper.succeed();
	}

	/** Two blocks with nothing behind them stand on a foot; a block set behind one and the screen hangs instead. */
	@GameTest
	public void tvStandsWithNothingBehind(GameTestHelper helper) {
		if (TestFocus.skip("tv")) {
			helper.succeed();
			return;
		}
		helper.setBlock(new BlockPos(2, 1, 3), Blocks.STONE);
		helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
		helper.setBlock(new BlockPos(2, 2, 3), facing(Direction.NORTH));
		helper.setBlock(new BlockPos(3, 2, 3), facing(Direction.NORTH));
		helper.assertTrue(helper.getBlockState(new BlockPos(2, 2, 3)).getValue(FlatTvBlock.STAND)
				&& helper.getBlockState(new BlockPos(3, 2, 3)).getValue(FlatTvBlock.STAND), "Nothing behind: the screen stands");
		helper.assertTrue(be(helper, new BlockPos(2, 2, 3)).screenWidth() == 2, "The two make a 2 x 1");
		helper.setBlock(new BlockPos(2, 2, 2), Blocks.STONE);
		helper.runAfterDelay(3, () -> {
			helper.assertTrue(!helper.getBlockState(new BlockPos(2, 2, 3)).getValue(FlatTvBlock.STAND)
					&& !helper.getBlockState(new BlockPos(3, 2, 3)).getValue(FlatTvBlock.STAND), "A wall behind one: the whole screen hangs");
			helper.succeed();
		});
	}

	/**
	 * The controls: a web link sets the screen loading at 0 (held there until its setter's game has it), PLAY starts
	 * it, a seek while it plays holds it at the new moment (till the seeker's game has the picture there, then PLAY),
	 * PAUSE holds it and a seek then keeps it paused; replay and the whole playlist tick on; worked (and the window
	 * asked for) from 40 blocks off but not 70; a file path or script link is refused; only the owner's party changes it
	 * (or a creative player); the bottom-left block broken, the link (and its count) carries on in the screen that's left.
	 */
	@GameTest
	public void tvControlsAreChecked(GameTestHelper helper) {
		if (TestFocus.skip("tv")) {
			helper.succeed();
			return;
		}
		wall(helper, 1);
		for (int c = 0; c < 2; c++) {
			helper.setBlock(new BlockPos(1 + c, 2, 2), facing(Direction.NORTH));
		}
		BlockPos ctrlAt = helper.absolutePos(new BlockPos(1, 2, 2));
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		owner.setGameMode(GameType.SURVIVAL);
		owner.setPos(ctrlAt.getX() + 0.5, ctrlAt.getY() - 1, ctrlAt.getZ() + 3.5);
		ServerPlayer stranger = helper.makeMockServerPlayerInLevel();
		stranger.setGameMode(GameType.SURVIVAL);
		stranger.setPos(ctrlAt.getX() + 0.5, ctrlAt.getY() - 1, ctrlAt.getZ() + 3.5);
		TvBlockEntity tv = be(helper, new BlockPos(1, 2, 2));
		tv.setPlacer(owner.getUUID());
		helper.assertTrue(owner.getUUID().equals(tv.owner()), "The screen is its placer's");

		JugcraftTv.control(owner, new TvPayloads.Control(ctrlAt, TvPayloads.Control.LINK, "https://example.com/films/flyover.mp4", 0));
		helper.assertTrue(tv.url().equals("https://example.com/films/flyover.mp4") && tv.paused() && tv.autoplay() && tv.position() == 0,
				"A link sets the screen loading at 0");
		JugcraftTv.control(owner, new TvPayloads.Control(ctrlAt, TvPayloads.Control.PLAY));
		helper.assertTrue(!tv.paused() && !tv.autoplay(), "PLAY starts it");
		JugcraftTv.control(owner, new TvPayloads.Control(ctrlAt, TvPayloads.Control.SEEK, "", 65_000));
		helper.assertTrue(tv.paused() && tv.autoplay() && tv.position() == 65_000,
				"A seek while it plays holds it at 1:05 till the seeker's game has the picture there (" + tv.position() + ")");
		JugcraftTv.control(owner, new TvPayloads.Control(ctrlAt, TvPayloads.Control.PLAY));
		helper.assertTrue(!tv.paused() && !tv.autoplay() && Math.abs(tv.position() - 65_000) < 1_000, "Then it plays on from 1:05");
		JugcraftTv.control(owner, new TvPayloads.Control(ctrlAt, TvPayloads.Control.PAUSE));
		helper.assertTrue(tv.paused() && Math.abs(tv.position() - 65_000) < 1_000, "PAUSE holds it there");
		JugcraftTv.control(owner, new TvPayloads.Control(ctrlAt, TvPayloads.Control.SEEK, "", 75_000));
		long held = tv.position();
		helper.assertTrue(tv.paused() && !tv.autoplay() && held == 75_000, "Paused, a seek keeps it paused at the new moment");
		JugcraftTv.control(owner, new TvPayloads.Control(ctrlAt, TvPayloads.Control.VOLUME, "", 40));
		helper.assertTrue(tv.volume() == 40, "The volume is the screen's");
		JugcraftTv.control(owner, new TvPayloads.Control(ctrlAt, TvPayloads.Control.LOOP, "", 1));
		JugcraftTv.control(owner, new TvPayloads.Control(ctrlAt, TvPayloads.Control.PLAYLIST, "", 1));
		helper.assertTrue(tv.loop() && tv.playlist(), "Replay and the whole playlist are the screen's to tick");
		JugcraftTv.control(stranger, new TvPayloads.Control(ctrlAt, TvPayloads.Control.LOOP, "", 0));
		helper.assertTrue(tv.loop(), "Someone outside the owner's party can't untick replay");

		// From well back (a big screen is watched from there), measured to the screen's nearest part; not from across the map.
		BlockPos right = helper.absolutePos(new BlockPos(2, 2, 2));
		owner.setPos(right.getX() + 0.5, right.getY(), right.getZ() + 40.5);
		JugcraftTv.control(owner, new TvPayloads.Control(ctrlAt, TvPayloads.Control.VOLUME, "", 55));
		helper.assertTrue(tv.volume() == 55, "Worked from 40 blocks off");
		helper.assertTrue(JugcraftTv.ask(owner, right), "Its window asked for from 40 blocks off (a right-click past the game's reach)");
		owner.setPos(right.getX() + 0.5, right.getY(), right.getZ() + 70.5);
		JugcraftTv.control(owner, new TvPayloads.Control(ctrlAt, TvPayloads.Control.VOLUME, "", 20));
		helper.assertTrue(tv.volume() == 55, "Not from 70 blocks off");
		helper.assertTrue(!JugcraftTv.ask(owner, right), "Nor its window");
		owner.setPos(ctrlAt.getX() + 0.5, ctrlAt.getY() - 1, ctrlAt.getZ() + 3.5);
		helper.assertTrue(!JugcraftTv.ask(owner, helper.absolutePos(new BlockPos(1, 2, 1))), "A block that isn't a TV opens nothing");

		for (String bad : new String[] {"file:///etc/passwd", "javascript:alert(1)", "https://", "https://exa mple.com/a.mp4", "ftp://example.com/a.mp4"}) {
			JugcraftTv.control(owner, new TvPayloads.Control(ctrlAt, TvPayloads.Control.LINK, bad, 0));
			helper.assertTrue(tv.url().equals("https://example.com/films/flyover.mp4"), "Refused: " + bad);
		}
		JugcraftTv.control(stranger, new TvPayloads.Control(ctrlAt, TvPayloads.Control.STOP));
		helper.assertTrue(!tv.url().isEmpty(), "Someone outside the owner's party can't stop it");
		stranger.setGameMode(GameType.CREATIVE);
		helper.assertTrue(tv.mayControl(stranger), "A creative player may");
		stranger.setGameMode(GameType.SURVIVAL);

		// The bottom-left block taken out: the block beside it is the screen now, still playing the link.
		int count = tv.session();
		helper.setBlock(new BlockPos(1, 2, 2), Blocks.AIR);
		TvBlockEntity left = be(helper, new BlockPos(2, 2, 2));
		helper.assertTrue(left.isController() && left.url().equals("https://example.com/films/flyover.mp4") && owner.getUUID().equals(left.owner()),
				"The link and the owner carry on in the screen that's left");
		helper.assertTrue(left.paused() && Math.abs(left.position() - held) < 1_000 && left.loop() && left.playlist(),
				"Still paused where it was, replay and the playlist ticked");
		helper.assertTrue(count > 0 && left.session() == count, "The same link count (each game's player plays on, not started over)");
		JugcraftTv.control(owner, new TvPayloads.Control(helper.absolutePos(new BlockPos(2, 2, 2)), TvPayloads.Control.STOP));
		helper.assertTrue(left.url().isEmpty(), "STOP turns it off");
		helper.succeed();
	}

	/**
	 * A speaker set down links itself to the nearest screen and stands two blocks tall; a Sync Key set to another screen
	 * (right-click the screen, then the speaker's upper half) links it there, sneak-right-click unlinks it, and a speaker
	 * right-clicked first then a screen links too; its upper half broken takes the lower with it.
	 */
	@GameTest
	public void tvSpeakersLink(GameTestHelper helper) {
		if (TestFocus.skip("tv")) {
			helper.succeed();
			return;
		}
		wall(helper, 1);
		// screen A: two blocks at x 1-2; screen B: one block at x 4 (a gap between: two screens)
		helper.setBlock(new BlockPos(1, 2, 2), facing(Direction.NORTH));
		helper.setBlock(new BlockPos(2, 2, 2), facing(Direction.NORTH));
		helper.setBlock(new BlockPos(4, 2, 2), facing(Direction.NORTH));
		BlockPos a = helper.absolutePos(new BlockPos(1, 2, 2)), b = helper.absolutePos(new BlockPos(4, 2, 2));
		helper.assertTrue(be(helper, new BlockPos(1, 2, 2)).isController() && be(helper, new BlockPos(4, 2, 2)).isController(), "Two screens");
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos at = new BlockPos(1, 1, 5), absAt = helper.absolutePos(at);
		player.setPos(absAt.getX() + 0.5, absAt.getY(), absAt.getZ() + 2.5);
		BlockState lower = JugcraftTv.SPEAKER.defaultBlockState().setValue(TvSpeakerBlock.ROTATION, 8);
		helper.setBlock(at, lower);
		ItemStack item = new ItemStack(JugcraftTv.SPEAKER);
		JugcraftTv.SPEAKER.setPlacedBy(helper.getLevel(), absAt, lower, player, item);
		helper.assertTrue(helper.getBlockState(at.above()).is(JugcraftTv.SPEAKER)
				&& helper.getBlockState(at.above()).getValue(TvSpeakerBlock.HALF) == DoubleBlockHalf.UPPER, "It stands two blocks tall");
		TvSpeakerBlockEntity speaker = helper.getBlockEntity(at, TvSpeakerBlockEntity.class);
		helper.assertTrue(speaker != null && a.equals(speaker.screen()), "Set down, it links itself to the nearest screen (A): " + speaker.screen());

		ItemStack key = new ItemStack(JugcraftTv.SYNC_KEY);
		player.setItemInHand(InteractionHand.MAIN_HAND, key);
		use(player, b);
		helper.assertTrue(key.get(JugcraftTv.SYNC_SCREEN) != null && b.equals(key.get(JugcraftTv.SYNC_SCREEN).pos()), "The key is set to screen B");
		use(player, absAt.above());
		helper.assertTrue(b.equals(speaker.screen()), "Its upper half right-clicked: the speaker plays B now");
		player.setShiftKeyDown(true);
		use(player, absAt);
		helper.assertTrue(speaker.screen() == null, "Sneak + right-click unlinks it");
		player.setShiftKeyDown(false);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftTv.SYNC_KEY));
		use(player, absAt);
		helper.assertTrue(player.getMainHandItem().has(JugcraftTv.SYNC_SPEAKER), "A clear key right-clicked on a speaker holds it");
		use(player, helper.absolutePos(new BlockPos(2, 2, 2)));
		helper.assertTrue(a.equals(speaker.screen()), "Then a screen right-clicked (any block of it): linked to that screen (A)");
		helper.assertTrue(!player.getMainHandItem().has(JugcraftTv.SYNC_SPEAKER) && a.equals(player.getMainHandItem().get(JugcraftTv.SYNC_SCREEN).pos()),
				"The key is then set to that screen for the next speaker");

		helper.destroyBlock(at.above());
		helper.assertTrue(helper.getBlockState(at).isAir() && helper.getBlockState(at.above()).isAir(), "Its upper half broken takes both halves");
		helper.succeed();
	}

	/** The player right-clicks the block at {@code pos} (absolute) with what's in their main hand. */
	private static void use(ServerPlayer player, BlockPos pos) {
		ItemStack stack = player.getMainHandItem();
		stack.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.SOUTH, pos, false)));
	}
}
