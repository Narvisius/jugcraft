package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.energy.CableBlock;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStationBlock;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStations;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;

/** In-game views of all three real cable materials meeting the same station gland. */
public final class TowerUtilityClientChecks {
	private TowerUtilityClientChecks() { }

	/** The Energy Exchange's marked power socket and the way it faces. */
	private record Socket(BlockPos pos, Direction facing) { }

	/**
	 * Runs each cable material from the tower's Energy Exchange socket (the Tri-Pillar's faces west over the yard),
	 * and photographs it from the socket's side.
	 */
	public static void cableSockets(ClientGameTestContext context, TestServerContext server, BlockPos core) {
		// The socket of the station the tower built, from the station's own blocks (whatever layout the tower has).
		Socket socket = server.computeOnServer(minecraft -> {
			var level = minecraft.overworld();
			if (!(level.getBlockEntity(core) instanceof TowerCoreBlockEntity tower)) throw new AssertionError("No Tower Core at " + core);
			BlockPos master = core.offset(ExchangeStations.masterOffset(ExchangeStations.ENERGY, tower.layoutVersion()));
			var state = level.getBlockState(master);
			if (!state.is(ExchangeStations.ENERGY)) throw new AssertionError("No Energy Exchange at " + master + ": " + state);
			Direction facing = state.getValue(ExchangeStationBlock.FACING);
			return new Socket(ExchangeStations.ENERGY.footprint(state).partPos(master, facing, ExchangeStations.ENERGY.portPart(state)), facing);
		});
		BlockPos port = socket.pos();
		Direction out = socket.facing(), in = out.getOpposite(), side = out.getCounterClockWise();
		String[] names = {"copper", "aluminum", "silver"};
		Block[] cables = {JugcraftMachines.COPPER_CABLE, JugcraftMachines.ALUMINUM_CABLE, JugcraftMachines.SILVER_CABLE};
		for (int i = 0; i < cables.length; i++) {
			Block cable = cables[i];
			server.runOnServer(minecraft -> {
				var level = minecraft.overworld();
				for (int distance = 1; distance <= 2; distance++) level.setBlock(port.relative(out, distance), cable.defaultBlockState(), Block.UPDATE_ALL);
				// Use the same neighbor query as normal placement, after the whole test chain exists.
				for (int distance = 1; distance <= 2; distance++) {
					var pos = port.relative(out, distance);
					var state = level.getBlockState(pos);
					for (Direction direction : Direction.values()) state = state.setValue(CableBlock.PROPERTY_BY_DIRECTION.get(direction),
							CableBlock.connectsTo(level, pos, direction, level.getBlockState(pos.relative(direction))));
					level.setBlock(pos, state, Block.UPDATE_ALL);
				}
				if (!level.getBlockState(port.relative(out)).getValue(CableBlock.PROPERTY_BY_DIRECTION.get(in)))
					throw new AssertionError("Cable does not visibly connect to the power socket");
			});
			context.waitFor(client -> client.level.getBlockState(port.relative(out)).is(cable)
					&& client.level.getBlockState(port.relative(out)).getValue(CableBlock.PROPERTY_BY_DIRECTION.get(in)));
			// Four blocks out from the socket's middle, 2.4 to its side and 1.3 down.
			double x = port.getX() + .5 + out.getStepX() * 4 + side.getStepX() * 2.4;
			double z = port.getZ() + .5 + out.getStepZ() * 4 + side.getStepZ() * 2.4;
			server.runCommand("tp @p %s %s %s facing %s %s %s".formatted(x, port.getY() - .8, z,
					port.getX() + .5, port.getY() + .5, port.getZ() + .5));
			context.waitTicks(20); context.getInput().lookAt(port); context.waitTicks(5);
			context.takeScreenshot("jugcraft_tower_power_gland_" + names[i]);
		}
	}
}
