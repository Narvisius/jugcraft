package io.github.jimbozoomer.jugcraft.academy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** A bounded authored footprint, in the dimension owning its SavedData. Facing: N/E/S/W = 0/1/2/3. */
public record AcademySite(BlockPos anchor, int facing, boolean reflected) {
	public static final Codec<AcademySite> CODEC = RecordCodecBuilder.create(i -> i.group(
			BlockPos.CODEC.fieldOf("anchor").forGetter(AcademySite::anchor),
			Codec.intRange(0, 3).fieldOf("facing").forGetter(AcademySite::facing),
			Codec.BOOL.fieldOf("reflected").forGetter(AcademySite::reflected)).apply(i, AcademySite::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, AcademySite> STREAM = StreamCodec.composite(
			BlockPos.STREAM_CODEC, AcademySite::anchor, ByteBufCodecs.VAR_INT, AcademySite::facing,
			ByteBufCodecs.BOOL, AcademySite::reflected, AcademySite::new);
	public AcademySite { anchor = anchor.immutable(); if (facing < 0 || facing > 3) throw new IllegalArgumentException("academy facing"); }
	public double influence(double x, double y, double z) {
		double dx = x-anchor.getX()-0.5, dz = z-anchor.getZ()-0.5;
		double localX = switch(facing) { case 1 -> dz; case 2 -> -dx; case 3 -> -dz; default -> dx; };
		double localZ = switch(facing) { case 1 -> -dx; case 2 -> -dz; case 3 -> dx; default -> dz; };
		if (reflected) localX = -localX;
		return AcademyClimate.influence(localX, y-anchor.getY(), localZ);
	}
}
