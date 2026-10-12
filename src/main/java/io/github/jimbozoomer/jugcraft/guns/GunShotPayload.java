package io.github.jimbozoomer.jugcraft.guns;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;

/**
 * Client to server: the player pulled the trigger of the gun in their main hand, or (slice 10G, two guns at once) in their
 * other hand (once per shot; a held automatic sends one each time its interval comes round). The server decides whether
 * it fires ({@link GunShots#fire}).
 */
public record GunShotPayload(boolean offHand) implements CustomPacketPayload {
	public static final GunShotPayload MAIN = new GunShotPayload(false);
	public static final GunShotPayload OFF = new GunShotPayload(true);
	public static final Type<GunShotPayload> TYPE = new Type<>(Jugcraft.id("gun_shot"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GunShotPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, GunShotPayload::offHand,
			offHand -> offHand ? OFF : MAIN);

	/** The trigger of the gun in this hand. */
	public static GunShotPayload of(InteractionHand hand) {
		return hand == InteractionHand.OFF_HAND ? OFF : MAIN;
	}

	/** The hand whose gun's trigger was pulled. */
	public InteractionHand hand() {
		return offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
