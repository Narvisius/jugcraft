package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** A spooky firework's spark: its colour (0xRRGGBB), and whether it twinkles as it fades. */
public record SpookySparkOptions(int colour, boolean twinkle) implements ParticleOptions {
	public static final MapCodec<SpookySparkOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.INT.fieldOf("colour").forGetter(SpookySparkOptions::colour),
			Codec.BOOL.optionalFieldOf("twinkle", false).forGetter(SpookySparkOptions::twinkle)).apply(instance, SpookySparkOptions::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, SpookySparkOptions> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT, SpookySparkOptions::colour,
			ByteBufCodecs.BOOL, SpookySparkOptions::twinkle,
			SpookySparkOptions::new);

	@Override
	public ParticleType<SpookySparkOptions> getType() {
		return JugcraftAgriculture.SPOOKY_SPARK;
	}
}
