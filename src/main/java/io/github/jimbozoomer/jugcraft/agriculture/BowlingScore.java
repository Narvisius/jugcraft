package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.ArrayList;
import java.util.List;

/**
 * Ten-pin scoring for Pumpkin Bowling, for a lane of any number of pins: {@value #FRAMES} frames of up to two rolls; a
 * strike (every pin with the first roll) scores the pins and the next two rolls, a spare (every pin with two) the pins
 * and the next roll; the last frame gives one more roll after a spare, two after a strike. Rolls are how many pins each
 * knocked down.
 */
public final class BowlingScore {
	public static final int FRAMES = 10;

	private BowlingScore() {
	}

	/** The score so far, counting strike and spare bonuses as far as the rolls go. */
	public static int score(List<Integer> rolls, int pins) {
		int score = 0;
		int i = 0;
		int n = rolls.size();
		for (int frame = 0; frame < FRAMES && i < n; frame++) {
			int first = rolls.get(i);
			if (first >= pins) {
				score += pins + at(rolls, i + 1) + at(rolls, i + 2);
				i += 1;
			} else if (i + 1 < n && first + rolls.get(i + 1) >= pins) {
				score += pins + at(rolls, i + 2);
				i += 2;
			} else {
				score += first + at(rolls, i + 1);
				i += 2;
			}
		}
		return score;
	}

	private static int at(List<Integer> rolls, int i) {
		return i < rolls.size() ? rolls.get(i) : 0;
	}

	/** {frame (0 to 9, or 10 when the game is over), roll within it (0, 1 or 2)} for the next roll. */
	public static int[] next(List<Integer> rolls, int pins) {
		int i = 0;
		int n = rolls.size();
		for (int frame = 0; frame < FRAMES - 1; frame++) {
			if (i >= n) {
				return new int[] {frame, 0};
			}
			if (rolls.get(i) >= pins) {
				i += 1;
			} else if (i + 1 >= n) {
				return new int[] {frame, 1};
			} else {
				i += 2;
			}
		}
		int left = n - i;
		if (left < 2) {
			return new int[] {FRAMES - 1, left};
		}
		boolean bonus = rolls.get(i) >= pins || rolls.get(i) + rolls.get(i + 1) >= pins;
		return bonus && left < 3 ? new int[] {FRAMES - 1, 2} : new int[] {FRAMES, 0};
	}

	public static boolean over(List<Integer> rolls, int pins) {
		return next(rolls, pins)[0] >= FRAMES;
	}

	/**
	 * Whether the pins are stood up again after the last roll: when all are down, or the frame is over (after a second
	 * roll in frames one to nine, or when the game ends).
	 */
	public static boolean standPins(List<Integer> rolls, int pins, int standingAfter) {
		if (rolls.isEmpty()) {
			return false;
		}
		if (standingAfter == 0) {
			return true;
		}
		int[] next = next(rolls, pins);
		return next[1] == 0 || next[0] >= FRAMES;
	}

	/**
	 * Each frame's marks so far, as a scoreboard shows them: X for a strike, / for a spare, - for no pins, else the
	 * count; the last frame's bonus rolls too.
	 */
	public static List<String> marks(List<Integer> rolls, int pins) {
		List<String> frames = new ArrayList<>();
		int i = 0;
		int n = rolls.size();
		for (int frame = 0; frame < FRAMES && i < n; frame++) {
			StringBuilder marks = new StringBuilder();
			int standing = pins;
			boolean fresh = true;
			int count = frame < FRAMES - 1 ? (rolls.get(i) >= pins ? 1 : 2) : 3;
			for (int roll = 0; roll < count && i < n; roll++, i++) {
				if (frame == FRAMES - 1 && roll == 2 && marks.indexOf("X") < 0 && marks.indexOf("/") < 0) {
					break;
				}
				int knocked = rolls.get(i);
				marks.append(knocked >= standing ? (fresh ? "X" : "/") : knocked == 0 ? "-" : String.valueOf(knocked));
				fresh = knocked >= standing;
				standing = fresh ? pins : standing - knocked;
			}
			frames.add(marks.toString());
		}
		return frames;
	}
}
