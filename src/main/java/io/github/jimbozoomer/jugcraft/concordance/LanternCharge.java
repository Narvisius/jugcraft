package io.github.jimbozoomer.jugcraft.concordance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The Radiance in a Kindled Lantern ({@code jugcraft:radiance}): {@code stored} measures as of game time
 * {@code since}. While the lantern is lit it burns one measure every {@link KindledLanternItem#BURN_TICKS} from
 * {@code since}, worked out when it is read, so the item only changes when it is lit, put out, recharged or burns out.
 */
public record LanternCharge(int stored, long since) {
	public static final LanternCharge EMPTY = new LanternCharge(0, 0L);

	public static final Codec<LanternCharge> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("stored").forGetter(LanternCharge::stored),
			Codec.LONG.optionalFieldOf("since", 0L).forGetter(LanternCharge::since)
	).apply(instance, LanternCharge::new));

	public static final StreamCodec<ByteBuf, LanternCharge> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, LanternCharge::stored,
			ByteBufCodecs.VAR_LONG, LanternCharge::since,
			LanternCharge::new);

	public LanternCharge {
		stored = Math.clamp(stored, 0, KindledLanternItem.CAPACITY);
	}

	/** The measures left at {@code now}: all of them if unlit, fewer as a lit lantern burns. */
	public int remaining(long now, boolean lit) {
		if (!lit || now <= since) {
			return stored;
		}
		long burnt = (now - since) / KindledLanternItem.BURN_TICKS;
		return (int) Math.max(0L, stored - burnt);
	}

	/**
	 * The measures kept if it stops burning at {@code now} (put out): a measure begun is a measure spent (roadmap step
	 * 29), so putting a lantern out and lighting it again never burns for nothing.
	 */
	public int kept(long now, boolean lit) {
		if (!lit || now <= since) {
			return stored;
		}
		long begun = Math.ceilDiv(now - since, (long) KindledLanternItem.BURN_TICKS);
		return (int) Math.max(0L, stored - begun);
	}

	/**
	 * The charge fixed at {@code now}: the burnt measures taken off and the clock moved to the start of the measure
	 * now burning, so lighting, putting out and recharging never lose or gain part of a measure.
	 */
	public LanternCharge settle(long now, boolean lit) {
		if (!lit || now <= since) {
			return new LanternCharge(stored, now);
		}
		long burnt = (now - since) / KindledLanternItem.BURN_TICKS;
		return new LanternCharge((int) Math.max(0L, stored - burnt), since + burnt * KindledLanternItem.BURN_TICKS);
	}
}
