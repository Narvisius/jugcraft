package io.github.jimbozoomer.jugcraft.concordance.ecology;

import org.jspecify.annotations.Nullable;

/**
 * The five conditions a habitat is judged by (roadmap step 14), each a whole number from 0 to {@link #max}:
 * <ul>
 * <li>{@link #MOISTURE}: the bed's own wetness, 0 to 7, as farmland's is (water within four blocks, rain, a wet
 * sprinkler or a canteen keep it at 7; growth dries it);</li>
 * <li>{@link #LIGHT}: the server's light at the plant, sky or block, whichever is greater, never the time of day, a
 * dynamic light or a shader;</li>
 * <li>{@link #NUTRIENTS}: what the bed holds, which growth spends and only declared sources refill;</li>
 * <li>{@link #DIVERSITY}: how many different kinds of plant grow round the plant, itself included;</li>
 * <li>{@link #DISTURBANCE}: how much working magic stands round it (data: each disturbing block's value).</li>
 * </ul>
 * Keep the ids and limits equal to tools/concordance_ecology.py.
 */
public enum Factor {
	MOISTURE("moisture", 7),
	LIGHT("light", 15),
	NUTRIENTS("nutrients", Habitat.MAX_NUTRIENTS),
	DIVERSITY("diversity", Sampler.MAX_DIVERSITY),
	DISTURBANCE("disturbance", Sampler.MAX_DISTURBANCE);

	public final String id;
	public final int max;

	Factor(String id, int max) {
		this.id = id;
		this.max = max;
	}

	public static @Nullable Factor fromId(String id) {
		for (Factor factor : values()) {
			if (factor.id.equals(id)) {
				return factor;
			}
		}
		return null;
	}
}
