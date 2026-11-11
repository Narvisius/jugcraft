package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.tower.FurnitureBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A fire alarm pull station (the administration building part 1; tools/admin_fittings.py): pulled (a click), the
 * building's bells ring and everyone within {@link #REACH} blocks is told who pulled it.
 */
public class FireAlarmPullBlock extends FurnitureBlock {
	public static final int REACH = 48;

	public FireAlarmPullBlock(Properties properties, double[]... boxes) {
		super(properties, boxes);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.8F, 0.6F);
		for (int k = 0; k < 3; k++) {
			level.playSound(null, pos.above(), SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0F, 1.0F + 0.1F * k);
		}
		Component msg = Component.translatable("message.jugcraft.nuclear.fire_pull", player.getName()).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
		for (ServerPlayer p : ((ServerLevel) level).players()) {
			if (p.blockPosition().distSqr(pos) <= REACH * REACH) {
				p.sendSystemMessage(msg);
			}
		}
		return InteractionResult.SUCCESS_SERVER;
	}
}
