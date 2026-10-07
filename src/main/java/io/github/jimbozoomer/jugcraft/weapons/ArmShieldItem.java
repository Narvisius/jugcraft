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
 * A heater or tower shield (Arms VI, batch 55, docs/features/arms-vi.md; {@link JugcraftArms#SHIELDS}). It blocks through
 * its blocks-attacks component ({@link JugcraftArms#shield}), as vanilla's shield does. It is a ShieldItem so that, raised
 * on screen, it is held as vanilla holds its shield (the blocking model's pose). Vanilla turns any other blocking item
 * as a parrying sword, which swings a shield out of sight.
 */
public class ArmShieldItem extends ShieldItem {
	private final String kind;

	public ArmShieldItem(String kind, Properties properties) {
		super(properties);
		this.kind = kind;
	}

	/** heater_shield or tower_shield. */
	public String kind() {
		return kind;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		// Its trait, with what it is as the description (docs/features/trait-details.md).
		String key = "tooltip.jugcraft.arms." + kind;
		TraitTooltips traits = TraitTooltips.of(tooltip);
		traits.trait(key + ".trait", ChatFormatting.YELLOW, Component.translatable(key));
		traits.end();
	}
}
