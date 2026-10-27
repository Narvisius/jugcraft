package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.creatures.herobrine.HerobrineDepths;
import io.github.jimbozoomer.jugcraft.creatures.herobrine.HerobrineMarks;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import java.util.ArrayList;

/** Disposable gallery of every underground/submerged layout, using real planner and apply validation. */
public final class HerobrineDepthsClientTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c) {
        try (var world = c.worldBuilder().adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
            var server=world.getServer();
            world.getConnection().waitForChunksRender();
            BlockPos at=c.computeOnClient(mc -> new BlockPos(mc.player.getBlockX()+1024,220,mc.player.getBlockZ()+1024));
            int x=at.getX(), y=at.getY(), z=at.getZ();
            server.runCommand("gamerule minecraft:send_command_feedback false");
            server.runCommand("weather clear"); server.runCommand("time set noon");
            server.runOnServer(s -> { var p=s.getPlayerList().getPlayers().getFirst(); p.getAbilities().flying=true; p.onUpdateAbilities(); });
            c.runOnClient(mc -> { mc.options.setCameraType(CameraType.FIRST_PERSON); mc.options.fov().set(70); if(!mc.gui.hud.isHidden())mc.gui.hud.toggle(); });
            var variants=new ArrayList<>(HerobrineDepths.CAVES); variants.addAll(HerobrineDepths.WATER);
            for(String variant:variants) {
                boolean water=HerobrineDepths.WATER.contains(variant);
                server.runCommand("tp @p %s %d %s 0 10".formatted(x+.5,y,z-12.5));
                c.waitTicks(20); world.getConnection().waitForChunksRender();
                server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x-20,y-2,z-20,x+20,y+14,z+20));
                server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x-20,y-2,z-20,x+20,y-1,z+20));
                if(variant.equals("broken_bridge")||variant.equals("balcony"))
                    server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x-5,y-10,z+2,x+5,y-1,z+8));
                if(water) server.runCommand("fill %d %d %d %d %d %d minecraft:water".formatted(x-15,y,z-15,x+15,y+6,z+19));
                c.waitTicks(2); server.runCommand("kill @e[type=minecraft:item]");
                server.runOnServer(s -> {
                    var plan=HerobrineDepths.plan(s.overworld(),at,variant);
                    if(plan==null)throw new AssertionError("Missing gallery plan: "+variant);
                    String id=HerobrineMarks.buildNow(s.overworld(),plan);
                    if(!HerobrineMarks.get(s).marks().get(id).finished())throw new AssertionError("Incomplete "+variant);
                });
                if(water) server.runCommand("tp @p %s %d %s 45 40".formatted(x+10.5,y+9,z-10.5));
                else server.runCommand("tp @p %s %d %s 30 15".formatted(x+8.5,y+3,z-10.5));
                c.waitTicks(25); world.getConnection().waitForChunksRender();
                c.takeScreenshot("herobrine_"+variant);
                if(variant.equals("watching_room")||variant.equals("empty_hall")||variant.equals("shelter")||variant.equals("buried_shrine")) {
                    server.runCommand("tp @p %s %d %s 0 0".formatted(x-.5,y,z+.5));
                    c.waitTicks(15); c.takeScreenshot("herobrine_"+variant+"_inside");
                }
            }
        }
    }
}
