package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The work on a pair of Knitting Needles (data component {@code jugcraft:knitting}): the project (a {@link Knitwear}), the
 * rows knitted so far, and the sum of their yarns' colours, red, green and blue, so the garment comes out their blend
 * (as leather takes a mix of dyes).
 */
public record KnittingWork(int project, int rows, int red, int green, int blue) {
	public static final KnittingWork NONE = new KnittingWork(0, 0, 0, 0, 0);
	public static final Codec<KnittingWork> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.intRange(0, Knitwear.values().length - 1).optionalFieldOf("project", 0).forGetter(KnittingWork::project),
			Codec.intRange(0, 64).optionalFieldOf("rows", 0).forGetter(KnittingWork::rows),
			Codec.INT.optionalFieldOf("red", 0).forGetter(KnittingWork::red),
			Codec.INT.optionalFieldOf("green", 0).forGetter(KnittingWork::green),
			Codec.INT.optionalFieldOf("blue", 0).forGetter(KnittingWork::blue)).apply(i, KnittingWork::new));
	public static final StreamCodec<ByteBuf, KnittingWork> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

	public Knitwear knitwear() {
		return Knitwear.byIndex(project);
	}

	/** One more row in a yarn of colour {@code rgb}. */
	public KnittingWork row(int rgb) {
		return new KnittingWork(project, rows + 1, red + ((rgb >> 16) & 0xFF), green + ((rgb >> 8) & 0xFF), blue + (rgb & 0xFF));
	}

	/** The blend of the rows' colours (white with none). */
	public int color() {
		if (rows <= 0) {
			return 0xFFFFFF;
		}
		return (red / rows) << 16 | (green / rows) << 8 | (blue / rows);
	}

	/** Whether the project has all its rows. */
	public boolean done() {
		return rows >= knitwear().rows;
	}

	/** The same project, no rows; or the next project. */
	public KnittingWork restart(boolean next) {
		return new KnittingWork(next ? knitwear().next().ordinal() : project, 0, 0, 0, 0);
	}
}
