package io.github.jimbozoomer.jugcraft.concordance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.netty.buffer.ByteBuf;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a sheet of Research Notes holds ({@code jugcraft:research_notes}): who wrote it and, for each research entry they
 * had begun, how far they had come, never beyond understood. Notes are a shared record: another player who reads them
 * gains evidence where the research allows notes ({@link ResearchNotesItem}), but never the author's own observation
 * or mastery. At most {@value #MAX_ENTRIES} entries and a name of at most {@value #MAX_NAME} characters are kept.
 */
public record ResearchNotes(UUID author, String authorName, Map<String, ResearchState> entries, long written) {
	public static final int MAX_ENTRIES = 32;
	public static final int MAX_NAME = 16;

	public ResearchNotes {
		authorName = authorName.length() > MAX_NAME ? authorName.substring(0, MAX_NAME) : authorName;
		TreeMap<String, ResearchState> kept = new TreeMap<>();
		for (Map.Entry<String, ResearchState> entry : new TreeMap<>(entries).entrySet()) {
			if (kept.size() >= MAX_ENTRIES) {
				break;
			}
			ResearchState state = entry.getValue();
			if (state != ResearchState.NONE) {
				kept.put(entry.getKey(), state.atLeast(ResearchState.UNDERSTOOD) ? ResearchState.UNDERSTOOD : state);
			}
		}
		entries = Collections.unmodifiableMap(kept);
	}

	public static final Codec<ResearchNotes> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.CODEC.fieldOf("author").forGetter(ResearchNotes::author),
			Codec.STRING.optionalFieldOf("author_name", "").forGetter(ResearchNotes::authorName),
			Codec.unboundedMap(Codec.STRING, ConcordanceCodecs.STATE).optionalFieldOf("entries", Map.of()).forGetter(ResearchNotes::entries),
			Codec.LONG.optionalFieldOf("written", 0L).forGetter(ResearchNotes::written)
	).apply(instance, ResearchNotes::new));

	public static final StreamCodec<ByteBuf, ResearchNotes> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
}
