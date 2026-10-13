package io.github.jimbozoomer.jugcraft.concordance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;

/**
 * The tunings set on an instrument ({@code jugcraft:tunings}, roadmap step 10): for each invocation (by Spell Engine
 * spell id), the one modifier joined to it and the Focus that adds to a cast. Only the modifier's id is trusted, and
 * only as a choice: the server looks the tuned plan up in its own rules every cast, refuses a tuning the invocation
 * does not offer or the instrument cannot hold, and charges the Focus its rules say. The stored Focus is for the casting
 * HUD and the tooltip, which run on the client without the rules. At most {@value #MAX} entries.
 */
public record Tunings(Map<String, Tuning> entries) {
	public static final int MAX = 16;
	public static final Tunings EMPTY = new Tunings(Map.of());

	public record Tuning(String modifier, int focus) {
		public Tuning {
			focus = Math.max(0, focus);
		}

		static final Codec<Tuning> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("modifier").forGetter(Tuning::modifier),
				Codec.INT.optionalFieldOf("focus", 0).forGetter(Tuning::focus)
		).apply(instance, Tuning::new));
	}

	public Tunings {
		Map<String, Tuning> kept = new TreeMap<>();
		for (Map.Entry<String, Tuning> entry : entries.entrySet()) {
			if (kept.size() >= MAX) {
				break;
			}
			kept.put(entry.getKey(), entry.getValue());
		}
		entries = Collections.unmodifiableMap(kept);
	}

	public @Nullable Tuning get(String spell) {
		return entries.get(spell);
	}

	/** These tunings with {@code spell} tuned ({@code tuning} non-null) or untuned. */
	public Tunings with(String spell, @Nullable Tuning tuning) {
		Map<String, Tuning> copy = new LinkedHashMap<>(entries);
		if (tuning == null) {
			copy.remove(spell);
		} else {
			copy.put(spell, tuning);
		}
		return new Tunings(copy);
	}

	public static final Codec<Tunings> CODEC = Codec.unboundedMap(Codec.STRING, Tuning.CODEC).xmap(Tunings::new, Tunings::entries);
	public static final StreamCodec<ByteBuf, Tunings> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
}
