package io.github.jimbozoomer.jugcraft.warehouse;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

/** A stacker drone or pallet drone, to park at a Drone Charging Dock (using it on the dock). */
public class DroneItem extends Item {
	public final DroneKind kind;

	public DroneItem(DroneKind kind, Properties properties) {
		super(properties);
		this.kind = kind;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof DockBlockEntity dock && context.getPlayer() != null) {
			return dock.park(context.getPlayer(), context.getItemInHand(), kind);
		}
		return InteractionResult.PASS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.warehouse.drone").withStyle(net.minecraft.ChatFormatting.GRAY));
	}
}
