package io.github.jimbozoomer.jugcraft.control;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A relay (batch 36): a logic controller switches it on or off with its channel ({@link Channels}), and while on it
 * gives a full redstone signal on every side. Put it next to a machine set to a redstone mode and the controller runs
 * the machine.
 */
public class RelayBlock extends Block implements ChannelSwitch {
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	public RelayBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(Channels.CHANNEL, DyeColor.WHITE).setValue(POWERED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(Channels.CHANNEL, POWERED);
	}

	@Override
	public boolean switchTo(Level level, BlockPos pos, BlockState state, boolean on) {
		if (state.getValue(POWERED) == on) {
			return false;
		}
		level.setBlock(pos, state.setValue(POWERED, on), Block.UPDATE_ALL);
		return true;
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return state.getValue(POWERED) ? 15 : 0;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		return Channels.dye(stack, state, level, pos, player);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			player.sendOverlayMessage(Component.translatable(state.getValue(POWERED) ? "message.jugcraft.relay.on" : "message.jugcraft.relay.off",
					Channels.name(state.getValue(Channels.CHANNEL))));
		}
		return InteractionResult.SUCCESS;
	}
}
