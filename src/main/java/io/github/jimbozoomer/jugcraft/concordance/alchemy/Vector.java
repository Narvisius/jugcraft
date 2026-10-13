package io.github.jimbozoomer.jugcraft.concordance.alchemy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * An amount of each {@link Axis}, in thousandths of a unit (milli-units). Whole numbers and floor division only, so the
 * same additions and the same process always give exactly the same amounts, on every machine, and an amount written
 * down (a formula, a draught) means the same when read back. Immutable.
 */
public final class Vector {
	public static final Vector ZERO = new Vector(new long[Axis.values().length]);
	private final long[] milli;

	private Vector(long[] milli) {
		this.milli = milli;
	}

	/** From milli-units by axis (absent axes are 0; negative amounts are refused). */
	public static Vector of(Map<Axis, Long> amounts) {
		long[] values = new long[Axis.values().length];
		for (Map.Entry<Axis, Long> entry : amounts.entrySet()) {
			if (entry.getValue() < 0) {
				throw new IllegalArgumentException(entry.getKey().id + " is negative");
			}
			values[entry.getKey().ordinal()] = entry.getValue();
		}
		return new Vector(values);
	}

	/** From a list of milli-units in axis order (a saved vector); missing or negative entries read as 0. */
	public static Vector fromList(List<Long> values) {
		long[] out = new long[Axis.values().length];
		for (int i = 0; i < out.length && i < values.size(); i++) {
			out[i] = Math.max(0L, values.get(i));
		}
		return new Vector(out);
	}

	/** The milli-units in axis order, for saving. */
	public List<Long> toList() {
		List<Long> out = new ArrayList<>();
		for (long value : milli) {
			out.add(value);
		}
		return out;
	}

	public long get(Axis axis) {
		return milli[axis.ordinal()];
	}

	public Vector plus(Vector other) {
		long[] out = new long[milli.length];
		for (int i = 0; i < out.length; i++) {
			out[i] = Math.addExact(milli[i], other.milli[i]);
		}
		return new Vector(out);
	}

	/** This less {@code other}, never below 0 on any axis. */
	public Vector minus(Vector other) {
		long[] out = new long[milli.length];
		for (int i = 0; i < out.length; i++) {
			out[i] = Math.max(0L, milli[i] - other.milli[i]);
		}
		return new Vector(out);
	}

	/** Each axis times {@code permille}/1000, rounded down. */
	public Vector scale(long permille) {
		long[] out = new long[milli.length];
		for (int i = 0; i < out.length; i++) {
			out[i] = Math.multiplyExact(milli[i], permille) / 1000L;
		}
		return new Vector(out);
	}

	/** Only the given axes times {@code permille}/1000 (rounded down); the rest unchanged. */
	public Vector scale(long permille, Axis... axes) {
		long[] out = milli.clone();
		for (Axis axis : axes) {
			out[axis.ordinal()] = Math.multiplyExact(out[axis.ordinal()], permille) / 1000L;
		}
		return new Vector(out);
	}

	/** Each axis divided by {@code parts}, rounded down. */
	public Vector divide(int parts) {
		long[] out = new long[milli.length];
		for (int i = 0; i < out.length; i++) {
			out[i] = milli[i] / parts;
		}
		return new Vector(out);
	}

	public boolean isZero() {
		for (long value : milli) {
			if (value != 0) {
				return false;
			}
		}
		return true;
	}

	/** The axis with the most (the first such, in axis order), or null if all are 0. */
	public @Nullable Axis dominant() {
		Axis best = null;
		for (Axis axis : Axis.values()) {
			if (get(axis) > 0 && (best == null || get(axis) > get(best))) {
				best = axis;
			}
		}
		return best;
	}

	public Map<Axis, Long> asMap() {
		Map<Axis, Long> out = new EnumMap<>(Axis.class);
		for (Axis axis : Axis.values()) {
			if (get(axis) != 0) {
				out.put(axis, get(axis));
			}
		}
		return out;
	}

	/** Milli-units as a unit amount with two decimals ("1.36"), for showing players. */
	public static String units(long milli) {
		return String.format(Locale.ROOT, "%.2f", milli / 1000.0);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Vector vector && Arrays.equals(milli, vector.milli);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(milli);
	}

	@Override
	public String toString() {
		StringBuilder out = new StringBuilder("{");
		for (Axis axis : Axis.values()) {
			if (get(axis) != 0) {
				out.append(out.length() > 1 ? ", " : "").append(axis.id).append('=').append(units(get(axis)));
			}
		}
		return out.append('}').toString();
	}
}
