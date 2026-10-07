package io.github.jimbozoomer.jugcraft.gear;

import io.github.jimbozoomer.jugcraft.tools.Chargeable;
import io.github.jimbozoomer.jugcraft.tools.Jetpack;
import io.github.jimbozoomer.jugcraft.tools.PoweredToolItem;
import io.github.jimbozoomer.jugcraft.tools.ToolUpgrades;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorType;

/**
 * One piece of the powered exosuit (batch 28, docs/features/exosuit.md). It is armor that holds JE, charged at the
 * charging station; {@link Exosuit} runs its powers while it is worn and charged. The chestplate is also a jetpack.
 * The two liveries (Vanguard and Ronin) are the same piece in different paint.
 */
public class ExosuitItem extends Item implements Chargeable, Jetpack {
	public enum Style {
		VANGUARD("vanguard"), RONIN("ronin");

		public final String id;

		Style(String id) {
			this.id = id;
		}
	}

	private final ArmorType type;
	private final Style style;

	public ExosuitItem(Properties properties, ArmorType type, Style style) {
		super(properties);
		this.type = type;
		this.style = style;
	}

	public ArmorType type() {
		return type;
	}

	public Style style() {
		return style;
	}

	/** helmet, chestplate, leggings or boots. */
	public String piece() {
		return piece(type);
	}

	public static String piece(ArmorType type) {
		return switch (type) {
			case HELMET -> "helmet";
			case CHESTPLATE -> "chestplate";
			case LEGGINGS -> "leggings";
			default -> "boots";
		};
	}

	@Override
	public long baseCapacity() {
		return Exosuit.CAPACITY;
	}

	@Override
	public boolean isJetpack(ItemStack stack) {
		return type == ArmorType.CHESTPLATE;
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return PoweredToolItem.barWidth(stack);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return PoweredToolItem.BAR_COLOR;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(PoweredToolItem.energyLine(stack));
		ToolUpgrades.appendTooltip(stack, tooltip);
		tooltip.accept(Component.translatable("tooltip.jugcraft.exosuit_" + piece()).withStyle(ChatFormatting.GRAY));
	}
}
