import io.github.jimbozoomer.jugcraft.academy.AcademyPuzzle;

/** Run with the compiled pure puzzle class, without a Minecraft installation. */
public class AcademyLogicProbe {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        AcademyPuzzle.Attempt a = null, b = null;
        int[] sequence = {0, 2, 1, 3};
        for (int i = 0; i < 4; i++) {
            var result = AcademyPuzzle.press(a, sequence[i], i * 8L);
            check(result.correct() && result.complete() == (i == 3), "valid sequence");
            a = result.next();
            if (i == 1) b = AcademyPuzzle.press(b, 0, 10).next();
        }
        check(b.step() == 1, "players must not advance one another");
        check(!AcademyPuzzle.press(b, 1, 18).correct(), "wrong order resets");
        check(!AcademyPuzzle.press(b, 2, 211).correct(), "expired sequence");
        check(AcademyPuzzle.press(b, 2, 210).correct(), "timeout boundary");
        check(!AcademyPuzzle.press(b, 2, 9).correct(), "clock rollback resets");
        check(!AcademyPuzzle.press(null, -1, 0).correct(), "invalid rune");
        for (int i = 0; i < 4; i++) check(AcademyPuzzle.shownRune(i * 20) == sequence[i], "visual clue order");
        check(AcademyPuzzle.shownRune(85) == -1, "pause between demonstrations");
        check(io.github.jimbozoomer.jugcraft.academy.AcademyClimate.influence(0, 0, 0) == 1, "courtyard climate");
        check(io.github.jimbozoomer.jugcraft.academy.AcademyClimate.influence(0, 0, 74) == 1, "bridge climate");
        check(io.github.jimbozoomer.jugcraft.academy.AcademyClimate.influence(200, 0, 0) == 0, "outside climate");
        check(io.github.jimbozoomer.jugcraft.academy.AcademyClimate.influence(0, -30, 0) == 0, "underground exclusion");
        double boundary = io.github.jimbozoomer.jugcraft.academy.AcademyClimate.influence(-60, 0, 40);
        check(boundary >= 0 && boundary <= 1, "bounded fade");
        System.out.println("Academy puzzle: sequence, isolation, reset, timeout, rollback and visual clue checks passed");
    }
}
