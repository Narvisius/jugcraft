package io.github.jimbozoomer.jugcraft.concordance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import io.github.jimbozoomer.jugcraft.concordance.rules.Knowledge;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * How the Concordance's per-player state is saved and sent. Every record carries a {@code schema} so a later version
 * can migrate it; an unknown research state from a newer version reads as not started rather than failing the whole
 * player file. The rules package stays free of Minecraft types, so the codecs live here.
 */
public final class ConcordanceCodecs {
	private ConcordanceCodecs() {
	}

	public static final Codec<ResearchState> STATE = Codec.STRING.xmap(ConcordanceCodecs::state, ResearchState::id);

	private static ResearchState state(String id) {
		ResearchState state = ResearchState.fromId(id.toLowerCase(Locale.ROOT));
		return state == null ? ResearchState.NONE : state;
	}

	public static final Codec<Knowledge.Progress> PROGRESS = RecordCodecBuilder.create(instance -> instance.group(
			STATE.fieldOf("state").forGetter(Knowledge.Progress::state),
			Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("evidence", Map.of()).forGetter(Knowledge.Progress::evidence)
	).apply(instance, Knowledge.Progress::new));

	public static final Codec<Knowledge> KNOWLEDGE = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.optionalFieldOf("schema", Knowledge.SCHEMA).forGetter(Knowledge::schema),
			Codec.unboundedMap(Codec.STRING, PROGRESS).optionalFieldOf("research", Map.of()).forGetter(Knowledge::entries),
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("invocations", Map.of()).forGetter(Knowledge::invocations)
	).apply(instance, (schema, entries, invocations) -> new Knowledge(Knowledge.SCHEMA, entries, invocations)));

	/** Sent only to the player it belongs to (the attachment syncs to its target alone). */
	public static final StreamCodec<ByteBuf, Knowledge> KNOWLEDGE_STREAM = ByteBufCodecs.fromCodec(KNOWLEDGE);

	public static final Codec<FocusPool> FOCUS = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.optionalFieldOf("schema", FocusPool.SCHEMA).forGetter(pool -> FocusPool.SCHEMA),
			Codec.INT.fieldOf("stored").forGetter(FocusPool::stored),
			Codec.LONG.fieldOf("stamp").forGetter(FocusPool::stamp)
	).apply(instance, (schema, stored, stamp) -> new FocusPool(stored, stamp)));

	public static final StreamCodec<ByteBuf, FocusPool> FOCUS_STREAM = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, FocusPool::stored,
			ByteBufCodecs.VAR_LONG, FocusPool::stamp,
			FocusPool::new);
}
