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
public record TerminalModePayload(BlockPos pos, BlockPos accessPos) implements CustomPacketPayload {
	public TerminalModePayload(BlockPos pos) { this(pos, pos); }
	public static final Type<TerminalModePayload> TYPE = new Type<>(Jugcraft.id("drone_terminal_mode"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TerminalModePayload> CODEC =
			StreamCodec.composite(BlockPos.STREAM_CODEC, TerminalModePayload::pos,
					BlockPos.STREAM_CODEC, TerminalModePayload::accessPos, TerminalModePayload::new);

	@Override
	public Type<TerminalModePayload> type() {
		return TYPE;
	}

	static void register() {
		PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> handle(context.player(), payload));
	}

	public static void handle(ServerPlayer player, TerminalModePayload payload) {
		if (io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock.mayAccessTerminal(player, payload.pos(), payload.accessPos())
				&& player.level().getBlockEntity(payload.pos()) instanceof DroneTerminalBlockEntity terminal) {
			terminal.toggleMode(player);
		}
	}
}
