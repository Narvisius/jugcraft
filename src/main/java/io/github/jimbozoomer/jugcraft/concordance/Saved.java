package io.github.jimbozoomer.jugcraft.concordance;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.slf4j.Logger;

/**
 * How the Concordance keeps what it saves in a world readable across versions (roadmap step 30).
 * <ul>
 * <li>{@link #versioned}: every saved format is written as {@code {"version": N, "data": ...}}. A save from before
 * versions (the bare data) reads as version 0; each older version is brought forward by its upgrade steps in order
 * before it is read, so a renamed identifier or a changed shape is migrated rather than lost. A save from a newer
 * version is read as far as this one understands it (fields it does not know are ignored), with a warning.</li>
 * <li>{@link #keeping}: a map whose entries are independent (a spire, a player's claims, a bound will) reads each entry
 * on its own. An entry that cannot be read (written by a newer version, or broken) is kept exactly as it was and
 * written back unchanged, so one bad entry never empties the whole record and nothing is destroyed by reading it.</li>
 * <li>{@link #stamp}: a block entity's or creature's save carries its format's version under {@value #VERSION}; its load
 * reads every field with a default already, and a later format reads {@link #version} to migrate an older save.</li>
 * </ul>
 */
public final class Saved {
	private static final Logger LOGGER = LogUtils.getLogger();
	/** The key a block entity's or creature's save carries its format's version under. */
	public static final String VERSION = "version";

	private Saved() {
	}

	/** Stamps a block entity's or creature's save with its format's {@code version}. */
	public static void stamp(ValueOutput output, int version) {
		output.putInt(VERSION, version);
	}

	/** The version a block entity's or creature's save was written as: 0 for one from before versions. */
	public static int version(ValueInput input) {
		return input.getIntOr(VERSION, 0);
	}

	/** A map read entry by entry: the entries understood, and those kept as written because they could not be read. */
	public record Kept<K, V>(Map<K, V> read, Map<String, Dynamic<?>> unread) {
		public Kept {
			read = new LinkedHashMap<>(read);
			unread = new LinkedHashMap<>(unread);
		}

		public static <K, V> Kept<K, V> of(Map<K, V> read) {
			return new Kept<>(read, Map.of());
		}
	}

	/**
	 * {@code body} written as version {@code current}, read from any older version through {@code upgrades} (the
	 * {@code i}th takes version {@code i}'s data to version {@code i + 1}; version 0 is the bare data saved before
	 * versions). {@code name} names the format in warnings.
	 */
	public static <A> Codec<A> versioned(String name, int current, Codec<A> body, List<UnaryOperator<Dynamic<?>>> upgrades) {
		if (upgrades.size() != current) {
			throw new IllegalArgumentException(name + ": version " + current + " needs " + current + " upgrade steps, not " + upgrades.size());
		}
		return new Codec<>() {
			@Override
			public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
				int version = 0;
				T data = input;
				Optional<MapLike<T>> map = ops.getMap(input).result();
				if (map.isPresent() && map.get().get("version") != null && map.get().get("data") != null) {
					version = ops.getNumberValue(map.get().get("version")).result().map(Number::intValue).orElse(0);
					data = map.get().get("data");
				}
				if (version > current) {
					LOGGER.warn("Jugcraft {}: saved by a newer version ({}, this reads {}); reading what it understands", name, version, current);
				}
				Dynamic<T> dynamic = new Dynamic<>(ops, data);
				for (int step = Math.max(0, version); step < current; step++) {
					dynamic = upgrades.get(step).apply(dynamic).convert(ops);
				}
				return body.decode(ops, dynamic.getValue()).map(pair -> Pair.of(pair.getFirst(), input));
			}

			@Override
			public <T> DataResult<T> encode(A value, DynamicOps<T> ops, T prefix) {
				return body.encodeStart(ops, value).map(data -> ops.createMap(Map.of(ops.createString("version"), ops.createInt(current),
						ops.createString("data"), data)));
			}
		};
	}

	/** As {@link #versioned(String, int, Codec, List)} for a format that has not changed since versions began (version 1). */
	public static <A> Codec<A> versioned(String name, Codec<A> body) {
		return versioned(name, 1, body, List.of(UnaryOperator.identity()));
	}

	/**
	 * A map read entry by entry (see {@link Kept}): an entry whose key or value cannot be read is kept as written, keyed
	 * by its key as written, logged, and written back unchanged.
	 */
	public static <K, V> Codec<Kept<K, V>> keeping(String name, Codec<K> key, Codec<V> value) {
		return new Codec<>() {
			@Override
			public <T> DataResult<Pair<Kept<K, V>, T>> decode(DynamicOps<T> ops, T input) {
				DataResult<MapLike<T>> map = ops.getMap(input);
				if (map.result().isEmpty()) {
					return map.map(unused -> Pair.of(Kept.<K, V>of(Map.of()), input));
				}
				Map<K, V> read = new LinkedHashMap<>();
				Map<String, Dynamic<?>> unread = new LinkedHashMap<>();
				map.result().get().entries().forEach(entry -> {
					String raw = ops.getStringValue(entry.getFirst()).result().orElse(null);
					if (raw == null) {
						return;
					}
					DataResult<K> decodedKey = key.parse(ops, entry.getFirst());
					DataResult<V> decoded = value.parse(ops, entry.getSecond());
					if (decodedKey.result().isPresent() && decoded.result().isPresent()) {
						read.put(decodedKey.result().get(), decoded.result().get());
					} else {
						LOGGER.warn("Jugcraft {}: kept entry {} as it was written, since it cannot be read: {}", name, raw,
								decoded.error().map(error -> error.message()).orElse("its key"));
						unread.put(raw, new Dynamic<>(ops, entry.getSecond()));
					}
				});
				return DataResult.success(Pair.of(new Kept<>(read, unread), input));
			}

			@Override
			public <T> DataResult<T> encode(Kept<K, V> kept, DynamicOps<T> ops, T prefix) {
				Map<T, T> out = new LinkedHashMap<>();
				for (Map.Entry<String, Dynamic<?>> entry : kept.unread().entrySet()) {
					out.put(ops.createString(entry.getKey()), entry.getValue().convert(ops).getValue());
				}
				for (Map.Entry<K, V> entry : kept.read().entrySet()) {
					DataResult<T> encodedKey = key.encodeStart(ops, entry.getKey());
					DataResult<T> encoded = value.encodeStart(ops, entry.getValue());
					if (encodedKey.result().isEmpty() || encoded.result().isEmpty()) {
						return DataResult.error(() -> name + ": cannot write entry " + entry.getKey());
					}
					out.put(encodedKey.result().get(), encoded.result().get());
				}
				return DataResult.success(ops.createMap(out));
			}
		};
	}
}
