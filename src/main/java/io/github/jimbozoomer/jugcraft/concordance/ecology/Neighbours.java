package io.github.jimbozoomer.jugcraft.concordance.ecology;

import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import java.util.List;

/**
 * A mixed bed (Concordance Chunk C, C-08, C-P16, C-P58): the other crops whose company in the area round a plant (the kinds
 * {@link Sampler} finds in the {@code (2 * RADIUS + 1)} square at the plant's height) makes it step {@link #percent} percent
 * more often. Kinds count, never copies: a plant's own kind is never one. The bonus changes only the chance of a step, never
 * what a step costs, so a mixed bed makes no nutrient; a crop that fixes nutrients neither takes nor gives one. Data: a
 * crop's optional {@code "neighbours": {"organisms": [ids], "percent": n}}. Keep {@link #MAX_PERCENT} equal to
 * tools/concordance_ecology.py.
 */
public record Neighbours(List<String> organisms, int percent) {
	/** The most a mixed bed may speed a plant. */
	public static final int MAX_PERCENT = 50;
	/** No mixed bed: what every organism without the field has. */
	public static final Neighbours NONE = new Neighbours(List.of(), 0);

	public Neighbours {
		organisms = List.copyOf(organisms);
		if (organisms.isEmpty() ? percent != 0 : percent < 1 || percent > MAX_PERCENT) {
			throw new IllegalArgumentException("a mixed bed names its neighbours and speeds by 1 to " + MAX_PERCENT + " percent: "
					+ organisms + ", " + percent);
		}
	}

	/** Whether it names any neighbour. */
	public boolean any() {
		return !organisms.isEmpty();
	}

	/** What a mixed bed says: "Mixed bed: +25%" ({@code compose.jugcraft.ecology.mixed}). */
	public Text line() {
		return Text.of("ecology.mixed", percent);
	}
}
