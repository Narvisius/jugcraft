package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Curiosity Cabinet (Halloween decorations batch 17): a carved mahogany cabinet two blocks tall with glazed doors and
 * three velvet-lined shelves of three places ({@value #PLACES}). Aim at a place on its front and use an item to put one of
 * it there, or use it again to take the thing back, as with a chiseled bookshelf; the doors swing open while you reach
 * in (drawn by the client, {@value #DOOR_TICKS} ticks). Its lower half keeps the things, both halves take them, and a
 * comparator reads how full it is.
 */
public class CuriosityCabinetBlock extends TallDecorationBlock implements EntityBlock, ShowcaseBlockEntity.Showcase {
	public static final int PLACES = 9;
	public static final int DOOR_TICKS = 40;
	/** The shelves' tops, in pixels from the lower block's floor, and the columns' boundaries across the model. */
	public static final float[] SHELF_TOPS = {2.5F, 12.4F, 22.4F};
	public static final float[] COLUMN_SPLITS = {6.25F, 9.75F};

	public CuriosityCabinetBlock(Properties properties) {
		super(properties, Block.box(1.0, 0.0, 2.0, 15.0, 16.0, 15.6), Block.box(1.0, 0.0, 2.0, 15.0, 16.0, 15.6));
	}

	@Override
	public int places() {
		return PLACES;
	}

	@Override
	public boolean accepts(ItemStack stack) {
		return true;
	}

	@Override
	public SoundEvent putSound() {
		return SoundEvents.CHISELED_BOOKSHELF_INSERT;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new ShowcaseBlockEntity(pos, state) : null;
	}

	/** The place a hit on the front of the cabinet whose lower block is at {@code lower} aims at: 0 to 8, or -1. */
	public static int place(BlockState state, BlockPos lower, BlockHitResult hit) {
		Direction facing = state.getValue(FACING);
		if (hit.getDirection() != facing) {
			return -1;
		}
		Vec3 at = hit.getLocation();
		double[] local = ShowcaseBlockEntity.local(facing, (at.x - lower.getX()) * 16, (at.z - lower.getZ()) * 16);
		double y = (at.y - lower.getY()) * 16;
		int row = y < SHELF_TOPS[1] - 1 ? 0 : y < SHELF_TOPS[2] - 1 ? 1 : 2;
		int column = local[0] < COLUMN_SPLITS[0] ? 0 : local[0] < COLUMN_SPLITS[1] ? 1 : 2;
		return row * 3 + column;
	}

	private static BlockPos lower(BlockState state, BlockPos pos) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		return use(stack, state, level, pos, player, hand, hit);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.getMainHandItem().isEmpty()) {
			return InteractionResult.PASS;
		}
		return use(ItemStack.EMPTY, state, level, pos, player, InteractionHand.MAIN_HAND, hit);
	}

	private InteractionResult use(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		BlockPos lower = lower(state, pos);
		if (!(level.getBlockEntity(lower) instanceof ShowcaseBlockEntity cabinet)) {
			return InteractionResult.PASS;
		}
		InteractionResult result = cabinet.use(place(state, lower, hit), stack, player, hand, this);
		if (result.consumesAction() && !level.isClientSide()) {
			level.blockEvent(lower, this, 1, 0);
		}
		return stack.isEmpty() && result == InteractionResult.TRY_WITH_EMPTY_HAND ? InteractionResult.PASS : result;
	}

	/** The doors swing open on clients when it is used. */
	@Override
	protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
		if (level.getBlockEntity(pos) instanceof ShowcaseBlockEntity cabinet) {
			cabinet.mark(level.getGameTime());
			return true;
		}
		return false;
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return level.getBlockEntity(lower(state, pos)) instanceof ShowcaseBlockEntity cabinet ? (cabinet.count() * 15 + PLACES - 1) / PLACES : 0;
	}
}
