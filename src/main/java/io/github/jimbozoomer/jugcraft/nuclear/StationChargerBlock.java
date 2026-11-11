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
 * A DC equipment room's battery charger (round 26, 8b; older rooms round 3, Justin: "Charger switches", "Annunciator
 * buttons"): its AC INPUT and DC OUTPUT breakers through the door's cut-outs switch (a click on one: its handle up ON,
 * down OFF); its MODE selector turns between FLOAT and EQUALIZE (EQUALIZE runs the EQ TIMER down from 24 game hours,
 * then it goes back to FLOAT by itself); its annunciator's ACK, RESET and TEST buttons (DcRoom). With its AC breaker
 * open nothing comes in from its supply (AC FAIL); with its DC breaker open it puts nothing out (CHARGER FAIL). A
 * click anywhere else says how it stands.
 *
 * <p>Where things are: tools/plant_panels.py station_charger (model pixels on the piece facing north: x across, y up
 * from the floor 0..32, its door's face at z 15).
 */
public class StationChargerBlock extends LivePanelBlock {
	/** The breakers' middles (AC INPUT, DC OUTPUT), the MODE knob (x, y, reach), the buttons' column and rows. */
	static final double[] BREAKERS = {4.5, 11.5}, MODE = {3.4, 10.0, 0.95}, BUTTONS = {16.55, 14.75, 12.95};
	static final double BUTTON_X = 12.1;

	public StationChargerBlock(Properties properties) {
		super(properties, 1, 2, 4);
	}

	public static boolean modeAt(double x, double y) {
		return Math.abs(x - MODE[0]) <= MODE[2] && Math.abs(y - MODE[1]) <= MODE[2];
	}

	/** The breaker at this point: 0 AC INPUT, 1 DC OUTPUT, -1 none. */
	public static int breakerAt(double x, double y) {
		return DcRoom.breakerAt(x, y, BREAKERS[0]) ? 0 : DcRoom.breakerAt(x, y, BREAKERS[1]) ? 1 : -1;
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
		if (!(level.getBlockEntity(origin) instanceof LivePanelBlockEntity c)) {
			return InteractionResult.PASS;
		}
		ServerLevel server = (ServerLevel) level;
		String name = "Battery charger " + DcRoom.LABELS[LivePanelBlockEntity.variant(state) % 4];
		double[] at = local(facing, origin, hit.getLocation());
		boolean front = at[2] > 14;
		int breaker = front ? breakerAt(at[0], at[1]) : -1, button = front ? buttonAt(at[0], at[1]) : -1;
		if (breaker >= 0) {
			int bit = breaker == 0 ? DcRoom.AC_OPEN : DcRoom.DC_OPEN;
			c.dcFlags ^= bit;
			boolean closed = (c.dcFlags & bit) == 0;
			c.syncNow(server);
			level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, closed ? 1.5F : 1.2F);
			player.sendOverlayMessage(Component.translatable(breaker == 0 ? "message.jugcraft.nuclear.charger_ac" : "message.jugcraft.nuclear.charger_dc",
					name, DcRoom.state(closed)));
		} else if (front && modeAt(at[0], at[1])) {
			long now = level.getGameTime();
			c.mode = c.mode == DcRoom.EQUALIZE ? DcRoom.FLOAT : DcRoom.EQUALIZE;
			c.eqUntil = c.mode == DcRoom.EQUALIZE ? now + DcRoom.EQ_TICKS : 0;
			c.syncNow(server);
			level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.6F, 0.9F);
			player.sendOverlayMessage(Component.translatable(c.mode == DcRoom.EQUALIZE ? "message.jugcraft.nuclear.charger_equalize"
					: "message.jugcraft.nuclear.charger_float", name));
		} else if (button >= 0) {
			DcRoom.button(server, c, button, pos, player, name);
		} else {
			String out = (c.dcFlags & DcRoom.AC_OPEN) != 0 ? "AC input breaker open" : !c.ac ? "no AC supply"
					: (c.dcFlags & DcRoom.DC_OPEN) != 0 ? "DC output breaker open"
					: String.format(Locale.ROOT, "%s, %.0f A", c.mode == DcRoom.EQUALIZE ? "EQUALIZE" : "FLOAT", DcRoom.chargerAmps(level, c));
			if (c.mode == DcRoom.EQUALIZE) {
				out += String.format(Locale.ROOT, " (%.1f h of equalize left)", DcRoom.eqHours(c, level.getGameTime()));
			}
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.charger_status", name, out, DcRoom.alarms(c)));
		}
		return InteractionResult.SUCCESS_SERVER;
	}
}
