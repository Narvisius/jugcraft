package io.github.jimbozoomer.jugcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * The casino's sounds, played from the game's own block so they come from the machine. These are the player's own
 * sounds, timed to what their screen shows (reels stopping, the ball dropping, the cards turning); other players nearby
 * hear the server's sounds for the same game.
 */
public final class CasinoSound {
	private CasinoSound() {
	}

	public static void play(long pos, SoundEvent sound, float volume, float pitch) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}
		BlockPos at = BlockPos.of(pos);
		mc.level.playLocalSound(at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5, sound, SoundSource.BLOCKS, volume, pitch, false);
	}

	/** A reel or wheel ticking round. */
	public static void tick(long pos, float pitch) {
		play(pos, SoundEvents.NOTE_BLOCK_HAT.value(), 0.12F, pitch);
	}

	/** A reel clunking to a stop. */
	public static void reelStop(long pos, int reel) {
		play(pos, SoundEvents.CHAIN_PLACE, 0.45F, 1.25F + reel * 0.06F);
	}

	/** A lever pulled, a button pressed. */
	public static void lever(long pos) {
		play(pos, SoundEvents.LEVER_CLICK, 0.6F, 0.85F);
	}

	/** Chips clacking onto the felt. */
	public static void chip(long pos) {
		play(pos, SoundEvents.NOTE_BLOCK_HAT.value(), 0.35F, 1.4F);
	}

	/** A card dealt. */
	public static void card(long pos) {
		play(pos, SoundEvents.BOOK_PAGE_TURN, 0.6F, 1.3F);
	}

	/** Dice bouncing. */
	public static void dice(long pos) {
		play(pos, SoundEvents.WOOD_HIT, 0.5F, 1.6F);
	}

	/** A coin landing (the win counting up, a coin dropped). */
	public static void coin(long pos, float pitch) {
		play(pos, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.25F, pitch);
	}

	/** The rising chime while the last reels decide a big win. */
	public static void anticipation(long pos, float pitch) {
		play(pos, SoundEvents.NOTE_BLOCK_CHIME.value(), 0.5F, pitch);
	}

	/** A peg struck by a Plinko ball. */
	public static void peg(long pos, float pitch) {
		play(pos, SoundEvents.NOTE_BLOCK_XYLOPHONE.value(), 0.18F, pitch);
	}
}
