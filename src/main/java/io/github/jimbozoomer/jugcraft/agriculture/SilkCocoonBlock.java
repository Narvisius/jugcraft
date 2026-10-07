package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Silk Cocoon (Halloween decorations batch 19, the Spider's Larder): a wrapped, body-shaped bundle hanging on a thread
 * from a ceiling or a chain, swaying. It is a {@value SilkCocoonBlockEntity#SLOTS}-slot larder: open it and it wriggles
 * and groans faintly. At night, now and then (on a random tick), it twitches by itself. It falls, spilling what it
 * holds, without its ceiling. The bundle is drawn by the client (client/SilkCocoonRenderer.java).
 */
public class SilkCocoonBlock extends BaseEntityBlock {
	public static final int WRIGGLE_TICKS = 20;
	/** The block event that makes it wriggle (its parameter: 1 for a groan with it). */
	public static final int EVENT_WRIGGLE = 1;
	private static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);

	public SilkCocoonBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return Block.canSupportCenter(level, pos.above(), Direction.DOWN) || level.getBlockState(pos.above()).is(Blocks.IRON_CHAIN);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().canSurvive(context.getLevel(), context.getClickedPos()) ? defaultBlockState() : null;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		return direction == Direction.UP && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
	}

	/** Opened, it wriggles and groans. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SilkCocoonBlockEntity cocoon) {
			player.openMenu(cocoon);
			wriggle(level, pos, true);
		}
		return InteractionResult.SUCCESS;
	}

	static void wriggle(Level level, BlockPos pos, boolean groan) {
		level.blockEvent(pos, level.getBlockState(pos).getBlock(), EVENT_WRIGGLE, groan ? 1 : 0);
		level.playSound(null, pos, SoundEvents.WOOL_HIT, SoundSource.BLOCKS, 0.8F, 0.7F);
		if (groan) {
			level.playSound(null, pos, SoundEvents.ZOMBIE_AMBIENT, SoundSource.BLOCKS, 0.25F, 0.6F);
		}
	}

	/** At night, now and then, it twitches by itself. */
	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (MourningAngelBlock.night(level)) {
			wriggle(level, pos, false);
		}
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return true;
	}

	@Override
	protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
		if (id == EVENT_WRIGGLE && level.getBlockEntity(pos) instanceof SilkCocoonBlockEntity cocoon) {
			cocoon.mark(level.getGameTime());
			return true;
		}
		return super.triggerEvent(state, level, pos, id, param);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return level.getBlockEntity(pos) instanceof SilkCocoonBlockEntity cocoon
				? net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromContainer(cocoon) : 0;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SilkCocoonBlockEntity(pos, state);
	}
}
