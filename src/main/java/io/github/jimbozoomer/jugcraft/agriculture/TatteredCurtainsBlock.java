package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Tattered Curtains: ragged, moth-eaten cheesecloth on an iron rod, for windows and doorways. They hang at the back of
 * their block, against a wall or window behind them or under a lintel above; placed under one another they make one
 * drape ({@link #PART}: the rod at the top, the ragged hem at the bottom) of up to {@value #MAX_DROP} blocks. Use them
 * to draw the whole drape open or closed. Closed, they sway in a draft that nobody can find, more at night
 * ({@link #sway}, drawn by the client, client/TatteredCurtainsRenderer.java). Anyone walks through them.
 */
public class TatteredCurtainsBlock extends BaseEntityBlock {
	public static final int MAX_DROP = 8;
	public static final float SWAY = 1.0F;
	public static final float NIGHT_SWAY = 2.0F;
	public static final int SWAY_PERIOD = 70;
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
	public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
	/** By the direction they face (they hang at the back of the block, toward the opposite side). */
	private static final Map<Direction, VoxelShape> SHAPES = Map.of(
			Direction.NORTH, Block.box(0.0, 0.0, 12.0, 16.0, 16.0, 15.0), Direction.SOUTH, Block.box(0.0, 0.0, 1.0, 16.0, 16.0, 4.0),
			Direction.WEST, Block.box(12.0, 0.0, 0.0, 15.0, 16.0, 16.0), Direction.EAST, Block.box(1.0, 0.0, 0.0, 4.0, 16.0, 16.0));

	/** Where in its drape a block of curtain is: alone (rod and hem), the top (rod), between, or the bottom (hem). */
	public enum Part implements StringRepresentable {
		SINGLE("single"), TOP("top"), MIDDLE("middle"), BOTTOM("bottom");

		private final String name;

		Part(String name) {
			this.name = name;
		}

		public boolean top() {
			return this == SINGLE || this == TOP;
		}

		public boolean bottom() {
			return this == SINGLE || this == BOTTOM;
		}

		static Part of(boolean above, boolean below) {
			return above ? below ? MIDDLE : BOTTOM : below ? TOP : SINGLE;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public TatteredCurtainsBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false).setValue(PART, Part.SINGLE));
	}

	/**
	 * How far (in pixels, toward the room) the cloth of a closed drape stands out at {@code down} blocks below its rod
	 * and {@code across} (0 to 1) along it, at {@code time} (ticks with the partial tick): nothing at the rod, the most at
	 * the hem three blocks down and below; twice as much at night.
	 */
	public static float sway(BlockPos pos, boolean night, float time, float down, float across) {
		float most = night ? NIGHT_SWAY : SWAY;
		float reach = Math.min(1.0F, down / 3.0F);
		float phase = ((pos.getX() * 7 + pos.getZ() * 13) & 0xFF) / 40.0F;
		float wave = Mth.sin((time / SWAY_PERIOD) * Mth.TWO_PI + across * 3.0F + phase) * 0.7F
				+ Mth.sin((time / SWAY_PERIOD) * Mth.TWO_PI * 2.3F + across * 7.0F) * 0.3F;
		return most * reach * wave;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DecorationBlockEntity(JugcraftAgriculture.TATTERED_CURTAINS_ENTITY, pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	private static boolean drape(BlockState state, BlockState other) {
		return other.is(state.getBlock()) && other.getValue(FACING) == state.getValue(FACING);
	}

	/** They hang under more of the same drape, under a lintel, or before a wall or window. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockPos above = pos.above();
		BlockState over = level.getBlockState(above);
		return drape(state, over) || over.isFaceSturdy(level, above, Direction.DOWN)
				|| !level.getBlockState(pos.relative(state.getValue(FACING).getOpposite())).isAir();
	}

	/** Facing the room (away from the wall clicked, or toward the player), joining any drape above or below. */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Direction side = context.getClickedFace();
		Direction facing = side.getAxis().isHorizontal() ? side : context.getHorizontalDirection().getOpposite();
		BlockState over = level.getBlockState(pos.above());
		BlockState state = defaultBlockState().setValue(FACING, facing);
		if (drape(state, over)) {
			state = state.setValue(OPEN, over.getValue(OPEN));
		}
		if (length(level, pos, state) > MAX_DROP) {
			return null;
		}
		state = state.setValue(PART, Part.of(drape(state, over), drape(state, level.getBlockState(pos.below()))));
		return state.canSurvive(level, pos) ? state : null;
	}

	/** How long the drape would be with {@code state} at {@code pos}: the curtains above and below it, and it. */
	private static int length(BlockGetter level, BlockPos pos, BlockState state) {
		int length = 1;
		for (BlockPos at = pos.above(); length <= MAX_DROP && drape(state, level.getBlockState(at)); at = at.above()) {
			length++;
		}
		for (BlockPos at = pos.below(); length <= MAX_DROP && drape(state, level.getBlockState(at)); at = at.below()) {
			length++;
		}
		return length;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		if (!state.canSurvive(level, pos)) {
			return Blocks.AIR.defaultBlockState();
		}
		if (direction.getAxis().isVertical()) {
			return state.setValue(PART, Part.of(drape(state, level.getBlockState(pos.above())), drape(state, level.getBlockState(pos.below()))));
		}
		return state;
	}

	/** Draws the whole drape open, or closed. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			boolean open = !state.getValue(OPEN);
			BlockPos top = pos;
			for (int i = 0; i < MAX_DROP && drape(state, level.getBlockState(top.above())); i++) {
				top = top.above();
			}
			for (int i = 0; i < MAX_DROP && drape(state, level.getBlockState(top)); i++, top = top.below()) {
				level.setBlock(top, level.getBlockState(top).setValue(OPEN, open), Block.UPDATE_CLIENTS);
			}
			level.playSound(null, pos, SoundEvents.WOOL_STEP, SoundSource.BLOCKS, 0.8F, open ? 1.2F : 0.9F);
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
		builder.add(FACING, OPEN, PART);
	}
}
