package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * An operations office piece (round 26, #13: OpsOffice): the equipment out-of-service board, a work control console,
 * the shift schedule board, the master clock, the radio charging rack or the PPE shelf. Its hitbox follows what it is (a
 * desk, or a board or rack on its wall). Right-clicking one says what it shows; sneaking and right-clicking a console
 * with an empty hand turns it to its other positions (work control, tagout).
 */
public class OpsPanelBlock extends LivePanelBlock {
	static final String[] CONSOLE_LABELS = {"WORK CONTROL", "TAGOUT"};

	public OpsPanelBlock(Properties properties, int width, int rows, int variants) {
		super(properties, width, rows, variants);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		double[] box = switch (OpsOffice.kind(state)) {
			case OpsOffice.CONSOLE -> new double[] {0, 0, 1, 16, 13, 16};
			case OpsOffice.HUB -> new double[] {0, 0, 0, 16, 16, 2.6};
			case OpsOffice.SCHEDULE -> new double[] {0, 0, 0, 16, 16, 1.6};
			case OpsOffice.CLOCK -> new double[] {1.2, 1.2, 0, 14.8, 14.8, 2.2};
			case OpsOffice.RADIO -> new double[] {0.6, 1.4, 0, 15.4, 15.8, 6.0};
			case OpsOffice.PPE -> new double[] {0, 1.0, 0, 16, 15.0, 9.0};
			default -> new double[] {0, 0, 0, 16, 16, 16};
		};
		return turned(box, state.getValue(FACING));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		Direction facing = state.getValue(FACING);
		BlockPos origin = pos.subtract(offset(facing, state.getValue(partProperty())));
		IntegerProperty v = variantProperty();
		if (player.isShiftKeyDown() && v != null && variants > 1) {
			int next = (LivePanelBlockEntity.variant(state) + 1) % variants;
			for (int p = 0; p < width * rows; p++) {
				BlockPos at = origin.offset(offset(facing, p));
				BlockState s = level.getBlockState(at);
				if (s.getBlock() == this) {
					level.setBlock(at, s.setValue(v, next), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
				}
			}
			level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.6F, 1.2F);
			String label = next < CONSOLE_LABELS.length ? CONSOLE_LABELS[next] : String.valueOf(next + 1);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.tsc_relabel", label));
			return InteractionResult.SUCCESS_SERVER;
		}
		String text = level.getBlockEntity(origin) instanceof LivePanelBlockEntity panel ? OpsOffice.describe(panel, (ServerLevel) level)
				: "Nothing to show";
		player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.tsc", text));
		return InteractionResult.SUCCESS_SERVER;
	}
}
