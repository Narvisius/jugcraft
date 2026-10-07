package io.github.jimbozoomer.jugcraft.biome;

import java.util.function.Supplier;

/** Isolates a viewer's vanilla-only registry bootstrap from actual data-pack world generation. */
public final class BiomeBootstrapScope {
	private static final ThreadLocal<Boolean> VANILLA_ONLY = ThreadLocal.withInitial(() -> false);

	private BiomeBootstrapScope() {
	}

	public static boolean isVanillaOnly() {
		return VANILLA_ONLY.get();
	}

	public static <T> T vanillaOnly(Supplier<T> bootstrap) {
		boolean previous = VANILLA_ONLY.get();
		VANILLA_ONLY.set(true);
		try {
			return bootstrap.get();
		} finally {
			if (previous) VANILLA_ONLY.set(true);
			else VANILLA_ONLY.remove();
		}
	}
}
