package io.github.jimbozoomer.jugcraft.tower.crew;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * A right-click on one of a landed drop ship's seats (client to server; Justin's pick, 9 November 2026: "Right-click to
 * sit"): the ship (its entity id) and the seat ({@link CrewShip#SEATS}). The client finds the seat looked at (the ship
 * isn't an entity one can click: its seats are only drawn); the server checks the ship is down, the seat free and near
 * enough ({@link CrewShip#sit}).
 */
public record CrewShipSeatPayload(int ship, int seat) implements CustomPacketPayload {
	public static final Type<CrewShipSeatPayload> TYPE = new Type<>(Jugcraft.id("crew_ship_seat"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CrewShipSeatPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, CrewShipSeatPayload::ship, ByteBufCodecs.VAR_INT, CrewShipSeatPayload::seat, CrewShipSeatPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> {
			if (context.player().level().getEntity(payload.ship()) instanceof CrewShip ship) {
				ship.sit(context.player(), payload.seat());
			}
		});
	}
}
