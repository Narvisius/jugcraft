package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What an Aura Candle is made of, kept on the item (data component {@code jugcraft:candle}) and on the placed candle:
 * the wax of its first layer, how many layers it has, the colour of its outermost layer, every scent its layers carry,
 * whether any layer was brightened or extended, how long it burns in all and how long it has burned. A candle carrying
 * more than {@value WaxPotBlockEntity#MAX_SCENTS} scents is muddled: it lights but has no aura.
 */
public record CandleMix(CandleWax wax, int dips, int color, List<CandleScent> scents, boolean bright, boolean lasting, int burn, int burned) {
	public static final Codec<CandleMix> CODEC = RecordCodecBuilder.create(i -> i.group(
			CandleWax.CODEC.fieldOf("wax").forGetter(CandleMix::wax),
			Codec.intRange(1, AuraCandleBlock.MAX_DIPS).fieldOf("dips").forGetter(CandleMix::dips),
			Codec.INT.fieldOf("color").forGetter(CandleMix::color),
			CandleScent.CODEC.listOf().optionalFieldOf("scents", List.of()).forGetter(CandleMix::scents),
			Codec.BOOL.optionalFieldOf("bright", false).forGetter(CandleMix::bright),
			Codec.BOOL.optionalFieldOf("lasting", false).forGetter(CandleMix::lasting),
			Codec.intRange(0, Integer.MAX_VALUE).fieldOf("burn").forGetter(CandleMix::burn),
			Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("burned", 0).forGetter(CandleMix::burned)).apply(i, CandleMix::new));
	public static final StreamCodec<ByteBuf, CandleMix> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

	public CandleMix {
		scents = List.copyOf(scents);
	}

	/** A plain candle of one layer, for one placed without a recipe (by a command, say). */
	public static CandleMix plain(CandleWax wax) {
		return new CandleMix(wax, 1, wax.color, List.of(), false, false, wax.burnPerDip, 0);
	}

	/** How long a layer of {@code wax} burns, brightened or extended. */
	public static int burnFor(CandleWax wax, boolean bright, boolean lasting) {
		float factor = (bright ? WaxPotBlockEntity.BRIGHT_BURN : 1.0F) * (lasting ? WaxPotBlockEntity.LONG_BURN : 1.0F);
		return Math.round(wax.burnPerDip * factor);
	}

	/** A first layer: string dipped in a pot's wax. */
	public static CandleMix first(CandleWax wax, int color, List<CandleScent> scents, boolean bright, boolean lasting) {
		return new CandleMix(wax, 1, color, scents, bright, lasting, burnFor(wax, bright, lasting), 0);
	}

	/** This candle with one more layer of a pot's wax: its colour on the outside, its scents and strength added. */
	public CandleMix dip(CandleWax potWax, int potColor, List<CandleScent> potScents, boolean potBright, boolean potLasting) {
		List<CandleScent> all = new ArrayList<>(scents);
		for (CandleScent scent : potScents) {
			if (!all.contains(scent)) {
				all.add(scent);
			}
		}
		return new CandleMix(wax, dips + 1, potColor, all, bright || potBright, lasting || potLasting, burn + burnFor(potWax, potBright, potLasting),
				burned);
	}

	public boolean muddled() {
		return scents.size() > WaxPotBlockEntity.MAX_SCENTS;
	}

	public int radius() {
		return AuraCandleBlock.RADIUS[dips - 1];
	}

	public int remaining() {
		return Math.max(0, burn - burned);
	}

	public CandleMix withBurned(int burned) {
		return new CandleMix(wax, dips, color, scents, bright, lasting, burn, burned);
	}

	/** "Beeswax Candle of Swiftness and Moonlight", "Tallow Candle", "Muddled Tallow Candle". */
	public Component name() {
		String key = "item.jugcraft.aura_candle.";
		if (muddled()) {
			return Component.translatable(key + "muddled", wax.displayName());
		}
		return switch (scents.size()) {
			case 0 -> Component.translatable(key + "plain", wax.displayName());
			case 1 -> Component.translatable(key + "one", wax.displayName(), scents.get(0).displayName());
			default -> Component.translatable(key + "two", wax.displayName(), scents.get(0).displayName(), scents.get(1).displayName());
		};
	}
}
