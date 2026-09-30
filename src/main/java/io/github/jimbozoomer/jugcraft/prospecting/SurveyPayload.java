package io.github.jimbozoomer.jugcraft.prospecting;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client: the prospector's readings, shown on its screen. */
public record SurveyPayload(List<OreSurvey.Reading> readings) implements CustomPacketPayload {
	public static final Type<SurveyPayload> TYPE = new Type<>(Jugcraft.id("ore_survey"));
	private static final StreamCodec<RegistryFriendlyByteBuf, OreSurvey.Reading> READING = StreamCodec.composite(
			Identifier.STREAM_CODEC, OreSurvey.Reading::icon,
			ByteBufCodecs.VAR_INT, OreSurvey.Reading::signal,
			ByteBufCodecs.VAR_INT, OreSurvey.Reading::depth,
			OreSurvey.Reading::new);
	public static final StreamCodec<RegistryFriendlyByteBuf, SurveyPayload> CODEC = StreamCodec.composite(
			READING.apply(ByteBufCodecs.list(64)), SurveyPayload::readings,
			SurveyPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
