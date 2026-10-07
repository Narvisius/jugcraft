package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

/**
 * Making candy items from a batch: the candy's item, its colour (a dyed colour, or for candy corn a colour for each of its
 * three bands), and, if it is flavoured, a name that says so ("Chocolate Fudge"), the flavours' effects when eaten and a
 * line for each in its tooltip. Candy with nothing changed is the plain item, so it stacks with candy from anywhere else.
 */
public final class Candies {
	/** A colour left as the candy's own. */
	public static final int NATURAL = -1;
	/** How long a piece of candy takes to eat, in seconds (half a meal's). */
	public static final float EAT_SECONDS = 0.8F;
	/** Candy corn's bands, tip to base, where a layer is undyed: white, orange, yellow. */
	public static final int[] CORN_BANDS = {0xF8F4EA, 0xF08A24, 0xF6C836};

	private Candies() {
	}

	/** {@code count} pieces of {@code kind}, flavoured and coloured ({@code colors}: one a layer, or one). */
	public static ItemStack make(CandyKind kind, List<CandyFlavour> flavours, List<Integer> colors, int count) {
		ItemStack stack = new ItemStack(kind.item(), count);
		if (kind == CandyKind.CANDY_CORN) {
			List<Integer> bands = bands(colors);
			if (!bands.equals(List.of(CORN_BANDS[0], CORN_BANDS[1], CORN_BANDS[2]))) {
				stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), bands));
			}
		} else if (kind.tinted() && !colors.isEmpty() && colors.get(0) != NATURAL) {
			stack.set(DataComponents.DYED_COLOR, new DyedItemColor(colors.get(0)));
		}
		List<CandyFlavour> kept = kind.flavoured() ? flavours : List.of();
		if (!kept.isEmpty()) {
			stack.set(DataComponents.ITEM_NAME, name(kind, kept));
			List<MobEffectInstance> effects = new ArrayList<>();
			List<Component> lines = new ArrayList<>();
			for (CandyFlavour flavour : kept) {
				effects.add(new MobEffectInstance(flavour.effect, flavour.seconds * 20));
				lines.add(Component.translatable("tooltip.jugcraft.candy.flavour", flavour.displayName(),
						Component.translatable(flavour.effect.value().getDescriptionId()), flavour.seconds)
						.withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false)));
			}
			Consumable plain = stack.getOrDefault(DataComponents.CONSUMABLE, Consumables.DEFAULT_FOOD);
			stack.set(DataComponents.CONSUMABLE, Consumables.defaultFood().consumeSeconds(plain.consumeSeconds())
					.onConsume(new ApplyStatusEffectsConsumeEffect(effects)).build());
			stack.set(DataComponents.LORE, new ItemLore(lines));
		}
		return stack;
	}

	/** Candy corn's three bands, tip to base: one layer colours all three, two the tip and the rest, three one each. */
	public static List<Integer> bands(List<Integer> layers) {
		List<Integer> bands = new ArrayList<>();
		for (int band = 0; band < CORN_BANDS.length; band++) {
			int layer = layers.isEmpty() ? band : Math.min(band, layers.size() - 1);
			int color = layer < layers.size() ? layers.get(layer) : NATURAL;
			bands.add(color == NATURAL ? CORN_BANDS[layer] : color);
		}
		return bands;
	}

	/** "Chocolate Fudge", "Chocolate and Honey Fudge". */
	public static Component name(CandyKind kind, List<CandyFlavour> flavours) {
		return switch (flavours.size()) {
			case 0 -> kind.displayName();
			case 1 -> Component.translatable("item.jugcraft.candy.one", flavours.get(0).displayName(), kind.displayName());
			default -> Component.translatable("item.jugcraft.candy.two", flavours.get(0).displayName(), flavours.get(1).displayName(),
					kind.displayName());
		};
	}

	/** The colour its pieces show: the first layer's, or the candy's own. */
	public static int shown(CandyKind kind, List<Integer> colors) {
		if (kind == CandyKind.CANDY_CORN) {
			return bands(colors).get(0);
		}
		return !kind.tinted() || colors.isEmpty() || colors.get(0) == NATURAL ? kind.color : colors.get(0);
	}
}
