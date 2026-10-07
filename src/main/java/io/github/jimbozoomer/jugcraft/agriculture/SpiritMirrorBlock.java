package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Spirit Mirror: an old silvered mirror in a gilt frame that hangs on a wall (it falls if the wall goes). By day
 * it is only a mirror. At night a pale face shows in the glass now and then, for {@value #VISIBLE} ticks in every
 * {@value #PERIOD}, fading in and out over {@value #FADE}, to anyone in front of it within {@value #RANGE} blocks
 * (drawn by the client, client/SpiritMirrorRenderer.java; each mirror keeps its own time). Look into it (use it) and
 * it tells you what you see.
 */
public class SpiritMirrorBlock extends BaseEntityBlock {
	public static final int PERIOD = 600;
	public static final int VISIBLE = 80;
	public static final int FADE = 20;
	public static final double RANGE = 8.0;
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	/** By the direction the mirror faces (it hangs on the opposite side). */
	private static final Map<Direction, VoxelShape> SHAPES = Map.of(
			Direction.NORTH, Block.box(1.0, 0.0, 14.0, 15.0, 16.0, 16.0), Direction.SOUTH, Block.box(1.0, 0.0, 0.0, 15.0, 16.0, 2.0),
			Direction.WEST, Block.box(14.0, 0.0, 1.0, 16.0, 16.0, 15.0), Direction.EAST, Block.box(0.0, 0.0, 1.0, 2.0, 16.0, 15.0));

	public SpiritMirrorBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	/**
	 * How much of the face shows in the mirror at {@code pos} (0 to 1) at {@code time} (ticks, with the partial tick):
	 * none by day; at night, for {@value #VISIBLE} ticks of each {@value #PERIOD}, fading in and out.
	 */
	public static float face(BlockPos pos, boolean night, float time) {
		if (!night) {
			return 0.0F;
		}
		float t = (time + Math.floorMod(pos.hashCode() * 17, PERIOD)) % PERIOD;
		if (t >= VISIBLE) {
			return 0.0F;
		}
		return Math.min(1.0F, Math.min(t, VISIBLE - t) / FADE);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DecorationBlockEntity(JugcraftAgriculture.SPIRIT_MIRROR_ENTITY, pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	/** It hangs on the face of a sturdy block behind it. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		BlockPos wall = pos.relative(facing.getOpposite());
		return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction side = context.getClickedFace();
		if (side.getAxis().isVertical()) {
			return null;
		}
		BlockState state = defaultBlockState().setValue(FACING, side);
		return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
	}

	/** Looking into it: by day your reflection; at night, for a moment, someone behind you. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			boolean night = MourningAngelBlock.night(level);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.spirit_mirror." + (night ? "night" : "day")));
			if (night) {
				level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 0.5F);
			}
		}
		return InteractionResult.SUCCESS;
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
