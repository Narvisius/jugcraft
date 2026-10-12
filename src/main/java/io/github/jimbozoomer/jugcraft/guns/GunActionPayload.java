package io.github.jimbozoomer.jugcraft.guns;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;

/**
 * Server to client: the player with this entity id fired, began to reload, stabbed with the gun in their hand or set its
 * barrels spinning, so the clients that see them play its animation (the shooter's own client has already played it).
 * {@code rounds} is how many shells a shell-at-a-time reload loads; {@code offHand}, that it was the gun in their other
 * hand (slice 10G, two guns at once).
 */
public record GunActionPayload(int entity, int action, int rounds, boolean offHand) implements CustomPacketPayload {
	public static final int SHOOT = 0;
	public static final int AIM_SHOOT = 1;
	public static final int RELOAD = 2;
	public static final int STOP = 3;
	/** A bayonet stab (slice 7). */
	public static final int STAB = 4;
	/** The trigger of a gun whose barrels spin up was pulled: they begin to turn (slice 8C). */
	public static final int SPIN = 5;
	public static final Type<GunActionPayload> TYPE = new Type<>(Jugcraft.id("gun_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GunActionPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, GunActionPayload::entity,
			ByteBufCodecs.VAR_INT, GunActionPayload::action,
			ByteBufCodecs.VAR_INT, GunActionPayload::rounds,
			ByteBufCodecs.BOOL, GunActionPayload::offHand,
			GunActionPayload::new);

	/** The gun in the main hand did it. */
	public GunActionPayload(int entity, int action, int rounds) {
		this(entity, action, rounds, false);
	}

	/** The gun in this hand did it. */
	public GunActionPayload(int entity, int action, int rounds, InteractionHand hand) {
		this(entity, action, rounds, hand == InteractionHand.OFF_HAND);
	}

	/** The hand whose gun did it. */
	public InteractionHand hand() {
		return offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
