package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A piñata as an item (fall addition 28): used on the underside of a block with something to tie a rope to, with two
 * clear blocks below, it hangs a {@link Pinata} of its kind there, empty, for its user to fill and the party to burst.
 */
public class PinataItem extends Item {
	private final Pinata.Kind kind;

	public PinataItem(Properties properties, Pinata.Kind kind) {
		super(properties);
		this.kind = kind;
	}

	public Pinata.Kind kind() {
		return kind;
	}

	/** Whether a piñata can hang under {@code support}: a rope to tie and two clear blocks below it. */
	public static boolean canHangUnder(Level level, BlockPos support) {
		return Pinata.canHangFrom(level, support) && level.getBlockState(support.below()).getCollisionShape(level, support.below()).isEmpty()
				&& level.getBlockState(support.below(2)).getCollisionShape(level, support.below(2)).isEmpty();
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getClickedFace() != Direction.DOWN) {
			return InteractionResult.PASS;
		}
		Level level = context.getLevel();
		BlockPos support = context.getClickedPos();
		if (!canHangUnder(level, support)) {
			if (context.getPlayer() instanceof ServerPlayer player) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.pinata.no_room"));
			}
			return InteractionResult.FAIL;
		}
		if (level instanceof ServerLevel server) {
			if (context.getPlayer() != null && !server.mayInteract(context.getPlayer(), support)
					|| Pinata.hang(server, support, kind, context.getPlayer()) == null) {
				return InteractionResult.FAIL;
			}
			server.playSound(null, support.below(), SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 1.0F, 1.0F);
			context.getItemInHand().consume(1, context.getPlayer());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.jugcraft.pinata.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
