package io.github.jimbozoomer.jugcraft.finance;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A coin pusher. Every Jug dropped lands on the shelf (the pot, held by the Exchange); the moving pusher shoves now and
 * then a share of the shelf over the edge. Nine tenths of what goes over falls in the player's tray and a tenth off the
 * sides to the house, so over time the machine pays back 90% of what goes in. Most drops push nothing over; about one
 * in three pushes a little, one in twenty a lot. Plain Java: the block entity moves the money.
 */
public class CoinPusher {
	public static final int PLAYER_SHARE_PERCENT = 90;

	/** Jugs on the shelf. */
	public long shelf;
	public long drops;
	public long lastWin;
	public long lastSpill;
	public final List<String> log = new ArrayList<>();
	private final Random random;

	public CoinPusher(Random random) {
		this.random = random;
	}

	/** Adds a drop to the shelf and returns how many Jugs go over the edge (the caller splits them). */
	public long drop(long amount) {
		shelf += amount;
		drops++;
		double roll = random.nextDouble();
		double share;
		if (roll < 0.05) {
			share = 0.06 + random.nextDouble() * 0.19;
		} else if (roll < 0.35) {
			share = random.nextDouble() * 0.06;
		} else {
			share = 0;
		}
		long spill = (long) Math.floor(shelf * share);
		shelf -= spill;
		lastSpill = spill;
		return spill;
	}

	/** The player's part of a spill. */
	public static long playerPart(long spill) {
		return spill * PLAYER_SHARE_PERCENT / 100;
	}
}
