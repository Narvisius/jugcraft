package io.github.jimbozoomer.jugcraft.control;

import io.github.jimbozoomer.jugcraft.agriculture.ScarecrowBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Control channels (batch 36): the sixteen dye colours. A sensor reports on its colour's channel and a relay switches
 * with its colour's channel; use a dye on either to set it (the dye is used up, as on a sign).
 */
public final class Channels {
	public static final EnumProperty<DyeColor> CHANNEL = EnumProperty.create("channel", DyeColor.class);
	public static final int COUNT = 16;

	private Channels() {
	}

	/** Sets the channel of the block at {@code pos} from a dye in hand, or passes if the item is not a new colour. */
	public static InteractionResult dye(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player) {
		DyeColor color = ScarecrowBlock.dyeColor(stack);
		if (color == null || state.getValue(CHANNEL) == color) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide()) {
			if (!player.mayBuild() || !level.mayInteract(player, pos)) {
				return InteractionResult.FAIL;
			}
			level.setBlock(pos, state.setValue(CHANNEL, color), Block.UPDATE_ALL);
			stack.consume(1, player);
			level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.channel", name(color)));
		}
		return InteractionResult.SUCCESS;
	}

	public static Component name(DyeColor color) {
		return Component.translatable("color.minecraft." + color.getSerializedName());
	}
}
