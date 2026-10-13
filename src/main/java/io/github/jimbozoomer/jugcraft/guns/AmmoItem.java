package io.github.jimbozoomer.jugcraft.guns;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** A gun's round: loaded from the inventory when the gun reloads ({@link GunShots#reload}). */
public class AmmoItem extends Item {
	public AmmoItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.guns." + BuiltInRegistries.ITEM.getKey(this).getPath())
				.withStyle(ChatFormatting.GRAY));
	}
}
