package io.github.jimbozoomer.jugcraft.concordance.artifice;

import org.jspecify.annotations.Nullable;

/**
 * What an enhancement gives (roadmap step 19): an attribute, by its registry id (vanilla's, or a Spell Power statistic
 * such as {@code spell_power:arcane}), how it applies and by how much. The server turns it into an attribute modifier.
 */
public record Stat(String attribute, Operation operation, double amount) {
	public enum Operation {
		ADD_VALUE("add_value"), ADD_MULTIPLIED_BASE("add_multiplied_base"), ADD_MULTIPLIED_TOTAL("add_multiplied_total");

		public final String id;

		Operation(String id) {
			this.id = id;
		}

		public static @Nullable Operation fromId(String id) {
			for (Operation operation : values()) {
				if (operation.id.equals(id)) {
					return operation;
				}
			}
			return null;
		}
	}
}
