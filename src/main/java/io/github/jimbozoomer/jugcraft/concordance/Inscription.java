package io.github.jimbozoomer.jugcraft.concordance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.concordance.compose.Grammar;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A spell inscribed on an instrument ({@code jugcraft:inscription}): its canonical text, with the Focus and cooldown it
 * compiled to when it was inscribed. Only the text is trusted, and only as text: the server compiles it again, with
 * the current data and the caster's current research, every time it is cast. The two numbers are for the casting HUD
 * and the tooltip, which run on the client without the rules.
 */
public record Inscription(String text, int focus, int cooldown) {
	public Inscription {
		text = text.length() > Grammar.MAX_TEXT ? text.substring(0, Grammar.MAX_TEXT) : text;
		focus = Math.max(0, focus);
		cooldown = Math.max(0, cooldown);
	}

	public static final Codec<Inscription> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("text").forGetter(Inscription::text),
			Codec.INT.optionalFieldOf("focus", 0).forGetter(Inscription::focus),
			Codec.INT.optionalFieldOf("cooldown", 0).forGetter(Inscription::cooldown)
	).apply(instance, Inscription::new));

	public static final StreamCodec<ByteBuf, Inscription> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
}
