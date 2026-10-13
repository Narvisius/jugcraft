package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Rice Roll Medley (the kitchen and cooking expansion's slice 4; tools/rice.py MEDLEY): the owner's platter of rolls,
 * set down whole on a table and served a roll at a time. A use takes the next roll ({@link #PIECES}, the last one first)
 * into the player's inventory; once the platter is bare, a use clears it and gives the platter back. It is made from the
 * rolls it serves, so it is never a gain. Only a whole medley drops itself; a comparator reads the rolls left.
 */
public class RollMedleyBlock extends HorizontalDirectionalBlock {
	/** The rolls on the platter, by place; with {@code n} left, places 0 to {@code n - 1} still hold theirs. */
	public static final List<String> PIECES = List.of("kelp_roll_slice", "kelp_roll_slice", "cod_roll", "salmon_roll", "kelp_roll_slice",
			"kelp_roll_slice", "cod_roll", "salmon_roll");
	public static final int MAX = 8;
	public static final IntegerProperty ROLLS = IntegerProperty.create("rolls", 0, MAX);
	private static final VoxelShape FULL = Block.box(1.0, 0.0, 1.0, 15.0, 3.0, 15.0);
	private static final VoxelShape BARE = Block.box(1.0, 0.0, 1.0, 15.0, 1.0, 15.0);

	public RollMedleyBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ROLLS, MAX));
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(ROLLS) > 0 ? FULL : BARE;
	}

	/** Takes the next roll, or clears a bare platter (which drops the platter). */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		int rolls = state.getValue(ROLLS);
		if (rolls == 0) {
			level.destroyBlock(pos, true, player);
			return InteractionResult.SUCCESS;
		}
		ItemStack roll = new ItemStack(JugcraftAgriculture.item(PIECES.get(rolls - 1)));
		if (!player.getInventory().add(roll)) {
			Block.popResource(level, pos, roll);
		}
		level.setBlock(pos, state.setValue(ROLLS, rolls - 1), Block.UPDATE_ALL);
		level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4F, 1.4F);
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		return InteractionResult.SUCCESS;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return level.getBlockState(pos.below()).isSolid();
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		return direction == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
				: super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	/** Rolls left: 15 for a whole medley. */
	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return state.getValue(ROLLS) * 15 / MAX;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, ROLLS);
	}
}
