package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Silk Spool Stack (Halloween decorations batch 19, the Spider's Larder): three tall wooden spools wound with silk,
 * standing in a row across the block. Use a dye on a spool to rewind it in that colour (the dye is used); each starts
 * white, purple and red. The silk is drawn by the client in its colour (client/SilkSpoolRenderer.java).
 */
public class SilkSpoolStackBlock extends HorizontalDirectionalBlock implements EntityBlock {
	public static final int SPOOLS = 3;
	private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 3.0, 15.0, 14.0, 13.0);
	private static final VoxelShape SHAPE_X = Block.box(3.0, 0.0, 1.0, 13.0, 14.0, 15.0);

	public SilkSpoolStackBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE : SHAPE_X;
	}

	/**
	 * Which spool, 0 to {@value #SPOOLS} less one from the left as seen from its front, a hit at {@code at} on the block
	 * at {@code pos} facing {@code facing} lands on.
	 */
	public static int spoolAt(BlockPos pos, Direction facing, Vec3 at) {
		Direction right = facing.getCounterClockWise();
		double across = (at.x - pos.getX() - 0.5) * right.getStepX() + (at.z - pos.getZ() - 0.5) * right.getStepZ();
		return Math.max(0, Math.min(SPOOLS - 1, (int) Math.floor((across + 0.5) * SPOOLS)));
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		DyeColor colour = ScarecrowBlock.dyeColor(stack);
		if (colour == null) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(level.getBlockEntity(pos) instanceof SilkSpoolStackBlockEntity spools) || !level.mayInteract(player, pos)) {
			return InteractionResult.FAIL;
		}
		int spool = spoolAt(pos, state.getValue(FACING), hit.getLocation());
		if (spools.colour(spool) == colour) {
			return InteractionResult.PASS;
		}
		spools.setColour(spool, colour);
		stack.consume(1, player);
		level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8F, 1.2F);
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		return InteractionResult.SUCCESS;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SilkSpoolStackBlockEntity(pos, state);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}
}
