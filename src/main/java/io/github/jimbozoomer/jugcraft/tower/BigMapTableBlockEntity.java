package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Big Map Table's master (tower/BigMapTableBlock): the drone terminal it shows, as every depot display, and the map
 * it scans ({@link MapScan}): round its tower's core when its terminal belongs to a tower, else round the terminal, else
 * round itself, zoomed out as far as the terminal's drones are.
 */
public final class BigMapTableBlockEntity extends DepotDisplayBlockEntity {
	private final MapScan scan = new MapScan();

	public BigMapTableBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftTower.BIG_MAP_TABLE_ENTITY, pos, state);
	}

	public MapScan scan() {
		return scan;
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, BigMapTableBlockEntity table) {
		DepotDisplayBlockEntity.serverTick(level, pos, state, table);
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		BlockPos link = table.terminal();
		DroneTerminalBlockEntity terminal = link != null && server.isLoaded(link) && server.getBlockEntity(link) instanceof DroneTerminalBlockEntity t
				? t : null;
		BlockPos centre = pos;
		if (terminal != null) {
			centre = terminal.towerPos() != null ? terminal.towerPos() : link;
		}
		table.scan.tick(server, pos, centre, terminal == null ? 0 : terminal.farthestFlight(centre));
	}
}
