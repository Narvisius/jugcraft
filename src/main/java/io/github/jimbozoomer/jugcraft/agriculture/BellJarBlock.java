package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Bell Jar (Halloween decorations batch 17): a glass dome on a turned plinth that shows one thing under glass, turning
 * slowly on its plinth (once every {@value #TURN_TICKS} ticks, drawn by the client). Use it with something to put one of
 * it under the dome; use it again to take it back.
 */
public class BellJarBlock extends Block implements EntityBlock, ShowcaseBlockEntity.Showcase {
	public static final int TURN_TICKS = 240;
	private static final VoxelShape SHAPE = Shapes.or(Block.box(2.5, 0.0, 2.5, 13.5, 2.6, 13.5), Block.box(4.2, 2.6, 4.2, 11.8, 14.6, 11.8));

	public BellJarBlock(Properties properties) {
		super(properties);
	}

	@Override
	public int places() {
		return 1;
	}

	@Override
	public boolean accepts(ItemStack stack) {
		return true;
	}

	@Override
	public SoundEvent putSound() {
		return SoundEvents.GLASS_HIT;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ShowcaseBlockEntity(pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		return level.getBlockEntity(pos) instanceof ShowcaseBlockEntity jar ? jar.use(0, stack, player, hand, this) : InteractionResult.PASS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.getMainHandItem().isEmpty() || !(level.getBlockEntity(pos) instanceof ShowcaseBlockEntity jar)) {
			return InteractionResult.PASS;
		}
		InteractionResult result = jar.use(0, ItemStack.EMPTY, player, InteractionHand.MAIN_HAND, this);
		return result == InteractionResult.TRY_WITH_EMPTY_HAND ? InteractionResult.PASS : result;
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return level.getBlockEntity(pos) instanceof ShowcaseBlockEntity jar && jar.count() > 0 ? 15 : 0;
	}
}
