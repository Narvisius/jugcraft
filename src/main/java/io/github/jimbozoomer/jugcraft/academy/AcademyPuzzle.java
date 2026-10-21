package io.github.jimbozoomer.jugcraft.academy;

/** S01's server-owned sequence. An attempt expires after ten seconds between presses. */
public final class AcademyPuzzle {
	public static final int TIMEOUT = 200;
	private static final int[] SEQUENCE = {0, 2, 1, 3};
	public record Attempt(int step, long tick) {}
	public record Result(Attempt next, boolean correct, boolean complete) {}
	private AcademyPuzzle() {}
	public static int shownRune(long tick) {
		int phase = Math.floorMod(tick, 120);
		return phase < 80 && phase % 20 < 12 ? SEQUENCE[phase / 20] : -1;
	}
	public static Result press(Attempt previous, int rune, long now) {
		int step = previous == null || now < previous.tick() || now - previous.tick() > TIMEOUT
				? 0 : previous.step();
		if (step < 0 || step >= SEQUENCE.length) step = 0;
		boolean correct = rune >= 0 && rune < 4 && SEQUENCE[step] == rune;
		int next = correct ? step + 1 : 0;
		return new Result(new Attempt(next == 4 ? 0 : next, now), correct, next == 4);
	}
}
