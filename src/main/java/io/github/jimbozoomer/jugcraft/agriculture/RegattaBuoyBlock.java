package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A regatta buoy: floats on still water like a lily pad (placed on it, and gone if the water is), with a
 * {@link #NUMBER} from 1 to {@link #MAX_NUMBER} that sets its place in the course. Using it counts the number
 * up (sneaking, down). Boats bump into it; the Regatta Flag finds it by its {@link RegattaBuoyBlockEntity}.
 */
public class RegattaBuoyBlock extends BaseEntityBlock {
	public static final int MAX_NUMBER = 16;
	public static final IntegerProperty NUMBER = IntegerProperty.create("number", 1, MAX_NUMBER);
	private static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 14.0, 12.0);

	public RegattaBuoyBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(NUMBER, 1));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new RegattaBuoyBlockEntity(pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** Like a lily pad: on still water, with no water in its own block. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return level.getFluidState(pos.below()).getType() == Fluids.WATER && level.getFluidState(pos).isEmpty();
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
				: super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer server) {
			int number = state.getValue(NUMBER);
			number = player.isSecondaryUseActive() ? (number + MAX_NUMBER - 2) % MAX_NUMBER + 1 : number % MAX_NUMBER + 1;
			level.setBlock(pos, state.setValue(NUMBER, number), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.8F, 1.2F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
			server.sendOverlayMessage(Component.translatable("message.jugcraft.regatta.buoy", number));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NUMBER);
	}
}
