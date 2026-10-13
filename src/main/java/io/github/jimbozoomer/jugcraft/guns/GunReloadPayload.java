package io.github.jimbozoomer.jugcraft.guns;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;

/**
 * Client to server: the player pressed reload with a gun in their main hand, or asked to reload the gun in their other
 * hand (slice 10G, two guns at once; the client picks which) ({@link GunShots#reload}).
 */
public record GunReloadPayload(boolean offHand) implements CustomPacketPayload {
	public static final GunReloadPayload MAIN = new GunReloadPayload(false);
	public static final GunReloadPayload OFF = new GunReloadPayload(true);
	public static final Type<GunReloadPayload> TYPE = new Type<>(Jugcraft.id("gun_reload"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GunReloadPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, GunReloadPayload::offHand,
			offHand -> offHand ? OFF : MAIN);

	/** A reload of the gun in this hand. */
	public static GunReloadPayload of(InteractionHand hand) {
		return hand == InteractionHand.OFF_HAND ? OFF : MAIN;
	}

	/** The hand whose gun is to be reloaded. */
	public InteractionHand hand() {
		return offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
