package io.github.jimbozoomer.jugcraft.test;
import io.github.jimbozoomer.jugcraft.pets.*;
import io.github.jimbozoomer.jugcraft.client.PetInventoryScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.entity.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import java.util.UUID;

/**
 * The companion stable screen (client/PetInventoryScreen): the empty stable, a roster of two, recall and deploy of the
 * exact pet through the screen's keys, a behaviour change, keyboard selection and Enter, a knocked-out companion whose
 * Deploy key waits out its recovery, and the screen fitting a 640 x 480 window. Screenshots at each step.
 */
public final class PetInventoryClientTests implements FabricClientGameTest {
    public void runTest(ClientGameTestContext c){
        try(var world=c.worldBuilder().adjustSettings(s->s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()){
            world.getConnection().waitForChunksRender(); var server=world.getServer();
            UUID[] id={null,null};
            // The empty stable first.
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();Pets.view(p,"");});
            c.waitForScreen(PetInventoryScreen.class);c.waitTicks(10);c.takeScreenshot("pets_empty_stable");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var l=p.level();var at=p.blockPosition().above(3);
                for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++){l.setBlock(at.offset(x,-1,z),Blocks.STONE_BRICKS.defaultBlockState(),3);for(int y=0;y<5;y++)l.removeBlock(at.offset(x,y,z),false);}
                p.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
                var wolf=EntityTypes.WOLF.create(l,EntitySpawnReason.COMMAND);wolf.tame(p);wolf.setCustomName(Component.literal("Juniper"));wolf.snapTo(p.getX()+2,p.getY(),p.getZ(),0,0);l.addFreshEntity(wolf);id[0]=wolf.getUUID();Pets.enroll(wolf);
                var cat=EntityTypes.CAT.create(l,EntitySpawnReason.COMMAND);cat.tame(p);cat.setCustomName(Component.literal("Biscuit the Long-Named Cat of the Harvest Fair"));cat.snapTo(p.getX()-2,p.getY(),p.getZ(),0,0);l.addFreshEntity(cat);id[1]=cat.getUUID();Pets.enroll(cat);
                Pets.view(p,"");
            });
            c.waitTicks(10);c.takeScreenshot("pets_active_roster");
            // Recall and deploy the exact pet through the big key.
            c.clickScreenButton("Recall to stable");c.waitTicks(10);c.takeScreenshot("pets_stored");
            server.runOnServer(s->{if(!PetRoster.get(s).entries.get(id[0]).getStringOr("state","").equals("STORED"))throw new AssertionError("UI did not recall pet");});
            c.clickScreenButton("Deploy");c.waitTicks(10);
            server.runOnServer(s->{if(s.overworld().getEntity(id[0])==null)throw new AssertionError("UI did not deploy the exact pet");});
            // A behaviour key.
            c.clickScreenButton("Stay");c.waitTicks(10);c.takeScreenshot("pets_stay");
            server.runOnServer(s->{if(!PetRoster.get(s).entries.get(id[0]).getStringOr("mode","").equals("STAY"))throw new AssertionError("UI did not set STAY");});
            // The keyboard: Down picks the cat, Enter recalls it.
            c.getInput().pressKey(InputConstants.KEY_DOWN);c.waitTicks(5);
            c.getInput().pressKey(InputConstants.KEY_RETURN);c.waitTicks(10);c.takeScreenshot("pets_keyboard");
            server.runOnServer(s->{if(!PetRoster.get(s).entries.get(id[1]).getStringOr("state","").equals("STORED"))throw new AssertionError("Enter did not recall the chosen pet");});
            // A knocked-out companion: its key counts down and cannot deploy it yet.
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();
                if(s.overworld().getEntity(id[0]) instanceof Mob wolf){var entry=PetRoster.get(s).entries.get(id[0]);Pets.store(wolf,entry,true);}
                Pets.view(p,"Juniper was knocked out.");});
            c.waitTicks(10);c.getInput().pressKey(InputConstants.KEY_UP);c.waitTicks(5);c.takeScreenshot("pets_knocked_out");
            c.runOnClient(mc->{boolean waiting=Screens.getWidgets(mc.gui.screen()).stream().anyMatch(w->w.getMessage().getString().startsWith("Recovering")&&!w.active);
                if(!waiting)throw new AssertionError("A knocked-out pet's Deploy key waits for its recovery");});
            server.runOnServer(s->{if(!PetRoster.get(s).entries.get(id[0]).getStringOr("state","").equals("KNOCKED_OUT"))throw new AssertionError("The knock-out is recorded");});
            // The smallest usual window: the stable fits it.
            c.getInput().resizeWindow(640,480);c.waitTicks(10);c.takeScreenshot("pets_compact");
            c.runOnClient(mc->{var screen=(PetInventoryScreen)mc.gui.screen();var area=screen.panelArea();
                if(area.left()<0||area.top()<0||area.right()>screen.width||area.bottom()>screen.height)throw new AssertionError("The stable does not fit a 640x480 window: "+area);});
            c.clickScreenButton("Done");c.waitForScreen(null);
        }
    }
}
