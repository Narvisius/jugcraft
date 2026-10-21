package io.github.jimbozoomer.jugcraft.finance;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: a scratch card was used. Its kind, what it paid (already paid) and its nine panels. */
public record ScratchPayload(int card, long prize, List<Long> panels) implements CustomPacketPayload {
	public static final Type<ScratchPayload> TYPE = new Type<>(Jugcraft.id("scratch_card"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ScratchPayload> CODEC = StreamCodec.ofMember(
			ScratchPayload::write, ScratchPayload::read);

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeVarInt(card);
		buf.writeVarLong(prize);
		buf.writeVarInt(Math.min(9, panels.size()));
		for (int i = 0; i < Math.min(9, panels.size()); i++) {
			buf.writeVarLong(panels.get(i));
		}
	}

	private static ScratchPayload read(RegistryFriendlyByteBuf buf) {
		int card = buf.readVarInt();
		long prize = buf.readVarLong();
		int n = Math.min(9, buf.readVarInt());
		List<Long> panels = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			panels.add(buf.readVarLong());
		}
		return new ScratchPayload(card, prize, panels);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
