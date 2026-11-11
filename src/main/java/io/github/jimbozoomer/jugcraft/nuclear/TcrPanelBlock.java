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
 * A turbine control room cabinet (turbine hall part 3: TurbineControlRoom). Right-clicking one says what it shows of its
 * turbine; sneaking and right-clicking one that has labels (the I/O cabinets 1 and 2, the protection panels' systems A
 * and B) with an empty hand steps it to its next label.
 */
public class TcrPanelBlock extends LivePanelBlock {
	private final String[] labels;

	public TcrPanelBlock(Properties properties, int width, String... labels) {
		super(properties, width, 2, Math.max(1, labels.length));
		this.labels = labels;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		Direction facing = state.getValue(FACING);
		BlockPos origin = pos.subtract(offset(facing, state.getValue(partProperty())));
		IntegerProperty v = variantProperty();
		if (player.isShiftKeyDown() && v != null && player.getMainHandItem().isEmpty()) {
			int next = (state.getValue(v) + 1) % variants;
			for (int p = 0; p < width * rows; p++) {
				BlockPos at = origin.offset(offset(facing, p));
				BlockState s = level.getBlockState(at);
				if (s.getBlock() == this) {
					level.setBlock(at, s.setValue(v, next), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
				}
			}
			level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.6F, 1.2F);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.relay_relabel", labels[next]));
			return InteractionResult.SUCCESS_SERVER;
		}
		EquipmentBlockEntity e = level.getBlockEntity(origin) instanceof LivePanelBlockEntity panel ? TurbineControlRoom.turbine(panel, level) : null;
		TurbineControlRoom.Turbine t = TurbineControlRoom.Turbine.of(e);
		String label = labels.length > 0 ? labels[LivePanelBlockEntity.variant(state)] + ": " : "";
		String text;
		if (e == null || !t.present()) {
			text = label + "no turbine-generator within " + TurbineHall.FEED_REACH + " blocks with a reactor (its controllers have nothing to read)";
		} else {
			text = label + switch (TurbineControlRoom.kind(state)) {
				case 0 -> "both 125 VDC feeds and the 120 V UPS in, no ground fault";
				case 1 -> "controller R in control, S and T tracking; turbine " + (t.latched() ? "latched" : "tripped") + " at "
						+ String.format(Locale.ROOT, "%,.0f", t.rpm()) + " rpm";
				case 2 -> "all I/O packs healthy and talking to the controllers";
				case 3 -> String.format(Locale.ROOT, "%,.0f rpm, highest bearing vibration %.1f mils, thrust %+.0f mils", t.rpm(),
						maxVibration(t, level.getGameTime()), t.thrust());
				case 4 -> t.excited() ? String.format(Locale.ROOT, "field breaker 41 closed, %,.0f A field, %.1f kV", t.fieldKa() * 1000, t.genKv())
						: "field breaker 41 open (it closes at 95% speed)";
				default -> t.lockout() ? "86G lockout tripped (TRIP, 32 reverse power): it resets when the turbine latches again"
						: "all relays normal, 86G reset, generator breaker " + (t.online() ? "closed" : "open");
			};
		}
		player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.tcr_status", state.getBlock().getName(), text));
		return InteractionResult.SUCCESS_SERVER;
	}

	private static double maxVibration(TurbineControlRoom.Turbine t, double time) {
		double m = 0;
		for (int k = 0; k < 8; k++) {
			m = Math.max(m, t.vibration(k, time));
		}
		return m;
	}
}
