package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.energy.EnergyConnectable;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Fog Machine: a riveted dieselpunk cabinet with a brass fluid tank, a gauge and a grille nozzle. Use it with an
 * empty hand to switch it on or off (a redstone signal also switches it on); sneak-use it to change the fog's radius
 * ({@link FogMachineBlockEntity#RADII}). Running, it draws electricity ({@link FogMachineBlockEntity}) and rolls low fog
 * over the ground around it, drawn only on the screens of players near it. Cables connect to it.
 */
public class FogMachineBlock extends BaseEntityBlock implements EnergyConnectable {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty ENABLED = BooleanProperty.create("enabled");
	public static final BooleanProperty RUNNING = BooleanProperty.create("running");
	public static final IntegerProperty RADIUS = IntegerProperty.create("radius", 0, 3);
	public static final int LIGHT = 6;
	/** The cabinet, the tank on top, and the nozzle out of the front (by the way the front faces). */
	private static final VoxelShape BODY = Shapes.or(Block.box(2.0, 0.0, 3.0, 14.0, 10.0, 13.0), Block.box(4.0, 10.0, 4.0, 12.0, 15.0, 12.0));
	private static final Map<Direction, VoxelShape> SHAPES = Map.of(
			Direction.NORTH, Shapes.or(BODY, Block.box(6.0, 3.0, 0.0, 10.0, 7.0, 3.0)),
			Direction.SOUTH, Shapes.or(BODY, Block.box(6.0, 3.0, 13.0, 10.0, 7.0, 16.0)),
			Direction.WEST, Shapes.or(Block.box(3.0, 0.0, 2.0, 13.0, 10.0, 14.0), Block.box(4.0, 10.0, 4.0, 12.0, 15.0, 12.0), Block.box(0.0, 3.0, 6.0, 3.0, 7.0, 10.0)),
			Direction.EAST, Shapes.or(Block.box(3.0, 0.0, 2.0, 13.0, 10.0, 14.0), Block.box(4.0, 10.0, 4.0, 12.0, 15.0, 12.0), Block.box(13.0, 3.0, 6.0, 16.0, 7.0, 10.0)));

	public FogMachineBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ENABLED, false).setValue(RUNNING, false)
				.setValue(RADIUS, 1));
	}

	public static int light(BlockState state) {
		return state.getValue(RUNNING) ? LIGHT : 0;
	}

	/** The fog's radius in blocks for this state. */
	public static int radius(BlockState state) {
		return FogMachineBlockEntity.RADII[state.getValue(RADIUS)];
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		BlockState next;
		if (player.isSecondaryUseActive()) {
			next = state.setValue(RADIUS, (state.getValue(RADIUS) + 1) % FogMachineBlockEntity.RADII.length);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.fog_machine.radius", radius(next)));
		} else {
			next = state.setValue(ENABLED, !state.getValue(ENABLED));
			player.sendOverlayMessage(Component.translatable(next.getValue(ENABLED) ? "message.jugcraft.fog_machine.on" : "message.jugcraft.fog_machine.off"));
		}
		level.setBlock(pos, next, Block.UPDATE_ALL);
		level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, 0.8F);
		if (level.getBlockEntity(pos) instanceof FogMachineBlockEntity machine) {
			machine.update(server);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, Orientation orientation, boolean moved) {
		super.neighborChanged(state, level, pos, neighbor, orientation, moved);
		if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof FogMachineBlockEntity machine) {
			machine.update(server);
		}
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FogMachineBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (type != JugcraftAgriculture.FOG_MACHINE_ENTITY) {
			return null;
		}
		return level.isClientSide()
				? (tickLevel, pos, tickState, entity) -> ((FogMachineBlockEntity) entity).clientTick(tickLevel, tickState)
				: (tickLevel, pos, tickState, entity) -> ((FogMachineBlockEntity) entity).serverTick((ServerLevel) tickLevel);
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
		builder.add(FACING, ENABLED, RUNNING, RADIUS);
	}
}
