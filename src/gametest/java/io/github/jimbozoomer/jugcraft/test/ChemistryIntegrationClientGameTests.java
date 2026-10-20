package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.building.AgedWood;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** Combined aged-wood rendering and chemistry inventory persistence on an isolated test save. */
public class ChemistryIntegrationClientGameTests implements FabricClientGameTest {
    private static final List<String> ITEMS=List.of("aspirin","salicylic_acid","boric_acid",
            "control_rod_assembly","citric_acid","glucose_syrup","pvac_glue","silicone","boron_doped_silicon");
    @Override public void runTest(ClientGameTestContext context) {
        TestWorldSave save;
        Map<BlockPos,BlockState> expected=new LinkedHashMap<>();
        try (TestSingleplayerContext world=context.worldBuilder().adjustSettings(creator->{
            creator.setName("Chemistry Integration Inspection");
            creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            creator.setGenerateStructures(false);
            for (var entry:creator.getNormalPresetList()) if(entry.preset().is(WorldPresets.FLAT))creator.setWorldType(entry);
        }).create()) {
            world.getConnection().waitForChunksRender();
            BlockPos origin=context.computeOnClient(client->BlockPos.containing(client.player.position()));
            for(String command:List.of("difficulty peaceful","time set noon","weather clear","gamerule minecraft:spawn_mobs false"))
                world.getServer().runCommand(command);
            world.getServer().runOnServer(server->{
                var level=server.overworld();
                for(var pos:BlockPos.betweenClosed(origin.offset(-4,-1,-14),origin.offset(21,-1,4)))
                    level.setBlock(pos,Blocks.STONE.defaultBlockState(),Block.UPDATE_CLIENTS);
                int index=0;
                for(String wood:AgedWood.WOODS) for(String part:List.of("planks","stairs","slab")) {
                    var state=AgedWood.BLOCKS.get("aged_"+wood+"_"+part).defaultBlockState();
                    var pos=origin.offset(index*3,0,-8);
                    level.setBlock(pos,state,Block.UPDATE_CLIENTS);expected.put(pos,state);
                    PineWoodClientGameTests.label(level,pos.offset(0,0,2),"Aged "+wood,part);
                    index++;
                }
            });
            for(String id:ITEMS)world.getServer().runCommand("give @a jugcraft:"+id+" 8");
            world.getServer().runCommand("tp @a "+(origin.getX()+8.5)+" "+(origin.getY()+5)+" "+(origin.getZ()+4.5));
            world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.getAbilities().flying=true;p.onUpdateAbilities();});
            context.waitTicks(30);world.getConnection().waitForChunksRender();
            context.getInput().lookAt(origin.offset(8,0,-8));context.waitTicks(10);
            context.takeScreenshot("jugcraft_chemistry_aged_wood");
            world.getServer().runCommand("gamemode survival @a");
            context.waitTicks(10);context.getInput().pressKey(options->options.keyInventory);
            context.waitTicks(10);context.takeScreenshot("jugcraft_chemistry_inventory");
            context.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE);
            context.waitForScreen(null);
            save=world.getWorldSave();
        }
        try(TestSingleplayerContext world=save.open()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(server->{
                expected.forEach((pos,state)->{if(!server.overworld().getBlockState(pos).equals(state))throw new AssertionError("Aged wood changed on reopen: "+pos);});
                var inventory=server.getPlayerList().getPlayers().getFirst().getInventory();
                for(String id:ITEMS)if(inventory.countItem(BuiltInRegistries.ITEM.getValue(Jugcraft.id(id)))!=8)
                    throw new AssertionError("Chemistry item did not persist: "+id);
            });
        }
    }
}
