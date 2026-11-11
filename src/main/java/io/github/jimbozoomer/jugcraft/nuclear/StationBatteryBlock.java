package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The station battery (round 25): placed as one piece like any panel, its placing block holds the charge
 * (StationBatteryBlockEntity), and every block of it is the battery to a cable or a charger. Right-click: its charge.
 *
 * <p>Older rooms round 4 (tools/battery_rooms.py): labelled by train, string and rack (its variant: 1A string 1 cells
 * 1-30, cells 31-60, string 2 ..., then 1B), each cell's number on its cover. A click on a cell reads it: its volts
 * (the string's share of the bus it floats on, or its own open-circuit volts off the bus), its electrolyte's specific
 * gravity and temperature, from the rack's real charge; a click elsewhere says the rack's charge.
 */
public class StationBatteryBlock extends MultiFurnitureBlock implements EntityBlock {
	/** The cells (tools/plant_panels.py SB_*): the first's left side, cell to cell, a cell's width; the steps' covers' tops. */
	static final double X0 = 2.15, PITCH = 4.0, CELL_W = 3.7, LOW_TOP = 15.2, HIGH_BOTTOM = 16.4, HIGH_TOP = 28.2, CELLS = 15;

	public StationBatteryBlock(Properties properties, int width, int rows) {
		this(properties, width, rows, 1);
	}

	public StationBatteryBlock(Properties properties, int width, int rows, int variants) {
		super(properties, width, rows, true, variants, true, false, 15.6);
	}

	/** The cell at this point of a rack's front (model pixels across the whole rack), its number in its string, or 0. */
	public static int cellAt(double x, double y, int variant) {
		int k = (int) Math.floor((x - X0) / PITCH);
		if (k < 0 || k >= CELLS || x - X0 - k * PITCH > CELL_W || y < 3 || y > HIGH_TOP + 0.6) {
			return 0;
		}
		int first = variant % 2 * 30 + 1;
		return y >= HIGH_BOTTOM - 0.6 ? first + 14 - k : y <= LOW_TOP + 0.4 ? first + 15 + k : 0;
	}

	/** A cell's volts, specific gravity and temperature (degrees C): the rack's charge and what it is doing, its bus's volts. */
	public static double[] cell(StationBatteryBlockEntity bank, int n, double busVolts) {
		double soc = bank.charge();
		double h = Math.sin(n * 12.9898 + bank.getBlockPos().asLong() * 0.0001) * 43758.5453;
		double spread = (h - Math.floor(h)) * 2 - 1;
		double volts = busVolts > 0 ? busVolts / 60 : 1.95 + 0.2 * soc;
		double rate = bank.flow() / 8.0;
		return new double[] {volts + 0.012 * spread, 1.100 + 0.115 * soc + 0.003 * spread, 25 + (rate > 0 ? Math.min(3, rate / 50) : 0) + 0.3 * spread};
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(partProperty()) == origin() ? new StationBatteryBlockEntity(pos, state) : null;
	}

	/** No ticker (it only looks live): its scheduled tick after energy stops moving through it (StationBatteryBlockEntity). */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
		if (level.getBlockEntity(pos) instanceof StationBatteryBlockEntity battery) {
			battery.settle(level);
		}
	}

	/** The battery any block of it belongs to (made if the rack was placed before batteries held charge), or null. */
	public static @Nullable StationBatteryBlockEntity battery(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof StationBatteryBlock block)) {
			return null;
		}
		BlockPos origin = pos.subtract(block.offset(state.getValue(FACING), state.getValue(block.partProperty())));
		return level.getBlockEntity(origin) instanceof StationBatteryBlockEntity battery ? battery : null;
	}

	/** What a cable touching the battery finds: its charge, to put in or take out. */
	public static @Nullable EnergyStorage storage(Level level, BlockPos pos) {
		StationBatteryBlockEntity battery = battery(level, pos);
		return battery == null ? null : battery.energy;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		StationBatteryBlockEntity battery = battery(level, pos);
		if (battery == null) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			net.minecraft.core.Direction facing = state.getValue(FACING);
			BlockPos origin = pos.subtract(offset(facing, state.getValue(partProperty())));
			double[] at = local(facing, origin, hit.getLocation());
			int v = variantProperty() == null ? 0 : state.getValue(variantProperty());
			int n = at[2] > 6 ? cellAt(at[0] + 16 * (origin() % width), at[1], v) : 0;
			if (n > 0) {
				LivePanelBlockEntity board = DcRoom.boardOf(level, origin);
				double bus = board != null && (board.sbFlags & DcRoom.BUS) != 0 && DcRoom.rackOnBus(level, board, origin) ? DcRoom.busVolts(level, board) : 0;
				double[] c = cell(battery, n, bus);
				String train = v / 4 % 2 == 1 ? "1B" : "1A";
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.battery_cell", n, train, v / 2 % 2 + 1,
						String.format(java.util.Locale.ROOT, "%.2f", c[0]), String.format(java.util.Locale.ROOT, "%.3f", c[1]),
						String.format(java.util.Locale.ROOT, "%.1f", c[2]), Math.round(c[2] * 1.8 + 32)));
				return InteractionResult.SUCCESS;
			}
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.station_battery", Math.round(battery.charge() * 100),
					String.format("%,d", battery.energy.getAmount()), String.format("%,d", StationBatteryBlockEntity.CAPACITY),
					Component.translatable(battery.flow() > 0 ? "message.jugcraft.nuclear.battery_charging"
							: battery.flow() < 0 ? "message.jugcraft.nuclear.battery_discharging" : "message.jugcraft.nuclear.battery_float",
							String.format("%,d", Math.abs(battery.flow())))));
		}
		return InteractionResult.SUCCESS;
	}

	/** Where a hit on the rack is in its own frame (as LivePanelBlock.local: model pixels, the piece facing north). */
	static double[] local(net.minecraft.core.Direction facing, BlockPos origin, net.minecraft.world.phys.Vec3 hit) {
		double x = (hit.x - origin.getX()) * 16, y = (hit.y - origin.getY()) * 16, z = (hit.z - origin.getZ()) * 16;
		int turns = switch (facing) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
		for (int i = 0; i < turns; i++) {
			double nx = z, nz = 16 - x;
			x = nx;
			z = nz;
		}
		return new double[] {x, y, z};
	}
}
