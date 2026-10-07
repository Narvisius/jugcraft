package io.github.jimbozoomer.jugcraft.season;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: today's season day (see {@link SeasonCalendar}; 0 means off), for the seasonal colours, and
 * whether winter snow is falling (rain then falls as snow in biomes tagged {@code #jugcraft:has_winter_snow}).
 */
public record SeasonPayload(int day, boolean snowing) implements CustomPacketPayload {
	public static final Type<SeasonPayload> TYPE = new Type<>(Jugcraft.id("season"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SeasonPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SeasonPayload::day,
			ByteBufCodecs.BOOL, SeasonPayload::snowing,
			SeasonPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
