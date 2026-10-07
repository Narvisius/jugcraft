package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Autumn Wreath: chestnut leaves wound into a ring with ears of ornamental corn and a spray of mums. It hangs on
 * the side of a block or on a door, facing out, and falls if what holds it goes. Use a mum on it to swap its flowers
 * for that colour ({@link #FLOWERS}); the mum is used.
 */
public class AutumnWreathBlock extends Block {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<Mums> FLOWERS = EnumProperty.create("flowers", Mums.class);
	/** By the direction it faces (it hangs on the opposite side). */
	private static final Map<Direction, VoxelShape> SHAPES = Map.of(
			Direction.NORTH, Block.box(1.0, 1.0, 14.0, 15.0, 15.0, 16.0), Direction.SOUTH, Block.box(1.0, 1.0, 0.0, 15.0, 15.0, 2.0),
			Direction.WEST, Block.box(14.0, 1.0, 1.0, 16.0, 15.0, 15.0), Direction.EAST, Block.box(0.0, 1.0, 1.0, 2.0, 15.0, 15.0));

	/** The wreath's mums, after the four mum colours. */
	public enum Mums implements StringRepresentable {
		YELLOW, ORANGE, RED, PURPLE;

		/** The mum item of this colour. */
		public String mum() {
			return getSerializedName() + "_mum";
		}

		/** The colour of the mum in {@code stack}, or null if it holds none. */
		public static @Nullable Mums of(ItemStack stack) {
			for (Mums mums : values()) {
				if (stack.is(JugcraftAgriculture.item(mums.mum()))) {
					return mums;
				}
			}
			return null;
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public AutumnWreathBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FLOWERS, Mums.ORANGE));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	/** It needs a sturdy side behind it, or a door. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		BlockPos behind = pos.relative(facing.getOpposite());
		BlockState holder = level.getBlockState(behind);
		return holder.is(BlockTags.DOORS) || holder.isFaceSturdy(level, behind, facing);
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

	/** A mum of another colour swaps the wreath's flowers for its own. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		Mums mums = Mums.of(stack);
		if (mums == null) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (mums == state.getValue(FLOWERS)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			stack.consume(1, player);
			level.setBlock(pos, state.setValue(FLOWERS, mums), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
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
		builder.add(FACING, FLOWERS);
	}
}
