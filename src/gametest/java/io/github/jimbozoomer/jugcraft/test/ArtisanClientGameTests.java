package io.github.jimbozoomer.jugcraft.test;
import io.github.jimbozoomer.jugcraft.furniture.*;
import io.github.jimbozoomer.jugcraft.client.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import java.util.*;

public final class ArtisanClientGameTests implements FabricClientGameTest {
    private static void click(ClientGameTestContext c,int x,int y){
        double[] point=c.computeOnClient(mc->{var s=mc.gui.screen();float scale=Math.min(1F,Math.min((s.width-8F)/440,(s.height-8F)/284));int left=Math.round((s.width/scale-440)/2),top=Math.round((s.height/scale-284)/2);return new double[]{(left+x)*scale/net.minecraft.client.MouseHandler.getScaledXPos(mc.getWindow(),1),(top+y)*scale/net.minecraft.client.MouseHandler.getScaledYPos(mc.getWindow(),1)};});
        c.getInput().setCursorPos(point[0],point[1]);c.waitTicks(1);c.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);c.waitTicks(6);
    }
    @Override public void runTest(ClientGameTestContext c){
        try(var world=c.worldBuilder().adjustSettings(s->s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()){
            var server=world.getServer();world.getConnection().waitForChunksRender();var origin=c.computeOnClient(mc->mc.player.blockPosition());var bench=origin.above();
            server.runCommand("gamerule minecraft:spawn_mobs false");server.runCommand("time set noon");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().clearContent();p.level().setBlockAndUpdate(bench,ArtisanFurniture.WORKBENCH.defaultBlockState());});
            c.runOnClient(mc->mc.gui.setScreen(new ArtisanScreen(bench)));c.waitTicks(8);c.takeScreenshot("artisan_editor_stool");
            click(c,220,64); // Cabinet preset.
            click(c,306,64); // Save local design.
            c.runOnClient(mc->{try{var saved=ArtisanLibrary.load();if(saved.size()!=1||!saved.getFirst().storage())throw new AssertionError("Editor library did not save cabinet");}catch(java.io.IOException e){throw new AssertionError(e);}});
            click(c,306,39); // Build through the actual client payload.
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(p.getInventory().countItem(ArtisanFurniture.FURNITURE.asItem())!=1)throw new AssertionError("Build button did not make one server-owned item");});
            c.takeScreenshot("artisan_editor_cabinet");
            click(c,40,64);click(c,220,244); // Stool then saved-library cabinet.
            click(c,306,270); // Delete local saved copy, preserve held design.
            c.runOnClient(mc->{try{if(!ArtisanLibrary.load().isEmpty())throw new AssertionError("Delete saved did not remove library entry");}catch(java.io.IOException e){throw new AssertionError(e);}mc.gui.setScreen(null);});
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var level=p.level();
                var made=java.util.stream.IntStream.range(0,p.getInventory().getContainerSize()).mapToObj(p.getInventory()::getItem).filter(i->i.is(ArtisanFurniture.FURNITURE.asItem())).findFirst().orElseThrow();var craftedDesign=FurnitureDesign.parse(made.get(ArtisanFurniture.DESIGN));
                for(int n=0;n<4;n++){var at=bench.offset(n*2,0,3);level.setBlockAndUpdate(at,ArtisanFurniture.FURNITURE.defaultBlockState().setValue(DesignedFurnitureBlock.FACING,Direction.from2DDataValue(n)));var d=n==0?FurnitureDesign.stool():n==1?craftedDesign:new FurnitureDesign("Lamp",List.of(new FurnitureDesign.Part(3,0,3,10,2,10,3,false),new FurnitureDesign.Part(7,2,7,2,8,2,3,false),new FurnitureDesign.Part(3,10,3,10,6,10,4,false)),0,false,15,1,1);((DesignedFurnitureEntity)level.getBlockEntity(at)).design(d);}
                p.teleportTo(bench.getX()+7,bench.getY()+2,bench.getZ()+10);p.setYRot(145);p.setXRot(15);
            });server.runCommand("tp @p %f %f %f 145 15".formatted(bench.getX()+7.0,bench.getY()+2.0,bench.getZ()+10.0));c.getInput().pressKey(o->o.keyToggleGui);c.waitTicks(20);c.takeScreenshot("artisan_custom_furniture_rendered");c.getInput().pressKey(o->o.keyToggleGui);
            c.runOnClient(mc->{if(!(mc.level.getBlockEntity(bench.offset(2,0,3)) instanceof DesignedFurnitureEntity e)||!e.design().storage())throw new AssertionError("Custom design did not sync to client");});
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var at=bench.offset(2,0,3);p.teleportTo(at.getX()+1,at.getY(),at.getZ()+1);p.openMenu((DesignedFurnitureEntity)p.level().getBlockEntity(at));});c.waitTicks(8);c.takeScreenshot("artisan_storage_menu");c.runOnClient(mc->mc.player.closeContainer());
        }
    }
}
