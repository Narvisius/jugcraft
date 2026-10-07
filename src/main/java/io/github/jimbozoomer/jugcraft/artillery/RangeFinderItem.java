package io.github.jimbozoomer.jugcraft.artillery;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The Range Finder (batch 51): use it to mark the block you look at, up to {@value JugcraftArtillery#MARK_RANGE}
 * blocks away, as the target for your guns (and any gunner within that distance of you without a mark of their own);
 * it tells you how far away it is. Sneak and use it to clear your mark. See {@link Spotting}.
 */
public class RangeFinderItem extends Item {
	public RangeFinderItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (player.isShiftKeyDown()) {
			Spotting.clear(player);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.artillery.cleared"));
			return InteractionResult.SUCCESS;
		}
		HitResult hit = player.pick(JugcraftArtillery.MARK_RANGE, 1.0F, false);
		if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.artillery.no_target", JugcraftArtillery.MARK_RANGE));
			return InteractionResult.FAIL;
		}
		Spotting.mark(player, block.getBlockPos());
		int distance = (int) Math.round(Math.sqrt(net.minecraft.world.phys.Vec3.atCenterOf(block.getBlockPos()).distanceToSqr(player.getEyePosition())));
		player.sendOverlayMessage(Component.translatable("message.jugcraft.artillery.marked", distance));
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS, 1.0F, 1.2F);
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.jugcraft.range_finder").withStyle(ChatFormatting.GRAY));
	}
}
