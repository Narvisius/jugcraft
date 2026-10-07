package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A Mooring Post (fall addition 29): a cast-iron bollard on a granite plinth with a winch. Using it ties the nearest hot-air
 * balloon within {@value HotAirBalloon#MOOR_REACH} blocks to it, or casts off the one it holds. A tethered balloon
 * rises and sways on its rope but goes no further than {@value HotAirBalloon#ROPE} blocks across from the post and
 * {@value HotAirBalloon#TETHER_HEIGHT} above it: rides at a fiesta. Breaking the post casts its balloon off.
 */
public class MooringPostBlock extends HorizontalDirectionalBlock {
	public static final String ID = "mooring_post";
	private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 4, 15), Block.box(3.5, 4, 3.5, 12.5, 15, 12.5));

	public MooringPostBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	/** The balloons moored to the post at {@code pos}. */
	public static List<HotAirBalloon> moored(Level level, BlockPos pos) {
		double r = HotAirBalloon.ROPE + HotAirBalloon.MOOR_REACH;
		return level.getEntitiesOfClass(HotAirBalloon.class, new AABB(pos).inflate(r, HotAirBalloon.TETHER_HEIGHT + r, r),
				balloon -> pos.equals(balloon.mooring()));
	}

	/** The nearest balloon within reach of the post at {@code pos} that isn't moored elsewhere, or null. */
	public static @Nullable HotAirBalloon nearest(Level level, BlockPos pos) {
		Vec3 post = Vec3.atCenterOf(pos);
		double reach = HotAirBalloon.MOOR_REACH;
		return level.getEntitiesOfClass(HotAirBalloon.class, new AABB(pos).inflate(reach),
						balloon -> balloon.mooring() == null && balloon.position().distanceTo(post) <= reach).stream()
				.min(Comparator.comparingDouble(balloon -> balloon.position().distanceToSqr(post))).orElse(null);
	}

	/** Casts off the balloon the post holds, or ties the nearest; returns the message for its user. */
	public static String toggle(Level level, BlockPos pos) {
		List<HotAirBalloon> held = moored(level, pos);
		if (!held.isEmpty()) {
			held.forEach(balloon -> balloon.moor(null));
			return "message.jugcraft.mooring.untied";
		}
		HotAirBalloon balloon = nearest(level, pos);
		if (balloon == null) {
			return "message.jugcraft.mooring.none";
		}
		balloon.moor(pos);
		return "message.jugcraft.mooring.tied";
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level instanceof ServerLevel server && player instanceof ServerPlayer user) {
			String message = toggle(server, pos);
			user.sendOverlayMessage(Component.translatable(message));
			if (!message.endsWith("none")) {
				server.playSound(null, pos, Midway.sound("entity.leash_knot.place", SoundEvents.WOOD_HIT), SoundSource.BLOCKS, 1.0F, 0.8F);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (!(level.getBlockState(pos).getBlock() instanceof MooringPostBlock)) {
			moored(level, pos).forEach(balloon -> balloon.moor(null));
		}
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
