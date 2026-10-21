package io.github.jimbozoomer.jugcraft.academy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public final class AcademyWardBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	// Reflection is separate from facing so asymmetric teacher/library positions survive template mirrors.
	public static final BooleanProperty REFLECTED = BooleanProperty.create("reflected");
	public AcademyWardBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(REFLECTED, false));
	}
	public static BlockPos offset(BlockPos origin, BlockState state, int x, int y, int z) {
		if (state.getValue(REFLECTED)) x = -x;
		Direction south = state.getValue(FACING).getOpposite();
		Direction east = south.getCounterClockWise();
		return origin.offset(east.getStepX()*x + south.getStepX()*z, y, east.getStepZ()*x + south.getStepZ()*z);
	}
	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, REFLECTED);
	}
	@Override protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}
	@Override protected BlockState mirror(BlockState state, Mirror mirror) {
		return mirror == Mirror.NONE ? state : state.setValue(FACING, mirror.mirror(state.getValue(FACING)))
				.cycle(REFLECTED);
	}
	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new AcademyWardBlockEntity(pos, state); }
	@Override public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return !level.isClientSide() && type == JugcraftAcademy.WARD_ENTITY
				? (l, p, s, e) -> ((AcademyWardBlockEntity)e).tick((ServerLevel)l) : null;
	}
	@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof AcademyWardBlockEntity ward) ward.clue(server);
		return InteractionResult.SUCCESS;
	}
}
