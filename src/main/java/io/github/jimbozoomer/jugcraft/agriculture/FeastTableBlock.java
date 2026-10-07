package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Harvest Feast Table: a long trestle table of oak with an orange runner, built a length at a time; lengths placed
 * end to end along the same axis join into one table, with legs only at its ends ({@link Part}). Each length holds two
 * dishes ({@link FeastTableBlockEntity}):
 * <ul>
 * <li>use it holding food to serve it on the dish you point at, up to eight servings;</li>
 * <li>use it with an empty hand to eat a serving from the dish you point at: you get the food as if you ate it, and the
 * feast's blessing ({@link Feasts}), shared with everyone who has eaten at the table lately;</li>
 * <li>sneak and use it with an empty hand to take the dish back.</li>
 * </ul>
 */
public class FeastTableBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
	public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
	private static final String MESSAGES = "message.jugcraft.feast_table.";
	private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 13.0, 16.0);

	/** Where a length is in its table: on its own, at the start or end (along the axis) or in the middle. */
	public enum Part implements StringRepresentable {
		SINGLE("single"), START("start"), MIDDLE("middle"), END("end");

		private final String name;

		Part(String name) {
			this.name = name;
		}

		static Part of(boolean before, boolean after) {
			return before ? after ? MIDDLE : END : after ? START : SINGLE;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public FeastTableBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X).setValue(PART, Part.SINGLE));
	}

	/** Whether a length in {@code other} joins the table {@code state} is part of: a feast table along the same axis. */
	public static boolean joins(BlockState state, BlockState other) {
		return other.getBlock() instanceof FeastTableBlock && other.getValue(AXIS) == state.getValue(AXIS);
	}

	private BlockState shaped(BlockState state, LevelReader level, BlockPos pos) {
		Direction.Axis axis = state.getValue(AXIS);
		boolean before = joins(state, level.getBlockState(pos.relative(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE))));
		boolean after = joins(state, level.getBlockState(pos.relative(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE))));
		return state.setValue(PART, Part.of(before, after));
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getClockWise().getAxis());
		// Placed against another length's end, it carries on along that table.
		BlockState against = context.getLevel().getBlockState(context.getClickedPos().relative(context.getClickedFace().getOpposite()));
		if (against.getBlock() instanceof FeastTableBlock && context.getClickedFace().getAxis() == against.getValue(AXIS)) {
			state = state.setValue(AXIS, against.getValue(AXIS));
		}
		return shaped(state, context.getLevel(), context.getClickedPos());
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos,
			BlockState neighbor, RandomSource random) {
		return direction.getAxis() == state.getValue(AXIS) ? shaped(state, level, pos) : state;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FeastTableBlockEntity(pos, state);
	}

	/** The dish under {@code hit}: the half of the length nearer the negative or the positive end of the table. */
	public static int dishAt(BlockState state, BlockPos pos, BlockHitResult hit) {
		double along = state.getValue(AXIS) == Direction.Axis.X ? hit.getLocation().x - pos.getX() : hit.getLocation().z - pos.getZ();
		return along < 0.5 ? 0 : 1;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!FeastTableBlockEntity.food(stack)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FeastTableBlockEntity table) {
			int served = table.serve(dishAt(state, pos, hit), stack);
			if (served > 0) {
				stack.consume(served, player);
				level.playSound(null, pos, SoundEvents.DECORATED_POT_INSERT, SoundSource.BLOCKS, 0.7F, 1.2F);
				level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
			} else {
				player.sendOverlayMessage(Component.translatable(MESSAGES + "dish_taken"));
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.getMainHandItem().isEmpty()) {
			return InteractionResult.PASS;
		}
		if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer diner)
				|| !(level.getBlockEntity(pos) instanceof FeastTableBlockEntity table)) {
			return InteractionResult.SUCCESS;
		}
		int index = dishAt(state, pos, hit);
		if (player.isSecondaryUseActive()) {
			ItemStack dish = table.clear(index);
			if (!dish.isEmpty() && !player.getInventory().add(dish)) {
				Block.popResource(level, pos.above(), dish);
			}
			return InteractionResult.SUCCESS;
		}
		if (table.dish(index).isEmpty()) {
			player.sendOverlayMessage(Component.translatable(MESSAGES + "empty"));
			return InteractionResult.SUCCESS;
		}
		// The feast is worked out with this dish still on the table, then the serving is eaten as it would be by hand.
		Feasts.ate(server, pos, diner);
		ItemStack serving = table.takeServing(index);
		ItemStack left = serving.finishUsingItem(level, player);
		if (!left.isEmpty() && !ItemStack.isSameItem(left, serving) && !player.getInventory().add(left)) {
			Block.popResource(level, pos.above(), left);
		}
		level.gameEvent(player, GameEvent.EAT, pos);
		return InteractionResult.SUCCESS;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90
				? state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X) : state;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS, PART);
	}
}
