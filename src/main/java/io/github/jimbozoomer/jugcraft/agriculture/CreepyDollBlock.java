package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
 * The Creepy Doll: a porcelain doll in a faded velvet dress, sitting with its legs out, on a floor, shelf or fence
 * post (it falls if that goes). Its head never moves while anyone watches. But look away for {@value #UNSEEN_TICKS}
 * ticks and look back, and it has turned: toward you, or now and then (one time in {@value #ELSEWHERE_CHANCE})
 * somewhere else entirely. Each player sees it turn for themselves (client/CreepyDollRenderer.java). Wind it (use it)
 * and its music box plays a note.
 */
public class CreepyDollBlock extends BaseEntityBlock {
	public static final int UNSEEN_TICKS = 10;
	public static final int ELSEWHERE_CHANCE = 3;
	/** How far the head turns either way, in degrees. */
	public static final float MAX_TURN = 100.0F;
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 11.0, 13.0);
	/** Its music box's notes: a falling A minor arpeggio, as note-block notes. */
	private static final int[] NOTES = {15, 12, 8, 3};

	public CreepyDollBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	/**
	 * Where the head of the doll at {@code pos} has turned (degrees clockwise from straight ahead) the {@code glance}th
	 * time a viewer looks back at it, {@code toward} being the way that viewer is from its head: toward them, or one
	 * time in {@value #ELSEWHERE_CHANCE} far off to one side.
	 */
	public static float glance(BlockPos pos, int glance, float toward) {
		int hash = pos.hashCode() * 31 + glance * 977;
		if (Math.floorMod(hash, ELSEWHERE_CHANCE) == 0) {
			float away = (hash & 2) == 0 ? -1.0F : 1.0F;
			return Mth.clamp(toward + away * 90.0F, -MAX_TURN, MAX_TURN);
		}
		return Mth.clamp(toward, -MAX_TURN, MAX_TURN);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DecorationBlockEntity(JugcraftAgriculture.CREEPY_DOLL_ENTITY, pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** It sits on anything with a top to sit on: a floor, a slab, a shelf, a fence post. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return Block.canSupportCenter(level, pos.below(), Direction.UP);
	}

	/** It faces whoever placed it. */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		return direction == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
	}

	/** Winding it plays one note of its music box, falling down an arpeggio. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			int note = NOTES[(int) Math.floorMod(level.getGameTime() / 10, NOTES.length)];
			level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.6F, MusicBoxBlockEntity.pitch(note));
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
