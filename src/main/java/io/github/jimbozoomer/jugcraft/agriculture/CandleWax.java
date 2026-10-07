package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.Locale;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The waxes a candle is dipped from: what puts each in a Wax Melting Pot (item tag {@code jugcraft:candle_wax/<wax>}),
 * how many measures one item gives, how long each layer of it burns, and its natural colour (before dyes). Beeswax
 * (honeycomb) burns twice as long as tallow (rendered from rotten flesh), and gives two measures a comb.
 */
public enum CandleWax implements StringRepresentable {
	BEESWAX(2, 4800, 0xE8B84A),
	TALLOW(1, 2400, 0xEEE6D2);

	public static final Codec<CandleWax> CODEC = StringRepresentable.fromEnum(CandleWax::values);

	public final int measures;
	public final int burnPerDip;
	public final int color;
	public final TagKey<Item> tag;

	CandleWax(int measures, int burnPerDip, int color) {
		this.measures = measures;
		this.burnPerDip = burnPerDip;
		this.color = color;
		this.tag = TagKey.create(Registries.ITEM, Jugcraft.id("candle_wax/" + getSerializedName()));
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public Component displayName() {
		return Component.translatable("candle_wax.jugcraft." + getSerializedName());
	}

	/** The wax {@code stack} melts into, or null. */
	public static @Nullable CandleWax of(ItemStack stack) {
		for (CandleWax wax : values()) {
			if (stack.is(wax.tag)) {
				return wax;
			}
		}
		return null;
	}
}
