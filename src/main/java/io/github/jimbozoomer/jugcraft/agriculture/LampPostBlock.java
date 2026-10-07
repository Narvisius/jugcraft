package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The cemetery lamp post (graveyard pack 4): three blocks of cast iron, its lantern on the top one lit while it is dark
 * outside (light {@value #LIGHT}) and out by day. The lantern looks at the sky every {@value #CHECK_TICKS} ticks by a
 * scheduled tick, so it costs nothing in between. Placed whole facing the player, or not at all; breaking any block
 * breaks the post, dropping it once from its base.
 */
public class LampPostBlock extends Block {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, 2);
	public static final BooleanProperty LIT = BooleanProperty.create("lit");
	public static final int LIGHT = 15;
	public static final int CHECK_TICKS = 100;
	private static final VoxelShape[] SHAPES = {
			Shapes.or(Block.box(4.0, 0.0, 4.0, 12.0, 8.4, 12.0), Block.box(6.9, 8.4, 6.9, 9.1, 16.0, 9.1)),
			Block.box(6.5, 0.0, 6.5, 9.5, 16.0, 9.5),
			Shapes.or(Block.box(6.5, 0.0, 6.5, 9.5, 2.0, 9.5), Block.box(4.6, 2.0, 4.6, 11.4, 15.6, 11.4))};

	public LampPostBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, 0).setValue(LIT, false));
	}

	/** The light a block of it gives: its lantern's, while lit. */
	public static int light(BlockState state) {
		return state.getValue(PART) == 2 && state.getValue(LIT) ? LIGHT : 0;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Player player = context.getPlayer();
		for (int part = 1; part <= 2; part++) {
			BlockPos above = pos.above(part);
			if (level.isOutsideBuildHeight(above) || !level.getBlockState(above).canBeReplaced(BlockPlaceContext.at(context, above, Direction.UP))
					|| (player != null && !level.mayInteract(player, above))) {
				return null;
			}
		}
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(LIT, !level.isBrightOutside());
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide()) {
			level.setBlock(pos.above(), state.setValue(PART, 1), Block.UPDATE_ALL);
			level.setBlock(pos.above(2), state.setValue(PART, 2), Block.UPDATE_ALL);
		}
	}

	/** The lantern starts looking at the sky as soon as it is set. */
	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean movedByPiston) {
		super.onPlace(state, level, pos, old, movedByPiston);
		if (state.getValue(PART) == 2 && !old.is(this)) {
			level.scheduleTick(pos, this, CHECK_TICKS);
		}
	}

	/** Lit while it is dark outside; every block of the post shares the lantern's state. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(PART) != 2) {
			return;
		}
		boolean dark = !level.isBrightOutside();
		if (dark != state.getValue(LIT)) {
			for (int part = 0; part <= 2; part++) {
				BlockPos at = pos.below(2 - part);
				BlockState block = level.getBlockState(at);
				if (block.is(this) && block.getValue(PART) == part) {
					level.setBlock(at, block.setValue(LIT, dark), Block.UPDATE_ALL);
				}
			}
		}
		level.scheduleTick(pos, this, CHECK_TICKS);
	}

	/** Breaking any block breaks the post: the base drops it; the rest go without drops. */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(this)) {
			return; // Lit or put out, not removed.
		}
		int part = state.getValue(PART);
		BlockPos base = pos.below(part);
		if (part != 0) {
			BlockState below = level.getBlockState(base);
			if (below.is(this) && below.getValue(PART) == 0) {
				level.destroyBlock(base, true);
			}
			return;
		}
		for (int up = 1; up <= 2; up++) {
			BlockState above = level.getBlockState(pos.above(up));
			if (above.is(this) && above.getValue(PART) == up) {
				level.removeBlock(pos.above(up), false);
			}
		}
	}

	/** In creative, breaking an upper block takes the post away without dropping it. */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && player.getAbilities().instabuild && state.getValue(PART) != 0) {
			BlockPos base = pos.below(state.getValue(PART));
			if (level.getBlockState(base).is(this)) {
				level.removeBlock(base, false);
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[state.getValue(PART)];
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
		builder.add(FACING, PART, LIT);
	}
}
