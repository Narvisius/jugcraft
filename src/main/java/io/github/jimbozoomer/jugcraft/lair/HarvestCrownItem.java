package io.github.jimbozoomer.jugcraft.lair;

import io.github.jimbozoomer.jugcraft.agriculture.ScarecrowBlock;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * The Harvest Crown: used on a scarecrow at night, out under the sky among the pumpkins, it opens the Crowfield
 * ({@link HarvestCrownRite}) and is used up. Used on anything else, or in the air, it says what it wants. Every use on a
 * scarecrow, answered or not, rests the crown for {@value HarvestCrownRite#COOLDOWN} ticks.
 */
public class HarvestCrownItem extends Item {
	public HarvestCrownItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (!(context.getLevel() instanceof ServerLevel level) || !(context.getPlayer() instanceof ServerPlayer player)) {
			return InteractionResult.SUCCESS;
		}
		BlockPos pos = context.getClickedPos();
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof ScarecrowBlock)) {
			player.sendOverlayMessage(HarvestCrownRite.message(HarvestCrownRite.Missing.NOT_SCARECROW, 0));
			return InteractionResult.SUCCESS;
		}
		BlockPos lower = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
		ItemStack crown = context.getItemInHand();
		player.getCooldowns().addCooldown(crown, HarvestCrownRite.COOLDOWN);
		HarvestCrownRite.crown(level, player, lower, crown);
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendOverlayMessage(HarvestCrownRite.message(HarvestCrownRite.Missing.NOT_SCARECROW, 0));
		}
		return InteractionResult.PASS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.harvest_crown").withStyle(ChatFormatting.GRAY));
	}
}
