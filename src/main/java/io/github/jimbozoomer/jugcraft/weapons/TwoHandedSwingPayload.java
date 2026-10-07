package io.github.jimbozoomer.jugcraft.weapons;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the player clicked to swing the two-handed arm in their main hand ({@link TwoHanded}). */
public record TwoHandedSwingPayload() implements CustomPacketPayload {
	public static final TwoHandedSwingPayload INSTANCE = new TwoHandedSwingPayload();
	public static final Type<TwoHandedSwingPayload> TYPE = new Type<>(Jugcraft.id("two_handed_swing"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TwoHandedSwingPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
