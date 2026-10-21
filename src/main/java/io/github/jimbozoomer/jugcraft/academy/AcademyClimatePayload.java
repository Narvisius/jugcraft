package io.github.jimbozoomer.jugcraft.academy;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AcademyClimatePayload(String dimension, List<AcademySite> sites) implements CustomPacketPayload {
	public static final Type<AcademyClimatePayload> TYPE = new Type<>(Jugcraft.id("academy_climate"));
	public static final StreamCodec<RegistryFriendlyByteBuf, AcademyClimatePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, AcademyClimatePayload::dimension,
			AcademySite.STREAM.apply(ByteBufCodecs.list(16)), AcademyClimatePayload::sites, AcademyClimatePayload::new);
	public AcademyClimatePayload { sites = List.copyOf(sites); if (sites.size() > 16) throw new IllegalArgumentException("academy sites"); }
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
