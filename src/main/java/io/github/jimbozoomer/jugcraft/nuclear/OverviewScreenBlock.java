package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The plant overview screen (round 34; tools/overview_display.py its display, tools/overview_screen.py its pages): a
 * slim LCD three blocks across and two high on a wall, showing the plant live (OverviewScreenBlockEntity at its bottom
 * middle block, client/OverviewScreenRenderer.java). A click on a building on its site map shows that building's page
 * for 30 seconds; on a page, SITE MAP goes back.
 */
public class OverviewScreenBlock extends MultiFurnitureBlock implements EntityBlock {
	/** The display's depth off the wall (its bezel's face, so a click lands on the picture). */
	static final double DEPTH = 1.95;
	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	static {
		for (Direction d : Direction.Plane.HORIZONTAL) {
			SHAPES.put(d, turned(new double[] {0, 0, 0, 16, 16, DEPTH}, d));
		}
	}

	public OverviewScreenBlock(Properties properties) {
		super(properties, 3, 2, true);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(partProperty()) == origin() ? new OverviewScreenBlockEntity(pos, state) : null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel) || type != PlantInterior.OVERVIEW_ENTITY) {
			return null;
		}
		return (BlockEntityTicker<T>) (BlockEntityTicker<OverviewScreenBlockEntity>) (l, pos, s, screen) -> screen.serverTick((ServerLevel) l);
	}

	/**
	 * Where a point in the world is on the screen, in its board coordinates: x across from its left edge as you face it,
	 * y up from its bottom (origin: the bottom middle block, facing: the way its viewer looks).
	 */
	static double[] local(Direction facing, BlockPos origin, Vec3 at) {
		Direction right = facing.getClockWise();
		double rx = at.x - (origin.getX() + 0.5), rz = at.z - (origin.getZ() + 0.5);
		return new double[] {rx * right.getStepX() + rz * right.getStepZ() + 1.5, at.y - origin.getY()};
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		Direction facing = state.getValue(FACING);
		BlockPos origin = pos.subtract(offset(facing, state.getValue(partProperty())));
		if (!(level.getBlockEntity(origin) instanceof OverviewScreenBlockEntity screen)) {
			return InteractionResult.PASS;
		}
		double[] at = local(facing, origin, hit.getLocation());
		level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.3F, 1.8F);
		player.sendOverlayMessage(screen.click((ServerLevel) level, at[0], at[1]));
		return InteractionResult.SUCCESS_SERVER;
	}
}
