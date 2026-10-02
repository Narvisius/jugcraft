package io.github.jimbozoomer.jugcraft.drone;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A hologram table section for the drone control room. Nine sections in a 3x3 (touching no other
 * sections) combine into one table ({@code part} 1 to 9, row by row from the north-west): a dark glass
 * top with a projector in the middle. The formed table links to the nearest drone terminal and the
 * client projects a small live map of that depot above it: platform, pads, supply pickup, and every
 * drone, docked or flying. It is drawn from the depot view the terminal already sends, so it costs the
 * server nothing extra.
 */
public class HoloTableBlock extends BaseEntityBlock implements DepotDisplayBlockEntity.Display {
	public static final int SIZE = 3;
	/** 0 = loose section; 1..9 = tile of a formed 3x3 table. */
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, SIZE * SIZE);
	/** The middle tile, with the projector. */
	public static final int PROJECTOR_PART = 1 + (SIZE / 2) * SIZE + SIZE / 2;
	/** Table height in pixels. */
	public static final int HEIGHT = 13;
	public static final int LINK_RANGE = 24;
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, HEIGHT, 16);

	public HoloTableBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(PART, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PART);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DepotDisplayBlockEntity(JugcraftDrones.HOLO_ENTITY, pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, JugcraftDrones.HOLO_ENTITY, DepotDisplayBlockEntity::serverTick);
	}

	@Override
	public boolean isAnchor(BlockState state) {
		return state.getValue(PART) == PROJECTOR_PART;
	}

	@Override
	public int linkRange() {
		return LINK_RANGE;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!level.isClientSide() && !oldState.is(this)) {
			reform(level, pos);
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		if (level.getBlockState(pos).is(this)) {
			return;
		}
		for (BlockPos next : new BlockPos[] {pos.north(), pos.south(), pos.east(), pos.west()}) {
			if (level.getBlockState(next).is(this)) {
				reform(level, next);
			}
		}
	}

	/** Forms the touching group of sections round {@code start} if it is exactly 3x3, else sets it loose. */
	static void reform(Level level, BlockPos start) {
		Set<BlockPos> group = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		group.add(start.immutable());
		queue.add(start.immutable());
		boolean tooBig = false;
		while (!queue.isEmpty()) {
			BlockPos at = queue.poll();
			for (BlockPos next : new BlockPos[] {at.north(), at.south(), at.east(), at.west()}) {
				if (!group.contains(next) && level.getBlockState(next).getBlock() instanceof HoloTableBlock) {
					if (group.size() >= 16) {
						tooBig = true;
						continue;
					}
					group.add(next.immutable());
					queue.add(next.immutable());
				}
			}
		}
		int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
		for (BlockPos pos : group) {
			minX = Math.min(minX, pos.getX());
			maxX = Math.max(maxX, pos.getX());
			minZ = Math.min(minZ, pos.getZ());
			maxZ = Math.max(maxZ, pos.getZ());
		}
		boolean formed = !tooBig && group.size() == SIZE * SIZE && maxX - minX + 1 == SIZE && maxZ - minZ + 1 == SIZE;
		for (BlockPos pos : group) {
			int part = formed ? 1 + (pos.getZ() - minZ) * SIZE + (pos.getX() - minX) : 0;
			BlockState state = level.getBlockState(pos);
			if (state.getValue(PART) != part) {
				level.setBlock(pos, state.setValue(PART, part), Block.UPDATE_CLIENTS);
			}
			if (part == PROJECTOR_PART && level.getBlockEntity(pos) instanceof DepotDisplayBlockEntity anchor) {
				anchor.relink();
			}
		}
	}
}
