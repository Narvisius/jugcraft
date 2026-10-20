package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.menagerie.*;
import io.github.jimbozoomer.jugcraft.client.CavernDeviceScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Final combined render/UI pass in a disposable world, independent of owner saves. */
public final class OwnerCollectionsClientGameTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c) {
        try (var world=c.worldBuilder().adjustSettings(settings->{
            settings.setGameMode(net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.SelectedGameMode.CREATIVE);
            settings.setWorldType(new net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.WorldTypeEntry(
                settings.getSettings().worldgenLoadContext().lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
                    .getOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.NORMAL)));
            settings.setSeed("owner-collections-oct19");
        }).create()) {
            world.getConnection().waitForChunksRender();var server=world.getServer();
            var start=c.computeOnClient(mc->mc.player.blockPosition());int x=start.getX(),y=120,z=start.getZ();
            server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x-5,y-1,z-5,x+30,y-1,z+30));
            server.runCommand("time set noon");server.runCommand("weather clear");server.runCommand("gamerule minecraft:spawn_mobs false");
            server.runCommand("tp @p %f %f %f 180 15".formatted(x+.5,y+1.0,z+4.5));
            for(String id:java.util.List.of("cavern_nuclear_furnace_component","cavern_quarry","cavern_confection_oven","cavern_conversion_crucible","cavern_hologram_projector","beast_capsid")) {
                server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.closeContainer();var at=new BlockPos(x,y,z);p.level().setBlockAndUpdate(at,Blocks.AIR.defaultBlockState());
                    var block=Menagerie.BLOCKS.get(id);p.level().setBlockAndUpdate(at,block.defaultBlockState());block.setPlacedBy(p.level(),at,p.level().getBlockState(at),p,new ItemStack(block));
                    if(!(p.level().getBlockEntity(at) instanceof MenuProvider menu))throw new AssertionError("Missing menu: "+id);p.openMenu(menu);
                });
                c.waitTicks(15);
                c.runOnClient(mc->{if(mc.gui.screen()==null)throw new AssertionError("Menu failed to open: "+id);if(id.startsWith("cavern_")&&!(mc.gui.screen() instanceof CavernDeviceScreen))throw new AssertionError("Wrong cave screen");});
                c.takeScreenshot("owner_station_"+id);
            }
            server.runOnServer(s->s.getPlayerList().getPlayers().getFirst().closeContainer());c.waitTicks(5);
            var blocks=Menagerie.BLOCKS.entrySet().stream().filter(e->e.getKey().startsWith("household_")||e.getKey().startsWith("cavern_")||e.getKey().startsWith("beast_")).toList();
            for(int page=0;page<(blocks.size()+11)/12;page++) {
                int offset=page*12;
                server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x-2,y,z-2,x+24,y+5,z+12));
                server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(int i=0;i<12&&offset+i<blocks.size();i++)p.level().setBlockAndUpdate(new BlockPos(x+i%4*6,y,z+i/4*4),blocks.get(offset+i).getValue().defaultBlockState());});
                server.runCommand("tp @p %f %f %f 180 25".formatted(x+9.5,y+7.0,z+20.0));c.waitTicks(12);c.takeScreenshot("owner_blocks_"+page);
            }
            server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x-2,y,z-2,x+24,y+5,z+12));
            server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x,y-1,z,x+12,y-1,z+5));
            for(int bites=0;bites<4;bites++)server.runCommand("setblock %d %d %d jugcraft:ecology_maple_pie[bites=%d]".formatted(x+bites*3,y,z,bites));
            for(int i=0;i<2;i++)for(int amount:new int[]{1,4})server.runCommand("setblock %d %d %d jugcraft:%s[flower_amount=%d]".formatted(x+i*6+(amount==4?3:0),y,z+4,i==0?"landscape_purple_wildflowers":"landscape_white_petals",amount));
            server.runCommand("tp @p %f %f %f 180 24".formatted(x+5.0,y+3.5,z+10.0));c.waitTicks(12);c.takeScreenshot("owner_flora_and_food");
            server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x-2,y,z-2,x+24,y+5,z+12));
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();int i=0;for(var type:java.util.List.of(CavernSubmarine.TYPE,CavernSubmarine.BOARD)){var entity=type.create(p.level(),EntitySpawnReason.COMMAND);if(entity==null)throw new AssertionError("Missing vehicle");entity.snapTo(x+3+i++*7,y,z+3,0,0);entity.setNoGravity(true);p.level().addFreshEntity(entity);}});
            server.runCommand("tp @p %f %f %f 180 15".formatted(x+6.5,y+2.0,z+12.0));c.waitTicks(20);c.takeScreenshot("owner_cavern_vehicles");
        }
    }
}
