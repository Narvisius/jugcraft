package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.vehiclebay.*;
import io.github.jimbozoomer.jugcraft.client.VehicleBayScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Independent disposable game, never the owner's launcher/profile or manual playtest. */
public class VehicleBayClientGameTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c) {
        try(var world=c.worldBuilder().adjustSettings(s->s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()){
            world.getConnection().waitForChunksRender();var server=world.getServer();
            var origin=c.computeOnClient(m->BlockPos.containing(m.player.position()));
            var mecha=origin.offset(-2,0,-10);var tank=origin.offset(9,0,-10);
            server.runCommand("time set noon");server.runCommand("weather clear");
            server.runCommand("gamerule minecraft:send_command_feedback false");
            server.runOnServer(s->{var level=s.overworld();
                var player=s.getPlayerList().getPlayers().getFirst();player.getAbilities().flying=true;player.onUpdateAbilities();
                for(int x=-12;x<=14;x++)for(int z=-16;z<=6;z++){
                    level.setBlockAndUpdate(origin.offset(x,-1,z),Blocks.SMOOTH_STONE.defaultBlockState());
                    for(int y=0;y<=9;y++)level.setBlockAndUpdate(origin.offset(x,y,z),Blocks.AIR.defaultBlockState());
                }
                for(var pos:new BlockPos[]{mecha,tank}){
                    var block=pos.equals(mecha)?VehicleBays.MECHA:VehicleBays.TANK;var state=block.defaultBlockState();
                    level.setBlockAndUpdate(pos,state);block.setPlacedBy(level,pos,state,null,ItemStack.EMPTY);
                    var bay=(VehicleBayBlockEntity)level.getBlockEntity(pos);VehicleBayGameTests.supply(bay);
                }
            });
            server.runCommand("tp @p %f %f %f facing %f %f %f".formatted(origin.getX()+1.,origin.getY()+4.,origin.getZ()-19.,origin.getX()+1.,origin.getY()+.5,origin.getZ()-7.));
            c.runOnClient(m->{if(!m.gui.hud.isHidden())m.gui.hud.toggle();});c.waitTicks(40);
            c.takeScreenshot("jugcraft_vehicle_bays_front");
            server.runCommand("tp @p %f %f %f facing %f %f %f".formatted(origin.getX()-13.,origin.getY()+5.,origin.getZ()-16.,origin.getX()-4.,origin.getY()+1.,origin.getZ()-7.));
            c.waitTicks(25);c.takeScreenshot("jugcraft_mecha_bay_detail");
            server.runCommand("tp @p %f %f %f facing %f %f %f".formatted(origin.getX()+16.,origin.getY()+3.,origin.getZ()-15.,origin.getX()+6.,(double)origin.getY(),origin.getZ()-7.));
            c.waitTicks(25);c.takeScreenshot("jugcraft_tank_bay_detail");
            // Open the real server container; the buttons send normal menu actions.
            server.runCommand("tp @p %f %f %f".formatted(mecha.getX()+2.,(double)mecha.getY(),mecha.getZ()-2.));
            server.runOnServer(s->s.getPlayerList().getPlayers().getFirst().openMenu((VehicleBayBlockEntity)s.overworld().getBlockEntity(mecha)));
            c.waitForScreen(VehicleBayScreen.class);c.waitTicks(10);c.takeScreenshot("jugcraft_mecha_bay_console");
            c.clickScreenButton("screen.jugcraft.bay.start");c.waitTicks(10);
            server.runOnServer(s->{var bay=(VehicleBayBlockEntity)s.overworld().getBlockEntity(mecha);if(!bay.busy())throw new AssertionError("Real assembly button starts powered work");});
            c.takeScreenshot("jugcraft_mecha_bay_working");
            c.clickScreenButton("screen.jugcraft.bay.cancel");c.waitTicks(5);
            server.runOnServer(s->{var bay=(VehicleBayBlockEntity)s.overworld().getBlockEntity(mecha);if(bay.busy()||bay.getItem(0).getCount()!=2)throw new AssertionError("Real cancel preserves components");});
            c.setScreen(()->null);
            server.runOnServer(s->{var bay=(VehicleBayBlockEntity)s.overworld().getBlockEntity(mecha);bay.start();});
            server.runCommand("tp @p %f %f %f facing %f %f %f".formatted(origin.getX()-13.,origin.getY()+5.,origin.getZ()-16.,origin.getX()-4.,origin.getY()+1.,origin.getZ()-7.));
            c.waitTicks(45);c.takeScreenshot("jugcraft_mecha_bay_motion");
            c.waitTicks(180);c.takeScreenshot("jugcraft_mecha_bay_stopped");
        }
    }
}
