package io.github.jimbozoomer.jugcraft.party;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the Party screen opened (or a button was pressed); answer with a {@link PartyStatePayload}. */
public record PartyRequestPayload() implements CustomPacketPayload {
	public static final PartyRequestPayload INSTANCE = new PartyRequestPayload();
	public static final Type<PartyRequestPayload> TYPE = new Type<>(Jugcraft.id("party_request"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PartyRequestPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<PartyRequestPayload> type() {
		return TYPE;
	}
}
