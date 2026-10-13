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
 * A DC room's ground detector and battery monitor (round 26, 8f; Justin: "Healthy, with TEST", "Yes, with a screen",
 * "End of the equipment row"): a cabinet one block wide and two high at the end of the equipment row, labelled by train
 * (1A, 1B), reading the train's station battery (its placing block keeps it: LivePanelBlockEntity battery). Its screen
 * shows the string's volts and amps, its charge, temperature and state and each of its 60 cells' volts; its voltmeters
 * read each side of the 125 V bus to ground, half the bus each while neither side is grounded (no ground faults are
 * modelled: the bus is healthy). Holding right-click on its + BUS or - BUS test button puts a ground on that side
 * through the test resistor (the game uses the block again every 4 ticks while it is held, and each use holds the test
 * {@link #HOLD} ticks): that side's meter drops to nearly nothing and the other reads the whole bus, its GND lamp and
 * the ALARM lamp light, the screen says which side and the alarm beeps. Right-clicking anywhere else says how the bus
 * and the battery are. What it shows: client/EdgTelemetry.java groundDetector.
 *
 * <p>The buttons match tools/plant_panels.py GD_TESTS (model pixels on the piece facing north: x across, y up from the
 * floor 0..32, its door's face at z 15).
 */
public class GroundDetectorBlock extends LivePanelBlock {
	/** The test buttons' middles, + BUS then - BUS, and how far round them a press counts (model pixels). */
	static final double[][] TESTS = {{3.9, 5.7}, {10.1, 5.7}};
	static final double TEST_REACH = 1.0;
	/** How long one use holds the test (game ticks): held, the game uses the block again every 4. */
	public static final int HOLD = 6;
	/** The alarm's beeps while a test is on (at most every this many game ticks). */
	static final int BEEP = 10;

	public GroundDetectorBlock(Properties properties) {
		super(properties, 1, 2, 2);
	}

	/** The test button at this point of its front (model pixels): 1 + BUS, 2 - BUS, 0 none. */
	public static int testAt(double x, double y) {
		for (int k = 0; k < TESTS.length; k++) {
			if (Math.abs(x - TESTS[k][0]) <= TEST_REACH && Math.abs(y - TESTS[k][1]) <= TEST_REACH) {
				return k + 1;
			}
		}
		return 0;
	}

	/** The DC bus's volts on its battery (as a panel with no charger of its own reads it: client/EdgTelemetry.java battery). */
	public static double busVolts(StationBatteryBlockEntity bank) {
		double soc = bank.charge();
		if (bank.flow > 0) {
			return 129 + 6 * soc;
		}
		return soc > 0.999 ? 132 : 105 + 24 * soc;
	}

	/** Presses a test button (side 1 + BUS, 2 - BUS) at game time now: the test holds {@link #HOLD} ticks. */
	public static boolean press(ServerLevel level, LivePanelBlockEntity panel, int side, long now) {
		boolean fresh = !panel.testing(now) || panel.testSide != side;
		panel.test(level, side, now + HOLD);
		return fresh;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		Direction facing = state.getValue(FACING);
		BlockPos origin = pos.subtract(offset(facing, state.getValue(partProperty())));
		if (!(level.getBlockEntity(origin) instanceof LivePanelBlockEntity panel)) {
			return InteractionResult.PASS;
		}
		double[] at = local(facing, origin, hit.getLocation());
		int side = at[2] > 14 ? testAt(at[0], at[1]) : 0;
		String label = LivePanelBlockEntity.variant(state) == 1 ? "1B" : "1A";
		StationBatteryBlockEntity bank = panel.battery == null ? null : StationBatteryBlock.battery(level, panel.battery);
		if (side == 0 || bank == null) {
			player.sendOverlayMessage(bank == null ? Component.translatable("message.jugcraft.nuclear.ground_no_battery", label)
					: Component.translatable("message.jugcraft.nuclear.ground_status", label, volts(busVolts(bank) / 2), (int) Math.round(bank.charge() * 100)));
			return InteractionResult.SUCCESS_SERVER;
		}
		long now = level.getGameTime();
		if (press((ServerLevel) level, panel, side, now)) {
			level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.6F, 1.2F);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.ground_test", side == 1 ? "+" : "-", label));
		}
		if (now >= panel.beepAt) {
			// The alarm: a beep each half second while the ground is on.
			level.playSound(null, origin.above(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.7F, 1.5F);
			panel.beepAt = now + BEEP;
		}
		return InteractionResult.SUCCESS_SERVER;
	}

	private static String volts(double v) {
		return String.format(Locale.ROOT, "%.0f", v);
	}
}
