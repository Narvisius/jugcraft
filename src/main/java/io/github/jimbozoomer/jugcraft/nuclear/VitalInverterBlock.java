package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A DC equipment room's vital inverter (round 26, 8g; older rooms round 3, Justin: "Inverter bypass switch",
 * "Annunciator buttons"): its manual bypass switch turns between NORMAL (the static switch carries the load on the
 * inverter, or the bypass with the inverter down) and BYPASS (the load on its regulating transformer whatever the
 * inverter does); its DC INPUT breaker switches; its annunciator's ACK, RESET and TEST buttons (DcRoom). A click anywhere
 * else says how it stands.
 *
 * <p>Where things are: tools/plant_panels.py vital_inverter (model pixels on the piece facing north, its door's face at
 * z 15).
 */
public class VitalInverterBlock extends LivePanelBlock {
	/** The DC input breaker's middle; the manual bypass switch's area (x0, y0, x1, y1); the buttons' column and rows. */
	static final double BREAKER = 11.5, BUTTON_X = 12.1;
	static final double[] BYPASS = {2.7, 4.9, 5.3, 7.8}, BUTTONS = {11.9, 10.3, 8.7};

	public VitalInverterBlock(Properties properties) {
		super(properties, 1, 2, 4);
	}

	public static boolean bypassAt(double x, double y) {
		return x >= BYPASS[0] && x <= BYPASS[2] && y >= BYPASS[1] && y <= BYPASS[3];
	}

	public static boolean breakerAt(double x, double y) {
		return DcRoom.breakerAt(x, y, BREAKER);
	}

	public static int buttonAt(double x, double y) {
		return DcRoom.buttonAt(x, y, BUTTON_X, BUTTONS);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		Direction facing = state.getValue(FACING);
		BlockPos origin = pos.subtract(offset(facing, state.getValue(partProperty())));
		if (!(level.getBlockEntity(origin) instanceof LivePanelBlockEntity inv)) {
			return InteractionResult.PASS;
		}
		ServerLevel server = (ServerLevel) level;
		String name = "Inverter " + DcRoom.LABELS[LivePanelBlockEntity.variant(state) % 4];
		double[] at = local(facing, origin, hit.getLocation());
		boolean front = at[2] > 14;
		int button = front ? buttonAt(at[0], at[1]) : -1;
		if (front && breakerAt(at[0], at[1])) {
			inv.dcFlags ^= DcRoom.DC_OPEN;
			boolean closed = (inv.dcFlags & DcRoom.DC_OPEN) == 0;
			inv.syncNow(server);
			level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, closed ? 1.5F : 1.2F);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.inverter_dc", name, DcRoom.state(closed)));
		} else if (front && bypassAt(at[0], at[1])) {
			inv.mode = inv.mode == DcRoom.BYPASS ? DcRoom.NORMAL : DcRoom.BYPASS;
			inv.syncNow(server);
			level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.8F, 0.7F);
			DcRoom.Inverter s = DcRoom.inverter(level, inv);
			player.sendOverlayMessage(Component.translatable(inv.mode == DcRoom.BYPASS
					? (s.bypassOk() ? "message.jugcraft.nuclear.inverter_bypass" : "message.jugcraft.nuclear.inverter_bypass_dead")
					: "message.jugcraft.nuclear.inverter_normal", name));
		} else if (button >= 0) {
			DcRoom.button(server, inv, button, pos, player, name);
		} else {
			DcRoom.Inverter s = DcRoom.inverter(level, inv);
			String out = s.onInv() ? "on the inverter" : s.onBypass() ? (inv.mode == DcRoom.BYPASS ? "on manual bypass" : "on the bypass") : "NO OUTPUT";
			if (s.output()) {
				out += String.format(Locale.ROOT, ", %.0f A", DcRoom.inverterLoad(level, inv));
			}
			out += !s.dc() ? ((inv.dcFlags & DcRoom.DC_OPEN) != 0 ? "; DC input breaker open" : "; no DC") : "";
			out += s.bypassOk() ? "" : "; bypass dead";
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.inverter_status", name, out, DcRoom.alarms(inv)));
		}
		return InteractionResult.SUCCESS_SERVER;
	}
}
