package io.github.jimbozoomer.jugcraft.control;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * An alarm klaxon (batch 37): a logic controller switches it with its channel, like a relay. While on, its beacon
 * lights and it sounds every {@link #INTERVAL} ticks, heard up to {@link #VOLUME} times the usual distance.
 */
public class AlarmBlock extends Block implements ChannelSwitch {
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	public static final int INTERVAL = 30;
	public static final float VOLUME = 3.0F;
	private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 11, 13);

	public AlarmBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(Channels.CHANNEL, DyeColor.WHITE).setValue(POWERED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(Channels.CHANNEL, POWERED);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public boolean switchTo(Level level, BlockPos pos, BlockState state, boolean on) {
		if (state.getValue(POWERED) == on) {
			return false;
		}
		level.setBlock(pos, state.setValue(POWERED, on), Block.UPDATE_ALL);
		if (on) {
			level.scheduleTick(pos, this, 1);
		}
		return true;
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!state.getValue(POWERED)) {
			return;
		}
		level.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, VOLUME, 0.6F);
		level.scheduleTick(pos, this, INTERVAL);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		return Channels.dye(stack, state, level, pos, player);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			player.sendOverlayMessage(Component.translatable(state.getValue(POWERED) ? "message.jugcraft.alarm.on" : "message.jugcraft.alarm.off",
					Channels.name(state.getValue(Channels.CHANNEL))));
		}
		return InteractionResult.SUCCESS;
	}
}
