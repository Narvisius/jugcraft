package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Ferris Wheel's booth (fall addition 27): a loading platform with the operator's lever and gauge, the one real block
 * of the wheel. Placed facing whoever places it, where the wheel has room ({@link #clear}: {@value #WIDTH} blocks across,
 * {@value #HEIGHT} up and {@value #DEPTH} through, all of it air or plants), it raises the {@link FerrisWheel} over
 * itself. Using it boards the car at the bottom ({@link FerrisWheel#board}). Its block entity takes kinetic energy from
 * a touching shaft, gearbox or source and hands it to the wheel ({@link FerrisWheelBlockEntity}). Breaking it takes the
 * wheel down too, setting any riders on the ground; it drops itself.
 */
public class FerrisWheelBlock extends HorizontalDirectionalBlock implements EntityBlock {
	public static final String ID = "ferris_wheel";
	public static final int WIDTH = 15;
	public static final int HEIGHT = 16;
	public static final int DEPTH = 3;

	public FerrisWheelBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	/**
	 * Whether the wheel has room over a booth at {@code booth} facing {@code facing}: every block across, up and through
	 * the space it turns in, but the booth's own, can be replaced (air, grass, flowers), and none is outside the world.
	 */
	public static boolean clear(Level level, BlockPos booth, Direction facing) {
		Direction across = facing.getClockWise();
		int half = WIDTH / 2;
		int through = DEPTH / 2;
		for (int a = -half; a <= half; a++) {
			for (int up = 0; up < HEIGHT; up++) {
				for (int t = -through; t <= through; t++) {
					if (a == 0 && up == 0 && t == 0) {
						continue;
					}
					BlockPos pos = booth.relative(across, a).above(up).relative(facing, t);
					if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.getBlockState(pos).canBeReplaced()) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/** The wheels standing over the booth at {@code pos}. */
	public static List<FerrisWheel> wheels(Level level, BlockPos pos) {
		return level.getEntitiesOfClass(FerrisWheel.class, new AABB(pos).inflate(0.5), wheel -> wheel.booth().equals(pos));
	}

	/** Facing whoever places it, if the wheel has room. */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection().getOpposite();
		if (!clear(context.getLevel(), context.getClickedPos(), facing)) {
			if (context.getPlayer() instanceof ServerPlayer player) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.ferris_wheel.no_room", WIDTH, HEIGHT, DEPTH));
			}
			return null;
		}
		return defaultBlockState().setValue(FACING, facing);
	}

	/** Raises the wheel over the booth. */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level instanceof ServerLevel server && wheels(server, pos).isEmpty()) {
			FerrisWheel.raise(server, pos, state.getValue(FACING));
		}
	}

	/** Boards the car at the bottom (raising the wheel again first, should it have gone). */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer rider)) {
			return InteractionResult.SUCCESS;
		}
		List<FerrisWheel> found = wheels(server, pos);
		FerrisWheel wheel = found.isEmpty() ? FerrisWheel.raise(server, pos, state.getValue(FACING)) : found.get(0);
		if (wheel != null) {
			wheel.board(rider);
		}
		return InteractionResult.SUCCESS;
	}

	/** The booth gone, the wheel goes: its riders are set down, and it is taken away. */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).getBlock() instanceof FerrisWheelBlock) {
			return;
		}
		for (FerrisWheel wheel : wheels(level, pos)) {
			wheel.ejectPassengers();
			wheel.discard();
		}
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FerrisWheelBlockEntity(pos, state);
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
