package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.creatures.herobrine.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import java.util.concurrent.atomic.AtomicReference;

/** Every sky island and surface ruin rendered in a disposable development world. */
public final class HerobrineLandmarksClientTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c) {
        try(var world=c.worldBuilder().adjustSettings(s->s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
            var server=world.getServer(); world.getConnection().waitForChunksRender();
            BlockPos center=c.computeOnClient(mc->new BlockPos(mc.player.getBlockX()+1024,200,mc.player.getBlockZ()+1024));
            server.runCommand("gamerule minecraft:send_command_feedback false");
            server.runCommand("weather clear"); server.runCommand("time set noon");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getAbilities().flying=true;p.onUpdateAbilities();});
            c.runOnClient(mc->{mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.fov().set(70);if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();});
            for(String variant:HerobrineSkyIslands.VARIANTS) {
                server.runCommand("tp @p %s 220 %s 135 18".formatted(center.getX()+30.5,center.getZ()+30.5));
                c.waitTicks(20); world.getConnection().waitForChunksRender();
                AtomicReference<HerobrineMarkPlans.Plan> built=new AtomicReference<>();
                server.runOnServer(s->{
                    var plan=HerobrineSkyIslands.plan(s.overworld(),center,variant,false);
                    if(plan==null)throw new AssertionError("Sky gallery rejected "+variant);
                    built.set(plan);
                    String id=HerobrineMarks.buildNow(s.overworld(),plan);
                    var mark=HerobrineMarks.get(s).marks().get(id);
                    if(mark==null||!mark.finished())throw new AssertionError("Sky gallery incomplete: "+variant);
                });
                c.waitTicks(40);world.getConnection().waitForChunksRender();
                c.takeScreenshot("herobrine_sky_"+variant);
                server.runCommand("tp @p %s 192 %s 135 -12".formatted(center.getX()+30.5,center.getZ()+30.5));
                c.waitTicks(20);c.takeScreenshot("herobrine_sky_"+variant+"_underside");
                server.runOnServer(s->{for(var step:built.get().steps())s.overworld().setBlock(step.pos(),Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS);});
                c.waitTicks(20);
            }
            BlockPos feet=center.above(20);int x=feet.getX(),y=feet.getY(),z=feet.getZ();
            for(String variant:HerobrineSurfacePlans.VARIANTS) {
                server.runCommand("tp @p %s %d %s 135 16".formatted(x+15.5,y+5,z+15.5));
                server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x-18,y-2,z-18,x+18,y+12,z+18));
                server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x-18,y-1,z-18,x+18,y-1,z+18));
                c.waitTicks(2); server.runCommand("kill @e[type=minecraft:item]");
                server.runOnServer(s->{
                    var plan=HerobrineSurfacePlans.plan(s.overworld(),feet,variant,feet.south(12),false);
                    if(plan==null)throw new AssertionError("Surface gallery rejected "+variant);
                    String id=HerobrineMarks.buildNow(s.overworld(),plan);
                    var mark=HerobrineMarks.get(s).marks().get(id);
                    if(mark==null||!mark.finished())throw new AssertionError("Surface gallery incomplete "+variant);
                });
                c.waitTicks(30);world.getConnection().waitForChunksRender();c.takeScreenshot("herobrine_surface_"+variant);
            }
        }
    }
}
