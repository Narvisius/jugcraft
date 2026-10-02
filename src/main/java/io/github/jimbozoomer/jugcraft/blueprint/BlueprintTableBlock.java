package io.github.jimbozoomer.jugcraft.blueprint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Blueprint Table, a drafting station two blocks wide (an MBS placed by one item, like a bed): steel trestle
 * legs, a tilted drawing board with a blueprint taped on, a drafting arm, a pencil ledge, a shelf of drawings and
 * rolled plans, and a swing-arm lamp on the right. Right-click either half to open its screen.
 */
public class BlueprintTableBlock extends HorizontalDirectionalBlock {
	/** Which half: MAIN is where the player placed it (their left), SIDE the half to its right, with the lamp. */
	public enum Part implements StringRepresentable {
		MAIN("main"), SIDE("side");

		private final String name;

		Part(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
	/** The trestle and the board, up to the raised back edge of the board. */
	private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 12, 16), Block.box(0, 12, 6, 16, 16, 16));
	/** Opens the table screen on the client; set by the client entry point (no-op on a server). */
	public static java.util.function.Consumer<BlockPos> openScreen = pos -> {
	};

	public BlueprintTableBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, Part.MAIN));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, PART);
	}

	/** Where the other half is, seen from this one: SIDE is to the player's right as they face the board. */
	public static BlockPos partner(BlockState state, BlockPos pos) {
		Direction toSide = state.getValue(FACING).getCounterClockWise();
		return state.getValue(PART) == Part.MAIN ? pos.relative(toSide) : pos.relative(toSide.getOpposite());
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection().getOpposite();
		BlockPos side = context.getClickedPos().relative(facing.getCounterClockWise());
		Level level = context.getLevel();
		if (!level.getBlockState(side).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(side)) {
			return null;
		}
		return defaultBlockState().setValue(FACING, facing);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide()) {
			level.setBlock(partner(state, pos), state.setValue(PART, Part.SIDE), Block.UPDATE_ALL);
		}
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
		if (neighbourPos.equals(partner(state, pos))
				&& !(neighbour.is(this) && neighbour.getValue(PART) != state.getValue(PART) && neighbour.getValue(FACING) == state.getValue(FACING))) {
			return Blocks.AIR.defaultBlockState(); // the other half is gone: this one goes too (its loot drops only for the main half)
		}
		return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbour, random);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (level instanceof ServerLevel && state.getValue(PART) == Part.SIDE) {
			// Only the main half drops the table: break it here so a creative player's break drops nothing.
			BlockPos main = partner(state, pos);
			BlockState mainState = level.getBlockState(main);
			if (mainState.is(this) && mainState.getValue(PART) == Part.MAIN) {
				level.destroyBlock(main, !player.preventsBlockDrops(), player);
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			openScreen.accept(state.getValue(PART) == Part.MAIN ? pos : partner(state, pos));
		}
		return InteractionResult.SUCCESS;
	}
}
