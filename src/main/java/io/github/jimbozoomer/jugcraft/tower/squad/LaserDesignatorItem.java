package io.github.jimbozoomer.jugcraft.tower.squad;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * The LTD-7 laser designator (sheets 4 and 5): hold right-click to raise it and look through its own sight (the client
 * draws it: client/LaserDesignatorClient; scroll zooms 1x to 5x, crouch and scroll picks a squad you have a charge for);
 * left-click marks the spot looked at (only you see the mark; no beam). The server checks the mark and calls the squad
 * down ({@link SquadDrop#mark}).
 */
public class LaserDesignatorItem extends Item {
	public LaserDesignatorItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		player.startUsingItem(hand);
		if (player instanceof net.minecraft.server.level.ServerPlayer server) {
			SquadPayloads.sendAccount(server);              // the sight shows what's on their account
		}
		return InteractionResult.CONSUME;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.SPYGLASS;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity user) {
		return 72000;
	}

	/** Whether {@code user} is looking through a designator's sight now. */
	public static boolean sighted(LivingEntity user) {
		return user.isUsingItem() && user.getUseItem().getItem() instanceof LaserDesignatorItem;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.jugcraft.laser_designator.tip1").withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable("item.jugcraft.laser_designator.tip2").withStyle(ChatFormatting.DARK_GRAY));
	}
}
