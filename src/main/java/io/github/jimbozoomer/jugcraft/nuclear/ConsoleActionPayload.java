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
 * The remote shutdown panel sends these too: its reactor trip (SCRAM), its transfer switches (TRANSFER: value 1 takes
 * control from the control room, 0 hands it back) and the owner's switch. While a panel has control the console can
 * only trip the reactor.
 */
public record ConsoleActionPayload(BlockPos console, int action, int value) implements CustomPacketPayload {
	public static final int SET_RODS = 0, SCRAM = 1, RESET = 2, PROTECTION = 3, REFUEL = 4, MODE = 5, TRANSFER = 6;
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
			if (player.isSpectator() || !io.github.jimbozoomer.jugcraft.placement.StructurePermissions.mayChange(player.level(), pos, player) || !player.level().isLoaded(pos) || player.distanceToSqr(Vec3.atCenterOf(pos)) > REACH_SQR
					|| !(player.level().getBlockEntity(pos) instanceof EquipmentBlockEntity console)
					|| console.kind() != EquipmentKind.CONTROL_CONSOLE && console.kind() != EquipmentKind.REMOTE_SHUTDOWN_PANEL) {
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
		boolean panel = console.kind() == EquipmentKind.REMOTE_SHUTDOWN_PANEL;
		if (panel && player.level() instanceof net.minecraft.server.level.ServerLevel server) {
			console.linkShutdownPanel(server);
		}
		if (console.linked == null || !(player.level().getBlockEntity(console.linked) instanceof EquipmentBlockEntity vessel)
				|| !vessel.kind().reactor()) {
			player.sendOverlayMessage(Component.translatable(panel ? "message.jugcraft.nuclear.panel_no_reactor"
					: "message.jugcraft.nuclear.console_unlinked", EquipmentBlockEntity.PANEL_RANGE));
			return false;
		}
		ReactorCore core = vessel.core;
		if (panel) {
			// The panel finds its reactor by itself, so it may only touch one its user may run.
			if (!vessel.mayUse(player)) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.not_owner"));
				return false;
			}
			switch (action) {
				case SCRAM -> {
					core.scrammed = true;
					player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.panel_tripped"));
				}
				case TRANSFER -> {
					core.remote = value != 0;
					player.sendOverlayMessage(Component.translatable(core.remote ? "message.jugcraft.nuclear.panel_control_here"
							: "message.jugcraft.nuclear.panel_control_returned"));
				}
				default -> {
					return false;
				}
			}
			vessel.changed();
			console.linkShutdownPanel((net.minecraft.server.level.ServerLevel) player.level());
			return true;
		}
		if (core.remote && action != SCRAM) {
			// The transfer switches are turned at a remote shutdown panel: the control room has lost the reactor's controls.
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.control_at_panel"));
			return false;
		}
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
