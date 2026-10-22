package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.client.update.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screens.TitleScreen;

/** Menu rendering only, with fixture data. No networking, installation or live world. */
public class UpdateMenuClientGameTests implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context){
        for(var state:new JugcraftUpdates.State[]{JugcraftUpdates.State.AVAILABLE,JugcraftUpdates.State.DOWNLOADING,JugcraftUpdates.State.READY,JugcraftUpdates.State.ERROR}) {
            context.runOnClient(client->{
                JugcraftUpdates.state=state;JugcraftUpdates.current="0.1.0-alpha.20261021.2";JugcraftUpdates.latest="0.1.0-alpha.20261021.3";JugcraftUpdates.progress=0.55f;
                JugcraftUpdates.message=switch(state){case AVAILABLE->"A new version is ready. Download it when you're ready.";case DOWNLOADING->"Downloading jugcraft.jar";case READY->"Download verified. Apply & quit, wait a moment, then press Play in Modrinth.";default->"Update service is unavailable. Try again later.";};
                client.gui.setScreen(new UpdateScreen(new TitleScreen()));
            });
            context.waitTicks(5);
            context.runOnClient(client->{
                var widgets=Screens.getWidgets(client.gui.screen());
                if(widgets.size()!=2)throw new AssertionError("Expected update and back controls");
                if(widgets.getFirst().active==(state==JugcraftUpdates.State.DOWNLOADING))throw new AssertionError("Incorrect download button state");
                if(!widgets.get(1).active)throw new AssertionError("Player must be able to return to menu");
            });
            context.takeScreenshot("jugcraft_update_"+state.name().toLowerCase(java.util.Locale.ROOT));
            context.runOnClient(client->client.gui.screen().onClose());
            context.runOnClient(client->{if(!(client.gui.screen() instanceof TitleScreen))throw new AssertionError("Back returns to title");});
        }
        context.runOnClient(client->{JugcraftUpdates.state=JugcraftUpdates.State.IDLE;});
    }
}
