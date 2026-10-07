package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

/**
 * The Iron-Bound Coffin (Halloween decorations batch 18): a Coffin of black wood bound with riveted iron, lined in
 * purple velvet, with {@value IronBoundCoffinBlockEntity#SLOTS} slots (a double chest's). It opens and can be slept in
 * like the Coffin. A Skeleton Key locks it ({@link SkeletonKeyItem}; {@link #LOCKED} hangs the padlock on its hasp):
 * locked, only someone holding that key opens it, and hoppers and pipes can neither take from it nor fill it. As with
 * vanilla's locked containers, a lock stops opening, not breaking; land claims protect against that.
 */
public class IronBoundCoffinBlock extends CoffinBlock {
	public static final BooleanProperty LOCKED = BooleanProperty.create("locked");

	public IronBoundCoffinBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(LOCKED, false));
	}

	/** Locks the coffin with its head at {@code head} to {@code wards} (0 unlocks it), and shows the padlock on both halves. */
	public static void setLock(Level level, BlockPos head, IronBoundCoffinBlockEntity coffin, int wards) {
		coffin.setLock(wards);
		BlockState state = level.getBlockState(head);
		if (!(state.getBlock() instanceof IronBoundCoffinBlock)) {
			return;
		}
		boolean locked = wards != 0;
		level.setBlock(head, state.setValue(LOCKED, locked), Block.UPDATE_ALL);
		BlockPos foot = head.relative(state.getValue(FACING).getOpposite());
		BlockState footState = level.getBlockState(foot);
		if (footState.getBlock() instanceof IronBoundCoffinBlock) {
			level.setBlock(foot, footState.setValue(LOCKED, locked), Block.UPDATE_ALL);
		}
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(PART) == BedPart.HEAD ? new IronBoundCoffinBlockEntity(pos, state) : null;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(LOCKED);
	}
}
