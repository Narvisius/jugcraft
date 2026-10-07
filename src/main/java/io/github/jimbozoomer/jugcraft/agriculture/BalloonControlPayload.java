package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * A hot-air balloon pilot's two keys (fall addition 29): the burner (jump) and the vent (back). Sent by the pilot's
 * client when they change. The server takes them only from the balloon's pilot (its first rider), for the balloon they
 * are in; anything else is ignored. The server flies the balloon; the keys are all a client says.
 */
public record BalloonControlPayload(boolean burner, boolean vent) implements CustomPacketPayload {
	public static final Type<BalloonControlPayload> TYPE = new Type<>(Jugcraft.id("balloon_control"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BalloonControlPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, BalloonControlPayload::burner, ByteBufCodecs.BOOL, BalloonControlPayload::vent, BalloonControlPayload::new);

	@Override
	public Type<BalloonControlPayload> type() {
		return TYPE;
	}

	static void register() {
		PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> apply(context.player(), payload.burner(), payload.vent()));
	}

	/** Hands the keys to the balloon {@code player} pilots; returns whether they pilot one. */
	public static boolean apply(ServerPlayer player, boolean burner, boolean vent) {
		if (player.getVehicle() instanceof HotAirBalloon balloon && balloon.pilot() == player) {
			balloon.control(burner, vent);
			return true;
		}
		return false;
	}
}
