package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Colossal Skull (Halloween decorations batch 18, the Buried Colossus): a giant's cracked skull two blocks wide, tall
 * and deep, meant to be half sunk in the ground; placed and broken as one ({@link MultiDecorationBlock}, from the block
 * aimed at to the right, up and away). Give any of its blocks a redstone signal and its jaw drops open with a grinding
 * of bone and the wind moans through it ({@link #POWERED}, on every block); it shuts when the signal goes. Use it and the
 * jaw snaps. At night its eye sockets glow faintly blue. The jaw and the glow are the client's, from the block entity
 * on its first block.
 */
public class ColossalSkullBlock extends MultiDecorationBlock implements EntityBlock {
	private static final int[][] CELLS = {{0, 0, 0}, {1, 0, 0}, {0, 1, 0}, {1, 1, 0}, {0, 0, 1}, {1, 0, 1}, {0, 1, 1}, {1, 1, 1}};
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, CELLS.length - 1);
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	public static final float JAW_DEGREES = 28.0F;
	/** How long the jaw takes to drop or shut, and to snap, in ticks. */
	public static final int JAW_TICKS = 14;
	public static final int SNAP_TICKS = 8;
	public static final int EVENT_JAW = 1;
	public static final int EVENT_SNAP = 2;

	public ColossalSkullBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(POWERED, false));
	}

	@Override
	public int[][] cells() {
		return CELLS;
	}

	@Override
	public IntegerProperty partProperty() {
		return PART;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}

	/** Whether any block of the skull with its first block at {@code master} has a redstone signal. */
	private boolean powered(Level level, BlockPos master, Direction facing) {
		for (int part = 0; part < CELLS.length; part++) {
			if (level.hasNeighborSignal(partPos(master, facing, part))) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
		super.neighborChanged(state, level, pos, neighbor, orientation, moved);
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		BlockPos master = masterPos(pos, state);
		Direction facing = state.getValue(FACING);
		BlockState first = level.getBlockState(master);
		if (!first.is(this) || part(first) != MASTER) {
			return;
		}
		boolean powered = powered(level, master, facing);
		if (powered == first.getValue(POWERED)) {
			return;
		}
		for (int part = 0; part < CELLS.length; part++) {
			BlockPos at = partPos(master, facing, part);
			BlockState there = level.getBlockState(at);
			if (there.is(this) && part(there) == part) {
				level.setBlock(at, there.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
			}
		}
		server.blockEvent(master, this, EVENT_JAW, powered ? 1 : 0);
		server.playSound(null, master, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0F, 0.4F);
		if (powered) {
			server.playSound(null, master, SoundEvents.BREEZE_IDLE_AIR, SoundSource.BLOCKS, 1.4F, 0.5F);
		}
	}

	/** The jaw snaps. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			BlockPos master = masterPos(pos, state);
			level.blockEvent(master, this, EVENT_SNAP, 0);
			level.playSound(null, master, SoundEvents.SKELETON_HURT, SoundSource.BLOCKS, 1.2F, 0.4F);
			level.gameEvent(player, GameEvent.BLOCK_ACTIVATE, master);
		}
		return InteractionResult.SUCCESS;
	}

	/** Marks the jaw's drop or snap on clients, for the renderer to move it from. */
	@Override
	protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
		if (level.getBlockEntity(pos) instanceof ColossalSkullBlockEntity skull) {
			if (id == EVENT_SNAP) {
				skull.snap(level.getGameTime());
			} else {
				skull.jaw(level.getGameTime());
			}
			return true;
		}
		return false;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return part(state) == MASTER ? new ColossalSkullBlockEntity(pos, state) : null;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(POWERED);
	}
}
