package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

/**
 * The Haunted Portrait: a gilt-framed painting that hangs on a wall (it falls if the wall goes). Four sitters
 * ({@link Portrait}); sneak-use it with an empty hand to change which. The client draws the pupils so that they follow
 * whoever is looking (client/HauntedPortraitRenderer.java); at night they glow red. Purely decoration, all year.
 */
public class HauntedPortraitBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<Portrait> PORTRAIT = EnumProperty.create("portrait", Portrait.class);
	/** By the direction the painting faces (it hangs on the opposite side). */
	private static final Map<Direction, VoxelShape> SHAPES = Map.of(
			Direction.NORTH, Block.box(0.0, 0.0, 14.0, 16.0, 16.0, 16.0), Direction.SOUTH, Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 2.0),
			Direction.WEST, Block.box(14.0, 0.0, 0.0, 16.0, 16.0, 16.0), Direction.EAST, Block.box(0.0, 0.0, 0.0, 2.0, 16.0, 16.0));

	/** The sitters, each with its eyes as (x, y, width, height) in its 16x16 texture (tools/agriculture.py HAUNTED_PORTRAIT). */
	public enum Portrait implements StringRepresentable {
		LADY("lady", List.of(new int[] {6, 6, 2, 1}, new int[] {9, 6, 2, 1})),
		CAPTAIN("captain", List.of(new int[] {5, 6, 2, 1}, new int[] {9, 6, 2, 1})),
		CAT("cat", List.of(new int[] {5, 7, 2, 2}, new int[] {9, 7, 2, 2})),
		OWL("owl", List.of(new int[] {4, 5, 3, 3}, new int[] {9, 5, 3, 3}));

		private final String name;
		public final List<int[]> eyes;

		Portrait(String name, List<int[]> eyes) {
			this.name = name;
			this.eyes = eyes;
		}

		public Portrait next() {
			return values()[(ordinal() + 1) % values().length];
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public HauntedPortraitBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PORTRAIT, Portrait.LADY));
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

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			Portrait next = state.getValue(PORTRAIT).next();
			level.setBlock(pos, state.setValue(PORTRAIT, next), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.PAINTING_PLACE, SoundSource.BLOCKS, 0.8F, 0.8F);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.haunted_portrait." + next.getSerializedName().toLowerCase(Locale.ROOT)));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new HauntedPortraitBlockEntity(pos, state);
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
		builder.add(FACING, PORTRAIT);
	}
}
