package io.github.jimbozoomer.jugcraft.concordance.ritual;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * The shape a ritual needs around its anchor (roadmap step 12), from {@code data/<ns>/concordance/structure/}: parts,
 * each a role at an offset from the anchor block. A pattern is checked exactly as written and never rotated, so one
 * meant to be built facing any way must be symmetric (the Lesser Circle is). At most {@value #MAX_PARTS} parts within
 * {@value #MAX_REACH} blocks of the anchor, which bounds what one check reads.
 *
 * @param anchor the block id of the anchor the pattern is built around
 */
public record StructurePattern(String id, String anchor, List<Part> parts) {
	public static final int MAX_PARTS = 64;
	public static final int MAX_REACH = 8;
	/** Channels are numbered for the linked mask a client draws from, so there are at most 16. */
	public static final int MAX_CHANNELS = 16;

	public enum Role {
		/** Carries Ley Charge into the working: a pylon. Each step draws from every channel. */
		CHANNEL("channel"),
		/** Contains the working: a ward stone. Losing one while a ritual runs lets it loose (backlash). */
		BOUNDARY("boundary"),
		/** Must stay empty (air): the space the working fills. */
		CLEARANCE("clearance");

		public final String id;

		Role(String id) {
			this.id = id;
		}

		public static @Nullable Role fromId(String id) {
			for (Role role : values()) {
				if (role.id.equals(id)) {
					return role;
				}
			}
			return null;
		}
	}

	public record Offset(int x, int y, int z) {
		public int reach() {
			return Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z)));
		}

		@Override
		public String toString() {
			return x + "," + y + "," + z;
		}
	}

	/**
	 * One position in the pattern.
	 *
	 * @param block the block id, or {@code #tag}, the position must hold; null for a clearance, which must be empty
	 */
	public record Part(Role role, Offset offset, @Nullable String block) {
		public Part {
			if ((role == Role.CLEARANCE) != (block == null)) {
				throw new IllegalArgumentException(role.id + " at " + offset + ": a clearance names no block, every other part one");
			}
		}
	}

	public StructurePattern {
		parts = List.copyOf(parts);
		if (parts.isEmpty() || parts.size() > MAX_PARTS) {
			throw new IllegalArgumentException("a structure has 1 to " + MAX_PARTS + " parts");
		}
		Set<Offset> seen = new HashSet<>();
		int channels = 0;
		for (Part part : parts) {
			if (part.offset().reach() == 0 || part.offset().reach() > MAX_REACH) {
				throw new IllegalArgumentException("part at " + part.offset() + " must be 1 to " + MAX_REACH + " blocks from the anchor");
			}
			if (!seen.add(part.offset())) {
				throw new IllegalArgumentException("two parts at " + part.offset());
			}
			if (part.role() == Role.CHANNEL) {
				channels++;
			}
		}
		if (channels > MAX_CHANNELS) {
			throw new IllegalArgumentException("at most " + MAX_CHANNELS + " channels");
		}
	}

	/** The channel parts, in the order the linked mask numbers them. */
	public List<Part> channels() {
		List<Part> out = new ArrayList<>();
		for (Part part : parts) {
			if (part.role() == Role.CHANNEL) {
				out.add(part);
			}
		}
		return out;
	}

	/** The furthest any part is from the anchor, along one axis. */
	public int reach() {
		int reach = 0;
		for (Part part : parts) {
			reach = Math.max(reach, part.offset().reach());
		}
		return reach;
	}
}
