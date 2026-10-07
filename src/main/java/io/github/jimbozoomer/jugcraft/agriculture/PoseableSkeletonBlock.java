package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Poseable Skeleton: a life-sized skeleton two blocks tall. Use it to pose it ({@link #POSE}): sitting on the
 * ground with its legs out, standing and waving, lounging back on its elbows with a knee up, or hanging by its hands
 * from whatever is above it. Its whole body is drawn by the lower half's block model; sitting or lounging, nothing of
 * it is in the upper half, which then has no shape.
 */
public class PoseableSkeletonBlock extends TallDecorationBlock {
	public static final EnumProperty<Pose> POSE = EnumProperty.create("pose", Pose.class);
	private static final VoxelShape LOWER = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);
	private static final VoxelShape UPPER = Block.box(3.0, 0.0, 5.0, 13.0, 16.0, 11.0);
	private static final VoxelShape LOW = Block.box(3.0, 0.0, 1.0, 13.0, 12.0, 13.0);

	public enum Pose implements StringRepresentable {
		SITTING, WAVING, LOUNGING, HANGING;

		public Pose next() {
			return values()[(ordinal() + 1) % values().length];
		}

		/** Whether it stands up into the upper half. */
		public boolean tall() {
			return this == WAVING || this == HANGING;
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public PoseableSkeletonBlock(Properties properties) {
		super(properties, LOWER, UPPER);
		registerDefaultState(defaultBlockState().setValue(POSE, Pose.SITTING));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		boolean tall = state.getValue(POSE).tall();
		if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
			return tall ? LOWER : LOW;
		}
		return tall ? UPPER : Shapes.empty();
	}

	/** Puts it in its next pose. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
			BlockState base = level.getBlockState(lower);
			if (!base.is(this)) {
				return InteractionResult.PASS;
			}
			setBoth(level, lower, base.setValue(POSE, base.getValue(POSE).next()));
			level.playSound(null, lower, SoundEvents.SKELETON_AMBIENT, SoundSource.BLOCKS, 0.5F, 1.2F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(POSE);
	}
}
