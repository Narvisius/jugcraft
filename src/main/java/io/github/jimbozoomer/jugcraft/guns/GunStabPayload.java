package io.github.jimbozoomer.jugcraft.guns;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: the player stabbed with the bayonet on the gun in their main hand (the stab key). The server decides
 * whether it lands and what it hits ({@link GunShots#stab}).
 */
public record GunStabPayload() implements CustomPacketPayload {
	public static final GunStabPayload INSTANCE = new GunStabPayload();
	public static final Type<GunStabPayload> TYPE = new Type<>(Jugcraft.id("gun_stab"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GunStabPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
