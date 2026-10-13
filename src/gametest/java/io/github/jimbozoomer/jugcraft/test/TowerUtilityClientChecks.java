package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.energy.CableBlock;
import io.github.jimbozoomer.jugcraft.machine.JugcraftMachines;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;

/** In-game views of all three real cable materials meeting the same station gland. */
public final class TowerUtilityClientChecks {
	private TowerUtilityClientChecks() { }
	public static void cableSockets(ClientGameTestContext context, TestServerContext server, BlockPos core) {
		BlockPos port = core.offset(-22, 2, 0);
		String[] names = {"copper", "aluminum", "silver"};
		Block[] cables = {JugcraftMachines.COPPER_CABLE, JugcraftMachines.ALUMINUM_CABLE, JugcraftMachines.SILVER_CABLE};
		for (int i = 0; i < cables.length; i++) {
			Block cable = cables[i];
			server.runOnServer(minecraft -> {
				var level = minecraft.overworld();
				for (int distance = 1; distance <= 2; distance++) level.setBlock(port.west(distance), cable.defaultBlockState(), Block.UPDATE_ALL);
				// Use the same neighbor query as normal placement, after the whole test chain exists.
				for (int distance = 1; distance <= 2; distance++) {
					var pos = port.west(distance);
					var state = level.getBlockState(pos);
					for (Direction direction : Direction.values()) state = state.setValue(CableBlock.PROPERTY_BY_DIRECTION.get(direction),
							CableBlock.connectsTo(level, pos, direction, level.getBlockState(pos.relative(direction))));
					level.setBlock(pos, state, Block.UPDATE_ALL);
				}
				if (!level.getBlockState(port.west()).getValue(CableBlock.EAST)) throw new AssertionError("Cable does not visibly connect to the power socket");
			});
			context.waitFor(client -> client.level.getBlockState(port.west()).is(cable)
					&& client.level.getBlockState(port.west()).getValue(CableBlock.EAST));
			server.runCommand("tp @p %s %s %s facing %s %s %s".formatted(
					core.getX() - 25.5, core.getY() + 1.2, core.getZ() + 2.9,
					port.getX() + .5, port.getY() + .5, port.getZ() + .5));
			context.waitTicks(20); context.getInput().lookAt(port); context.waitTicks(5);
			context.takeScreenshot("jugcraft_tower_power_gland_" + names[i]);
		}
	}
}
