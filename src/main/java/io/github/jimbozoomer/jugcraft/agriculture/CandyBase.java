package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.Nullable;

/**
 * What the sugar is boiled in: water (a water bottle), for clear sugar syrup, or milk (a milk bucket), for cream. Which
 * candy a kettle makes is set by its base and the stage it has cooked to ({@link #makes}).
 */
public enum CandyBase implements StringRepresentable {
	SYRUP,
	CREAM;

	public static final Codec<CandyBase> CODEC = StringRepresentable.fromEnum(CandyBase::values);

	/** By base, then by {@link CandyStage}: the candy poured at that stage, or null where it is still too runny to set. */
	private static final CandyKind[][] MAKES = {
			{null, CandyKind.ROCK_CANDY, CandyKind.CANDY_CORN, CandyKind.CANDY_CORN, CandyKind.SALT_WATER_TAFFY, CandyKind.SALT_WATER_TAFFY,
					CandyKind.HARD_CANDY, CandyKind.CARAMEL, CandyKind.BURNT_SUGAR},
			{null, null, CandyKind.FUDGE, CandyKind.CREAM_CARAMEL, CandyKind.CREAM_CARAMEL, CandyKind.TOFFEE, CandyKind.TOFFEE,
					CandyKind.BURNT_SUGAR, CandyKind.BURNT_SUGAR}};

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public Component displayName() {
		return Component.translatable("candy_base.jugcraft." + getSerializedName());
	}

	/** The candy this base poured at {@code stage} sets into, or null if it would not set. */
	public @Nullable CandyKind makes(CandyStage stage) {
		return MAKES[ordinal()][stage.ordinal()];
	}
}
