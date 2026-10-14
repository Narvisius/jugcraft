package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The control room HVAC room's wall monitors (round 26, #11: HvacRoom): each intake's radiation monitor and toxic gas
 * analyser (labelled A or B by train), and the refrigerant monitor by the chillers. Right-clicking one says what it
 * reads; sneaking and right-clicking it with an empty hand steps it to its other train's label.
 */
public class HvacMonitorBlock extends WallLivePanelBlock {
	public HvacMonitorBlock(Properties properties, int variants, double depth) {
		super(properties, variants, depth);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		Direction facing = state.getValue(FACING);
		BlockPos origin = pos.subtract(offset(facing, state.getValue(partProperty())));
		int variant = LivePanelBlockEntity.variant(state);
		IntegerProperty v = variantProperty();
		if (player.isShiftKeyDown() && v != null) {
			int next = (variant + 1) % variants;
			for (int p = 0; p < width * rows; p++) {
				BlockPos at = origin.offset(offset(facing, p));
				BlockState s = level.getBlockState(at);
				if (s.getBlock() == this) {
					level.setBlock(at, s.setValue(v, next), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
				}
			}
			level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.6F, 1.2F);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.relay_relabel", next == 0 ? "A" : "B"));
			return InteractionResult.SUCCESS_SERVER;
		}
		String train = variant == 0 ? "A" : "B";
		String text = switch (HvacRoom.kind(state)) {
			case HvacRoom.RAD -> {
				double cpm = level.getBlockEntity(origin) instanceof LivePanelBlockEntity panel ? panel.hvValue : HvacRoom.RAD_BACKGROUND;
				yield String.format(Locale.ROOT, "Radiation monitor RM-CR-1%s: %,.0f cpm (%s)", train, Math.max(HvacRoom.RAD_BACKGROUND, cpm),
						cpm >= HvacRoom.RAD_HIGH ? "HIGH: the HVAC goes to emergency" : cpm >= HvacRoom.RAD_ALERT ? "ALERT" : "normal");
			}
			case HvacRoom.TOX -> String.format(Locale.ROOT, "Toxic gas analyzer AT-CR-2%s: chlorine 0.0 ppm, ammonia 0 ppm (normal)", train);
			default -> "Refrigerant monitor RM-CR-3: R-134a under 5 ppm at both chillers (normal)";
		};
		player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.hvac_monitor", text));
		return InteractionResult.SUCCESS_SERVER;
	}
}
