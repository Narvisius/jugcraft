package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A Yard Inflatable: a big vinyl figure two blocks tall (a ghost, a black cat, a stack of pumpkins or a spider, one
 * block each) on a little blower. Switched on by hand ({@link #ON}) or by a redstone signal at either half
 * ({@link #POWERED}), the blower fills it over {@value #INFLATE_TICKS} ticks and it glows from inside (light
 * {@value #LIGHT}), wobbling in the breeze; switched off, it sags and folds flat on the lawn over
 * {@value #DEFLATE_TICKS} ticks. The figure is drawn by the client from the lower half's block entity
 * (client/InflatableRenderer.java); flat, it is only a low pile to walk over.
 */
public class InflatableBlock extends TallDecorationBlock implements EntityBlock {
	public static final int INFLATE_TICKS = 40;
	public static final int DEFLATE_TICKS = 60;
	public static final int LIGHT = 7;
	public static final float WOBBLE_DEGREES = 3.0F;
	public static final int WOBBLE_PERIOD = 45;
	public static final BooleanProperty ON = BooleanProperty.create("on");
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	private static final VoxelShape FLAT = Block.box(1.0, 0.0, 1.0, 15.0, 4.0, 16.0);
	private static final VoxelShape FULL_LOWER = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 16.0);
	private static final VoxelShape FULL_UPPER = Block.box(2.0, 0.0, 2.0, 14.0, 14.0, 14.0);

	private final String design;

	public InflatableBlock(Properties properties, String design) {
		super(properties, FULL_LOWER, FULL_UPPER);
		this.design = design;
		registerDefaultState(defaultBlockState().setValue(ON, false).setValue(POWERED, false));
	}

	/** Which figure it is: ghost, cat, pumpkin or spider. */
	public String design() {
		return design;
	}

	/** Whether the blower is running, by hand or by redstone. */
	public static boolean inflated(BlockState state) {
		return state.getValue(ON) || state.getValue(POWERED);
	}

	public static int light(BlockState state) {
		return inflated(state) && state.getValue(HALF) == DoubleBlockHalf.LOWER ? LIGHT : 0;
	}

	/** How full a figure at {@code fill} (0 flat to 1 full) is a tick later, filling or emptying. */
	public static float fill(float fill, boolean inflated) {
		return Mth.clamp(fill + (inflated ? 1.0F / INFLATE_TICKS : -1.0F / DEFLATE_TICKS), 0.0F, 1.0F);
	}

	/** How far (degrees) the figure at {@code pos} leans in the breeze at {@code time}, about x and z; more in rain. */
	public static float[] wobble(BlockPos pos, float time, float rain) {
		float phase = (pos.hashCode() & 0xFF) / 256.0F * Mth.TWO_PI;
		float amount = WOBBLE_DEGREES * (1.0F + rain);
		float t = time / WOBBLE_PERIOD * Mth.TWO_PI + phase;
		return new float[] {amount * Mth.sin(t), amount * 0.6F * Mth.cos(t * 0.7F)};
	}

	public static BlockPos lower(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new DecorationBlockEntity(JugcraftAgriculture.INFLATABLE_ENTITY, pos, state) : null;
	}

	/** Full while it is blown up; a low pile while it is flat. */
	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		boolean lower = state.getValue(HALF) == DoubleBlockHalf.LOWER;
		if (inflated(state)) {
			return lower ? FULL_LOWER : FULL_UPPER;
		}
		return lower ? FLAT : Shapes.empty();
	}

	/** Switches the blower on or off. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			BlockPos lower = lower(pos, state);
			BlockState base = level.getBlockState(lower);
			if (!base.is(this)) {
				return InteractionResult.PASS;
			}
			boolean on = !base.getValue(ON);
			setBoth(level, lower, base.setValue(ON, on));
			level.playSound(null, lower, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4F, on ? 0.9F : 0.7F);
			if (on && !base.getValue(POWERED)) {
				level.playSound(null, lower, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8F, 0.6F);
			}
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	/** A redstone signal at either half runs the blower. */
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
		super.neighborChanged(state, level, pos, neighbor, orientation, moved);
		if (level.isClientSide()) {
			return;
		}
		BlockPos lower = lower(pos, state);
		BlockState base = level.getBlockState(lower);
		if (!base.is(this)) {
			return;
		}
		boolean powered = level.hasNeighborSignal(lower) || level.hasNeighborSignal(lower.above());
		if (powered != base.getValue(POWERED)) {
			setBoth(level, lower, base.setValue(POWERED, powered));
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(ON, POWERED);
	}
}
