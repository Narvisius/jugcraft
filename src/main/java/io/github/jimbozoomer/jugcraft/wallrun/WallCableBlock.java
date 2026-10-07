package io.github.jimbozoomer.jugcraft.wallrun;

import io.github.jimbozoomer.jugcraft.energy.CableBlock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** A cable run inside a made wall ({@link WallRuns}): part of the power network like a laid cable, at its cable's rate. */
public class WallCableBlock extends CableBlock implements EntityBlock, WallRun {
	private final Block carrier;

	public WallCableBlock(Properties properties, long rate, Block carrier) {
		super(properties, rate);
		this.carrier = carrier;
	}

	@Override
	public Block carrier() {
		return carrier;
	}

	@Override
	public boolean joins(BlockGetter level, BlockPos pos, Direction direction, BlockState neighbor) {
		return CableBlock.connectsTo(level, pos, direction, neighbor);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return WallRuns.sleeves(this, defaultBlockState(), context.getLevel(), context.getClickedPos());
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		BlockState joined = super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
		return joined.setValue(PROPERTY_BY_DIRECTION.get(direction), WallRuns.sleeve(this, level, pos, direction, neighborState));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		return WallRuns.destroyProgress(super.getDestroyProgress(state, player, level, pos), player, level, pos);
	}

	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
		return WallRuns.drops(super.getDrops(state, builder), builder);
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return WallRuns.clone(level, pos);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		WallRuns.placed(level, pos, this);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		WallRuns.tick(level, pos);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new WallRunBlockEntity(pos, state);
	}
}
