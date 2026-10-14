package io.github.jimbozoomer.jugcraft.nuclear;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A player's radiation: the dose taken so far (mSv; it slowly wears off, and death clears it) and the dose rate where
 * they stand (mSv/h, game hours). Saved with the player and synced to them for the dose readout.
 */
public record RadiationState(double dose, double rate) {
	public static final RadiationState NONE = new RadiationState(0, 0);
	public static final Codec<RadiationState> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.DOUBLE.fieldOf("dose").forGetter(RadiationState::dose),
			Codec.DOUBLE.optionalFieldOf("rate", 0.0).forGetter(RadiationState::rate)).apply(i, RadiationState::new));
	public static final StreamCodec<ByteBuf, RadiationState> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.DOUBLE, RadiationState::dose, ByteBufCodecs.DOUBLE, RadiationState::rate, RadiationState::new);
}
