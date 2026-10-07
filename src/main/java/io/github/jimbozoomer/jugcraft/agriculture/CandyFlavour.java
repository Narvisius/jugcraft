package io.github.jimbozoomer.jugcraft.agriculture;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.Locale;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The flavours stirred into a Candy Kettle (item tag {@code jugcraft:candy_flavours/<flavour>}), up to
 * {@value CandyKettleBlockEntity#MAX_FLAVOURS} a batch. Each piece of candy made with a flavour gives its eater a short
 * effect, and an undyed candy takes the colour of its first flavour.
 */
public enum CandyFlavour implements StringRepresentable {
	CHOCOLATE(MobEffects.SPEED, 10, 0x5A3218),
	BERRY(MobEffects.REGENERATION, 4, 0xC8283C),
	GLOW_BERRY(MobEffects.NIGHT_VISION, 30, 0xF0B030),
	HONEY(MobEffects.ABSORPTION, 10, 0xE8A020),
	CRANBERRY(MobEffects.RESISTANCE, 10, 0xB0122E),
	SPICED(MobEffects.FIRE_RESISTANCE, 15, 0xA0522D),
	CHESTNUT(MobEffects.HASTE, 15, 0x8A5A30);

	public static final Codec<CandyFlavour> CODEC = StringRepresentable.fromEnum(CandyFlavour::values);

	public final Holder<MobEffect> effect;
	public final int seconds;
	public final int color;
	public final TagKey<Item> tag;

	CandyFlavour(Holder<MobEffect> effect, int seconds, int color) {
		this.effect = effect;
		this.seconds = seconds;
		this.color = color;
		this.tag = TagKey.create(Registries.ITEM, Jugcraft.id("candy_flavours/" + getSerializedName()));
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public Component displayName() {
		return Component.translatable("candy_flavour.jugcraft." + getSerializedName()).withStyle(style -> style.withColor(TextColor.fromRgb(color)));
	}

	/** The flavour {@code stack} gives, if any. */
	public static @Nullable CandyFlavour of(ItemStack stack) {
		for (CandyFlavour flavour : values()) {
			if (stack.is(flavour.tag)) {
				return flavour;
			}
		}
		return null;
	}
}
