package io.github.jimbozoomer.jugcraft.warehouse;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The In-Rack Sprinkler kit (part W3): used on a pallet place, it fits the place with an in-rack sprinkler line (the
 * rack's SPRINKLER), which works with the building's sprinkler system (the releasing panel opens its head over a fire).
 */
public class RackSprinklerKit extends Item {
	public RackSprinklerKit(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		BlockState state = context.getLevel().getBlockState(context.getClickedPos());
		if (!(state.getBlock() instanceof RackBlock) || state.getValue(RackBlock.SPRINKLER)) {
			return InteractionResult.PASS;
		}
		if (!context.getLevel().isClientSide()) {
			context.getLevel().setBlock(context.getClickedPos(), state.setValue(RackBlock.SPRINKLER, true), Block.UPDATE_ALL);
			if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
				context.getItemInHand().shrink(1);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
		out.accept(Component.translatable("tooltip.jugcraft.warehouse.rack_sprinkler_kit").withStyle(ChatFormatting.GRAY));
	}
}
