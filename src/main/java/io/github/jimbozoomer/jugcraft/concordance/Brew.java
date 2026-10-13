package io.github.jimbozoomer.jugcraft.concordance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a draught or salve carries ({@code jugcraft:brew}, roadmap step 13): the effects its dose had when it was
 * bottled, and the outcome's canonical key. Set by the server when it bottles; the effects are bounded again by the
 * shared effect limits when they are applied, so even an edited item cannot do more than a bottled one could.
 */
public record Brew(List<Dose> effects, String outcome) {
	/** The most effects one brew carries (one per property and the contaminant). */
	public static final int MAX_EFFECTS = 7;

	public record Dose(String status, int amplifier, int ticks, boolean harmful) {
		static final Codec<Dose> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("status").forGetter(Dose::status),
				Codec.INT.fieldOf("amplifier").forGetter(Dose::amplifier),
				Codec.INT.fieldOf("ticks").forGetter(Dose::ticks),
				Codec.BOOL.optionalFieldOf("harmful", false).forGetter(Dose::harmful)
		).apply(instance, Dose::new));
	}

	public Brew {
		effects = List.copyOf(effects.size() > MAX_EFFECTS ? effects.subList(0, MAX_EFFECTS) : effects);
	}

	public static final Codec<Brew> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Dose.CODEC.listOf().fieldOf("effects").forGetter(Brew::effects),
			Codec.STRING.optionalFieldOf("outcome", "none").forGetter(Brew::outcome)
	).apply(instance, Brew::new));

	public static final StreamCodec<ByteBuf, Brew> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
}
