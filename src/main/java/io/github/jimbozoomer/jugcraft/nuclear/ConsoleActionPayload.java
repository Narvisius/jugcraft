package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * A button on the Reactor Control Console (client to server): set the rods, SCRAM, reset, the protection switch,
 * open or close for refuelling, and the owner's Personal/Party switch. The server checks reach and permission.
 */
public record ConsoleActionPayload(BlockPos console, int action, int value) implements CustomPacketPayload {
	public static final int SET_RODS = 0, SCRAM = 1, RESET = 2, PROTECTION = 3, REFUEL = 4, MODE = 5;
	/** Squared reach from the console (it is a big desk; the screen stays open while you stand at it). */
	public static final double REACH_SQR = 10 * 10;
	public static final Type<ConsoleActionPayload> TYPE = new Type<>(Jugcraft.id("reactor_console_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ConsoleActionPayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, ConsoleActionPayload::console, ByteBufCodecs.VAR_INT, ConsoleActionPayload::action,
			ByteBufCodecs.VAR_INT, ConsoleActionPayload::value, ConsoleActionPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	static void register() {
		PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			BlockPos pos = payload.console();
			if (!player.level().isLoaded(pos) || player.distanceToSqr(Vec3.atCenterOf(pos)) > REACH_SQR
					|| !(player.level().getBlockEntity(pos) instanceof EquipmentBlockEntity console)
					|| console.kind() != EquipmentKind.CONTROL_CONSOLE) {
				return;
			}
			apply(console, player, payload.action(), payload.value());
		});
	}

	/** Carries out a console action for a player (also used by tests). Returns whether it did anything. */
	public static boolean apply(EquipmentBlockEntity console, ServerPlayer player, int action, int value) {
		if (!console.mayUse(player)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.not_owner"));
			return false;
		}
		if (action == MODE) {
			if (console.owner() != null && !console.owner().equals(player.getUUID())) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.owner_only"));
				return false;
			}
			console.toggleMode();
			return true;
		}
		if (console.linked == null || !(player.level().getBlockEntity(console.linked) instanceof EquipmentBlockEntity vessel)
				|| !vessel.kind().reactor()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.console_unlinked"));
			return false;
		}
		ReactorCore core = vessel.core;
		switch (action) {
			case SET_RODS -> core.rodTarget = Math.max(0, Math.min(100, value));
			case SCRAM -> core.scrammed = true;
			case RESET -> {
				if (!core.reset()) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.cannot_reset"));
					return false;
				}
			}
			case PROTECTION -> core.protection = !core.protection;
			case REFUEL -> {
				if (core.refuelOpen) {
					core.refuelOpen = false;
				} else if (core.shutDown() && core.temperature < 120 && core.rodTarget >= 100) {
					core.refuelOpen = true;
				} else {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.refuel_not_shut_down"));
					return false;
				}
			}
			default -> {
				return false;
			}
		}
		vessel.changed();
		return true;
	}
}
