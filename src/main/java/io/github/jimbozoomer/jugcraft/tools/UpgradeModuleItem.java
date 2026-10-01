package io.github.jimbozoomer.jugcraft.tools;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** A tool upgrade module (see {@link ToolUpgrades}); use it on a charging station holding the tool to fit it. */
public class UpgradeModuleItem extends Item {
	private final ToolUpgrades.Kind kind;

	public UpgradeModuleItem(Properties properties, ToolUpgrades.Kind kind) {
		super(properties);
		this.kind = kind;
	}

	public ToolUpgrades.Kind kind() {
		return kind;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft." + kind.id + "_module").withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable("tooltip.jugcraft.module_fitting").withStyle(ChatFormatting.DARK_GRAY));
	}
}
