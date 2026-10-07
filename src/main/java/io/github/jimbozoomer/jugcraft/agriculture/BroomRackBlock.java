package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Broom Rack (Halloween decorations batch 17): a dark oak rail on a wall with {@value #PEGS} brass pegs. Each peg
 * holds and shows one broom (the item tag {@code jugcraft:brooms}: Witch's Brooms, Enchanted Brooms and Flying
 * Broomsticks); use a peg with a broom to hang it, and again to take it down. It needs its wall.
 */
public class BroomRackBlock extends Block implements EntityBlock, ShowcaseBlockEntity.Showcase {
	public static final int PEGS = 3;
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final TagKey<Item> BROOMS = TagKey.create(Registries.ITEM, Jugcraft.id("brooms"));
	/** The pegs across the rail (pixels, for a rack facing north) and the boundaries between them. */
	public static final float[] PEG_X = {3.5F, 8.0F, 12.5F};
	private static final VoxelShape[] SHAPES = {Block.box(0.5, 9.0, 9.6, 15.5, 13.6, 16.0), Block.box(0.0, 9.0, 0.5, 6.4, 13.6, 15.5),
			Block.box(0.5, 9.0, 0.0, 15.5, 13.6, 6.4), Block.box(9.6, 9.0, 0.5, 16.0, 13.6, 15.5)};

	public BroomRackBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public int places() {
		return PEGS;
	}

	@Override
	public boolean accepts(ItemStack stack) {
		return stack.is(BROOMS);
	}

	@Override
	public SoundEvent putSound() {
		return SoundEvents.WOOD_HIT;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ShowcaseBlockEntity(pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[switch (state.getValue(FACING)) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		}];
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		for (Direction direction : context.getNearestLookingDirections()) {
			if (direction.getAxis().isHorizontal()) {
				BlockState state = defaultBlockState().setValue(FACING, direction.getOpposite());
				if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
					return state;
				}
			}
		}
		return null;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		BlockPos behind = pos.relative(facing.getOpposite());
		return level.getBlockState(behind).isFaceSturdy(level, behind, facing);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		return !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
	}

	/** The peg a hit on the rack aims at. */
	public static int peg(BlockState state, BlockPos pos, Vec3 at) {
		double[] local = ShowcaseBlockEntity.local(state.getValue(FACING), (at.x - pos.getX()) * 16, (at.z - pos.getZ()) * 16);
		return local[0] < (PEG_X[0] + PEG_X[1]) / 2 ? 0 : local[0] < (PEG_X[1] + PEG_X[2]) / 2 ? 1 : 2;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		return level.getBlockEntity(pos) instanceof ShowcaseBlockEntity rack ? rack.use(peg(state, pos, hit.getLocation()), stack, player, hand, this)
				: InteractionResult.PASS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.getMainHandItem().isEmpty() || !(level.getBlockEntity(pos) instanceof ShowcaseBlockEntity rack)) {
			return InteractionResult.PASS;
		}
		InteractionResult result = rack.use(peg(state, pos, hit.getLocation()), ItemStack.EMPTY, player, InteractionHand.MAIN_HAND, this);
		return result == InteractionResult.TRY_WITH_EMPTY_HAND ? InteractionResult.PASS : result;
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
