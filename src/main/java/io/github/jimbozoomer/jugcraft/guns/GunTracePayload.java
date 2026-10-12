package io.github.jimbozoomer.jugcraft.guns;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

/**
 * Server to client (slice 8D): where the player with this entity id's energy weapon shot went, so every client that sees
 * them, theirs too, draws it from the gun ({@code GunEffects}): a beam to its end, or an arc to each creature it leapt to
 * in turn (or to where it struck, having found none). Only the server knows what a shot met. {@code offHand}: the shot was
 * the gun's in their other hand (slice 10G, two guns at once), so it is drawn from that side.
 */
public record GunTracePayload(int shooter, int kind, List<Vec3> points, boolean offHand) implements CustomPacketPayload {
	public static final int BEAM = 0;
	public static final int ARC = 1;
	/** Most points a trace carries: an arc's first mark and each of its leaps. */
	public static final int MAX_POINTS = 1 + JugcraftGuns.ARC_HOPS;
	public static final Type<GunTracePayload> TYPE = new Type<>(Jugcraft.id("gun_trace"));
	private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> POINT = StreamCodec.composite(
			ByteBufCodecs.DOUBLE, Vec3::x,
			ByteBufCodecs.DOUBLE, Vec3::y,
			ByteBufCodecs.DOUBLE, Vec3::z,
			Vec3::new);
	public static final StreamCodec<RegistryFriendlyByteBuf, GunTracePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, GunTracePayload::shooter,
			ByteBufCodecs.VAR_INT, GunTracePayload::kind,
			POINT.apply(ByteBufCodecs.list(MAX_POINTS)), GunTracePayload::points,
			ByteBufCodecs.BOOL, GunTracePayload::offHand,
			GunTracePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
