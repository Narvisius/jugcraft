package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
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
 * A food display (the kitchen and cooking expansion's slice 2, {@link FoodDisplay}): a plate, a platter or a serving tray
 * set on a table to show food on. Use it with something to set one of it down where it is used (the plate has one place,
 * the platter and the tray a quarter each); use the place again to take it back. What is on it is kept like the Witch's
 * Workshop's displays ({@link ShowcaseBlockEntity}): saved, drawn by the client, spilled when it is broken, and read by a
 * comparator.
 */
public class FoodDisplayBlock extends Block implements EntityBlock, ShowcaseBlockEntity.Showcase {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	private final FoodDisplay display;
	private final SoundEvent sound;
	private final VoxelShape shape;

	public FoodDisplayBlock(FoodDisplay display, SoundEvent sound, Properties properties) {
		super(properties);
		this.display = display;
		this.sound = sound;
		shape = Block.box(display.inset, 0, display.inset, 16 - display.inset, display.tall, 16 - display.inset);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	public FoodDisplay display() {
		return display;
	}

	@Override
	public int places() {
		return display.places();
	}

	@Override
	public boolean accepts(ItemStack stack) {
		return true;
	}

	@Override
	public SoundEvent putSound() {
		return sound;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ShowcaseBlockEntity(JugcraftAgriculture.FOOD_DISPLAY_ENTITY, pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** The place a use at {@code at} aims at: the nearest of its layout to where the hit lies on it. */
	public int place(BlockState state, BlockPos pos, Vec3 at) {
		double[] local = ShowcaseBlockEntity.local(state.getValue(FACING), (at.x - pos.getX()) * 16, (at.z - pos.getZ()) * 16);
		int nearest = 0;
		double best = Double.MAX_VALUE;
		for (int i = 0; i < display.layout.length; i++) {
			double dx = local[0] - display.layout[i][0];
			double dz = local[1] - display.layout[i][1];
			if (dx * dx + dz * dz < best) {
				best = dx * dx + dz * dz;
				nearest = i;
			}
		}
		return nearest;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		return level.getBlockEntity(pos) instanceof ShowcaseBlockEntity shown ? shown.use(place(state, pos, hit.getLocation()), stack, player, hand, this)
				: InteractionResult.PASS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.getMainHandItem().isEmpty() || !(level.getBlockEntity(pos) instanceof ShowcaseBlockEntity shown)) {
			return InteractionResult.PASS;
		}
		InteractionResult result = shown.use(place(state, pos, hit.getLocation()), ItemStack.EMPTY, player, InteractionHand.MAIN_HAND, this);
		return result == InteractionResult.TRY_WITH_EMPTY_HAND ? InteractionResult.PASS : result;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return level.getBlockState(pos.below()).isSolid();
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		return direction == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	/** How full it is: 15 with every place taken. */
	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return level.getBlockEntity(pos) instanceof ShowcaseBlockEntity shown ? shown.count() * 15 / display.places() : 0;
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
