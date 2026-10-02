package io.github.jimbozoomer.jugcraft.drone;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** A drone. Use it on a landing pad to link it to that pad's depot. */
public class DroneItem extends Item {
	private final DroneTier tier;

	public DroneItem(DroneTier tier, Properties properties) {
		super(properties);
		this.tier = tier;
	}

	public DroneTier tier() {
		return tier;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		if (!level.getBlockState(context.getClickedPos()).is(JugcraftDrones.LANDING_PAD)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide() && context.getPlayer() != null) {
			DroneTerminalBlockEntity depot = DroneDepots.depotForPad(level, context.getClickedPos());
			if (depot == null) {
				context.getPlayer().sendOverlayMessage(Component.translatable("message.jugcraft.drone.no_depot"));
			} else if (depot.linkDrone(context.getPlayer(), tier)) {
				context.getItemInHand().shrink(1);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.drone", tier.number(), tier.capacity(), tier.speed(), tier.upkeep(),
				Component.translatable("tooltip.jugcraft.drone.size." + tier.size().name().toLowerCase(java.util.Locale.ROOT))));
	}
}
