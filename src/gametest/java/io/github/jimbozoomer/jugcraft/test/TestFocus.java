package io.github.jimbozoomer.jugcraft.test;

import java.util.List;

/**
 * Lets a test run cover only the feature being worked on: with the environment variable
 * {@code JUGCRAFT_TEST_FOCUS} set (a comma-separated list of machines, petro, party, drones, tower, blueprints,
 * powerline, nuclear), the tests of every other feature pass at once instead of running. Unset, everything runs.
 */
public final class TestFocus {
	private TestFocus() {
	}

	public static boolean skip(String feature) {
		String focus = System.getenv("JUGCRAFT_TEST_FOCUS");
		return focus != null && !focus.isBlank() && !List.of(focus.toLowerCase().split("\\s*,\\s*")).contains(feature);
	}
}
