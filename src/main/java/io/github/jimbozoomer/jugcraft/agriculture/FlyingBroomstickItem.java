package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * The flying broomstick as an item (fall addition 22): used, it is laid out where the player stands and, if it has any
 * ointment left ({@link Broomstick#charge(ItemStack)}), the player is on it and away; dry, it is only laid down, to be
 * anointed. It keeps its charge in {@code jugcraft:broom_charge}, shown in its tooltip.
 */
public class FlyingBroomstickItem extends Item {
	public FlyingBroomstickItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player.isPassenger()) {
			return InteractionResult.FAIL;
		}
		ItemStack stack = player.getItemInHand(hand);
		if (level instanceof ServerLevel server) {
			Broomstick broom = new Broomstick(level, player.position(), player.getYRot(), Broomstick.charge(stack));
			server.addFreshEntity(broom);
			stack.consume(1, player);
			if (broom.dry()) {
				if (player instanceof ServerPlayer walker) {
					walker.sendOverlayMessage(Component.translatable("message.jugcraft.broom.needs_ointment"));
				}
			} else {
				player.startRiding(broom);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		int charge = Broomstick.charge(stack);
		tooltip.accept((charge > 0 ? Component.translatable("tooltip.jugcraft.broom.charge", Broomstick.seconds(charge))
				: Component.translatable("tooltip.jugcraft.broom.empty")).withStyle(ChatFormatting.DARK_PURPLE));
		tooltip.accept(Component.translatable("tooltip.jugcraft.broom.how").withStyle(ChatFormatting.GRAY));
	}
}
