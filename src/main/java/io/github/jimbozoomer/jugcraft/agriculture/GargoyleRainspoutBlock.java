package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Gargoyle Rainspout (Halloween decorations batch 18): a grotesque head on a long gutter-spout, hung on a wall, its
 * mouth a block out from it. When it rains on the roof above it, it pours a stream from its mouth ({@link #POURING},
 * drawn by the client); a cauldron (vanilla's, the Bubbling Cauldron or the Horned Skull Cauldron) up to {@value #REACH}
 * blocks under its mouth fills by a level every {@value #FILL_TICKS} ticks, as under dripstone. It checks the weather
 * once every {@value #CHECK_TICKS} ticks.
 */
public class GargoyleRainspoutBlock extends HorizontalDirectionalBlock implements EntityBlock {
	public static final int REACH = 4;
	public static final int FILL_TICKS = 200;
	public static final int CHECK_TICKS = 20;
	/** How far down the client draws the stream, at most. */
	public static final int STREAM = 8;
	public static final BooleanProperty POURING = BooleanProperty.create("pouring");

	public GargoyleRainspoutBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POURING, false));
	}

	/** Hung on the face aimed at, sticking out of it; only on the side of a block. */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		if (!face.getAxis().isHorizontal()) {
			return null;
		}
		BlockState state = defaultBlockState().setValue(FACING, face);
		return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		BlockPos wall = pos.relative(facing.getOpposite());
		return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return LongDecorationBlock.turned(new double[] {4.0, 4.0, 0.0, 12.0, 13.0, 16.0}, state.getValue(FACING));
	}

	/** Whether it is raining on the roof over {@code pos}: on the highest block above it, open to the sky, in rain. */
	public static boolean rainingOver(Level level, BlockPos pos) {
		int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
		return level.isRainingAt(new BlockPos(pos.getX(), Math.max(top, pos.getY() + 1), pos.getZ()));
	}

	/** The block under the mouth of a spout at {@code spout} the stream lands on: the first that is not air, up to {@value #REACH} down. */
	public static @Nullable BlockPos landing(BlockGetter level, BlockPos spout, Direction facing, int reach) {
		BlockPos mouth = spout.relative(facing);
		for (int down = 1; down <= reach; down++) {
			BlockPos at = mouth.below(down);
			if (!level.getBlockState(at).isAir()) {
				return at;
			}
		}
		return null;
	}

	/**
	 * Pours a level of water into the cauldron under the mouth of the spout at {@code spout}, if there is one within
	 * reach with room for water. Returns whether it filled one.
	 */
	public static boolean pour(Level level, BlockPos spout, Direction facing) {
		BlockPos at = landing(level, spout, facing, REACH);
		if (at == null) {
			return false;
		}
		BlockState state = level.getBlockState(at);
		BlockState filled = null;
		if (state.is(Blocks.CAULDRON)) {
			filled = Blocks.WATER_CAULDRON.defaultBlockState();
		} else if (state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) < LayeredCauldronBlock.MAX_FILL_LEVEL) {
			filled = state.setValue(LayeredCauldronBlock.LEVEL, state.getValue(LayeredCauldronBlock.LEVEL) + 1);
		} else if (state.getBlock() instanceof BubblingCauldronBlock && state.getValue(BubblingCauldronBlock.CONTENTS) == BubblingCauldronBlock.Brew.EMPTY) {
			filled = state.setValue(BubblingCauldronBlock.CONTENTS, BubblingCauldronBlock.Brew.WATER);
		} else if (state.getBlock() instanceof HornedSkullCauldronBlock && level.getBlockEntity(at) instanceof HornedSkullCauldronBlockEntity pot) {
			int levels = state.getValue(HornedSkullCauldronBlock.LEVEL);
			if (levels < HornedSkullCauldronBlock.LEVELS && (levels == 0 || pot.isWater())) {
				pot.setContents(new PotionContents(Potions.WATER));
				filled = state.setValue(HornedSkullCauldronBlock.LEVEL, levels + 1).setValue(HornedSkullCauldronBlock.POTION, false);
			}
		}
		if (filled == null) {
			return false;
		}
		level.setBlock(at, filled, Block.UPDATE_ALL);
		level.playSound(null, at, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON, SoundSource.BLOCKS, 1.0F, 0.9F);
		level.gameEvent(null, GameEvent.BLOCK_CHANGE, at);
		return true;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new GargoyleRainspoutBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != JugcraftAgriculture.GARGOYLE_RAINSPOUT_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((GargoyleRainspoutBlockEntity) entity).serverTick((ServerLevel) tickLevel, pos, tickState);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, POURING);
	}
}
