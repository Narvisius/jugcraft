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
 * The scents stirred into molten wax (item tag {@code jugcraft:candle_scents/<scent>}), and the aura a lit candle
 * carrying each gives. Most give players in the radius a vanilla effect; three are auras of their own, worked out by
 * {@link AuraCandleBlockEntity#pulse}: warding slows and weakens hostile mobs, harvest gives plants random ticks, and
 * revealing makes other creatures glow (restless spirits included, which shows them). Ghostly, from ectoplasm, turns
 * players invisible. The colour tints the flame and the drifting scent.
 */
public enum CandleScent implements StringRepresentable {
	SWIFTNESS(MobEffects.SPEED, 0x7CC8F0),
	LEAPING(MobEffects.JUMP_BOOST, 0x9CF26E),
	MOONLIGHT(MobEffects.NIGHT_VISION, 0x5A78FF),
	FEATHERFALL(MobEffects.SLOW_FALLING, 0xF2F2E8),
	TIDE(MobEffects.WATER_BREATHING, 0x3FA9C8),
	EMBER(MobEffects.FIRE_RESISTANCE, 0xFF6A1A),
	DILIGENCE(MobEffects.HASTE, 0xE8C84A),
	MENDING(MobEffects.REGENERATION, 0xF27ACB),
	WARDING(null, 0x7A3FCF),
	HARVEST(null, 0x5FBF3A),
	REVEALING(null, 0x9FFFE8),
	GHOSTLY(MobEffects.INVISIBILITY, 0xB8FFD8);

	public static final Codec<CandleScent> CODEC = StringRepresentable.fromEnum(CandleScent::values);

	/** The effect players in the radius get, or null for an aura of its own. */
	public final @Nullable Holder<MobEffect> effect;
	public final int color;
	public final TagKey<Item> tag;

	CandleScent(@Nullable Holder<MobEffect> effect, int color) {
		this.effect = effect;
		this.color = color;
		this.tag = TagKey.create(Registries.ITEM, Jugcraft.id("candle_scents/" + getSerializedName()));
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public Component displayName() {
		return Component.translatable("candle_scent.jugcraft." + getSerializedName()).withStyle(style -> style.withColor(TextColor.fromRgb(color)));
	}

	/** The scent {@code stack} gives, or null. */
	public static @Nullable CandleScent of(ItemStack stack) {
		for (CandleScent scent : values()) {
			if (stack.is(scent.tag)) {
				return scent;
			}
		}
		return null;
	}
}
