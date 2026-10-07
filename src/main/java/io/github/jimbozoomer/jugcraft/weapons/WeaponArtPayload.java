package io.github.jimbozoomer.jugcraft.weapons;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: the player with this entity id began a phase of their arm's weapon art ({@link WeaponArts}), so
 * every client that sees them plays its animation: phase 0 is the art itself, phase 1 the leap slam's landing.
 */
public record WeaponArtPayload(int entity, int phase) implements CustomPacketPayload {
	public static final Type<WeaponArtPayload> TYPE = new Type<>(Jugcraft.id("weapon_art"));
	public static final StreamCodec<RegistryFriendlyByteBuf, WeaponArtPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, WeaponArtPayload::entity,
			ByteBufCodecs.VAR_INT, WeaponArtPayload::phase,
			WeaponArtPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
