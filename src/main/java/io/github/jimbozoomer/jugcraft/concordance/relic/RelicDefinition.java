package io.github.jimbozoomer.jugcraft.concordance.relic;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * A relic (roadmap step 20, data: concordance/relic): its item, the charge it holds, whether it binds to the first player
 * it serves, and its modes. Each mode names its own contexts; a relic with no installed mode cannot be installed.
 */
public record RelicDefinition(String id, String item, int capacity, boolean owned, List<Mode> modes) {
	/** The mode that works in {@code context}, or null. */
	public @Nullable Mode mode(Context context) {
		for (Mode mode : modes) {
			if (mode.contexts().contains(context)) {
				return mode;
			}
		}
		return null;
	}

	/** Every context it works in, in the enum's order. */
	public List<Context> contexts() {
		Set<Context> all = EnumSet.noneOf(Context.class);
		for (Mode mode : modes) {
			all.addAll(mode.contexts());
		}
		return new ArrayList<>(all);
	}

	public boolean installable() {
		return mode(Context.INSTALLED) != null;
	}
}
