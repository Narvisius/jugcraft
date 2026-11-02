package io.github.jimbozoomer.jugcraft.test;
import io.github.jimbozoomer.jugcraft.pets.*;
import io.github.jimbozoomer.jugcraft.client.PetInventoryScreen;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.entity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import java.util.UUID;

public final class PetInventoryClientTests implements FabricClientGameTest {
    public void runTest(ClientGameTestContext c){
        try(var world=c.worldBuilder().adjustSettings(s->s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()){
            world.getConnection().waitForChunksRender(); var server=world.getServer();
            UUID[] id={null};
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var l=p.level();var at=p.blockPosition().above(3);
                for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++){l.setBlock(at.offset(x,-1,z),Blocks.STONE_BRICKS.defaultBlockState(),3);for(int y=0;y<5;y++)l.removeBlock(at.offset(x,y,z),false);}
                p.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
                var wolf=EntityTypes.WOLF.create(l,EntitySpawnReason.COMMAND);wolf.tame(p);wolf.setCustomName(Component.literal("Juniper"));wolf.snapTo(p.getX()+2,p.getY(),p.getZ(),0,0);l.addFreshEntity(wolf);id[0]=wolf.getUUID();Pets.enroll(wolf);Pets.view(p,"");
            });
            c.waitForScreen(PetInventoryScreen.class);c.waitTicks(10);c.takeScreenshot("pets_active_roster");
            c.clickScreenButton("Recall pet");c.waitTicks(10);c.takeScreenshot("pets_stored");
            server.runOnServer(s->{if(!PetRoster.get(s).entries.get(id[0]).getStringOr("state","").equals("STORED"))throw new AssertionError("UI did not recall pet");});
            c.clickScreenButton("Summon pet");c.waitTicks(10);
            server.runOnServer(s->{if(s.overworld().getEntity(id[0])==null)throw new AssertionError("UI did not summon exact pet");});
            c.clickScreenButton("Mode: FOLLOW");c.waitTicks(10);c.takeScreenshot("pets_stay");
            c.getInput().resizeWindow(640,480);c.waitTicks(10);c.takeScreenshot("pets_compact");
            c.clickScreenButton("Done");c.waitForScreen(null);
        }
    }
}
