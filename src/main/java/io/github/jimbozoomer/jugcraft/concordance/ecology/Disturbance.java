package io.github.jimbozoomer.jugcraft.concordance.ecology;

import java.util.List;

/**
 * What a kind of working magic gives off into the habitats round it (data: {@code concordance/disturbance/}): the
 * blocks ({@code jugcraft:ley_pylon}, or {@code #tags}) and how much each gives, from 1 to 15.
 */
public record Disturbance(String id, List<String> blocks, int value) {
	public Disturbance {
		blocks = List.copyOf(blocks);
	}
}
