package io.github.jimbozoomer.jugcraft.drone;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sent by the terminal screen's PERSONAL / PARTY button. The server checks that the player is close
 * to that terminal and owns it (the same rule as sneak-using the terminal) before switching.
 */
public record TerminalModePayload(BlockPos pos) implements CustomPacketPayload {
	public static final Type<TerminalModePayload> TYPE = new Type<>(Jugcraft.id("drone_terminal_mode"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TerminalModePayload> CODEC =
			StreamCodec.composite(BlockPos.STREAM_CODEC, TerminalModePayload::pos, TerminalModePayload::new);
	/** The player must be this close to the terminal (squared blocks). */
	private static final double MAX_DISTANCE_SQR = 8 * 8;

	@Override
	public Type<TerminalModePayload> type() {
		return TYPE;
	}

	static void register() {
		PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			BlockPos pos = payload.pos();
			if (player.level().isLoaded(pos) && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) <= MAX_DISTANCE_SQR
					&& player.level().getBlockEntity(pos) instanceof DroneTerminalBlockEntity terminal) {
				terminal.toggleMode(player);
			}
		});
	}
}
