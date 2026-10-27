package io.github.jimbozoomer.jugcraft.concordance.alchemy;

import org.jspecify.annotations.Nullable;

/**
 * A workshop product (Concordance Chunk C, C-02, C-P2; data: {@code data/<ns>/concordance/product/<name>.json}): a brew
 * the crucible already makes, named on the stack when it is of this form, carries this status and carries nothing
 * harmful ({@link AlchemyCatalog#product}). Never an item of its own: a stack is a product by what it carries, never by
 * its name (tools/concordance_alchemy.py PRODUCTS).
 *
 * @param id     {@code <ns>:<name>}, stable once released
 * @param form   the form it must be bottled in
 * @param status the status effect it must carry, at any level
 * @param name   the translation key its stack is named with (vanilla's item name)
 * @param codex  the codex entry that explains it ({@code alembic/<entry>})
 */
public record Product(String id, Form form, String status, String name, String codex) {
	/** How a dose is delivered: drunk from a glass bottle, or applied from a bowl. */
	public enum Form {
		DRAUGHT("draught"), SALVE("salve");

		public final String id;

		Form(String id) {
			this.id = id;
		}

		public static @Nullable Form fromId(String id) {
			for (Form form : values()) {
				if (form.id.equals(id)) {
					return form;
				}
			}
			return null;
		}
	}
}
