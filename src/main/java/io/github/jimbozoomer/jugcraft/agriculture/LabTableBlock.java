package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Lab Table: a riveted steel operating table two blocks long with leather straps, and on it a patient under a
 * stained sheet. It is one prop: the {@link BedPart#FOOT} block, where the player placed it, is the master (it drops the
 * table and holds the empty block entity the patient is drawn from); the {@link BedPart#HEAD} block lies away from the
 * player ({@link #FACING}). While either block has a redstone signal ({@link #POWERED}) the patient sits bolt upright
 * under its sheet, with a crackle of sparks; when the power goes it lies back down. At night it twitches now and then.
 * The patient is drawn by the client (client/LabTableRenderer.java).
 */
public class LabTableBlock extends BaseEntityBlock {
	public static final float SIT_DEGREES = 70.0F;
	public static final float SIT_SPEED = 6.0F;
	public static final int TWITCH_PERIOD = 97;
	public static final int TWITCH_TICKS = 4;
	/** How long a patient woken by a Lightning Harness sits up, arms out (batch 19). */
	public static final int WAKE_TICKS = 100;
	/** The block event that wakes the patient. */
	public static final int EVENT_WAKE = 1;
	/** The way from the foot to the head. */
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
	/** On the foot: whether either block has a redstone signal (the patient sits up). */
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 15.0, 16.0);

	public LabTableBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, BedPart.FOOT).setValue(POWERED, false));
	}

	/** The foot (the master) of the table with a block at {@code pos}. */
	public static BlockPos foot(BlockPos pos, BlockState state) {
		return state.getValue(PART) == BedPart.FOOT ? pos : pos.relative(state.getValue(FACING).getOpposite());
	}

	/** The other block of the table with a block at {@code pos}. */
	public static BlockPos other(BlockPos pos, BlockState state) {
		return state.getValue(PART) == BedPart.FOOT ? pos.relative(state.getValue(FACING)) : pos.relative(state.getValue(FACING).getOpposite());
	}

	/** Whether the patient twitches at {@code time}: at night, for a few ticks of each period (by where it lies). */
	public static boolean twitching(BlockPos pos, boolean night, long time) {
		return night && Math.floorMod(time + pos.hashCode(), TWITCH_PERIOD) < TWITCH_TICKS;
	}

	/**
	 * Wakes the patient of the table whose foot is at {@code foot}: for {@value #WAKE_TICKS} ticks it sits bolt upright,
	 * arms out, groaning, its eyes flashing (a Lightning Harness above calls this).
	 */
	public static void wake(Level level, BlockPos foot) {
		BlockState state = level.getBlockState(foot);
		if (!(state.getBlock() instanceof LabTableBlock) || state.getValue(PART) != BedPart.FOOT) {
			return;
		}
		level.blockEvent(foot, state.getBlock(), EVENT_WAKE, 0);
		BlockPos head = other(foot, state);
		level.playSound(null, head, SoundEvents.ZOMBIE_AMBIENT, SoundSource.BLOCKS, 1.0F, 0.5F);
		if (level instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.ELECTRIC_SPARK, head.getX() + 0.5, head.getY() + 1.2, head.getZ() + 0.5, 24, 0.4, 0.4, 0.4, 0.08);
		}
	}

	/** Whether a patient woken at game time {@code woken} is still awake at {@code now}. */
	public static boolean awake(long woken, long now) {
		return now >= woken && now - woken < WAKE_TICKS;
	}

	/** The foot's block entity marks when the patient was woken, on the server and on clients. */
	@Override
	protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
		if (id == EVENT_WAKE && level.getBlockEntity(pos) instanceof DecorationBlockEntity table) {
			table.mark(level.getGameTime());
			return true;
		}
		return super.triggerEvent(state, level, pos, id, param);
	}

	/** {@code current} lean of the patient moved toward sitting up or lying down by {@value #SIT_SPEED} degrees. */
	public static float lean(float current, boolean powered, float ticks) {
		float target = powered ? SIT_DEGREES : 0.0F;
		return current + Mth.clamp(target - current, -SIT_SPEED * ticks, SIT_SPEED * ticks);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(PART) == BedPart.FOOT ? new DecorationBlockEntity(JugcraftAgriculture.LAB_TABLE_ENTITY, pos, state) : null;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** It lies away from the player, the foot where they aimed; the head's block must be free. */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection();
		BlockPos head = context.getClickedPos().relative(facing);
		Level level = context.getLevel();
		if (level.isOutsideBuildHeight(head) || !level.getWorldBorder().isWithinBounds(head)
				|| !level.getBlockState(head).canBeReplaced(BlockPlaceContext.at(context, head, Direction.UP))) {
			return null;
		}
		return defaultBlockState().setValue(FACING, facing).setValue(PART, BedPart.FOOT);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide()) {
			level.setBlock(other(pos, state), state.setValue(PART, BedPart.HEAD), Block.UPDATE_ALL);
		}
	}

	/** Breaking either block breaks the table: the foot drops it once. */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(this)) {
			return; // Powered, not removed.
		}
		BlockPos other = other(pos, state);
		BlockState there = level.getBlockState(other);
		if (!there.is(this) || there.getValue(FACING) != state.getValue(FACING) || there.getValue(PART) == state.getValue(PART)) {
			return;
		}
		if (state.getValue(PART) == BedPart.HEAD) {
			level.destroyBlock(other, true);
		} else {
			level.removeBlock(other, false);
		}
	}

	/** In creative, breaking the head takes the table away without dropping it. */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && player.getAbilities().instabuild && state.getValue(PART) == BedPart.HEAD) {
			BlockPos foot = foot(pos, state);
			if (level.getBlockState(foot).is(this)) {
				level.removeBlock(foot, false);
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	/** A redstone signal at either block sits the patient up; it lies down when the signal goes. */
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
		super.neighborChanged(state, level, pos, neighbor, orientation, moved);
		if (level.isClientSide()) {
			return;
		}
		BlockPos foot = foot(pos, state);
		BlockState footState = level.getBlockState(foot);
		if (!footState.is(this) || footState.getValue(PART) != BedPart.FOOT) {
			return;
		}
		boolean powered = level.hasNeighborSignal(foot) || level.hasNeighborSignal(other(foot, footState));
		if (powered == footState.getValue(POWERED)) {
			return;
		}
		level.setBlock(foot, footState.setValue(POWERED, powered), Block.UPDATE_ALL);
		if (powered && level instanceof ServerLevel server) {
			BlockPos head = other(foot, footState);
			level.playSound(null, head, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 0.3F, 1.8F);
			level.playSound(null, head, SoundEvents.ZOMBIE_AMBIENT, SoundSource.BLOCKS, 0.8F, 0.6F);
			server.sendParticles(ParticleTypes.ELECTRIC_SPARK, head.getX() + 0.5, head.getY() + 1.1, head.getZ() + 0.5, 16, 0.4, 0.3, 0.4, 0.05);
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
		builder.add(FACING, PART, POWERED);
	}
}
