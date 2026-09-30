package io.github.jimbozoomer.jugcraft.logistics;

import java.util.Set;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Pneumatic extractor: every {@link #INTERVAL} ticks it pulls up to {@link #ITEMS_PER_PULL} items
 * out of the inventory its intake faces and pushes them out of its other sides, into item pipes or
 * straight into an adjacent inventory. A redstone signal pauses it. It needs no power and has no
 * block entity: it runs on scheduled block ticks.
 */
public class PneumaticExtractorBlock extends Block implements ItemConnectable {
	public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
	public static final int INTERVAL = 8;
	public static final int ITEMS_PER_PULL = 16;

	public PneumaticExtractorBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		// Placed against a chest, the intake faces that chest.
		return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		ItemNetworks.invalidate(level);
		if (!level.isClientSide()) {
			level.scheduleTick(pos, this, INTERVAL);
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		ItemNetworks.invalidate(level);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		level.scheduleTick(pos, this, INTERVAL);
		if (level.hasNeighborSignal(pos)) {
			return;
		}
		Direction facing = state.getValue(FACING);
		BlockPos intake = pos.relative(facing);
		Storage<ItemVariant> source = ItemStorage.SIDED.find(level, intake, facing.getOpposite());
		if (source == null || !source.supportsExtraction()) {
			return;
		}
		long moved = 0;
		for (Direction side : Direction.values()) {
			if (side != facing) {
				moved += ItemNetworks.push(level, pos, side, source, ITEMS_PER_PULL - moved, Set.of(intake));
			}
		}
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}
}
