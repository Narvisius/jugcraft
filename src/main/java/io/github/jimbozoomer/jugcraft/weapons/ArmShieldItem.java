package io.github.jimbozoomer.jugcraft.weapons;

import io.github.jimbozoomer.jugcraft.gear.TraitTooltips;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A heater or tower shield (Arms VI, batch 55, docs/features/arms-vi.md; {@link JugcraftArms#SHIELDS}), or an armor set's
 * shield in a shape of its own (Arms VII, {@link ArmVariants#SET_SHIELDS}). It blocks through its blocks-attacks
 * component ({@link JugcraftArms#shield}), as vanilla's shield does. It is a ShieldItem so that, raised on screen, it is
 * held as vanilla holds its shield (the blocking model's pose). Vanilla turns any other blocking item as a parrying
 * sword, which swings a shield out of sight.
 */
public class ArmShieldItem extends ShieldItem {
	private final String kind;
	private final String line;

	public ArmShieldItem(String kind, Properties properties) {
		this(kind, null, properties);
	}

	/** An armor set's shield: of kind, its own shape, in line, its armor set ({@link ArmVariants#SETS}). */
	public ArmShieldItem(String kind, String line, Properties properties) {
		super(properties);
		this.kind = kind;
		this.line = line;
	}

	/** heater_shield or tower_shield, or an armor set's shield's own shape (star_shield). */
	public String kind() {
		return kind;
	}

	/** The armor set an armor set's shield is of, or null. */
	public String line() {
		return line;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		// Its trait, with what it is as the description (docs/features/trait-details.md); an armor set's shield names its
		// set, as the set's arm does ("Of the Sentinel set").
		String key = "tooltip.jugcraft.arms." + kind;
		TraitTooltips traits = TraitTooltips.of(tooltip);
		traits.trait(key + ".trait", ChatFormatting.YELLOW, Component.translatable(key));
		if (line != null) {
			traits.trait("tooltip.jugcraft.arms.line." + line + ".trait", ChatFormatting.DARK_PURPLE);
		}
		traits.end();
	}
}
