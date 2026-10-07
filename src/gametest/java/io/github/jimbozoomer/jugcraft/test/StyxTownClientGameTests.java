package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.styx.*;
import io.github.jimbozoomer.jugcraft.town.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import java.util.UUID;

/** New-world city integration, road clearance, protection, and resident identity after a real save/reopen. */
public final class StyxTownClientGameTests implements FabricClientGameTest {
	@Override public void runTest(ClientGameTestContext context) {
		context.runOnClient(c -> c.options.renderDistance().set(5));
		TestWorldSave save;
		UUID resident;
		BlockPos home;
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(c -> {
			c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			c.setWorldType(new WorldCreationUiState.WorldTypeEntry(c.getSettings().worldgenLoadContext()
					.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL)));
			c.setSeed("jugcraft");
		}).create()) {
			var server = world.getServer();
			BlockPos town = server.computeOnServer(s -> TownState.get(s).origin());
			check(town != null, "New world has a spawn city");
			home = StyxTownDistrict.home(town);
			server.runCommand("time set noon");server.runCommand("weather clear");
			view(context,world,home.offset(32,65,25),180,45);
			for (int i=0;i<180;i++) {
				context.waitTicks(10);
				if (server.computeOnServer(s -> StyxState.get(s.overworld()).resident.isPresent())) break;
			}
			resident = server.computeOnServer(s -> {
				var level=s.overworld();var state=StyxState.get(level);
				check(state.townHome && state.origin.orElseThrow().equals(home),"Resident belongs to the east district");
				check(StyxTownDistrict.ready(level,home),"All district chunks are complete");
				var npc=(Styxhexenhammer)level.getEntity(state.resident.orElseThrow());
				check(npc != null,"Styx spawned after construction");npc.setNoAi(true);
				for (var b:StyxConservatory.placements(3)) {
					if (b.offset().getY()==0 || b.state().isAir()) continue;
					check(level.getBlockState(home.offset(b.offset())).getBlock()==b.state().getBlock(),"Building block missing at "+b.offset());
				}
				for (int x=175;x<=205;x++) {
					BlockPos p=town.offset(x,0,96);
					check(!level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()
						&& level.getBlockState(p.above(2)).isAir(),"East gate path obstructed at "+p);
					check(Town.isProtected(level,p),"Connecting path is protected");
				}
				check(Town.isInside(level,home.offset(52,1,34)),"Greenhouse gets town hostile-spawn protection");
				check(!StyxTownDistrict.canReserve(level,town),"An existing home prevents a second reservation");
				return npc.getUUID();
			});
			context.getInput().pressKey(o -> o.keyToggleGui);
			view(context,world,town.offset(213,48,146),180,12);
			world.getConnection().waitForChunksRender();context.takeScreenshot("jugcraft_styx_city_district");
			view(context,world,town.offset(184,2,96),-90,-12);
			context.takeScreenshot("jugcraft_styx_city_east_road");
			view(context,world,home.offset(13,2,27),180,0);
			context.takeScreenshot("jugcraft_styx_city_resident");
			context.getInput().pressKey(o -> o.keyToggleGui);
			save=world.getWorldSave();
		}
		try (var world=save.open()) {
			context.waitTicks(40);
			world.getServer().runOnServer(s -> {
				var state=StyxState.get(s.overworld());
				check(state.townHome && state.origin.orElseThrow().equals(home),"District home survives reopen");
				check(state.resident.orElseThrow().equals(resident),"Resident UUID survives reopen");
				check(StyxTownDistrict.ready(s.overworld(),home),"Construction cursor survives reopen");
				check(s.overworld().getEntity(resident) instanceof Styxhexenhammer,"Same resident reloads");
			});
		}
	}
	private static void view(ClientGameTestContext context,TestSingleplayerContext world,BlockPos p,int yaw,int pitch) {
		world.getServer().runCommand("execute if block %d %d %d air run setblock %d %d %d barrier".formatted(p.getX(),p.getY()-1,p.getZ(),p.getX(),p.getY()-1,p.getZ()));
		world.getServer().runCommand("tp @p %d.5 %d %d.5 %d %d".formatted(p.getX(),p.getY(),p.getZ(),yaw,pitch));
		context.waitTicks(40);
	}
	private static void check(boolean ok,String message) {if(!ok) throw new AssertionError(message);}
}
