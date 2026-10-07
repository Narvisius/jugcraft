package io.github.jimbozoomer.jugcraft.rocketry;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A flight plan (batch 39, docs/features/rocket-post.md): sneak and use it on a rocket pad to make that pad its
 * destination. Put it in another pad's plan slot and that pad's rockets fly there. It is kept, not used up.
 */
public class FlightPlanItem extends Item {
	public FlightPlanItem(Properties properties) {
		super(properties);
	}

	public static @Nullable GlobalPos target(ItemStack stack) {
		return stack.is(JugcraftRocketry.FLIGHT_PLAN) ? stack.get(JugcraftRocketry.FLIGHT_TARGET) : null;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (!(level.getBlockEntity(pos) instanceof RocketPadBlockEntity)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide() && context.getPlayer() != null) {
			context.getItemInHand().set(JugcraftRocketry.FLIGHT_TARGET, GlobalPos.of(level.dimension(), pos.immutable()));
			context.getPlayer().sendOverlayMessage(Component.translatable("message.jugcraft.flight_plan.set",
					pos.getX(), pos.getY(), pos.getZ()));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		GlobalPos target = target(stack);
		tooltip.accept(target == null
				? Component.translatable("tooltip.jugcraft.flight_plan.blank").withStyle(ChatFormatting.GRAY)
				: Component.translatable("tooltip.jugcraft.flight_plan.target", target.pos().getX(), target.pos().getY(),
						target.pos().getZ(), target.dimension().identifier().toString()).withStyle(ChatFormatting.AQUA));
	}
}
