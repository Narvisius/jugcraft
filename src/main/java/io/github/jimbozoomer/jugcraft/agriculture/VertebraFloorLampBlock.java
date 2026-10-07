package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Vertebra Floor Lamp (Halloween decorations batch 18): a column of vertebrae two blocks tall on a pelvis foot,
 * under a skull shade lit from inside ({@value #LIGHT} light from its upper half, {@link #LIT}). Use it to switch it; a
 * redstone signal on either half switches it on while it lasts, and off when it goes.
 */
public class VertebraFloorLampBlock extends TallDecorationBlock {
	public static final int LIGHT = 13;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	public VertebraFloorLampBlock(Properties properties) {
		super(properties, Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0), Block.box(3.5, 0.0, 3.5, 12.5, 12.0, 12.5));
		registerDefaultState(defaultBlockState().setValue(LIT, true).setValue(POWERED, false));
	}

	public static int light(BlockState state) {
		return state.getValue(LIT) && state.getValue(HALF) == DoubleBlockHalf.UPPER ? LIGHT : 0;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			boolean lit = !state.getValue(LIT);
			setBoth(level, pos, state.setValue(LIT, lit));
			level.playSound(null, pos, lit ? SoundEvents.BONE_BLOCK_PLACE : SoundEvents.BONE_BLOCK_HIT, SoundSource.BLOCKS, 0.7F, 1.4F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
		super.neighborChanged(state, level, pos, neighbor, orientation, moved);
		if (level instanceof ServerLevel) {
			BlockPos other = pos.relative(state.getValue(HALF).getDirectionToOther());
			boolean powered = level.hasNeighborSignal(pos) || level.hasNeighborSignal(other);
			if (powered != state.getValue(POWERED)) {
				setBoth(level, pos, state.setValue(POWERED, powered).setValue(LIT, powered));
			}
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(LIT, POWERED);
	}
}
