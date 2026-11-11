package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Document control's plan file (the administration building part 2; tools/admin_rooms.py plan_file_part): two blocks
 * across, waist high, five wide shallow drawers of drawings. Click a drawer to pull it out (the sheets in it), again to
 * push it back; DRAWER is the one out (1 to 5 from the bottom, 0 none).
 */
public class PlanFileBlock extends MultiFurnitureBlock {
	public static final IntegerProperty DRAWER = IntegerProperty.create("drawer", 0, 5);
	/** The drawers' fronts (model pixels up, tools/admin_rooms.py PF_Y) and what each holds (PF_SERIES). */
	static final double FIRST = 1.6, PITCH = 2.4;
	public static final String[] SERIES = {"M-100 P&ID RCS", "M-200 P&ID CVCS", "M-300 P&ID SECONDARY", "E-100 ONE-LINES", "C-100 CIVIL / STRUCT"};

	public PlanFileBlock(Properties properties) {
		super(properties, 2, 1, true, 1, true, false, 14.4);
		registerDefaultState(defaultBlockState().setValue(DRAWER, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(DRAWER);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		return state == null ? null : state.setValue(DRAWER, 0);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return turned(new double[] {0.2, 0, 0.8, 15.8, 14.4, 14.8}, state.getValue(FACING));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		double y = (hit.getLocation().y - pos.getY()) * 16;
		int k = (int) Math.floor((y - FIRST) / PITCH);
		if (k < 0 || k >= SERIES.length) {
			return InteractionResult.PASS;
		}
		int now = state.getValue(DRAWER) == k + 1 ? 0 : k + 1;
		Direction facing = state.getValue(FACING);
		BlockPos origin = pos.subtract(offset(facing, state.getValue(partProperty())));
		for (int p = 0; p < width * rows; p++) {
			BlockPos at = origin.offset(offset(facing, p));
			BlockState s = level.getBlockState(at);
			if (s.is(this)) {
				level.setBlock(at, s.setValue(DRAWER, now), Block.UPDATE_ALL);
			}
		}
		level.playSound(null, pos, now > 0 ? SoundEvents.BARREL_OPEN : SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, 0.5F, 1.5F);
		player.sendOverlayMessage(Component.translatable(now > 0 ? "message.jugcraft.nuclear.pf_open" : "message.jugcraft.nuclear.pf_shut", SERIES[k]));
		return InteractionResult.SUCCESS_SERVER;
	}
}
