package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;

/**
 * The candies a Candy Kettle makes, each its own item, with the colour it sets to when nothing dyes or flavours it. Most
 * are drawn in their colour (a dyed colour on the item); caramel and burnt sugar keep their own. Candy corn is poured in
 * layers, rock candy grows its crystals for a day, and taffy must be pulled while it is warm; hard candy with sticks makes
 * lollipops.
 */
public enum CandyKind implements StringRepresentable {
	ROCK_CANDY(0xE6DEF6),
	CANDY_CORN(0xF8F4EA),
	SALT_WATER_TAFFY(0xF4E2C8),
	HARD_CANDY(0xE8A838),
	LOLLIPOP(0xE8A838),
	CARAMEL(0xC07828),
	FUDGE(0x7A4A2A),
	CREAM_CARAMEL(0xC8883A),
	TOFFEE(0xB0702A),
	BURNT_SUGAR(0x2A1A10);

	public static final Codec<CandyKind> CODEC = StringRepresentable.fromEnum(CandyKind::values);

	/** Its colour, undyed and unflavoured. */
	public final int color;

	CandyKind(int color) {
		this.color = color;
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public Item item() {
		return JugcraftAgriculture.item(getSerializedName());
	}

	public Component displayName() {
		return Component.translatable("item.jugcraft." + getSerializedName());
	}

	/** Whether its colour can be changed (dyes, or a flavour's colour): all but caramel and burnt sugar. */
	public boolean tinted() {
		return this != CARAMEL && this != BURNT_SUGAR;
	}

	/** Whether it keeps its flavours: all but burnt sugar, which tastes of nothing but burning. */
	public boolean flavoured() {
		return this != BURNT_SUGAR;
	}
}
