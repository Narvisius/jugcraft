package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What is left in an unsealed jar of preserves (data component {@code jugcraft:jar_contents}): how many servings, and the
 * game time it was cooked or opened, from which it keeps {@value PreserveJarItem#SPOIL_TICKS} ticks. A jar without it is
 * full and fresh (a sealed jar has none: it keeps until it is opened).
 */
public record JarContents(int servings, long made) {
	public static final Codec<JarContents> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.intRange(1, PreserveJarItem.SERVINGS).fieldOf("servings").forGetter(JarContents::servings),
			Codec.LONG.fieldOf("made").forGetter(JarContents::made)).apply(i, JarContents::new));
	public static final StreamCodec<ByteBuf, JarContents> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
}
