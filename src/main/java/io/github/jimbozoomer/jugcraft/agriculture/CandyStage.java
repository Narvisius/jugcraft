package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Locale;
import net.minecraft.network.chat.Component;

/**
 * How far boiled sugar has cooked, read off the Candy Kettle's thermometer (degrees Celsius, as candy makers' stages go):
 * the hotter it has been, the less water is left in it and the harder it sets. A kettle's candy is set by the highest
 * temperature it has reached, so taking it off the heat holds it at its stage.
 */
public enum CandyStage {
	SYRUP(0),
	THREAD(110),
	SOFT_BALL(115),
	FIRM_BALL(120),
	HARD_BALL(125),
	SOFT_CRACK(132),
	HARD_CRACK(145),
	CARAMEL(155),
	BURNT(175);

	/** The temperature the stage starts at. */
	public final int from;

	CandyStage(int from) {
		this.from = from;
	}

	/** The stage sugar cooked to {@code temperature} has reached. */
	public static CandyStage at(int temperature) {
		CandyStage stage = SYRUP;
		for (CandyStage candidate : values()) {
			if (temperature >= candidate.from) {
				stage = candidate;
			}
		}
		return stage;
	}

	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public Component displayName() {
		return Component.translatable("candy_stage.jugcraft." + id());
	}
}
