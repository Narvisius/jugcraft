package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A batch of candy poured onto a Candy Tray (data component {@code jugcraft:candy_batch}): what it sets into, its
 * flavours, its colour (a colour a layer for candy corn; {@link Candies#NATURAL} where nothing coloured it), how many
 * pieces it breaks into, when it was poured (game time) and, for taffy, how many times it has been pulled.
 */
public record CandyBatch(CandyKind kind, List<CandyFlavour> flavours, List<Integer> colors, int pieces, long poured, int pulls) {
	public static final Codec<CandyBatch> CODEC = RecordCodecBuilder.create(i -> i.group(
			CandyKind.CODEC.fieldOf("kind").forGetter(CandyBatch::kind),
			CandyFlavour.CODEC.listOf().optionalFieldOf("flavours", List.of()).forGetter(CandyBatch::flavours),
			Codec.INT.listOf().optionalFieldOf("colors", List.of()).forGetter(CandyBatch::colors),
			Codec.intRange(1, 64).fieldOf("pieces").forGetter(CandyBatch::pieces),
			Codec.LONG.fieldOf("poured").forGetter(CandyBatch::poured),
			Codec.intRange(0, CandyTrayItem.PULLS).optionalFieldOf("pulls", 0).forGetter(CandyBatch::pulls)).apply(i, CandyBatch::new));
	public static final StreamCodec<ByteBuf, CandyBatch> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

	public CandyBatch {
		flavours = List.copyOf(flavours);
		colors = List.copyOf(colors);
	}

	/** Ticks after pouring until it can be broken into pieces: a day for rock candy to grow, a few seconds for the rest. */
	public int setTicks() {
		return kind == CandyKind.ROCK_CANDY ? CandyTrayItem.CRYSTAL_TICKS : CandyTrayItem.SET_TICKS;
	}

	/** Ticks left until it has set, at game time {@code now}. */
	public long left(long now) {
		return Math.max(0, poured + setTicks() - now);
	}

	/** Whether it is still warm enough to pull (taffy) or to take another layer (candy corn). */
	public boolean warm(long now) {
		return now < poured + CandyTrayItem.WARM_TICKS;
	}

	public boolean pulled() {
		return pulls >= CandyTrayItem.PULLS;
	}

	/** Taffy not yet fully pulled while it is warm. */
	public boolean pullable(long now) {
		return kind == CandyKind.SALT_WATER_TAFFY && !pulled() && warm(now);
	}

	/** What it breaks into at {@code now}: taffy left to go cold unpulled sets hard, as hard candy. */
	public CandyKind sets(long now) {
		return kind == CandyKind.SALT_WATER_TAFFY && !pulled() && !warm(now) ? CandyKind.HARD_CANDY : kind;
	}

	/** Whether it can be broken into pieces at {@code now}. */
	public boolean ready(long now) {
		if (kind == CandyKind.SALT_WATER_TAFFY) {
			return pulled() || !warm(now);
		}
		return left(now) == 0;
	}

	public CandyBatch pull() {
		return new CandyBatch(kind, flavours, colors, pieces, poured, Math.min(CandyTrayItem.PULLS, pulls + 1));
	}

	/** Candy corn with another layer poured on: its colour, pieces and flavours added; it sets again from now. */
	public CandyBatch layer(List<CandyFlavour> more, int color, int morePieces, long now) {
		List<CandyFlavour> all = new ArrayList<>(flavours);
		for (CandyFlavour flavour : more) {
			if (!all.contains(flavour)) {
				all.add(flavour);
			}
		}
		List<Integer> layers = new ArrayList<>(colors);
		layers.add(color);
		return new CandyBatch(kind, all, layers, Math.min(64, pieces + morePieces), now, pulls);
	}

	/** Its flavours if it were layered with {@code more}. */
	public int flavoursWith(List<CandyFlavour> more) {
		return (int) java.util.stream.Stream.concat(flavours.stream(), more.stream()).distinct().count();
	}
}
