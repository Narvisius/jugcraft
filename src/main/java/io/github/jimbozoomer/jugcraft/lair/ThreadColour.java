package io.github.jimbozoomer.jugcraft.lair;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/** The colours of the Spindle Loft's thread (tools/lairs.py THREAD_COLOURS): its four spools', and the white of her silk. */
public enum ThreadColour implements StringRepresentable {
	GREEN, BLUE, BEIGE, RED, WHITE;

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
