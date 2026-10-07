package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The cider in a Cider Barrel, kept on the barrel's item when it is broken (data component {@code jugcraft:barrel_cider}):
 * how many servings, and the game time its batch started ageing. Ageing is counted from that time, so it goes on while the
 * barrel is carried, or its chunk is unloaded.
 */
public record BarrelCider(int servings, long started) {
	public static final Codec<BarrelCider> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.intRange(1, CiderBarrelBlockEntity.CAPACITY).fieldOf("servings").forGetter(BarrelCider::servings),
			Codec.LONG.fieldOf("started").forGetter(BarrelCider::started)).apply(i, BarrelCider::new));
	public static final StreamCodec<ByteBuf, BarrelCider> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
}
