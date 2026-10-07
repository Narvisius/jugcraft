package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The epitaph cut into a headstone: at most {@value #LINES} lines of at most {@value #LINE_LENGTH} characters each,
 * control characters dropped and spaces trimmed, without blank lines at the end. It is kept on the headstone's block
 * entity and, when the headstone is broken, on its item (data component {@code jugcraft:epitaph}).
 */
public record Epitaph(List<String> lines) {
	public static final int LINES = 4;
	public static final int LINE_LENGTH = 24;
	public static final Epitaph BLANK = new Epitaph(List.of());
	public static final Codec<Epitaph> CODEC = Codec.STRING.listOf().xmap(Epitaph::of, Epitaph::lines);
	/** Lines as sent over the network: no more than {@value #LINES}, none longer than a few times a line's length. */
	public static final StreamCodec<ByteBuf, List<String>> LINES_STREAM_CODEC =
			ByteBufCodecs.stringUtf8(LINE_LENGTH * 4).apply(ByteBufCodecs.list(LINES));
	public static final StreamCodec<ByteBuf, Epitaph> STREAM_CODEC = LINES_STREAM_CODEC.map(Epitaph::of, Epitaph::lines);

	public Epitaph {
		lines = List.copyOf(lines);
	}

	/** The epitaph {@code raw} makes: each line cleaned and cut to length, at most {@value #LINES}, trailing blanks dropped. */
	public static Epitaph of(List<String> raw) {
		List<String> out = new ArrayList<>();
		for (int i = 0; i < Math.min(LINES, raw.size()); i++) {
			out.add(clean(raw.get(i)));
		}
		while (!out.isEmpty() && out.get(out.size() - 1).isEmpty()) {
			out.remove(out.size() - 1);
		}
		return out.isEmpty() ? BLANK : new Epitaph(out);
	}

	/** Whether {@code raw}, as a player sent it, is a valid epitaph: at most {@value #LINES} lines of {@value #LINE_LENGTH}. */
	public static boolean fits(List<String> raw) {
		if (raw.size() > LINES) {
			return false;
		}
		for (String line : raw) {
			if (line.codePointCount(0, line.length()) > LINE_LENGTH) {
				return false;
			}
		}
		return true;
	}

	static String clean(String raw) {
		StringBuilder out = new StringBuilder();
		raw.codePoints().filter(c -> !Character.isISOControl(c)).forEach(out::appendCodePoint);
		String text = out.toString().strip();
		return text.codePointCount(0, text.length()) > LINE_LENGTH ? text.substring(0, text.offsetByCodePoints(0, LINE_LENGTH)) : text;
	}

	public boolean isBlank() {
		return lines.isEmpty();
	}

	/** This epitaph with its first line set to {@code name} (as a named Name Tag cuts it), keeping the rest. */
	public Epitaph withFirstLine(String name) {
		List<String> out = new ArrayList<>(lines);
		if (out.isEmpty()) {
			out.add(name);
		} else {
			out.set(0, name);
		}
		return of(out);
	}

	/** The lines padded with blanks to {@value #LINES}, for the epitaph screen. */
	public List<String> padded() {
		List<String> out = new ArrayList<>(lines);
		while (out.size() < LINES) {
			out.add("");
		}
		return out;
	}
}
