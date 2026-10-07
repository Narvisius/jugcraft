package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.styx.*;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;

/** Real-client proof of the complete placement path and character/flower renderers, with review screenshots. */
public final class StyxClientGameTests implements FabricClientGameTest {
	@Override public void runTest(ClientGameTestContext context) {
		context.runOnClient(c -> c.options.renderDistance().set(5));
		try(TestSingleplayerContext world=context.worldBuilder().adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			world.getConnection().waitForChunksRender();TestServerContext server=world.getServer();
			server.runCommand("time set noon");server.runCommand("weather clear");
			BlockPos base=server.computeOnServer(s -> {
				var p=s.getPlayerList().getPlayers().getFirst();int y=s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,p.blockPosition().getX(),p.blockPosition().getZ())-1;
				return new BlockPos(p.blockPosition().getX()-StyxConservatory.WIDTH/2,y,p.blockPosition().getZ()-StyxConservatory.DEPTH/2);
			});
			// Prepare only this disposable test world's site; the player command never clears terrain.
			server.runOnServer(s -> {
				for(int x=-2;x<StyxConservatory.WIDTH+2;x++) for(int z=-2;z<StyxConservatory.DEPTH+15;z++) {
					s.overworld().setBlock(base.offset(x,0,z),net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(),3);
					for(int y=1;y<StyxConservatory.HEIGHT;y++) s.overworld().setBlock(base.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
				}
			});
			server.runCommand("execute as @p at @s run jugcraft styx preview");
			server.runCommand("execute as @p at @s run jugcraft styx place");
			server.runOnServer(s -> {if(!StyxState.get(s.overworld()).origin.equals(java.util.Optional.of(base)))throw new AssertionError("Operator commands did not begin at the previewed origin");});
			for(int i=0;i<300;i++) {
				context.waitTicks(1);
				if(server.computeOnServer(s -> StyxState.get(s.overworld()).resident.isPresent()))break;
			}
			server.runOnServer(s -> {
				var state=StyxState.get(s.overworld());if(state.resident.isEmpty())throw new AssertionError("Observatory did not complete; cursor "+state.placed);
				if(state.layout!=StyxConservatory.CURRENT_LAYOUT)throw new AssertionError("New placement must use the current observatory layout");
				var npc=(Styxhexenhammer)s.overworld().getEntity(state.resident.get());
				if(npc==null)throw new AssertionError("Resident missing");npc.setNoAi(true);
			});
			context.getInput().pressKey(options -> options.keyToggleGui);
			context.runOnClient(c -> c.options.fov().set(70));
			view(context,server,base,60,37,75,149,11);world.getConnection().waitForChunksRender();context.takeScreenshot("jugcraft_styx_observatory_overview");
			view(context,server,base,44,36,67,146,4);context.takeScreenshot("jugcraft_styx_observatory_dome");
			context.runOnClient(c -> c.options.fov().set(75));
			view(context,server,base,29,53,45,132,-8);context.takeScreenshot("jugcraft_styx_telescope");
			view(context,server,base,15,10,26,170,8);context.takeScreenshot("jugcraft_styx_library");
			view(context,server,base,52,3,38,180,-10);context.takeScreenshot("jugcraft_styx_greenhouse_interior");
			view(context,server,base,51,1.6,17,90,10);context.takeScreenshot("jugcraft_styx_flower_bed");
			context.runOnClient(c -> c.options.fov().set(55));
			view(context,server,base,70,16,59,137,7);context.takeScreenshot("jugcraft_styx_greenhouse_facade");
			context.runOnClient(c -> c.options.fov().set(70));
			server.runCommand("time set midnight");view(context,server,base,60,37,75,149,11);context.takeScreenshot("jugcraft_styx_observatory_night");
			server.runCommand("time set noon");view(context,server,base,13,1.6,26.5,180,5);context.takeScreenshot("jugcraft_styx_at_home");
			view(context,server,base,35,4,39,180,0);
			context.getInput().pressKey(options -> options.keyToggleGui);
			context.runOnClient(c -> c.options.fov().set(70));
			server.runOnServer(s -> {var npc=(Styxhexenhammer)s.overworld().getEntity(StyxState.get(s.overworld()).resident.orElseThrow());npc.setNoAi(false);});
			for(int phase=0;phase<4;phase++) {
				server.runCommand("time set "+(phase*6000+100));
				BlockPos destination=StyxConservatory.routineTarget(base,StyxConservatory.CURRENT_LAYOUT,phase);
				boolean arrived=false;
				for(int i=0;i<300;i++) {
					context.waitTicks(10);
					arrived=server.computeOnServer(s -> {
						var npc=s.overworld().getEntity(StyxState.get(s.overworld()).resident.orElseThrow());
						return npc.distanceToSqr(destination.getX()+.5,destination.getY(),destination.getZ()+.5)<3;
					});
					if(arrived)break;
				}
				if(!arrived)throw new AssertionError("Styx cannot reach routine phase "+phase+": "+server.computeOnServer(s -> s.overworld().getEntity(StyxState.get(s.overworld()).resident.orElseThrow()).position()));
			}
		}
	}
	private static void view(ClientGameTestContext context,TestServerContext server,BlockPos base,double x,double y,double z,float yaw,float pitch) {
		server.runOnServer(s -> {var p=s.getPlayerList().getPlayers().getFirst();p.getAbilities().flying=true;p.onUpdateAbilities();});
		context.runOnClient(c -> {c.player.getAbilities().flying=true;c.player.onUpdateAbilities();});context.waitTicks(2);
		server.runCommand(String.format(Locale.ROOT,"tp @p %.2f %.2f %.2f %.1f %.1f",base.getX()+x,base.getY()+y,base.getZ()+z,yaw,pitch));
		context.runOnClient(c -> {c.player.getAbilities().flying=true;c.player.onUpdateAbilities();});context.waitTicks(20);
	}
}
