package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.energy.EnergyConnectable;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A String Light Hook: a small iron post with a pumpkin bulb, fixed to a floor, wall or ceiling like a lever.
 * Jack-o'-Lantern String Lights ({@link StringLightsItem}) are strung from one hook to another. A hook lights
 * (light {@value #LIGHT}) while it has a redstone signal, or while the electric network feeds it
 * ({@link StringLightHookBlockEntity}); cables connect to it. Sneak-use it with an empty hand to take down its strand.
 */
public class StringLightHookBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock, EnergyConnectable {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	public static final int LIGHT = 10;
	private static final VoxelShape FLOOR = Block.box(5.0, 0.0, 5.0, 11.0, 8.0, 11.0);
	private static final VoxelShape CEILING = Block.box(5.0, 8.0, 5.0, 11.0, 16.0, 11.0);
	/** On a wall, by the direction the hook points out (it is fixed to the opposite side). */
	private static final Map<Direction, VoxelShape> WALL = Map.of(
			Direction.NORTH, Block.box(5.0, 5.0, 8.0, 11.0, 11.0, 16.0), Direction.SOUTH, Block.box(5.0, 5.0, 0.0, 11.0, 11.0, 8.0),
			Direction.WEST, Block.box(8.0, 5.0, 5.0, 16.0, 11.0, 11.0), Direction.EAST, Block.box(0.0, 5.0, 5.0, 8.0, 11.0, 11.0));

	public StringLightHookBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FACE, AttachFace.WALL).setValue(LIT, false));
	}

	public static int light(BlockState state) {
		return state.getValue(LIT) ? LIGHT : 0;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACE)) {
			case FLOOR -> FLOOR;
			case CEILING -> CEILING;
			case WALL -> WALL.get(state.getValue(FACING));
		};
	}

	/** Sneak-use with an empty hand takes the strand down from this hook (it drops as an item). */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StringLightHookBlockEntity hook && hook.link() != null) {
			hook.unstring(true);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.string_lights.taken_down"));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
		super.neighborChanged(state, level, pos, neighbor, orientation, moved);
		if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof StringLightHookBlockEntity hook) {
			hook.update(server);
		}
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new StringLightHookBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != JugcraftAgriculture.STRING_LIGHT_HOOK_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((StringLightHookBlockEntity) entity).serverTick((ServerLevel) tickLevel);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACE, FACING, LIT);
	}
}
