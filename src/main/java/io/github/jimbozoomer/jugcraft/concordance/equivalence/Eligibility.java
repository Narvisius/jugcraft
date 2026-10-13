package io.github.jimbozoomer.jugcraft.concordance.equivalence;

import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Whether an item may be weighed at all (roadmap step 21). Owning an item, or seeing it in a recipe viewer, never makes
 * it eligible: it must be catalogued, plain, and free of anything that makes it more than matter. In order, a stack is
 * refused if its item is excluded by tag ({@value #EXCLUDED_TAG}: magical things whatever they carry), if it carries
 * magic (any component of Jugcraft, Spell Engine, Spell Power or Trinkets, or {@link #MAGIC}), an inventory
 * ({@link #INVENTORY}), a unique identity ({@link #IDENTITY}), a bound creature or block ({@link #BOUND}), any other change
 * from the plain item ("metadata"), or if it is not catalogued. Pure: the server says what a stack carries.
 */
public final class Eligibility {
	public static final String EXCLUDED_TAG = "jugcraft:equivalence/excluded";
	/** Components that hold other items. */
	public static final Set<String> INVENTORY = Set.of("minecraft:container", "minecraft:bundle_contents", "minecraft:charged_projectiles",
			"minecraft:container_loot");
	/** Components that make one stack unlike any other. */
	public static final Set<String> IDENTITY = Set.of("minecraft:custom_name", "minecraft:written_book_content",
			"minecraft:writable_book_content", "minecraft:map_id", "minecraft:profile", "minecraft:lodestone_tracker", "minecraft:lock");
	/** Components that carry a creature or a block's own state. */
	public static final Set<String> BOUND = Set.of("minecraft:entity_data", "minecraft:bucket_entity_data", "minecraft:bees",
			"minecraft:block_entity_data", "minecraft:block_state");
	/** Vanilla components that carry magic. */
	public static final Set<String> MAGIC = Set.of("minecraft:enchantments", "minecraft:stored_enchantments", "minecraft:potion_contents",
			"minecraft:suspicious_stew_effects", "minecraft:ominous_bottle_amplifier", "minecraft:enchantment_glint_override");
	/** Mods whose components are magic the scale never weighs. */
	public static final Set<String> MAGIC_NAMESPACES = Set.of("jugcraft", "spell_engine", "spell_power", "trinkets");

	/** What the server sees of a stack: its item, the components its stack adds or removes, and its item's tags. */
	public record Specimen(String item, Set<String> patched, Set<String> tags) {
	}

	private Eligibility() {
	}

	/** Why {@code specimen} may not be weighed, or the empty string if it may. */
	public static String check(Specimen specimen, EquivalenceCatalog catalog) {
		if (specimen.tags().contains(EXCLUDED_TAG)) {
			return "excluded";
		}
		for (String component : specimen.patched()) {
			int colon = component.indexOf(':');
			if (MAGIC.contains(component) || colon > 0 && MAGIC_NAMESPACES.contains(component.substring(0, colon))) {
				return "magical";
			}
		}
		if (specimen.patched().stream().anyMatch(INVENTORY::contains)) {
			return "inventory";
		}
		if (specimen.patched().stream().anyMatch(IDENTITY::contains)) {
			return "unique";
		}
		if (specimen.patched().stream().anyMatch(BOUND::contains)) {
			return "bound";
		}
		if (!specimen.patched().isEmpty()) {
			return "metadata";
		}
		return catalog.material(specimen.item()) == null ? "uncatalogued" : "";
	}

	/** Why an item may never be catalogued, judged by the components every one of it carries (null if it may). */
	public static @Nullable String nature(Set<String> defaults) {
		for (String component : defaults) {
			int colon = component.indexOf(':');
			if (MAGIC.contains(component) || colon > 0 && MAGIC_NAMESPACES.contains(component.substring(0, colon))) {
				return "magical";
			}
			if (INVENTORY.contains(component)) {
				return "inventory";
			}
			if (BOUND.contains(component)) {
				return "bound";
			}
		}
		return null;
	}
}
