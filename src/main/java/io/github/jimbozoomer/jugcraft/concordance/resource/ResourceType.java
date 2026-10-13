package io.github.jimbozoomer.jugcraft.concordance.resource;

import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * One exact resource type: a kind, and for essences the Principle ({@code essence/radiance}). Two types are the same
 * only if both parts are: Radiance essence and Ember essence are as different as Focus and Vitae.
 */
public record ResourceType(ResourceKind kind, @Nullable String principle) {
	/** The ten Principles. Keep equal to PRINCIPLES in tools/concordance.py. */
	public static final Set<String> PRINCIPLES = Set.of("radiance", "ember", "rime", "tempest", "strata", "verdance", "tide",
			"tether", "echo", "hollow");

	public static final ResourceType FOCUS = new ResourceType(ResourceKind.FOCUS, null);
	public static final ResourceType RADIANCE = essence("radiance");

	public ResourceType {
		if (kind.qualified != (principle != null)) {
			throw new IllegalArgumentException(kind.id + (kind.qualified ? " needs a Principle" : " takes no Principle"));
		}
		if (principle != null && !PRINCIPLES.contains(principle)) {
			throw new IllegalArgumentException("unknown Principle " + principle);
		}
	}

	public static ResourceType of(ResourceKind kind) {
		return new ResourceType(kind, null);
	}

	public static ResourceType essence(String principle) {
		return new ResourceType(ResourceKind.ESSENCE, principle);
	}

	/** {@code focus}, {@code ley_charge}, {@code essence/radiance} ... */
	public String id() {
		return principle == null ? kind.id : kind.id + "/" + principle;
	}

	/** The type an id names, or null when it names none. */
	public static @Nullable ResourceType parse(String id) {
		int slash = id.indexOf('/');
		ResourceKind kind = ResourceKind.fromId(slash < 0 ? id : id.substring(0, slash));
		String principle = slash < 0 ? null : id.substring(slash + 1);
		if (kind == null || kind.qualified != (principle != null) || principle != null && !PRINCIPLES.contains(principle)) {
			return null;
		}
		return new ResourceType(kind, principle);
	}

	@Override
	public String toString() {
		return id();
	}
}
