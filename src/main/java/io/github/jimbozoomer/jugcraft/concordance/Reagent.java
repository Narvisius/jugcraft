package io.github.jimbozoomer.jugcraft.concordance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A prepared ingredient ({@code jugcraft:reagent}, roadmap step 13): which ingredient (by item id) and how it was
 * prepared (a preparation id). Only a choice: the crucible looks both up in its own rules when it goes in.
 */
public record Reagent(String item, String preparation) {
	public static final Codec<Reagent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("item").forGetter(Reagent::item),
			Codec.STRING.fieldOf("preparation").forGetter(Reagent::preparation)
	).apply(instance, Reagent::new));

	public static final StreamCodec<ByteBuf, Reagent> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
}
