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
 * A Technical Support Center panel (round 26, #12: TscRoom): the classification board, a video wall display, a
 * console, the communications console, the NRC desk, a status board, the area radiation monitor or the frisking
 * station. Its hitbox follows what it is (a desk, or a board a few pixels out from its wall). Right-clicking one says
 * what the TSC has (the class and when it was declared, the reactor, the CR HVAC; a monitor its dose rate); sneaking
 * and right-clicking one with an empty hand steps a panel with labels on to its next (a video wall's display, a status
 * board's board, a console's positions).
 */
public class TscPanelBlock extends LivePanelBlock {
	/** Each kind's labels, as plants place them (tools/tsc_room.py). */
	static final String[][] LABELS = {{}, {"SPDS", "RAD MET", "ELEC EVENTS"}, {"OPS", "RAD", "ENGINEERING", "ELECTRICAL / MECHANICAL",
			"MAINTENANCE", "ADMIN / SECURITY", "SITE EMERGENCY MGR"}, {}, {}, {"STAFFING", "PRIORITIES", "EVENTS", "NRC"}, {}, {}};

	public TscPanelBlock(Properties properties, int width, int rows, int variants) {
		super(properties, width, rows, variants);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		int row = state.getValue(partProperty()) / width;
		double[] box = switch (TscRoom.kind(state)) {
			case TscRoom.CONSOLE, TscRoom.COMMS_DESK -> new double[] {0, 0, 1, 16, 13, 16};
			case TscRoom.NRC -> new double[] {0, 0, 1, 16, 12.2, 15};
			case TscRoom.VIDEO -> new double[] {0, 0, 0, 16, 16, 2.6};
			case TscRoom.HUB -> new double[] {0, 0, 0, 16, 16, 3.0};
			case TscRoom.BOARD -> new double[] {0, 0, 0, 16, 16, 3.2};
			case TscRoom.AREA -> row == 0 ? new double[] {2.4, 0, 0, 13.6, 16, 3.6} : new double[] {2.4, 0, 0, 13.6, 14, 3.6};
			case TscRoom.FRISKER -> row == 0 ? new double[] {1, 0, 3, 15, 12, 13} : new double[] {1.6, 4, 0, 14.4, 14, 0.6};
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
		int kind = TscRoom.kind(state);
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
			String label = kind >= 0 && next < LABELS[kind].length ? LABELS[kind][next] : String.valueOf(next + 1);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.tsc_relabel", label));
			return InteractionResult.SUCCESS_SERVER;
		}
		String text = level.getBlockEntity(origin) instanceof LivePanelBlockEntity panel ? TscRoom.describe(panel, (ServerLevel) level)
				: "Nothing to show";
		player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.tsc", text));
		return InteractionResult.SUCCESS_SERVER;
	}
}
